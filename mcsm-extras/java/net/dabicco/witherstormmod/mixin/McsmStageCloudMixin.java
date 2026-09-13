package net.dabicco.witherstormmod.mixin;

import net.minecraft.client.renderer.CloudRenderer;
import net.mcsm.extras.client.McsmNativeSkyRenderer;
import net.mcsm.extras.client.McsmExperimentalStoryStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide infinite native clouds only from the explicit black stage exterior. */
@Mixin(CloudRenderer.class)
public abstract class McsmStageCloudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcsm$hideExteriorStageClouds(CallbackInfo ci) {
        if (McsmExperimentalStoryStage.active() && McsmNativeSkyRenderer.stageOutside()) {
            ci.cancel();
        }
    }
}
