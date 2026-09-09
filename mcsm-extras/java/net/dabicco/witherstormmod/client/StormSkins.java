package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.resources.Identifier;

/**
 * Storm body + teeth emissive atlas selection.
 * Textures must stay UV-safe: AI/image-generated art is useful as reference,
 * but the live atlases are constrained rewrites so the model's teeth, eyes,
 * command block, and body islands do not slide out of place.
 */
public final class StormSkins {
    private static final Identifier LEGACY_CLASSIC = id("textures/entity/wither_storm.png");
    private static final Identifier LEGACY_OG = id("textures/entity/wither_storm_og.png");
    private static final Identifier PHASE4_CLASSIC = id("textures/entity/phase_4_assets.png");
    private static final Identifier PHASE4_OG = id("textures/entity/phase_4_assets_og.png");
    private static final Identifier PHASE55_CLASSIC = id("textures/entity/phase_4_assets_p55.png");
    private static final Identifier PHASE55_OG = id("textures/entity/phase_4_assets_og_p55.png");
    private static final Identifier PHASE6_CLASSIC = id("textures/entity/phase_4_assets_p6.png");
    private static final Identifier PHASE6_OG = id("textures/entity/phase_4_assets_og_p6.png");
    private static final Identifier PHASE7_CLASSIC = id("textures/entity/phase_4_assets_p7.png");
    private static final Identifier PHASE7_OG = id("textures/entity/phase_4_assets_og_p7.png");
    private static final Identifier DEVOURER_CLASSIC = id("textures/entity/devourer_assets.png");
    private static final Identifier DEVOURER_OG = id("textures/entity/devourer_assets_og.png");
    private static final Identifier DEVOURER55_CLASSIC = id("textures/entity/devourer_assets_p55.png");
    private static final Identifier DEVOURER55_OG = id("textures/entity/devourer_assets_og_p55.png");
    private static final Identifier DEVOURER6_CLASSIC = id("textures/entity/devourer_assets_p6.png");
    private static final Identifier DEVOURER6_OG = id("textures/entity/devourer_assets_og_p6.png");
    private static final Identifier DEVOURER7_CLASSIC = id("textures/entity/devourer_assets_p7.png");
    private static final Identifier DEVOURER7_OG = id("textures/entity/devourer_assets_og_p7.png");

    private static volatile double phaseHint = 0.0D;

    private StormSkins() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("dabywitherstormmod", path);
    }

    public static void setPhaseHint(double phase) {
        phaseHint = phase;
    }

    public static boolean og() {
        return Math.round(DabyWSClientConfig.stormSkin) >= 1L;
    }

    public static Identifier legacy() {
        return og() ? LEGACY_OG : LEGACY_CLASSIC;
    }

    public static Identifier phase4() {
        boolean ogSkin = og();
        double p = phaseHint;
        if (p >= 7.0D) return ogSkin ? PHASE7_OG : PHASE7_CLASSIC;
        if (p >= 6.0D) return ogSkin ? PHASE6_OG : PHASE6_CLASSIC;
        if (p >= 5.5D) return ogSkin ? PHASE55_OG : PHASE55_CLASSIC;
        return ogSkin ? PHASE4_OG : PHASE4_CLASSIC;
    }

    public static Identifier devourer() {
        boolean ogSkin = og();
        double p = phaseHint;
        if (p >= 7.0D) return ogSkin ? DEVOURER7_OG : DEVOURER7_CLASSIC;
        if (p >= 6.0D) return ogSkin ? DEVOURER6_OG : DEVOURER6_CLASSIC;
        if (p >= 5.5D) return ogSkin ? DEVOURER55_OG : DEVOURER55_CLASSIC;
        return ogSkin ? DEVOURER_OG : DEVOURER_CLASSIC;
    }

    public static Identifier teethGlow(double phase) {
        setPhaseHint(phase);
        boolean ogSkin = DabyWSClientConfig.stormSkin >= 0.5;
        String path;
        if (phase >= 7.0) {
            path = ogSkin ? "textures/entity/wither_storm_og_p7_e.png" : "textures/entity/wither_storm_p7_e.png";
        } else if (phase >= 6.0) {
            path = ogSkin ? "textures/entity/wither_storm_og_p6_e.png" : "textures/entity/wither_storm_p6_e.png";
        } else if (phase >= 5.5) {
            path = ogSkin ? "textures/entity/wither_storm_og_p55_e.png" : "textures/entity/wither_storm_p55_e.png";
        } else if (phase >= 5.1) {
            path = ogSkin ? "textures/entity/wither_storm_og_p51_e.png" : "textures/entity/wither_storm_p51_e.png";
        } else if (phase >= 5.0) {
            path = ogSkin ? "textures/entity/wither_storm_og_p5_e.png" : "textures/entity/wither_storm_p5_e.png";
        } else if (phase >= 4.0) {
            path = ogSkin ? "textures/entity/wither_storm_og_e.png" : "textures/entity/wither_storm_e.png";
        } else {
            path = "textures/entity/wither_storm_no_teeth_glow_e.png";
        }
        return Identifier.fromNamespaceAndPath("dabywitherstormmod", path);
    }
}
