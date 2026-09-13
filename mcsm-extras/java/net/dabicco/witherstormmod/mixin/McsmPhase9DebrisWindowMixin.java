package net.dabicco.witherstormmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dabicco.witherstormmod.entity.renderer.WitherStormRenderer;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.mcsm.extras.client.McsmPhase9DebrisState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks the existing entity debris call while its phase-9 frame is submitted. */
@Mixin(WitherStormRenderer.class)
public abstract class McsmPhase9DebrisWindowMixin {
    @Inject(method = "submit", at = @At("HEAD"), remap = false, require = 1)
    private void mcsm$phase9Begin(WitherStormRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        // The recovered entity ladder tops out at 6.99; 6.80+ is its phase-9
        // finale window. Explicit editor values 8/9 also pass this gate.
        McsmPhase9DebrisState.set(state != null && state.phase >= 6.80D);
    }

    @Inject(method = "submit", at = @At("TAIL"), remap = false, require = 1)
    private void mcsm$phase9End(WitherStormRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        McsmPhase9DebrisState.set(false);
    }
}
