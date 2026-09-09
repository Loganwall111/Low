package net.mcsm.extras;

import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCSM 1.9.172 -- Visual FX Driver, Atmospheric VFX, Blasts & Enhanced Storm AI.
 */
public final class McsmFxDriver {

    /** Per-storm scratch: [0] = last seen phase, [1] = 1 once death fx fired. */
    private static final Map<UUID, double[]> STATE = new HashMap<>();

    /** Supernova ring colours, in MCSM order. */
    private static final float[][] RINGS = {
        {0.62f, 0.22f, 0.95f},   // purple
        {1.00f, 0.42f, 0.78f},   // pink
        {0.28f, 0.48f, 1.00f},   // blue
        {1.00f, 0.58f, 0.16f},   // orange
        {0.36f, 0.95f, 0.42f},   // green
        {1.00f, 0.92f, 0.35f},   // yellow
    };

    /** Blast kinds: a phase rise, or the death supernova. */
    private static final int KIND_RISE  = 4;
    private static final int KIND_DEATH = 99;

    /** Expansion durations in ticks: 3 s for a rise, 5 s for the death blast. */
    private static final int RISE_TICKS  = 60;
    private static final int DEATH_TICKS = 100;

    /**
     * Where one frame of a blast goes: server broadcast, or local client spawn.
     */
    public interface Sink {
        void send(DustParticleOptions options, double x, double y, double z,
                  int count, double dx, double dy, double dz, double speed);
    }

    private static volatile long lastDeathArmMs = 0L;

    public static long lastDeathArmMs() {
        return lastDeathArmMs;
    }

    private static final Map<UUID, double[]> BLASTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RISE_ARMED = new ConcurrentHashMap<>();
    private static long lastClientGameTime = -1L;
    private static long lastClientStepMs = 0L;

    public static void tick(WitherStormEntity self, Level level, long gt) {
        if (self == null || level == null || level.isClientSide()) {
            return;
        }
        if (!(level instanceof ServerLevel srv)) {
            return;
        }
        try {
            McsmExtrasConfig.load();
            UUID id = self.getUUID();
            double[] st = STATE.computeIfAbsent(id, k -> new double[]{-1.0, 0.0});
            double phase = self.getPhase();

            // ---- phase rise shockwaves at 4 and 7 --------------------------
            double last = st[0];
            if (last >= 0.0) {
                if (last < 4.0 && phase >= 4.0) riseShockwave(srv, self, 4);
                if (last < 7.0 && phase >= 7.0) riseShockwave(srv, self, 7);
            }
            st[0] = phase;

            // ---- death: supernova rings -> flash -> recovery ---------------
            if (st[1] == 0.0 && self.isDeadOrDying()) {
                st[1] = 1.0;
                lastDeathArmMs = System.currentTimeMillis();
                if (McsmExtrasConfig.deathCinematic) {
                    supernova(srv, self, gt);
                }
                if (McsmExtrasConfig.realityTear) {
                    recover(srv, self);
                }
            }
            if (st[1] > 0.0 && !self.isDeadOrDying()) {
                st[1] = 0.0;     // healed back: allow replay
            }

            // ---- phase 5.5+: purple sky motes + sparks ---------------------
            if (McsmExtrasConfig.purpleSky && phase >= 5.5 && gt % 4L == 0L) {
                purpleMotes(srv, self, gt);
            }

            // ---- dust waves while it sweeps the ground ---------------------
            if (McsmExtrasConfig.dustWaves && gt % 6L == 0L) {
                dustWave(srv, self);
            }

            // ---- smoke screen: heavy smoke pooled under the body -----------
            if (McsmExtrasConfig.smokeScreen && gt % 3L == 0L) {
                smokePool(srv, self);
            }

            // ---- the command wire: core to ground anchor, with a pulse -----
            if (McsmExtrasConfig.commandWire) {
                commandWire(srv, self, gt);
            }

            // ---- briefing: told once, the first time they get close -------
            if (McsmExtrasConfig.mcsmInstructions && gt % 20L == 0L) {
                briefNearby(srv, self);
            }

            // ---- Magical colored sparkles (white/pink/purple) -------------
            if (McsmExtrasConfig.coloredSparkles && gt % 3L == 0L) {
                coloredSparkles(srv, self, gt);
            }

            // ---- Enhanced Wither Storm AI & Targeting ---------------------
            if (McsmExtrasConfig.witherStormEnhancedAi && gt % 20L == 0L) {
                enhancedAiTick(srv, self);
            }

            // ---- Environmental & Portal VFX around players ---------------
            if (gt % 15L == 0L) {
                environmentalPlayerVfx(srv, gt);
            }

            // Server-side fallback step for dedicated servers
            if (System.currentTimeMillis() - lastClientStepMs > 250L) {
                stepBlasts((options, x, y, z, count, dx, dy, dz, speed) ->
                    spawn(srv, options, x, y, z, count, dx, dy, dz, speed), gt);
            }
        } catch (Throwable ignored) {
            // Never let a particle break a tick.
        }
    }

