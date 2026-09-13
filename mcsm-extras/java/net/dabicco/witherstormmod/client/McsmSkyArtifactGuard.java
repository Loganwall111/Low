package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;

/**
 * Single owner for the retired camera-wide sky layers.
 *
 * The attached oval submitted by McsmAtmosphericMeshComponent is deliberately
 * not touched here.  These switches are only the old screen vignette, sun
 * bloom, distant backdrop, and cloud-deck routes that could cover the native
 * sky with a second top band.
 */
public final class McsmSkyArtifactGuard {
    private McsmSkyArtifactGuard() {
    }

    /** Disable every retired full-screen/world-wide sky cover before rendering. */
    public static void disableExtraSkyLayers() {
        DabyWSClientConfig.stormProximityVignette = false;
        DabyWSClientConfig.vignetteIntensity = 0.0D;
        DabyWSClientConfig.sunGlow = false;
        DabyWSClientConfig.sunGlowStrength = 0.0D;
        DabyWSClientConfig.stormBackdropQuad = false;
        DabyWSClientConfig.stormCloudDeck = 0.0D;
        DabyWSClientConfig.cloudDeckLayer = false;
    }

    /** True only while an in-range storm needs the continuous storm sky. */
    public static boolean stormSkyActive() {
        try {
            return McsmStormAtmosphere.nearestPhase() >= 4.45F
                    && McsmStormAtmosphere.distanceInfluence() > 0.0F;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
