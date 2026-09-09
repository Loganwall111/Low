package net.dabicco.witherstormmod.mixin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dabicco.witherstormmod.structures.McsmSchematic;
import net.dabicco.witherstormmod.structures.McsmWorldgen;
import net.mcsm.extras.McsmTemplateSummoner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Devouring Storms first-spawn Story Mode arrival.
 *
 * Preferred path: use converted vanilla NBT templates through
 * StructureTemplateManager. Fallback path, used until the clean MC105/MC201 NBT
 * source folders are supplied: queue the recovered legacy Story Mode schematics
 * and place the player at the Episode One treehouse cluster.
 */
@Mixin(McsmWorldgen.class)
public abstract class McsmEpisodeSpawnMixin {

    @Unique
    private static boolean dabyws$attemptedSummon = false;
    @Unique
    private static boolean dabyws$worldReady = false;
    @Unique
    private static BlockPos dabyws$storySpawn = null;
    @Unique
    private static final Set<UUID> DABYWS$ARRIVED = new HashSet<>();

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void dabyws$episodeOneSpawn(ServerLevel level,
            CallbackInfoReturnable<Integer> cir) {
        if (level.dimension() != Level.OVERWORLD || level.players().isEmpty()) {
            return;
        }

        if (!dabyws$attemptedSummon) {
            dabyws$attemptedSummon = true;
            ServerPlayer first = level.players().get(0);
            dabyws$storySpawn = first.blockPosition();
            int placed = McsmTemplateSummoner.summon(level, dabyws$storySpawn, "world");
            dabyws$worldReady = placed > 0;
            if (dabyws$worldReady) {
                first.sendSystemMessage(Component.literal(
                        "\u00a75\u00a7lEpisode One \u00a78\u2014 \u00a7d\u00a7lA New Order"));
                first.sendSystemMessage(Component.literal(
                        "\u00a77The converted Story Mode blueprint has been summoned where you spawned."));
            } else {
                dabyws$storySpawn = dabyws$queueEpisodeOne(level);
                dabyws$worldReady = dabyws$storySpawn != null;
                if (dabyws$worldReady) {
                    first.sendSystemMessage(Component.literal(
                            "\u00a75\u00a7lEpisode One \u00a78\u2014 \u00a7d\u00a7lA New Order"));
                    first.sendSystemMessage(Component.literal(
                            "\u00a77The Story Mode opening area is being built; you start at the treehouse."));
                }
            }
        }

        if (!dabyws$worldReady || dabyws$storySpawn == null) {
            return;
        }

        for (ServerPlayer p : level.players()) {
            if (DABYWS$ARRIVED.add(p.getUUID())) {
                p.teleportTo(level,
                        dabyws$storySpawn.getX() + 0.5D,
                        dabyws$storySpawn.getY() + 2.0D,
                        dabyws$storySpawn.getZ() + 0.5D,
                        Set.of(), p.getYRot(), p.getXRot(), false);
                p.sendSystemMessage(Component.literal(
                        "\u00a77You arrive inside the Story Mode opening area."));
            }
        }
    }

    @Unique
    private static BlockPos dabyws$queueEpisodeOne(ServerLevel level) {
        BlockPos start = null;
        // 1.9.194 native-memory fix: first login used to queue four large
        // schematic areas at once. Even with lower render/simulation distance,
        // the sudden block flood could make Sodium/Iris allocate too many
        // chunk-render buffers and crash outside Java heap. Start with the
        // treehouse only; players can still use /ds towns build/summon for the
        // larger towns after the world settles.
        String[] wants = { "Wilderness Treehouse" };
        for (String want : wants) {
            for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
                if (!s.label().equalsIgnoreCase(want)) {
                    continue;
                }
                try {
                    McsmSchematic sch = McsmSchematic.load(level.getServer().getResourceManager(), s.path());
                    McsmWorldgen.enqueue(sch, new BlockPos(s.x(), s.y(), s.z()), s.label());
                    if (start == null || s.label().equalsIgnoreCase("Wilderness Treehouse")) {
                        start = new BlockPos(s.x(), s.y(), s.z());
                    }
                } catch (Throwable ignored) {
                    // Missing one legacy schematic should not block the others.
                }
                break;
            }
        }
        return start;
    }
}
