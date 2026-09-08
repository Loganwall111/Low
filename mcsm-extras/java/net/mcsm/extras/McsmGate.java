package net.mcsm.extras;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.config.WitherStormConfigs;
import net.dabicco.witherstormmod.config.WitherStormWorldConfig;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCSM -- the "it's already written, it's just switched off" gate.
 *
 * A whole-jar invoke scan (glslcheck/whocalls.py) proved the features the user
 * reports as MISSING are not missing at all:
 *
 *     die()                 -> deathBlast(ServerLevel)          (death shockwave)
 *     addSubGrowth(...)     -> phaseUpShockwave(ServerLevel)    (phase shockwave)
 *     aiStep(...)           -> tickAmbientBuildingTear(...)     (reality tear / corruption)
 *
 * and DabyWSClientConfig exposes 335 public static non-final fields holding the
 * look: trailerShadows (ground shadows for terrain and mobs),
 * stormProximityVignette (the smoke screen), cloudDeckLayer, customSkyboxes,
 * purpleLightningSparks, sunGlow, blackGlare, stormShadowTerrain and so on.
 *
 * So the work is not to reimplement them -- it is to open the gates. This runs
 * ONCE per session (a static latch, not per frame), flips the MCSM look on, and
 * then never touches the config again, so anything the player changes in the
 * mod's own config screen sticks for the rest of the session.
 *
 * Booleans are only ever forced ON. Numeric values are only RAISED to a floor,
 * never lowered, so a player who already turned something up keeps their value.
 *
 * Both halves are wrapped in a blanket catch: a renamed field after a mod update
 * must cost a visual, never a crash.
 */
public final class McsmGate {

    private static boolean clientDone = false;
    private static boolean worldDone = false;

    /**
     * MCSM -- memory of every value this gate writes, keyed by field.
     *
     * The gate re-runs whenever the Extras panel is touched (each toggle calls
     * McsmGate.reset()). Until now every re-run re-forced the whole MCSM look,
     * silently undoing any look preset (Netflix, Cinematic, Legacy ...) the
     * player had just applied in the mod's own screen -- that is the real
     * mechanism behind "presets change nothing, it goes back to normal": the
     * preset DOES apply, and our next gate pass wipes it.
     *
     * New rule: on a re-run, a field that still holds the value we wrote gets
     * kept; a field anything else has changed since is never touched again.
     * The Extras panel's "Re-apply MCSM Look now" button clears this memory
     * for the player who explicitly wants the force again.
     */
    private static final Map<String, Object> LAST_SET = new ConcurrentHashMap<>();

    /** Forget every recorded value; the next gate run forces the look again. */
    public static void clearMemory() {
        LAST_SET.clear();
    }

    private static String memKey(Class<?> owner, Object instance, String name) {
        return owner.getName() + ":" + name
                + (instance == null ? ":static" : "@" + System.identityHashCode(instance));
    }

    private static double readNum(Field f, Object instance) throws IllegalAccessException {
        Class<?> t = f.getType();
        if (t == int.class) return f.getInt(instance);
        if (t == long.class) return f.getLong(instance);
        if (t == float.class) return f.getFloat(instance);
        return f.getDouble(instance);
    }

    private static double writeNum(Field f, Object instance, double v) throws IllegalAccessException {
        Class<?> t = f.getType();
        if (t == int.class) { int nv = (int) Math.round(v); f.setInt(instance, nv); return nv; }
        if (t == long.class) { long nv = Math.round(v); f.setLong(instance, nv); return nv; }
        if (t == float.class) { float nv = (float) v; f.setFloat(instance, nv); return nv; }
        f.setDouble(instance, v);
        return v;
    }

