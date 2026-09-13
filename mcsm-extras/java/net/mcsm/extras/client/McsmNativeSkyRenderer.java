package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.McsmSkyArtifactGuard;
import net.dabicco.witherstormmod.client.StoryModeSkyTint;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.SkyRenderState;

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

        // Keep the ordinary native sky renderer as the only geometry path.
        // During a storm, however, the raw native time-of-day colour can
        // reintroduce the orange upper band. Blend its colour toward the
        // lower storm atmosphere instead of adding another sky card; this
        // keeps the native/main sky path while removing that top strip.
        float[] stormSky = new float[3];
        float stormBlend = McsmStormAtmosphere.skyBlend(stormSky);
        if (stormBlend > 0.01F) {
            float amount = Math.min(1.0F, stormBlend * 1.35F);
            int sr = Math.round(stormSky[0] * 255.0F);
            int sg = Math.round(stormSky[1] * 255.0F);
            int sb = Math.round(stormSky[2] * 255.0F);
            int ar = (authoritative >> 16) & 0xFF;
            int ag = (authoritative >> 8) & 0xFF;
            int ab = authoritative & 0xFF;
            int r = Math.round(ar + (sr - ar) * amount);
            int g = Math.round(ag + (sg - ag) * amount);
            int b = Math.round(ab + (sb - ab) * amount);
            authoritative = 0xFF000000 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
        }
        state.skyColor = authoritative;
        // The sunrise/sunset fan is the separate upper band; make it
        // transparent and cancel its geometry submission as well.
        state.sunriseAndSunsetColor = 0;
        state.shouldRenderDarkDisc = false;
        ownsSky = true;
    }

    public static boolean ownsSky() {
        return ownsSky;
    }
}
