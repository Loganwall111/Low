package net.mcsm.extras;

import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCSM -- the visible half of the Story Mode sequence.
 *
 * The mod's own shockwave and death blast DO fire (whocalls.py shows
 * die() -> deathBlast and addSubGrowth() -> phaseUpShockwave), so this does not
 * replace them -- it adds the signature MCSM staging around them, on the server,
 * where every effect is a plain vanilla particle so there is nothing to
 * desync and nothing for the client to opt into:
 *
 *   - phase 4 rise and phase 7 rise: an expanding ring of dust, ground to sky
 *   - death: a supernova of coloured rings, then a white flash
 *   - during phase 5.5+: purple motes and electric sparks under the storm
 *   - while the storm sweeps: dust waves off the ground
 *   - on death ("the tear closes"): nearby players are healed and the sequence
 *     is marked ended -- MCSM reads this as recovery, not as a loss
 *
 * Every branch is gated by McsmExtrasConfig so the control panel can turn each
 * piece off, and the whole thing is wrapped in a catch: a particle can never
 * break a tick.
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
                if (McsmExtrasConfig.deathCinematic) {
                    supernova(srv, self, gt);
                }
                if (McsmExtrasConfig.realityTear) {
                    recover(srv, self);
                }
            }
            if (st[1] > 0.0 && !self.isDeadOrDying()) {
                st[1] = 0.0;     // healed back (creative/command): allow a replay
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

            // ---- MCSM: advance any expanding blast in flight ------
            tickBlasts(srv, self);
        } catch (Throwable ignored) {
            // Never let a particle break a tick.
        }
    }

    // MCSM -- 26.2's ServerLevel.sendParticles takes PARTICLE OPTIONS,
    // not ParticleTypes, and DustParticleOptions is (int packed RGB, float
    // scale), not (Vector3f, float). Every effect here is therefore plain
    // dust with a colour: the closest deterministic spelling of the same
    // staging on the API the 26.2 client actually has (verified from the
    // runner's javap dump, ci/api/level.txt).
    private static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(rgb, scale);
    }

    private static int pack(float r, float g, float b) {
        int ri = (int) (Math.max(0.0f, Math.min(1.0f, r)) * 255.0f);
        int gi = (int) (Math.max(0.0f, Math.min(1.0f, g)) * 255.0f);
        int bi = (int) (Math.max(0.0f, Math.min(1.0f, b)) * 255.0f);
        return (ri << 16) | (gi << 8) | bi;
    }

    /**
     * Sends one particle batch with delivery FORCED.
     *
     * MCSM -- this is why the shockwaves were never seen. All nineteen
     * call sites in this driver used the
     * sendParticles(options, x, y, z, count, dx, dy, dz, speed) overload, which
     * forwards force = false. The server then drops the packet for any player
     * more than 32 blocks from the particle (blockPos.distSqr(target) >= 1024)
     * and drops it again when the client's Particle setting is decreased. A
     * Wither Storm's core and its ring geometry sit hundreds of blocks away and
     * high overhead, so the storm-centred effects -- rise shockwave, supernova,
     * purple motes, most of the dust wave -- were discarded on the server and
     * never reached a client, while the player-centred ones (the heal burst in
     * recover(), the lower rungs of the command wire, smoke pooled directly
     * underfoot) did arrive. That is precisely the reported split between what
     * was visible and what was not, and no gate check could ever have found it:
     * the code ran, the packets were built, and the server threw them away.
     *
     * The overload used here sets force = true, decreased = false.
     */
    private static void spawn(ServerLevel srv, DustParticleOptions options,
                              double x, double y, double z, int count,
                              double dx, double dy, double dz, double speed) {
        try {
            srv.sendParticles(options, true, false, x, y, z, count, dx, dy, dz, speed);
        } catch (Throwable ignored) {
            // a visual must never take the world tick down with it
        }
    }

    // ---------------------------------------------------------------------
    // MCSM -- EXPANDING BLASTS
    //
    // riseShockwave() and supernova() each emitted their whole geometry in a
    // single tick and stopped: three static rings, six static rings, gone. A
    // shockwave is only legible if the front TRAVELS, so each detonation now
    // also arms a small state machine keyed by the storm's UUID and advanced
    // from the storm's own tick. It walks the radius out along an ease-out
    // curve (fast at detonation, decelerating as it dies), stacks two verticals
    // per segment so the front reads as a wall rather than a line on the
    // ground, trails debris behind the front, and cycles the RINGS palette so
    // the colour travels with the wave instead of sitting still.
    //
    // Budget: 14 segments x 2 verticals + ~5 debris = ~33 packets per blast per
    // tick. Now that delivery is forced these actually arrive, so the count is
    // deliberately a fraction of the 144 the one-shot rings used to send.
    // ---------------------------------------------------------------------
    /**
     * Armed blasts: UUID -> [0]=startGameTime [1]=kind [2]=x [3]=y [4]=z
     * [5]=floorY [6]=bodyHeight. The geometry is captured when the blast is
     * armed rather than read from the entity each frame, because the death
     * blast has to keep expanding after the storm's entity is gone. Concurrent
     * map: the server thread arms it, the render thread draws it.
     */
    private static final Map<UUID, double[]> BLASTS = new ConcurrentHashMap<>();

    /**
     * Last game time a rise front was armed, per storm. Two observers can
     * notice the same phase jump; without this latch the second one re-arms
     * the blast and prints the armed line twice (seen in the player's chat
     * screenshots for phase 4).
     */
    private static final Map<UUID, Long> RISE_ARMED = new ConcurrentHashMap<>();

    /** Blast kinds: a phase rise, or the death supernova. */
    private static final int KIND_RISE  = 4;
    private static final int KIND_DEATH = 99;

    /** Expansion durations in ticks: 3 s for a rise, 5 s for the death blast. */
    private static final int RISE_TICKS  = 60;
    private static final int DEATH_TICKS = 100;

    /**
     * Where one frame of a blast goes: a server broadcast, or a local spawn.
     * Public so the client half (net.mcsm.extras.client.McsmClientBlasts) can
     * supply its own without this class ever importing a client class -- a
     * dedicated server loads this one, and must not have to resolve Minecraft.
     */
    public interface Sink {
        void send(DustParticleOptions options, double x, double y, double z,
                  int count, double dx, double dy, double dz, double speed);
    }

    /**
     * Wall clock of the last server-side death arming. In single-player the
     * client shares this JVM, so the sky latch in McsmBlobCarrierPatch reads it
     * and starts the 16 s band even for a death the client never sees the
     * entity enter -- /kill removes the storm outright, without it ever
     * reporting isDeadOrDying() to a client-side scan.
     */
    private static volatile long lastDeathArmMs = 0L;

    /** Read by the client sky latch; 0 before any death has armed. */
    public static long lastDeathArmMs() {
        return lastDeathArmMs;
    }

    /** Game time of the last client step, so frames cannot outrun ticks. */
    private static long lastClientGameTime = Long.MIN_VALUE;

    /**
     * Wall clock of the last client step. In single-player the client and the
     * integrated server share this JVM and therefore this map, so the server
     * half uses it to stand down: broadcasting a blast the client is already
     * drawing locally would double every particle.
     */
    private static volatile long lastClientStepMs = 0L;

    /** Arms an expanding blast for this storm. */
    private static void startBlast(ServerLevel srv, WitherStormEntity self, int kind) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double floorY = self.getBoundingBox().minY;
        long gt = srv.getGameTime();
        // MCSM -- dedupe rise arming: one front per phase jump, even
        // if more than one hook reports the transition. Phase 4 -> phase 7
        // rises are minutes apart, so a 5-second window cannot swallow a
        // legitimate second rise. Death keeps its own guard in deathCinematic.
        if (kind == KIND_RISE) {
            Long prev = RISE_ARMED.get(self.getUUID());
            if (prev != null && gt - prev < 100L) {
                return;
            }
            RISE_ARMED.put(self.getUUID(), gt);
        }
        BLASTS.put(self.getUUID(), new double[]{
                gt, kind, x, y, z, floorY, y - floorY});
        // MCSM -- say so in chat. These are two rare events per storm
        // life, and the line is the proof that the hook fired at all when the
        // report is "no shockwave happened".
        String msg = kind == KIND_DEATH
                ? "[ds] death sequence armed: expanding blast 5s + sky band 16s"
                : "[ds] phase " + kind + " rise shockwave armed: front expands 3s";
        for (ServerPlayer sp : srv.getPlayers(
                q -> q.distanceToSqr(x, y, z) < 600.0 * 600.0)) {
            sp.sendSystemMessage(Component.literal(msg));
        }
    }

    /**
     * Server-side advance, for a dedicated server where no client shares this
     * JVM. It runs from the storm's own tick, so it stops when the storm does --
     * which is precisely why the client half below exists.
     */
    private static void tickBlasts(ServerLevel srv, WitherStormEntity self) {
        if (System.currentTimeMillis() - lastClientStepMs < 2000L) {
            return;   // a local client is drawing this blast already
        }
        double[] d = BLASTS.get(self.getUUID());
        if (d == null) {
            return;
        }
        int duration = d[1] == KIND_DEATH ? DEATH_TICKS : RISE_TICKS;
        double t = (srv.getGameTime() - d[0]) / duration;
        if (t >= 1.0D) {
            BLASTS.remove(self.getUUID());
            return;
        }
        expandRing((o, px, py, pz, c, dx, dy, dz, sp) ->
                       spawn(srv, o, px, py, pz, c, dx, dy, dz, sp),
                   d, t, d[1] == KIND_DEATH);
    }

    /**
     * Advances every armed blast one tick, sending each frame to {@code sink}.
     * This is the half that makes the DEATH blast survivable.
     *
     * Driven once per frame from LevelRenderer.render by the client class. When
     * the storm dies its entity stops ticking within about a second, so a
     * server-driven animation freezes almost immediately -- that is the
     * "one-tick puff" this replaces. The client keeps its own clock and spawns
     * its own particles, so the front runs its full five seconds whether or not
     * the entity still exists. Throttled to one step per game tick rather than
     * per frame, so a 240 Hz monitor does not get eight times the particles.
     *
     * @param sink     where this frame's particles go
     * @param gameTime the caller's clock, used both to pace and to age the blast
     * @return true if a step was taken
     */
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
        for (Iterator<Map.Entry<UUID, double[]>> it = BLASTS.entrySet().iterator();
             it.hasNext();) {
            double[] d = it.next().getValue();
            int duration = d[1] == KIND_DEATH ? DEATH_TICKS : RISE_TICKS;
            double t = (gameTime - d[0]) / duration;
            if (t >= 1.0D || t < -4.0D) {
                it.remove();      // finished, or armed in a world we have left
                continue;
            }
            if (t < 0.0D) {
                t = 0.0D;
            }
            try {
                expandRing(sink, d, t, d[1] == KIND_DEATH);
                stepped = true;
            } catch (Throwable ignored) {
                // a visual must never break a frame
            }
        }
        return stepped;
    }

    /** One frame of the expanding front; {@code t} runs 0..1 across the blast. */
    private static void expandRing(Sink sink, double[] d, double t, boolean death) {
        double x = d[2], y = d[3], z = d[4], floorY = d[5], bodyH = d[6];

        double ease = 1.0D - (1.0D - t) * (1.0D - t);
        double maxR = death ? 320.0D : 200.0D;
        double r = 6.0D + ease * maxR;
        double fade = 1.0D - t;

        // the front starts at the storm's floor and rides up its body
        double lift = (death ? bodyH * 0.45D : bodyH * 0.12D) * (1.0D - t * 0.35D);
        double baseY = floorY + lift + 1.0D;

        int k = Math.min(RINGS.length - 1, (int) (t * RINGS.length));
        float[] c = RINGS[k];
        float[] n = RINGS[Math.min(RINGS.length - 1, k + 1)];
        // MCSM -- motes twice as fat as before. At three hundred blocks
        // a 2-scale dust particle is sub-pixel: the death WAS rendering (the
        // chat lines proved the arming) and still read as nothing happening.
        float scale = (float) (5.0D + fade * 5.0D);

        int segments = 14;
        for (int i = 0; i < segments; i++) {
            double a = (i / (double) segments) * Math.PI * 2.0D;
            double px = x + Math.cos(a) * r;
            double pz = z + Math.sin(a) * r;

            // ground wall + sky wall: a front with height reads as a shockwave
            sink.send(dust(pack(c[0], c[1], c[2]), scale),
                      px, baseY, pz, 1, 0.0, 0.30, 0.0, 0.0);
            sink.send(dust(pack(n[0], n[1], n[2]), scale * 0.8f),
                      px, baseY + 3.0D + r * 0.02D, pz, 1, 0.0, 0.25, 0.0, 0.0);

            // debris dragged along just behind the front
            if (i % 3 == 0) {
                sink.send(dust(0x9aa0a6, 1.6f),
                          x + (px - x) * 0.94D, baseY + 0.5D, z + (pz - z) * 0.94D,
                          1, 0.4, 0.2, 0.4, 0.02);
            }
        }

        // MCSM -- the distance legibility pass, deaths only: a third
        // altitude layer, a white column from the storm's floor to the sky for
        // the first half (the "beam out of the sky" at the moment of death),
        // and pink embers raining out of the front as it dies.
        if (death) {
            for (int i = 0; i < segments; i += 2) {
                double a = (i / (double) segments) * Math.PI * 2.0D;
                double px = x + Math.cos(a) * r;
                double pz = z + Math.sin(a) * r;
                sink.send(dust(pack(c[0], c[1], c[2]), scale * 0.7f),
                          px, baseY + 10.0D, pz, 1, 0.0, 0.2, 0.0, 0.0);
            }
            if (t < 0.5D) {
                for (double yy = floorY + 2.0D; yy < floorY + 120.0D; yy += 6.0D) {
                    sink.send(dust(0xffffff, 7.0f), x, yy, z,
                              1, 0.4, 0.0, 0.4, 0.0);
                }
            }
            if (t > 0.7D) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * Math.PI * 2.0D;
                    double rr = r * (0.3D + Math.random() * 0.7D);
                    sink.send(dust(0xff9ad5, 4.0f),
                              x + Math.cos(a) * rr, baseY + Math.random() * 20.0D,
                              z + Math.sin(a) * rr, 1, 0.3, 0.5, 0.3, 0.01);
                }
            }
        }

        // the detonation core: lit at the start, blows out white at the end
        if (t < 0.12D || t > 0.90D) {
            sink.send(dust(0xffffff, death ? 7.0f : 4.0f), x, y + 2.0D, z,
                      24, 3.0, 2.0, 3.0, 0.2);
        }
    }

    /** Direct mixin hook: phase-up shockwave fired from addSubGrowth(), not only from tick polling. */
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

    /** Direct mixin hook: death blast must happen while die() still has a live entity position. */
    public static void deathCinematic(WitherStormEntity self, Level level) {
        if (self == null || level == null || level.isClientSide()) return;
        if (!(level instanceof ServerLevel srv)) return;
        try {
            // MCSM -- die() AND remove() both lead here now, because
            // /kill never calls die(); the second arrival must not restart the
            // blast or fire the one-shot rings twice.
            double[] st = STATE.computeIfAbsent(self.getUUID(),
                    k -> new double[]{self.getPhase(), 0.0});
            if (st[1] > 0.0) {
                return;
            }
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

    /** Expanding dust ring, ground to sky, plus the grey smoke of the impact. */
    private static void riseShockwave(ServerLevel srv, WitherStormEntity self, int phase) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        float[] c = (phase >= 7) ? new float[]{1.0f, 0.45f, 0.80f}
                                 : new float[]{0.65f, 0.30f, 0.95f};
        DustParticleOptions puff = dust(pack(c[0], c[1], c[2]), 2.4f);
        for (int ring = 0; ring < 3; ring++) {
            double r = 6.0 + ring * 7.0;
            for (int i = 0; i < 48; i++) {
                double a = (i / 48.0) * Math.PI * 2.0;
                double px = x + Math.cos(a) * r;
                double pz = z + Math.sin(a) * r;
                spawn(srv, puff, px, y - 1.0 + ring * 0.6, pz, 1, 0.0, 0.35, 0.0, 0.0);
                if (i % 6 == 0) {
                    spawn(srv, dust(0x9aa0a6, 2.2f), px, y, pz, 2, 1.2, 1.0, 1.2, 0.03);
                    spawn(srv, dust(0xb8b8b8, 1.8f), px, y + 0.5, pz, 1, 1.0, 0.8, 1.0, 0.04);
                }
            }
        }
        spawn(srv, dust(0xd8e6ff, 0.7f), x, y + 2.0, z, 60, 6.0, 3.0, 6.0, 0.3);
        // MCSM -- the one-shot rings above are the detonation; this
        // arms the front that actually travels outward over the next 3 s.
        startBlast(srv, self, KIND_RISE);
    }

    /** Six expanding rings, one per MCSM colour, then a white flash. */
    private static void supernova(ServerLevel srv, WitherStormEntity self, long gt) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        for (int k = 0; k < RINGS.length; k++) {
            DustParticleOptions puff = dust(pack(RINGS[k][0], RINGS[k][1], RINGS[k][2]), 3.0f);
            double r = 4.0 + k * 3.4;
            double yy = y + k * 1.1;
            for (int i = 0; i < 64; i++) {
                double a = (i / 64.0) * Math.PI * 2.0 + (k * 0.13);
                spawn(srv, puff,
                    x + Math.cos(a) * r, yy, z + Math.sin(a) * r,
                    1, 0.0, 0.6, 0.0, 0.0);
            }
        }
        spawn(srv, dust(0xffffff, 4.0f), x, y + 3.0, z, 2, 0.0, 0.0, 0.0, 0.0);
        spawn(srv, dust(0xd8e6ff, 0.7f), x, y + 3.0, z, 120, 8.0, 5.0, 8.0, 0.4);
        spawn(srv, dust(0x9aa0a6, 2.2f), x, y + 2.0, z, 80, 7.0, 4.0, 7.0, 0.12);
        // MCSM -- and the death front, expanding for 5 s. This is the
        // particle half of the death sequence; the sky half (cracks, implosion,
        // ring flare) is driven into the FogSkyEnd band 1906..2900 by
        // McsmBlobCarrierPatch and drawn by mcsm_death() in core/sky.fsh.
        startBlast(srv, self, KIND_DEATH);
    }

    /** The tear closes: MCSM reads survival as recovery, so heal and cleanse. */
    private static void recover(ServerLevel srv, WitherStormEntity self) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        for (ServerPlayer sp : srv.getPlayers(p -> p.isAlive()
                && p.distanceToSqr(x, y, z) < 96.0 * 96.0)) {
            sp.heal(12.0f);
            spawn(srv, dust(0x7ddf64, 0.8f), sp.getX(), sp.getY() + 1.0, sp.getZ(),
                              24, 0.8, 0.8, 0.8, 0.2);
            spawn(srv, dust(0xff4d6d, 1.0f), sp.getX(), sp.getY() + 1.6, sp.getZ(),
                              8, 0.5, 0.5, 0.5, 0.1);
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
            spawn(srv, dust(0x9d6bff, 1.4f), x + Math.cos(a) * rr, py, z + Math.sin(a) * rr,
                              3, 0.6, 1.4, 0.6, 0.02);
        }
        if (gt % 40L == 0L) {
            spawn(srv, dust(0xd8e6ff, 0.7f), x, y + 18.0, z, 40, 10.0, 6.0, 10.0, 0.35);
        }
    }

    private static void dustWave(ServerLevel srv, WitherStormEntity self) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double r = self.getBoundingBox().getXsize() * 0.5 + 6.0;
        double py = Math.max(self.getBoundingBox().minY, y - 22.0);
        for (int i = 0; i < 8; i++) {
            double a = (i / 8.0) * Math.PI * 2.0;
            spawn(srv, dust(0x9aa0a6, 2.2f), x + Math.cos(a) * r, py, z + Math.sin(a) * r,
                              3, 1.6, 0.7, 1.6, 0.05);
        }
    }

    private static void smokePool(ServerLevel srv, WitherStormEntity self) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double r = self.getBoundingBox().getXsize() * 0.55;
        double py = Math.max(self.getBoundingBox().minY, y - 30.0) + 1.0;
        spawn(srv, dust(0x5a5a5a, 4.0f), x, py, z, 6, r * 0.7, 1.2, r * 0.7, 0.02);
        spawn(srv, dust(0x777777, 3.0f), x, py + 2.0, z, 8, r * 0.9, 2.0, r * 0.9, 0.03);
    }

    /**
     * The wire: a taut line of end-rod motes from the storm's core down to its
     * ground anchor, a pulse running down it, and a sparking node where it
     * lands. The anchor is the bounding-box floor -- already used by the dust
     * wave, so it needs no heightmap lookup.
     */
    private static void commandWire(ServerLevel srv, WitherStormEntity self, long gt) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        double gy = self.getBoundingBox().minY + 0.5;
        double len = y - gy;
        if (len < 2.0) {
            return;
        }
        if (gt % 2L != 0L) {
            return;   // halve the rate: three strands now cost triple
        }
        // MCSM -- three strands and a brighter core. A single 0.9-scale
        // dust line at two hundred blocks read as nothing at all; "there are no
        // beams coming out of the sky" was a visibility failure, not a missing
        // feature. The beam also runs UP from the core, so it reads as a beam
        // out of the sky and not just a scratch on the storm's belly.
        int steps = 26;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double wy = y - len * t;
            for (int strand = -1; strand <= 1; strand++) {
                double off = strand * 0.45D;
                spawn(srv, dust(strand == 0 ? 0xd8fff2 : 0xbfffe8,
                                strand == 0 ? 1.4f : 0.9f),
                      x + off, wy, z + off, 1, 0.03, 0.0, 0.03, 0.0);
            }
        }
        for (int i = 1; i <= 12; i++) {
            spawn(srv, dust(0xbfffe8, 0.9f), x, y + i * 3.0D, z,
                  1, 0.05, 0.0, 0.05, 0.0);
        }
        double pt = (gt % 30L) / 30.0;
        spawn(srv, dust(0xffb347, 1.8f), x, y - len * pt, z, 3, 0.05, 0.05, 0.05, 0.0);
        spawn(srv, dust(0xffb347, 1.8f), x, y + pt * 36.0D, z, 3, 0.05, 0.05, 0.05, 0.0);
        if (gt % 20L == 0L) {
            for (int i = 0; i < 16; i++) {
                double a = (i / 16.0) * Math.PI * 2.0;
                spawn(srv, dust(0xd8e6ff, 0.7f),
                    x + Math.cos(a) * 1.6, gy + 0.3, z + Math.sin(a) * 1.6,
                    1, 0.1, 0.2, 0.1, 0.02);
            }
        }
    }

    /** Brief anyone who has come within 120 blocks and not been told yet. */
    private static void briefNearby(ServerLevel srv, WitherStormEntity self) {
        double x = self.getX(), y = self.getY(), z = self.getZ();
        for (ServerPlayer sp : srv.getPlayers(p -> p.isAlive()
                && p.distanceToSqr(x, y, z) < 120.0 * 120.0)) {
            McsmStory.brief(sp);
        }
    }

    /** Forget a storm (entity removed) so the map cannot grow without bound. */
    public static void forget(UUID id) {
        STATE.remove(id);
        BLASTS.remove(id);   // MCSM -- an in-flight blast dies with it
    }

    private McsmFxDriver() {}
}
