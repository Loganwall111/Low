package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Storm-only sky/fog colour from mcsm_atmosphere palettes.
 * Calm day/night never go purple — only active storm phases do.
 * Wired every client tick; does not touch FabricSkyboxes.
 */
public final class McsmStormAtmosphere {

    private McsmStormAtmosphere() {
    }

    private static float ramp(float v, float lo, float hi) {
        if (hi <= lo) {
            return v >= hi ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** Nearest active storm phase, or 0 if none / far. */
    public static float nearestPhase() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                return 0.0F;
            }
            float best = 0.0F;
            double bestD = Double.MAX_VALUE;
            var pos = mc.player.position();
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                if (d.phase < 4.0F) {
                    continue;
                }
                double dx = d.dispX - pos.x;
                double dy = d.dispY - pos.y;
                double dz = d.dispZ - pos.z;
                double dd = dx * dx + dy * dy + dz * dz;
                if (dd < bestD) {
                    bestD = dd;
                    best = d.phase;
                }
            }
            // beyond this range the sky/fog is vanilla Story Mode calm again
            if (bestD > 1700.0 * 1700.0) {
                return 0.0F;
            }
            return best;
        } catch (Throwable t) {
            return 0.0F;
        }
    }

    public static float distanceInfluence() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) return 0.0F;
            double bestD = Double.MAX_VALUE;
            var pos = mc.player.position();
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                if (d.phase < 4.0F) continue;
                double dx = d.dispX - pos.x;
                double dy = d.dispY - pos.y;
                double dz = d.dispZ - pos.z;
                bestD = Math.min(bestD, dx * dx + dy * dy + dz * dz);
            }
            if (bestD == Double.MAX_VALUE) return 0.0F;
            double dist = Math.sqrt(bestD);
            return 1.0F - Mth.clamp((float)((dist - 900.0D) / 800.0D), 0.0F, 1.0F);
        } catch (Throwable t) {
            return 0.0F;
        }
    }

    /**
     * Write storm sky RGB into out[3] when storm owns the sky.
     * Returns blend 0..1 (0 = pure calm StoryModeSkyTint).
     */
    /** Compatibility colour for older fog callers; native SkyRenderer is authoritative. */
    public static float skyBlend(float[] out) {
        float phase = nearestPhase();
        if (phase < 4.90F || out == null || out.length < 3) {
            return 0.0F;
        }
        float[] p5 = {0x14 / 255.0F, 0x22 / 255.0F, 0x26 / 255.0F};
        float[] p55 = {0x10 / 255.0F, 0x06 / 255.0F, 0x19 / 255.0F};
        float[] p6 = {0x1A / 255.0F, 0x12 / 255.0F, 0x26 / 255.0F};
        float t55 = ramp(phase, 5.00F, 5.50F);
        float t6 = ramp(phase, 5.90F, 6.00F);
        out[0] = p5[0] + (p55[0] - p5[0]) * t55;
        out[1] = p5[1] + (p55[1] - p5[1]) * t55;
        out[2] = p5[2] + (p55[2] - p5[2]) * t55;
        out[0] += (p6[0] - out[0]) * t6;
        out[1] += (p6[1] - out[1]) * t6;
        out[2] += (p6[2] - out[2]) * t6;
        return Mth.clamp(0.80F * distanceInfluence(), 0.0F, 0.80F);
    }

    public static void tick() {
        // reserved — StoryModeSkyTint / fog mixins call skyBlend
    }
}
