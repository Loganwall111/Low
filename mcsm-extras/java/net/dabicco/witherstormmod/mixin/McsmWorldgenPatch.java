package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dabicco.witherstormmod.structures.McsmSchematic;
import net.dabicco.witherstormmod.structures.McsmWorldgen;
import net.mcsm.extras.McsmNpcs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Mega-phase 7 / 7b / 9 / 12: structures land WHOLE, Sky City goes up among
 * the cloud decks, and towns get their cast (McsmNpcs).
 *
 * 1.9.150 CRASH FIX: McsmWorldgen.tick(ServerLevel) returns int. Mixin
 * injects on a returning method MUST take CallbackInfoReturnable, not plain
 * CallbackInfo — otherwise APPLY fails with InvalidInjectionException and
 * the whole world tick dies the moment McsmWorldgen is first classloaded.
 *
 * The base places every schematic through a static queue with a 24k
 * blocks/tick budget (visible "segments"), and that static queue survives
 * world loads so leftovers from the previous world keep placing into the
 * new one ("scattered fragments"). Both are fixed here: the queue is
 * cleared whenever the level instance changes, and the budget is raised so
 * each schematic completes in about a tick.
 *
 * The floating sites (Sky City y=296 and siblings) are raised +3904 via an
 * enqueue HEAD intercept: the shipped mixin jar has no ModifyReturnValue,
 * so we cancel the original enqueue and re-enqueue with the raised origin.
 * Floating y values sit at 276-308; ground sites sit at 34-64, so the
 * (200, 1000) window isolates them cleanly. The re-entered call sees
 * y~4200 and passes through untouched (ThreadLocal re-entry guard).
 */
@Mixin(McsmWorldgen.class)
public abstract class McsmWorldgenPatch {

    private static ServerLevel lastLevel;
    private static final ThreadLocal<Boolean> RAISING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** tick(ServerLevel) -> int. CIR required (1.9.150 crash fix). */
    @Inject(method = "tick", at = @At("HEAD"), remap = false, require = 0)
    private static void dabyws$wholeStructures(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        try {
            if (lastLevel != level) {
                lastLevel = level;
                McsmWorldgen.clear();
            }
            McsmWorldgen.setBudget(900000);
            // mega-phase 9: the towns get their cast, and their dialogue hook
            try {
                McsmNpcs.tick(level);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
            // never take the world tick down — budget/NPC fail soft
        }
    }

    @Inject(method = "enqueue", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dabyws$skyCityAltitude(McsmSchematic sch, BlockPos origin, String label,
            CallbackInfo ci) {
        if (Boolean.TRUE.equals(RAISING.get())) {
            return;
        }
        int y = origin.getY();
        // floating sites only (Sky City 296, Speakeasy 284, Jungle Fortress
        // 276, Mushroom Island 308) - ground towns live below y=100
        if (y > 200 && y < 1000) {
            RAISING.set(Boolean.TRUE);
            try {
                McsmWorldgen.enqueue(sch,
                        new BlockPos(origin.getX(), y + 3904, origin.getZ()), label);
            } finally {
                RAISING.set(Boolean.FALSE);
            }
            ci.cancel();
        }
    }
}
