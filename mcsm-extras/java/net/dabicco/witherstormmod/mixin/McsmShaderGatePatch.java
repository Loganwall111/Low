package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.client.ShaderPackCompat;
import net.mcsm.extras.McsmDiag;
import net.mcsm.extras.McsmExtrasConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MCSM -- re-enable the mod's own visuals under Iris.
 *
 * Bytecode survey: ShaderPackCompat.active() gates SIX systems, every one with
 * "ifne <skip>" -- i.e. when a shader pack is loaded the mod switches its own
 * effects OFF and expects the pack to draw them:
 *
 *     StormSunGlow          -> sun glow + ground shadowing
 *     StormShadowMap        -> the storm's cast shadow
 *     StormImpactLights     -> coloured impact lighting
 *     StormBloom            -> halo / eye-glow bloom
 *     GlowRenderTypes       -> emitterMark: turquoise teeth + eye glow
 *     WitherStormHeadRenderer.shaderGlowGain()
 *
 * Under the "mod owns the look" architecture the pack no longer draws any of
 * it, so the handoff left nothing on screen. Forcing active() false makes the
 * mod render its own visuals whether Iris is on or off.
 *
 * Safe w.r.t. FoglessRenderTypes: fogless() is
 *     active && !legacyDistantRenderer && !ShaderPackCompat.active()
 * so this alone would ENABLE the broken bodyCutout path. McsmStormVisibilityPatch
 * overrides fogless()/reverseShading() at HEAD, so that path stays disabled.
 */
@Mixin(value = ShaderPackCompat.class, remap = false)
public abstract class McsmShaderGatePatch {

    /** Logged once so the log proves this patch is live. */
    private static boolean reported = false;

    @Inject(method = "active", at = @At("HEAD"), cancellable = true)
    private static void mcsm$modOwnsTheLook(CallbackInfoReturnable<Boolean> cir) {
        // MCSM -- the force is now a player-facing toggle. Dabicco's
        // look presets route part of their difference through the shader-pack
        // path this gate closes, so with it forced they appeared to do
        // nothing; turning "Shader Pack Gate" off in MCSM Extras hands the
        // answer back to the mod for an A/B test.
        McsmExtrasConfig.load();
        if (!McsmExtrasConfig.shaderPackGate) {
            return;
        }
        if (!reported) {
            reported = true;
            McsmDiag.banner();
            McsmDiag.say("ShaderPackCompat.active() forced FALSE -- the mod now"
                       + " draws its own sun glow, shadow map, impact lights,"
                       + " bloom, turquoise teeth and eye glow even under Iris.");
        }
        cir.setReturnValue(Boolean.FALSE);
    }
}
