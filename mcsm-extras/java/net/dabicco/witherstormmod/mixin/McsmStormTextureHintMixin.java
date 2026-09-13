package net.dabicco.witherstormmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dabicco.witherstormmod.client.StormSkins;
import net.dabicco.witherstormmod.entity.renderer.WitherStormRenderer;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Updates texture selection at submit time, so interleaved storms cannot borrow a phase. */
@Mixin(WitherStormRenderer.class)
public abstract class McsmStormTextureHintMixin {
    @Inject(method = "submit", at = @At("HEAD"), remap = false, require = 0)
    private void mcsm$setTexturePhase(WitherStormRenderState state, PoseStack poseStack,
                                      SubmitNodeCollector collector, CameraRenderState camera,
                                      CallbackInfo ci) {
        StormSkins.setPhaseHint(state.phase);
    }
}
