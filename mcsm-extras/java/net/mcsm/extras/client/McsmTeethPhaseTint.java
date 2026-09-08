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
            if (phase < 0.5F) {
                return;
            }
            float r, g, b, inten;
            if (phase >= 6.0F) {
                r = 0.08F; g = 0.16F; b = 0.38F; inten = 1.0F;   // extremely dark blue
            } else if (phase >= 5.5F) {
                r = 0.98F; g = 0.99F; b = 1.00F; inten = 1.35F;  // glowing white
            } else if (phase >= 5.1F) {
                r = 0.92F; g = 0.96F; b = 1.00F; inten = 1.15F;  // white + slight blue
            } else if (phase >= 5.0F) {
                r = 1.00F; g = 1.00F; b = 1.00F; inten = 1.20F;  // pure white
            } else {
                r = 0.76F; g = 0.88F; b = 1.00F; inten = 1.10F;  // phase 4 light blue
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
