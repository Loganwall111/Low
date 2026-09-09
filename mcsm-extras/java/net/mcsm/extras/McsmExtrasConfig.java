package net.mcsm.extras;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * MCSM extras config. Lives beside the mod's own config (their in-game
 * config screen + /dabyws still drive the mod itself; this file only drives
 * the MCSM additions: tentacle grab cadence, beacon storm path, rise fx,
 * visual parameters, custom shaders, animations, and NPC systems).
 * Written with defaults on first launch.
 */
public final class McsmExtrasConfig {
    public static final String BUILD_VERSION = "1.9.192";
    public static boolean enableTentacleGrab = true;
    public static double  grabIntervalSeconds = 11.0;
    public static boolean enableBeaconStorm = true;
    public static double  beaconCooldownSeconds = 30.0;
    public static boolean enableRiseFx = true;
    public static boolean spiralCounterClockwise = true;
    public static boolean enableBeaconBlock = true;

    // ---- fields that ALSO existed in the 1.9.88-1.9.95 jars ----------------
    /** Use Tainted's original 103-part CEM model for the phase-5 body. */
    public static boolean ogCemModels = false;
    /** Apparent size of the smudge/halo quads behind the storm (0.5 = new). */
    public static double  smudgeScale = 0.5;

    // ---- MCSM 1.9.98 batch (phase 29/30 user orders, 2026-09-04) ----------
    /** Storm glare mass scale; read by the blob carrier every frame. */
    public static double  glareSize = 0.58;
    /** Mod-side aurora borealis at night (cold-biome biased). */
    public static boolean auroraEnabled = true;
    /** Full death cinematic: distortion -> white cracks -> implosion flash ->
     *  supernova rings -> segments. */
    public static boolean deathCinematic = true;
    /** Expanding ring shockwaves on phase 4 rise, phase 7 rise, and death. */
    public static boolean supernovaRings = true;
    /** Skull impacts: grey ground smoke + yellow electric sparks + crackle. */
    public static boolean smokeScreen = true;
    /** Phase 5.5+: purple lightning strikes + purple motes in the sky. */
    public static boolean purpleSky = true;
    /** Dust trails when the storm sweeps blocks. */
    public static boolean dustWaves = true;
    /** Post-death reality tear with the black aurora + corruption spread. */
    public static boolean realityTear = true;
    /** Command block obliterate flash erases entities incl. players. */
    public static boolean obliterateFlash = true;
    /** Prank variant: also kicks players. Default OFF (grief-safe). */
    public static boolean obliterateKick = false;

    // ---- MCSM 1.9.100 batch: the gates ------------------------------------
    /** Force the client's Story Mode look on (shadows, glare, smoke screen,
     *  skyboxes, vignette, tremor). */
    public static boolean forceMcsmLook = true;
    /** Force the world config on: building tear, corruption, shockwave
     *  particles, structure raids, withered mobs, cave rumble. */
    public static boolean forceMcsmWorld = true;

    public static boolean shaderPackGate = true;
    /** Taut glowing wire from the storm's core down to its ground anchor. */
    public static boolean commandWire = true;
    /** Brief a player the first time they get close to a live storm. */
    public static boolean mcsmInstructions = true;

    // ---- Atmosphere & Custom Graphics Parameters --------------------------
    /** Night sky opacity: 0.0 = clear, 1.0 = deepest navy. */
    public static double nightSkyOpacity = 1.0;
    /** Phase 5.5 transition point: exact phase where purple-pink horizon begins. */
    public static double phase55Threshold = 5.5;
    /** Salmon-pink intensity in phase 5.5-5.9 range. */
    public static double phase5_9PinkIntensity = 1.0;
    /** Cloud alpha: 0.0 = transparent, 1.0 = normal. */
    public static double cloudAlpha = 1.0;
    /** Cloud speed multiplier: 1.0 = normal. */
    public static double cloudSpeed = 1.0;
    /** Rain/snow intensity multiplier. */
    public static double precipitationIntensity = 1.0;
    /** Storm glare animation phase (0 = static, 1 = flowing). */
    public static double glareAnimPhase = 0.0;
    /** Storm body animation pulse: phase-1 eye/jaw throb rhythm. */
    public static double bodyAnimPulse = 0.0;
    /** Glare animation intensity multiplier. */
    public static double glareAnimIntensity = 0.0;
    /** Non-euclidean glare: stays fixed relative to storm center. */
    public static boolean glareNonEuclidean = false;

