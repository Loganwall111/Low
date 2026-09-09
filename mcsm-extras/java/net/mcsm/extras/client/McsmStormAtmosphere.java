package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.StoryModeSkyTint;
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
    public static float skyBlend(float[] out) {
        float p = nearestPhase();
        if (p < 4.9F) {
            return 0.0F;
        }
        // phase colour decks — sampled from user atmosphere strips
        float wTeal = ramp(p, 4.90F, 5.10F) * (1.0F - ramp(p, 5.25F, 5.40F));
        float wPurp = ramp(p, 5.20F, 5.38F) * (1.0F - ramp(p, 5.48F, 5.58F));
        float wPink = ramp(p, 5.48F, 5.60F) * (1.0F - ramp(p, 5.95F, 6.12F)); // 5.5-5.9 ONLY
        float wSix  = ramp(p, 5.95F, 6.20F);
        float tot = wTeal + wPurp + wPink + wSix;
        if (tot < 0.02F) {
            return 0.0F;
        }
        // zenith-ish colours (fog/sky carrier)
        float[] teal = {0.08F, 0.32F, 0.30F};
        float[] purp = {0.24F, 0.08F, 0.34F};
        float[] pink = {0.38F, 0.13F, 0.32F}; // 5.5 magenta-pink, but not a whole-night wash
        float[] six  = {0.18F, 0.17F, 0.20F}; // phase 6 is storm-grey with only a little purple
        out[0] = (teal[0] * wTeal + purp[0] * wPurp + pink[0] * wPink + six[0] * wSix) / tot;
        out[1] = (teal[1] * wTeal + purp[1] * wPurp + pink[1] * wPink + six[1] * wSix) / tot;
        out[2] = (teal[2] * wTeal + purp[2] * wPurp + pink[2] * wPink + six[2] * wSix) / tot;
        // presence scales with phase weight; 5.5 is strongest purple-pink, and
        // fades back to calm/vanilla Story Mode sky when the player gets far
        // away from the storm.
        float blend = Mth.clamp(tot, 0.0F, 1.0F) * distanceInfluence();
        // Keep purple/pink as storm atmosphere only; do not repaint the entire
        // normal night sky purple when the player is merely nearby.
        return blend * 0.46F;
    }

    public static void tick() {
        // reserved — StoryModeSkyTint / fog mixins call skyBlend
    }
}
