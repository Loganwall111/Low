package net.mcsm.extras.client;

/**
 * Frame-local gate for the existing StormDebris renderer. Phase 9 uses the
 * renderer's already-authored outer debris entries instead of a second ring,
 * vortex, billboard, or black-cylinder model.
 */
public final class McsmPhase9DebrisState {
    private static volatile boolean active;

    private McsmPhase9DebrisState() {
    }

    public static void set(boolean value) {
        active = value;
    }

    public static boolean active() {
        return active;
    }
}
