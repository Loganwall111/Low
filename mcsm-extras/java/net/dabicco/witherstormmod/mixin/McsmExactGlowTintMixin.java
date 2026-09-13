package net.dabicco.witherstormmod.mixin;

import net.mcsm.extras.client.McsmTeethPhaseTint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents shader/config gain from changing the requested phase colors. */
@Mixin(net.dabicco.witherstormmod.entity.renderer.WitherStormHeadRenderer.class)
public abstract class McsmExactGlowTintMixin {
    @Inject(method = "glowTint", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void mcsm$exactTeeth(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(McsmTeethPhaseTint.teethTintArgb());
    }

    @Inject(method = "eyeTint", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void mcsm$exactEyes(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(McsmTeethPhaseTint.eyeTintArgb());
    }
}
