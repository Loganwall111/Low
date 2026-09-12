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
                // phase 7+: GREEN-WHITE glow (user 2026-09-11)
                r = 0.78F; g = 1.00F; b = 0.85F; inten = 4.20F; glow = true;
            } else if (phase >= 6.0F) {
                // phase 6: greenish-blue, MORE blue (user 2026-09-11)
                r = 0.50F; g = 0.85F; b = 1.00F; inten = 4.20F; glow = true;
            } else if (phase >= 5.5F) {
                r = 0.82F; g = 1.00F; b = 0.96F; inten = 4.00F; glow = true;   // phase 5.5: cyan-white
            } else if (phase >= 5.0F) {
                // 1.9.202 regression fix: glow=false hid the teeth overlay
                // entirely ("no glowing teeth"). Phase 5 must still RENDER —
                // completely white, with the aura around the glow (user
                // 2026-09-11: "During 5 they're meant to glow just white with
                // an aura around the glow").
                r = 1.00F; g = 1.00F; b = 1.00F; inten = 4.40F; glow = true;   // phase 5: the ONLY pure-white phase
            } else if (phase >= 4.0F) {
                r = 0.82F; g = 1.00F; b = 0.96F; inten = 3.60F; glow = true;   // phase 4: cyan-white
            } else {
                r = 0.98F; g = 0.98F; b = 0.86F; inten = 0.0F; glow = false;  // phase 3: no glowing teeth
            }
            DabyWSClientConfig.eyeColorR = r;
            DabyWSClientConfig.eyeColorG = g;
            DabyWSClientConfig.eyeColorB = b;
            DabyWSClientConfig.turquoiseTeethIntensity = inten;
            DabyWSClientConfig.turquoiseTeeth = glow;
            if (phase >= 5.0F) {
                // The native head renderer owns both eye lenses and the teeth
                // overlay. Keep both emissive submissions alive for the
                // phase-5 model even when a migrated config carried an old
                // zero glow setting; the render type is full-bright and bloom
                // remains fail-soft in the base renderer.
                DabyWSClientConfig.headEyeGlow = true;
                DabyWSClientConfig.glowStrength = Math.max(DabyWSClientConfig.glowStrength, 1.0);
            }

            // 1.9.217 -- beamColor tints the EYEBALL itself (WitherStormHeadRenderer.eyeTint).
            // The eyes must read neon PURPLE, the beam colour constraint, not the
            // teeth colours; day/night only nudges the brightness, never the hue.
            float day = 0.30F + 0.70F * (0.5F + 0.5F * (float)Math.sin(
                    (mc.level.getGameTime() % 24000L) / 24000.0D * Math.PI * 2.0D - Math.PI / 2.0D));
            float eyef = phase >= 7.0F ? 0.55F : (phase >= 6.0F ? 0.78F : (phase >= 5.5F ? 0.92F : 0.66F));
            DabyWSClientConfig.beamColorR = 0.62F * (0.55F + 0.45F * day) * eyef;
            DabyWSClientConfig.beamColorG = 0.26F * (0.55F + 0.45F * day) * eyef;
            DabyWSClientConfig.beamColorB = 1.00F * (0.62F + 0.38F * day);
        } catch (Throwable ignored) {
        }
    }
}
