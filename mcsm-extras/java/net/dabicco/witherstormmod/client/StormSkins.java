package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.resources.Identifier;

/**
 * Texture policy for every native storm model pass.
 *
 * Phase 0 deliberately keeps its tiny starter atlas. Every later storm body,
 * head, jaw, skull, neck, tentacle, detached piece, and distant preview uses
 * the dark Phase 6 body sheet. Emissive layers may still use their dedicated
 * eye/teeth sheets through RenderTypes.eyes(...).
 */
public final class StormSkins {
    private static final Identifier LEGACY_CLASSIC = id("textures/entity/wither_storm.png");
    private static final Identifier LEGACY_OG = id("textures/entity/wither_storm_og.png");
    private static final Identifier PHASE6_BODY = id("textures/entity/wither_storm/wither_storm.png");

    // Retained as compatibility constants for callers that still ask for the
    // old phase ladder. They are intentionally no longer selected for bodies.
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

    /**
     * Phase 0 only: retain the tiny starter model's original atlas. Once the
     * entity has entered Phase 1, the universal Phase 6 body sheet takes over.
     */
    public static Identifier legacy() {
        return phaseHint >= 1.0D ? PHASE6_BODY : (og() ? LEGACY_OG : LEGACY_CLASSIC);
    }

    /** Select the universal skin without changing the Phase 0 starter atlas. */
    public static Identifier body(double phase) {
        setPhaseHint(phase);
        return phase >= 1.0D ? PHASE6_BODY : (og() ? LEGACY_OG : LEGACY_CLASSIC);
    }

    /** The one opaque body atlas used by all Phase 1 and later model passes. */
    public static Identifier phase6Body() {
        return PHASE6_BODY;
    }

    /** Compatibility name used by older renderer bytecode; head/body callers are Phase 1+. */
    public static Identifier phase4() {
        return PHASE6_BODY;
    }

    /** Detached/devourer pieces use the same universal body atlas. */
    public static Identifier devourer() {
        return PHASE6_BODY;
    }

    /** Actual emissive teeth atlases; never bind the opaque body sheet as glow. */
    public static Identifier teethGlow(double phase) {
        setPhaseHint(phase);
        boolean ogSkin = DabyWSClientConfig.stormSkin >= 0.5;
        String path;
        if (phase >= 7.0D) {
            path = ogSkin ? "textures/entity/wither_storm_og_p7_e.png" : "textures/entity/wither_storm_p7_e.png";
        } else if (phase >= 6.0D) {
            path = ogSkin ? "textures/entity/wither_storm_og_p6_e.png" : "textures/entity/wither_storm_p6_e.png";
        } else if (phase >= 5.5D) {
            path = ogSkin ? "textures/entity/wither_storm_og_p55_e.png" : "textures/entity/wither_storm_p55_e.png";
        } else if (phase >= 5.1D) {
            path = ogSkin ? "textures/entity/wither_storm_og_p51_e.png" : "textures/entity/wither_storm_p51_e.png";
        } else if (phase >= 5.0D) {
            path = ogSkin ? "textures/entity/wither_storm_og_p5_e.png" : "textures/entity/wither_storm_p5_e.png";
        } else if (phase >= 4.0D) {
            path = ogSkin ? "textures/entity/wither_storm_og_e.png" : "textures/entity/wither_storm_e.png";
        } else {
            path = "textures/entity/wither_storm_no_teeth_glow_e.png";
        }
        return id(path);
    }
}