    // ---- Advanced Atmospheric VFX & Lighting ------------------------------
    /** Beacon luminous glow corona. */
    public static boolean beaconGlow = true;
    /** Nether and End portal luminous casted light. */
    public static boolean portalLights = true;
    /** Biome-specific subtle mist/fog palettes. */
    public static boolean biomeAtmospherics = true;
    /** Nether deep crimson fog and rising lava sparks. */
    public static boolean netherRedFog = true;
    /** Snow/ice biome gigantic blue sky band. */
    public static boolean snowSkyBand = true;
    /** Multi-colored Aurora Borealis ribbons (blue/pink/purple/orange). */
    public static boolean auroraRibbons = true;
    /** Twinkling multi-colored stars. */
    public static boolean twinklingStars = true;
    /** Night sky comets / shooting stars. */
    public static boolean comets = true;
    /** Magical colored sparkles (white/pink/purple). */
    public static boolean coloredSparkles = true;
    /** Underwater god rays and hazy dark blue depth. */
    public static boolean waterGodRays = true;
    /** End sky purple spiral vortex and reality rip. */
    public static boolean endSkyVortex = true;
    /** Screen-space shadows & lighting contrast. */
    public static boolean globalShadows = true;
    /** Enhanced aggressive Wither Storm AI & targeting. */
    public static boolean witherStormEnhancedAi = true;
    /** NPC natural walking and speaking animations. */
    public static boolean npcWalkAnimations = true;
    /** Early Wither Storm MCSM sway/tilt and quick summon look-down snap. */
    public static boolean stormBodySway = true;
    /** Endless rear cube/back growth is experimental and grief-heavy; default OFF. */
    public static boolean infiniteBackGrowth = false;
    /** Growth speed multiplier for the optional infinite back growth. */
    public static double infiniteBackGrowthSpeed = 0.10;

    // ---- Iris Shader Pack -------------------------------------------------
    /** Ship + auto-install the Devouring Storms Iris shader pack. */
    public static boolean embeddedShaderPack = true;

    private static boolean loaded = false;
    private static long stamp = -1L;

    private static File file() {
        return new File(new File(System.getProperty("user.dir", "."), "config"), "mcsm_storm_extras.properties");
    }

