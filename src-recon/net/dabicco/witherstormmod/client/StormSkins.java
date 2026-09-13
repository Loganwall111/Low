package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.resources.Identifier;

/**
 * Single source of truth for storm body materials. Phase 0 keeps its tiny
 * starter atlas; every Phase 1+ opaque storm piece uses the matching dark
 * Phase 6 atlas for its model UV layout.
 */
public final class StormSkins {
   private static final Identifier LEGACY_CLASSIC = id("textures/entity/wither_storm.png");
   private static final Identifier LEGACY_OG = id("textures/entity/wither_storm_og.png");
   // Phase 6 main/head/tentacle atlas. Do not use the nested 160x160
   // vanilla sheet here; these models use the 512x512 phase atlas UVs.
   private static final Identifier PHASE6_BODY = id("textures/entity/phase_4_assets_p6.png");
   // Matching 512x512 UV layout, transparent except for native eye/teeth
   // emissive islands. The 160x160 CEM sheets must not be bound here.
   private static final Identifier PHASE6_EMISSIVE = id("textures/entity/phase_4_assets_e.png");
   private static final Identifier PHASE6_DEVOURER = id("textures/entity/devourer_assets_p6.png");

   private static volatile double phaseHint;

   private StormSkins() {
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("dabywitherstormmod", path);
   }

   public static void setPhaseHint(double phase) {
      phaseHint = phase;
   }

   public static double phaseHint() {
      return phaseHint;
   }

   public static boolean og() {
      return Math.round(DabyWSClientConfig.stormSkin) >= 1L;
   }

   /** Phase 0 only; Phase 1 and later use the main-model Phase 6 atlas. */
   public static Identifier legacy() {
      return phaseHint >= 1.0D ? PHASE6_BODY : (og() ? LEGACY_OG : LEGACY_CLASSIC);
   }

   /** Select the universal skin while preserving the Phase 0 starter atlas. */
   public static Identifier body(double phase) {
      setPhaseHint(phase);
      return phase >= 1.0D ? PHASE6_BODY : (og() ? LEGACY_OG : LEGACY_CLASSIC);
   }

   /** Main-model Phase 6 atlas for bodies, heads, jaws, necks, and tentacles. */
   public static Identifier phase6Body() {
      return PHASE6_BODY;
   }

   /** Dedicated native-model eye/teeth emissive atlas with matching 512 UVs. */
   public static Identifier phase6Emissive() {
      return PHASE6_EMISSIVE;
   }

   /** Compatibility name retained for old Phase 1+ renderer callers. */
   public static Identifier phase4() {
      return PHASE6_BODY;
   }

   /** Detached/devourer pieces use their matching Phase 6 UV atlas. */
   public static Identifier devourer() {
      return PHASE6_DEVOURER;
   }

   public static Identifier teethGlow(double phase) {
      setPhaseHint(phase);
      boolean og = DabyWSClientConfig.stormSkin >= 0.5;
      return phase >= 7.0
         ? id(og ? "textures/entity/wither_storm_og_p7_e.png" : "textures/entity/wither_storm_p7_e.png")
         : phase >= 6.0
            ? id(og ? "textures/entity/wither_storm_og_p6_e.png" : "textures/entity/wither_storm_p6_e.png")
            : phase >= 5.5
               ? id(og ? "textures/entity/wither_storm_og_p55_e.png" : "textures/entity/wither_storm_p55_e.png")
               : phase >= 5.1
                  ? id(og ? "textures/entity/wither_storm_og_p51_e.png" : "textures/entity/wither_storm_p51_e.png")
                  : phase >= 5.0
                     ? id(og ? "textures/entity/wither_storm_og_p5_e.png" : "textures/entity/wither_storm_p5_e.png")
                     : phase >= 4.0
                        ? id(og ? "textures/entity/wither_storm_og_e.png" : "textures/entity/wither_storm_e.png")
                        : id("textures/entity/wither_storm_no_teeth_glow_e.png");
   }
}
