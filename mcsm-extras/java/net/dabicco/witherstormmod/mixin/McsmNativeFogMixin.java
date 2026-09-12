package net.dabicco.witherstormmod.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.mcsm.extras.client.McsmNativeSkyRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the native sky endpoint to Minecraft's existing fog calculation.
 * This is a state colour adjustment only; it does not draw a fog plane or a
 * second sky layer.
 */
@Mixin(FogRenderer.class)
public abstract class McsmNativeFogMixin {
    @Inject(method = "computeFogColor", at = @At("TAIL"), require = 1)
    private void mcsm$blendNativeSky(Camera camera, float partialTick, ClientLevel level,
            int renderDistance, float darkenAmount, Vector4f color, CallbackInfo ci) {
        float[] sky = new float[3];
        float opacity = McsmNativeSkyRenderer.fogColor(level, sky);
        if (opacity <= 0.0F) {
            return;
        }
        // The vanilla fog mixin may already have contributed weather, biome,
        // or story colour. Preserve that contribution under the native 0.80
        // atmosphere ceiling instead of replacing it with an opaque overlay.
        color.x = color.x * (1.0F - opacity) + sky[0] * opacity;
        color.y = color.y * (1.0F - opacity) + sky[1] * opacity;
        color.z = color.z * (1.0F - opacity) + sky[2] * opacity;
    }
}
