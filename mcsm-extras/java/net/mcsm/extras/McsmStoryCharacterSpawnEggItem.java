package net.mcsm.extras;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A real Devouring Storms item for manually placing Story Mode characters.
 * The current renderer still uses the safe vanilla mob body under the hood, but
 * this gives the cast their own spawn-egg path instead of relying only on town
 * villager population.
 */
public class McsmStoryCharacterSpawnEggItem extends Item {
    public McsmStoryCharacterSpawnEggItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server) || player == null) {
            return InteractionResult.PASS;
        }
        BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
        spawn(server, player, at);
        ItemStack stack = ctx.getItemInHand();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && level instanceof ServerLevel server) {
            BlockPos at = player.blockPosition().relative(player.getDirection(), 2);
            spawn(server, player, at);
            ItemStack stack = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Spawns a named Minecraft: Story Mode cast NPC.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak-use cycles through Jesse, Petra, Axel, Radar, Ivor and more.").withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static void spawn(ServerLevel server, Player player, BlockPos at) {
        String name = nextName(server, player);
        Mob mob = McsmNpcs.spawnStoryCharacter(server, at, name, true);
        if (mob != null) {
            server.sendParticles(new DustParticleOptions(0xD9F8FF, 1.2F),
                    mob.getX(), mob.getY() + 1.15, mob.getZ(), 16, 0.35, 0.55, 0.35, 0.04);
            server.sendParticles(new DustParticleOptions(0xB66BFF, 0.9F),
                    mob.getX(), mob.getY() + 1.75, mob.getZ(), 8, 0.25, 0.3, 0.25, 0.02);
            server.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.VILLAGER_YES,
                    SoundSource.NEUTRAL, 0.85F, 1.15F);
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("[ds] spawned Story Mode character: " + name));
            }
        }
    }

    private static String nextName(ServerLevel server, Player player) {
        String[] names = McsmNpcs.storyCharacterNames();
        if (names.length == 0) {
            return "Jesse";
        }
        if (player.isShiftKeyDown()) {
            long idx = Math.floorMod(server.getGameTime() / 10L + player.getUUID().getLeastSignificantBits(), names.length);
            return names[(int) idx];
        }
        int idx = Math.floorMod(player.getRandom().nextInt(), names.length);
        return names[idx];
    }
}
