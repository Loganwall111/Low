package net.dabicco.witherstormmod.mixin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dabicco.witherstormmod.structures.McsmWorldgen;
import net.mcsm.extras.McsmTemplateSummoner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Devouring Storms 1.9.178 -- first-spawn Story Mode world summon.
 *
 * The old version of this mixin queued the broken EnderCon .schematic and then
 * teleported players to its absolute coordinates. The schematic assets are now
 * intentionally purged, so first arrival must use the new vanilla NBT template
 * path instead:
 *
 *  - On the first overworld tick with players present, summon the converted
 *    Story Mode blueprint world at the player's actual spawn/current position.
 *  - Structure placement goes through StructureTemplateManager via
 *    McsmTemplateSummoner, never through McsmSchematic.
 *  - Every freshly joined player is delivered once into that summoned spawn
 *    world and gets the Episode One arrival message.
 *  - If the uploaded/converted NBT blueprints are not present yet, the mixin
 *    fails open and leaves normal gameplay alone; /ds towns summon will report
 *    the missing blueprint clearly when used manually.
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
                        "\u00a77The Story Mode world has been summoned where you spawned."));
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
                        "\u00a77You arrive inside the summoned Story Mode world at spawn."));
            }
        }
    }
}
