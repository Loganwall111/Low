package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.client.StormDebris;
import net.mcsm.extras.client.McsmPhase9DebrisState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Reuses the base StormDebris arrays for the phase-9 expansion. The normal
 * renderer stops at 2,500 entries; the late authored outer entries continue
 * through 5,880. No new model or procedural black ring is introduced.
 */
@Mixin(StormDebris.class)
public abstract class McsmPhase9DebrisCountMixin {
    @ModifyConstant(method = "submit", constant = @Constant(intValue = 2500),
            require = 0, remap = false)
    private static int mcsm$useAuthoredOuterDebris(int original) {
        return McsmPhase9DebrisState.active() ? 5880 : original;
    }
}
