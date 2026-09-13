package net.dabicco.witherstormmod.mixin;

import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Keeps the base config entry name while the native implementation lives in
 * McsmStormSkyColorPatch.  The former implementation darkened the vanilla
 * sky state and is intentionally not retained here.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
}