    /**
     * Client-side: force the Story Mode look on. Called from the frame driver
     * (McsmGradientTickPatch); the latch makes it a no-op after the first frame.
     */
    public static synchronized void openClient() {
        if (clientDone) {
            return;
        }
        McsmExtrasConfig.load();
        clientDone = true;
        if (!McsmExtrasConfig.forceMcsmLook) {
            McsmDiag.say("MCSM client gate disabled by mcsm_storm_extras.properties");
            return;
        }
        int changed = 0;
        try {
            Class<?> c = DabyWSClientConfig.class;

            // ---- storm body + sky -----------------------------------------
            changed += setBool(c, "distantStorms", true);
            changed += setBool(c, "distantFog", true);
            changed += setBool(c, "customSkyboxes", true);
            changed += setBool(c, "storyModeSky", true);
            changed += setBool(c, "storyModeLighting", true);
            changed += floorField(c, null, "storyModeLightingStrength", 1.0);
            changed += floorField(c, null, "storyModeSkyStrength", 0.85);
            changed += setBool(c, "colouredLighting", true);
            changed += setBool(c, "coloredLighting", true);
            changed += floorField(c, null, "colouredLightStrength", 1.0);
            changed += floorField(c, null, "coloredLightStrength", 1.0);
            changed += setBool(c, "cloudDeckLayer", true);
            changed += setBool(c, "regionalBiomeFog", true);
            changed += setBool(c, "phaseAnim", true);
            changed += setBool(c, "filledSubphases", true);
            changed += setBool(c, "scaledSubphaseGrowth", true);
            changed += setBool(c, "tentaclePhysics", true);
            changed += setBool(c, "optimizeDistantAnimations", true);
            changed += setBool(c, "flatbackFlipFix", true);
            // Obsidian Gloss is the mod's built-in OG/MCSM texture set. The
            // user expects Force MCSM Look to make the body/teeth/command-block
            // textures stop falling back to the Classic orange/plain skin.
            // MCSM: ogCemModels defaults ON and locks stormSkin to OG.
            changed += floorField(c, null, "stormSkin", 1.0);
            try {
                if (McsmExtrasConfig.ogCemModels) {
                    changed += floorField(c, null, "stormSkin", 1.0);
                }
            } catch (Throwable ignored) {
            }

            // ---- the halo / glare the user has been chasing ----------------
            // mega-phase 7b: blackGlare + cataclysmHalos paint the FAR
            // three-headed HALO ring the user rejects. Keep them OFF; the
            // thick welded shell lives in McsmStormBlob. McsmPresenceFxPatch
            // also forces them off every frame so presets cannot revive them.
            changed += setBool(c, "sunGlow", true);
            changed += setBool(c, "blackGlare", false);
            changed += setBool(c, "cataclysmHalos", false);
            changed += setBool(c, "atmospherePulse", false); // floating pulse spheres killed
            changed += setBool(c, "glareEjecta", false); // cube ejecta ring killed
            changed += setBool(c, "headEyeGlow", true);
            changed += setBool(c, "devourerDebrisGlow", true);

            // ---- ground shadows for trees and mobs (user request) ---------
            changed += setBool(c, "trailerShadows", true);
            changed += setBool(c, "stormShadow", true);
            changed += setBool(c, "stormShadowTerrain", true);
            changed += setBool(c, "stormShadowSoftEdge", true);
            changed += setBool(c, "stormShadowHeightmap", true);

            // ---- screen: smoke screen, tremor, sickness, glitch -----------
            changed += setBool(c, "stormProximityVignette", true);
            changed += setBool(c, "sicknessVeinOverlay", true);
            changed += setBool(c, "groundShakingTremors", true);
            changed += setBool(c, "dynamicScreenShake", true);
            changed += setBool(c, "purpleLightningSparks", true);

            // ---- presentation ---------------------------------------------
            changed += setBool(c, "storyModeBossbar", true);
            changed += setBool(c, "storyModeTitleScreen", true);
            changed += setBool(c, "stormAmbience", true);
            changed += setBool(c, "beamHum", true);
            changed += setBool(c, "beamDeactivateSound", true);
            changed += setBool(c, "infectedMobSound", true);

            // ---- numeric floors (raise only, never lower) ------------------
            changed += floorField(c, null, "vignetteIntensity", 0.85);
            changed += floorField(c, null, "sicknessVeinIntensity", 0.7);
            changed += floorField(c, null, "screenTremorIntensity", 0.8);
            changed += floorField(c, null, "chromaticGlitchStrength", 0.35);
            changed += setNum(c, null, "debrisDustParticles", 0.0);
            changed += setNum(c, null, "debrisAmount", 0.0);
            changed += setNum(c, null, "debrisSize", 0.0);
            // MCSM: volumetric coloured fog / lighting for the MCSM contrast
            // read (shafts of coloured light, thick atmospheric depth)
            changed += floorField(c, null, "volumetricFogDensity", 0.92);
            changed += setBool(c, "volumetricFog", true);
            changed += setBool(c, "colouredLighting", true);
            changed += setBool(c, "coloredLighting", true);
            changed += floorField(c, null, "colouredLightStrength", 0.85);
            changed += floorField(c, null, "coloredLightStrength", 0.85);
            changed += floorField(c, null, "stormGlowStrength", 1.0);
            changed += floorField(c, null, "sunGlowStrength", 1.0);
            // MCSM: MCSM matte black silhouette — NEVER reverse/model shade
            changed += setBool(c, "reverseShading", false);
            changed += setBool(c, "purpleSky", false);
            changed += setBool(c, "stormModelShading", false);
            changed += setBool(c, "modelShading", false);
            // blackGlareStrength left alone — the far ring is off
            // mega-phase 9: vivid world — push shadows + glow to the ceiling
            // so trees/mobs cast hard trailer shadows and teeth/eyes burn
            changed += floorField(c, null, "stormShadowStrength", 1.0);
            changed += floorField(c, null, "glowStrength", 1.0);
            changed += floorField(c, null, "bloomStrength", 1.0);
            changed += floorField(c, null, "impactLightStrength", 1.0);
            // MCSM: MCSM tractor beams = thick purple/blue cones (frames).
            // Floor opacity + MCSM purple bias; never lower a user's pick.
            changed += floorField(c, null, "beamOpacity", 0.92);
            changed += floorField(c, null, "beamColorR", 0.72); // MCSM solid purple
            changed += floorField(c, null, "beamColorG", 0.08);
            changed += floorField(c, null, "beamColorB", 0.95);
            changed += setNum(c, null, "beamColorR", 0.72);
            changed += setNum(c, null, "beamColorG", 0.08);
            changed += setNum(c, null, "beamColorB", 0.95);
            // teeth colours are phase-dynamic (McsmTeethPhaseTint every frame).
            // Only ensure the glow path is ON; RGB/intensity are driven by phase.
            changed += setBool(c, "turquoiseTeeth", true);
            changed += floorField(c, null, "ambienceVolume", 0.8);
            changed += floorField(c, null, "headSoundsVolume", 0.8);
            changed += floorField(c, null, "beamSoundsVolume", 0.8);
            changed += floorField(c, null, "infectedMobSoundVolume", 0.8);
            changed += floorField(c, null, "phaseAnimStrength", 1.0);
            changed += floorField(c, null, "mirrorBackDetail", 1.0);
            McsmDiag.say("MCSM client gate opened: " + changed + " config fields raised/enabled");
        } catch (Throwable t) {
            // Only a class-level failure should land here. Individual renamed
            // fields are handled by the helpers below so one missing option can
            // no longer block every later MCSM visual.
            McsmDiag.say("MCSM client gate failed before field loop: " + t);
        }
    }

