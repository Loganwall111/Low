package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.Mth;

/**
 * Native storm atmosphere state for Minecraft's SkyRenderer.
 *
 * This class deliberately has no texture, skybox, mesh, or buffer submission
 * path.  The vanilla SkyRenderer owns the sky pass; this class only supplies
 * its render state and the matching fog colour.  The vanilla disc is spherical
 * around the camera, so its vertical coordinate is a view-angle coordinate,
 * not a world-space dome or a billboard.
 *
 * The two tracking values are kept here as the Java-side equivalents of the
 * documented carriers used by the old shader path: u_StormPhase and
 * u_TimeOfDay.  They are updated during SkyRenderer state extraction and are
 * also available to fog/glare code without requiring a shader pack.
 */
public final class McsmNativeSkyRenderer {
    private static final float MAX_STORM_OPACITY = 0.80F;

    private static volatile float u_StormPhase;
    private static volatile float u_TimeOfDay;
    private static volatile float u_StormOpacity;

    private McsmNativeSkyRenderer() {
    }

    /** Apply a continuously interpolated atmosphere to the native sky state. */
    public static void apply(ClientLevel level, float partialTick, SkyRenderState state) {
        if (level == null || state == null) {
            return;
        }

        long clock = level.getOverworldClockTime();
        float time = state.timeOfDay;
        if (!(time >= 0.0F && time <= 1.0F)) {
            time = (float) Math.floorMod(clock, 24000L) / 24000.0F;
        }
        float phase = nearestPhase();
        float distance = McsmStormAtmosphere.distanceInfluence();
        float storm = stormOpacity(phase, distance);

        u_StormPhase = phase;
        u_TimeOfDay = time;
        u_StormOpacity = storm;

        // Keep vanilla completely untouched outside the active storm.  This is
        // important for dimensions and for the pre-phase-5 story atmosphere.
        if (storm <= 0.0001F) {
            return;
        }

        float[] top = new float[3];
        float[] mid = new float[3];
        float[] horizon = new float[3];
        calmGradient(time, top, mid, horizon);

        float[] stormTop = new float[3];
        float[] stormMid = new float[3];
        float[] stormHorizon = new float[3];
        stormGradient(phase, stormTop, stormMid, stormHorizon);
        lerp(top, stormTop, storm, top);
        lerp(mid, stormMid, storm, mid);
        lerp(horizon, stormHorizon, storm, horizon);

        // SkyRenderState exposes the endpoints consumed by the native
        // spherical disc.  Keep the middle stop alongside the tracking state
        // so fog and any future native sky-pass expansion use the same ramp;
        // the endpoint assignment is intentionally ARGB, never a texture.
        state.skyColor = rgb(top, state.skyColor);
        state.sunriseAndSunsetColor = rgb(horizon, state.sunriseAndSunsetColor);
        state.starBrightness *= 1.0F - celestialSuppression(storm);
        state.rainBrightness = Mth.lerp(storm * 0.35F, state.rainBrightness, 0.0F);
        state.shouldRenderDarkDisc = false;
        state.isSunriseOrSunset = false;
    }

    /** True when the storm is strong enough to own all celestial rendering. */
    public static boolean suppressCelestials() {
        return u_StormOpacity >= 0.64F;
    }

    public static float stormPhase() {
        return u_StormPhase;
    }

    public static float timeOfDay() {
        return u_TimeOfDay;
    }

    public static float stormOpacity() {
        return u_StormOpacity;
    }

