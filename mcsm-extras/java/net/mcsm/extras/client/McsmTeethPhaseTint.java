package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.StormSkins;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.Minecraft;

/**
 * Drives model teeth/eye glow colours from the nearest storm phase so the
 * base-mod teethBoost pass matches the MCSM frames without Iris:
 *   phase 3          no teeth glow
 *   phase 4          small cool-white/cyan glow on the three heads
 *   phase 5          flat white teeth, no big glow
 *   phase 5.5        white teeth with glow
 *   phase 4.5-5.4    sea-green teeth channels (#00A877)
 *   phase 5.5+       neon-cyan teeth channels (#00F3FF)
 *   all late phases  purple eye lens with a full-bright bloom source
 */
public final class McsmTeethPhaseTint {

    private McsmTeethPhaseTint() {
    }

    /** Packed full-bright ARGB used by the exact native teeth hook. */
    public static int teethTintArgb() {
        // Use the renderer's phase hint as well as the distant-storm tracker;
        // local storms do not always publish a DistantStormData entry.
        double phase = Math.max(StormSkins.phaseHint(), McsmStormAtmosphere.nearestPhase());
        if (phase >= 5.5D) {
            return rgb(0.0F, 0.953F, 1.0F);   // #00F3FF
        }
        if (phase >= 4.5D) {
            return rgb(0.0F, 0.659F, 0.467F); // #00A877
        }
        return rgb((float) DabyWSClientConfig.eyeColorR,
                (float) DabyWSClientConfig.eyeColorG,
                (float) DabyWSClientConfig.eyeColorB);
    }

    /** Packed full-bright ARGB used by the exact native eye hook. */
    public static int eyeTintArgb() {
        // Dedicated eyes stay on RenderType.eyes; only the phase tint shifts
        // between the requested sea-green and neon-cyan decks.
        double phase = McsmStormAtmosphere.nearestPhase();
        // The eye lens is the small purple focal light; teeth remain on the
        // separate sea-green/neon-cyan palette below.
        return rgb(0.694F, 0.302F, 1.0F); // #B14DFF
    }

    private static int rgb(float r, float g, float b) {
        int ir = Math.max(0, Math.min(255, Math.round(r * 255.0F)));
        int ig = Math.max(0, Math.min(255, Math.round(g * 255.0F)));
        int ib = Math.max(0, Math.min(255, Math.round(b * 255.0F)));
        return 0xFF000000 | ir << 16 | ig << 8 | ib;
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
            if (phase >= 5.5F) {
                // Neon-cyan deck requested for the late cinematic pass.
                r = 0.00F; g = 0.953F; b = 1.00F; inten = 4.20F; glow = true; // #00F3FF
            } else if (phase >= 4.5F) {
                // Sea-green initialization through the slate transition.
                r = 0.00F; g = 0.659F; b = 0.467F; inten = 3.90F; glow = true; // #00A877
            } else if (phase >= 4.0F) {
                r = 0.82F; g = 1.00F; b = 0.96F; inten = 3.60F; glow = true;
            } else {
                r = 0.98F; g = 0.98F; b = 0.86F; inten = 0.0F; glow = false;
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
            if (phase >= 5.5F) {
                DabyWSClientConfig.beamColorR = 0.00F;
                DabyWSClientConfig.beamColorG = 0.953F;
                DabyWSClientConfig.beamColorB = 1.00F;
            } else if (phase >= 4.5F) {
                DabyWSClientConfig.beamColorR = 0.00F;
                DabyWSClientConfig.beamColorG = 0.659F;
                DabyWSClientConfig.beamColorB = 0.467F;
            }
        } catch (Throwable ignored) {
        }
    }
}
