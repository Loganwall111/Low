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
 * the MCSM additions: tentacle grab cadence, beacon storm path, rise fx and
 * the counterclockwise spiral pin). Written with defaults on first launch.
 */
public final class McsmExtrasConfig {
    public static final String BUILD_VERSION = "0.0.0";
    public static boolean enableTentacleGrab = true;
    public static double  grabIntervalSeconds = 11.0;
    public static boolean enableBeaconStorm = true;
    public static double  beaconCooldownSeconds = 30.0;
    public static boolean enableRiseFx = true;
    public static boolean spiralCounterClockwise = true;
    public static boolean enableBeaconBlock = true;

    // ---- fields that ALSO existed in the MCSM-MCSM jars ----------------
    // (these live in the shipped jar's bytecode; other jar-side classes read
    // them, so the names/types must NEVER change or those classes crash with
    // NoSuchFieldError. ogCemModels defaults TRUE as of MCSM: the user
    // wants the MCSM/OG look (3 heads + command block, Obsidian Gloss) on
    // by default. The toggle still lets players opt out.)
    /** Prefer the OG/MCSM body look (Obsidian Gloss + trailer model path). */
    public static boolean ogCemModels = true;
    /** Apparent size of the smudge/halo quads behind the storm (0.5 = new). */
    public static double  smudgeScale = 0.72; // MCSM soft skirt

    // ---- MCSM batch (phase 29/30 user orders, 2026-09-04) ----------
    /** Storm glare mass scale; read by the blob carrier every frame. */
    public static double  glareSize = 0.92; // MCSM thick volume default
    /** Mod-side aurora borealis at night (cold-biome biased). */
    public static boolean auroraEnabled = true;
    /** Full death cinematic: distortion -> white cracks -> implosion flash ->
     *  supernova rings -> segments. Drives the sky carrier band 1906..2906. */
    public static boolean deathCinematic = true;
    /** Expanding ring shockwaves on phase 4 rise, phase 7 rise, and death. */
    public static boolean supernovaRings = true;
    /** Skull impacts: grey ground smoke + yellow electric sparks + crackle. */
    public static boolean smokeScreen = true;
    /** Phase 5.5+: purple lightning strikes + purple motes in the sky. */
    public static boolean purpleSky = true;
    /** Dust trails when the storm sweeps blocks. */
    public static boolean dustWaves = true;
    /** Post-death reality tear with the black aurora + corruption spread
     *  (user: "turned on by default in the config"). */
    public static boolean realityTear = true;
    /** Command block obliterate flash erases entities incl. players. */
    public static boolean obliterateFlash = true;
    /** Prank variant: also kicks players. Default OFF (grief-safe). */
    public static boolean obliterateKick = false;

    // ---- MCSM batch: the gates ------------------------------------
    /** Force the client's Story Mode look on (shadows, glare, smoke screen,
     *  skyboxes, vignette, tremor). Booleans only ever go ON, numeric values
     *  are only raised -- see McsmGate. */
    public static boolean forceMcsmLook = true;
    /** Force the world config on: building tear, corruption, shockwave
     *  particles, structure raids, withered mobs, cave rumble. */
    public static boolean forceMcsmWorld = true;

    // MCSM -- McsmShaderGatePatch forces ShaderPackCompat.active() to
    // false so the mod draws its own visuals under Iris. Dabicco's look presets
    // (Cinematic, Netflix) route part of their difference through that
    // shader-pack path, so with the gate forced they appear to "do nothing".
    // This toggle lets the player hand the answer back to the mod and A/B the
    // presets without editing files.
    public static boolean shaderPackGate = true;
    /** Taut glowing wire from the storm's core down to its ground anchor. */
    public static boolean commandWire = true;
    /** Brief a player the first time they get close to a live storm. */
    public static boolean mcsmInstructions = true;

    // ---- MCSM -- mega-phase 5b ------------------------------------
    /** Ship + auto-install the Devouring Storms Iris shader pack from inside
     *  the mod jar (user order: merged into the mod, DEFAULT ON). Applies at
     *  launch; see McsmShaderPackInstall. */
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
            p.setProperty("embedded_shader_pack", String.valueOf(embeddedShaderPack));
            try (OutputStream out = new FileOutputStream(f)) {
                p.store(out, "MCSM - storm gameplay patches + visuals + gates (glare size, aurora, death cinematic, supernova, smoke, tear, forced MCSM look/world). config_version below is the build that wrote this file.");
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
                f.getParentFile().mkdirs();
                p.setProperty("enable_tentacle_grab", "true");
                p.setProperty("grab_interval_seconds", "11.0");
                p.setProperty("enable_beacon_storm", "true");
                p.setProperty("beacon_cooldown_seconds", "30.0");
                p.setProperty("enable_rise_fx", "true");
                p.setProperty("spiral_counter_clockwise", "true");
                p.setProperty("enable_beacon_block", "true");
                try (OutputStream out = new FileOutputStream(f)) {
                    p.store(out, "MCSM - storm gameplay patches (grab, beacon, rise fx, spiral)");
                }
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
            // Migrate known legacy defaults only; user-picked slider values stay.
            if (cv == null || !BUILD_VERSION.equals(cv.trim())) {
                if (Math.abs(glareSize - 1.18) < 0.001 || Math.abs(glareSize - 0.58) < 0.001) {
                    glareSize = 0.92; // MCSM thick volume
                }
                if (Math.abs(smudgeScale - 0.5) < 0.001) {
                    smudgeScale = 0.72;
                }
            }
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
