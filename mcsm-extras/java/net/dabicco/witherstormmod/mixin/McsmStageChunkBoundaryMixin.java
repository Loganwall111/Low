package net.dabicco.witherstormmod.mixin;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.mcsm.extras.client.McsmExperimentalStoryStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops the client render-side chunk request at the 16x16 stage perimeter.
 *
 * The `requireChunk` distinction is important: render queries may safely get
 * null and skip a section, while gameplay/block queries still receive the
 * real server chunk.  This is deliberately not a server ChunkGenerator mixin;
 * a client-only opt-in must never alter saves, server generation, or another
 * player's world.
 */
@Mixin(ClientChunkCache.class)
public abstract class McsmStageChunkBoundaryMixin {
    @Inject(method = "getChunk", at = @At("HEAD"), cancellable = true, require = 0)
    private void mcsm$clipStageRenderChunk(int chunkX, int chunkZ, ChunkStatus status,
            boolean requireChunk, CallbackInfoReturnable<?> cir) {
        if (!requireChunk) {
            // The level is intentionally obtained through the normal client
            // singleton rather than by reaching into private cache state.
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null
                    && !McsmExperimentalStoryStage.shouldRenderChunk(chunkX, chunkZ, mc.level)) {
                cir.setReturnValue(null);
            }
        }
    }
}
