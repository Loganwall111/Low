package net.dabicco.witherstormmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.fabricmc.loader.api.FabricLoader;

public class DabyWSClientConfig {
   public static boolean phaseAnim = true;
   public static double phaseAnimStrength = 1.0;
   public static boolean tentaclePhysics = false;
   public static boolean distantStorms = true;
   public static boolean distantFog = true;
   public static boolean optimizeDistantAnimations = false;
   public static boolean legacyDistantRenderer = false;
   public static boolean legacyHeads = false;
   public static boolean filledSubphases = true;
   public static boolean scaledSubphaseGrowth = false;
   public static boolean flatbackFlipFix = true;
   public static double mirrorBackDetail = 1.0;
   public static final boolean useNewFormidibomb = false;
   public static boolean trailerShadows = true;
   public static boolean groundShakingTremors = true;
   public static double screenTremorIntensity = 1.0;
   public static boolean stormProximityVignette = true;
   public static double vignetteIntensity = 1.0;
   public static boolean sicknessVeinOverlay = true;
   public static double sicknessVeinIntensity = 1.0;
   public static double chromaticGlitchStrength = 1.0;
   public static boolean storyModeBossbar = true;
   public static boolean storyModeTitleScreen = true;
   public static boolean cloudDeckLayer = true;
   public static boolean regionalBiomeFog = true;
   public static boolean purpleLightningSparks = true;
   public static double debrisDustParticles = 1.0;
   public static boolean headEyeGlow = true;
   public static double volumetricFogDensity = 1.0;
   public static boolean dynamicScreenShake = true;
   public static boolean stormAmbience = true;
   public static boolean beamHum = true;
   public static double ambienceVolume = 1.0;
   public static double headSoundsVolume = 1.0;
   public static double beamSoundsVolume = 1.0;
   public static boolean beamDeactivateSound = true;
   public static double beamHumVolume = 1.0;
   public static double beamHumRange = 48.0;
   public static boolean devourerDebrisGlow = true;
   public static boolean infectedMobSound = true;
   public static double infectedMobSoundVolume = 1.0;
   public static double beamEndFade = 0.55;
   public static double debrisAmount = 2.0;
   public static double fogColorR = 0.19;
   public static double fogColorG = 0.07;
   public static double fogColorB = 0.275;
   public static boolean stormFog = false;
   public static double stormFogStrength = 0.85F;
   public static boolean farLandsHaze = false;
   public static double farLandsDistance = 4000.0;
   public static double farLandsStrength = 0.8F;
   public static boolean biomeFogTint = false;
   public static double biomeFogStrength = 0.5;
   public static boolean separateFogColor = true;
   public static double skyDarkenR = 0.126;
   public static double skyDarkenG = 0.055;
   public static double skyDarkenB = 0.194;
   public static double skyDarkenIntensity = 0.6;
   public static double eyeColorR = 0.74;
   public static double eyeColorG = 0.8;
   public static double eyeColorB = 1.0;
   public static double stormGlowStrength = 1.0;
   public static boolean stormGlowFlip = false;
   public static double beamColorR = 0.3;
   public static double beamColorG = 0.22;
   public static double beamColorB = 1.0;
   public static boolean stormModelShading = true;
   public static boolean stormBackfaceCull = false;
   public static boolean stormRenderStats = false;
   public static double shadowMapResolution = 4096.0;
   public static boolean stormMusic = true;
   public static double stormMusicVolume = 1.0;
   public static double stormMusicRange = 256.0;
   public static double stormMusicCaveCutoff = 4.0;
   public static boolean stormShadow = true;
   public static boolean stormShadowSoftEdge = true;
   public static boolean stormShadowTerrain = true;
   public static boolean stormShadowHeightmap = false;
   public static boolean stormSelfShadow = true;
   public static boolean shadowCullBackFaces = true;
   public static boolean bowelsFrameHud = false;
   public static double stormShadingContrast = 0.75;
   public static boolean sunGlow = true;
   public static double sunGlowStrength = 2.2;
   public static double sunGlowR = 1.0;
   public static double sunGlowG = 0.82;
   public static double sunGlowB = 0.34;
   public static double stormShadowStrength = 0.55;
   public static double stormShadowR = 0.42;
   public static double stormShadowG = 0.46;
   public static double stormShadowB = 0.58;
   public static double skyDarkenLighting = 0.72;
   public static double cloudDarkenStrength = 0.9;
   public static double cloudColorR = 0.115;
   public static double cloudColorG = 0.095;
   public static double cloudColorB = 0.105;
   public static double beamOpacity = 0.6;
   public static boolean impactLight = true;
   public static double impactLightSize = 1.0;
   public static double impactLightBrightness = 1.0;
   public static double impactLightRange = 512.0;
   public static boolean impactLightUseBeamColor = true;
   public static double glowStrength = 1.0;
   public static boolean reverseShading = true;
   public static double bloomStrength = 2.0;
   public static final String[] BLOOM_LABELS = new String[]{"Off", "Subtle", "Normal", "Strong"};
   public static double bloomDebug = 0.0;
   public static final String[] BLOOM_DEBUG_LABELS = new String[]{
      "Off", "1 Source", "2 Scene Depth", "3 Bloom Depth", "4 Depth Mask", "5 Blur H", "6 Blur V", "7 Wide H", "8 Final Bloom", "9 UV Align"
   };
   public static boolean bloomMaskToStorm = true;
   public static double effectsPreset = 1.0;
   public static final String[] PRESET_LABELS = new String[]{"Custom", "MCSM OG Visuals", "Legacy Java", "Cinematic", "Netflix"};
   public static boolean beamInnerFaces = false;
   public static double debrisSize = 1.0;
   public static double stormSkin = 1.0;
   public static final String[] SKIN_LABELS = new String[]{"Classic", "Obsidian Gloss (OG)"};
   public static double stormStars = 1.0;
   public static final String[] STAR_LABELS = new String[]{"Off", "Storm Nights", "Every Night"};
   public static double starDensity = 1.0;
   public static double starTwinkleSpeed = 1.0;
   public static double starBrightness = 1.0;
   public static double stormCloudDeck = 1.0;
   public static final String[] CLOUD_DECK_LABELS = new String[]{"Off", "Subtle", "Dense"};
   public static double stormCloudCoverage = 1.0;
   public static double stormCloudAltitude = 0.0;
   public static double stormCloudPaletteMix = 0.85;
   public static boolean atmospherePulse = true;
   public static double pulseStrength = 1.0;
   public static double pulsePeriod = 4.0;
   public static double pulseSize = 1.0;
   public static boolean stormBackdrop = true;
   // The world-anchored Story Mode backdrop is on by default; the retired
   // full-screen/cylinder cloud layers remain separately disabled.
   public static boolean stormBackdropQuad = true;
   public static double stormBackdropStrength = 1.0;
   public static double stormBackdropSize = 6.0;
   public static double stormBackdropPulse = 1.0;
   public static boolean stormBackdropGrow = true;
   /** Keep the central black cylinder/blur off by default; the backdrop supplies the atmosphere. */
   public static boolean stormBackdropBlack = false;
   public static double stormBackdropBlackStrength = 1.0;
   public static boolean stormBackdropPhase4 = true;
   public static double stormBackdropPhase4Strength = 1.0;
   public static boolean stormBackdropTurquoise = true;
   public static boolean stormBackdropPurple = true;
   public static boolean stormBackdropPink = true;
   public static boolean stormBackdropEmber = false;
   public static double stormBackdropEmberStrength = 0.6;
   public static boolean storyModeClouds = true;
   public static double storyModeCloudStrength = 1.0;
   public static boolean storyModeCloudFade = true;
   public static double storyModeCloudFadeAmount = 0.75;
   public static boolean storyModeSky = true;
   public static double storyModeSkyStrength = 0.85;
   public static double storyModeFogStrength = 0.15;
   public static boolean storyModeLighting = true;
   public static double storyModeLightingStrength = 0.7;
   public static boolean phaseSky45Enabled = true;
   public static double phaseSky45R = 0.094;
   public static double phaseSky45G = 0.184;
   public static double phaseSky45B = 0.18;
   public static boolean phaseSky50Enabled = true;
   public static double phaseSky50R = 0.11;
   public static double phaseSky50G = 0.21;
   public static double phaseSky50B = 0.2;
   public static boolean phaseSky60Enabled = true;
   public static double phaseSky60R = 0.22;
   public static double phaseSky60G = 0.145;
   public static double phaseSky60B = 0.325;
   public static boolean phaseSky65Enabled = true;
   public static double phaseSky65R = 0.463;
   public static double phaseSky65G = 0.102;
   public static double phaseSky65B = 0.404;
   public static boolean phaseSky70Enabled = true;
   public static double phaseSky70R = 0.639;
   public static double phaseSky70G = 0.18;
   public static double phaseSky70B = 0.573;
   public static boolean phaseSkyRedEnabled = true;
   public static double phaseSkyRedR = 0.4;
   public static double phaseSkyRedG = 0.075;
   public static double phaseSkyRedB = 0.145;
   public static boolean todSkyDayEnabled = true;
   public static double todSkyDayR = 0.596;
   public static double todSkyDayG = 0.549;
   public static double todSkyDayB = 0.965;
   public static boolean todSkyDuskEnabled = true;
   public static double todSkyDuskR = 0.69;
   public static double todSkyDuskG = 0.4;
   public static double todSkyDuskB = 0.4;
   public static boolean todSkyNightEnabled = true;
   public static double todSkyNightR = 0.098;
   public static double todSkyNightG = 0.114;
   public static double todSkyNightB = 0.4;
   public static boolean todSkyDawnEnabled = true;
   public static double todSkyDawnR = 0.76;
   public static double todSkyDawnG = 0.56;
   public static double todSkyDawnB = 0.66;
   public static boolean todHorizonDayEnabled = true;
   public static double todHorizonDayR = 0.76;
   public static double todHorizonDayG = 0.72;
   public static double todHorizonDayB = 0.98;
   public static boolean todHorizonDuskEnabled = true;
   public static double todHorizonDuskR = 0.945;
   public static double todHorizonDuskG = 0.573;
   public static double todHorizonDuskB = 0.404;
   public static boolean todHorizonNightEnabled = true;
   public static double todHorizonNightR = 0.29;
   public static double todHorizonNightG = 0.404;
   public static double todHorizonNightB = 0.925;
   public static boolean todHorizonDawnEnabled = true;
   public static double todHorizonDawnR = 0.96;
   public static double todHorizonDawnG = 0.66;
   public static double todHorizonDawnB = 0.76;
   public static boolean todCloudDayEnabled = true;
   public static double todCloudDayR = 0.965;
   public static double todCloudDayG = 0.961;
   public static double todCloudDayB = 1.0;
   public static boolean todCloudDuskEnabled = true;
   public static double todCloudDuskR = 0.98;
   public static double todCloudDuskG = 0.76;
   public static double todCloudDuskB = 0.62;
   public static boolean todCloudNightEnabled = true;
   public static double todCloudNightR = 0.298;
   public static double todCloudNightG = 0.361;
   public static double todCloudNightB = 0.678;
   public static boolean todCloudDawnEnabled = true;
   public static double todCloudDawnR = 0.941;
   public static double todCloudDawnG = 0.8;
   public static double todCloudDawnB = 0.859;
   public static boolean todLightDayEnabled = true;
   public static double todLightDayR = 1.0;
   public static double todLightDayG = 0.98;
   public static double todLightDayB = 1.0;
   public static boolean todLightDuskEnabled = true;
   public static double todLightDuskR = 1.0;
   public static double todLightDuskG = 0.78;
   public static double todLightDuskB = 0.64;
   public static boolean todLightNightEnabled = true;
   public static double todLightNightR = 0.56;
   public static double todLightNightG = 0.64;
   public static double todLightNightB = 1.0;
   public static boolean todLightDawnEnabled = true;
   public static double todLightDawnR = 1.0;
   public static double todLightDawnG = 0.86;
   public static double todLightDawnB = 0.9;
   public static boolean biomeSkyPlainsEnabled = true;
   public static double biomeSkyPlainsR = 0.596;
   public static double biomeSkyPlainsG = 0.549;
   public static double biomeSkyPlainsB = 0.965;
   public static boolean biomeSkyDesertEnabled = true;
   public static double biomeSkyDesertR = 0.98;
   public static double biomeSkyDesertG = 0.82;
   public static double biomeSkyDesertB = 0.58;
   public static boolean biomeSkySnowyEnabled = true;
   public static double biomeSkySnowyR = 0.72;
   public static double biomeSkySnowyG = 0.84;
   public static double biomeSkySnowyB = 1.0;
   public static boolean biomeSkySwampEnabled = true;
   public static double biomeSkySwampR = 0.42;
   public static double biomeSkySwampG = 0.55;
   public static double biomeSkySwampB = 0.4;
   public static boolean biomeSkyJungleEnabled = true;
   public static double biomeSkyJungleR = 0.38;
   public static double biomeSkyJungleG = 0.64;
   public static double biomeSkyJungleB = 0.52;
   public static boolean biomeSkySavannaEnabled = true;
   public static double biomeSkySavannaR = 0.9;
   public static double biomeSkySavannaG = 0.76;
   public static double biomeSkySavannaB = 0.48;
   public static boolean biomeSkyBadlandsEnabled = true;
   public static double biomeSkyBadlandsR = 0.88;
   public static double biomeSkyBadlandsG = 0.56;
   public static double biomeSkyBadlandsB = 0.36;
   public static boolean biomeSkyOceanEnabled = true;
   public static double biomeSkyOceanR = 0.42;
   public static double biomeSkyOceanG = 0.62;
   public static double biomeSkyOceanB = 0.94;
   public static boolean biomeSkyMushroomEnabled = true;
   public static double biomeSkyMushroomR = 0.7;
   public static double biomeSkyMushroomG = 0.52;
   public static double biomeSkyMushroomB = 0.78;
   public static boolean biomeSkyNetherEnabled = true;
   public static double biomeSkyNetherR = 0.61;
   public static double biomeSkyNetherG = 0.07;
   public static double biomeSkyNetherB = 0.48;
   public static boolean biomeSkyEndEnabled = true;
   public static double biomeSkyEndR = 0.18;
   public static double biomeSkyEndG = 0.14;
   public static double biomeSkyEndB = 0.24;
   public static double phaseFog45 = 1.0;
   public static double phaseFog50 = 1.0;
   public static double phaseFog60 = 1.0;
   public static double phaseFog65 = 1.0;
   public static double phaseFog70 = 1.0;
   public static double stormSkyRange = 900.0;
   public static double stormSkyFalloff = 0.55;
   public static double stormSkyCore = 0.65;
   public static double stormSkySmooth = 0.05;
   public static boolean turquoiseTeeth = true;
   public static double turquoiseTeethIntensity = 1.6;
   public static boolean cataclysmHalos = true;
   public static double haloStrength = 1.0;
   public static boolean blackGlare = true;
   public static double blackGlareStrength = 1.0;
   public static boolean glareEjecta = true;
   public static double ejectaRate = 1.0;
   public static double ejectaBrightness = 1.0;
   public static boolean pulseHeartbeat = false;
   public static double pulseHeartbeatVolume = 1.0;
   public static double pulseHeartbeatRange = 512.0;
   public static boolean phaseFogPalettes = true;
   public static double paletteStrength = 0.85;
   public static double turquoiseFogR = 0.031;
   public static double turquoiseFogG = 0.42;
   public static double turquoiseFogB = 0.36;
   public static double cataclysmFogR = 0.055;
   public static double cataclysmFogG = 0.028;
   public static double cataclysmFogB = 0.1;
   public static boolean configOpened = false;
   public static boolean bossbarNotched = true;
   public static boolean clusterVolumetricLighting = false;
   public static double bossbarColor = 5.0;
   public static final String[] BOSSBAR_COLOR_LABELS = new String[]{"Pink", "Blue", "Red", "Green", "Yellow", "Purple", "White"};
   public static double tentacleIdleSpeed = 1.0;
   public static double tentacleWaveTravel = 1.0;
   public static double tentacleCurlDepth = 1.0;
   public static double tentacleCrossAxis = 0.8;
   public static double bigTentacleCurlDepth = 1.0;
   public static double bigTentacleHangBreath = 1.0;
   public static double bigTentacleSideSweep = 1.0;
   public static double lateGrowthWrithe = 1.35;
   public static double verletGravity = 0.05;
   public static double verletSway = 2.2;
   public static double verletDamping = 0.9;
   public static double verletWrithe = 0.1;
   public static double verletWritheSpeed = 0.035;
   public static double yawSmoothTime = 0.55;
   public static double yawSnapDegrees = 45.0;
   public static double growthSmoothRate = 1.6;
   public static double changeoverShake = 1.0;
   public static double jawLagGain = 0.55;
   public static double jawLagMax = 16.0;
   public static double jawLagCatchup = 0.22;
   public static double bodyLeanGain = 1.0;
   public static double bodyBankGain = 1.0;
   public static double nameStyle = 0.0;
   public static final String[] NAME_STYLE_LABELS = new String[]{"Classic", "Cracker's", "Legacy"};
   public static final Map<String, net.dabicco.witherstormmod.config.DabyWSClientConfig.Key> KEYS = new LinkedHashMap<>();
   private static final Map<String, Double> DEFAULTS = new LinkedHashMap<>();
   private static final int CONFIG_VERSION = 13;
   private static int loadedVersion;
   public static final String RESET_VERSION = "Beta 1.9.33";
   private static final Map<String, Double> PRESET_MCSM;
   private static final Map<String, Double> PRESET_LEGACY;
   private static final Map<String, Double> PRESET_NETFLIX;
   private static final Map<String, Double> PRESET_CINEMATIC;
   private static final Gson GSON;
   private static boolean wipedByRestructure;