    /**
     * Fog colour at the horizon.  FogRenderer calls this after vanilla has
     * computed its native colour, so terrain, foliage, and entities fade into
     * the same atmosphere rather than into a second horizontal strip.
     */
    public static float fogColor(ClientLevel level, float[] out) {
        if (out == null || out.length < 3) {
            return 0.0F;
        }
        float time = level == null ? u_TimeOfDay :
                (float) Math.floorMod(level.getOverworldClockTime(), 24000L) / 24000.0F;
        float phase = nearestPhase();
        float opacity = stormOpacity(phase, McsmStormAtmosphere.distanceInfluence());
        if (opacity <= 0.0001F) {
            return 0.0F;
        }

        float[] calmTop = new float[3];
        float[] calmMid = new float[3];
        float[] calmHorizon = new float[3];
        calmGradient(time, calmTop, calmMid, calmHorizon);
        float[] stormTop = new float[3];
        float[] stormMid = new float[3];
        float[] stormHorizon = new float[3];
        stormGradient(phase, stormTop, stormMid, stormHorizon);
        lerp(calmHorizon, stormHorizon, opacity, out);

        // Phase 5.5–5.9 has a distinct atmospheric ground-fog blend.  It is
        // blended, not hard-switched, so the native fog model remains seam-free.
        float purpleFog = smoothstep(5.45F, 5.62F, phase)
                * (1.0F - smoothstep(5.88F, 6.02F, phase));
        float[] ground = rgb(0x8E, 0x5D, 0xA3);
        lerp(out, ground, purpleFog * opacity * 0.72F, out);
        return opacity;
    }

    /** Convert a spherical view direction to the continuous vertical ramp. */
    public static void screenSpaceColor(float viewY, float[] out) {
        if (out == null || out.length < 3) {
            return;
        }
        float y = Mth.clamp(viewY, -1.0F, 1.0F);
        // asin maps the camera ray to elevation; this avoids a planar/horizon
        // seam and is the coordinate used by the native spherical sky pass.
        float vertical = (float) (Math.asin(y) / Math.PI + 0.5D);
        float phase = u_StormPhase;
        float time = u_TimeOfDay;
        float[] top = new float[3];
        float[] mid = new float[3];
        float[] horizon = new float[3];
        calmGradient(time, top, mid, horizon);
        float[] st = new float[3];
        float[] sm = new float[3];
        float[] sl = new float[3];
        float[] sh = new float[3];
        stormGradient(phase, st, sm, sh);
        stormLower(phase, sl);
        float opacity = u_StormOpacity;
        lerp(top, st, opacity, top);
        lerp(mid, sm, opacity, mid);
        lerp(horizon, sh, opacity, horizon);
        lerp(mid, sl, opacity, sl);
        // Four stops are used for the phase 6/7 sunset while the native
        // endpoint state remains compatible with Minecraft's SkyRenderer.
        if (vertical < 0.333333F) {
            lerp(horizon, sl, vertical * 3.0F, out);
        } else if (vertical < 0.666667F) {
            lerp(sl, mid, (vertical - 0.333333F) * 3.0F, out);
        } else {
            lerp(mid, top, (vertical - 0.666667F) * 3.0F, out);
        }
    }

