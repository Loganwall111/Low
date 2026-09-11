package net.mcsm.extras;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.config.WitherStormConfigs;
import net.dabicco.witherstormmod.config.WitherStormWorldConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCSM 1.9.100 -- the "it's already written, it's just switched off" gate.
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
     * MCSM 1.9.112 -- memory of every value this gate writes, keyed by field.
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

    // ---------------------------------------------------------------------
    // 1.9.209 -- "some settings are not activating when I click on them".
    //
    // The base mod persists its screen into config/dabywitherstormmod-client.json
    // and every gate run was stomping those choices back at session start, so a
    // clicked setting looked dead. Now the gate reads that file once and NEVER
    // forces any key the player has explicitly persisted to a different value.
    // Fresh installs (no file yet) still get the full MCSM default.
    // ---------------------------------------------------------------------
    private static Map<String, Double> persisted = null;

    private static Map<String, Double> persistedOverrides() {
        if (persisted != null) {
            return persisted;
        }
        persisted = new HashMap<>();
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gameDirectory == null) {
                return persisted;
            }
            Path p = mc.gameDirectory.toPath().resolve("config").resolve("dabywitherstormmod-client.json");
            if (!Files.exists(p)) {
                return persisted;
            }
            String text = Files.readString(p);
            String[] keys = { "tentaclePhysics", "glareEjecta", "devourerDebrisGlow",
                    "stormShadowHeightmap", "bloomStrength", "sunGlowStrength",
                    "blackGlareStrength", "debrisAmount", "debrisDustParticles",
                    "stormShadow", "sunGlow", "blackGlare", "headEyeGlow" };
            for (String k : keys) {
                Matcher m = Pattern.compile("\"" + k + "\"\\s*:\\s*(true|false|-?[0-9.]+)").matcher(text);
                if (m.find()) {
                    String v = m.group(1);
                    persisted.put(k, "true".equals(v) ? 1.0 : "false".equals(v) ? 0.0 : Double.parseDouble(v));
                }
            }
        } catch (Throwable ignored) {
            // a moved config dir must cost the respect pass, never the gate
        }
        return persisted;
    }

    /** True when the player has persisted their own value for this key. */
    private static boolean playerOwns(String name, double ourValue) {
        Double v = persistedOverrides().get(name);
        return v != null && Math.abs(v - ourValue) > 1e-9;
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
        // 1.9.208: the vanilla look is permanently disabled -- there is no
        // "regular" presentation to fall back to any more. The MCSM gate
        // always opens.
        McsmExtrasConfig.forceMcsmLook = true;
        McsmExtrasConfig.shaderPackGate = true;
        int changed = 0;
        try {
            Class<?> c = DabyWSClientConfig.class;

            // ---- storm body + sky -----------------------------------------
            changed += setBool(c, "distantStorms", true);
            changed += setBool(c, "distantFog", true);
            changed += setBool(c, "customSkyboxes", true);
            changed += setBool(c, "cloudDeckLayer", true);
            changed += setBool(c, "regionalBiomeFog", true);
            changed += setBool(c, "phaseAnim", true);
            changed += setBool(c, "filledSubphases", true);
            changed += setBool(c, "scaledSubphaseGrowth", true);
            // simulated tentacles look wrong per user feedback; off by default
            changed += setBool(c, "tentaclePhysics", false);
            // 1.9.208: the shader is the default. Everything the mod draws
            // (sun glow, shadow map, teeth/eye glow) ports over the Iris
            // program list via the ShaderPackCompat gate above.
            changed += setBool(c, "optimizeDistantAnimations", true);
            changed += setBool(c, "flatbackFlipFix", true);
            // Obsidian Gloss is the mod's built-in OG/MCSM texture set. The
            // user expects Force MCSM Look to make the body/teeth/command-block
            // textures stop falling back to the Classic orange/plain skin.
            changed += floorField(c, null, "stormSkin", 1.0);

            // ---- 1.9.204: Story Mode neon-purple tractor beams ---------------
            // Solid violet (0.55, 0.15, 1.0), near-opaque, so the eyes read as a
            // powerful radiating light instead of a faint blue haze.
            changed += floorField(c, null, "beamColorR", 0.55);
            changed += ceilingField(c, null, "beamColorG", 0.15);
            changed += floorField(c, null, "beamColorB", 1.0);
            changed += floorField(c, null, "beamOpacity", 1.35);

            // ---- the halo / glare the user has been chasing ----------------
            // 1.9.212: the fake sun-glow card is off (the shader draws its
            // own sun); the ORIGINAL oval Catalyst Halo is back on.
            changed += setBool(c, "sunGlow", false);
            changed += setBool(c, "blackGlare", true);
            changed += setBool(c, "glareEjecta", true);
            changed += setBool(c, "cataclysmHalos", true);
            changed += setBool(c, "atmospherePulse", true);
            changed += setBool(c, "headEyeGlow", true);
            changed += setBool(c, "devourerDebrisGlow", true);

            // ---- ground shadows for trees and mobs (user request) ---------
            changed += setBool(c, "trailerShadows", true);
            changed += setBool(c, "stormShadow", true);
            changed += setBool(c, "stormShadowTerrain", true);
            changed += setBool(c, "stormShadowSoftEdge", true);
            changed += setBool(c, "stormShadowHeightmap", false);

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
            changed += ceilingField(c, null, "debrisDustParticles", 0.0);
            changed += ceilingField(c, null, "debrisAmount", 0.0);
            changed += floorField(c, null, "volumetricFogDensity", 0.6);
            changed += floorField(c, null, "stormGlowStrength", 1.0);
            changed += ceilingField(c, null, "sunGlowStrength", 0.0);
            changed += ceilingField(c, null, "blackGlareStrength", 0.65);
            changed += floorField(c, null, "stormShadowStrength", 1.0);
            changed += floorField(c, null, "glowStrength", 1.0);
            // Full-res HDR storm bloom is the native-memory pressure point in
            // the user's Iris/Sodium logs. Teeth/eyes stay emissive cyan through
            // their render pass, but the expensive full-screen bloom buffer is
            // off by default for stability.
            changed += ceilingField(c, null, "bloomStrength", 0.0);
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
            if (playerOwns(name, value ? 1.0 : 0.0)) {
                return 0;   // 1.9.209: persisted player choice wins
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
            if (playerOwns(name, Math.max(cur, min))) {
                return 0;   // 1.9.209: persisted player choice wins
            }
            double nv = writeNum(f, instance, Math.max(cur, min));
            LAST_SET.put(key, nv);
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** Intervals count DOWN in desirability: a smaller positive number fires sooner. */
    private static int ceilingField(Class<?> owner, Object instance, String name, double max) {
        try {
            Field f = owner.getField(name);
            String key = memKey(owner, instance, name);
            double cur = readNum(f, instance);
            Object prev = LAST_SET.get(key);
            if (prev instanceof Double d && Math.abs(cur - d) > 1e-9) {
                return 0;
            }
            if (playerOwns(name, Math.min(cur, max))) {
                return 0;   // 1.9.209: persisted player choice wins
            }
            double nv = writeNum(f, instance, Math.min(cur, max));
            LAST_SET.put(key, nv);
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }

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
