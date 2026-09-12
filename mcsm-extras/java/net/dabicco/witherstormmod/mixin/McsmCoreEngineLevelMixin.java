package net.dabicco.witherstormmod.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;

import net.mcsm.extras.client.McsmCoreEngineController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the shared cinematic ambient pass after the world scene is complete. */
@Mixin(value = LevelRenderer.class, priority = 700)
public abstract class McsmCoreEngineLevelMixin {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;"
                    + "Lnet/minecraft/client/DeltaTracker;Z"
                    + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"
                    + "Lorg/joml/Matrix4fc;"
                    + "Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
                    + "Lorg/joml/Vector4f;Z)V",
            at = @At("RETURN"), remap = false, require = 1)
    private void mcsm$renderCorePass(GraphicsResourceAllocator allocator,
            DeltaTracker deltaTracker, boolean renderBlockOutline,
            CameraRenderState cameraState, Matrix4fc frustumMatrix,
            GpuBufferSlice fogBuffer, Vector4f fogColor, boolean skyVisible,
            CallbackInfo ci) {
        McsmCoreEngineController.renderAmbientPass(cameraState);
    }
}
