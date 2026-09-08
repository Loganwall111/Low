package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.Minecraft;

/**
 * Drives model teeth/eye glow colours from the nearest storm phase so the
 * base-mod teethBoost pass matches the MCSM frames without Iris:
 *   4.x     light blue
 *   5.0     pure white
 *   5.1-5.4 white + slight blue
 *   5.5-5.9 glowing white
 *   6+      extremely dark blue
 */
public final class McsmTeethPhaseTint {

    private McsmTeethPhaseTint() {
    }

    public static void tick() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) {
                return;
            }
            float phase = 0.0F;
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                if (d.phase > phase) {
                    phase = d.phase;
                }
            }
            // Always drive colours. Phase 0-3.9 still gets light-blue teeth so
            // stage-0 / early never render as pure black mouths.
            float r, g, b, inten;
            if (phase >= 6.0F) {
                r = 0.12F; g = 0.28F; b = 0.70F; inten = 1.25F;  // dark blue (readable, not black)
            } else if (phase >= 5.5F) {
                r = 1.00F; g = 1.00F; b = 1.00F; inten = 1.55F;  // glowing white
            } else if (phase >= 5.1F) {
                r = 0.90F; g = 0.96F; b = 1.00F; inten = 1.35F;  // white + slight blue
            } else if (phase >= 5.0F) {
                r = 1.00F; g = 1.00F; b = 1.00F; inten = 1.40F;  // pure white
            } else {
                r = 0.70F; g = 0.88F; b = 1.00F; inten = 1.45F;  // phase 4 / early light blue
            }
            DabyWSClientConfig.eyeColorR = r;
            DabyWSClientConfig.eyeColorG = g;
            DabyWSClientConfig.eyeColorB = b;
            DabyWSClientConfig.turquoiseTeethIntensity = inten;
            DabyWSClientConfig.turquoiseTeeth = true;
        } catch (Throwable ignored) {
        }
    }
}
