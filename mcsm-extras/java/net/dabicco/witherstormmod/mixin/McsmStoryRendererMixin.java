package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.DabyWitherStormModClient;
import net.mcsm.extras.client.StoryCharacterRenderer;
import net.mcsm.extras.entity.McsmEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.9.205 -- registers the Story Mode character renderer alongside the base
 * mod's own EntityRenderers.register calls (same init method, same API).
 */
@Mixin(DabyWitherStormModClient.class)
public abstract class McsmStoryRendererMixin {

    @Inject(method = "onInitializeClient", at = @At("TAIL"), remap = false, require = 0)
    private void mcsm$storyRenderer(CallbackInfo ci) {
        try {
            if (McsmEntities.STORY_CHARACTER != null) {
                EntityRenderers.register(McsmEntities.STORY_CHARACTER, StoryCharacterRenderer::new);
            }
        } catch (Throwable ignored) {
        }
    }
}
