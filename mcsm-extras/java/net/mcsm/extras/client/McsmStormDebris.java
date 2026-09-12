package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.mcsm.extras.McsmExtrasConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.9.208 -- DEBRIS OVERHAUL + GLACIER FLAKES.
 *
 *  - Debris density is pinned at MAXIMUM by default (debrisAlwaysMax): the
 *    storm is wrapped in the full hundred-fragment dark block swarm plus the
 *    purple dust vortex, no matter the phase.
 *  - Real orbiting CUBE rings: obsidian fragments released on circular
 *    horizontal, vertical and diagonal tracks with pure tangential velocity,
 *    so they visibly circle the body instead of just drifting.
 *  - GLACIER FLAKES (phase 4+): icy-white snow/ice block fragments torn off
 *    the body surface and pulled UPWARD, spiralling around the storm like a
 *    tornado, the way the reference frames read.
 *
 * Fully wrapped: a visual can never break a frame.
 */
public final class McsmStormDebris {

    private McsmStormDebris() {
    }

    private static final BlockParticleOption OBSIDIAN =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState());
    private static final BlockParticleOption BLACKSTONE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACKSTONE.defaultBlockState());
    private static final BlockParticleOption CRYING =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CRYING_OBSIDIAN.defaultBlockState());
    private static final BlockParticleOption GLACIER =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());
    private static final BlockParticleOption GLACIER2 =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState());
    private static final DustParticleOptions PURPLE = new DustParticleOptions(0xFF8A2BE2, 1.9F);
    private static final DustParticleOptions VIOLET = new DustParticleOptions(0xFF5B1FA8, 2.4F);
    private static final DustParticleOptions ICY = new DustParticleOptions(0xFFEAF6FF, 1.1F);
    private static final DustParticleOptions ICY_BLUE = new DustParticleOptions(0xFFB8E6FF, 0.8F);

    private static float spin = 0.0F;
    /** previous phase per storm index -- phase-4 summon burst detection */
    private static final Map<Integer, Float> LAST_PHASE = new HashMap<>();

    private static double radius(float phase) {
        if (phase < 4.0F) return 4.0D + 1.5D * phase;
        if (phase < 5.0F) return 10.0D + 8.0D * (phase - 4.0D);
        return phase < 6.0F ? 18.0D + 22.0D * (phase - 5.0D)
                : Math.min(340.0D, 62.0D + 46.0D * (phase - 6.0D));
    }

    /** Call once per rendered frame. */
    public static void tick() {
        try {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc == null ? null : mc.level;
            if (level == null || mc.player == null || mc.isPaused()) return;
            spin += 0.045F;
            McsmExtrasConfig.load();
            boolean maxDebris = McsmExtrasConfig.debrisAlwaysMax;
            boolean glaciers = McsmExtrasConfig.glacierFlakes;
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                float phase = d.phase;
                if (phase < 2.0F) continue;
                double dx = d.dispX - mc.player.getX();
                double dz = d.dispZ - mc.player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 420.0D) continue;
                double r = radius(phase);
                double distMul = 1.0D - Mth.clamp((dist - 200.0D) / 220.0D, 0.0D, 0.85D);
                // 1.9.208: default = full density, every phase.
                int n = maxDebris
                        ? (int) (110.0D * distMul)
                        : (int) (Mth.clamp(12.0F + 18.0F * (phase - 2.0F), 12.0F, 110.0F) * distMul);
                for (int i = 0; i < n; i++) {
                    double ang = Math.random() * Math.PI * 2.0D;
                    double rr = r * (0.55D + Math.random() * 1.35D);
                    double h = (Math.random() * 2.0D - 1.0D) * r * 0.9D;
                    double px = d.dispX + Math.cos(ang) * rr;
                    double pz = d.dispZ + Math.sin(ang) * rr;
                    double py = d.dispY + r * 0.35D + h;
                    // swirl: tangential velocity + slow inward pull (vortex)
                    double tv = 0.18D + 0.12D * Math.random();
                    double vx = -Math.sin(ang) * tv - Math.cos(ang) * 0.05D;
                    double vz = Math.cos(ang) * tv - Math.sin(ang) * 0.05D;
                    double vy = (Math.random() - 0.5D) * 0.08D;
                    double roll = Math.random();
                    if (roll < 0.62D) {
                        BlockParticleOption blk = roll < 0.40D ? OBSIDIAN : (roll < 0.55D ? BLACKSTONE : CRYING);
                        level.addParticle(blk, px, py, pz, vx, vy, vz);
                    } else if (roll < 0.88D) {
                        level.addParticle(PURPLE, px, py, pz, vx * 0.5D, vy, vz * 0.5D);
                    } else {
                        level.addParticle(VIOLET, px, py + r * 0.15D, pz, vx * 0.3D, 0.03D, vz * 0.3D);
                    }
                }

                // ---- orbiting CUBE rings: horizontal + vertical + diagonal --
                // particles released each frame on circular tracks with pure
                // tangential velocity, so they circle the body for real.
                int cubes = (int) (26.0D * distMul);
                double ringR = r * 1.6D;
                for (int i = 0; i < cubes; i++) {
                    double ang = spin * 0.9D + (2.0D * Math.PI * i / 26.0D);
                    double tv = 0.34D;
                    double px = d.dispX + Math.cos(ang) * ringR;
                    double pz = d.dispZ + Math.sin(ang) * ringR;
                    double py = d.dispY + r * 0.30D + Math.sin(ang * 2.0D) * r * 0.08D;
                    level.addParticle(OBSIDIAN, px, py, pz,
                            -Math.sin(ang) * tv, 0.0D, Math.cos(ang) * tv);
                }
                int cubesV = (int) (18.0D * distMul);
                double ringV = r * 1.4D;
                for (int i = 0; i < cubesV; i++) {
                    double ang = spin * 0.7D + (2.0D * Math.PI * i / 18.0D);
                    double px = d.dispX + Math.cos(ang) * ringV;
                    double pz = d.dispZ;
                    double py = d.dispY + Math.sin(ang) * ringV;
                    level.addParticle(BLACKSTONE, px, py, pz,
                            -Math.sin(ang) * 0.30D, 0.0D, 0.0D);
                }
                int cubesD = (int) (14.0D * distMul);
                double ringD = r * 1.75D;
                for (int i = 0; i < cubesD; i++) {
                    double ang = spin * 0.8D + (2.0D * Math.PI * i / 14.0D);
                    double px = d.dispX + Math.cos(ang) * ringD;
                    double pz = d.dispZ + Math.sin(ang) * ringD * 0.55D;
                    double py = d.dispY + Math.sin(ang) * ringD * 0.55D;
                    level.addParticle(CRYING, px, py, pz,
                            -Math.sin(ang) * 0.30D, 0.0D, Math.cos(ang) * 0.30D);
                }

                // ---- GLACIER FLAKES (phase 4+): icy fragments ripped off the
                // body blocks, pulled UP and spiralling like a tornado -------
                if (glaciers && phase >= 4.0F) {
                    int flakes = (int) (Mth.clamp(20.0F + 14.0F * (phase - 4.0F), 20.0F, 90.0F) * distMul);
                    for (int i = 0; i < flakes; i++) {
                        double ang = Math.random() * Math.PI * 2.0D;
                        double rr = r * (0.30D + Math.random() * 0.85D);
                        double px = d.dispX + Math.cos(ang) * rr;
                        double pz = d.dispZ + Math.sin(ang) * rr;
                        double py = d.dispY + r * 0.2D + (Math.random() * 2.0D - 1.0D) * r * 0.8D;
                        // tornado: strong upward pull + tangential spiral
                        double tv = 0.10D + 0.16D * Math.random();
                        double up = 0.22D + 0.30D * Math.random();
                        double vx = -Math.sin(ang) * tv;
                        double vz = Math.cos(ang) * tv;
                        double roll = Math.random();
                        if (roll < 0.45D) {
                            level.addParticle(GLACIER, px, py, pz, vx, up, vz);
                        } else if (roll < 0.75D) {
                            level.addParticle(GLACIER2, px, py, pz, vx * 0.8D, up * 1.15D, vz * 0.8D);
                        } else if (roll < 0.90D) {
                            level.addParticle(ICY, px, py, pz, vx, up * 0.8D, vz);
                        } else {
                            level.addParticle(ICY_BLUE, px, py + r * 0.4D, pz, vx * 0.5D, -0.04D, vz * 0.5D);
                        }
                    }
                }

                // ---- BLOCK-MATCHING TORNADO (1.9.212) ---------------------
                // Phase 4 summons a tornado of particles that match the very
                // blocks they tear off; later phases also pull from the
                // ground under the storm. Not random dust -- the actual
                // block states from the world, spiralling upward.
                if (phase >= 4.0F) {
                    int cols = phase >= 6.0F ? 12 : 6;
                    for (int i = 0; i < cols; i++) {
                        double ang = Math.random() * Math.PI * 2.0D;
                        double rr = r * (0.35D + Math.random() * 0.65D);
                        int bx = (int) Math.floor(d.dispX + Math.cos(ang) * rr);
                        int bz = (int) Math.floor(d.dispZ + Math.sin(ang) * rr);
                        // find the first solid block up from the ground
                        int by = (int) Math.floor(d.dispY - r);
                        BlockState st = level.getBlockState(new BlockPos(bx, by, bz));
                        int scan = 0;
                        while (st.isAir() && scan < 64) {
                            by++;
                            st = level.getBlockState(new BlockPos(bx, by, bz));
                            scan++;
                        }
                        if (st.isAir()) {
                            continue;
                        }
                        double tv = 0.10D + 0.14D * Math.random();
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, st),
                                bx + 0.5D, by + 0.5D, bz + 0.5D,
                                -Math.sin(ang) * tv, 0.30D + 0.25D * Math.random(), Math.cos(ang) * tv);
                    }
                    // phase-4 summon: one outward burst when the storm crosses 4
                    Float prev = LAST_PHASE.put(d.entityId, phase);
                    if (prev != null && prev < 4.0F) {
                        for (int i = 0; i < 70; i++) {
                            double ang = Math.random() * Math.PI * 2.0D;
                            double rr2 = r * (0.5D + Math.random() * 0.9D);
                            level.addParticle(OBSIDIAN,
                                    d.dispX + Math.cos(ang) * rr2, d.dispY + (Math.random() - 0.5D) * r * 1.4D,
                                    d.dispZ + Math.sin(ang) * rr2,
                                    Math.cos(ang) * 0.5D, 0.15D + Math.random() * 0.5D, Math.sin(ang) * 0.5D);
                        }
                    }
                }

                // big dark smoke plumes in the body core for silhouette density
                int core = Math.max(2, n / 6);
                for (int i = 0; i < core; i++) {
                    double ang = Math.random() * Math.PI * 2.0D;
                    double rr = r * Math.random() * 0.9D;
                    level.addParticle(ParticleTypes.LARGE_SMOKE,
                            d.dispX + Math.cos(ang) * rr, d.dispY + r * 0.3D + (Math.random() - 0.5D) * r,
                            d.dispZ + Math.sin(ang) * rr,
                            -Math.sin(ang) * 0.12D, 0.02D, Math.cos(ang) * 0.12D);
                }
            }
        } catch (Throwable ignored) {
            // a visual must never break a frame
        }
    }
}
