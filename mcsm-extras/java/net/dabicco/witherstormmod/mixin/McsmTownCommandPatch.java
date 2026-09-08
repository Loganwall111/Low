package net.dabicco.witherstormmod.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.dabicco.witherstormmod.structures.McsmCommand;
import net.dabicco.witherstormmod.structures.McsmSchematic;
import net.dabicco.witherstormmod.structures.McsmWorldgen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Devouring Storms MCSM -- the town queue, made findable.
 *
 * Deep-scan of the base jar (ci/api/scan/) settled the "towns never build"
 * report. Nothing in the builder is broken:
 *
 *   - McsmWorldgen holds a static QUEUE; McsmCommand.build() loads each
 *     site's .schematic from the jar assets and enqueues it;
 *   - DabyWitherStormMod registers McsmWorldgen.tick(level) on
 *     ServerTickEvents.END_LEVEL_TICK, and tick() places up to budget
 *     (24000) blocks PER TICK -- a whole town appears in seconds, not
 *     "over the next few minutes" (that message is flavour);
 *   - every one of the 33 referenced schematics is present in the jar;
 *   - sites build at ABSOLUTE coordinates: ANCHOR_X=-640, ANCHOR_Z=256
 *     plus per-site offsets out to ~1400 blocks, y=64..296 (Sky City
 *     floats at y=296).
 *
 * So the queue DID run. The player simply was not standing at (-640, 64,
 * 256)-ish, and the discovery path is broken UX: "/mcsm tp all" is not a
 * thing ("No location called 'all'"), site keys are label-derived slugs
 * (Beacon Town -> beacon_town) that nothing ever shows, and the queued
 * message never prints coordinates.
 *
 * This adds /ds towns as the usable surface over THEIR machinery (no
 * reimplementation, we call their public methods):
 *
 *   /ds towns              list every site with absolute coordinates
 *   /ds towns build [site] queue one site or all of them, printing coords
 *   /ds towns tp <site>    teleport onto the site (tab-completes keys)
 *   /ds towns status       how many jobs are still pending
 *
 * Registered at the TAIL of their own McsmCommand.register(dispatcher), so
 * it lives and dies with their command surface and cannot register before
 * the dispatcher exists.
 */
@Mixin(value = McsmCommand.class, remap = false)
public abstract class McsmTownCommandPatch {

