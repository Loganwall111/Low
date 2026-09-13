package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.entity.renderer.WitherStormRenderer;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Body palette guard: phase atmosphere and emissive colours must never leak
 * into the opaque storm vertex multiplier.
 */
@Mixin(WitherStormRenderer.class)
public abstract class McsmStormBodyNeutralTintMixin {
    @Shadow
    private boolean previewShadowPass;

    @Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mcsm$neutralStormBodyTint(WitherStormRenderState state,
                                            CallbackInfoReturnable<Integer> cir) {
        if (!this.previewShadowPass) {
            cir.setReturnValue(-1);
        }
    }
}
