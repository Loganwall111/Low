package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.9.204 -- Story Mode debris vortex around the Wither Storm.
 *
 * The MCSM storm is never a clean silhouette: it is wrapped in hundreds of dark
 * chaotic block fragments and a purple dust cloud that spirals into the body.
 * This spawns that cloud client-side every frame around every tracked storm
 * (phase >= 2), scaled with the body radius, so the creature reads dense and
 * massive instead of thin.  Obsidian/blackstone block-crack particles are the
 * "block fragments"; purple dust is the tractor-beam vortex tint.
 *
 * Fully wrapped: a visual can never break a frame.
 */
public final class McsmStormDebris {

    private McsmStormDebris() {}

    private static final BlockParticleOption OBSIDIAN =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState());
    private static final BlockParticleOption BLACKSTONE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACKSTONE.defaultBlockState());
    private static final BlockParticleOption CRYING =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CRYING_OBSIDIAN.defaultBlockState());
    private static final DustParticleOptions PURPLE = new DustParticleOptions(0xFF8A2BE2, 1.9F);
    private static final DustParticleOptions VIOLET = new DustParticleOptions(0xFF5B1FA8, 2.4F);

    private static float spin = 0.0F;

    private static double radius(float phase) {
        if (phase < 4.0F) return 4.0D + 1.5D * phase;
        if (phase < 5.0F) return 10.0D + 8.0D * (phase - 4.0F);
        return phase < 6.0F ? 18.0D + 22.0D * (phase - 5.0F) : 40.0D + 30.0D * (phase - 6.0F);
    }

    /** Call once per rendered frame. */
    public static void tick() {
        try {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc == null ? null : mc.level;
            if (level == null || mc.player == null || mc.isPaused()) return;
            spin += 0.035F;
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                float phase = d.phase;
                if (phase < 2.0F) continue;
                double dx = d.dispX - mc.player.getX();
                double dz = d.dispZ - mc.player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 420.0D) continue;
                double r = radius(phase);
                // density: ~40 fragments/frame at phase 4, ~110 at phase 6+, fewer when far
                int n = (int) (Mth.clamp(12.0F + 18.0F * (phase - 2.0F), 12.0F, 110.0F)
                        * (1.0D - Mth.clamp((dist - 200.0D) / 220.0D, 0.0D, 0.85D)));
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