    private static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(rgb, scale);
    }

    private static int pack(float r, float g, float b) {
        int ri = (int) (Math.max(0.0f, Math.min(1.0f, r)) * 255.0f);
        int gi = (int) (Math.max(0.0f, Math.min(1.0f, g)) * 255.0f);
        int bi = (int) (Math.max(0.0f, Math.min(1.0f, b)) * 255.0f);
        return (ri << 16) | (gi << 8) | bi;
    }

    private static void spawn(ServerLevel srv, DustParticleOptions options,
                              double x, double y, double z, int count,
                              double dx, double dy, double dz, double speed) {
        try {
            srv.sendParticles(options, true, false, x, y, z, count, dx, dy, dz, speed);
        } catch (Throwable ignored) {
        }
    }

    private static void startBlast(ServerLevel srv, WitherStormEntity self, int kind) {
        long gt = srv.getGameTime();
        UUID id = self.getUUID();
        if (kind == KIND_RISE) {
            Long last = RISE_ARMED.get(id);
            if (last != null && gt - last < 40L) return;
            RISE_ARMED.put(id, gt);
        }
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double floorY = self.getBoundingBox().minY;
        double bodyH = Math.max(12.0, self.getBoundingBox().getYsize());
        BLASTS.put(id, new double[]{ (double) gt, (double) kind, x, y, z, floorY, bodyH });
    }

    public static boolean stepBlasts(Sink sink, long gameTime) {
        if (sink == null || BLASTS.isEmpty()) {
            return false;
        }
        if (gameTime == lastClientGameTime) {
            return false;
        }
        lastClientGameTime = gameTime;
        lastClientStepMs = System.currentTimeMillis();

        boolean stepped = false;
        for (Iterator<Map.Entry<UUID, double[]>> it = BLASTS.entrySet().iterator(); it.hasNext();) {
            double[] d = it.next().getValue();
            int duration = d[1] == KIND_DEATH ? DEATH_TICKS : RISE_TICKS;
            double t = (gameTime - d[0]) / (double) duration;
            if (t > 1.0D) {
                it.remove();
                continue;
            }
            if (t < 0.0D) {
                t = 0.0D;
            }
            try {
                expandRing(sink, d, t, d[1] == KIND_DEATH);
                stepped = true;
            } catch (Throwable ignored) {
            }
        }
        return stepped;
    }

    private static void expandRing(Sink sink, double[] d, double t, boolean death) {
        double x = d[2], y = d[3], z = d[4], floorY = d[5], bodyH = d[6];
        double ease = 1.0D - (1.0D - t) * (1.0D - t);
        double maxR = death ? 320.0D : 200.0D;
        double r = 6.0D + ease * maxR;

        double lift = (death ? bodyH * 0.45D : bodyH * 0.12D) * (1.0D - t * 0.35D);
        double baseY = floorY + lift + 1.0D;

        int k = Math.min(RINGS.length - 1, (int) (t * RINGS.length));
        float[] c = RINGS[k];
        DustParticleOptions puff = dust(pack(c[0], c[1], c[2]), (float) Math.max(0.8, 3.2 * (1.0 - t * 0.6)));

        int segs = 18;
        for (int i = 0; i < segs; i++) {
            double a = (i / (double) segs) * Math.PI * 2.0;
            sink.send(puff, x + Math.cos(a) * r, baseY, z + Math.sin(a) * r, 1, 0.0, 0.2, 0.0, 0.0);
            sink.send(puff, x + Math.cos(a) * r, baseY + bodyH * 0.35D, z + Math.sin(a) * r, 1, 0.0, 0.3, 0.0, 0.0);
        }
    }

    public static void phaseShockwave(WitherStormEntity self, Level level, int phase) {
        if (self == null || level == null || level.isClientSide()) return;
        McsmExtrasConfig.load();
        if (!McsmExtrasConfig.enableRiseFx && !McsmExtrasConfig.supernovaRings) return;
        if (!(level instanceof ServerLevel srv)) return;
        try {
            riseShockwave(srv, self, phase);
        } catch (Throwable ignored) {
        }
    }

    public static void deathCinematic(WitherStormEntity self, Level level) {
        if (self == null || level == null || level.isClientSide()) return;
        if (!(level instanceof ServerLevel srv)) return;
        try {
            double[] st = STATE.computeIfAbsent(self.getUUID(), k -> new double[]{self.getPhase(), 0.0});
            if (st[1] > 0.0) return;
            st[1] = 1.0;
            lastDeathArmMs = System.currentTimeMillis();
            McsmExtrasConfig.load();
            long gt = level.getGameTime();
            if (McsmExtrasConfig.deathCinematic || McsmExtrasConfig.supernovaRings) {
                supernova(srv, self, gt);
            }
            if (McsmExtrasConfig.realityTear) {
                recover(srv, self);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void riseShockwave(ServerLevel srv, WitherStormEntity self, int phase) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        float[] c = (phase >= 7) ? new float[]{1.0f, 0.45f, 0.80f} : new float[]{0.65f, 0.30f, 0.95f};
        DustParticleOptions puff = dust(pack(c[0], c[1], c[2]), 2.4f);
        for (int ring = 0; ring < 3; ring++) {
            double r = 6.0 + ring * 7.0;
            for (int i = 0; i < 48; i++) {
                double a = (i / 48.0) * Math.PI * 2.0;
                double px = x + Math.cos(a) * r;
                double pz = z + Math.sin(a) * r;
                spawn(srv, puff, px, y - 1.0 + ring * 0.6, pz, 1, 0.0, 0.35, 0.0, 0.0);
            }
        }
        startBlast(srv, self, KIND_RISE);
    }

    private static void supernova(ServerLevel srv, WitherStormEntity self, long gt) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        for (int k = 0; k < RINGS.length; k++) {
            DustParticleOptions puff = dust(pack(RINGS[k][0], RINGS[k][1], RINGS[k][2]), 3.0f);
            double r = 4.0 + k * 3.4;
            double yy = y + k * 1.1;
            for (int i = 0; i < 64; i++) {
                double a = (i / 64.0) * Math.PI * 2.0 + (k * 0.13);
                spawn(srv, puff, x + Math.cos(a) * r, yy, z + Math.sin(a) * r, 1, 0.0, 0.6, 0.0, 0.0);
            }
        }
        spawn(srv, dust(0xffffff, 4.0f), x, y + 3.0, z, 2, 0.0, 0.0, 0.0, 0.0);
        startBlast(srv, self, KIND_DEATH);
    }

    private static void recover(ServerLevel srv, WitherStormEntity self) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        for (ServerPlayer sp : srv.getPlayers(p -> p.isAlive() && p.distanceToSqr(x, y, z) < 96.0 * 96.0)) {
            sp.heal(12.0f);
            spawn(srv, dust(0x7ddf64, 0.8f), sp.getX(), sp.getY() + 1.0, sp.getZ(), 24, 0.8, 0.8, 0.8, 0.2);
            spawn(srv, dust(0xff4d6d, 1.0f), sp.getX(), sp.getY() + 1.6, sp.getZ(), 8, 0.5, 0.5, 0.5, 0.1);
        }
        spawn(srv, dust(0xffd76a, 1.2f), x, y + 2.0, z, 160, 4.0, 3.0, 4.0, 0.35);
    }

    private static void purpleMotes(ServerLevel srv, WitherStormEntity self, long gt) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double r = self.getBoundingBox().getXsize() * 0.5 + 14.0;
        long seed = gt * 2654435761L;
        for (int i = 0; i < 10; i++) {
            double a = ((seed >>> (i * 3)) % 360) / 360.0 * Math.PI * 2.0;
            double rr = r + ((seed >>> (i * 5)) % 24);
            double py = y + ((seed >>> (i * 7)) % 40) - 12.0;
            spawn(srv, dust(0x9d6bff, 1.4f), x + Math.cos(a) * rr, py, z + Math.sin(a) * rr, 3, 0.6, 1.4, 0.6, 0.02);
        }
    }

    private static void dustWave(ServerLevel srv, WitherStormEntity self) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double r = self.getBoundingBox().getXsize() * 0.5 + 6.0;
        double py = Math.max(self.getBoundingBox().minY, y - 22.0);
        for (int i = 0; i < 8; i++) {
            double a = (i / 8.0) * Math.PI * 2.0;
            spawn(srv, dust(0x9aa0a6, 2.2f), x + Math.cos(a) * r, py, z + Math.sin(a) * r, 3, 1.6, 0.7, 1.6, 0.05);
        }
    }

    private static void smokePool(ServerLevel srv, WitherStormEntity self) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double r = self.getBoundingBox().getXsize() * 0.55;
        double py = Math.max(self.getBoundingBox().minY, y - 30.0) + 1.0;
        spawn(srv, dust(0x5a5a5a, 4.0f), x, py, z, 6, r * 0.7, 1.2, r * 0.7, 0.02);
        spawn(srv, dust(0x777777, 3.0f), x, py + 2.0, z, 8, r * 0.9, 2.0, r * 0.9, 0.03);
    }

    private static void commandWire(ServerLevel srv, WitherStormEntity self, long gt) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double gy = self.getBoundingBox().minY + 0.5;
        double len = y - gy;
        if (len < 2.0) return;
        if (gt % 2L != 0L) return;

        int steps = 26;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double wy = y - len * t;
            for (int strand = -1; strand <= 1; strand++) {
                double off = strand * 0.45D;
                spawn(srv, dust(strand == 0 ? 0xd8fff2 : 0xbfffe8, strand == 0 ? 1.4f : 0.9f),
                      x + off, wy, z + off, 1, 0.03, 0.0, 0.03, 0.0);
            }
        }
        double pt = (gt % 30L) / 30.0;
        spawn(srv, dust(0xffb347, 1.8f), x, y - len * pt, z, 3, 0.05, 0.05, 0.05, 0.0);
    }

    /** Shimmering white, pink and purple sparkles around the storm body. */
    private static void coloredSparkles(ServerLevel srv, WitherStormEntity self, long gt) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double r = self.getBoundingBox().getXsize() * 0.6 + 8.0;
        long seed = gt * 104729L;
        int[] colors = {0xFFFFFF, 0xFF69B4, 0x9D4EDD, 0x5BC0BE, 0xF72585};
        for (int i = 0; i < 8; i++) {
            double a = ((seed >>> (i * 4)) % 360) / 360.0 * Math.PI * 2.0;
            double dist = r + ((seed >>> (i * 2)) % 16);
            double py = y + ((seed >>> (i * 3)) % 30) - 8.0;
            int c = colors[i % colors.length];
            spawn(srv, dust(c, 1.2f), x + Math.cos(a) * dist, py, z + Math.sin(a) * dist, 2, 0.4, 0.4, 0.4, 0.02);
        }
    }

    /** Enhanced aggressive Wither Storm AI: priority target tracking and attacks. */
    private static void enhancedAiTick(ServerLevel srv, WitherStormEntity self) {
        try {
            double x = self.getX(), y = self.getY(), z = self.getZ();
            ServerPlayer priorityTarget = null;
            double bestScore = -1.0;

            for (ServerPlayer player : srv.getPlayers(p -> p.isAlive() && p.distanceToSqr(x, y, z) < 200.0 * 200.0)) {
                double score = 10.0;
                if (player.getMainHandItem().is(Items.BEACON) || player.getOffhandItem().is(Items.BEACON)) {
                    score += 50.0;
                }
                if (player.getMainHandItem().is(Items.NETHER_STAR) || player.getOffhandItem().is(Items.NETHER_STAR)) {
                    score += 40.0;
                }
                double dist = player.distanceTo(self);
                score += Math.max(0.0, 100.0 - dist);

                if (score > bestScore) {
                    bestScore = score;
                    priorityTarget = player;
                }
            }

            if (priorityTarget != null) {
                self.setUltimateTarget(priorityTarget);
                if (priorityTarget.distanceTo(self) < 64.0 && srv.getRandom().nextFloat() < 0.25F) {
                    self.forceTentacleSlam();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** Environmental particles: Nether lava sparks, Portal lights, fireflies, comets. */
    private static void environmentalPlayerVfx(ServerLevel srv, long gt) {
        try {
            for (ServerPlayer sp : srv.players()) {
                if (!sp.isAlive()) continue;
                double px = sp.getX(), py = sp.getY(), pz = sp.getZ();
                BlockPos center = sp.blockPosition();

                // 1. Nether Portal & End Portal Lights / Particles
                if (McsmExtrasConfig.portalLights) {
                    for (int dx = -8; dx <= 8; dx += 4) {
                        for (int dy = -4; dy <= 6; dy += 3) {
                            for (int dz = -8; dz <= 8; dz += 4) {
                                BlockPos bp = center.offset(dx, dy, dz);
                                BlockState bs = srv.getBlockState(bp);
                                if (bs.is(Blocks.NETHER_PORTAL)) {
                                    spawn(srv, dust(0x9A4BFF, 1.5f), bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 4, 0.4, 0.6, 0.4, 0.05);
                                    spawn(srv, dust(0xD86BFF, 1.0f), bp.getX() + 0.5, bp.getY() + 0.8, bp.getZ() + 0.5, 2, 0.2, 0.4, 0.2, 0.03);
                                } else if (bs.is(Blocks.END_PORTAL) || bs.is(Blocks.END_PORTAL_FRAME)) {
                                    spawn(srv, dust(0x3BFFDC, 1.4f), bp.getX() + 0.5, bp.getY() + 0.6, bp.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.04);
                                    spawn(srv, dust(0x9D4EDD, 1.2f), bp.getX() + 0.5, bp.getY() + 0.7, bp.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
                                } else if (McsmExtrasConfig.beaconGlow && bs.is(Blocks.BEACON)) {
                                    spawn(srv, dust(0x4DFFFF, 2.0f), bp.getX() + 0.5, bp.getY() + 1.2, bp.getZ() + 0.5, 5, 0.5, 1.5, 0.5, 0.08);
                                    spawn(srv, dust(0xFFFFFF, 1.5f), bp.getX() + 0.5, bp.getY() + 2.0, bp.getZ() + 0.5, 3, 0.2, 2.0, 0.2, 0.1);
                                }
                            }
                        }
                    }
                }

                // 2. Nether Lava Sparks
                if (McsmExtrasConfig.netherRedFog && srv.dimension() == Level.NETHER) {
                    if (srv.getRandom().nextFloat() < 0.6F) {
                        double ox = (srv.getRandom().nextDouble() - 0.5) * 24.0;
                        double oz = (srv.getRandom().nextDouble() - 0.5) * 24.0;
                        spawn(srv, dust(0xFF4500, 1.3f), px + ox, py + srv.getRandom().nextDouble() * 6.0, pz + oz, 3, 0.2, 0.8, 0.2, 0.05);
                        spawn(srv, dust(0xFFD700, 1.0f), px + ox, py + srv.getRandom().nextDouble() * 8.0, pz + oz, 2, 0.1, 0.6, 0.1, 0.04);
                    }
                }

                // 3. Night Sky Comets / Shooting Stars
                if (McsmExtrasConfig.comets && srv.dimension() == Level.OVERWORLD && srv.isNight()) {
                    if (srv.getRandom().nextFloat() < 0.12F) {
                        double cx = px + (srv.getRandom().nextDouble() - 0.5) * 80.0;
                        double cy = py + 45.0 + srv.getRandom().nextDouble() * 20.0;
                        double cz = pz + (srv.getRandom().nextDouble() - 0.5) * 80.0;
                        for (int k = 0; k < 6; k++) {
                            spawn(srv, dust(0xD8E6FF, 1.8f), cx + k * 1.5, cy - k * 0.8, cz + k * 1.5, 1, 0.05, 0.05, 0.05, 0.0);
                            spawn(srv, dust(0x80D8FF, 1.2f), cx + k * 1.5, cy - k * 0.8, cz + k * 1.5, 1, 0.05, 0.05, 0.05, 0.0);
                        }
                    }
                }

                // 4. Overworld Fireflies at night
                if (McsmExtrasConfig.biomeAtmospherics && srv.dimension() == Level.OVERWORLD && srv.isNight()) {
                    if (srv.getRandom().nextFloat() < 0.4F) {
                        double fx = px + (srv.getRandom().nextDouble() - 0.5) * 16.0;
                        double fy = py + 0.5 + srv.getRandom().nextDouble() * 2.5;
                        double fz = pz + (srv.getRandom().nextDouble() - 0.5) * 16.0;
                        spawn(srv, dust(0xCCFF33, 0.9f), fx, fy, fz, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void briefNearby(ServerLevel srv, WitherStormEntity self) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        for (ServerPlayer sp : srv.getPlayers(p -> p.isAlive() && p.distanceToSqr(x, y, z) < 120.0 * 120.0)) {
            McsmStory.brief(sp);
        }
    }

    public static void forget(UUID id) {
        STATE.remove(id);
        BLASTS.remove(id);
    }

    private McsmFxDriver() {}
}
