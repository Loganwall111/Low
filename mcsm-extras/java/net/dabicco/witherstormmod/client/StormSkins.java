package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.resources.Identifier;

/**
 * Storm body + teeth emissive atlas selection.
 * Teeth glow is phase-dynamic (user frames):
 *   phase 3          no glow
 *   phase 4          slight light-cyan glow
 *   phase 5          flat white, low/non-glowing
 *   phase 5.5        glowing white
 *   phase 6          blue glowing split-phase teeth
 *   phase 7+         green-blue glowing teeth
 */
public final class StormSkins {
    private static final Identifier LEGACY_CLASSIC = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/wither_storm.png");
    private static final Identifier LEGACY_OG = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/wither_storm_og.png");
    private static final Identifier PHASE4_CLASSIC = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/phase_4_assets.png");
    private static final Identifier PHASE4_OG = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/phase_4_assets_og.png");
    private static final Identifier DEVOURER_CLASSIC = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/devourer_assets.png");
    private static final Identifier DEVOURER_OG = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/devourer_assets_og.png");

    private StormSkins() {
    }

    public static boolean og() {
        return Math.round(DabyWSClientConfig.stormSkin) >= 1L;
    }

    public static Identifier legacy() {
        return og() ? LEGACY_OG : LEGACY_CLASSIC;
    }

    public static Identifier phase4() {
        return og() ? PHASE4_OG : PHASE4_CLASSIC;
    }

    public static Identifier devourer() {
        return og() ? DEVOURER_OG : DEVOURER_CLASSIC;
    }

    public static Identifier teethGlow(double phase) {
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
