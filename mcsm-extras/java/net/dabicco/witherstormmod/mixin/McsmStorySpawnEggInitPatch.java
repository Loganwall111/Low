package net.dabicco.witherstormmod.mixin;

import net.minecraft.world.item.Item;
import net.mcsm.extras.McsmStoryCharacterSpawnEggItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers the standalone Story Mode character spawn egg item. */
@Mixin(net.dabicco.witherstormmod.ModItems.class)
public class McsmStorySpawnEggInitPatch {
    private static Item STORY_CHARACTER_SPAWN_EGG;

    @Inject(method = {"initialize"}, at = @At("TAIL"), remap = false, require = 0)
    private static void mcsm$registerStorySpawnEgg(CallbackInfo ci) {
        try {
            if (STORY_CHARACTER_SPAWN_EGG == null) {
                STORY_CHARACTER_SPAWN_EGG = net.dabicco.witherstormmod.ModItems.register(
                        "story_character_spawn_egg",
                        McsmStoryCharacterSpawnEggItem::new,
                        new Item.Properties());
            }
        } catch (Throwable t) {
            System.err.println("[MCSM] story character spawn egg registration skipped: " + t);
        }
    }
}
