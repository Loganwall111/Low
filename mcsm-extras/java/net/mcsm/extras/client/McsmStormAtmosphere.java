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
        // phase colour decks — sampled from the user's uploaded gradient set:
        // 5 turquoise, 5.5 pink/purple/orange, 5.9 purple-blue-pink, 6 brown-pink/black.
        float wTeal = ramp(p, 4.90F, 5.10F) * (1.0F - ramp(p, 5.25F, 5.40F));
        float wPurp = ramp(p, 5.20F, 5.42F) * (1.0F - ramp(p, 5.48F, 5.60F));
        float wPink = ramp(p, 5.48F, 5.65F) * (1.0F - ramp(p, 5.78F, 5.94F));
        float wLate = ramp(p, 5.78F, 5.92F) * (1.0F - ramp(p, 5.96F, 6.10F));
        float wSix  = ramp(p, 5.95F, 6.20F) * (1.0F - ramp(p, 7.90F, 8.10F));
        // 1.9.208: phase 8-9 -- the whole sky goes dark-orange/ember.
        float w89   = ramp(p, 7.95F, 8.20F);
        float tot = wTeal + wPurp + wPink + wLate + wSix + w89;
        if (tot < 0.02F) {
            return 0.0F;
        }
        // 1.9.221 (port) -- decks re-keyed to the 2026-09-11 hex palettes:
        //   5   #1A2223 / #2E4544 / #7C9885 (turquoise emergence)
        //   5.5 #3A1B54 / #5E2775 / #7D4B91 (deep purple & pink corruption)
        //   6   #A36B73 / #D69776                (four-color sunset split)
        float[] teal = {0.10F, 0.27F, 0.27F};
        float[] purp = {0.23F, 0.11F, 0.33F};
        float[] pink = {0.37F, 0.15F, 0.46F};
        float[] late = {0.49F, 0.29F, 0.57F};
        float[] six  = {0.64F, 0.42F, 0.45F};
        float[] e89  = {0.62F, 0.30F, 0.13F};
        out[0] = (teal[0] * wTeal + purp[0] * wPurp + pink[0] * wPink + late[0] * wLate + six[0] * wSix + e89[0] * w89) / tot;
        out[1] = (teal[1] * wTeal + purp[1] * wPurp + pink[1] * wPink + late[1] * wLate + six[1] * wSix + e89[1] * w89) / tot;
        out[2] = (teal[2] * wTeal + purp[2] * wPurp + pink[2] * wPink + late[2] * wLate + six[2] * wSix + e89[2] * w89) / tot;
        // presence scales with phase weight; 5.5 is strongest purple-pink, and
        // fades back to calm/vanilla Story Mode sky when the player gets far
        // away from the storm.
        float blend = Mth.clamp(tot, 0.0F, 1.0F) * distanceInfluence();
        // Keep purple/pink as storm atmosphere only; do not repaint the entire
        // normal night sky purple when the player is merely nearby. Phase 8-9
        // commits harder -- the ember sky owns the horizon.
        return blend * (0.46F + 0.34F * w89 / Math.max(0.001F, tot));
    }

    public static void tick() {
        // reserved — StoryModeSkyTint / fog mixins call skyBlend
    }
}
