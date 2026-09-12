package net.mcsm.extras.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * 1.9.205 -- registry for the Devouring Storms extra entities.
 * Registered from the base mod's onInitialize (McsmBuiltinPackMixin), i.e.
 * at the same point the base mod registers its own entity types.
 */
public final class McsmEntities {

    public static final Identifier STORY_CHARACTER_ID = Identifier.fromNamespaceAndPath("dabywitherstormmod", "story_character");

    public static EntityType<StoryCharacterEntity> STORY_CHARACTER;

    private static boolean done = false;

    private McsmEntities() {}

    public static void register() {
        if (done) {
            return;
        }
        done = true;
        try {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, STORY_CHARACTER_ID);
            EntityType<StoryCharacterEntity> type = EntityType.Builder
                    .of(StoryCharacterEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(key);
            STORY_CHARACTER = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
            FabricDefaultAttributeRegistry.register(STORY_CHARACTER, StoryCharacterEntity.createAttributes());
        } catch (Throwable t) {
            // never take the mod down over the cast; McsmNpcs falls back to villagers
            STORY_CHARACTER = null;
        }
    }
}
