package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dabicco.witherstormmod.client.StormBackdrop;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.mcsm.extras.client.McsmPhaseSky;
import net.mcsm.extras.client.McsmStormBlob;

/**
 * MCSM: cancel base StormBackdrop.
 * Order: thick MCSM glare volume first, then teeth + beams on top.
 */
@Mixin(StormBackdrop.class)
public abstract class McsmStormBlobMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dabyws$correctedBlob(LevelRenderContext ctx, CallbackInfo ci) {
        if (Minecraft.getInstance() != null) {
            McsmPhaseSky.submit(ctx);
            McsmStormBlob.submit(ctx);
        }
        ci.cancel();
    }
}