   public static String earlyName() {
      return switch ((int)Math.round(nameStyle)) {
         case 1 -> "Wither Storm";
         case 2 -> "Wither (Wither Storm)";
         default -> "Commanded Wither";
      };
   }

   public static String stormName() {
      return switch ((int)Math.round(nameStyle)) {
         case 1 -> "Wither Storm";
         default -> "The Wither Storm";
      };
   }

   private static void setFilledSubphases(double v) {
      filledSubphases = v >= 0.5;
      if (!filledSubphases) {
         scaledSubphaseGrowth = false;
      }
   }

   private static void setScaledSubphaseGrowth(double v) {
      scaledSubphaseGrowth = filledSubphases && v >= 0.5;
   }

   public static boolean isLocked(String keyName) {
      return keyName.equals("scaledSubphaseGrowth") && !filledSubphases || keyName.equals("phaseAnimStrength") && !phaseAnim;
   }

   private static void key(String name, String description, double min, double max, boolean toggle, DoubleSupplier get, DoubleConsumer set) {
      KEYS.put(name, new net.dabicco.witherstormmod.config.DabyWSClientConfig.Key(name, description, min, max, toggle, (String[])null, get, set));
   }

   private static void keyCycle(String name, String description, String[] labels, DoubleSupplier get, DoubleConsumer set) {
      KEYS.put(name, new net.dabicco.witherstormmod.config.DabyWSClientConfig.Key(name, description, 0.0, labels.length - 1, false, labels, get, set));
   }

   private static double snapToPowerOfTwo(double v) {
      double clamped = Math.max(512.0, Math.min(8192.0, v));
      int exp = (int)Math.round(Math.log(clamped) / Math.log(2.0));
      return Math.pow(2.0, Math.max(9, Math.min(13, exp)));
   }

   public static double defaultOf(String name) {
      return DEFAULTS.getOrDefault(name, 0.0);
   }

   private static Map<String, Double> presetValues(int preset) {
      return switch (preset) {
         case 1 -> PRESET_MCSM;
         case 2 -> PRESET_LEGACY;
         case 3 -> PRESET_CINEMATIC;
         case 4 -> PRESET_NETFLIX;
         default -> null;
      };
   }

   public static boolean isPresetKey(String name) {
      return PRESET_MCSM.containsKey(name);
   }

   public static void applyPreset(int preset) {
      applyPreset(preset, (Collection<String>)null);
   }

   public static void applyPreset(int preset, Collection<String> tabKeys) {
      Map<String, Double> values = presetValues(preset);
      if (values != null) {
         if (tabKeys != null) {
            resetDefaults(tabKeys);
         }

         for (Entry<String, Double> e : values.entrySet()) {
            net.dabicco.witherstormmod.config.DabyWSClientConfig.Key key = KEYS.get(e.getKey());
            if (key != null) {
               key.set().accept(e.getValue());
            }
         }

         effectsPreset = preset;
      }
   }

   public static void refreshPreset() {
      for (int preset = 1; preset <= 3; preset++) {
         Map<String, Double> values = presetValues(preset);
         boolean match = true;

         for (Entry<String, Double> e : values.entrySet()) {
            net.dabicco.witherstormmod.config.DabyWSClientConfig.Key key = KEYS.get(e.getKey());
            if (key == null || Math.abs(key.get().getAsDouble() - e.getValue()) > 1.0E-6) {
               match = false;
               break;
            }
         }

         if (match) {
            effectsPreset = preset;
            return;
         }
      }

      effectsPreset = 0.0;
   }

   private static Path file() {
      return FabricLoader.getInstance().getConfigDir().resolve("dabywitherstormmod-client.json");
   }

   public static void load() {
      Path path = file();
      if (Files.exists(path)) {
         try {
            JsonObject json = (JsonObject)GSON.fromJson(Files.readString(path), JsonObject.class);
            if (json == null) {
               return;
            }

            for (net.dabicco.witherstormmod.config.DabyWSClientConfig.Key key : KEYS.values()) {
               if (json.has(key.name())) {
                  key.set().accept(key.clamp(json.get(key.name()).getAsDouble()));
               }
            }

            if (json.has("effectsPreset")) {
               effectsPreset = json.get("effectsPreset").getAsDouble();
            }

            if (json.has("configOpened")) {
               configOpened = json.get("configOpened").getAsBoolean();
            }

            loadedVersion = json.has("configVersion") ? json.get("configVersion").getAsInt() : 1;
            migrate();
            refreshPreset();
         } catch (IOException | RuntimeException var4) {
            PrintStream var10000 = System.out;
            String var10001 = String.valueOf(path);
            var10000.println("Failed to read " + var10001 + ": " + var4);
         }
      }
   }

   public static boolean consumeRestructureNotice() {
      boolean was = wipedByRestructure;
      wipedByRestructure = false;
      return was;
   }

   private static void migrate() {
      if (loadedVersion < 13) {
         if (loadedVersion < 13) {
            resetDefaults();
            wipedByRestructure = true;
            loadedVersion = 13;
            save();
         } else {
            if (loadedVersion < 3) {
               for (String name : new String[]{"beamColorR", "beamColorG", "beamColorB", "beamOpacity"}) {
                  KEYS.get(name).set().accept(defaultOf(name));
               }
            }

            if (loadedVersion < 6) {
               for (String name : new String[]{"eyeColorR", "eyeColorG", "eyeColorB"}) {
                  KEYS.get(name).set().accept(defaultOf(name));
               }
            }

            if (loadedVersion < 7) {
               KEYS.get("bloomStrength").set().accept(defaultOf("bloomStrength"));
            }

            if (loadedVersion < 8) {
               KEYS.get("bloomMaskToStorm").set().accept(defaultOf("bloomMaskToStorm"));
            }

            if (loadedVersion < 9) {
               KEYS.get("bloomStrength").set().accept(defaultOf("bloomStrength"));
            }

            if (loadedVersion < 10) {
               KEYS.get("bloomStrength").set().accept(defaultOf("bloomStrength"));
               KEYS.get("bloomMaskToStorm").set().accept(defaultOf("bloomMaskToStorm"));
            }

            loadedVersion = 13;
            save();
         }
      }
   }

   public static void save() {
      JsonObject json = new JsonObject();

      for (net.dabicco.witherstormmod.config.DabyWSClientConfig.Key key : KEYS.values()) {
         json.addProperty(key.name(), key.get().getAsDouble());
      }

      json.addProperty("effectsPreset", effectsPreset);
      json.addProperty("configOpened", configOpened);
      json.addProperty("configVersion", 13);

      try {
         Files.writeString(file(), GSON.toJson(json));
      } catch (IOException var4) {
         PrintStream var10000 = System.out;
         String var10001 = String.valueOf(file());
         var10000.println("Failed to write " + var10001 + ": " + var4);
      }
   }

