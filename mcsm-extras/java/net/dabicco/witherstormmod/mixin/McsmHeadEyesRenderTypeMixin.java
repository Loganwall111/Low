package net.dabicco.witherstormmod.mixin;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps native head teeth and eye geometry on the direct eyes material. */
@Mixin(net.dabicco.witherstormmod.entity.renderer.WitherStormHeadRenderer.class)
public abstract class McsmHeadEyesRenderTypeMixin {
    @Redirect(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/dabicco/witherstormmod/client/GlowRenderTypes;emitterMark(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            remap = false,
            require = 0)
    private static RenderType mcsm$headEmitterEyes(Identifier texture) {
        return RenderTypes.eyes(texture);
    }

    @Redirect(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/dabicco/witherstormmod/client/GlowRenderTypes;bloomSource(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            remap = false,
            require = 0)
    private static RenderType mcsm$headBloomEyes(Identifier texture) {
        return RenderTypes.eyes(texture);
    }

    /**
     * The native Phase 6 head has separate glow/eye model parts, but the
     * renderer historically fed both passes the opaque body atlas. Redirect
     * those eyes calls to the matching 512x512 transparent emissive atlas;
     * this keeps the uploaded universal body texture out of eye/teeth glow.
     */
    @Redirect(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;eyes(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            remap = false,
            require = 0)
    private static RenderType mcsm$headDedicatedEmissive(Identifier texture) {
        return RenderTypes.eyes(net.dabicco.witherstormmod.client.StormSkins.phase6Emissive());
    }
}