    /** Write current values back (used by the config-screen rows). */
    public static synchronized void save() {
        try {
            File f = file();
            f.getParentFile().mkdirs();
            Properties p = new Properties();
            p.setProperty("config_version", BUILD_VERSION);
            p.setProperty("enable_tentacle_grab", String.valueOf(enableTentacleGrab));
            p.setProperty("grab_interval_seconds", String.valueOf(grabIntervalSeconds));
            p.setProperty("enable_beacon_storm", String.valueOf(enableBeaconStorm));
            p.setProperty("beacon_cooldown_seconds", String.valueOf(beaconCooldownSeconds));
            p.setProperty("night_sky_opacity", String.valueOf(nightSkyOpacity));
            p.setProperty("phase_55_threshold", String.valueOf(phase55Threshold));
            p.setProperty("phase_5_9_pink_intensity", String.valueOf(phase5_9PinkIntensity));
            p.setProperty("glare_anim_phase", String.valueOf(glareAnimPhase));
            p.setProperty("body_anim_pulse", String.valueOf(bodyAnimPulse));
            p.setProperty("glare_anim_intensity", String.valueOf(glareAnimIntensity));
            p.setProperty("glare_non_euclidean", String.valueOf(glareNonEuclidean));
            p.setProperty("cloud_alpha", String.valueOf(cloudAlpha));
            p.setProperty("cloud_speed", String.valueOf(cloudSpeed));
            p.setProperty("precipitation_intensity", String.valueOf(precipitationIntensity));
            p.setProperty("enable_rise_fx", String.valueOf(enableRiseFx));
            p.setProperty("spiral_counter_clockwise", String.valueOf(spiralCounterClockwise));
            p.setProperty("enable_beacon_block", String.valueOf(enableBeaconBlock));
            p.setProperty("og_cem_models", String.valueOf(ogCemModels));
            p.setProperty("smudge_scale", String.valueOf(smudgeScale));
            p.setProperty("glare_size", String.valueOf(glareSize));
            p.setProperty("aurora_enabled", String.valueOf(auroraEnabled));
            p.setProperty("shader_pack_gate", String.valueOf(shaderPackGate));
            p.setProperty("death_cinematic", String.valueOf(deathCinematic));
            p.setProperty("supernova_rings", String.valueOf(supernovaRings));
            p.setProperty("smoke_screen", String.valueOf(smokeScreen));
            p.setProperty("purple_sky", String.valueOf(purpleSky));
            p.setProperty("dust_waves", String.valueOf(dustWaves));
            p.setProperty("reality_tear", String.valueOf(realityTear));
            p.setProperty("obliterate_flash", String.valueOf(obliterateFlash));
            p.setProperty("obliterate_kick", String.valueOf(obliterateKick));
            p.setProperty("force_mcsm_look", String.valueOf(forceMcsmLook));
            p.setProperty("force_mcsm_world", String.valueOf(forceMcsmWorld));
            p.setProperty("command_wire", String.valueOf(commandWire));
            p.setProperty("mcsm_instructions", String.valueOf(mcsmInstructions));
            p.setProperty("beacon_glow", String.valueOf(beaconGlow));
            p.setProperty("portal_lights", String.valueOf(portalLights));
            p.setProperty("biome_atmospherics", String.valueOf(biomeAtmospherics));
            p.setProperty("nether_red_fog", String.valueOf(netherRedFog));
            p.setProperty("snow_sky_band", String.valueOf(snowSkyBand));
            p.setProperty("aurora_ribbons", String.valueOf(auroraRibbons));
            p.setProperty("twinkling_stars", String.valueOf(twinklingStars));
            p.setProperty("comets", String.valueOf(comets));
            p.setProperty("colored_sparkles", String.valueOf(coloredSparkles));
            p.setProperty("water_god_rays", String.valueOf(waterGodRays));
            p.setProperty("end_sky_vortex", String.valueOf(endSkyVortex));
            p.setProperty("global_shadows", String.valueOf(globalShadows));
            p.setProperty("wither_storm_enhanced_ai", String.valueOf(witherStormEnhancedAi));
            p.setProperty("npc_walk_animations", String.valueOf(npcWalkAnimations));
            p.setProperty("storm_body_sway", String.valueOf(stormBodySway));
            p.setProperty("infinite_back_growth", String.valueOf(infiniteBackGrowth));
            p.setProperty("infinite_back_growth_speed", String.valueOf(infiniteBackGrowthSpeed));
            p.setProperty("embedded_shader_pack", String.valueOf(embeddedShaderPack));
            try (OutputStream out = new FileOutputStream(f)) {
                p.store(out, "MCSM - storm gameplay patches + visuals + gates. config_version below is the build that wrote this file.");
            }
            stamp = f.lastModified();
        } catch (Throwable t) {
            // ignore; file stays as-is
        }
    }

