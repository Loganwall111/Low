package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dabicco.witherstormmod.client.StormDebris;

/**
 * Kill StormDebris cube halo / spiral ring / billboard debris field entirely.
 * Matches every overload by simple name; require=0 so a rename is safe.
 */
@Mixin(StormDebris.class)
public abstract class McsmDebrisKillPatch {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void mcsm$killSubmit(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "submitEarly", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void mcsm$killEarly(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "submitSeveredCloud", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void mcsm$killSevered(CallbackInfo ci) {
        ci.cancel();
    }
}
