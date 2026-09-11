package net.dabicco.witherstormmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.dabicco.witherstormmod.block.WitheredDustBlock;
import net.dabicco.witherstormmod.bowels.ModBowelsEntities;
import net.dabicco.witherstormmod.bowels.client.BowelsCrackModel;
import net.dabicco.witherstormmod.bowels.client.BowelsHeartRenderer;
import net.dabicco.witherstormmod.bowels.client.BowelsMawRenderer;
import net.dabicco.witherstormmod.bowels.client.BowelsPedestalRenderer;
import net.dabicco.witherstormmod.bowels.client.BowelsTentacleRenderer;
import net.dabicco.witherstormmod.bowels.client.SeveredTentacleRenderer;
import net.dabicco.witherstormmod.client.ActionButtons;
import net.dabicco.witherstormmod.client.BeamHumSound;
import net.dabicco.witherstormmod.client.BeamMoteSpawner;
import net.dabicco.witherstormmod.client.BowelsDebug;
import net.dabicco.witherstormmod.client.BowelsHud;
import net.dabicco.witherstormmod.client.BowelsMusic;
import net.dabicco.witherstormmod.client.CaveRumbleClient;
import net.dabicco.witherstormmod.client.ClientConfigCache;
import net.dabicco.witherstormmod.client.ClientConfigCommandHandler;
import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.ClientSicknessManager;
import net.dabicco.witherstormmod.client.ClientWitheredManager;
import net.dabicco.witherstormmod.client.CommandBlockPowerSound;
import net.dabicco.witherstormmod.client.ControlPanelLightTint;
import net.dabicco.witherstormmod.client.DistantStormRenderer;
import net.dabicco.witherstormmod.client.StormCloudDeck;
import net.dabicco.witherstormmod.client.StormHaloRings;
import net.dabicco.witherstormmod.client.StormPresenceFX;
import net.dabicco.witherstormmod.client.StormVortexFX;
import net.dabicco.witherstormmod.client.StormStarfield;
import net.dabicco.witherstormmod.client.FormidibombBlast;
import net.dabicco.witherstormmod.client.FormidibombEmissiveTint;
import net.dabicco.witherstormmod.client.FormidibombFlash;
import net.dabicco.witherstormmod.client.FormidibombProperty;
import net.dabicco.witherstormmod.client.RetrieverClientTooltip;
// import net.dabicco.witherstormmod.client.RetrieverCountProperties; // unused after RangeSelect accessor removal
import net.dabicco.witherstormmod.client.SpawnTowerGloom;
import net.dabicco.witherstormmod.client.StormAmbienceSound;
import net.dabicco.witherstormmod.client.StormDistantVocals;
import net.dabicco.witherstormmod.client.StormLoopSounds;
import net.dabicco.witherstormmod.client.StormMusic;
import net.dabicco.witherstormmod.client.StormSkyDarken;
import net.dabicco.witherstormmod.client.WitherVeinLayer;
import net.dabicco.witherstormmod.client.WitheredRenderer;
import net.dabicco.witherstormmod.client.WithergloopSound;
import net.dabicco.witherstormmod.client.gui.FurnaceFilterScreen;
import net.dabicco.witherstormmod.client.gui.WitherStormConfigScreen;
import net.dabicco.witherstormmod.client.particle.BeamMoteParticle;
import net.dabicco.witherstormmod.client.renderer.BlackHoleRenderer;
import net.dabicco.witherstormmod.client.renderer.WitherStormClusterRenderer;
import net.dabicco.witherstormmod.config.ClientConfigCommandPayload;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.config.SyncWitherStormConfigPayload;
import net.dabicco.witherstormmod.entity.ModEntityTypes;
import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.dabicco.witherstormmod.entity.WitherStormHeadEntity;
import net.dabicco.witherstormmod.entity.cluster.WitherStormClusterEntity;
import net.dabicco.witherstormmod.entity.model.ModEntityModelLayers;
import net.dabicco.witherstormmod.entity.renderer.CrossDimensionalRenderer;
import net.dabicco.witherstormmod.entity.renderer.FormidibombRenderer;
import net.dabicco.witherstormmod.entity.renderer.GrabTentacleRenderer;
import net.dabicco.witherstormmod.entity.renderer.GrappledTntRenderer;
import net.dabicco.witherstormmod.entity.renderer.NetherScaleRenderer;
import net.dabicco.witherstormmod.entity.renderer.SeveredWitherStormRenderer;
import net.dabicco.witherstormmod.entity.renderer.SuperSkullRenderer;
import net.dabicco.witherstormmod.entity.renderer.SuperTntRenderer;
import net.dabicco.witherstormmod.entity.renderer.WitherStormHeadRenderer;
import net.dabicco.witherstormmod.entity.renderer.WitherStormRenderer;
import net.dabicco.witherstormmod.entity.renderer.WitheredBlockRenderer;
import net.dabicco.witherstormmod.item.RetrieverTooltip;
import net.dabicco.witherstormmod.menu.ModMenus;
import net.dabicco.witherstormmod.mixin.ItemTintSourcesAccessor;
import net.dabicco.witherstormmod.mixin.LivingEntitySwimAccessor;
import net.dabicco.witherstormmod.mixin.SelectItemModelPropertiesAccessor;
import net.dabicco.witherstormmod.network.CaveRumblePayload;
import net.dabicco.witherstormmod.network.CommandBlockPowerPayload;
import net.dabicco.witherstormmod.network.FormidibombFlashPayload;
import net.dabicco.witherstormmod.network.SpawnStructurePayload;
import net.dabicco.witherstormmod.network.StormRemovedPacket;
import net.dabicco.witherstormmod.network.WitherSicknessPayload;
import net.dabicco.witherstormmod.network.WitherStormPositionPacket;
import net.dabicco.witherstormmod.network.WitheredCastPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public class DabyWitherStormModClient implements ClientModInitializer {
   private static final int SWIM_DELAY_TICKS = 60;

   private static boolean inBeamColumnClient(WitherStormHeadEntity head, Player player) {
      if (head.isSonicDisabled()) {
         return false;
      } else if (!head.isBeamActive()) {
         return false;
      } else {
         Vec3 ground = head.clientBeamEnd != null ? head.clientBeamEnd : head.getBeamEndExact();
         Vec3 headPos = head.position();
         Vec3 axis = ground.subtract(headPos);
         double len = axis.length();
         if (len < 0.001) {
            return false;
         } else {
            Vec3 n = axis.scale((double)1.0F / len);
            Vec3 rel = player.position().add((double)0.0F, (double)player.getBbHeight() * (double)0.5F, (double)0.0F).subtract(headPos);
            double along = rel.dot(n);
            if (!(along < (double)0.0F) && !(along > len + (double)3.0F)) {
               double radius = (double)ClientConfigCache.cfg.beamGroundRadius + (double)2.5F;
               return rel.subtract(n.scale(along)).length() <= radius;
            } else {
               return false;
            }
         }
      }
   }

   public void onInitializeClient() {
      DabyWSClientConfig.load();
      MenuScreens.register(ModMenus.FURNACE_FILTER, FurnaceFilterScreen::new);
      ItemTooltipCallback.EVENT.register((ItemTooltipCallback)(stack, context, flag, lines) -> {
         PotionContents contents = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
         if (contents != null) {
            Holder<Potion> potion = contents.potion().orElse(null);
            if (ModPotions.isHyperInvisibility(potion)) {
               lines.add(Component.literal("Shields you from the eyes of dastardly beasts...").withStyle(ChatFormatting.DARK_PURPLE).withStyle(ChatFormatting.ITALIC));
            }
         }
      });
      ClientTickEvents.START_CLIENT_TICK.register(ActionButtons::tick);
      BlockColorRegistry.register(List.of(new BlockTintSource() {
         public int color(BlockState state) {
            return WitheredDustBlock.tint((Integer)state.getValue(RedStoneWireBlock.POWER));
         }

         public Set<Property<?>> relevantProperties() {
            return Set.of(RedStoneWireBlock.POWER);
         }
      }), new Block[]{ModBlocks.WITHERED_DUST});
      ItemTintSourcesAccessor.dabyws$idMapper().put(Identifier.fromNamespaceAndPath("dabywitherstormmod", "panel_light"), ControlPanelLightTint.MAP_CODEC);
      ItemTintSourcesAccessor.dabyws$idMapper().put(Identifier.fromNamespaceAndPath("dabywitherstormmod", "formidibomb_light"), FormidibombEmissiveTint.MAP_CODEC);
      SelectItemModelPropertiesAccessor.dabyws$idMapper().put(Identifier.fromNamespaceAndPath("dabywitherstormmod", "formidibomb"), FormidibombProperty.TYPE);
      // RangeSelectItemModelPropertiesAccessor hook REMOVED: the accessor mixin
      // was dropped from dabywitherstormmod.mixins.json (manual repair), so any
      // reference to that class here throws NoClassDefFoundError and crashes
      // client initialization. The retriever_tnt / retriever_rockets item
      // properties are deliberately unregistered to keep the boot clean.
      ParticleProviderRegistry.getInstance().register(ModParticles.BEAM_MOTE, BeamMoteParticle.Provider::new);
      ModEntityModelLayers.registerModelLayers();
      ModelLayerRegistry.registerModelLayer(BowelsCrackModel.LAYER, BowelsCrackModel::createBodyLayer);
      EntityRenderers.register(ModBowelsEntities.HEART, BowelsHeartRenderer::new);
      EntityRenderers.register(ModBowelsEntities.TENTACLE, BowelsTentacleRenderer::new);
      EntityRenderers.register(ModBowelsEntities.SEVERED_TENTACLE, SeveredTentacleRenderer::new);
      EntityRenderers.register(ModBowelsEntities.PEDESTAL, BowelsPedestalRenderer::new);
      EntityRenderers.register(ModBowelsEntities.MAW, BowelsMawRenderer::new);
      EntityRenderers.register(ModEntityTypes.WITHER_STORM, WitherStormRenderer::new);
      EntityRenderers.register(ModEntityTypes.SEVERED_WITHER_STORM, SeveredWitherStormRenderer::new);
      EntityRenderers.register(ModEntityTypes.WITHER_STORM_CLUSTER, WitherStormClusterRenderer::new);
      EntityRenderers.register(ModEntityTypes.WITHER_STORM_HEAD, WitherStormHeadRenderer::new);
      EntityRenderers.register(ModEntityTypes.BLACK_HOLE, BlackHoleRenderer::new);
      EntityRenderers.register(ModEntityTypes.SUPER_SKULL, SuperSkullRenderer::new);
      EntityRenderers.register(ModEntityTypes.SUPER_TNT, SuperTntRenderer::new);
      EntityRenderers.register(ModEntityTypes.GRAPPLED_TNT, GrappledTntRenderer::new);
      EntityRenderers.register(ModEntityTypes.FORMIDIBOMB, FormidibombRenderer::new);
      ClientTooltipComponentCallback.EVENT.register((ClientTooltipComponentCallback)(component) -> {
         RetrieverClientTooltip var10000;
         if (component instanceof RetrieverTooltip rt) {
            var10000 = new RetrieverClientTooltip(rt);
         } else {
            var10000 = null;
         }

         return var10000;
      });
      EntityRenderers.register(ModEntityTypes.GRAB_TENTACLE, GrabTentacleRenderer::new);
      EntityRenderers.register(ModEntityTypes.CROSS_DIMENSIONAL, CrossDimensionalRenderer::new);
      EntityRenderers.register(ModEntityTypes.NETHER_SCALE, NetherScaleRenderer::new);
      EntityRenderers.register(ModEntityTypes.WITHERED_BLOCK, WitheredBlockRenderer::new);
      LevelRenderEvents.COLLECT_SUBMITS.register(DistantStormRenderer::render);
      LevelRenderEvents.COLLECT_SUBMITS.register(WitheredRenderer::render);
      LevelRenderEvents.COLLECT_SUBMITS.register(FormidibombBlast::render);
      LevelRenderEvents.COLLECT_SUBMITS.register(StormStarfield::submit);
      LevelRenderEvents.COLLECT_SUBMITS.register(StormCloudDeck::submit);
      LevelRenderEvents.COLLECT_SUBMITS.register(StormPresenceFX::submit);
      LevelRenderEvents.COLLECT_SUBMITS.register(StormHaloRings::submit);
      LevelRenderEvents.COLLECT_SUBMITS.register(StormVortexFX::submit);
      ClientTickEvents.START_CLIENT_TICK.register(StormPresenceFX::tick);
      ClientTickEvents.START_CLIENT_TICK.register(StormVortexFX::tick);
      ClientPlayNetworking.registerGlobalReceiver(SpawnStructurePayload.TYPE, (payload, context) -> context.client().execute(() -> {
            StormMusic.setInsideSpawnTower(payload.inside());
            SpawnTowerGloom.set(payload.inside(), payload.x(), payload.floorY(), payload.z());
         }));
      ClientPlayNetworking.registerGlobalReceiver(CaveRumblePayload.TYPE, (payload, context) -> context.client().execute(() -> CaveRumbleClient.begin(payload.durationTicks(), payload.intensity())));
      ClientPlayNetworking.registerGlobalReceiver(CommandBlockPowerPayload.TYPE, (payload, context) -> context.client().execute(() -> context.client().getSoundManager().play(new CommandBlockPowerSound(payload.x(), payload.y(), payload.z()))));
      ClientPlayNetworking.registerGlobalReceiver(ClusterBlocksPayload.TYPE, (payload, context) -> {
         ClientLevel level = context.client().level;
         if (level != null) {
            Entity entity = level.getEntity(payload.clusterEntityId());
            if (entity instanceof WitherStormClusterEntity) {
               WitherStormClusterEntity cluster = (WitherStormClusterEntity)entity;
               List<BlockState> states = new ArrayList();

               for(int id : payload.blockStateIds()) {
                  states.add(Block.stateById(id));
               }

               cluster.setClientBlockData(states, payload.offsets());
            }

         }
      });
      ClientPlayNetworking.registerGlobalReceiver(SyncWitherStormConfigPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            ClientConfigCache.cfg.applyArray(payload.values());
            ClientConfigCache.canEditServer = payload.canEdit();
            Screen patt0$temp = context.client().gui.screen();
            if (patt0$temp instanceof WitherStormConfigScreen screen) {
               screen.onServerConfigSynced();
            }

         }));
      ClientPlayNetworking.registerGlobalReceiver(ClientConfigCommandPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientConfigCommandHandler.handle(context.client(), payload)));
      ClientPlayNetworking.registerGlobalReceiver(WitherSicknessPayload.TYPE, WitherSicknessPayload::handleClient);
      ClientPlayNetworking.registerGlobalReceiver(WitheredCastPayload.TYPE, WitheredCastPayload::handleClient);
      LivingEntityRenderLayerRegistrationCallback.EVENT.register((LivingEntityRenderLayerRegistrationCallback)(type, renderer, helper, ctx) -> {
         if (type != ModEntityTypes.WITHER_STORM) {
            if (type != EntityTypes.WARDEN && type != EntityTypes.CREAKING) {
               WitherVeinLayer layer = new WitherVeinLayer(renderer);
               helper.register(layer);
            }
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(WitherStormPositionPacket.TYPE, WitherStormPositionPacket::handleClient);
      ClientPlayNetworking.registerGlobalReceiver(StormRemovedPacket.TYPE, StormRemovedPacket::handleClient);
      ClientPlayNetworking.registerGlobalReceiver(FormidibombFlashPayload.TYPE, FormidibombFlashPayload::handleClient);
      HudElementRegistry.addLast(DabyWitherStormMod.id("formidibomb_flash"), FormidibombFlash::render);
      HudElementRegistry.addLast(DabyWitherStormMod.id("bowels_frame"), BowelsHud::render);
      ClientPlayConnectionEvents.DISCONNECT.register((ClientPlayConnectionEvents.Disconnect)(handler, client) -> {
         ClientDistantStormManager.clear();
         ClientSicknessManager.clear();
         ClientWitheredManager.clear();
         StormSkyDarken.clear();
      });
      StormAmbienceSound[] ambience = new StormAmbienceSound[]{null};
      BeamHumSound[] beamHum = new BeamHumSound[]{null};
      int[] heldTicks = new int[]{0};
      ClientTickEvents.END_CLIENT_TICK.register(BowelsDebug::tick);
      ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(client) -> {
         if (client.level == null) {
            ambience[0] = null;
            beamHum[0] = null;
         } else {
            BeamMoteSpawner.tick(client);
            StormLoopSounds.tick(client);
            StormMusic.tick(client);
            BowelsMusic.tick(client);
            WithergloopSound.tick(client);
            StormDistantVocals.tick(client);
            if (client.level.getGameTime() % 100L == 0L) {
               ClientWitheredManager.prune();
            }

            if (client.level.getGameTime() % 10L == 0L) {
               RandomSource rng = client.level.getRandom();

               for(Entity e : client.level.entitiesForRendering()) {
                  if (e instanceof ItemEntity) {
                     ItemEntity item = (ItemEntity)e;
                     if (item.getItem().is(ModItems.WITHER_FRAGMENT) && !(item.distanceToSqr(client.player) > (double)576.0F) && rng.nextInt(2) == 0) {
                        client.level.addParticle(ParticleTypes.SCULK_SOUL, item.getX() + (rng.nextDouble() - (double)0.5F) * 0.4, item.getY() + (double)0.25F + rng.nextDouble() * 0.2, item.getZ() + (rng.nextDouble() - (double)0.5F) * 0.4, (double)0.0F, 0.015, (double)0.0F);
                        if (rng.nextInt(6) == 0) {
                           client.level.addParticle(ParticleTypes.ASH, item.getX() + (rng.nextDouble() - (double)0.5F) * (double)0.5F, item.getY() + 0.2, item.getZ() + (rng.nextDouble() - (double)0.5F) * (double)0.5F, (double)0.0F, 0.01, (double)0.0F);
                        }
                     }
                  }
               }
            }

            if (client.player != null) {
               int myId = client.player.getId();
               boolean held = false;

               for(Entity e : client.level.entitiesForRendering()) {
                  if (e instanceof WitherStormHeadEntity) {
                     WitherStormHeadEntity h = (WitherStormHeadEntity)e;
                     if (h.getSuckedId() == myId && h.isBeamActive()) {
                        held = true;
                        break;
                     }
                  }

                  if (e instanceof WitherStormEntity) {
                     WitherStormEntity ws = (WitherStormEntity)e;
                     if (ws.getSnatchId() == myId) {
                        held = true;
                        break;
                     }
                  }
               }

               heldTicks[0] = held ? heldTicks[0] + 1 : 0;
               boolean tryingToMove = client.player.input != null && (client.player.input.getMoveVector().lengthSquared() > 1.0E-5F || client.player.input.keyPresses.jump());
               if (held && !client.player.onGround() && heldTicks[0] >= 60 && tryingToMove) {
                  client.player.setSwimming(true);
                  client.player.setPose(Pose.SWIMMING);
                  LivingEntitySwimAccessor swim = (LivingEntitySwimAccessor)client.player;
                  swim.dabyws$setSwimAmount(Math.min(1.0F, swim.dabyws$getSwimAmount() + 0.15F));
               }
            }

            boolean stormExists = !ClientDistantStormManager.all().isEmpty();
            if (stormExists && DabyWSClientConfig.stormAmbience && StormAmbienceSound.anyStormInHearRange(client) && needsRestart(client, ambience[0])) {
               ambience[0] = new StormAmbienceSound();
               client.getSoundManager().play(ambience[0]);
            }

            if (DabyWSClientConfig.beamHum && BeamHumSound.isPlayerInAnyBeam(client) && needsRestart(client, beamHum[0])) {
               beamHum[0] = new BeamHumSound();
               client.getSoundManager().play(beamHum[0]);
            }

         }
      });
   }

   private static boolean needsRestart(Minecraft client, AbstractTickableSoundInstance sound) {
      return sound == null || sound.isStopped() || !client.getSoundManager().isActive(sound);
   }
}
