package net.mcsm.extras.client;

import net.mcsm.extras.McsmExtrasConfig;

/**
 * Small state gate for the optional Story Mode joke sun.
 *
 * The visible slab is deliberately rendered by the native SkyRenderer mixin,
 * not by a world entity or a block.  That keeps the normal entity pass in
 * front of it and avoids allocating a second persistent GPU mesh.  A
 * render-only object also has no safe vanilla block raycast target, so this
 * class intentionally does not consume attack clicks or create a physical
 * world object.
 */
public final class McsmStoryModeSunSlab {
    private static volatile boolean deleted = false;

    private McsmStoryModeSunSlab() {
    }

    /** Config gate; the vanilla celestial pass is untouched when this is false. */
    public static boolean configured() {
        try {
            McsmExtrasConfig.load();
            return McsmExtrasConfig.storyModeAccurateSunSun
                    || McsmExtrasConfig.ENABLE_EXPERIMENTAL_STORY_MODE_STAGE;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** True when this feature owns the native celestial pass for the frame. */
    public static boolean ownsCelestials() {
        return configured()
                && (McsmNativeSkyRenderer.suppressCelestials()
                    || McsmExtrasConfig.ENABLE_EXPERIMENTAL_STORY_MODE_STAGE);
    }

    /** Feature is visible while the native storm or stage celestial pass is active. */
    public static boolean enabled() {
        return ownsCelestials() && !deleted;
    }

    /**
     * Reserved for a future screen-space click target.  It is intentionally
     * not wired to Minecraft's block attack path: doing that would make an
     * invisible render-only quad delete arbitrary blocks on a miss.
     */
    public static void deleteRenderOnlySlab() {
        deleted = true;
    }

    public static void resetForNewWorld() {
        deleted = false;
    }
}
