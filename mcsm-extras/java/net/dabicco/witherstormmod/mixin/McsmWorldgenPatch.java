package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dabicco.witherstormmod.structures.McsmSchematic;
import net.dabicco.witherstormmod.structures.McsmWorldgen;
import net.mcsm.extras.McsmDiag;
import net.mcsm.extras.McsmExtrasConfig;
import net.mcsm.extras.McsmNpcs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Mega-phase 7 / 7b / 9 / 12: structures land WHOLE, Sky City goes up among
 * the cloud decks, and towns get their cast (McsmNpcs).
 *
 * MCSM CRASH FIX: McsmWorldgen.tick(ServerLevel) returns int. Mixin
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

    private static ServerLevel lastOverworld;
    private static ServerLevel autoTownsFor;
    private static final ThreadLocal<Boolean> RAISING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // 1.9.302 -- the story starts itself: the Episode-1 opening cluster
    // (treehouse, wilderness, EnderCon fair) is queued on the first tick of
    // a fresh overworld, so structures + NPCs appear without /ds towns start.
    private static final String[] EPISODE_ONE_TOWNS = {
            "Wilderness Treehouse", "The Wilderness", "EnderCon Town Fair",
    };

    /** tick(ServerLevel) -> int. CIR required (MCSM crash fix). */
    @Inject(method = "tick", at = @At("HEAD"), remap = false, require = 0)
    private static void dabyws$wholeStructures(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        try {
            // 1.9.304 CRASH FIX -- the base registers this tick for EVERY
            // loaded ServerLevel (overworld, nether, end AND the mod's
            // bowels dimension). Keying the logic on a bare
            // "lastLevel != level" flipped true on every invocation, so the
            // queue was wiped every tick and the Episode-1 schematics were
            // re-enqueued forever: the server overloaded ("Can't keep up!
            // ... ticks behind") and the game hung. Manage the OVERWORLD
            // only, keyed by its identity, once per world load. This is the
            // same flaw that made /ds towns build look like a no-op: the
            // queue was cleared before anything could place.
            if (level.dimension() != Level.OVERWORLD) {
                return;
            }
            // The experimental stage is explicitly render-only.  In an
            // integrated world the client and server share this config file;
            // cancel the existing automatic town queue so the stage cannot
            // spend minutes placing physical structures or force thousands of
            // chunk rebuilds behind the player's back.  Dedicated servers
            // keep their own config and therefore remain unchanged.
            McsmExtrasConfig.load();
            if (McsmExtrasConfig.ENABLE_EXPERIMENTAL_STORY_MODE_STAGE) {
                McsmWorldgen.clear();
                return;
            }
            if (lastOverworld != level) {
                lastOverworld = level;
                McsmWorldgen.clear();
            }
            if (autoTownsFor != level) {
                autoTownsFor = level;
                autoStartTowns(level);
            }
            // 1.9.194 native-memory fix: placing hundreds of thousands of
            // structure blocks in one tick forces Sodium/Iris to rebuild too
            // many chunk meshes at once and can kill the JVM with
            // "Native memory allocation (malloc) failed ... Chunk::new".
            // Keep the structure queue alive, but spread it across frames.
            McsmWorldgen.setBudget(4096);
            // mega-phase 9: the towns get their cast, and their dialogue hook
            try {
                McsmNpcs.tick(level);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
            // never take the world tick down — budget/NPC fail soft
        }
    }

    /** 1.9.304 -- the story starts itself, exactly once per world load, and
     *  never rebuilds a site that is already there (older world, or the
     *  base's own Episode-1 flow got there first). */
    private static void autoStartTowns(ServerLevel level) {
        try {
            McsmExtrasConfig.load();
            if (!McsmExtrasConfig.autoStartTowns) {
                return;
            }
            int built = 0;
            for (String want : EPISODE_ONE_TOWNS) {
                for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
                    if (!s.label().equalsIgnoreCase(want)) {
                        continue;
                    }
                    BlockPos origin = new BlockPos(s.x(), s.y(), s.z());
                    try {
                        // already built? skip (probe keeps re-builds and
                        // double-builds from ever stacking up).
                        if (level.isLoaded(origin) && !level.getBlockState(origin).isAir()) {
                            break;
                        }
                        McsmSchematic sch = McsmSchematic.load(
                                level.getServer().getResourceManager(), s.path());
                        McsmWorldgen.enqueue(sch, origin, s.label());
                        built++;
                    } catch (Throwable ignored) {
                        // one town failing to load never stops the rest
                    }
                    break;
                }
            }
            if (built > 0) {
                McsmDiag.say("[ds] Story Mode towns queued (" + built
                        + "/" + EPISODE_ONE_TOWNS.length
                        + "). The cast spawns when you get near.");
            }
        } catch (Throwable ignored) {
            // auto-start must never take the world tick down
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
