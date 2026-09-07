package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dabicco.witherstormmod.client.StormPresenceFX;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

/**
 * Mega-phase 7b: kill the far three-headed HALO ring / black-glare symbol
 * that StormPresenceFX draws behind the storm. Those two knobs paint the
 * halo_ring.png texture at bodyR*1.3..1.9 — the "three heads / symbol far
 * behind the wither" the user keeps rejecting.
 *
 * We force the knobs OFF every frame (so presets/gates cannot re-enable them)
 * and leave the rest of submit() alone: atmospherePulse, glareEjecta debris
 * and the beat tick still run. The thick welded shell aura is owned by
 * McsmStormBlob instead.
 *
 * require=0 so a renamed base method degrades silently.
 */
@Mixin(StormPresenceFX.class)
public abstract class McsmPresenceFxPatch {

    @Inject(method = "submit", at = @At("HEAD"), remap = false, require = 0)
    private static void dabyws$killFarHalo(LevelRenderContext ctx, CallbackInfo ci) {
        try {
            DabyWSClientConfig.cataclysmHalos = false;
            DabyWSClientConfig.blackGlare = false;
        } catch (Throwable ignored) {
        }
    }
}
