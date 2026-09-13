package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ships the requested backdrop defaults with the overlay even when the base
 * jar still contains the older config class. This only establishes defaults;
 * a later JSON load or an in-game config change remains authoritative.
 */
@Mixin(DabyWSClientConfig.class)
public abstract class McsmBackdropDefaultsMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false, require = 0)
    private static void mcsm$backdropDefaults(CallbackInfo ci) {
        DabyWSClientConfig.stormBackdrop = true;
        DabyWSClientConfig.stormBackdropQuad = true;
        DabyWSClientConfig.stormBackdropBlack = false;
    }
}
