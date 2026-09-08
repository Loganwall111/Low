package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Tractor beams adapt colour to weather + phase (user frames):
 *   clear / normal  → blue-purple MCSM beam
 *   rain / thunder  → pink-magenta beam
 *   phase 5.5+ rain → hot pink
 *   phase 6+ clear  → deeper blue
 */
public final class McsmBeamWeatherTint {

    private McsmBeamWeatherTint() {
    }

    public static void tick() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) {
                return;
            }
            ClientLevel level = mc.level;
            boolean raining = level.isRaining();
            boolean thunder = level.isThundering();
            float phase = 0.0F;
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                if (d.phase > phase) {
                    phase = d.phase;
                }
            }

            float r, g, b, op;
            if (raining || thunder) {
                // hot pink / magenta rain beams (user MCSM rain frame: bright white-pink cores)
                if (phase >= 5.5F) {
                    r = 1.00F; g = 0.45F; b = 0.95F; op = 0.98F;
                } else {
                    r = 0.98F; g = 0.40F; b = 0.92F; op = 0.96F;
                }
            } else if (phase >= 6.0F) {
                r = 0.35F; g = 0.30F; b = 1.00F; op = 0.94F; // deep blue-violet phase 6
            } else if (phase >= 5.5F) {
                r = 0.55F; g = 0.20F; b = 0.98F; op = 0.94F; // purple-blue 5.5
            } else {
                r = 0.30F; g = 0.40F; b = 1.00F; op = 0.92F; // normal blue
            }
            DabyWSClientConfig.beamColorR = r;
            DabyWSClientConfig.beamColorG = g;
            DabyWSClientConfig.beamColorB = b;
            DabyWSClientConfig.beamOpacity = op;
        } catch (Throwable ignored) {
        }
    }
}