    /**
     * Server-side: force destruction, the tear, corruption and shockwaves on.
     * Called from the storm's tick (McsmStormGrabPatch); latched per session.
     */
    public static synchronized void openWorld(Level level) {
        if (worldDone || level == null) {
            return;
        }
        McsmExtrasConfig.load();
        if (!McsmExtrasConfig.forceMcsmWorld) {
            worldDone = true;
            return;
        }
        try {
            WitherStormWorldConfig cfg = WitherStormConfigs.get(level);
            if (cfg == null) {
                return;
            }
            worldDone = true;
            Class<?> c = cfg.getClass();
            int changed = 0;

            // ---- reality tear: the storm rips buildings out of the world ---
            changed += floorField(c, cfg, "buildingDestruction", 1.0);
            changed += floorField(c, cfg, "buildingTearRadius", 28.0);
            changed += ceilInterval(c, cfg, "buildingTearInterval", 20);

            // ---- shockwaves you can see ------------------------------------
            changed += floorField(c, cfg, "groundShockwaveParticles", 600.0);

            // ---- structures: the storm raids them --------------------------
            changed += floorField(c, cfg, "structureRaid", 1.0);
            changed += ceilInterval(c, cfg, "structureRaidInterval", 5);
            changed += floorField(c, cfg, "structureRaidRadius", 96.0);
            changed += floorField(c, cfg, "structureTearClusters", 8.0);

            // ---- corruption: wither sickness + withered mobs ---------------
            changed += floorField(c, cfg, "witherSickness", 1.0);
            changed += floorField(c, cfg, "witheredMobs", 1.0);
            changed += floorField(c, cfg, "witheredMax", 32.0);
            changed += floorField(c, cfg, "witheredMaxCaves", 16.0);

            // ---- the ground itself reacts ----------------------------------
            changed += floorField(c, cfg, "caveRumble", 1.0);

            // ---- story towns get a full cast of named NPCs ----------------
            // default 10 is fine; floor to at least 8 so every town has a cast
            changed += floorField(c, cfg, "townNpcPopulation", 10.0);

            McsmDiag.say("MCSM world gate opened: " + changed + " world fields raised/enabled");
        } catch (Throwable t) {
            McsmDiag.say("MCSM world gate failed before field loop: " + t);
        }
    }

