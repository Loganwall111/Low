package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.Minecraft;

/** Exact phase palette for native RenderType.eyes() teeth and eye layers. */
public final class McsmTeethPhaseTint {
    private static final int CYAN = 0xFF00F3FF;
    private static final int SEA_GREEN = 0xFF00A877;

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
            // The old phase-4/5 presentation left a green backdrop/fog layout
            // enabled in the client config. It was a world/body colour filter,
            // not the requested sea-green Phase 6+ teeth colour. Keep the
            // native purple/ember layers available, but permanently close the
            // legacy green controls so the storm body cannot be re-hued green.
            DabyWSClientConfig.stormBackdropTurquoise = false;
            DabyWSClientConfig.phaseSky45Enabled = false;
            DabyWSClientConfig.phaseSky50Enabled = false;
            DabyWSClientConfig.phaseFogPalettes = false;
            int color = phase >= 6.0F ? SEA_GREEN : CYAN;
            if (phase < 4.0F) {
                color = 0xFFFFFFFF;
            }
            DabyWSClientConfig.eyeColorR = ((color >>> 16) & 0xFF) / 255.0D;
            DabyWSClientConfig.eyeColorG = ((color >>> 8) & 0xFF) / 255.0D;
            DabyWSClientConfig.eyeColorB = (color & 0xFF) / 255.0D;
            DabyWSClientConfig.beamColorR = DabyWSClientConfig.eyeColorR;
            DabyWSClientConfig.beamColorG = DabyWSClientConfig.eyeColorG;
            DabyWSClientConfig.beamColorB = DabyWSClientConfig.eyeColorB;
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

    public static int eyeTintArgb() {
        return currentColor();
    }

    private static int currentColor() {
        double phase = net.dabicco.witherstormmod.client.StormSkins.phaseHint();
        return phase >= 6.0D ? SEA_GREEN : (phase >= 4.0D ? CYAN : 0xFFFFFFFF);
    }
}
