package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dabicco.witherstormmod.structures.McsmSchematic;
import net.dabicco.witherstormmod.structures.McsmWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Mega-phase 7: structures land WHOLE, and Sky City goes up among the
 * cloud decks (user orders: "structures never in segments", "no scattered
 * Sky City fragments", "Sky City thousands of blocks up").
 *
 * The base mod places every schematic through a static queue with a 24k
 * blocks/tick budget, slicing towns upward over many ticks (the visible
 * "segments"), and that static queue survives world loads, so leftovers
 * from the previous world keep placing into the new one (the "scattered
 * fragments"). Both are fixed here: the queue is cleared whenever the
 * level instance changes, and the budget is raised so each schematic
 * completes in about a tick.
 *
 * Altitude: the shipped Mixin jar predates ModifyReturnValue, so the
 * raise hooks enqueue() instead. Ground sites top out at y=64; the
 * floating ones (Sky City 296, Speakeasy 284, Jungle Fortress 276,
 * Mushroom Island 308) are the only jobs in the 200..1000 band, so they
 * get re-enqueued 3904 blocks higher and the original call is cancelled.
 * The re-entered call sees y ~4200 and passes straight through.
 */
@Mixin(McsmWorldgen.class)
public abstract class McsmWorldgenPatch {

    private static ServerLevel lastLevel;

    @Inject(method = "tick", at = @At("HEAD"), remap = false, require = 0)
    private static void dabyws$wholeStructures(ServerLevel level, CallbackInfo ci) {
        if (lastLevel != level) {
            lastLevel = level;
            McsmWorldgen.clear();
        }
        McsmWorldgen.setBudget(900000);
    }

    @Inject(method = "enqueue", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dabyws$skyCityAltitude(McsmSchematic sch, BlockPos origin, String label, CallbackInfo ci) {
        int y = origin.getY();
        if (y > 200 && y < 1000) {
            McsmWorldgen.enqueue(sch, new BlockPos(origin.getX(), y + 3904, origin.getZ()), label);
            ci.cancel();
        }
    }
}
