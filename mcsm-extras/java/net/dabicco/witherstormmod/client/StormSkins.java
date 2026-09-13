package net.dabicco.witherstormmod.client;

import net.minecraft.resources.Identifier;

/**
 * Canonical phase texture registry for the 1.9.103 rendering baseline.
 *
 * All three sheets are 160x160 UV atlases sourced from the real CEM assets;
 * no generated colour filter or layout rewrite is applied at render time.
 */
public final class StormSkins {
    private static final Identifier AUTHENTIC_BODY = id("textures/entity/wither_storm/wither_storm.png");
    private static final Identifier PHASE_MID = id("textures/entity/wither_storm/phase_mid_skin.png");
    private static final Identifier PHASE_LATE = id("textures/entity/wither_storm/phase_late_skin.png");
    private static final Identifier PHASE_ENDGAME = id("textures/entity/wither_storm/phase_endgame_skin.png");

    private static volatile double phaseHint;

    private StormSkins() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("witherstormmod", path);
    }

    public static void setPhaseHint(double phase) {
        phaseHint = phase;
    }

    public static double phaseHint() {
        return phaseHint;
    }

    /** Compatibility signal for the old config surface; texture choice is canonical. */
    public static boolean og() {
        return false;
    }

    public static Identifier legacy() {
        return AUTHENTIC_BODY;
    }

    /** Phase 4 through 5.9. */
    public static Identifier phase4() {
        return phaseTexture(phaseHint);
    }

    /** Detached/devourer bodies remain on the same phase atlas. */
    public static Identifier devourer() {
        return phaseTexture(phaseHint);
    }

    /** Teeth/eye geometry samples the same phase atlas as the body. */
    public static Identifier teethGlow(double phase) {
        setPhaseHint(phase);
        return phaseTexture(phase);
    }

    private static Identifier phaseTexture(double phase) {
        if (phase >= 8.0D) {
            return PHASE_ENDGAME;
        }
        if (phase >= 6.0D) {
            return PHASE_LATE;
        }
        if (phase >= 4.0D) {
            return PHASE_MID;
        }
        return AUTHENTIC_BODY;
    }
}
