package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.Minecraft;

/** Exact phase palette for native RenderType.eyes() teeth and eye layers. */
public final class McsmTeethPhaseTint {
    private static final int CYAN = 0xFF00F3FF;
    private static final int SEA_GREEN = 0xFF00A877;
    private static final int PURPLE_EYE = 0xFFB45CFF;

    private McsmTeethPhaseTint() {
    }

    public static void tick() {
        try {
            McsmStormAtmosphere.tick();
            float phase = 0.0F;
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                for (ClientDistantStormManager.StormData storm : ClientDistantStormManager.all()) {
                    phase = Math.max(phase, storm.phase);
                }
            }
            net.dabicco.witherstormmod.client.StormSkins.setPhaseHint(phase);
            // The old phase-4/5 presentation mixed a green backdrop into the
            // body/world path. Restore that colour only as a local Phase
            // 4.45-5.0 atmosphere; never allow it to tint the storm body or
            // remain active once the Phase 5 purple deck begins.
            boolean greenBackdrop = phase >= 4.45F && phase < 5.0F;
            DabyWSClientConfig.stormBackdropTurquoise = greenBackdrop;
            DabyWSClientConfig.phaseSky45Enabled = greenBackdrop;
            DabyWSClientConfig.phaseSky50Enabled = false;
            DabyWSClientConfig.phaseFogPalettes = false;
            int color = phase >= 6.0F ? SEA_GREEN : CYAN;
            if (phase < 4.0F) {
                color = 0xFFFFFFFF;
            }
            DabyWSClientConfig.eyeColorR = ((color >>> 16) & 0xFF) / 255.0D;
            DabyWSClientConfig.eyeColorG = ((color >>> 8) & 0xFF) / 255.0D;
            DabyWSClientConfig.eyeColorB = (color & 0xFF) / 255.0D;
            // Beam/eye channels stay purple even when the teeth use cyan or
            // sea-green, so the middle eyes remain the luminous focal point.
            DabyWSClientConfig.beamColorR = ((PURPLE_EYE >>> 16) & 0xFF) / 255.0D;
            DabyWSClientConfig.beamColorG = ((PURPLE_EYE >>> 8) & 0xFF) / 255.0D;
            DabyWSClientConfig.beamColorB = (PURPLE_EYE & 0xFF) / 255.0D;
            DabyWSClientConfig.turquoiseTeethIntensity = phase >= 4.0F ? 1.0D : 0.0D;
            DabyWSClientConfig.turquoiseTeeth = phase >= 4.0F;
            if (phase >= 5.0F) {
                DabyWSClientConfig.headEyeGlow = true;
                DabyWSClientConfig.glowStrength = Math.max(DabyWSClientConfig.glowStrength, 1.0D);
            }
        } catch (Throwable ignored) {
        }
    }

    public static int teethTintArgb() {
        return currentColor();
    }

    /** Eyes are the purple emissive focal point; teeth retain the phase palette. */
    public static int eyeTintArgb() {
        double phase = net.dabicco.witherstormmod.client.StormSkins.phaseHint();
        return phase >= 4.0D ? PURPLE_EYE : 0xFFFFFFFF;
    }

    private static int currentColor() {
        double phase = net.dabicco.witherstormmod.client.StormSkins.phaseHint();
        return phase >= 6.0D ? SEA_GREEN : (phase >= 4.0D ? CYAN : 0xFFFFFFFF);
    }
}
