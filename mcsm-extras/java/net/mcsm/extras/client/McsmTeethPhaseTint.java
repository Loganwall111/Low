package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.Minecraft;

/**
 * Drives model teeth/eye glow colours from the nearest storm phase so the
 * base-mod teethBoost pass matches the MCSM frames without Iris:
 *   phase 3          no teeth glow
 *   phase 4          small cool-white/cyan glow on the three heads
 *   phase 5          flat white teeth, no big glow
 *   phase 5.5        white teeth with glow
 *   phase 6          blue/cyan glowing teeth after the split
 *   phase 7+         green-blue glowing teeth
 */
public final class McsmTeethPhaseTint {

    private McsmTeethPhaseTint() {
    }

    public static void tick() {
        try {
            net.mcsm.extras.client.McsmStormAtmosphere.tick();
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
            net.dabicco.witherstormmod.client.StormSkins.setPhaseHint(phase);
            if (phase < 0.5F) {
                return;
            }
            float r, g, b, inten;
            boolean glow;
            if (phase >= 7.0F) {
                r = 0.44F; g = 1.00F; b = 0.92F; inten = 2.35F; glow = true;   // bright green-blue late storm
            } else if (phase >= 6.0F) {
                r = 0.22F; g = 0.66F; b = 1.00F; inten = 2.45F; glow = true;   // blue glowing split teeth
            } else if (phase >= 5.5F) {
                r = 0.45F; g = 0.72F; b = 1.00F; inten = 2.20F; glow = true;   // 5.5+: brighter bluish glow (was too dark to read)
            } else if (phase >= 5.0F) {
                // 1.9.202 regression fix: glow=false hid the teeth overlay
                // entirely ("no glowing teeth"). Phase 5 must still RENDER —
                // completely white, with the overlay switched on.
                r = 1.00F; g = 1.00F; b = 1.00F; inten = 1.60F; glow = true;   // phase 5: completely white teeth, visible
            } else if (phase >= 4.0F) {
                r = 0.72F; g = 0.98F; b = 1.00F; inten = 1.25F; glow = true;   // slight phase-4 cyan-white glow
            } else {
                r = 0.98F; g = 0.98F; b = 0.86F; inten = 0.0F; glow = false;  // phase 3: no glowing teeth
            }
            DabyWSClientConfig.eyeColorR = r;
            DabyWSClientConfig.eyeColorG = g;
            DabyWSClientConfig.eyeColorB = b;
            DabyWSClientConfig.turquoiseTeethIntensity = inten;
            DabyWSClientConfig.turquoiseTeeth = glow;

            // Tractor beams should follow the same sky/storm family instead of
            // staying solid pink-purple all day. Keep them soft but phase-aware.
            float day = 0.55F + 0.45F * (float)Math.sin((mc.level.getGameTime() % 24000L) / 24000.0D * Math.PI * 2.0D);
            float br = phase >= 7.0F ? 0.42F : (phase >= 6.0F ? 0.30F : (phase >= 5.5F ? 0.78F : 0.62F));
            float bg = phase >= 7.0F ? 0.96F : (phase >= 6.0F ? 0.64F : (phase >= 5.5F ? 0.52F : 0.30F));
            float bb = phase >= 7.0F ? 0.86F : (phase >= 6.0F ? 1.00F : (phase >= 5.5F ? 1.00F : 0.95F));
            DabyWSClientConfig.beamColorR = br * (0.82F + 0.18F * day);
            DabyWSClientConfig.beamColorG = bg * (0.82F + 0.18F * day);
            DabyWSClientConfig.beamColorB = bb;
        } catch (Throwable ignored) {
        }
    }
}
