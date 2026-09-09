package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dabicco.witherstormmod.client.StormPresenceFX;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

/**
 * CANCEL the entire StormPresenceFX pass.
 *
 * That pass paints the far three-headed HALO ring (halo_ring.png), the
 * black-glare symbol, and the floating atmosphere-pulse spheres the user
 * keeps rejecting as "weird halo floating in mid air" / "face on it".
 *
 * All aura / glare / silhouette now lives in McsmStormBlob (body-glued) and
 * McsmPhaseSky (sky-glued to the storm bearing). require=0 so a rename is safe.
 */
@Mixin(StormPresenceFX.class)
public abstract class McsmPresenceFxPatch {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dabyws$killAllPresenceFx(LevelRenderContext ctx, CallbackInfo ci) {
        try {
            DabyWSClientConfig.cataclysmHalos = false;
            DabyWSClientConfig.blackGlare = false;
            DabyWSClientConfig.atmospherePulse = false;
            DabyWSClientConfig.glareEjecta = false;
        } catch (Throwable ignored) {
        }
        ci.cancel();
    }
}