    /** Cheap stat per call; reloads whenever the file is edited in game. */
    public static synchronized void load() {
        File f = file();
        if (loaded) {
            long m = f.lastModified();
            if (m == stamp) return;
            stamp = m;
        } else {
            loaded = true; stamp = f.lastModified();
        }
        try {
            Properties p = new Properties();
            if (f.isFile()) {
                try (InputStream in = new FileInputStream(f)) { p.load(in); }
            } else {
                save();
                return;
            }
            enableTentacleGrab = bool(p, "enable_tentacle_grab", enableTentacleGrab);
            grabIntervalSeconds = dbl(p, "grab_interval_seconds", grabIntervalSeconds);
            enableBeaconStorm  = bool(p, "enable_beacon_storm", enableBeaconStorm);
            beaconCooldownSeconds = dbl(p, "beacon_cooldown_seconds", beaconCooldownSeconds);
            enableRiseFx       = bool(p, "enable_rise_fx", enableRiseFx);
            spiralCounterClockwise = bool(p, "spiral_counter_clockwise", spiralCounterClockwise);
            enableBeaconBlock = bool(p, "enable_beacon_block", enableBeaconBlock);
            ogCemModels        = bool(p, "og_cem_models", ogCemModels);
            smudgeScale        = dbl(p, "smudge_scale", smudgeScale);
            glareSize          = dbl(p, "glare_size", glareSize);
            String cv = p.getProperty("config_version");
            if ((cv == null || !BUILD_VERSION.equals(cv.trim())) && Math.abs(glareSize - 1.18) < 0.001) {
                glareSize = 0.58;
            }
            nightSkyOpacity = dbl(p, "night_sky_opacity", nightSkyOpacity);
            phase55Threshold = dbl(p, "phase_55_threshold", phase55Threshold);
            phase5_9PinkIntensity = dbl(p, "phase_5_9_pink_intensity", phase5_9PinkIntensity);
            glareAnimPhase = dbl(p, "glare_anim_phase", glareAnimPhase);
            bodyAnimPulse = dbl(p, "body_anim_pulse", bodyAnimPulse);
            glareAnimIntensity = dbl(p, "glare_anim_intensity", glareAnimIntensity);
            glareNonEuclidean = bool(p, "glare_non_euclidean", glareNonEuclidean);
            cloudAlpha = dbl(p, "cloud_alpha", cloudAlpha);
            cloudSpeed = dbl(p, "cloud_speed", cloudSpeed);
            precipitationIntensity = dbl(p, "precipitation_intensity", precipitationIntensity);
            auroraEnabled      = bool(p, "aurora_enabled", auroraEnabled);
            shaderPackGate     = bool(p, "shader_pack_gate", shaderPackGate);
            deathCinematic     = bool(p, "death_cinematic", deathCinematic);
            supernovaRings     = bool(p, "supernova_rings", supernovaRings);
            smokeScreen        = bool(p, "smoke_screen", smokeScreen);
            purpleSky          = bool(p, "purple_sky", purpleSky);
            dustWaves          = bool(p, "dust_waves", dustWaves);
            realityTear        = bool(p, "reality_tear", realityTear);
            obliterateFlash    = bool(p, "obliterate_flash", obliterateFlash);
            obliterateKick     = bool(p, "obliterate_kick", obliterateKick);
            forceMcsmLook      = bool(p, "force_mcsm_look", forceMcsmLook);
            forceMcsmWorld     = bool(p, "force_mcsm_world", forceMcsmWorld);
            commandWire        = bool(p, "command_wire", commandWire);
            mcsmInstructions   = bool(p, "mcsm_instructions", mcsmInstructions);
            beaconGlow         = bool(p, "beacon_glow", beaconGlow);
            portalLights       = bool(p, "portal_lights", portalLights);
            biomeAtmospherics  = bool(p, "biome_atmospherics", biomeAtmospherics);
            netherRedFog       = bool(p, "nether_red_fog", netherRedFog);
            snowSkyBand        = bool(p, "snow_sky_band", snowSkyBand);
            auroraRibbons      = bool(p, "aurora_ribbons", auroraRibbons);
            twinklingStars     = bool(p, "twinkling_stars", twinklingStars);
            comets             = bool(p, "comets", comets);
            coloredSparkles    = bool(p, "colored_sparkles", coloredSparkles);
            waterGodRays       = bool(p, "water_god_rays", waterGodRays);
            endSkyVortex       = bool(p, "end_sky_vortex", endSkyVortex);
            globalShadows      = bool(p, "global_shadows", globalShadows);
            witherStormEnhancedAi = bool(p, "wither_storm_enhanced_ai", witherStormEnhancedAi);
            npcWalkAnimations  = bool(p, "npc_walk_animations", npcWalkAnimations);
            stormBodySway      = bool(p, "storm_body_sway", stormBodySway);
            infiniteBackGrowth = bool(p, "infinite_back_growth", infiniteBackGrowth);
            infiniteBackGrowthSpeed = dbl(p, "infinite_back_growth_speed", infiniteBackGrowthSpeed);
            embeddedShaderPack = bool(p, "embedded_shader_pack", embeddedShaderPack);
        } catch (Throwable t) {
            // stay on defaults; never crash the game over a config file
        }
    }

    private static boolean bool(Properties p, String k, boolean d) {
        String v = p.getProperty(k);
        return v == null ? d : Boolean.parseBoolean(v.trim());
    }

    private static double dbl(Properties p, String k, double d) {
        try { return Double.parseDouble(p.getProperty(k).trim()); } catch (Throwable t) { return d; }
    }

    private McsmExtrasConfig() {}
}
