package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.Camera;

/**
 * Devouring Storms: Story Mode third-person camera spacing.
 *
 * Minecraft's detached camera normally pulls back about four blocks.  The
 * Story Mode shots the user provided sit farther behind the avatar so the
 * full body, track/bridge, and incoming threat all fit in frame.  This mixin
 * widens only that third-person pullback constant; first-person and cinematic
 * effects keep their normal behavior.  require=0 keeps older/newer camera
 * bytecode from hard-crashing if Mojang moves the literal in a later 26.x jar.
 */
@Mixin(Camera.class)
public abstract class McsmStoryCameraMixin {

    @ModifyConstant(method = "update", constant = @Constant(doubleValue = 4.0D), require = 0)
    private double dabyws$storyModeThirdPersonDistance(double original) {
        return 6.35D;
    }

    @ModifyConstant(method = "update", constant = @Constant(floatValue = 4.0F), require = 0)
    private float dabyws$storyModeThirdPersonDistanceFloat(float original) {
        return 6.35F;
    }
}