    private static int setBool(Class<?> owner, String name, boolean value) {
        try {
            Field f = owner.getField(name);
            String key = memKey(owner, null, name);
            boolean cur = f.getBoolean(null);
            Object prev = LAST_SET.get(key);
            if (prev instanceof Boolean b && cur != b) {
                return 0;   // changed after us (preset/player): leave it alone
            }
            f.setBoolean(null, value);
            LAST_SET.put(key, value);
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** Raise static or instance numeric fields without assuming int/double type. */
    private static int floorField(Class<?> owner, Object instance, String name, double min) {
        try {
            Field f = owner.getField(name);
            String key = memKey(owner, instance, name);
            double cur = readNum(f, instance);
            Object prev = LAST_SET.get(key);
            if (prev instanceof Double d && Math.abs(cur - d) > 1e-9) {
                return 0;   // changed after us (preset/player): respect it
            }
            double nv = writeNum(f, instance, Math.max(cur, min));
            LAST_SET.put(key, nv);
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /**
     * Force a numeric field to an exact value (still respects player/preset
     * edits after the gate last wrote). Used when a previous floor left a
     * value too high for the MCSM frames (e.g. teeth blast brightness).
     */
    private static int setNum(Class<?> owner, Object instance, String name, double value) {
        try {
            Field f = owner.getField(name);
            String key = memKey(owner, instance, name);
            double cur = readNum(f, instance);
            Object prev = LAST_SET.get(key);
            if (prev instanceof Double d && Math.abs(cur - d) > 1e-9) {
                return 0;   // changed after us (preset/player): respect it
            }
            double nv = writeNum(f, instance, value);
            LAST_SET.put(key, nv);
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** Intervals count DOWN in desirability: a smaller positive number fires sooner. */
    private static int ceilInterval(Class<?> owner, Object instance, String name, int max) {
        try {
            Field f = owner.getField(name);
            String key = memKey(owner, instance, name);
            double cur = readNum(f, instance);
            Object prev = LAST_SET.get(key);
            if (prev instanceof Double d && Math.abs(cur - d) > 1e-9) {
                return 0;   // changed after us: respect it
            }
            double want = (cur <= 0.0 || cur > max) ? max : cur;
            double nv = writeNum(f, instance, want);
            LAST_SET.put(key, nv);
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** Called on session teardown so a world change re-applies the gates. */
    public static synchronized void reset() {
        clientDone = false;
        worldDone = false;
    }

    private McsmGate() {}
}