    private static final SuggestionProvider<CommandSourceStack> DS$SITES =
            (ctx, builder) -> {
                for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
                    builder.suggest(McsmCommand.key(s));
                }
                return CompletableFuture.completedFuture(builder.build());
            };

    @Inject(method = "register(Lcom/mojang/brigadier/CommandDispatcher;)V", at = @At("TAIL"))
    private static void ds$towns(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci) {
        try {
            LiteralArgumentBuilder<CommandSourceStack> towns = Commands.literal("towns");
            towns.executes(ctx -> ds$list(ctx.getSource()));
            towns.then(Commands.literal("build")
                    .executes(ctx -> ds$build(ctx.getSource(), "all"))
                    .then(Commands.argument("site", StringArgumentType.word())
                            .suggests(DS$SITES)
                            .executes(ctx -> ds$build(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "site")))));
            towns.then(Commands.literal("tp")
                    .then(Commands.argument("site", StringArgumentType.word())
                            .suggests(DS$SITES)
                            .executes(ctx -> ds$tp(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "site")))));
            towns.then(Commands.literal("status")
                    .executes(ctx -> ds$status(ctx.getSource())));
            towns.then(Commands.literal("start")
                    .executes(ctx -> ds$start(ctx.getSource())));
            dispatcher.register(Commands.literal("ds").then(towns));
        } catch (Throwable ignored) {
            // our extension failing must never take down their /mcsm command
        }
    }

    private static int ds$list(CommandSourceStack src) {
        List<McsmWorldgen.Site> sites = McsmWorldgen.layout();
        src.sendSuccess(() -> Component.literal("[ds] " + sites.size()
                + " Story Mode sites. They build at these ABSOLUTE coordinates,"
                + " not near you:"), false);
        for (McsmWorldgen.Site s : sites) {
            src.sendSuccess(() -> Component.literal("  - " + s.label() + " ("
                    + s.x() + ", " + s.y() + ", " + s.z() + ")  ->  /ds towns tp "
                    + McsmCommand.key(s)), false);
        }
        return sites.size();
    }

    private static int ds$build(CommandSourceStack src, String name) {
        boolean all = name.equalsIgnoreCase("all");
        int queued = 0;
        int failed = 0;
        for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
            if (!all && !McsmCommand.key(s).equalsIgnoreCase(name)
                    && !s.label().equalsIgnoreCase(name)) {
                continue;
            }
            try {
                McsmSchematic sch = McsmSchematic.load(
                        src.getServer().getResourceManager(), s.path());
                McsmWorldgen.enqueue(sch, new BlockPos(s.x(), s.y(), s.z()), s.label());
                queued++;
                src.sendSuccess(() -> Component.literal("[ds] queued " + s.label()
                        + " at (" + s.x() + ", " + s.y() + ", " + s.z()
                        + ") -- it builds within seconds; get there with /ds towns tp "
                        + McsmCommand.key(s)), false);
            } catch (Throwable t) {
                failed++;
                src.sendFailure(Component.literal("[ds] " + s.label()
                        + ": could not load schematic (" + t + ")"));
            }
        }
        if (queued == 0 && failed == 0) {
            src.sendFailure(Component.literal("[ds] no site called '" + name
                    + "'. /ds towns lists every site."));
        }
        return queued;
    }

    private static int ds$tp(CommandSourceStack src, String name) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.literal("/ds towns tp must be run by a player."));
            return 0;
        }
        for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
            if (!McsmCommand.key(s).equalsIgnoreCase(name)
                    && !s.label().equalsIgnoreCase(name)) {
                continue;
            }
            ServerLevel level = src.getLevel();
            p.teleportTo(level, s.x() + 0.5, s.y() + 2.0, s.z() + 0.5,
                    Collections.emptySet(), p.getYRot(), p.getXRot(), false);
            src.sendSuccess(() -> Component.literal("[ds] teleported to " + s.label()
                    + " (" + s.x() + ", " + s.y() + ", " + s.z()
                    + "). If it is empty ground, run /ds towns build "
                    + McsmCommand.key(s) + " and watch it rise."), false);
        return 1;
        }
        src.sendFailure(Component.literal("[ds] no site called '" + name
                + "'. /ds towns lists every site."));
        return 0;
    }

    /**
     * Devouring Storms MCSM -- the story starts where the story starts:
     * the treehouse in the wilderness outside EnderCon (Episode 1 opening).
     * Builds the three-site opening cluster and stands the player at the
     * treehouse. Site labels come from the recovered MCSM source
     * (src-recon/.../McsmWorldgen.java layout()).
     */
    private static final String[] DS$EPISODE_ONE = {
            "Wilderness Treehouse", "The Wilderness", "EnderCon Town Fair",
    };

    private static int ds$start(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        McsmWorldgen.Site treehouse = null;
        int queued = 0;
        for (String want : DS$EPISODE_ONE) {
            for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
                if (!s.label().equalsIgnoreCase(want)) {
                    continue;
                }
                try {
                    McsmSchematic sch = McsmSchematic.load(
                            src.getServer().getResourceManager(), s.path());
                    McsmWorldgen.enqueue(sch, new BlockPos(s.x(), s.y(), s.z()), s.label());
                    queued++;
                    src.sendSuccess(() -> Component.literal("[ds] building " + s.label()
                            + " at (" + s.x() + ", " + s.y() + ", " + s.z() + ")"), false);
                } catch (Throwable t) {
                    src.sendFailure(Component.literal("[ds] " + s.label()
                            + ": could not load schematic (" + t + ")"));
                }
                if (s.label().equalsIgnoreCase("Wilderness Treehouse")) {
                    treehouse = s;
                }
                break;
            }
        }
        if (treehouse == null) {
            src.sendFailure(Component.literal("[ds] the Wilderness Treehouse site is missing"
                    + " from this jar's layout -- /ds towns still lists what exists."));
            return 0;
        }
        final int n = queued;   // loop counter is not effectively final
        if (p != null) {
            McsmWorldgen.Site t = treehouse;
            p.teleportTo(src.getLevel(), t.x() + 0.5, t.y() + 2.0, t.z() + 0.5,
                    Collections.emptySet(), p.getYRot(), p.getXRot(), false);
            src.sendSuccess(() -> Component.literal("[ds] the story starts here: the treehouse"
                    + " in the wilderness outside EnderCon (" + t.x() + ", " + t.y() + ", " + t.z()
                    + "). " + n + " site(s) queued -- they rise within seconds."), false);
        } else {
            McsmWorldgen.Site t2 = treehouse;   // effectively final for the lambda
            src.sendSuccess(() -> Component.literal("[ds] " + n
                    + " Episode 1 site(s) queued around the Wilderness Treehouse ("
                    + t2.x() + ", " + t2.y() + ", " + t2.z() + ")."), false);
        }
        return queued;
    }

    private static int ds$status(CommandSourceStack src) {
        int pending = McsmWorldgen.pending();
        src.sendSuccess(() -> Component.literal("[ds] town queue: " + pending
                + " job(s) pending. The queue drains every server tick at up to"
                + " 24000 blocks/tick -- a town takes seconds, not minutes."), false);
        return pending;
    }
}
