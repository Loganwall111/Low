package net.mcsm.extras;

/**
 * MCSM -- startup + runtime diagnostics for OUR patches.
 *
 * Every defect chased so far (invisible storm, dead shadows, missing glare
 * blob) had the same investigative problem: there was no way to tell from the
 * game whether our mixins had applied at all. A silently skipped injector and
 * a correctly applied injector whose feature is disabled downstream look
 * identical from the outside.
 *
 * The mod itself already prints useful lines, e.g.
 *     [dabywitherstormmod][shadow] off: ... a shader pack is active ...
 *     [dabywitherstormmod] storm shadow map FAILED, shadows off: ...
 *
 * We now print alongside them under [mcsm], so a single log grep answers
 * "did the patch load, and what did it decide?".
 *
 * Everything here is fail-safe: diagnostics must never break a frame.
 */
public final class McsmDiag {

    private static final String TAG = "[ds]";

    /** Rate-limit for per-frame reporters: only print when the value changes. */
    private static String lastGate = null;
    private static String lastCarrier = null;
    private static boolean bannerDone = false;

    private McsmDiag() {
    }

    public static void say(String msg) {
        try {
            System.out.println(TAG + " " + msg);
        } catch (Throwable ignored) {
            // never propagate
        }
    }

    /** Printed once, the first time any of our patches runs. */
    public static void banner() {
        if (bannerDone) {
            return;
        }
        bannerDone = true;
        // MCSM -- the version is NO LONGER a literal here. It was
        // hand-typed and drifted (the jar said MCSM, this banner said
        // MCSM, the config-screen header said MCSM), so there was no way
        // to tell from inside the game which build was actually loaded -- the
        // exact confusion that made every fix look like "Minecraft did not
        // recognise the jar". Single source of truth: McsmExtrasConfig
        // .BUILD_VERSION, which ci/build.sh syncs from ./VERSION before javac.
        say("Devouring Storms " + McsmExtrasConfig.BUILD_VERSION + " active. Patches:");
        say("  McsmShaderGatePatch      ShaderPackCompat.active() -> false");
        say("  McsmStormVisibilityPatch fogless()/reverseShading()/modelShading() -> false (matte black)");
        say("  McsmBlobCarrierPatch     invertible cloudEnd carrier + death band");
        say("  McsmGradientTickPatch    drives StormSkyGradient.update()");
        say("  McsmStormGrabPatch       die()/addSubGrowth() -> shockwaves");
        say("  McsmFxDriver             expanding rings, motes, dust, wire");
        say("Build " + McsmExtrasConfig.BUILD_VERSION
            + " -- if the mod list or the Extras header shows any OTHER number,");
        say("you are running an older jar: delete every other dabywitherstormmod*.jar from mods/.");
        say("Grep this log for: [ds]  [dabywitherstormmod][shadow]");
    }

    /**
     * Reports what the storm gradient is producing. This is the value the
     * glare blob depends on; if it never becomes active, the blob cannot draw.
     */
    public static void gradient(boolean active, float phase, float yaw, float pitch) {
        String s = active
            ? String.format("gradient ACTIVE phase=%.2f yaw=%.1f pitch=%.1f", phase, yaw, pitch)
            : "gradient inactive (no storm in range, or phase outside 4.42..8.06)";
        if (!s.equals(lastGate)) {
            lastGate = s;
            say(s);
        }
    }

    private static String lastSky = null;

    /**
     * Explains WHY the sky is or is not a storm sky.
     *
     * StormSkyGradient.update() only picks a storm when phase >= 4.5 and it is
     * within 1400 blocks. Below that the vanilla sky is CORRECT, not a failure
     * -- but from a screenshot the two are indistinguishable, which cost real
     * debugging time. This line removes the ambiguity.
     */
    public static void skyReason(boolean active, float phase) {
        String s;
        if (active) {
            s = String.format("storm sky ON (phase %.2f)", phase);
        } else if (phase > 0.0F && phase < 4.5F) {
            s = String.format("storm sky OFF -- phase %.2f is below the 4.5 "
                            + "threshold. Vanilla sky here is CORRECT; the dome "
                            + "starts at 4.5 and turquoise at 5.0.", phase);
        } else {
            s = "storm sky OFF -- no storm within 1400 blocks (or phase 0).";
        }
        if (!s.equals(lastSky)) {
            lastSky = s;
            say(s);
        }
    }

    private static String lastFeat = null;

    /**
     * One-shot inventory of the visual features and the exact reason each is
     * on or off. Phase 31/32 traced all of these through bytecode and found
     * every gate open, yet the user still reports missing teeth, eye glow and
     * heads. Static analysis has run out of road: this prints the LIVE values
     * so the log can say which of them the game actually believes.
     */
    public static void features(boolean teeth, boolean eyeGlow, boolean sunGlow,
                                boolean shadows, boolean bloom, boolean ogSkin,
                                double stormSkin) {
        String s = "features: teeth=" + teeth
                 + " eyeGlow=" + eyeGlow
                 + " sunGlow=" + sunGlow
                 + " shadows=" + shadows
                 + " bloom=" + bloom
                 + " ogSkin=" + ogSkin + " (stormSkin=" + stormSkin + ")";
        if (!s.equals(lastFeat)) {
            lastFeat = s;
            say(s);
            say("  if a feature reads TRUE here but you cannot see it, the gate is"
              + " open and the fault is in drawing, not configuration.");
        }
    }

    private static String lastDeath = null;
    private static String lastDeathPct = null;

    /**
     * Death-cinematic STATE CHANGES only: start, end, and which FogData field
     * ended up carrying the band. The sequence runs sixteen seconds at sixty
     * frames a second or more, so an unthrottled line here would bury the log
     * and hide the very evidence it is meant to provide.
     */
    public static void death(String msg) {
        if (msg == null || msg.equals(lastDeath)) {
            return;
        }
        lastDeath = msg;
        say("death cinematic: " + msg);
    }

    /**
     * Progress, in whole tens of a percent, so the log proves the sky band is
     * actually moving through 1906..2900 rather than being stamped once and
     * forgotten -- the difference between a working sequence and a single frame.
     */
    public static void deathProgress(double t) {
        int pct = ((int) (t * 10.0)) * 10;
        String s = pct + "% (FogSkyEnd band 1906..2900)";
        if (s.equals(lastDeathPct)) {
            return;
        }
        lastDeathPct = s;
        say("death cinematic: " + s);
    }

    /** Reports the encoded carrier value actually handed to the shader. */
    public static void carrier(float cloudEnd, int yawIdx, int pitchIdx) {
        String s = String.format("blob carrier cloudEnd=%.0f (yawIdx=%d pitchIdx=%d)",
                                 cloudEnd, yawIdx, pitchIdx);
        if (!s.equals(lastCarrier)) {
            lastCarrier = s;
            say(s);
        }
    }
}
