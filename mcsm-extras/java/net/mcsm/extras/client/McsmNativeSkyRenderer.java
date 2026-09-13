package net.mcsm.extras.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.dabicco.witherstormmod.client.McsmSkyArtifactGuard;
import net.dabicco.witherstormmod.client.StoryModeSkyTint;
import net.mcsm.extras.McsmExtrasConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps Minecraft's native sky pass as the sole sky renderer.
 *
 * The old sky implementation supplied a second upper layer and left the
 * native renderer's zenith endpoint in place.  That is what produced the
 * clipped black daytime strip and the warm nighttime strip at the top of the
 * view.  This hook does not submit geometry or install a texture: it keeps the
 * colour already computed for the current native sky and removes the separate
 * sunrise/sunset fan. The lower/current native colour is therefore the only
 * authority for every sky pixel, including the extreme top of the spherical
 * pass.
 */
public final class McsmNativeSkyRenderer {
    private static volatile boolean ownsSky;

    private McsmNativeSkyRenderer() {
    }

    /** Apply one continuous colour to the native sky while a storm is present. */
    public static void apply(ClientLevel level, SkyRenderState state) {
        ownsSky = false;
        McsmSkyArtifactGuard.disableExtraSkyLayers();
        if (level == null || state == null || !McsmSkyArtifactGuard.stormSkyActive()) {
            return;
        }

        // SkyRenderState.skyColor is the live colour selected by Minecraft for
        // the current time/biome.  Prefer it over a second palette so the
        // lower, already-correct sky remains authoritative.
        int authoritative = state.skyColor;
        if ((authoritative & 0x00FFFFFF) == 0) {
            authoritative = state.sunriseAndSunsetColor;
        }
        if ((authoritative & 0x00FFFFFF) == 0) {
            float[] horizon = new float[3];
            StoryModeSkyTint.horizonColor(level.getOverworldClockTime(), horizon);
            authoritative = 0xFF000000
                    | (Math.round(horizon[0] * 255.0F) & 0xFF) << 16
                    | (Math.round(horizon[1] * 255.0F) & 0xFF) << 8
                    | (Math.round(horizon[2] * 255.0F) & 0xFF);
        }

        // Keep the ordinary native sky as the only colour/geometry path. The
        // sunrise/sunset fan is the separate upper band; make it transparent
        // and cancel its geometry submission as well. The attached lower deck
        // remains green/teal without washing the entire sky green.
        state.skyColor = authoritative;
        state.sunriseAndSunsetColor = 0;
        state.shouldRenderDarkDisc = false;
        ownsSky = true;
    }

    /** Return the storm's native horizon colour and its distance blend. */
    public static float fogColor(ClientLevel level, float[] out) {
        if (level == null || out == null || out.length < 3 || !McsmSkyArtifactGuard.stormSkyActive()) {
            return 0.0F;
        }
        StoryModeSkyTint.horizonColor(level.getOverworldClockTime(), out);
        return Math.min(0.80F, Math.max(0.0F,
                0.80F * McsmStormAtmosphere.distanceInfluence()));
    }

    public static boolean ownsSky() {
        return ownsSky;
    }

    /**
     * Compatibility gate for the optional sun-slab feature. The current native
     * sky path intentionally keeps ordinary sun/moon/stars, so this is only
     * true when the opt-in accurate-sun setting explicitly owns the frame.
     */
    public static boolean suppressCelestials() {
        return ownsSky && McsmExtrasConfig.storyModeAccurateSunSun;
    }

    /** True when the opt-in studio stage has put the camera outside its dome. */
    public static boolean stageOutside() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || !McsmExperimentalStoryStage.active()) {
                return false;
            }
            Field rendererField = Minecraft.class.getDeclaredField("gameRenderer");
            rendererField.setAccessible(true);
            Object renderer = rendererField.get(mc);
            if (renderer == null) {
                return false;
            }
            Method cameraMethod = renderer.getClass().getMethod("getMainCamera");
            Object camera = cameraMethod.invoke(renderer);
            if (camera == null) {
                return false;
            }
            Method positionMethod = camera.getClass().getMethod("getPosition");
            Object position = positionMethod.invoke(camera);
            return position instanceof Vec3
                    && McsmExperimentalStoryStage.cameraOutside(mc.level, (Vec3) position);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
