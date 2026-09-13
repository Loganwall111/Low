package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.entity.renderer.WitherStormRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Routes the native teeth overlay to vanilla's unlit eyes pipeline. */
@Mixin(WitherStormRenderer.class)
public abstract class McsmBodyEyesRenderTypeMixin {
    @Redirect(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/dabicco/witherstormmod/client/FoglessRenderTypes;eyes(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            remap = false,
            require = 0)
    private static RenderType mcsm$bodyEyes(Identifier texture) {
        return RenderTypes.eyes(texture);
    }
}
