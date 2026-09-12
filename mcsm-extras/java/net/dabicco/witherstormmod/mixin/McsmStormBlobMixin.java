package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dabicco.witherstormmod.client.StormBackdrop;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.mcsm.extras.client.McsmExperimentalStoryStage;
import net.mcsm.extras.client.McsmStormBlob;

/**
 * Devouring Storms: mega-phase 2 - hands the storm blob over to the
 * corrected re-submission (pinkish-violet 5.5-5.9, centred dark core,
 * smoothed skybox glide, weaker red ember). Client-only mixin on the base
 * mod's own StormBackdrop; the replacement draws nothing unless the game
 * client is up, and honours every base config toggle.
 */
@Mixin(StormBackdrop.class)
public abstract class McsmStormBlobMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dabyws$correctedBlob(LevelRenderContext ctx, CallbackInfo ci) {
        if (Minecraft.getInstance() != null) {
            // Stage geometry is submitted in the same collect-submits pass as
            // the storm, so the dome/pad is behind entity geometry and the
            // existing glare bridge remains untouched.
            McsmExperimentalStoryStage.submit(ctx);
            McsmStormBlob.submit(ctx);
        }
        ci.cancel();
    }
}
