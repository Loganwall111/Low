package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.client.FoglessRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MCSM — MCSM matte silhouette.
 *
 * bodyCutout / reverseShading / modelShading (STORM_SHADING hemisphere) all
 * paint grey cube faces, blue under-glow and "earring" edge highlights on the
 * body. Force every custom path OFF so the storm is a pitch-black matte mass
 * like the Telltale stills; teeth/eyes stay on the emissive layer.
 */
@Mixin(value = FoglessRenderTypes.class, remap = false)
public abstract class McsmStormVisibilityPatch {

    @Inject(method = "fogless", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mcsm$forceVanillaBodyPath(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Boolean.FALSE);
    }

    @Inject(method = "reverseShading", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mcsm$forceFlatShading(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Boolean.FALSE);
    }

    @Inject(method = "modelShading", at = @At("HEAD"), cancellable = true, require = 0)
    private static void mcsm$noHemisphereShade(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Boolean.FALSE);
    }
}