   public static void resetDefaults() {
      resetDefaults(KEYS.keySet());
   }

   public static void resetDefaults(Collection<String> names) {
      for (net.dabicco.witherstormmod.config.DabyWSClientConfig.Key key : KEYS.values()) {
         if (names.contains(key.name())) {
            key.set().accept(defaultOf(key.name()));
         }
      }
   }

   public static float soundMultiplier(String path) {
      if (path.startsWith("ambience")) {
         return 1.0F;
      } else if (path.equals("head_beam_loop")) {
         return (float)beamHumVolume;
      } else if (!path.equals("head_beam_deactivate") && !path.equals("tractor_beam_ground_disable")) {
         if (path.startsWith("head_beam") || path.equals("head_activate_beam")) {
            return (float)beamSoundsVolume;
         } else if (path.startsWith("head_")) {
            return (float)headSoundsVolume;
         } else if (!path.equals("infected_mob") && !path.startsWith("withered_")) {
            return 1.0F;
         } else {
            return infectedMobSound ? (float)infectedMobSoundVolume : 0.0F;
         }
      } else {
         return beamDeactivateSound ? (float)beamSoundsVolume : 0.0F;
      }
   }

   static {
      key("distantStorms", "Draw storms that are past normal entity range.", 0.0, 1.0, true, () -> distantStorms ? 1.0 : 0.0, v -> distantStorms = v >= 0.5);
      key("distantFog", "Haze distant storms so they sit in the weather.", 0.0, 1.0, true, () -> distantFog ? 1.0 : 0.0, v -> distantFog = v >= 0.5);
      key("legacyHeads", "Simpler heads and a cleaner beam. Less accurate to MCSM.", 0.0, 1.0, true, () -> legacyHeads ? 1.0 : 0.0, v -> legacyHeads = v >= 0.5);
      key(
         "clusterVolumetricLighting",
         "Relight every block of every debris cluster each frame. Much prettier up close and very expensive: a field of large clusters can cost most of your framerate.",
         0.0,
         1.0,
         true,
         () -> clusterVolumetricLighting ? 1.0 : 0.0,
         v -> clusterVolumetricLighting = v >= 0.5
      );
      key(
         "filledSubphases",
         "Grow the back mass in across the fractional subphases. Off removes it.",
         0.0,
         1.0,
         true,
         () -> filledSubphases ? 1.0 : 0.0,
         net.dabicco.witherstormmod.config.DabyWSClientConfig::setFilledSubphases
      );
      key(
         "scaledSubphaseGrowth",
         "Swell the new back up smoothly instead of building it cube by cube. Needs Filled Subphases.",
         0.0,
         1.0,
         true,
         () -> scaledSubphaseGrowth ? 1.0 : 0.0,
         net.dabicco.witherstormmod.config.DabyWSClientConfig::setScaledSubphaseGrowth
      );
      key(
         "flatbackFlipFix",
         "Fill the shell's flat rear with a mirrored copy so the mass reads rounded.",
         0.0,
         1.0,
         true,
         () -> flatbackFlipFix ? 1.0 : 0.0,
         v -> flatbackFlipFix = v >= 0.5
      );
      key(
         "mirrorBackDetail",
         "How much of that mirrored fill to draw. It hides behind the real back and costs nearly half of what late phase 5 adds, so it is the cheapest detail to give up. 1 is everything.",
         0.25,
         1.0,
         false,
         () -> mirrorBackDetail,
         v -> mirrorBackDetail = v
      );
      key("stormAmbience", "Play the looping storm ambience.", 0.0, 1.0, true, () -> stormAmbience ? 1.0 : 0.0, v -> stormAmbience = v >= 0.5);
      key("beamHum", "Play the tractor beam hum.", 0.0, 1.0, true, () -> beamHum ? 1.0 : 0.0, v -> beamHum = v >= 0.5);
      key("ambienceVolume", "", 0.0, 2.0, false, () -> ambienceVolume, v -> ambienceVolume = v);
      key("headSoundsVolume", "Growls, snarls, roars, hurt and shoot sounds.", 0.0, 2.0, false, () -> headSoundsVolume, v -> headSoundsVolume = v);
      key(
         "beamDeactivateSound",
         "The beam switching off. It fires far more often than the switch-on.",
         0.0,
         1.0,
         true,
         () -> beamDeactivateSound ? 1.0 : 0.0,
         v -> beamDeactivateSound = v >= 0.5
      );
      key("beamSoundsVolume", "", 0.0, 2.0, false, () -> beamSoundsVolume, v -> beamSoundsVolume = v);
      key("beamHumVolume", "", 0.0, 2.0, false, () -> beamHumVolume, v -> beamHumVolume = v);
      key("beamHumRange", "How far the hum carries, in blocks. 0 means only from inside it.", 0.0, 256.0, false, () -> beamHumRange, v -> beamHumRange = v);
      key("infectedMobSound", "The wail when a mob turns fully Withered.", 0.0, 1.0, true, () -> infectedMobSound ? 1.0 : 0.0, v -> infectedMobSound = v >= 0.5);
      key("infectedMobSoundVolume", "", 0.0, 2.0, false, () -> infectedMobSoundVolume, v -> infectedMobSoundVolume = v);
      key(
         "devourerDebrisGlow",
         "Let the phase-6 violet debris feed the bloom.",
         0.0,
         1.0,
         true,
         () -> devourerDebrisGlow ? 1.0 : 0.0,
         v -> devourerDebrisGlow = v >= 0.5
      );
      key("debrisAmount", "How much wreckage orbits the storm.", 0.0, 2.0, false, () -> debrisAmount, v -> debrisAmount = v);
      key("debrisSize", "Size of the wreckage blocks caught in the tractor beams.", 0.2, 3.0, false, () -> debrisSize, v -> debrisSize = v);
      keyCycle(
         "stormSkin",
         "Obsidian Gloss is the OG MCSM skin: shiny near-black flesh with a purple sheen, and the command block belly redrawn as obsidian-purple tiles instead of vanilla orange. Classic keeps the plain textures.",
         SKIN_LABELS,
         () -> stormSkin,
         v -> stormSkin = Math.round(v)
      );
      keyCycle(
         "stormStars",
         "A dome of twinkling stars in the blacked-out sky. Storm Nights shows it only while a phase-5+ storm has eaten the light; Every Night replaces the vanilla sky every night.",
         STAR_LABELS,
         () -> stormStars,
         v -> stormStars = Math.round(v)
      );
      key("starDensity", "How many stars fill the dome.", 0.25, 2.0, false, () -> starDensity, v -> starDensity = v);
      key("starTwinkleSpeed", "How fast the stars twinkle.", 0.0, 4.0, false, () -> starTwinkleSpeed, v -> starTwinkleSpeed = v);
      key("starBrightness", "Overall star brightness.", 0.0, 2.0, false, () -> starBrightness, v -> starBrightness = v);
      keyCycle(
         "stormCloudDeck",
         "MCSM-style weather slabs slowly orbiting the storm instead of a flat dark sky.",
         CLOUD_DECK_LABELS,
         () -> stormCloudDeck,
         v -> stormCloudDeck = Math.round(v)
      );
      key(
         "stormCloudCoverage", "How much of the sky around the storm the deck covers.", 0.25, 2.0, false, () -> stormCloudCoverage, v -> stormCloudCoverage = v
      );
      key("stormCloudAltitude", "Push the whole deck up or down.", -40.0, 40.0, false, () -> stormCloudAltitude, v -> stormCloudAltitude = v);
      key(
         "stormCloudPaletteMix",
         "How much the deck follows the phase palette versus your manual cloud colour.",
         0.0,
         1.0,
         false,
         () -> stormCloudPaletteMix,
         v -> stormCloudPaletteMix = v
      );
      key(
         "atmospherePulse",
         "The purple-blue glare breathing in the air around a late-phase storm - part of the atmosphere, bigger than the body.",
         0.0,
         1.0,
         true,
         () -> atmospherePulse ? 1.0 : 0.0,
         v -> atmospherePulse = v >= 0.5
      );
      key("pulseStrength", "How bright the pulse burns at its peak.", 0.0, 2.0, false, () -> pulseStrength, v -> pulseStrength = v);
      key("pulsePeriod", "Seconds between pulse peaks.", 1.0, 10.0, false, () -> pulsePeriod, v -> pulsePeriod = v);
      key("pulseSize", "How far the glow reaches past the body.", 0.5, 2.0, false, () -> pulseSize, v -> pulseSize = v);
      key(
         "stormBackdrop",
         "Atmospheric W's Cloud: the infinite, wide smog layer that hangs behind the Wither Storm and follows its sky direction.",
         0.0,
         1.0,
         true,
         () -> stormBackdrop ? 1.0 : 0.0,
         v -> stormBackdrop = v >= 0.5
      );
      key("stormBackdropStrength", "Overall opacity of the backdrop.", 0.0, 2.0, false, () -> stormBackdropStrength, v -> stormBackdropStrength = v);
      key("stormBackdropSize", "Backdrop size as a multiple of the storm's body radius.", 2.0, 14.0, false, () -> stormBackdropSize, v -> stormBackdropSize = v);
      key("stormBackdropPulse", "How fast the backdrop breathes.", 0.0, 5.0, false, () -> stormBackdropPulse, v -> stormBackdropPulse = v);
      key(
         "stormBackdropGrow",
         "Backdrop keeps growing past phase 5.5 as the storm grows.",
         0.0,
         1.0,
         true,
         () -> stormBackdropGrow ? 1.0 : 0.0,
         v -> stormBackdropGrow = v >= 0.5
      );
      key(
         "stormBackdropBlack",
         "The black blur in the centre of the backdrop.",
         0.0,
         1.0,
         true,
         () -> stormBackdropBlack ? 1.0 : 0.0,
         v -> stormBackdropBlack = v >= 0.5
      );
      key(
         "stormBackdropBlackStrength",
         "How dark the central blur gets.",
         0.0,
         2.0,
         false,
         () -> stormBackdropBlackStrength,
         v -> stormBackdropBlackStrength = v
      );
      key(
         "stormBackdropPhase4",
         "Phase 4: the blue glow behind the storm.",
         0.0,
         1.0,
         true,
         () -> stormBackdropPhase4 ? 1.0 : 0.0,
         v -> stormBackdropPhase4 = v >= 0.5
      );
      key(
         "stormBackdropPhase4Strength",
         "Brightness of the phase 4 blue glow.",
         0.0,
         2.0,
         false,
         () -> stormBackdropPhase4Strength,
         v -> stormBackdropPhase4Strength = v
      );
      key(
         "stormBackdropTurquoise",
         "Phase 4.5 - 5.1: the dark turquoise haze.",
         0.0,
         1.0,
         true,
         () -> stormBackdropTurquoise ? 1.0 : 0.0,
         v -> stormBackdropTurquoise = v >= 0.5
      );
      key(
         "stormBackdropPurple",
         "Phase 5.1+: the purple sky behind the storm.",
         0.0,
         1.0,
         true,
         () -> stormBackdropPurple ? 1.0 : 0.0,
         v -> stormBackdropPurple = v >= 0.5
      );
      key(
         "stormBackdropPink",
         "Phase 5.5+: magenta/pink wrapping around the purple.",
         0.0,
         1.0,
         true,
         () -> stormBackdropPink ? 1.0 : 0.0,
         v -> stormBackdropPink = v >= 0.5
      );
      key(
         "stormBackdropEmber",
         "Adds the orange ember tint from the sunset shots.",
         0.0,
         1.0,
         true,
         () -> stormBackdropEmber ? 1.0 : 0.0,
         v -> stormBackdropEmber = v >= 0.5
      );
      key("stormBackdropEmberStrength", "Strength of the ember tint.", 0.0, 2.0, false, () -> stormBackdropEmberStrength, v -> stormBackdropEmberStrength = v);
      key(
         "storyModeClouds",
         "Flat Story Mode clouds: one solid colour per cloud instead of vanilla's per-face shading, tinted by time of day.",
         0.0,
         1.0,
         true,
         () -> storyModeClouds ? 1.0 : 0.0,
         v -> storyModeClouds = v >= 0.5
      );
      key(
         "storyModeCloudStrength",
         "How far the clouds are pushed toward the Story Mode palette.",
         0.0,
         1.0,
         false,
         () -> storyModeCloudStrength,
         v -> storyModeCloudStrength = v
      );
      key(
         "storyModeCloudFade",
         "Clouds turn transparent when the storm's black backdrop is behind them.",
         0.0,
         1.0,
         true,
         () -> storyModeCloudFade ? 1.0 : 0.0,
         v -> storyModeCloudFade = v >= 0.5
      );
      key(
         "storyModeCloudFadeAmount",
         "How far the clouds fade out near the storm.",
         0.0,
         1.0,
         false,
         () -> storyModeCloudFadeAmount,
         v -> storyModeCloudFadeAmount = v
      );
      key(
         "storyModeSky",
         "The lavender Story Mode sky and fog, shifting through day, dusk, night and dawn.",
         0.0,
         1.0,
         true,
         () -> storyModeSky ? 1.0 : 0.0,
         v -> storyModeSky = v >= 0.5
      );
      key(
         "storyModeSkyStrength",
         "How far the SKY DOME is pushed toward the Story Mode palette.",
         0.0,
         1.0,
         false,
         () -> storyModeSkyStrength,
         v -> storyModeSkyStrength = v
      );
      key(
         "storyModeFogStrength",
         "How far distance FOG is tinted. Keep this low: fog colour is what far terrain fades into, so high values wash the world flat.",
         0.0,
         1.0,
         false,
         () -> storyModeFogStrength,
         v -> storyModeFogStrength = v
      );
      key(
         "storyModeLighting",
         "Tints world lighting with the Story Mode palette so shadows shift colour through the day.",
         0.0,
         1.0,
         true,
         () -> storyModeLighting ? 1.0 : 0.0,
         v -> storyModeLighting = v >= 0.5
      );
      key(
         "storyModeLightingStrength",
         "Strength of the coloured lighting tint.",
         0.0,
         1.0,
         false,
         () -> storyModeLightingStrength,
         v -> storyModeLightingStrength = v
      );
      key(
         "phaseSky45Enabled",
         "Enable the dark turquoise sky at phase 4.5.",
         0.0,
         1.0,
         true,
         () -> phaseSky45Enabled ? 1.0 : 0.0,
         v -> phaseSky45Enabled = v >= 0.5
      );
      key("phaseSky45R", "the dark turquoise sky at phase 4.5 - R channel.", 0.0, 1.0, false, () -> phaseSky45R, v -> phaseSky45R = v);
      key("phaseSky45G", "the dark turquoise sky at phase 4.5 - G channel.", 0.0, 1.0, false, () -> phaseSky45G, v -> phaseSky45G = v);
      key("phaseSky45B", "the dark turquoise sky at phase 4.5 - B channel.", 0.0, 1.0, false, () -> phaseSky45B, v -> phaseSky45B = v);
      key(
         "phaseSky50Enabled",
         "Enable the green sky held through phase 5.",
         0.0,
         1.0,
         true,
         () -> phaseSky50Enabled ? 1.0 : 0.0,
         v -> phaseSky50Enabled = v >= 0.5
      );
      key("phaseSky50R", "the green sky held through phase 5 - R channel.", 0.0, 1.0, false, () -> phaseSky50R, v -> phaseSky50R = v);
      key("phaseSky50G", "the green sky held through phase 5 - G channel.", 0.0, 1.0, false, () -> phaseSky50G, v -> phaseSky50G = v);
      key("phaseSky50B", "the green sky held through phase 5 - B channel.", 0.0, 1.0, false, () -> phaseSky50B, v -> phaseSky50B = v);
      key(
         "phaseSky60Enabled",
         "Enable the purple sky after phase 5 ends.",
         0.0,
         1.0,
         true,
         () -> phaseSky60Enabled ? 1.0 : 0.0,
         v -> phaseSky60Enabled = v >= 0.5
      );
      key("phaseSky60R", "the purple sky after phase 5 ends - R channel.", 0.0, 1.0, false, () -> phaseSky60R, v -> phaseSky60R = v);
      key("phaseSky60G", "the purple sky after phase 5 ends - G channel.", 0.0, 1.0, false, () -> phaseSky60G, v -> phaseSky60G = v);
      key("phaseSky60B", "the purple sky after phase 5 ends - B channel.", 0.0, 1.0, false, () -> phaseSky60B, v -> phaseSky60B = v);
      key("phaseSky65Enabled", "Enable the magenta sky as it grows.", 0.0, 1.0, true, () -> phaseSky65Enabled ? 1.0 : 0.0, v -> phaseSky65Enabled = v >= 0.5);
      key("phaseSky65R", "the magenta sky as it grows - R channel.", 0.0, 1.0, false, () -> phaseSky65R, v -> phaseSky65R = v);
      key("phaseSky65G", "the magenta sky as it grows - G channel.", 0.0, 1.0, false, () -> phaseSky65G, v -> phaseSky65G = v);
      key("phaseSky65B", "the magenta sky as it grows - B channel.", 0.0, 1.0, false, () -> phaseSky65B, v -> phaseSky65B = v);
      key("phaseSky70Enabled", "Enable the final violet-pink gradient.", 0.0, 1.0, true, () -> phaseSky70Enabled ? 1.0 : 0.0, v -> phaseSky70Enabled = v >= 0.5);
      key("phaseSky70R", "the final violet-pink gradient - R channel.", 0.0, 1.0, false, () -> phaseSky70R, v -> phaseSky70R = v);
      key("phaseSky70G", "the final violet-pink gradient - G channel.", 0.0, 1.0, false, () -> phaseSky70G, v -> phaseSky70G = v);
      key("phaseSky70B", "the final violet-pink gradient - B channel.", 0.0, 1.0, false, () -> phaseSky70B, v -> phaseSky70B = v);
      key(
         "phaseSkyRedEnabled",
         "Enable the red mixed into the late gradient.",
         0.0,
         1.0,
         true,
         () -> phaseSkyRedEnabled ? 1.0 : 0.0,
         v -> phaseSkyRedEnabled = v >= 0.5
      );
      key("phaseSkyRedR", "the red mixed into the late gradient - R channel.", 0.0, 1.0, false, () -> phaseSkyRedR, v -> phaseSkyRedR = v);
      key("phaseSkyRedG", "the red mixed into the late gradient - G channel.", 0.0, 1.0, false, () -> phaseSkyRedG, v -> phaseSkyRedG = v);
      key("phaseSkyRedB", "the red mixed into the late gradient - B channel.", 0.0, 1.0, false, () -> phaseSkyRedB, v -> phaseSkyRedB = v);
      key("todSkyDayEnabled", "Enable the daytime sky.", 0.0, 1.0, true, () -> todSkyDayEnabled ? 1.0 : 0.0, v -> todSkyDayEnabled = v >= 0.5);
      key("todSkyDayR", "the daytime sky - R channel.", 0.0, 1.0, false, () -> todSkyDayR, v -> todSkyDayR = v);
      key("todSkyDayG", "the daytime sky - G channel.", 0.0, 1.0, false, () -> todSkyDayG, v -> todSkyDayG = v);
      key("todSkyDayB", "the daytime sky - B channel.", 0.0, 1.0, false, () -> todSkyDayB, v -> todSkyDayB = v);
      key("todSkyDuskEnabled", "Enable the dusk sky.", 0.0, 1.0, true, () -> todSkyDuskEnabled ? 1.0 : 0.0, v -> todSkyDuskEnabled = v >= 0.5);
      key("todSkyDuskR", "the dusk sky - R channel.", 0.0, 1.0, false, () -> todSkyDuskR, v -> todSkyDuskR = v);
      key("todSkyDuskG", "the dusk sky - G channel.", 0.0, 1.0, false, () -> todSkyDuskG, v -> todSkyDuskG = v);
      key("todSkyDuskB", "the dusk sky - B channel.", 0.0, 1.0, false, () -> todSkyDuskB, v -> todSkyDuskB = v);
      key("todSkyNightEnabled", "Enable the night sky.", 0.0, 1.0, true, () -> todSkyNightEnabled ? 1.0 : 0.0, v -> todSkyNightEnabled = v >= 0.5);
      key("todSkyNightR", "the night sky - R channel.", 0.0, 1.0, false, () -> todSkyNightR, v -> todSkyNightR = v);
      key("todSkyNightG", "the night sky - G channel.", 0.0, 1.0, false, () -> todSkyNightG, v -> todSkyNightG = v);
      key("todSkyNightB", "the night sky - B channel.", 0.0, 1.0, false, () -> todSkyNightB, v -> todSkyNightB = v);
      key("todSkyDawnEnabled", "Enable the dawn sky.", 0.0, 1.0, true, () -> todSkyDawnEnabled ? 1.0 : 0.0, v -> todSkyDawnEnabled = v >= 0.5);
      key("todSkyDawnR", "the dawn sky - R channel.", 0.0, 1.0, false, () -> todSkyDawnR, v -> todSkyDawnR = v);
      key("todSkyDawnG", "the dawn sky - G channel.", 0.0, 1.0, false, () -> todSkyDawnG, v -> todSkyDawnG = v);
      key("todSkyDawnB", "the dawn sky - B channel.", 0.0, 1.0, false, () -> todSkyDawnB, v -> todSkyDawnB = v);
      key("todHorizonDayEnabled", "Enable the daytime horizon.", 0.0, 1.0, true, () -> todHorizonDayEnabled ? 1.0 : 0.0, v -> todHorizonDayEnabled = v >= 0.5);
      key("todHorizonDayR", "the daytime horizon - R channel.", 0.0, 1.0, false, () -> todHorizonDayR, v -> todHorizonDayR = v);
      key("todHorizonDayG", "the daytime horizon - G channel.", 0.0, 1.0, false, () -> todHorizonDayG, v -> todHorizonDayG = v);
      key("todHorizonDayB", "the daytime horizon - B channel.", 0.0, 1.0, false, () -> todHorizonDayB, v -> todHorizonDayB = v);
      key("todHorizonDuskEnabled", "Enable the dusk horizon.", 0.0, 1.0, true, () -> todHorizonDuskEnabled ? 1.0 : 0.0, v -> todHorizonDuskEnabled = v >= 0.5);
      key("todHorizonDuskR", "the dusk horizon - R channel.", 0.0, 1.0, false, () -> todHorizonDuskR, v -> todHorizonDuskR = v);
      key("todHorizonDuskG", "the dusk horizon - G channel.", 0.0, 1.0, false, () -> todHorizonDuskG, v -> todHorizonDuskG = v);
      key("todHorizonDuskB", "the dusk horizon - B channel.", 0.0, 1.0, false, () -> todHorizonDuskB, v -> todHorizonDuskB = v);
      key(
         "todHorizonNightEnabled",
         "Enable the night horizon.",
         0.0,
         1.0,
         true,
         () -> todHorizonNightEnabled ? 1.0 : 0.0,
         v -> todHorizonNightEnabled = v >= 0.5
      );
      key("todHorizonNightR", "the night horizon - R channel.", 0.0, 1.0, false, () -> todHorizonNightR, v -> todHorizonNightR = v);
      key("todHorizonNightG", "the night horizon - G channel.", 0.0, 1.0, false, () -> todHorizonNightG, v -> todHorizonNightG = v);
      key("todHorizonNightB", "the night horizon - B channel.", 0.0, 1.0, false, () -> todHorizonNightB, v -> todHorizonNightB = v);
      key("todHorizonDawnEnabled", "Enable the dawn horizon.", 0.0, 1.0, true, () -> todHorizonDawnEnabled ? 1.0 : 0.0, v -> todHorizonDawnEnabled = v >= 0.5);
      key("todHorizonDawnR", "the dawn horizon - R channel.", 0.0, 1.0, false, () -> todHorizonDawnR, v -> todHorizonDawnR = v);
      key("todHorizonDawnG", "the dawn horizon - G channel.", 0.0, 1.0, false, () -> todHorizonDawnG, v -> todHorizonDawnG = v);
      key("todHorizonDawnB", "the dawn horizon - B channel.", 0.0, 1.0, false, () -> todHorizonDawnB, v -> todHorizonDawnB = v);
      key("todCloudDayEnabled", "Enable daytime cloud colour.", 0.0, 1.0, true, () -> todCloudDayEnabled ? 1.0 : 0.0, v -> todCloudDayEnabled = v >= 0.5);
      key("todCloudDayR", "daytime cloud colour - R channel.", 0.0, 1.0, false, () -> todCloudDayR, v -> todCloudDayR = v);
      key("todCloudDayG", "daytime cloud colour - G channel.", 0.0, 1.0, false, () -> todCloudDayG, v -> todCloudDayG = v);
      key("todCloudDayB", "daytime cloud colour - B channel.", 0.0, 1.0, false, () -> todCloudDayB, v -> todCloudDayB = v);
      key("todCloudDuskEnabled", "Enable dusk cloud colour.", 0.0, 1.0, true, () -> todCloudDuskEnabled ? 1.0 : 0.0, v -> todCloudDuskEnabled = v >= 0.5);
      key("todCloudDuskR", "dusk cloud colour - R channel.", 0.0, 1.0, false, () -> todCloudDuskR, v -> todCloudDuskR = v);
      key("todCloudDuskG", "dusk cloud colour - G channel.", 0.0, 1.0, false, () -> todCloudDuskG, v -> todCloudDuskG = v);
      key("todCloudDuskB", "dusk cloud colour - B channel.", 0.0, 1.0, false, () -> todCloudDuskB, v -> todCloudDuskB = v);
      key("todCloudNightEnabled", "Enable night cloud colour.", 0.0, 1.0, true, () -> todCloudNightEnabled ? 1.0 : 0.0, v -> todCloudNightEnabled = v >= 0.5);
      key("todCloudNightR", "night cloud colour - R channel.", 0.0, 1.0, false, () -> todCloudNightR, v -> todCloudNightR = v);
      key("todCloudNightG", "night cloud colour - G channel.", 0.0, 1.0, false, () -> todCloudNightG, v -> todCloudNightG = v);
      key("todCloudNightB", "night cloud colour - B channel.", 0.0, 1.0, false, () -> todCloudNightB, v -> todCloudNightB = v);
      key("todCloudDawnEnabled", "Enable dawn cloud colour.", 0.0, 1.0, true, () -> todCloudDawnEnabled ? 1.0 : 0.0, v -> todCloudDawnEnabled = v >= 0.5);
      key("todCloudDawnR", "dawn cloud colour - R channel.", 0.0, 1.0, false, () -> todCloudDawnR, v -> todCloudDawnR = v);
      key("todCloudDawnG", "dawn cloud colour - G channel.", 0.0, 1.0, false, () -> todCloudDawnG, v -> todCloudDawnG = v);
      key("todCloudDawnB", "dawn cloud colour - B channel.", 0.0, 1.0, false, () -> todCloudDawnB, v -> todCloudDawnB = v);
      key("todLightDayEnabled", "Enable daytime light tint.", 0.0, 1.0, true, () -> todLightDayEnabled ? 1.0 : 0.0, v -> todLightDayEnabled = v >= 0.5);
      key("todLightDayR", "daytime light tint - R channel.", 0.0, 1.0, false, () -> todLightDayR, v -> todLightDayR = v);
      key("todLightDayG", "daytime light tint - G channel.", 0.0, 1.0, false, () -> todLightDayG, v -> todLightDayG = v);
      key("todLightDayB", "daytime light tint - B channel.", 0.0, 1.0, false, () -> todLightDayB, v -> todLightDayB = v);
      key("todLightDuskEnabled", "Enable dusk light tint.", 0.0, 1.0, true, () -> todLightDuskEnabled ? 1.0 : 0.0, v -> todLightDuskEnabled = v >= 0.5);
      key("todLightDuskR", "dusk light tint - R channel.", 0.0, 1.0, false, () -> todLightDuskR, v -> todLightDuskR = v);
      key("todLightDuskG", "dusk light tint - G channel.", 0.0, 1.0, false, () -> todLightDuskG, v -> todLightDuskG = v);
      key("todLightDuskB", "dusk light tint - B channel.", 0.0, 1.0, false, () -> todLightDuskB, v -> todLightDuskB = v);
      key("todLightNightEnabled", "Enable night light tint.", 0.0, 1.0, true, () -> todLightNightEnabled ? 1.0 : 0.0, v -> todLightNightEnabled = v >= 0.5);
      key("todLightNightR", "night light tint - R channel.", 0.0, 1.0, false, () -> todLightNightR, v -> todLightNightR = v);
      key("todLightNightG", "night light tint - G channel.", 0.0, 1.0, false, () -> todLightNightG, v -> todLightNightG = v);
      key("todLightNightB", "night light tint - B channel.", 0.0, 1.0, false, () -> todLightNightB, v -> todLightNightB = v);
      key("todLightDawnEnabled", "Enable dawn light tint.", 0.0, 1.0, true, () -> todLightDawnEnabled ? 1.0 : 0.0, v -> todLightDawnEnabled = v >= 0.5);
      key("todLightDawnR", "dawn light tint - R channel.", 0.0, 1.0, false, () -> todLightDawnR, v -> todLightDawnR = v);
      key("todLightDawnG", "dawn light tint - G channel.", 0.0, 1.0, false, () -> todLightDawnG, v -> todLightDawnG = v);
      key("todLightDawnB", "dawn light tint - B channel.", 0.0, 1.0, false, () -> todLightDawnB, v -> todLightDawnB = v);
      key("biomeSkyPlainsEnabled", "Enable the plains sky.", 0.0, 1.0, true, () -> biomeSkyPlainsEnabled ? 1.0 : 0.0, v -> biomeSkyPlainsEnabled = v >= 0.5);
      key("biomeSkyPlainsR", "the plains sky - R channel.", 0.0, 1.0, false, () -> biomeSkyPlainsR, v -> biomeSkyPlainsR = v);
      key("biomeSkyPlainsG", "the plains sky - G channel.", 0.0, 1.0, false, () -> biomeSkyPlainsG, v -> biomeSkyPlainsG = v);
      key("biomeSkyPlainsB", "the plains sky - B channel.", 0.0, 1.0, false, () -> biomeSkyPlainsB, v -> biomeSkyPlainsB = v);
      key("biomeSkyDesertEnabled", "Enable the desert sky.", 0.0, 1.0, true, () -> biomeSkyDesertEnabled ? 1.0 : 0.0, v -> biomeSkyDesertEnabled = v >= 0.5);
      key("biomeSkyDesertR", "the desert sky - R channel.", 0.0, 1.0, false, () -> biomeSkyDesertR, v -> biomeSkyDesertR = v);
      key("biomeSkyDesertG", "the desert sky - G channel.", 0.0, 1.0, false, () -> biomeSkyDesertG, v -> biomeSkyDesertG = v);
      key("biomeSkyDesertB", "the desert sky - B channel.", 0.0, 1.0, false, () -> biomeSkyDesertB, v -> biomeSkyDesertB = v);
      key("biomeSkySnowyEnabled", "Enable the snowy sky.", 0.0, 1.0, true, () -> biomeSkySnowyEnabled ? 1.0 : 0.0, v -> biomeSkySnowyEnabled = v >= 0.5);
      key("biomeSkySnowyR", "the snowy sky - R channel.", 0.0, 1.0, false, () -> biomeSkySnowyR, v -> biomeSkySnowyR = v);
      key("biomeSkySnowyG", "the snowy sky - G channel.", 0.0, 1.0, false, () -> biomeSkySnowyG, v -> biomeSkySnowyG = v);
      key("biomeSkySnowyB", "the snowy sky - B channel.", 0.0, 1.0, false, () -> biomeSkySnowyB, v -> biomeSkySnowyB = v);
      key("biomeSkySwampEnabled", "Enable the swamp sky.", 0.0, 1.0, true, () -> biomeSkySwampEnabled ? 1.0 : 0.0, v -> biomeSkySwampEnabled = v >= 0.5);
      key("biomeSkySwampR", "the swamp sky - R channel.", 0.0, 1.0, false, () -> biomeSkySwampR, v -> biomeSkySwampR = v);
      key("biomeSkySwampG", "the swamp sky - G channel.", 0.0, 1.0, false, () -> biomeSkySwampG, v -> biomeSkySwampG = v);
      key("biomeSkySwampB", "the swamp sky - B channel.", 0.0, 1.0, false, () -> biomeSkySwampB, v -> biomeSkySwampB = v);
      key("biomeSkyJungleEnabled", "Enable the jungle sky.", 0.0, 1.0, true, () -> biomeSkyJungleEnabled ? 1.0 : 0.0, v -> biomeSkyJungleEnabled = v >= 0.5);
      key("biomeSkyJungleR", "the jungle sky - R channel.", 0.0, 1.0, false, () -> biomeSkyJungleR, v -> biomeSkyJungleR = v);
      key("biomeSkyJungleG", "the jungle sky - G channel.", 0.0, 1.0, false, () -> biomeSkyJungleG, v -> biomeSkyJungleG = v);
      key("biomeSkyJungleB", "the jungle sky - B channel.", 0.0, 1.0, false, () -> biomeSkyJungleB, v -> biomeSkyJungleB = v);
      key("biomeSkySavannaEnabled", "Enable the savanna sky.", 0.0, 1.0, true, () -> biomeSkySavannaEnabled ? 1.0 : 0.0, v -> biomeSkySavannaEnabled = v >= 0.5);
      key("biomeSkySavannaR", "the savanna sky - R channel.", 0.0, 1.0, false, () -> biomeSkySavannaR, v -> biomeSkySavannaR = v);
      key("biomeSkySavannaG", "the savanna sky - G channel.", 0.0, 1.0, false, () -> biomeSkySavannaG, v -> biomeSkySavannaG = v);
      key("biomeSkySavannaB", "the savanna sky - B channel.", 0.0, 1.0, false, () -> biomeSkySavannaB, v -> biomeSkySavannaB = v);
      key(
         "biomeSkyBadlandsEnabled",
         "Enable the badlands sky.",
         0.0,
         1.0,
         true,
         () -> biomeSkyBadlandsEnabled ? 1.0 : 0.0,
         v -> biomeSkyBadlandsEnabled = v >= 0.5
      );
      key("biomeSkyBadlandsR", "the badlands sky - R channel.", 0.0, 1.0, false, () -> biomeSkyBadlandsR, v -> biomeSkyBadlandsR = v);
      key("biomeSkyBadlandsG", "the badlands sky - G channel.", 0.0, 1.0, false, () -> biomeSkyBadlandsG, v -> biomeSkyBadlandsG = v);
      key("biomeSkyBadlandsB", "the badlands sky - B channel.", 0.0, 1.0, false, () -> biomeSkyBadlandsB, v -> biomeSkyBadlandsB = v);
      key("biomeSkyOceanEnabled", "Enable the ocean sky.", 0.0, 1.0, true, () -> biomeSkyOceanEnabled ? 1.0 : 0.0, v -> biomeSkyOceanEnabled = v >= 0.5);
      key("biomeSkyOceanR", "the ocean sky - R channel.", 0.0, 1.0, false, () -> biomeSkyOceanR, v -> biomeSkyOceanR = v);
      key("biomeSkyOceanG", "the ocean sky - G channel.", 0.0, 1.0, false, () -> biomeSkyOceanG, v -> biomeSkyOceanG = v);
      key("biomeSkyOceanB", "the ocean sky - B channel.", 0.0, 1.0, false, () -> biomeSkyOceanB, v -> biomeSkyOceanB = v);
      key(
         "biomeSkyMushroomEnabled",
         "Enable the mushroom sky.",
         0.0,
         1.0,
         true,
         () -> biomeSkyMushroomEnabled ? 1.0 : 0.0,
         v -> biomeSkyMushroomEnabled = v >= 0.5
      );
      key("biomeSkyMushroomR", "the mushroom sky - R channel.", 0.0, 1.0, false, () -> biomeSkyMushroomR, v -> biomeSkyMushroomR = v);
      key("biomeSkyMushroomG", "the mushroom sky - G channel.", 0.0, 1.0, false, () -> biomeSkyMushroomG, v -> biomeSkyMushroomG = v);
      key("biomeSkyMushroomB", "the mushroom sky - B channel.", 0.0, 1.0, false, () -> biomeSkyMushroomB, v -> biomeSkyMushroomB = v);
      key("biomeSkyNetherEnabled", "Enable the nether sky.", 0.0, 1.0, true, () -> biomeSkyNetherEnabled ? 1.0 : 0.0, v -> biomeSkyNetherEnabled = v >= 0.5);
      key("biomeSkyNetherR", "the nether sky - R channel.", 0.0, 1.0, false, () -> biomeSkyNetherR, v -> biomeSkyNetherR = v);
      key("biomeSkyNetherG", "the nether sky - G channel.", 0.0, 1.0, false, () -> biomeSkyNetherG, v -> biomeSkyNetherG = v);
      key("biomeSkyNetherB", "the nether sky - B channel.", 0.0, 1.0, false, () -> biomeSkyNetherB, v -> biomeSkyNetherB = v);
      key("biomeSkyEndEnabled", "Enable the end sky.", 0.0, 1.0, true, () -> biomeSkyEndEnabled ? 1.0 : 0.0, v -> biomeSkyEndEnabled = v >= 0.5);
      key("biomeSkyEndR", "the end sky - R channel.", 0.0, 1.0, false, () -> biomeSkyEndR, v -> biomeSkyEndR = v);
      key("biomeSkyEndG", "the end sky - G channel.", 0.0, 1.0, false, () -> biomeSkyEndG, v -> biomeSkyEndG = v);
      key("biomeSkyEndB", "the end sky - B channel.", 0.0, 1.0, false, () -> biomeSkyEndB, v -> biomeSkyEndB = v);
      key("phaseFog45", "Fog density multiplier at phase 4.5.", 0.0, 2.0, false, () -> phaseFog45, v -> phaseFog45 = v);
      key("phaseFog50", "Fog density multiplier at phase 5.", 0.0, 2.0, false, () -> phaseFog50, v -> phaseFog50 = v);
      key("phaseFog60", "Fog density multiplier at phase 6.", 0.0, 2.0, false, () -> phaseFog60, v -> phaseFog60 = v);
      key("phaseFog65", "Fog density multiplier at phase 6.5.", 0.0, 2.0, false, () -> phaseFog65, v -> phaseFog65 = v);
      key("phaseFog70", "Fog density multiplier at phase 7.", 0.0, 2.0, false, () -> phaseFog70, v -> phaseFog70 = v);
      key("stormSkyRange", "How far away the storm still colours the sky, in blocks.", 100.0, 3000.0, false, () -> stormSkyRange, v -> stormSkyRange = v);
      key("stormSkyFalloff", "Fraction of the range held at full strength before easing out.", 0.0, 1.0, false, () -> stormSkyFalloff, v -> stormSkyFalloff = v);
      key("stormSkyCore", "How strongly the black core darkens the dome.", 0.0, 2.0, false, () -> stormSkyCore, v -> stormSkyCore = v);
      key("stormSkySmooth", "How quickly the sky eases toward its target colour.", 0.005, 0.5, false, () -> stormSkySmooth, v -> stormSkySmooth = v);
      key(
         "turquoiseTeeth",
         "Turquoise emissive glow on the Wither Storm's teeth.",
         0.0,
         1.0,
         true,
         () -> turquoiseTeeth ? 1.0 : 0.0,
         v -> turquoiseTeeth = v >= 0.5
      );
      key("turquoiseTeethIntensity", "Brightness of the turquoise teeth.", 0.0, 4.0, false, () -> turquoiseTeethIntensity, v -> turquoiseTeethIntensity = v);
      key(
         "cataclysmHalos",
         "From phase 5.8: the blue-purple halo ring around the area plus the original white halo under the body.",
         0.0,
         1.0,
         true,
         () -> cataclysmHalos ? 1.0 : 0.0,
         v -> cataclysmHalos = v >= 0.5
      );
      key("haloStrength", "Brightness of the cataclysm halo pair.", 0.0, 2.0, false, () -> haloStrength, v -> haloStrength = v);
      key("blackGlare", "The black-purple glare ring hugging the storm's silhouette.", 0.0, 1.0, true, () -> blackGlare ? 1.0 : 0.0, v -> blackGlare = v >= 0.5);
      key("blackGlareStrength", "How dark the rim glare goes.", 0.0, 2.0, false, () -> blackGlareStrength, v -> blackGlareStrength = v);
      key(
         "glareEjecta",
         "Turquoise and green cluster sparks ejecting from the glare ring.",
         0.0,
         1.0,
         true,
         () -> glareEjecta ? 1.0 : 0.0,
         v -> glareEjecta = v >= 0.5
      );
      key("ejectaRate", "How many cluster sparks burst off the rim.", 0.0, 3.0, false, () -> ejectaRate, v -> ejectaRate = v);
      key("ejectaBrightness", "Brightness of the ejecta sparks.", 0.0, 2.0, false, () -> ejectaBrightness, v -> ejectaBrightness = v);
      key(
         "pulseHeartbeat",
         "A deep thump from the nearest storm on every pulse peak.",
         0.0,
         1.0,
         true,
         () -> pulseHeartbeat ? 1.0 : 0.0,
         v -> pulseHeartbeat = v >= 0.5
      );
      key("pulseHeartbeatVolume", "Loudness of the pulse heartbeat.", 0.0, 2.0, false, () -> pulseHeartbeatVolume, v -> pulseHeartbeatVolume = v);
      key("pulseHeartbeatRange", "How far the heartbeat carries.", 128.0, 1024.0, false, () -> pulseHeartbeatRange, v -> pulseHeartbeatRange = v);
      key(
         "phaseFogPalettes",
         "Let the storm's phase recolour the sky: turquoise fog at phase 5, purple-black from phase 5.8 on.",
         0.0,
         1.0,
         true,
         () -> phaseFogPalettes ? 1.0 : 0.0,
         v -> phaseFogPalettes = v >= 0.5
      );
      key("paletteStrength", "How far the phase palettes override your manual colours.", 0.0, 1.0, false, () -> paletteStrength, v -> paletteStrength = v);
      key("turquoiseFogR", "Phase-5 turquoise fog anchor.", 0.0, 1.0, false, () -> turquoiseFogR, v -> turquoiseFogR = v);
      key("turquoiseFogG", "", 0.0, 1.0, false, () -> turquoiseFogG, v -> turquoiseFogG = v);
      key("turquoiseFogB", "", 0.0, 1.0, false, () -> turquoiseFogB, v -> turquoiseFogB = v);
      key("cataclysmFogR", "Phase-5.8+ purple-black fog anchor.", 0.0, 1.0, false, () -> cataclysmFogR, v -> cataclysmFogR = v);
      key("cataclysmFogG", "", 0.0, 1.0, false, () -> cataclysmFogG, v -> cataclysmFogG = v);
      key("cataclysmFogB", "", 0.0, 1.0, false, () -> cataclysmFogB, v -> cataclysmFogB = v);
      key("skyDarkenIntensity", "How much a late-phase storm darkens the sky.", 0.0, 1.0, false, () -> skyDarkenIntensity, v -> skyDarkenIntensity = v);
      key(
         "separateFogColor",
         "Give the fog its own colour instead of the sky's.",
         0.0,
         1.0,
         true,
         () -> separateFogColor ? 1.0 : 0.0,
         v -> separateFogColor = v >= 0.5
      );
      key("fogColorR", "", 0.0, 1.0, false, () -> fogColorR, v -> fogColorR = v);
      key("fogColorG", "", 0.0, 1.0, false, () -> fogColorG, v -> fogColorG = v);
      key("fogColorB", "", 0.0, 1.0, false, () -> fogColorB, v -> fogColorB = v);
      key(
         "stormFog",
         "Storm proximity fog: the closer you get to the storm, the thicker the purple haze closes in around you.",
         0.0,
         1.0,
         true,
         () -> stormFog ? 1.0 : 0.0,
         v -> stormFog = v >= 0.5
      );
      key(
         "stormFogStrength",
         "How thick the storm's proximity fog gets up close (0 = no effect).",
         0.0,
         1.0,
         false,
         () -> stormFogStrength,
         v -> stormFogStrength = v
      );
      key(
         "farLandsHaze",
         "Far-lands haze: the further you travel from the world origin, the thicker the purple haze closes in, giving that lonely Story-Mode far-lands feel.",
         0.0,
         1.0,
         true,
         () -> farLandsHaze ? 1.0 : 0.0,
         v -> farLandsHaze = v >= 0.5
      );
      key(
         "farLandsDistance",
         "Blocks from the world origin at which the far-lands haze starts to close in.",
         500.0,
         100000.0,
         true,
         () -> farLandsDistance,
         v -> farLandsDistance = v
      );
      key("farLandsStrength", "How thick the far-lands haze gets at extreme distance.", 0.0, 1.0, false, () -> farLandsStrength, v -> farLandsStrength = v);
      key(
         "biomeFogTint",
         "Biome-tinted storm fog: the storm's purple fog takes on the colour of the biome it is devouring.",
         0.0,
         1.0,
         true,
         () -> biomeFogTint ? 1.0 : 0.0,
         v -> biomeFogTint = v >= 0.5
      );
      key(
         "biomeFogStrength", "How strongly the storm fog blends toward the biome's colour.", 0.0, 1.0, false, () -> biomeFogStrength, v -> biomeFogStrength = v
      );
      key("skyDarkenR", "", 0.0, 1.0, false, () -> skyDarkenR, v -> skyDarkenR = v);
      key("skyDarkenG", "", 0.0, 1.0, false, () -> skyDarkenG, v -> skyDarkenG = v);
      key("skyDarkenB", "", 0.0, 1.0, false, () -> skyDarkenB, v -> skyDarkenB = v);
      key(
         "skyDarkenLighting",
         "How much the gloom darkens the world's actual lighting, not just the sky.",
         0.0,
         1.0,
         false,
         () -> skyDarkenLighting,
         v -> skyDarkenLighting = v
      );
      key(
         "cloudDarkenStrength",
         "How far the clouds are dragged toward the colour below.",
         0.0,
         1.0,
         false,
         () -> cloudDarkenStrength,
         v -> cloudDarkenStrength = v
      );
      key("cloudColorR", "", 0.0, 1.0, false, () -> cloudColorR, v -> cloudColorR = v);
      key("cloudColorG", "", 0.0, 1.0, false, () -> cloudColorG, v -> cloudColorG = v);
      key("cloudColorB", "", 0.0, 1.0, false, () -> cloudColorB, v -> cloudColorB = v);
      key("sunGlow", "Burn the sun yellow through the gloom. Nothing under a clear sky.", 0.0, 1.0, true, () -> sunGlow ? 1.0 : 0.0, v -> sunGlow = v >= 0.5);
      key("sunGlowStrength", "", 0.0, 3.0, false, () -> sunGlowStrength, v -> sunGlowStrength = v);
      key("sunGlowR", "", 0.0, 1.0, false, () -> sunGlowR, v -> sunGlowR = v);
      key("sunGlowG", "", 0.0, 1.0, false, () -> sunGlowG, v -> sunGlowG = v);
      key("sunGlowB", "", 0.0, 1.0, false, () -> sunGlowB, v -> sunGlowB = v);
      key(
         "stormModelShading",
         "Darken the storm's undersides and crevices so its mass reads as solid. Practically free. Off under a shader pack.",
         0.0,
         1.0,
         true,
         () -> stormModelShading ? 1.0 : 0.0,
         v -> stormModelShading = v >= 0.5
      );
      key(
         "reverseShading",
         "Light the body from behind and below, the way MCSM does, so the faces turned toward you are the dark ones.",
         0.0,
         1.0,
         true,
         () -> reverseShading ? 1.0 : 0.0,
         v -> reverseShading = v >= 0.5
      );
      key("stormMusic", "Play the storm's own score.", 0.0, 1.0, true, () -> stormMusic ? 1.0 : 0.0, v -> stormMusic = v >= 0.5);
      key("stormMusicVolume", "", 0.0, 2.0, false, () -> stormMusicVolume, v -> stormMusicVolume = v);
      key("stormMusicRange", "How far from the storm the score can be heard, in blocks.", 64.0, 1024.0, false, () -> stormMusicRange, v -> stormMusicRange = v);
      key(
         "stormMusicCaveCutoff",
         "How much sky the spot you are standing in needs before the score plays. Deep caves get silence. 0 plays everywhere.",
         0.0,
         15.0,
         false,
         () -> stormMusicCaveCutoff,
         v -> stormMusicCaveCutoff = v
      );
      key(
         "shadowMapResolution",
         "How big the shadow's own map is. Halving it quarters what the shadow costs and quarters the memory it takes, for a softer, steppier edge. Snaps to powers of two.",
         512.0,
         8192.0,
         false,
         () -> shadowMapResolution,
         v -> shadowMapResolution = snapToPowerOfTwo(v)
      );
      key(
         "stormShadow",
         "The storm casts a real sun shadow. Off under a shader pack.",
         0.0,
         1.0,
         true,
         () -> stormShadow ? 1.0 : 0.0,
         v -> stormShadow = v >= 0.5
      );
      key(
         "stormSelfShadow",
         "The storm shades its own body. Independent of the shadow it throws on the world.",
         0.0,
         1.0,
         true,
         () -> stormSelfShadow ? 1.0 : 0.0,
         v -> stormSelfShadow = v >= 0.5
      );
      key(
         "stormShadingContrast",
         "How much harder the storm's own shading is pushed than the world's.",
         0.0,
         2.0,
         false,
         () -> stormShadingContrast,
         v -> stormShadingContrast = v
      );
      key("stormShadowStrength", "", 0.0, 1.0, false, () -> stormShadowStrength, v -> stormShadowStrength = v);
      key(
         "stormShadowTerrain",
         "Resolve the ground at 2 blocks when deciding what is indoors. Off uses an 8-block grid: cheaper, rougher at cave mouths.",
         0.0,
         1.0,
         true,
         () -> stormShadowTerrain ? 1.0 : 0.0,
         v -> stormShadowTerrain = v >= 0.5
      );
      key("stormShadowR", "", 0.0, 1.0, false, () -> stormShadowR, v -> stormShadowR = v);
      key("stormShadowG", "", 0.0, 1.0, false, () -> stormShadowG, v -> stormShadowG = v);
      key("stormShadowB", "", 0.0, 1.0, false, () -> stormShadowB, v -> stormShadowB = v);
      key("glowStrength", "How bright the teeth burn. This is what the bloom feeds on.", 0.0, 1.0, false, () -> glowStrength, v -> glowStrength = v);
      key("eyeColorR", "", 0.0, 1.0, false, () -> eyeColorR, v -> eyeColorR = v);
      key("eyeColorG", "", 0.0, 1.0, false, () -> eyeColorG, v -> eyeColorG = v);
      key("eyeColorB", "", 0.0, 1.0, false, () -> eyeColorB, v -> eyeColorB = v);
      key("stormGlowStrength", "How brightly the silhouette glows at night. 0 is off.", 0.0, 2.0, false, () -> stormGlowStrength, v -> stormGlowStrength = v);
      keyCycle(
         "bloomStrength",
         "Screen-space glow over the finished image while a storm is near. Costs a few full-screen passes per frame.",
         BLOOM_LABELS,
         () -> bloomStrength,
         v -> bloomStrength = Math.round(v)
      );
      key(
         "bloomMaskToStorm",
         "Keep the glow on the storm's teeth and eye. Off blooms every bright thing on screen.",
         0.0,
         1.0,
         true,
         () -> bloomMaskToStorm ? 1.0 : 0.0,
         v -> bloomMaskToStorm = v >= 0.5
      );
      key("beamOpacity", "How solid the beam is. Lower lets you see the world through it.", 0.0, 2.0, false, () -> beamOpacity, v -> beamOpacity = v);
      key("beamEndFade", "How far the beam fades out where it meets the ground.", 0.0, 1.0, false, () -> beamEndFade, v -> beamEndFade = v);
      key("beamColorR", "", 0.0, 1.0, false, () -> beamColorR, v -> beamColorR = v);
      key("beamColorG", "", 0.0, 1.0, false, () -> beamColorG, v -> beamColorG = v);
      key("beamColorB", "", 0.0, 1.0, false, () -> beamColorB, v -> beamColorB = v);
      key(
         "beamInnerFaces",
         "Draw the beam's inner walls. Off hides the inner corners.",
         0.0,
         1.0,
         true,
         () -> beamInnerFaces ? 1.0 : 0.0,
         v -> beamInnerFaces = v >= 0.5
      );
      key("impactLight", "Light the ground where a beam lands.", 0.0, 1.0, true, () -> impactLight ? 1.0 : 0.0, v -> impactLight = v >= 0.5);
      key("impactLightSize", "How far the pool spreads. The centre stays as bright.", 0.25, 4.0, false, () -> impactLightSize, v -> impactLightSize = v);
      key("impactLightBrightness", "", 0.0, 2.0, false, () -> impactLightBrightness, v -> impactLightBrightness = v);
      key(
         "impactLightRange",
         "How far away a beam can be and still light the ground, in blocks.",
         32.0,
         1024.0,
         false,
         () -> impactLightRange,
         v -> impactLightRange = v
      );
      key(
         "impactLightUseBeamColor",
         "Off gives plain white light.",
         0.0,
         1.0,
         true,
         () -> impactLightUseBeamColor ? 1.0 : 0.0,
         v -> impactLightUseBeamColor = v >= 0.5
      );
      key("bossbarNotched", "", 0.0, 1.0, true, () -> bossbarNotched ? 1.0 : 0.0, v -> bossbarNotched = v >= 0.5);
      keyCycle("bossbarColor", "", BOSSBAR_COLOR_LABELS, () -> bossbarColor, v -> bossbarColor = Math.round(v));
      keyCycle(
         "nameStyle",
         "Classic: Commanded Wither, then The Wither Storm. Cracker's: Wither Storm for both. Legacy: Wither (Wither Storm), then The Wither Storm.",
         NAME_STYLE_LABELS,
         () -> nameStyle,
         v -> nameStyle = Math.round(v)
      );
      key("phaseAnim", "Apply a phase-driven animation profile to storm tentacles.", 0.0, 1.0, true, () -> phaseAnim ? 1.0 : 0.0, v -> phaseAnim = v >= 0.5);
      key("phaseAnimStrength", "Strength of the per-phase animation profile.", 0.0, 2.0, false, () -> phaseAnimStrength, v -> phaseAnimStrength = v);
      key(
         "tentacleIdleSpeed",
         "Speed of the sine that drives every tentacle idle. 1 is stock; above about 2 the limbs read as flailing rather than swimming.",
         0.1,
         3.0,
         false,
         () -> tentacleIdleSpeed,
         v -> tentacleIdleSpeed = v
      );
      key(
         "tentacleWaveTravel",
         "How much the bend lags from one bone to the next. 0 bends each limb as a single arc with no travelling wave at all.",
         0.0,
         2.0,
         false,
         () -> tentacleWaveTravel,
         v -> tentacleWaveTravel = v
      );
      key(
         "tentacleCurlDepth",
         "How far each small-tentacle bone bends. The bends compound down the chain, so small changes here are large on screen.",
         0.25,
         2.5,
         false,
         () -> tentacleCurlDepth,
         v -> tentacleCurlDepth = v
      );
      key(
         "tentacleCrossAxis",
         "How much of the small tentacles' curl goes on the second bend axis. This is what turns a nod into a figure-eight. 0 makes them nod only.",
         0.0,
         1.5,
         false,
         () -> tentacleCrossAxis,
         v -> tentacleCrossAxis = v
      );
      key(
         "bigTentacleCurlDepth",
         "How far each bone of the big pair bends. Their ramp is squared, so the base stays near still and the movement lives out at the tip.",
         0.25,
         2.5,
         false,
         () -> bigTentacleCurlDepth,
         v -> bigTentacleCurlDepth = v
      );
      key(
         "bigTentacleHangBreath",
         "How much the big pair settles and rises under its own weight, on a clock slower than the travelling wave. 0 makes them rigid props.",
         0.0,
         3.0,
         false,
         () -> bigTentacleHangBreath,
         v -> bigTentacleHangBreath = v
      );
      key(
         "bigTentacleSideSweep",
         "How far the big pair opens out to the sides past its hang.",
         0.0,
         2.0,
         false,
         () -> bigTentacleSideSweep,
         v -> bigTentacleSideSweep = v
      );
      key(
         "lateGrowthWrithe",
         "Idle speed multiplier applied to every tentacle from phase 5.8.",
         0.5,
         3.0,
         false,
         () -> lateGrowthWrithe,
         v -> lateGrowthWrithe = v
      );
      key(
         "trailerShadows",
         "Enable cinematic deep trailer shadow casting under the colossal storm body.",
         0.0,
         1.0,
         true,
         () -> trailerShadows ? 1.0 : 0.0,
         v -> trailerShadows = v >= 0.5
      );
      key(
         "groundShakingTremors",
         "Camera shake and earthquake screen rumbles during tentacle slams, roars, and explosions.",
         0.0,
         1.0,
         true,
         () -> groundShakingTremors ? 1.0 : 0.0,
         v -> groundShakingTremors = v >= 0.5
      );
      key(
         "screenTremorIntensity",
         "Earthquake screen rumble and camera tremor multiplier.",
         0.1,
         4.0,
         false,
         () -> screenTremorIntensity,
         v -> screenTremorIntensity = v
      );
      key(
         "stormProximityVignette",
         "Atmospheric dark purple vignette encircling the screen edges when near the storm.",
         0.0,
         1.0,
         true,
         () -> stormProximityVignette ? 1.0 : 0.0,
         v -> stormProximityVignette = v >= 0.5
      );
      key("vignetteIntensity", "Dark storm vignette intensity multiplier.", 0.1, 3.0, false, () -> vignetteIntensity, v -> vignetteIntensity = v);
      key(
         "sicknessVeinOverlay",
         "Creeping necrotic vein HUD overlay when afflicted with Wither Sickness.",
         0.0,
         1.0,
         true,
         () -> sicknessVeinOverlay ? 1.0 : 0.0,
         v -> sicknessVeinOverlay = v >= 0.5
      );
      key(
         "sicknessVeinIntensity",
         "Strength and visibility of creeping necrotic veins.",
         0.1,
         3.0,
         false,
         () -> sicknessVeinIntensity,
         v -> sicknessVeinIntensity = v
      );
      key(
         "chromaticGlitchStrength",
         "Screen glitch, chromatic distortion, and visual shockwaves during big roars.",
         0.0,
         3.0,
         false,
         () -> chromaticGlitchStrength,
         v -> chromaticGlitchStrength = v
      );
      key(
         "storyModeBossbar",
         "Story Mode boss health bar with purple gem framing and phase runes.",
         0.0,
         1.0,
         true,
         () -> storyModeBossbar ? 1.0 : 0.0,
         v -> storyModeBossbar = v >= 0.5
      );
      key(
         "storyModeTitleScreen",
         "Story Mode atmospheric title screen banner and quick-launch buttons.",
         0.0,
         1.0,
         true,
         () -> storyModeTitleScreen ? 1.0 : 0.0,
         v -> storyModeTitleScreen = v >= 0.5
      );
      key(
         "cloudDeckLayer",
         "Minecraft Story Mode volumetric cloud deck shader overrides.",
         0.0,
         1.0,
         true,
         () -> cloudDeckLayer ? 1.0 : 0.0,
         v -> cloudDeckLayer = v >= 0.5
      );
      key(
         "regionalBiomeFog",
         "Regional atmospheric biome fog transition system across 6 world regions.",
         0.0,
         1.0,
         true,
         () -> regionalBiomeFog ? 1.0 : 0.0,
         v -> regionalBiomeFog = v >= 0.5
      );
      key(
         "purpleLightningSparks",
         "Ambient purple lightning electrical discharges around the storm crown.",
         0.0,
         1.0,
         true,
         () -> purpleLightningSparks ? 1.0 : 0.0,
         v -> purpleLightningSparks = v >= 0.5
      );
      key(
         "debrisDustParticles",
         "Flying debris dust, block splinters, and smoke particle density.",
         0.0,
         5.0,
         false,
         () -> debrisDustParticles,
         v -> debrisDustParticles = v
      );
      key("headEyeGlow", "Colossal glowing eye beams and core eye lens flare glow.", 0.0, 1.0, true, () -> headEyeGlow ? 1.0 : 0.0, v -> headEyeGlow = v >= 0.5);
      key(
         "volumetricFogDensity",
         "Volumetric fog, mist, and atmospheric haze density.",
         0.0,
         4.0,
         false,
         () -> volumetricFogDensity,
         v -> volumetricFogDensity = v
      );
      key(
         "dynamicScreenShake",
         "Subtle footsteps ground tremor when the storm looms overhead.",
         0.0,
         1.0,
         true,
         () -> dynamicScreenShake ? 1.0 : 0.0,
         v -> dynamicScreenShake = v >= 0.5
      );
      key(
         "tentaclePhysics",
         "Replace the hand-authored idle with a verlet rope simulation. It reacts to the storm's own motion, and it can settle into poses the authored idle never produces.",
         0.0,
         1.0,
         true,
         () -> tentaclePhysics ? 1.0 : 0.0,
         v -> tentaclePhysics = v >= 0.5
      );
      key(
         "verletGravity",
         "Downward pull per step. These limbs are meant to hold their own shape, so the stock value is near zero.",
         0.0,
         0.5,
         false,
         () -> verletGravity,
         v -> verletGravity = v
      );
      key("verletSway", "How hard the rope reacts to the storm's own movement.", 0.0, 8.0, false, () -> verletSway, v -> verletSway = v);
      key(
         "verletDamping",
         "How much speed survives each step. Higher is heavier and slower to settle; past about 0.97 it never settles at all.",
         0.6,
         0.99,
         false,
         () -> verletDamping,
         v -> verletDamping = v
      );
      key("verletWrithe", "Strength of the writhe pushed along each rope.", 0.0, 0.5, false, () -> verletWrithe, v -> verletWrithe = v);
      key("verletWritheSpeed", "How fast that writhe travels.", 0.0, 0.2, false, () -> verletWritheSpeed, v -> verletWritheSpeed = v);
      key(
         "yawSmoothTime",
         "Seconds the body takes to settle onto a new heading. Vanilla ships rotation as a byte, so some smoothing is needed or the body clicks round in 1.4 degree steps. Higher is smoother and laggier.",
         0.05,
         2.0,
         false,
         () -> yawSmoothTime,
         v -> yawSmoothTime = v
      );
      key(
         "yawSnapDegrees",
         "Past this much error the smoothing steps aside and the body simply arrives. Meant for teleports and respawns, not for turning.",
         5.0,
         180.0,
         false,
         () -> yawSnapDegrees,
         v -> yawSnapDegrees = v
      );
      key("bodyLeanGain", "Multiplier on the storm's forward lean into its travel.", 0.0, 2.0, false, () -> bodyLeanGain, v -> bodyLeanGain = v);
      key("bodyBankGain", "Multiplier on how far the storm banks into its turns.", 0.0, 2.0, false, () -> bodyBankGain, v -> bodyBankGain = v);
      key(
         "growthSmoothRate",
         "How fast the drawn back growth chases the real one, per second. The synced phase only moves in jumps, so this is what hides the steps.",
         0.2,
         6.0,
         false,
         () -> growthSmoothRate,
         v -> growthSmoothRate = v
      );
      key(
         "changeoverShake",
         "Multiplier on the rattle as the wither shakes itself into the storm.",
         0.0,
         2.0,
         false,
         () -> changeoverShake,
         v -> changeoverShake = v
      );
      key(
         "jawLagGain",
         "How far a jaw swings behind the skull when a head whips round. Most of what sells the weight of a head this size.",
         0.0,
         2.0,
         false,
         () -> jawLagGain,
         v -> jawLagGain = v
      );
      key("jawLagMax", "The most a jaw is ever allowed to trail, in degrees.", 0.0, 48.0, false, () -> jawLagMax, v -> jawLagMax = v);
      key("jawLagCatchup", "How quickly a trailing jaw catches back up.", 0.02, 1.0, false, () -> jawLagCatchup, v -> jawLagCatchup = v);
      key(
         "legacyDistantRenderer",
         "Use the old fogged distant renderer instead of the fogless one. Distant storms come out washed out rather than crisp.",
         0.0,
         1.0,
         true,
         () -> legacyDistantRenderer ? 1.0 : 0.0,
         v -> legacyDistantRenderer = v >= 0.5
      );
      key(
         "optimizeDistantAnimations",
         "Step distant storm animations at tick rate. Cheaper, and choppier.",
         0.0,
         1.0,
         true,
         () -> optimizeDistantAnimations ? 1.0 : 0.0,
         v -> optimizeDistantAnimations = v >= 0.5
      );
      key(
         "stormBackfaceCull",
         "WARNING: very inconsistent. On paper the body draws both sides of every face, so dropping the far ones is close to half the pixel work. In practice the win depends entirely on how much of your screen the storm fills, and it can be nothing at all. Anything single-sided in the model becomes a hole. Measure it before you keep it.",
         0.0,
         1.0,
         true,
         () -> stormBackfaceCull ? 1.0 : 0.0,
         v -> stormBackfaceCull = v >= 0.5
      );
      key(
         "shadowCullBackFaces",
         "Skip away-facing surfaces when building the shadow map. They cannot change the shadow's shape, so this roughly halves the work.",
         0.0,
         1.0,
         true,
         () -> shadowCullBackFaces ? 1.0 : 0.0,
         v -> shadowCullBackFaces = v >= 0.5
      );
      key(
         "stormShadowSoftEdge",
         "Soften the shadow's edge with nine samples per pixel instead of one. This scales with your RESOLUTION, not with the storm, and it is the largest single shadow saving there is.",
         0.0,
         1.0,
         true,
         () -> stormShadowSoftEdge ? 1.0 : 0.0,
         v -> stormShadowSoftEdge = v >= 0.5
      );
      key(
         "stormShadowHeightmap",
         "WARNING: very inconsistent. The coarsest ground grid there is, meant for the cheapest possible setup, but the grid is rarely what your frame rate is spent on, so this often changes nothing while roughening every cave mouth and cliff edge. Try the resolution slider first.",
         0.0,
         1.0,
         true,
         () -> stormShadowHeightmap ? 1.0 : 0.0,
         v -> stormShadowHeightmap = v >= 0.5
      );
      key(
         "stormGlowFlip",
         "Only needed if the night glow washes over the storm instead of ringing it. That means the model's faces arrive wound the other way round.",
         0.0,
         1.0,
         true,
         () -> stormGlowFlip ? 1.0 : 0.0,
         v -> stormGlowFlip = v >= 0.5
      );
      keyCycle(
         "bloomDebug",
         "Draw one stage of the bloom pipeline over the screen instead of compositing. Stage 2 must show terrain and the body; all black means nothing was captured. Stage 4 marks visible teeth green and occluded ones red.",
         BLOOM_DEBUG_LABELS,
         () -> bloomDebug,
         v -> bloomDebug = Math.round(v)
      );
      key(
         "stormRenderStats",
         "Log what the storm drew each second: submits, cubes, vertices, shadow vertices.",
         0.0,
         1.0,
         true,
         () -> stormRenderStats ? 1.0 : 0.0,
         v -> stormRenderStats = v >= 0.5
      );
      key(
         "bowelsFrameHud",
         "Show the Bowels frame readout. Only draws while you are in the Bowels.",
         0.0,
         1.0,
         true,
         () -> bowelsFrameHud ? 1.0 : 0.0,
         v -> bowelsFrameHud = v >= 0.5
      );

      for (net.dabicco.witherstormmod.config.DabyWSClientConfig.Key k : KEYS.values()) {
         DEFAULTS.put(k.name(), k.get().getAsDouble());
      }

      loadedVersion = 13;
      PRESET_MCSM = Map.ofEntries(
         Map.entry("reverseShading", 1.0),
         Map.entry("bloomStrength", 2.0),
         Map.entry("beamOpacity", 0.6),
         Map.entry("beamColorR", 0.3),
         Map.entry("beamColorG", 0.22),
         Map.entry("beamColorB", 1.0),
         Map.entry("stormStars", 1.0),
         Map.entry("stormCloudDeck", 1.0),
         Map.entry("atmospherePulse", 1.0),
         Map.entry("cataclysmHalos", 1.0),
         Map.entry("blackGlare", 1.0),
         Map.entry("glareEjecta", 1.0),
         Map.entry("debrisSize", 1.8),
         Map.entry("phaseFogPalettes", 1.0),
         Map.entry("stormSkin", 1.0),
         Map.entry("storyModeSky", 1.0),
         Map.entry("storyModeSkyStrength", 0.85),
         Map.entry("storyModeFogStrength", 0.15),
         Map.entry("storyModeClouds", 1.0),
         Map.entry("storyModeCloudStrength", 1.0),
         Map.entry("storyModeLighting", 1.0),
         Map.entry("storyModeLightingStrength", 0.7),
         Map.entry("turquoiseTeeth", 1.0),
         Map.entry("turquoiseTeethIntensity", 1.6),
         Map.entry("stormBackdrop", 1.0),
         Map.entry("stormBackdropStrength", 1.0)
      );
      PRESET_LEGACY = Map.of("reverseShading", 0.0, "bloomStrength", 1.0, "beamOpacity", 0.74, "beamColorR", 0.52, "beamColorG", 0.46, "beamColorB", 1.0);
      PRESET_CINEMATIC = Map.ofEntries(
         Map.entry("reverseShading", 1.0),
         Map.entry("bloomStrength", 2.0),
         Map.entry("beamOpacity", 0.6),
         Map.entry("beamColorR", 0.3),
         Map.entry("beamColorG", 0.22),
         Map.entry("beamColorB", 1.0),
         Map.entry("stormSkin", 1.0),
         Map.entry("stormStars", 2.0),
         Map.entry("stormCloudDeck", 2.0),
         Map.entry("atmospherePulse", 1.0),
         Map.entry("pulseStrength", 1.3),
         Map.entry("cataclysmHalos", 1.0),
         Map.entry("haloStrength", 1.2),
         Map.entry("blackGlare", 1.0),
         Map.entry("glareEjecta", 1.0),
         Map.entry("ejectaRate", 1.4),
         Map.entry("pulseHeartbeat", 1.0),
         Map.entry("debrisSize", 2.0),
         Map.entry("phaseFogPalettes", 1.0),
         Map.entry("paletteStrength", 1.0)
      );
      PRESET_NETFLIX = Map.ofEntries(
         Map.entry("reverseShading", 1.0),
         Map.entry("bloomStrength", 1.35),
         Map.entry("beamOpacity", 0.7),
         Map.entry("beamColorR", 0.42),
         Map.entry("beamColorG", 0.3),
         Map.entry("beamColorB", 0.98),
         Map.entry("stormSkin", 1.0),
         Map.entry("stormStars", 1.0),
         Map.entry("stormCloudDeck", 1.0),
         Map.entry("atmospherePulse", 1.0),
         Map.entry("pulseStrength", 0.85),
         Map.entry("cataclysmHalos", 1.0),
         Map.entry("haloStrength", 0.9),
         Map.entry("blackGlare", 1.0),
         Map.entry("glareEjecta", 1.0),
         Map.entry("ejectaRate", 1.1),
         Map.entry("debrisSize", 1.5),
         Map.entry("phaseFogPalettes", 1.0),
         Map.entry("storyModeSky", 1.0),
         Map.entry("storyModeSkyStrength", 0.7),
         Map.entry("storyModeFogStrength", 0.12),
         Map.entry("storyModeClouds", 1.0),
         Map.entry("storyModeLighting", 1.0),
         Map.entry("storyModeLightingStrength", 0.55),
         Map.entry("turquoiseTeeth", 1.0),
         Map.entry("turquoiseTeethIntensity", 1.3),
         Map.entry("stormBackdrop", 1.0),
         Map.entry("stormBackdropStrength", 0.85)
      );
      GSON = new GsonBuilder().setPrettyPrinting().create();
   }

   public record Key(String name, String description, double min, double max, boolean toggle, String[] cycleLabels, DoubleSupplier get, DoubleConsumer set) {
      public double clamp(double v) {
         return Math.max(this.min, Math.min(this.max, v));
      }

      public boolean cycle() {
         return this.cycleLabels != null;
      }
   }
}
