package net.dabicco.witherstormmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dabicco.witherstormmod.entity.renderer.WitherStormRenderer;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.mcsm.extras.client.McsmCoreEngineController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Attaches the physical overcast to the actual Wither Storm renderer.
 *
 * The HEAD injection is intentionally before WitherStormRenderer's own
 * chassis/head/tentacle submissions. The TAIL injection leaves the real
 * entity geometry and its ordinary local debris in front of the backdrop while
 * retaining the phase-7 Vortex asset as the final local debris layer.
 */
@Mixin(WitherStormRenderer.class)
public abstract class McsmAtmosphericMeshMixin {
    @Inject(method = "submit", at = @At("HEAD"), remap = false, require = 1)
    private void mcsm$submitAtmosphere(WitherStormRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        McsmCoreEngineController.update(state);
        // The attached curved mask was rendering as a bright oval anchored in
        // the ground. Keep the component source for compatibility, but do not
        // submit that retired world geometry; StormBackdrop owns the backdrop.
    }

    @Inject(method = "submit", at = @At("TAIL"), remap = false, require = 1)
    private void mcsm$submitAttachedVortex(WitherStormRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        // The generated attached Vortex mesh is retired. Native model debris
        // remains in WitherStormRenderer and is expanded only for phase 9.
    }
}
