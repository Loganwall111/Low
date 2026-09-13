package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.entity.renderer.WitherStormHeadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps skull, jaw, neck, and head body vertices at #FFFFFF. */
@Mixin(WitherStormHeadRenderer.class)
public abstract class McsmHeadBodyNeutralTintMixin {
    @Redirect(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/dabicco/witherstormmod/entity/renderer/WitherStormHeadRenderer;deadTint(F)I"),
            remap = false,
            require = 0)
    private static int mcsm$neutralHeadBodyTint(float lit) {
        return -1;
    }
}
