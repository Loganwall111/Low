package net.dabicco.witherstormmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
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
   public static double mirrorBackDetail = (double)1.0F;
   public static final boolean useNewFormidibomb = false;
   public static boolean stormAmbience = true;
   public static boolean beamHum = true;
   public static double ambienceVolume = (double)1.0F;
   public static double headSoundsVolume = (double)1.0F;
   public static double beamSoundsVolume = (double)1.0F;
   public static boolean beamDeactivateSound = true;
   public static double beamHumVolume = (double)1.0F;
   public static double beamHumRange = (double)48.0F;
   public static boolean devourerDebrisGlow = true;
   public static boolean infectedMobSound = true;
   public static double infectedMobSoundVolume = (double)1.0F;
   public static double beamEndFade = 0.55;
   public static double debrisAmount = (double)2.0F;
   public static double fogColorR = 0.19;
   public static double fogColorG = 0.07;
   public static double fogColorB = 0.275;
   public static boolean stormFog = false;
   public static double stormFogStrength = (double)0.85F;
   public static boolean farLandsHaze = false;
   public static double farLandsDistance = (double)4000.0F;
   public static double farLandsStrength = (double)0.8F;
   public static boolean biomeFogTint = false;
   public static double biomeFogStrength = (double)0.5F;
   public static boolean separateFogColor = true;
   public static double skyDarkenR = 0.126;
   public static double skyDarkenG = 0.055;
   public static double skyDarkenB = 0.194;
   public static double skyDarkenIntensity = 0.6;
   public static double eyeColorR = 0.75;
   public static double eyeColorG = 0.2;
   public static double eyeColorB = (double)1.0F;
   public static double teethColorR = 0.45;
   public static double teethColorG = (double)1.0F;
   public static double teethColorB = (double)1.0F;
   public static double stormGlowStrength = (double)1.0F;
   public static boolean stormGlowFlip = false;
   public static double beamColorR = 0.35;
   public static double beamColorG = 0.1;
   public static double beamColorB = 0.95;
   public static boolean stormModelShading = true;
   public static boolean stormBackfaceCull = false;
   public static boolean stormRenderStats = false;
   public static double shadowMapResolution = (double)4096.0F;
   public static boolean stormMusic = true;
   public static double stormMusicVolume = (double)1.0F;
   public static double stormMusicRange = (double)256.0F;
   public static double stormMusicCaveCutoff = (double)4.0F;
   public static boolean stormShadow = true;
   public static boolean stormShadowSoftEdge = true;
   public static boolean stormShadowTerrain = true;
   public static boolean stormShadowHeightmap = false;
   public static boolean stormSelfShadow = true;
   public static boolean shadowCullBackFaces = true;
   public static boolean bowelsFrameHud = false;
   public static double stormShadingContrast = (double)0.75F;
   public static boolean sunGlow = true;
   public static double sunGlowStrength = 2.2;
   public static double sunGlowR = (double)1.0F;
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
   public static double beamNightBoost = 1.35;
   public static boolean impactLight = true;
   public static double impactLightSize = (double)1.0F;
   public static double impactLightBrightness = (double)1.0F;
   public static double impactLightRange = (double)512.0F;
   public static boolean impactLightUseBeamColor = true;
   public static double glowStrength = (double)1.0F;
   public static boolean reverseShading = true;
   public static double bloomStrength = (double)2.0F;
   public static final String[] BLOOM_LABELS = new String[]{"Off", "Subtle", "Normal", "Strong"};
   public static double bloomDebug = (double)0.0F;
   public static final String[] BLOOM_DEBUG_LABELS = new String[]{"Off", "1 Source", "2 Scene Depth", "3 Bloom Depth", "4 Depth Mask", "5 Blur H", "6 Blur V", "7 Wide H", "8 Final Bloom", "9 UV Align"};
   public static boolean bloomMaskToStorm = true;
   public static double effectsPreset = (double)1.0F;
   public static final String[] PRESET_LABELS = new String[]{"Custom", "Minecraft: Story Mode OG", "Minecraft: Story Mode Netflix", "Legacy Java", "Cinematic"};
   public static boolean beamInnerFaces = false;
   public static double debrisSize = (double)1.0F;
   public static boolean vortexDebris = true;
   public static double vortexStrength = (double)1.0F;
   public static double stormSkin = (double)1.0F;
   public static final String[] SKIN_LABELS = new String[]{"Classic", "Minecraft: Story Mode OG (Default)", "Minecraft: Story Mode Netflix"};
   public static double stormStars = (double)1.0F;
   public static final String[] STAR_LABELS = new String[]{"Off", "Storm Nights", "Every Night"};
   public static double starDensity = (double)1.0F;
   public static double starTwinkleSpeed = (double)1.0F;
   public static double starBrightness = (double)1.0F;
   public static double stormCloudDeck = (double)1.0F;
   public static final String[] CLOUD_DECK_LABELS = new String[]{"Off", "Subtle", "Dense"};
   public static double stormCloudCoverage = (double)1.0F;
   public static double stormCloudAltitude = (double)0.0F;
   public static double stormCloudPaletteMix = 0.85;
   public static boolean atmospherePulse = true;
   public static boolean stormAtmosphere = true;
   public static boolean stormBlobFX = true;
   public static double pulseStrength = (double)1.0F;
   public static double pulsePeriod = (double)4.0F;
   public static double pulseSize = (double)1.0F;
   public static boolean cataclysmHalos = true;
   public static double haloStrength = (double)1.0F;
   public static boolean haloRings = true;
   public static double haloRingStrength = (double)1.0F;
   public static boolean shieldDome = false;
   public static boolean blackGlare = true;
   public static double blackGlareStrength = (double)1.0F;
   public static boolean glareEjecta = true;
   public static double ejectaRate = (double)1.0F;
   public static double ejectaBrightness = (double)1.0F;
   public static boolean pulseHeartbeat = false;
   public static double pulseHeartbeatVolume = (double)1.0F;
   public static double pulseHeartbeatRange = (double)512.0F;
   public static boolean phaseFogPalettes = true;
   public static double paletteStrength = 0.85;
   public static double turquoiseFogR = 0.031;
   public static double turquoiseFogG = 0.42;
   public static double turquoiseFogB = 0.36;
   public static double cataclysmFogR = 0.055;
   public static double cataclysmFogG = 0.028;
   public static double cataclysmFogB = 0.10;
   public static boolean configOpened = false;
   public static boolean bossbarNotched = true;
   public static boolean clusterVolumetricLighting = false;
   public static double bossbarColor = (double)5.0F;
   public static final String[] BOSSBAR_COLOR_LABELS = new String[]{"Pink", "Blue", "Red", "Green", "Yellow", "Purple", "White"};
   public static double tentacleIdleSpeed = (double)1.0F;
   public static double tentacleWaveTravel = (double)1.0F;
   public static double tentacleCurlDepth = (double)1.0F;
   public static double tentacleCrossAxis = 0.8;
   public static double bigTentacleCurlDepth = (double)1.0F;
   public static double bigTentacleHangBreath = (double)1.0F;
   public static double bigTentacleSideSweep = (double)1.0F;
   public static double lateGrowthWrithe = 1.35;
   public static double verletGravity = 0.05;
   public static double verletSway = 2.2;
   public static double verletDamping = 0.9;
   public static double verletWrithe = 0.1;
   public static double verletWritheSpeed = 0.035;
   public static double yawSmoothTime = 0.55;
   public static double yawSnapDegrees = (double)45.0F;
   public static double growthSmoothRate = 1.6;
   public static double changeoverShake = (double)1.0F;
   public static double jawLagGain = 0.55;
   public static double jawLagMax = (double)16.0F;
   public static double jawLagCatchup = 0.22;
   public static double bodyLeanGain = (double)1.0F;
   public static double bodyBankGain = (double)1.0F;
   public static double nameStyle = (double)0.0F;
   public static final String[] NAME_STYLE_LABELS = new String[]{"Classic", "Cracker's", "Legacy"};
   public static final Map<String, Key> KEYS = new LinkedHashMap();
   private static final Map<String, Double> DEFAULTS = new LinkedHashMap();
   private static final int CONFIG_VERSION = 14;
   private static int loadedVersion;
   public static final String RESET_VERSION = "Beta 1.9.33";
   private static final Map<String, Double> PRESET_MCSM;
   private static final Map<String, Double> PRESET_MCSM_NETFLIX;
   private static final Map<String, Double> PRESET_LEGACY;
   private static final Map<String, Double> PRESET_CINEMATIC;
   private static final Gson GSON;
   private static boolean wipedByRestructure;

   public static String earlyName() {
      String var10000;
      switch ((int)Math.round(nameStyle)) {
         case 1 -> var10000 = "Wither Storm";
         case 2 -> var10000 = "Wither (Wither Storm)";
         default -> var10000 = "Commanded Wither";
      }

      return var10000;
   }

   public static String stormName() {
      String var10000;
      switch ((int)Math.round(nameStyle)) {
         case 1 -> var10000 = "Wither Storm";
         default -> var10000 = "The Wither Storm";
      }

      return var10000;
   }

   private static void setFilledSubphases(double v) {
      filledSubphases = v >= (double)0.5F;
      if (!filledSubphases) {
         scaledSubphaseGrowth = false;
      }

   }

   private static void setScaledSubphaseGrowth(double v) {
      scaledSubphaseGrowth = filledSubphases && v >= (double)0.5F;
   }

   public static boolean isLocked(String keyName) {
      return (keyName.equals("scaledSubphaseGrowth") && !filledSubphases) || (keyName.equals("phaseAnimStrength") && !phaseAnim);
   }

   private static void key(String name, String description, double min, double max, boolean toggle, DoubleSupplier get, DoubleConsumer set) {
      KEYS.put(name, new Key(name, description, min, max, toggle, (String[])null, get, set));
   }

   private static void keyCycle(String name, String description, String[] labels, DoubleSupplier get, DoubleConsumer set) {
      KEYS.put(name, new Key(name, description, (double)0.0F, (double)(labels.length - 1), false, labels, get, set));
   }

   private static double snapToPowerOfTwo(double v) {
      double clamped = Math.max((double)512.0F, Math.min((double)8192.0F, v));
      int exp = (int)Math.round(Math.log(clamped) / Math.log((double)2.0F));
      return Math.pow((double)2.0F, (double)Math.max(9, Math.min(13, exp)));
   }

   public static double defaultOf(String name) {
      return (Double)DEFAULTS.getOrDefault(name, (double)0.0F);
   }

   private static Map<String, Double> presetValues(int preset) {
      Map var10000;
      switch (preset) {
         case 1 -> var10000 = PRESET_MCSM;
         case 2 -> var10000 = PRESET_MCSM_NETFLIX;
         case 3 -> var10000 = PRESET_LEGACY;
         case 4 -> var10000 = PRESET_CINEMATIC;
         default -> var10000 = null;
      }

      return var10000;
   }

   public static boolean isPresetKey(String name) {
      return PRESET_MCSM.containsKey(name);
   }

   public static void applyPreset(int preset) {
      applyPreset(preset, (Collection)null);
   }

   public static void applyPreset(int preset, Collection<String> tabKeys) {
      Map<String, Double> values = presetValues(preset);
      if (values != null) {
         if (tabKeys != null) {
            resetDefaults(tabKeys);
         }

         for(Map.Entry<String, Double> e : values.entrySet()) {
            Key key = (Key)KEYS.get(e.getKey());
            if (key != null) {
               key.set().accept((Double)e.getValue());
            }
         }

         effectsPreset = (double)preset;
      }
   }

   public static void refreshPreset() {
      for(int preset = 1; preset <= 3; ++preset) {
         Map<String, Double> values = presetValues(preset);
         boolean match = true;

         for(Map.Entry<String, Double> e : values.entrySet()) {
            Key key = (Key)KEYS.get(e.getKey());
            if (key == null || Math.abs(key.get().getAsDouble() - (Double)e.getValue()) > 1.0E-6) {
               match = false;
               break;
            }
         }

         if (match) {
            effectsPreset = (double)preset;
            return;
         }
      }

      effectsPreset = (double)0.0F;
   }

   private static Path file() {
      return FabricLoader.getInstance().getConfigDir().resolve("dabywitherstormmod-client.json");
   }

   public static void load() {
      Path path = file();
      if (Files.exists(path, new LinkOption[0])) {
         try {
            JsonObject json = (JsonObject)GSON.fromJson(Files.readString(path), JsonObject.class);
            if (json == null) {
               return;
            }

            for(Key key : KEYS.values()) {
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
         } catch (RuntimeException | IOException e) {
            PrintStream var10000 = System.out;
            String var10001 = String.valueOf(path);
            var10000.println("Failed to read " + var10001 + ": " + String.valueOf(e));
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
               for(String name : new String[]{"beamColorR", "beamColorG", "beamColorB", "beamOpacity"}) {
                  ((Key)KEYS.get(name)).set().accept(defaultOf(name));
               }
            }

            if (loadedVersion < 6) {
               for(String name : new String[]{"eyeColorR", "eyeColorG", "eyeColorB"}) {
                  ((Key)KEYS.get(name)).set().accept(defaultOf(name));
               }
            }

            if (loadedVersion < 7) {
               ((Key)KEYS.get("bloomStrength")).set().accept(defaultOf("bloomStrength"));
            }

            if (loadedVersion < 8) {
               ((Key)KEYS.get("bloomMaskToStorm")).set().accept(defaultOf("bloomMaskToStorm"));
            }

            if (loadedVersion < 9) {
               ((Key)KEYS.get("bloomStrength")).set().accept(defaultOf("bloomStrength"));
            }

            if (loadedVersion < 10) {
               ((Key)KEYS.get("bloomStrength")).set().accept(defaultOf("bloomStrength"));
               ((Key)KEYS.get("bloomMaskToStorm")).set().accept(defaultOf("bloomMaskToStorm"));
            }

            loadedVersion = 13;
            save();
         }
      }
      if (loadedVersion < 14) {
         for(String name : new String[]{"teethColorR", "teethColorG", "teethColorB", "haloRings", "haloRingStrength", "shieldDome", "beamNightBoost", "vortexDebris", "vortexStrength"}) {
            Key key = (Key)KEYS.get(name);
            if (key != null) {
               key.set().accept(defaultOf(name));
            }
         }
         if (farLandsDistance < (double)500.0F) {
            farLandsDistance = defaultOf("farLandsDistance");
         }
         loadedVersion = 14;
         save();
      }
   }

   public static void save() {
      JsonObject json = new JsonObject();

      for(Key key : KEYS.values()) {
         json.addProperty(key.name(), key.get().getAsDouble());
      }

      json.addProperty("effectsPreset", effectsPreset);
      json.addProperty("configOpened", configOpened);
      json.addProperty("configVersion", 14);

      try {
         Files.writeString(file(), GSON.toJson(json));
      } catch (IOException e) {
         PrintStream var10000 = System.out;
         String var10001 = String.valueOf(file());
         var10000.println("Failed to write " + var10001 + ": " + String.valueOf(e));
      }

   }

   public static void resetDefaults() {
      resetDefaults(KEYS.keySet());
   }

   public static void resetDefaults(Collection<String> names) {
      for(Key key : KEYS.values()) {
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
         if (!path.startsWith("head_beam") && !path.equals("head_activate_beam")) {
            if (path.startsWith("head_")) {
               return (float)headSoundsVolume;
            } else if (!path.equals("infected_mob") && !path.startsWith("withered_")) {
               return 1.0F;
            } else {
               return infectedMobSound ? (float)infectedMobSoundVolume : 0.0F;
            }
         } else {
            return (float)beamSoundsVolume;
         }
      } else {
         return beamDeactivateSound ? (float)beamSoundsVolume : 0.0F;
      }
   }

   static {
      key("distantStorms", "Draw storms that are past normal entity range.", (double)0.0F, (double)1.0F, true, () -> distantStorms ? (double)1.0F : (double)0.0F, (v) -> distantStorms = v >= (double)0.5F);
      key("distantFog", "Haze distant storms so they sit in the weather.", (double)0.0F, (double)1.0F, true, () -> distantFog ? (double)1.0F : (double)0.0F, (v) -> distantFog = v >= (double)0.5F);
      key("legacyHeads", "Simpler heads and a cleaner beam. Less accurate to MCSM.", (double)0.0F, (double)1.0F, true, () -> legacyHeads ? (double)1.0F : (double)0.0F, (v) -> legacyHeads = v >= (double)0.5F);
      key("clusterVolumetricLighting", "Relight every block of every debris cluster each frame. Much prettier up close and very expensive: a field of large clusters can cost most of your framerate.", (double)0.0F, (double)1.0F, true, () -> clusterVolumetricLighting ? (double)1.0F : (double)0.0F, (v) -> clusterVolumetricLighting = v >= (double)0.5F);
      key("filledSubphases", "Grow the back mass in across the fractional subphases. Off removes it.", (double)0.0F, (double)1.0F, true, () -> filledSubphases ? (double)1.0F : (double)0.0F, DabyWSClientConfig::setFilledSubphases);
      key("scaledSubphaseGrowth", "Swell the new back up smoothly instead of building it cube by cube. Needs Filled Subphases.", (double)0.0F, (double)1.0F, true, () -> scaledSubphaseGrowth ? (double)1.0F : (double)0.0F, DabyWSClientConfig::setScaledSubphaseGrowth);
      key("flatbackFlipFix", "Fill the shell's flat rear with a mirrored copy so the mass reads rounded.", (double)0.0F, (double)1.0F, true, () -> flatbackFlipFix ? (double)1.0F : (double)0.0F, (v) -> flatbackFlipFix = v >= (double)0.5F);
      key("mirrorBackDetail", "How much of that mirrored fill to draw. It hides behind the real back and costs nearly half of what late phase 5 adds, so it is the cheapest detail to give up. 1 is everything.", (double)0.25F, (double)1.0F, false, () -> mirrorBackDetail, (v) -> mirrorBackDetail = v);
      key("stormAmbience", "Play the looping storm ambience.", (double)0.0F, (double)1.0F, true, () -> stormAmbience ? (double)1.0F : (double)0.0F, (v) -> stormAmbience = v >= (double)0.5F);
      key("beamHum", "Play the tractor beam hum.", (double)0.0F, (double)1.0F, true, () -> beamHum ? (double)1.0F : (double)0.0F, (v) -> beamHum = v >= (double)0.5F);
      key("ambienceVolume", "", (double)0.0F, (double)2.0F, false, () -> ambienceVolume, (v) -> ambienceVolume = v);
      key("headSoundsVolume", "Growls, snarls, roars, hurt and shoot sounds.", (double)0.0F, (double)2.0F, false, () -> headSoundsVolume, (v) -> headSoundsVolume = v);
      key("beamDeactivateSound", "The beam switching off. It fires far more often than the switch-on.", (double)0.0F, (double)1.0F, true, () -> beamDeactivateSound ? (double)1.0F : (double)0.0F, (v) -> beamDeactivateSound = v >= (double)0.5F);
      key("beamSoundsVolume", "", (double)0.0F, (double)2.0F, false, () -> beamSoundsVolume, (v) -> beamSoundsVolume = v);
      key("beamHumVolume", "", (double)0.0F, (double)2.0F, false, () -> beamHumVolume, (v) -> beamHumVolume = v);
      key("beamHumRange", "How far the hum carries, in blocks. 0 means only from inside it.", (double)0.0F, (double)256.0F, false, () -> beamHumRange, (v) -> beamHumRange = v);
      key("infectedMobSound", "The wail when a mob turns fully Withered.", (double)0.0F, (double)1.0F, true, () -> infectedMobSound ? (double)1.0F : (double)0.0F, (v) -> infectedMobSound = v >= (double)0.5F);
      key("infectedMobSoundVolume", "", (double)0.0F, (double)2.0F, false, () -> infectedMobSoundVolume, (v) -> infectedMobSoundVolume = v);
      key("devourerDebrisGlow", "Let the phase-6 violet debris feed the bloom.", (double)0.0F, (double)1.0F, true, () -> devourerDebrisGlow ? (double)1.0F : (double)0.0F, (v) -> devourerDebrisGlow = v >= (double)0.5F);
      key("debrisAmount", "How much wreckage orbits the storm.", (double)0.0F, (double)2.0F, false, () -> debrisAmount, (v) -> debrisAmount = v);
      key("debrisSize", "Size of the wreckage blocks caught in the tractor beams.", 0.2, (double)3.0F, false, () -> debrisSize, (v) -> debrisSize = v);
      keyCycle("stormSkin", "Obsidian Gloss is the OG MCSM skin: shiny near-black flesh with a purple sheen, and the command block belly redrawn as obsidian-purple tiles instead of vanilla orange. Classic keeps the plain textures.", SKIN_LABELS, () -> stormSkin, (v) -> stormSkin = (double)Math.round(v));
      keyCycle("stormStars", "A dome of twinkling stars in the blacked-out sky. Storm Nights shows it only while a phase-5+ storm has eaten the light; Every Night replaces the vanilla sky every night.", STAR_LABELS, () -> stormStars, (v) -> stormStars = (double)Math.round(v));
      key("starDensity", "How many stars fill the dome.", 0.25, (double)2.0F, false, () -> starDensity, (v) -> starDensity = v);
      key("starTwinkleSpeed", "How fast the stars twinkle.", (double)0.0F, (double)4.0F, false, () -> starTwinkleSpeed, (v) -> starTwinkleSpeed = v);
      key("starBrightness", "Overall star brightness.", (double)0.0F, (double)2.0F, false, () -> starBrightness, (v) -> starBrightness = v);
      keyCycle("stormCloudDeck", "MCSM-style weather slabs slowly orbiting the storm instead of a flat dark sky.", CLOUD_DECK_LABELS, () -> stormCloudDeck, (v) -> stormCloudDeck = (double)Math.round(v));
      key("stormCloudCoverage", "How much of the sky around the storm the deck covers.", 0.25, (double)2.0F, false, () -> stormCloudCoverage, (v) -> stormCloudCoverage = v);
      key("stormCloudAltitude", "Push the whole deck up or down.", -40.0, (double)40.0F, false, () -> stormCloudAltitude, (v) -> stormCloudAltitude = v);
      key("stormCloudPaletteMix", "How much the deck follows the phase palette versus your manual cloud colour.", (double)0.0F, (double)1.0F, false, () -> stormCloudPaletteMix, (v) -> stormCloudPaletteMix = v);
      key("atmospherePulse", "The purple-blue glare breathing in the air around a late-phase storm - part of the atmosphere, bigger than the body.", (double)0.0F, (double)1.0F, true, () -> atmospherePulse ? (double)1.0F : (double)0.0F, (v) -> atmospherePulse = v >= (double)0.5F);
      key("stormAtmosphere", "Full-screen Story-Mode atmosphere post pass (lavender-to-magenta fog gradient + phase tinting).", (double)0.0F, (double)1.0F, true, () -> stormAtmosphere ? (double)1.0F : (double)0.0F, (v) -> stormAtmosphere = v >= (double)0.5F);
      key("stormBlobFX", "Phase 5.1+ GLSL storm body: colour-shifting centre blob, halo, rear fog cloud, phase-6 flash and vortex mesh.", (double)0.0F, (double)1.0F, true, () -> stormBlobFX ? (double)1.0F : (double)0.0F, (v) -> stormBlobFX = v >= (double)0.5F);
      key("pulseStrength", "How bright the pulse burns at its peak.", (double)0.0F, (double)2.0F, false, () -> pulseStrength, (v) -> pulseStrength = v);
      key("pulsePeriod", "Seconds between pulse peaks.", (double)1.0F, (double)10.0F, false, () -> pulsePeriod, (v) -> pulsePeriod = v);
      key("pulseSize", "How far the glow reaches past the body.", 0.5, (double)2.0F, false, () -> pulseSize, (v) -> pulseSize = v);
      key("cataclysmHalos", "From phase 5.8: the blue-purple halo ring around the area plus the original white halo under the body.", (double)0.0F, (double)1.0F, true, () -> cataclysmHalos ? (double)1.0F : (double)0.0F, (v) -> cataclysmHalos = v >= (double)0.5F);
      key("haloStrength", "Brightness of the cataclysm halo pair.", (double)0.0F, (double)2.0F, false, () -> haloStrength, (v) -> haloStrength = v);
      key("haloRings", "Crisp cubic halo rings: the white under-halo from phase 4, the spiralling trio at phase 6, more at phase 7, and the three gigantic counter-rotating systems at phase 8-9.", (double)0.0F, (double)1.0F, true, () -> haloRings ? (double)1.0F : (double)0.0F, (v) -> haloRings = v >= (double)0.5F);
      key("haloRingStrength", "Brightness of the cubic halo-ring system.", (double)0.0F, (double)2.0F, false, () -> haloRingStrength, (v) -> haloRingStrength = v);
      key("shieldDome", "Draw the old blue shield dome over the storm. Off by default: the MCSM look has no dome covering the sky.", (double)0.0F, (double)1.0F, true, () -> shieldDome ? (double)1.0F : (double)0.0F, (v) -> shieldDome = v >= (double)0.5F);
      key("vortexDebris", "Block pellets rip off the ground and spiral up into the storm from phase 4, thickening into a full tornado by phase 6.", (double)0.0F, (double)1.0F, true, () -> vortexDebris ? (double)1.0F : (double)0.0F, (v) -> vortexDebris = v >= (double)0.5F);
      key("vortexStrength", "Density of the abduction vortex spiral.", (double)0.0F, (double)2.0F, false, () -> vortexStrength, (v) -> vortexStrength = v);
      key("blackGlare", "The black-purple glare ring hugging the storm's silhouette.", (double)0.0F, (double)1.0F, true, () -> blackGlare ? (double)1.0F : (double)0.0F, (v) -> blackGlare = v >= (double)0.5F);
      key("blackGlareStrength", "How dark the rim glare goes.", (double)0.0F, (double)2.0F, false, () -> blackGlareStrength, (v) -> blackGlareStrength = v);
      key("glareEjecta", "Turquoise and green cluster sparks ejecting from the glare ring.", (double)0.0F, (double)1.0F, true, () -> glareEjecta ? (double)1.0F : (double)0.0F, (v) -> glareEjecta = v >= (double)0.5F);
      key("ejectaRate", "How many cluster sparks burst off the rim.", (double)0.0F, (double)3.0F, false, () -> ejectaRate, (v) -> ejectaRate = v);
      key("ejectaBrightness", "Brightness of the ejecta sparks.", (double)0.0F, (double)2.0F, false, () -> ejectaBrightness, (v) -> ejectaBrightness = v);
      key("pulseHeartbeat", "A deep thump from the nearest storm on every pulse peak.", (double)0.0F, (double)1.0F, true, () -> pulseHeartbeat ? (double)1.0F : (double)0.0F, (v) -> pulseHeartbeat = v >= (double)0.5F);
      key("pulseHeartbeatVolume", "Loudness of the pulse heartbeat.", (double)0.0F, (double)2.0F, false, () -> pulseHeartbeatVolume, (v) -> pulseHeartbeatVolume = v);
      key("pulseHeartbeatRange", "How far the heartbeat carries.", (double)128.0F, (double)1024.0F, false, () -> pulseHeartbeatRange, (v) -> pulseHeartbeatRange = v);
      key("phaseFogPalettes", "Let the storm's phase recolour the sky: turquoise fog at phase 5, purple-black from phase 5.8 on.", (double)0.0F, (double)1.0F, true, () -> phaseFogPalettes ? (double)1.0F : (double)0.0F, (v) -> phaseFogPalettes = v >= (double)0.5F);
      key("paletteStrength", "How far the phase palettes override your manual colours.", (double)0.0F, (double)1.0F, false, () -> paletteStrength, (v) -> paletteStrength = v);
      key("turquoiseFogR", "Phase-5 turquoise fog anchor.", (double)0.0F, (double)1.0F, false, () -> turquoiseFogR, (v) -> turquoiseFogR = v);
      key("turquoiseFogG", "", (double)0.0F, (double)1.0F, false, () -> turquoiseFogG, (v) -> turquoiseFogG = v);
      key("turquoiseFogB", "", (double)0.0F, (double)1.0F, false, () -> turquoiseFogB, (v) -> turquoiseFogB = v);
      key("cataclysmFogR", "Phase-5.8+ purple-black fog anchor.", (double)0.0F, (double)1.0F, false, () -> cataclysmFogR, (v) -> cataclysmFogR = v);
      key("cataclysmFogG", "", (double)0.0F, (double)1.0F, false, () -> cataclysmFogG, (v) -> cataclysmFogG = v);
      key("cataclysmFogB", "", (double)0.0F, (double)1.0F, false, () -> cataclysmFogB, (v) -> cataclysmFogB = v);
      key("skyDarkenIntensity", "How much a late-phase storm darkens the sky.", (double)0.0F, (double)1.0F, false, () -> skyDarkenIntensity, (v) -> skyDarkenIntensity = v);
      key("separateFogColor", "Give the fog its own colour instead of the sky's.", (double)0.0F, (double)1.0F, true, () -> separateFogColor ? (double)1.0F : (double)0.0F, (v) -> separateFogColor = v >= (double)0.5F);
      key("fogColorR", "", (double)0.0F, (double)1.0F, false, () -> fogColorR, (v) -> fogColorR = v);
      key("fogColorG", "", (double)0.0F, (double)1.0F, false, () -> fogColorG, (v) -> fogColorG = v);
      key("fogColorB", "", (double)0.0F, (double)1.0F, false, () -> fogColorB, (v) -> fogColorB = v);
      key("stormFog", "Storm proximity fog: the closer you get to the storm, the thicker the purple haze closes in around you.", (double)0.0F, (double)1.0F, true, () -> stormFog ? (double)1.0F : (double)0.0F, (v) -> stormFog = v >= (double)0.5F);
      key("stormFogStrength", "How thick the storm's proximity fog gets up close (0 = no effect).", (double)0.0F, (double)1.0F, false, () -> stormFogStrength, (v) -> stormFogStrength = v);
      key("farLandsHaze", "Far-lands haze: the further you travel from the world origin, the thicker the purple haze closes in, giving that lonely Story-Mode far-lands feel.", (double)0.0F, (double)1.0F, true, () -> farLandsHaze ? (double)1.0F : (double)0.0F, (v) -> farLandsHaze = v >= (double)0.5F);
      key("farLandsDistance", "Blocks from the world origin at which the far-lands haze starts to close in.", (double)500.0F, (double)100000.0F, false, () -> farLandsDistance, (v) -> farLandsDistance = v);
      key("farLandsStrength", "How thick the far-lands haze gets at extreme distance.", (double)0.0F, (double)1.0F, false, () -> farLandsStrength, (v) -> farLandsStrength = v);
      key("biomeFogTint", "Biome-tinted storm fog: the storm's purple fog takes on the colour of the biome it is devouring.", (double)0.0F, (double)1.0F, true, () -> biomeFogTint ? (double)1.0F : (double)0.0F, (v) -> biomeFogTint = v >= (double)0.5F);
      key("biomeFogStrength", "How strongly the storm fog blends toward the biome's colour.", (double)0.0F, (double)1.0F, false, () -> biomeFogStrength, (v) -> biomeFogStrength = v);
      key("skyDarkenR", "", (double)0.0F, (double)1.0F, false, () -> skyDarkenR, (v) -> skyDarkenR = v);
      key("skyDarkenG", "", (double)0.0F, (double)1.0F, false, () -> skyDarkenG, (v) -> skyDarkenG = v);
      key("skyDarkenB", "", (double)0.0F, (double)1.0F, false, () -> skyDarkenB, (v) -> skyDarkenB = v);
      key("skyDarkenLighting", "How much the gloom darkens the world's actual lighting, not just the sky.", (double)0.0F, (double)1.0F, false, () -> skyDarkenLighting, (v) -> skyDarkenLighting = v);
      key("cloudDarkenStrength", "How far the clouds are dragged toward the colour below.", (double)0.0F, (double)1.0F, false, () -> cloudDarkenStrength, (v) -> cloudDarkenStrength = v);
      key("cloudColorR", "", (double)0.0F, (double)1.0F, false, () -> cloudColorR, (v) -> cloudColorR = v);
      key("cloudColorG", "", (double)0.0F, (double)1.0F, false, () -> cloudColorG, (v) -> cloudColorG = v);
      key("cloudColorB", "", (double)0.0F, (double)1.0F, false, () -> cloudColorB, (v) -> cloudColorB = v);
      key("sunGlow", "Burn the sun yellow through the gloom. Nothing under a clear sky.", (double)0.0F, (double)1.0F, true, () -> sunGlow ? (double)1.0F : (double)0.0F, (v) -> sunGlow = v >= (double)0.5F);
      key("sunGlowStrength", "", (double)0.0F, (double)3.0F, false, () -> sunGlowStrength, (v) -> sunGlowStrength = v);
      key("sunGlowR", "", (double)0.0F, (double)1.0F, false, () -> sunGlowR, (v) -> sunGlowR = v);
      key("sunGlowG", "", (double)0.0F, (double)1.0F, false, () -> sunGlowG, (v) -> sunGlowG = v);
      key("sunGlowB", "", (double)0.0F, (double)1.0F, false, () -> sunGlowB, (v) -> sunGlowB = v);
      key("stormModelShading", "Darken the storm's undersides and crevices so its mass reads as solid. Practically free. Off under a shader pack.", (double)0.0F, (double)1.0F, true, () -> stormModelShading ? (double)1.0F : (double)0.0F, (v) -> stormModelShading = v >= (double)0.5F);
      key("reverseShading", "Light the body from behind and below, the way MCSM does, so the faces turned toward you are the dark ones.", (double)0.0F, (double)1.0F, true, () -> reverseShading ? (double)1.0F : (double)0.0F, (v) -> reverseShading = v >= (double)0.5F);
      key("stormMusic", "Play the storm's own score.", (double)0.0F, (double)1.0F, true, () -> stormMusic ? (double)1.0F : (double)0.0F, (v) -> stormMusic = v >= (double)0.5F);
      key("stormMusicVolume", "", (double)0.0F, (double)2.0F, false, () -> stormMusicVolume, (v) -> stormMusicVolume = v);
      key("stormMusicRange", "How far from the storm the score can be heard, in blocks.", (double)64.0F, (double)1024.0F, false, () -> stormMusicRange, (v) -> stormMusicRange = v);
      key("stormMusicCaveCutoff", "How much sky the spot you are standing in needs before the score plays. Deep caves get silence. 0 plays everywhere.", (double)0.0F, (double)15.0F, false, () -> stormMusicCaveCutoff, (v) -> stormMusicCaveCutoff = v);
      key("shadowMapResolution", "How big the shadow's own map is. Halving it quarters what the shadow costs and quarters the memory it takes, for a softer, steppier edge. Snaps to powers of two.", (double)512.0F, (double)8192.0F, false, () -> shadowMapResolution, (v) -> shadowMapResolution = snapToPowerOfTwo(v));
      key("stormShadow", "The storm casts a real sun shadow. Off under a shader pack.", (double)0.0F, (double)1.0F, true, () -> stormShadow ? (double)1.0F : (double)0.0F, (v) -> stormShadow = v >= (double)0.5F);
      key("stormSelfShadow", "The storm shades its own body. Independent of the shadow it throws on the world.", (double)0.0F, (double)1.0F, true, () -> stormSelfShadow ? (double)1.0F : (double)0.0F, (v) -> stormSelfShadow = v >= (double)0.5F);
      key("stormShadingContrast", "How much harder the storm's own shading is pushed than the world's.", (double)0.0F, (double)2.0F, false, () -> stormShadingContrast, (v) -> stormShadingContrast = v);
      key("stormShadowStrength", "", (double)0.0F, (double)1.0F, false, () -> stormShadowStrength, (v) -> stormShadowStrength = v);
      key("stormShadowTerrain", "Resolve the ground at 2 blocks when deciding what is indoors. Off uses an 8-block grid: cheaper, rougher at cave mouths.", (double)0.0F, (double)1.0F, true, () -> stormShadowTerrain ? (double)1.0F : (double)0.0F, (v) -> stormShadowTerrain = v >= (double)0.5F);
      key("stormShadowR", "", (double)0.0F, (double)1.0F, false, () -> stormShadowR, (v) -> stormShadowR = v);
      key("stormShadowG", "", (double)0.0F, (double)1.0F, false, () -> stormShadowG, (v) -> stormShadowG = v);
      key("stormShadowB", "", (double)0.0F, (double)1.0F, false, () -> stormShadowB, (v) -> stormShadowB = v);
      key("glowStrength", "How bright the teeth burn. This is what the bloom feeds on.", (double)0.0F, (double)1.0F, false, () -> glowStrength, (v) -> glowStrength = v);
      key("eyeColorR", "", (double)0.0F, (double)1.0F, false, () -> eyeColorR, (v) -> eyeColorR = v);
      key("eyeColorG", "", (double)0.0F, (double)1.0F, false, () -> eyeColorG, (v) -> eyeColorG = v);
      key("eyeColorB", "", (double)0.0F, (double)1.0F, false, () -> eyeColorB, (v) -> eyeColorB = v);
      key("teethColorR", "", (double)0.0F, (double)1.0F, false, () -> teethColorR, (v) -> teethColorR = v);
      key("teethColorG", "", (double)0.0F, (double)1.0F, false, () -> teethColorG, (v) -> teethColorG = v);
      key("teethColorB", "", (double)0.0F, (double)1.0F, false, () -> teethColorB, (v) -> teethColorB = v);
      key("stormGlowStrength", "How brightly the silhouette glows at night. 0 is off.", (double)0.0F, (double)2.0F, false, () -> stormGlowStrength, (v) -> stormGlowStrength = v);
      keyCycle("bloomStrength", "Screen-space glow over the finished image while a storm is near. Costs a few full-screen passes per frame.", BLOOM_LABELS, () -> bloomStrength, (v) -> bloomStrength = (double)Math.round(v));
      key("bloomMaskToStorm", "Keep the glow on the storm's teeth and eye. Off blooms every bright thing on screen.", (double)0.0F, (double)1.0F, true, () -> bloomMaskToStorm ? (double)1.0F : (double)0.0F, (v) -> bloomMaskToStorm = v >= (double)0.5F);
      key("beamOpacity", "How solid the beam is. Lower lets you see the world through it.", (double)0.0F, (double)2.0F, false, () -> beamOpacity, (v) -> beamOpacity = v);
      key("beamNightBoost", "How much brighter the tractor beams burn at night. 1 leaves them unchanged.", (double)1.0F, (double)2.0F, false, () -> beamNightBoost, (v) -> beamNightBoost = v);
      key("beamEndFade", "How far the beam fades out where it meets the ground.", (double)0.0F, (double)1.0F, false, () -> beamEndFade, (v) -> beamEndFade = v);
      key("beamColorR", "", (double)0.0F, (double)1.0F, false, () -> beamColorR, (v) -> beamColorR = v);
      key("beamColorG", "", (double)0.0F, (double)1.0F, false, () -> beamColorG, (v) -> beamColorG = v);
      key("beamColorB", "", (double)0.0F, (double)1.0F, false, () -> beamColorB, (v) -> beamColorB = v);
      key("beamInnerFaces", "Draw the beam's inner walls. Off hides the inner corners.", (double)0.0F, (double)1.0F, true, () -> beamInnerFaces ? (double)1.0F : (double)0.0F, (v) -> beamInnerFaces = v >= (double)0.5F);
      key("impactLight", "Light the ground where a beam lands.", (double)0.0F, (double)1.0F, true, () -> impactLight ? (double)1.0F : (double)0.0F, (v) -> impactLight = v >= (double)0.5F);
      key("impactLightSize", "How far the pool spreads. The centre stays as bright.", (double)0.25F, (double)4.0F, false, () -> impactLightSize, (v) -> impactLightSize = v);
      key("impactLightBrightness", "", (double)0.0F, (double)2.0F, false, () -> impactLightBrightness, (v) -> impactLightBrightness = v);
      key("impactLightRange", "How far away a beam can be and still light the ground, in blocks.", (double)32.0F, (double)1024.0F, false, () -> impactLightRange, (v) -> impactLightRange = v);
      key("impactLightUseBeamColor", "Off gives plain white light.", (double)0.0F, (double)1.0F, true, () -> impactLightUseBeamColor ? (double)1.0F : (double)0.0F, (v) -> impactLightUseBeamColor = v >= (double)0.5F);
      key("bossbarNotched", "", (double)0.0F, (double)1.0F, true, () -> bossbarNotched ? (double)1.0F : (double)0.0F, (v) -> bossbarNotched = v >= (double)0.5F);
      keyCycle("bossbarColor", "", BOSSBAR_COLOR_LABELS, () -> bossbarColor, (v) -> bossbarColor = (double)Math.round(v));
      keyCycle("nameStyle", "Classic: Commanded Wither, then The Wither Storm. Cracker's: Wither Storm for both. Legacy: Wither (Wither Storm), then The Wither Storm.", NAME_STYLE_LABELS, () -> nameStyle, (v) -> nameStyle = (double)Math.round(v));
      key("phaseAnim", "Apply a phase-driven animation profile to storm tentacles.", 0, 1, true, () -> phaseAnim ? 1 : 0, v -> phaseAnim = v >= .5);
      key("phaseAnimStrength", "Strength of the per-phase animation profile.", 0, 2, false, () -> phaseAnimStrength, v -> phaseAnimStrength = v);
      key("tentacleIdleSpeed", "Speed of the sine that drives every tentacle idle. 1 is stock; above about 2 the limbs read as flailing rather than swimming.", 0.1, (double)3.0F, false, () -> tentacleIdleSpeed, (v) -> tentacleIdleSpeed = v);
      key("tentacleWaveTravel", "How much the bend lags from one bone to the next. 0 bends each limb as a single arc with no travelling wave at all.", (double)0.0F, (double)2.0F, false, () -> tentacleWaveTravel, (v) -> tentacleWaveTravel = v);
      key("tentacleCurlDepth", "How far each small-tentacle bone bends. The bends compound down the chain, so small changes here are large on screen.", (double)0.25F, (double)2.5F, false, () -> tentacleCurlDepth, (v) -> tentacleCurlDepth = v);
      key("tentacleCrossAxis", "How much of the small tentacles' curl goes on the second bend axis. This is what turns a nod into a figure-eight. 0 makes them nod only.", (double)0.0F, (double)1.5F, false, () -> tentacleCrossAxis, (v) -> tentacleCrossAxis = v);
      key("bigTentacleCurlDepth", "How far each bone of the big pair bends. Their ramp is squared, so the base stays near still and the movement lives out at the tip.", (double)0.25F, (double)2.5F, false, () -> bigTentacleCurlDepth, (v) -> bigTentacleCurlDepth = v);
      key("bigTentacleHangBreath", "How much the big pair settles and rises under its own weight, on a clock slower than the travelling wave. 0 makes them rigid props.", (double)0.0F, (double)3.0F, false, () -> bigTentacleHangBreath, (v) -> bigTentacleHangBreath = v);
      key("bigTentacleSideSweep", "How far the big pair opens out to the sides past its hang.", (double)0.0F, (double)2.0F, false, () -> bigTentacleSideSweep, (v) -> bigTentacleSideSweep = v);
      key("lateGrowthWrithe", "Idle speed multiplier applied to every tentacle from phase 5.8.", (double)0.5F, (double)3.0F, false, () -> lateGrowthWrithe, (v) -> lateGrowthWrithe = v);
      key("tentaclePhysics", "Replace the hand-authored idle with a verlet rope simulation. It reacts to the storm's own motion, and it can settle into poses the authored idle never produces.", (double)0.0F, (double)1.0F, true, () -> tentaclePhysics ? (double)1.0F : (double)0.0F, (v) -> tentaclePhysics = v >= (double)0.5F);
      key("verletGravity", "Downward pull per step. These limbs are meant to hold their own shape, so the stock value is near zero.", (double)0.0F, (double)0.5F, false, () -> verletGravity, (v) -> verletGravity = v);
      key("verletSway", "How hard the rope reacts to the storm's own movement.", (double)0.0F, (double)8.0F, false, () -> verletSway, (v) -> verletSway = v);
      key("verletDamping", "How much speed survives each step. Higher is heavier and slower to settle; past about 0.97 it never settles at all.", 0.6, 0.99, false, () -> verletDamping, (v) -> verletDamping = v);
      key("verletWrithe", "Strength of the writhe pushed along each rope.", (double)0.0F, (double)0.5F, false, () -> verletWrithe, (v) -> verletWrithe = v);
      key("verletWritheSpeed", "How fast that writhe travels.", (double)0.0F, 0.2, false, () -> verletWritheSpeed, (v) -> verletWritheSpeed = v);
      key("yawSmoothTime", "Seconds the body takes to settle onto a new heading. Vanilla ships rotation as a byte, so some smoothing is needed or the body clicks round in 1.4 degree steps. Higher is smoother and laggier.", 0.05, (double)2.0F, false, () -> yawSmoothTime, (v) -> yawSmoothTime = v);
      key("yawSnapDegrees", "Past this much error the smoothing steps aside and the body simply arrives. Meant for teleports and respawns, not for turning.", (double)5.0F, (double)180.0F, false, () -> yawSnapDegrees, (v) -> yawSnapDegrees = v);
      key("bodyLeanGain", "Multiplier on the storm's forward lean into its travel.", (double)0.0F, (double)2.0F, false, () -> bodyLeanGain, (v) -> bodyLeanGain = v);
      key("bodyBankGain", "Multiplier on how far the storm banks into its turns.", (double)0.0F, (double)2.0F, false, () -> bodyBankGain, (v) -> bodyBankGain = v);
      key("growthSmoothRate", "How fast the drawn back growth chases the real one, per second. The synced phase only moves in jumps, so this is what hides the steps.", 0.2, (double)6.0F, false, () -> growthSmoothRate, (v) -> growthSmoothRate = v);
      key("changeoverShake", "Multiplier on the rattle as the wither shakes itself into the storm.", (double)0.0F, (double)2.0F, false, () -> changeoverShake, (v) -> changeoverShake = v);
      key("jawLagGain", "How far a jaw swings behind the skull when a head whips round. Most of what sells the weight of a head this size.", (double)0.0F, (double)2.0F, false, () -> jawLagGain, (v) -> jawLagGain = v);
      key("jawLagMax", "The most a jaw is ever allowed to trail, in degrees.", (double)0.0F, (double)48.0F, false, () -> jawLagMax, (v) -> jawLagMax = v);
      key("jawLagCatchup", "How quickly a trailing jaw catches back up.", 0.02, (double)1.0F, false, () -> jawLagCatchup, (v) -> jawLagCatchup = v);
      key("legacyDistantRenderer", "Use the old fogged distant renderer instead of the fogless one. Distant storms come out washed out rather than crisp.", (double)0.0F, (double)1.0F, true, () -> legacyDistantRenderer ? (double)1.0F : (double)0.0F, (v) -> legacyDistantRenderer = v >= (double)0.5F);
      key("optimizeDistantAnimations", "Step distant storm animations at tick rate. Cheaper, and choppier.", (double)0.0F, (double)1.0F, true, () -> optimizeDistantAnimations ? (double)1.0F : (double)0.0F, (v) -> optimizeDistantAnimations = v >= (double)0.5F);
      key("stormBackfaceCull", "WARNING: very inconsistent. On paper the body draws both sides of every face, so dropping the far ones is close to half the pixel work. In practice the win depends entirely on how much of your screen the storm fills, and it can be nothing at all. Anything single-sided in the model becomes a hole. Measure it before you keep it.", (double)0.0F, (double)1.0F, true, () -> stormBackfaceCull ? (double)1.0F : (double)0.0F, (v) -> stormBackfaceCull = v >= (double)0.5F);
      key("shadowCullBackFaces", "Skip away-facing surfaces when building the shadow map. They cannot change the shadow's shape, so this roughly halves the work.", (double)0.0F, (double)1.0F, true, () -> shadowCullBackFaces ? (double)1.0F : (double)0.0F, (v) -> shadowCullBackFaces = v >= (double)0.5F);
      key("stormShadowSoftEdge", "Soften the shadow's edge with nine samples per pixel instead of one. This scales with your RESOLUTION, not with the storm, and it is the largest single shadow saving there is.", (double)0.0F, (double)1.0F, true, () -> stormShadowSoftEdge ? (double)1.0F : (double)0.0F, (v) -> stormShadowSoftEdge = v >= (double)0.5F);
      key("stormShadowHeightmap", "WARNING: very inconsistent. The coarsest ground grid there is, meant for the cheapest possible setup, but the grid is rarely what your frame rate is spent on, so this often changes nothing while roughening every cave mouth and cliff edge. Try the resolution slider first.", (double)0.0F, (double)1.0F, true, () -> stormShadowHeightmap ? (double)1.0F : (double)0.0F, (v) -> stormShadowHeightmap = v >= (double)0.5F);
      key("stormGlowFlip", "Only needed if the night glow washes over the storm instead of ringing it. That means the model's faces arrive wound the other way round.", (double)0.0F, (double)1.0F, true, () -> stormGlowFlip ? (double)1.0F : (double)0.0F, (v) -> stormGlowFlip = v >= (double)0.5F);
      keyCycle("bloomDebug", "Draw one stage of the bloom pipeline over the screen instead of compositing. Stage 2 must show terrain and the body; all black means nothing was captured. Stage 4 marks visible teeth green and occluded ones red.", BLOOM_DEBUG_LABELS, () -> bloomDebug, (v) -> bloomDebug = (double)Math.round(v));
      key("stormRenderStats", "Log what the storm drew each second: submits, cubes, vertices, shadow vertices.", (double)0.0F, (double)1.0F, true, () -> stormRenderStats ? (double)1.0F : (double)0.0F, (v) -> stormRenderStats = v >= (double)0.5F);
      key("bowelsFrameHud", "Show the Bowels frame readout. Only draws while you are in the Bowels.", (double)0.0F, (double)1.0F, true, () -> bowelsFrameHud ? (double)1.0F : (double)0.0F, (v) -> bowelsFrameHud = v >= (double)0.5F);

      for(Key k : KEYS.values()) {
         DEFAULTS.put(k.name(), k.get().getAsDouble());
      }

      loadedVersion = 14;
      PRESET_MCSM = Map.ofEntries(Map.entry("reverseShading", (double)1.0F), Map.entry("bloomStrength", (double)2.0F), Map.entry("beamOpacity", 0.6), Map.entry("beamColorR", 0.35), Map.entry("beamColorG", 0.1), Map.entry("beamColorB", 0.95), Map.entry("stormSkin", (double)1.0F), Map.entry("eyeColorR", 0.75), Map.entry("eyeColorG", 0.2), Map.entry("eyeColorB", (double)1.0F), Map.entry("teethColorR", 0.45), Map.entry("teethColorG", (double)1.0F), Map.entry("teethColorB", (double)1.0F), Map.entry("stormStars", (double)1.0F), Map.entry("stormCloudDeck", (double)0.0F), Map.entry("atmospherePulse", (double)1.0F), Map.entry("cataclysmHalos", (double)1.0F), Map.entry("haloRings", (double)1.0F), Map.entry("shieldDome", (double)0.0F), Map.entry("blackGlare", (double)1.0F), Map.entry("glareEjecta", (double)1.0F), Map.entry("debrisSize", 1.8), Map.entry("vortexDebris", (double)1.0F), Map.entry("phaseFogPalettes", (double)1.0F));
      PRESET_MCSM_NETFLIX = Map.ofEntries(Map.entry("reverseShading", (double)1.0F), Map.entry("bloomStrength", (double)1.8F), Map.entry("beamOpacity", 0.6), Map.entry("beamColorR", 0.35), Map.entry("beamColorG", 0.1), Map.entry("beamColorB", 0.95), Map.entry("stormSkin", (double)2.0F), Map.entry("eyeColorR", 0.78), Map.entry("eyeColorG", 0.25), Map.entry("eyeColorB", (double)1.0F), Map.entry("teethColorR", 0.55), Map.entry("teethColorG", (double)1.0F), Map.entry("teethColorB", (double)1.0F), Map.entry("stormStars", (double)1.0F), Map.entry("stormCloudDeck", (double)0.0F), Map.entry("atmospherePulse", (double)1.0F), Map.entry("cataclysmHalos", (double)1.0F), Map.entry("haloRings", (double)1.0F), Map.entry("shieldDome", (double)0.0F), Map.entry("blackGlare", (double)1.0F), Map.entry("glareEjecta", (double)1.0F), Map.entry("debrisSize", 1.5), Map.entry("vortexDebris", (double)1.0F), Map.entry("phaseFogPalettes", (double)1.0F));
      PRESET_LEGACY = Map.of("reverseShading", (double)0.0F, "bloomStrength", (double)1.0F, "beamOpacity", 0.74, "beamColorR", 0.52, "beamColorG", 0.46, "beamColorB", (double)1.0F);
      PRESET_CINEMATIC = Map.ofEntries(Map.entry("reverseShading", (double)1.0F), Map.entry("bloomStrength", (double)2.0F), Map.entry("beamOpacity", 0.6), Map.entry("beamColorR", 0.35), Map.entry("beamColorG", 0.1), Map.entry("beamColorB", 0.95), Map.entry("stormSkin", (double)1.0F), Map.entry("teethColorR", 0.45), Map.entry("teethColorG", (double)1.0F), Map.entry("teethColorB", (double)1.0F), Map.entry("stormStars", (double)2.0F), Map.entry("stormCloudDeck", (double)2.0F), Map.entry("atmospherePulse", (double)1.0F), Map.entry("pulseStrength", 1.3), Map.entry("cataclysmHalos", (double)1.0F), Map.entry("haloStrength", 1.2), Map.entry("haloRings", (double)1.0F), Map.entry("shieldDome", (double)0.0F), Map.entry("blackGlare", (double)1.0F), Map.entry("glareEjecta", (double)1.0F), Map.entry("ejectaRate", 1.4), Map.entry("pulseHeartbeat", (double)1.0F), Map.entry("debrisSize", (double)2.0F), Map.entry("vortexDebris", (double)1.0F), Map.entry("phaseFogPalettes", (double)1.0F), Map.entry("paletteStrength", (double)1.0F));
      GSON = (new GsonBuilder()).setPrettyPrinting().create();
   }

   public static record Key(String name, String description, double min, double max, boolean toggle, String[] cycleLabels, DoubleSupplier get, DoubleConsumer set) {
      public double clamp(double v) {
         return Math.max(this.min, Math.min(this.max, v));
      }

      public boolean cycle() {
         return this.cycleLabels != null;
      }
   }
}
);
      GSON = (new GsonBuilder()).setPrettyPrinting().create();
   }

   public static record Key(String name, String description, double min, double max, boolean toggle, String[] cycleLabels, DoubleSupplier get, DoubleConsumer set) {
      public double clamp(double v) {
         return Math.max(this.min, Math.min(this.max, v));
      }

      public boolean cycle() {
         return this.cycleLabels != null;
      }
   }
}