    private static float nearestPhase() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                return 0.0F;
            }
            double bestDistance = Double.MAX_VALUE;
            float bestPhase = 0.0F;
            for (ClientDistantStormManager.StormData storm : ClientDistantStormManager.all()) {
                if (storm.phase < 4.90F) {
                    continue;
                }
                double dx = storm.dispX - mc.player.getX();
                double dy = storm.dispY - mc.player.getY();
                double dz = storm.dispZ - mc.player.getZ();
                double distance = dx * dx + dy * dy + dz * dz;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestPhase = storm.phase;
                }
            }
            return bestDistance <= 1700.0D * 1700.0D ? bestPhase : 0.0F;
        } catch (Throwable ignored) {
            return 0.0F;
        }
    }

    private static float stormOpacity(float phase, float distance) {
        if (phase < 4.90F || distance <= 0.0F) {
            return 0.0F;
        }
        float phasePresence = smoothstep(4.90F, 5.00F, phase);
        return MAX_STORM_OPACITY * phasePresence * Mth.clamp(distance, 0.0F, 1.0F);
    }

    private static float celestialSuppression(float opacity) {
        return Mth.clamp(opacity / MAX_STORM_OPACITY, 0.0F, 1.0F);
    }

    private static void calmGradient(float time, float[] top, float[] mid, float[] horizon) {
        float[] dayTop = rgb(0x10, 0x21, 0x4C);
        float[] dayMid = rgb(0x1E, 0x46, 0xA8);
        float[] dayHorizon = rgb(0x2A, 0x5D, 0xEF);
        float[] duskTop = rgb(0x75, 0x79, 0xEA);
        float[] duskMid = rgb(0xA3, 0xA6, 0xFF);
        float[] duskHorizon = rgb(0xCD, 0xCE, 0xFF);
        float dawn = smoothstep(0.00F, 0.10F, time);
        dawn *= 1.0F - smoothstep(0.10F, 0.18F, time);
        float dusk = smoothstep(0.46F, 0.52F, time);
        dusk *= 1.0F - smoothstep(0.58F, 0.66F, time);
        float twilight = Mth.clamp(Math.max(dawn, dusk), 0.0F, 1.0F);
        lerp(dayTop, duskTop, twilight, top);
        lerp(dayMid, duskMid, twilight, mid);
        lerp(dayHorizon, duskHorizon, twilight, horizon);
    }

    private static void stormGradient(float phase, float[] top, float[] mid, float[] horizon) {
        float[] p5Top = rgb(0x14, 0x22, 0x26);
        float[] p5Mid = rgb(0x20, 0x3A, 0x3C);
        float[] p5Horizon = rgb(0x62, 0x82, 0x6F);
        float[] p55Top = rgb(0x10, 0x06, 0x19);
        float[] p55Mid = rgb(0x33, 0x1A, 0x47);
        float[] p55Horizon = rgb(0x5D, 0x2A, 0x72);
        float[] p6Top = rgb(0x1A, 0x12, 0x26);
        float[] p6Upper = rgb(0x3F, 0x26, 0x4A);
        float[] p6Horizon = rgb(0xCC, 0x82, 0x60);

        if (phase < 5.50F) {
            lerp(p5Top, p55Top, smoothstep(5.00F, 5.50F, phase), top);
            lerp(p5Mid, p55Mid, smoothstep(5.00F, 5.50F, phase), mid);
            lerp(p5Horizon, p55Horizon, smoothstep(5.00F, 5.50F, phase), horizon);
        } else if (phase < 5.90F) {
            top[0] = p55Top[0]; top[1] = p55Top[1]; top[2] = p55Top[2];
            mid[0] = p55Mid[0]; mid[1] = p55Mid[1]; mid[2] = p55Mid[2];
            horizon[0] = p55Horizon[0]; horizon[1] = p55Horizon[1]; horizon[2] = p55Horizon[2];
        } else {
            float t = smoothstep(5.90F, 6.00F, phase);
            lerp(p55Top, p6Top, t, top);
            lerp(p55Mid, p6Upper, t, mid);
            lerp(p55Horizon, p6Horizon, t, horizon);
        }
    }

    private static void stormLower(float phase, float[] lower) {
        if (phase < 5.90F) {
            float[] p55Mid = rgb(0x33, 0x1A, 0x47);
            lower[0] = p55Mid[0];
            lower[1] = p55Mid[1];
            lower[2] = p55Mid[2];
            return;
        }
        float[] p55Mid = rgb(0x33, 0x1A, 0x47);
        float[] p6Lower = rgb(0x99, 0x5F, 0x6B);
        float t = smoothstep(5.90F, 6.00F, phase);
        lower[0] = p55Mid[0] + (p6Lower[0] - p55Mid[0]) * t;
        lower[1] = p55Mid[1] + (p6Lower[1] - p55Mid[1]) * t;
        lower[2] = p55Mid[2] + (p6Lower[2] - p55Mid[2]) * t;
    }

    private static float smoothstep(float lo, float hi, float value) {
        if (hi <= lo) {
            return value >= hi ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((value - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static void lerp(float[] a, float[] b, float amount, float[] out) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        out[0] = a[0] + (b[0] - a[0]) * t;
        out[1] = a[1] + (b[1] - a[1]) * t;
        out[2] = a[2] + (b[2] - a[2]) * t;
    }

    private static float[] rgb(int r, int g, int b) {
        return new float[]{r / 255.0F, g / 255.0F, b / 255.0F};
    }

    private static int rgb(float[] color, int previous) {
        int alpha = previous & 0xFF000000;
        int r = Mth.clamp(Math.round(color[0] * 255.0F), 0, 255);
        int g = Mth.clamp(Math.round(color[1] * 255.0F), 0, 255);
        int b = Mth.clamp(Math.round(color[2] * 255.0F), 0, 255);
        return alpha | r << 16 | g << 8 | b;
    }
}
