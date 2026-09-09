package net.mcsm.extras;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Template item for a future registered "Story World Summoner" item.
 *
 * Registration is intentionally separate because the base jar already owns the
 * item registry; this class is the clean custom Item implementation requested
 * by the user. It places the converted vanilla NBT templates through
 * StructureTemplateManager via McsmTemplateSummoner.
 */
public class McsmStructureSummonerItemTemplate extends Item {
    public McsmStructureSummonerItemTemplate(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return InteractionResult.SUCCESS;
        }
        int placed = McsmTemplateSummoner.summon((ServerPlayer) player, "world");
        player.sendSystemMessage(Component.literal(placed > 0
                ? "[ds] summoned Story Mode world blueprints at spawn."
                : "[ds] Story Mode NBT blueprints are missing; run ci/convert_story_worlds.py first."));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer) {
            int placed = McsmTemplateSummoner.summon((ServerPlayer) player, "world");
            player.sendSystemMessage(Component.literal(placed > 0
                    ? "[ds] summoned Story Mode world blueprints at spawn."
                    : "[ds] Story Mode NBT blueprints are missing; run ci/convert_story_worlds.py first."));
        }
        return InteractionResult.SUCCESS;
    }
}
