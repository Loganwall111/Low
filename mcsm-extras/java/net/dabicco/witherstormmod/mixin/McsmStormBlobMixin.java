package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dabicco.witherstormmod.client.StormBackdrop;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.mcsm.extras.client.McsmStormBlob;

/**
 * 1.9.168: cancel base StormBackdrop and submit teeth+beams only.
 * PhaseSky nested-sphere glare (dotted circle / grid lines) is wiped.
 */
@Mixin(StormBackdrop.class)
public abstract class McsmStormBlobMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dabyws$correctedBlob(LevelRenderContext ctx, CallbackInfo ci) {
        if (Minecraft.getInstance() != null) {
            McsmStormBlob.submit(ctx);
        }
        ci.cancel();
    }
}
