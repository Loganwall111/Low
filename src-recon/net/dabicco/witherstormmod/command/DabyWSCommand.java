package net.dabicco.witherstormmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.dabicco.witherstormmod.BowelsEndRoom;
import net.dabicco.witherstormmod.BowelsEntry;
import net.dabicco.witherstormmod.BowelsGravity;
import net.dabicco.witherstormmod.BowelsHallway;
import net.dabicco.witherstormmod.ModAdvancements;
import net.dabicco.witherstormmod.ModSounds;
import net.dabicco.witherstormmod.SigeonNetwork;
import net.dabicco.witherstormmod.StormSpawnPlatform;
import net.dabicco.witherstormmod.bowels.BowelsHeartEntity;
import net.dabicco.witherstormmod.bowels.BowelsMawEntity;
import net.dabicco.witherstormmod.config.ClientConfigCommandPayload;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.config.WitherStormConfigs;
import net.dabicco.witherstormmod.config.WitherStormWorldConfig;
import net.dabicco.witherstormmod.config.WitherStormWorldConfig.Key;
import net.dabicco.witherstormmod.entity.BlackHoleEntity;
import net.dabicco.witherstormmod.entity.ModEntityTypes;
import net.dabicco.witherstormmod.entity.SeveredWitherStormEntity;
import net.dabicco.witherstormmod.entity.StoryNpcSpawner;
import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.dabicco.witherstormmod.entity.WitherStormHeadEntity;
import net.dabicco.witherstormmod.entity.withered.WitheredMobs;
import net.dabicco.witherstormmod.nether.NetherScaleManager;
import net.dabicco.witherstormmod.structures.StructureBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent.SuggestCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DabyWSCommand {
   private static final SuggestionProvider<CommandSourceStack> SERVER_KEYS = (ctx, builder) -> SharedSuggestionProvider.suggest(
      WitherStormWorldConfig.KEYS.keySet(), builder
   );
   private static final SuggestionProvider<CommandSourceStack> CLIENT_KEYS = (ctx, builder) -> SharedSuggestionProvider.suggest(
      DabyWSClientConfig.KEYS.keySet(), builder
   );
   private static final List<net.dabicco.witherstormmod.command.DabyWSCommand.PendingSpawn> PENDING = new ArrayList<>();
   private static final SuggestionProvider<CommandSourceStack> TARGETING_MODES = (ctx, builder) -> SharedSuggestionProvider.suggest(
      Arrays.stream(WitherStormWorldConfig.TARGETING_LABELS).map(s -> s.toLowerCase(Locale.ROOT)), builder
   );

   private static int mawReadout(CommandSourceStack source) {
      ServerLevel level = source.getLevel();
      List<BowelsMawEntity> maws = level.getEntitiesOfClass(
         BowelsMawEntity.class, new AABB(source.getPosition().subtract(200.0, 200.0, 200.0), source.getPosition().add(200.0, 200.0, 200.0))
      );
      if (maws.isEmpty()) {
         source.sendSuccess(() -> Component.literal("No maws within 200 blocks."), false);
         return 0;
      } else {
         Vec3 me = source.getPosition();
         source.sendSuccess(
            () -> Component.literal(String.format("you y=%.1f | room FLOOR_Y=%d CEIL_Y=%d | pull=%s", me.y, 60, 96, BowelsEndRoom.pull(me.y))), false
         );

         for (BowelsMawEntity maw : maws) {
            Vec3 at = maw.position();
            Vec3 end = maw.getBeamEndExact();
            double yawRad = Math.toRadians(maw.getYRot());
            double pitchRad = Math.toRadians(maw.getXRot());
            double cosP = Math.cos(pitchRad);
            Vec3 gaze = new Vec3(-Math.sin(yawRad) * cosP, -Math.sin(pitchRad), Math.cos(yawRad) * cosP);
            String line = String.format(
               "maw side=%d y=%.1f yaw=%.1f pitch=%.1f | gaze dy=%+.2f | beam=%s end y=%.1f dy=%+.1f",
               maw.getSide(),
               at.y,
               maw.getYRot(),
               maw.getXRot(),
               gaze.y,
               maw.isBeamActive() ? "on" : "OFF",
               end.y,
               end.y - at.y
            );
            source.sendSuccess(() -> Component.literal(line), false);
         }

         return maws.size();
      }
   }

   private static int locateTower(CommandSourceStack source) {
      BlockPos at = StormSpawnPlatform.towerPos();
      if (at == null) {
         source.sendFailure(
            Component.literal("No spawn tower is recorded for this world. If one is standing out there, walk into it and run /dabyws tower mark.")
         );
         return 0;
      } else {
         BlockPos from = BlockPos.containing(source.getPosition());
         int distance = (int)Math.sqrt(from.distSqr(at));
         int var10000 = at.getX();
         String coords = var10000 + " " + at.getY() + " " + at.getZ();
         source.sendSuccess(
            () -> Component.literal("Wither Storm spawn tower is at ")
               .append(
                  Component.literal("[" + coords + "]")
                     .withStyle(
                        style -> style.withColor(ChatFormatting.GREEN)
                           .withUnderlined(true)
                           .withClickEvent(new SuggestCommand("/tp @s " + coords))
                           .withHoverEvent(new ShowText(Component.literal("Click to put the teleport in your chat")))
                     )
               )
               .append(Component.literal(" (" + distance + " blocks away)")),
            false
         );
         return 1;
      }
   }

   private static int towerStatus(CommandSourceStack source) {
      AABB box = StormSpawnPlatform.towerBox();
      if (box == null) {
         source.sendSuccess(
            () -> Component.literal(
               "No tower recorded, so its dust, its gloom and Ivor's theme are all off. Stand in the tower and run /dabyws tower mark, or /dabyws tower place to build a fresh one where you are."
            ),
            false
         );
         return 0;
      } else {
         source.sendSuccess(
            () -> {
               int var10000 = (int)box.minX;
               return Component.literal(
                  "Tower recorded: ["
                     + var10000
                     + " "
                     + (int)box.minY
                     + " "
                     + (int)box.minZ
                     + "] to ["
                     + (int)box.maxX
                     + " "
                     + (int)box.maxY
                     + " "
                     + (int)box.maxZ
                     + "]"
               );
            },
            false
         );
         return 1;
      }
   }

   private static int towerMark(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      Vec3i size = StormSpawnPlatform.templateSize(source.getServer());
      if (size.getX() == 0) {
         source.sendFailure(Component.literal("The spawn_tower structure isn't loaded, so its size is unknown."));
         return 0;
      } else {
         StormSpawnPlatform.markTowerAt(source.getServer(), player.blockPosition(), size);
         StormSpawnPlatform.resurfaceTower(source.getServer());
         source.sendSuccess(() -> Component.literal("Tower recorded around you. Its dust, gloom and music work from now on."), true);
         return 1;
      }
   }

   private static int towerPlace(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      if (!StormSpawnPlatform.placeTowerAt(source.getServer(), player.blockPosition())) {
         source.sendFailure(Component.literal("Couldn't place it: the spawn_tower structure wasn't found."));
         return 0;
      } else {
         source.sendSuccess(() -> Component.literal("Tower built here and recorded."), true);
         return 1;
      }
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                          "dabyws"
                                       )
                                       .then(Commands.literal("locate").executes(ctx -> locateTower((CommandSourceStack)ctx.getSource()))))
                                    .then(Commands.literal("maws").executes(ctx -> mawReadout((CommandSourceStack)ctx.getSource()))))
                                 .then(
                                    ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                      "tower"
                                                   )
                                                   .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                                .executes(ctx -> towerStatus((CommandSourceStack)ctx.getSource())))
                                             .then(Commands.literal("status").executes(ctx -> towerStatus((CommandSourceStack)ctx.getSource()))))
                                          .then(Commands.literal("mark").executes(ctx -> towerMark((CommandSourceStack)ctx.getSource()))))
                                       .then(Commands.literal("place").executes(ctx -> towerPlace((CommandSourceStack)ctx.getSource())))
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("bowels")
                                       .executes(
                                          ctx -> bowelsEnter(
                                             (CommandSourceStack)ctx.getSource(), List.of(((CommandSourceStack)ctx.getSource()).getPlayerOrException())
                                          )
                                       ))
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities())
                                             .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                          .then(
                                             Commands.literal("enter")
                                                .executes(ctx -> bowelsEnter((CommandSourceStack)ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))
                                          )
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("spawn").executes(ctx -> spawnWitherStorm(ctx)))
                                 .then(
                                    ((LiteralArgumentBuilder)Commands.literal("blackhole").executes(ctx -> spawnBlackHoleCountdown(ctx)))
                                       .then(Commands.literal("now").executes(ctx -> spawnBlackHoleNow(ctx)))
                                 )
                           ))
                        .then(Commands.literal("blackhole").then(Commands.literal("getmass").executes(ctx -> getBlackHoleMass(ctx)))))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("setphase")
                              .then(
                                 Commands.argument("phase", FloatArgumentType.floatArg(0.0F, 10.0F))
                                    .executes(ctx -> setPhase(ctx, FloatArgumentType.getFloat(ctx, "phase")))
                              ))
                           .then(
                              Commands.argument("targets", EntityArgument.entities())
                                 .then(
                                    Commands.argument("phase", FloatArgumentType.floatArg(0.0F, 10.0F))
                                       .executes(ctx -> setPhaseSelected(ctx, FloatArgumentType.getFloat(ctx, "phase")))
                                 )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("movement")
                              .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                           .then(
                              ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                             "targets", EntityArgument.entities()
                                          )
                                          .then(
                                             ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ultimateTarget")
                                                         .then(
                                                            Commands.literal("get")
                                                               .executes(net.dabicco.witherstormmod.command.DabyWSCommand::movementTargetGet)
                                                         ))
                                                      .then(
                                                         Commands.literal("set")
                                                            .then(
                                                               Commands.argument("player", EntityArgument.player())
                                                                  .executes(net.dabicco.witherstormmod.command.DabyWSCommand::movementTargetSet)
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.literal("clear").executes(net.dabicco.witherstormmod.command.DabyWSCommand::movementTargetClear)
                                                   ))
                                                .then(
                                                   Commands.literal("reroll").executes(net.dabicco.witherstormmod.command.DabyWSCommand::movementTargetReroll)
                                                )
                                          ))
                                       .then(
                                          Commands.literal("setChasing")
                                             .then(
                                                Commands.argument("chasing", BoolArgumentType.bool())
                                                   .executes(ctx -> movementSetChasing(ctx, BoolArgumentType.getBool(ctx, "chasing")))
                                             )
                                       ))
                                    .then(Commands.literal("distract").executes(net.dabicco.witherstormmod.command.DabyWSCommand::movementDistract)))
                                 .then(Commands.literal("status").executes(net.dabicco.witherstormmod.command.DabyWSCommand::movementStatus))
                           ))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("targeting")
                                 .then(Commands.literal("get").executes(net.dabicco.witherstormmod.command.DabyWSCommand::targetingGet)))
                              .then(
                                 Commands.literal("set")
                                    .then(
                                       Commands.argument("mode", StringArgumentType.word())
                                          .suggests(TARGETING_MODES)
                                          .executes(net.dabicco.witherstormmod.command.DabyWSCommand::targetingSet)
                                    )
                              )
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("test")
                              .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                           .then(Commands.literal("netherscale").executes(net.dabicco.witherstormmod.command.DabyWSCommand::testNetherScale)))
                        .then(Commands.literal("portalprobe").executes(net.dabicco.witherstormmod.command.DabyWSCommand::testPortalProbe)))
                     .then(Commands.literal("bowelsadvance").executes(net.dabicco.witherstormmod.command.DabyWSCommand::testBowelsAdvance))
               ))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("config")
                        .executes(net.dabicco.witherstormmod.command.DabyWSCommand::openConfigGui))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("server")
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("get").executes(net.dabicco.witherstormmod.command.DabyWSCommand::listServerConfig))
                                    .then(
                                       Commands.argument("key", StringArgumentType.word())
                                          .suggests(SERVER_KEYS)
                                          .executes(ctx -> getServerConfig(ctx, StringArgumentType.getString(ctx, "key")))
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("set").requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                 .then(
                                    Commands.argument("key", StringArgumentType.word())
                                       .suggests(SERVER_KEYS)
                                       .then(
                                          Commands.argument("value", DoubleArgumentType.doubleArg())
                                             .executes(
                                                ctx -> setServerConfig(
                                                   ctx, StringArgumentType.getString(ctx, "key"), DoubleArgumentType.getDouble(ctx, "value")
                                                )
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("client")
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("get").executes(ctx -> clientConfigAction(ctx, 2, "", 0.0)))
                                 .then(
                                    Commands.argument("key", StringArgumentType.word())
                                       .suggests(CLIENT_KEYS)
                                       .executes(ctx -> clientConfigAction(ctx, 0, StringArgumentType.getString(ctx, "key"), 0.0))
                                 )
                           ))
                        .then(
                           Commands.literal("set")
                              .then(
                                 Commands.argument("key", StringArgumentType.word())
                                    .suggests(CLIENT_KEYS)
                                    .then(
                                       Commands.argument("value", DoubleArgumentType.doubleArg())
                                          .executes(
                                             ctx -> clientConfigAction(
                                                ctx, 1, StringArgumentType.getString(ctx, "key"), DoubleArgumentType.getDouble(ctx, "value")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
      registerExtra(dispatcher);
   }

   public static void registerTick() {
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         if (!PENDING.isEmpty()) {
            long now = server.getTickCount();
            Iterator<net.dabicco.witherstormmod.command.DabyWSCommand.PendingSpawn> it = PENDING.iterator();

            while (it.hasNext()) {
               net.dabicco.witherstormmod.command.DabyWSCommand.PendingSpawn p = it.next();
               long remaining = p.spawnAtTick() - now;
               if (remaining <= 0L) {
                  doSpawnBlackHole(p.level(), p.pos(), p.source());
                  it.remove();
               } else if (remaining % 20L == 0L) {
                  int secs = (int)(remaining / 20L);
                  p.source().sendSuccess(() -> Component.literal("Black hole in " + secs + "..."), false);
               }
            }
         }
      });
   }

   private static void doSpawnBlackHole(ServerLevel level, Vec3 pos, CommandSourceStack source) {
      BlackHoleEntity entity = (BlackHoleEntity)ModEntityTypes.BLACK_HOLE.create(level, EntitySpawnReason.COMMAND);
      if (entity == null) {
         source.sendFailure(Component.literal("Failed to create black hole"));
      } else {
         entity.setPos(pos);
         level.addFreshEntity(entity);
         source.sendSuccess(() -> Component.literal("Black hole spawned at " + pos), true);
      }
   }

   private static int spawnBlackHoleCountdown(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         source.sendFailure(Component.literal("Must be run by a player"));
         return 0;
      } else {
         Vec3 pos = player.position().add(0.0, 1.0, 0.0);
         long spawnAt = source.getLevel().getServer().getTickCount() + 100;
         PENDING.add(new net.dabicco.witherstormmod.command.DabyWSCommand.PendingSpawn(source.getLevel(), pos, source, spawnAt));
         source.sendSuccess(() -> Component.literal("Black hole in 5..."), false);
         return 1;
      }
   }

   private static int spawnBlackHoleNow(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         source.sendFailure(Component.literal("Must be run by a player"));
         return 0;
      } else {
         doSpawnBlackHole(source.getLevel(), player.position().add(0.0, 1.0, 0.0), source);
         return 1;
      }
   }

   private static int getBlackHoleMass(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         source.sendFailure(Component.literal("Must be run by a player"));
         return 0;
      } else {
         List<BlackHoleEntity> holes = source.getLevel().getEntitiesOfClass(BlackHoleEntity.class, player.getBoundingBox().inflate(100.0));
         if (holes.isEmpty()) {
            source.sendFailure(Component.literal("No black hole found within 100 blocks"));
            return 0;
         } else {
            BlackHoleEntity hole = holes.get(0);
            source.sendSuccess(
               () -> Component.literal(
                  String.format(
                     "Mass: %.2f | Radius: %.2f | Pull: %.1f | Claim: %.1f | Supermassive: %s",
                     hole.getMass(),
                     hole.getRadius(),
                     hole.getPullRadius(),
                     hole.getClusterClaimRadius(),
                     hole.isSupermassive()
                  )
               ),
               false
            );
            return 1;
         }
      }
   }

   private static int spawnWitherStorm(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerLevel level = source.getLevel();
      Vec3 spawnPos = source.getPosition().add(0.0, 2.0, 0.0);
      WitherStormEntity entity = (WitherStormEntity)ModEntityTypes.WITHER_STORM.create(level, EntitySpawnReason.COMMAND);
      if (entity == null) {
         source.sendFailure(Component.literal("Failed to create Wither Storm"));
         return 0;
      }

      // Initialize the phase before tracking the entity. This publishes the
      // phase-4 flag and head-spawn state in the same initial sync instead of
      // briefly rendering a command body with an uninitialized render state.
      entity.setPos(spawnPos);
      entity.setPhaseExact(0.0);
      level.addFreshEntity(entity);
      ServerPlayer player = source.getPlayer();
      if (player != null) {
         ModAdvancements.grant(player, "nothing_built");
      }
      source.sendSuccess(() -> Component.literal("Wither Storm spawned at " + spawnPos), true);
      return 1;
   }

   private static int setPhase(CommandContext<CommandSourceStack> ctx, float phase) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayer();
      ServerLevel level = source.getLevel();
      if (level instanceof ServerLevel) {
         List<WitherStormEntity> entities = level.getEntitiesOfClass(WitherStormEntity.class, player.getBoundingBox().inflate(50.0));
         if (!entities.isEmpty()) {
            WitherStormEntity entity = entities.get(0);
            entity.setPhaseExact(phase);
            source.sendSuccess(() -> Component.literal("Set Wither Storm phase to " + phase), true);
            return 1;
         }
      }

      source.sendFailure(Component.literal("No Wither Storm found nearby"));
      return 0;
   }

   private static int setPhaseSelected(CommandContext<CommandSourceStack> ctx, float phase) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         ws.setPhaseExact(phase);
         source.sendSuccess(() -> {
            String var10000 = stormLabel(ws);
            return Component.literal(var10000 + ": phase set to §e" + phase);
         }, true);
         count++;
      }

      return count;
   }

   private static int growStorms(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         ws.addSubGrowth(amount);
         source.sendSuccess(() -> {
            String var10000 = stormLabel(ws);
            return Component.literal(var10000 + ": grew by §e" + amount);
         }, true);
         count++;
      }

      return count;
   }

   private static int slamStorms(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         ws.forceTentacleSlam();
         source.sendSuccess(() -> {
            String var10000 = stormLabel(ws);
            return Component.literal(var10000 + ": tentacle slam triggered");
         }, true);
         count++;
      }

      return count;
   }

   private static int roarStorms(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         if (ws.level() instanceof ServerLevel server) {
            ws.roarAllHeads(true);
            server.playSound((Entity)null, ws.getX(), ws.getY(), ws.getZ(), ModSounds.HEAD_POWERFUL_ROAR, SoundSource.HOSTILE, 7.0F, 1.0F);
         }

         source.sendSuccess(() -> {
            String var10000 = stormLabel(ws);
            return Component.literal(var10000 + ": roared");
         }, true);
         count++;
      }

      return count;
   }

   private static int snatchStorms(CommandContext<CommandSourceStack> ctx, boolean throwMode) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;
      ServerPlayer player = source.getPlayer();

      for (WitherStormEntity ws : getStorms(ctx)) {
         LivingEntity target = player;
         if (player == null && ws.level() instanceof ServerLevel sl) {
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, ws.getBoundingBox().inflate(48.0))) {
               if (le != ws && le.isAlive()) {
                  target = le;
                  break;
               }
            }
         }

         if (target != null) {
            ws.beginSnatch(target, throwMode);
            String var10000 = stormLabel(ws);
            String action = throwMode ? ": tentacle throw swoop initiated!" : ": tentacle devour swoop initiated!";
            source.sendSuccess(() -> Component.literal(var10000 + action), true);
            count++;
         }
      }

      return count;
   }

   private static int stormStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         String var10000 = stormLabel(ws);
         source.sendSuccess(
            () -> Component.literal(var10000 + ": phase §e" + String.format(Locale.ROOT, "%.2f", ws.getPhase()) + "§r, sub-growth §e" + ws.getSubGrowth()),
            false
         );
         count++;
      }

      return count;
   }

   private static int consumeStorms(CommandContext<CommandSourceStack> ctx, int radius) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         int broken = 0;
         if (ws.level() instanceof ServerLevel server) {
            broken = ws.consumeBlocks(server, radius);
         }

         int fb = broken;
         source.sendSuccess(() -> {
            String var10000 = stormLabel(ws);
            return Component.literal(var10000 + ": consumed §e" + fb + "§r blocks");
         }, true);
         count++;
      }

      return count;
   }

   private static int healStorms(CommandContext<CommandSourceStack> ctx, float amount) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         ws.heal(amount);
         source.sendSuccess(() -> {
            String var10000 = stormLabel(ws);
            return Component.literal(var10000 + ": healed §e" + amount);
         }, true);
         count++;
      }

      return count;
   }

   private static int killStorms(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         if (ws.level() instanceof ServerLevel server) {
            ws.die(server.damageSources().genericKill());
         }

         source.sendSuccess(() -> {
            String var10000 = stormLabel(ws);
            return Component.literal(var10000 + ": destroyed (death blast fired)");
         }, true);
         count++;
      }

      return count;
   }

   private static int spawnStormAtPhase(CommandContext<CommandSourceStack> ctx, float phase) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerLevel level = source.getLevel();
      Vec3 spawnPos = source.getPosition().add(0.0, 2.0, 0.0);
      WitherStormEntity entity = (WitherStormEntity)ModEntityTypes.WITHER_STORM.create(level, EntitySpawnReason.COMMAND);
      if (entity == null) {
         source.sendFailure(Component.literal("Failed to create Wither Storm"));
         return 0;
      }

      // setPhaseExact updates both the server field and the synchronized
      // PHASE/PHASE4/SUBGROWTH values before the first client snapshot. This
      // is especially important for /dabyws storm spawn <phase>, where the
      // client otherwise receives the phase-0 command-body state first.
      entity.setPos(spawnPos);
      entity.setPhaseExact(phase);
      level.addFreshEntity(entity);
      source.sendSuccess(() -> Component.literal("Wither Storm spawned at phase §e" + phase), true);
      return 1;
   }

   private static int stormBuild(CommandContext<CommandSourceStack> ctx, String type) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         source.sendFailure(Component.literal("Must be a player to build here"));
         return 0;
      } else {
         int placed = StructureBuilder.build(source.getLevel(), player.blockPosition(), type);
         if ((type.equals("town") || type.equals("endertown")) && placed > 0) {
            StoryNpcSpawner.populate(source.getLevel(), player.blockPosition(), WitherStormConfigs.get(source.getLevel()).townNpcPopulation);
         }

         if (placed == 0) {
            source.sendFailure(
               Component.literal(
                  "Unknown building type: "
                     + type
                     + " (beacon | house | portal | church | town | endertown | temple | redstonia | boomtown | endercon | bunker | shrine | island | sky_fortress | farlands | ivor_lab | storm_nest | vault)"
               )
            );
            return 0;
         } else {
            source.sendSuccess(() -> Component.literal("Built §e" + type + "§r (" + placed + " blocks) in front of you."), true);
            return 1;
         }
      }
   }

   private static int stormPreset(CommandContext<CommandSourceStack> ctx, String name) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      WitherStormWorldConfig cfg = WitherStormConfigs.get(source.getLevel());
      switch (name) {
         case "instant":
            cfg.instantGrowth = 1;
            cfg.instantGrowthRate = 4.0;
            cfg.infinitePhases = 1;
            cfg.phaseCeiling = 10.0;
            break;
         case "raid":
            cfg.structureRaid = 1;
            cfg.targetingMode = 4;
            cfg.tentacleSlam = 1;
            cfg.tentacleSlamInterval = 180;
            break;
         case "apocalypse":
            cfg.instantGrowth = 1;
            cfg.instantGrowthRate = 4.0;
            cfg.infinitePhases = 1;
            cfg.phaseCeiling = 15.0;
            cfg.structureRaid = 1;
            cfg.targetingMode = 4;
            cfg.tentacleSlam = 1;
            cfg.tentacleSlamInterval = 160;
            cfg.phase4Requirement = 1200;
            cfg.phase5Requirement = 4000;
            break;
         default:
            source.sendFailure(Component.literal("Unknown preset: " + name + " (instant | raid | apocalypse)"));
            return 0;
      }

      cfg.markChanged();
      SigeonNetwork.broadcastSync(source.getLevel());
      source.sendSuccess(() -> Component.literal("[server] Applied preset §e" + name), true);
      return 1;
   }

   private static void registerExtra(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                      "storm"
                                                   )
                                                   .then(
                                                      ((LiteralArgumentBuilder)Commands.literal("grow")
                                                            .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                                         .then(
                                                            Commands.argument("targets", EntityArgument.entities())
                                                               .then(
                                                                  Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                                                     .executes(ctx -> growStorms(ctx, IntegerArgumentType.getInteger(ctx, "amount")))
                                                               )
                                                         )
                                                   ))
                                                .then(
                                                   ((LiteralArgumentBuilder)Commands.literal("slam")
                                                         .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                                      .then(Commands.argument("targets", EntityArgument.entities()).executes(ctx -> slamStorms(ctx)))
                                                ))
                                             .then(
                                                ((LiteralArgumentBuilder)Commands.literal("grab")
                                                      .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                                   .then(Commands.argument("targets", EntityArgument.entities()).executes(ctx -> snatchStorms(ctx, false)))
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)Commands.literal("throw")
                                                   .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                                .then(Commands.argument("targets", EntityArgument.entities()).executes(ctx -> snatchStorms(ctx, true)))
                                          ))
                                       .then(
                                          ((LiteralArgumentBuilder)Commands.literal("eat")
                                                .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                             .then(Commands.argument("targets", EntityArgument.entities()).executes(ctx -> snatchStorms(ctx, false)))
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.literal("roar")
                                             .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                          .then(Commands.argument("targets", EntityArgument.entities()).executes(ctx -> roarStorms(ctx)))
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)Commands.literal("consume")
                                          .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                       .then(
                                          Commands.argument("targets", EntityArgument.entities())
                                             .then(
                                                Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                                                   .executes(ctx -> consumeStorms(ctx, IntegerArgumentType.getInteger(ctx, "radius")))
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("spawn")
                                       .requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                    .then(
                                       Commands.argument("phase", FloatArgumentType.floatArg(0.0F, 10.0F))
                                          .executes(ctx -> spawnStormAtPhase(ctx, FloatArgumentType.getFloat(ctx, "phase")))
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("heal").requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                                 .then(
                                    Commands.argument("targets", EntityArgument.entities())
                                       .then(
                                          Commands.argument("amount", FloatArgumentType.floatArg(0.0F, 5000.0F))
                                             .executes(ctx -> healStorms(ctx, FloatArgumentType.getFloat(ctx, "amount")))
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("kill").requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                              .then(Commands.argument("targets", EntityArgument.entities()).executes(ctx -> killStorms(ctx)))
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("preset").requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                           .then(
                              Commands.argument("name", StringArgumentType.word()).executes(ctx -> stormPreset(ctx, StringArgumentType.getString(ctx, "name")))
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("build").requires(net.dabicco.witherstormmod.command.DabyWSCommand::mayEditServerConfig))
                        .then(Commands.argument("type", StringArgumentType.word()).executes(ctx -> stormBuild(ctx, StringArgumentType.getString(ctx, "type"))))
                  ))
               .then(Commands.literal("status").then(Commands.argument("targets", EntityArgument.entities()).executes(ctx -> stormStatus(ctx)))))
            .then(
               Commands.literal("phase")
                  .then(
                     Commands.argument("targets", EntityArgument.entities())
                        .then(
                           Commands.argument("phase", FloatArgumentType.floatArg(0.0F, 10.0F))
                              .executes(ctx -> setPhaseSelected(ctx, FloatArgumentType.getFloat(ctx, "phase")))
                        )
                  )
            )
      );
   }

   private static List<WitherStormEntity> getStorms(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      List<WitherStormEntity> storms = new ArrayList<>();

      for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
         if (e instanceof WitherStormEntity ws) {
            storms.add(ws);
         }
      }

      if (storms.isEmpty()) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("No Wither Storms matched the selector"));
      }

      return storms;
   }

   private static String stormLabel(WitherStormEntity ws) {
      return "Storm #" + ws.getId();
   }

   private static int targetingGet(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int mode = WitherStormConfigs.get(source.getLevel()).targetingMode;
      String name = WitherStormWorldConfig.TARGETING_LABELS[Math.max(0, Math.min(mode, WitherStormWorldConfig.TARGETING_LABELS.length - 1))];
      source.sendSuccess(() -> Component.literal("Targeting mode: §e" + name), false);
      return 1;
   }

   private static int targetingSet(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      String want = StringArgumentType.getString(ctx, "mode").toLowerCase(Locale.ROOT);
      int mode = -1;

      for (int i = 0; i < WitherStormWorldConfig.TARGETING_LABELS.length; i++) {
         if (WitherStormWorldConfig.TARGETING_LABELS[i].toLowerCase(Locale.ROOT).equals(want)) {
            mode = i;
            break;
         }
      }

      if (mode < 0) {
         source.sendFailure(Component.literal("Unknown mode '" + want + "' (ultimate, natural, nearest, group)"));
         return 0;
      } else {
         WitherStormWorldConfig cfg = WitherStormConfigs.get(source.getLevel());
         cfg.targetingMode = mode;
         cfg.markChanged();
         SigeonNetwork.broadcastSync(source.getLevel());
         String name = WitherStormWorldConfig.TARGETING_LABELS[mode];
         source.sendSuccess(() -> Component.literal("Targeting mode set to §a" + name), true);
         return 1;
      }
   }

   private static String targetName(CommandSourceStack source, UUID uuid) {
      if (uuid == null) {
         return "none";
      } else {
         Player online = source.getLevel().getPlayerByUUID(uuid);
         return online != null ? online.getName().getString() : uuid.toString();
      }
   }

   private static int movementTargetGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         String name = targetName(source, ws.getUltimateTargetUUID());
         String locked = ws.isUltimateTargetLocked() ? " §7(pinned)" : "";
         source.sendSuccess(() -> Component.literal(stormLabel(ws) + ": ultimate target = §e" + name + locked), false);
         count++;
      }

      return count;
   }

   private static int movementTargetSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         ws.setUltimateTarget(player);
         source.sendSuccess(() -> {
            String var10000 = stormLabel(ws);
            return Component.literal(var10000 + ": ultimate target pinned to §e" + player.getName().getString());
         }, true);
         count++;
      }

      return count;
   }

   private static int movementTargetClear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         ws.clearUltimateTarget();
         source.sendSuccess(() -> Component.literal(stormLabel(ws) + ": ultimate target cleared (auto-pick resumes)"), true);
         count++;
      }

      return count;
   }

   private static int movementTargetReroll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         Player picked = ws.rerollUltimateTarget();
         String name = picked != null ? picked.getName().getString() : "none";
         source.sendSuccess(() -> {
            String var10000 = stormLabel(ws);
            return Component.literal(var10000 + ": rerolled ultimate target -> §e" + name);
         }, true);
         count++;
      }

      return count;
   }

   private static int movementSetChasing(CommandContext<CommandSourceStack> ctx, boolean chasing) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         if (chasing && !ws.isPhase4()) {
            source.sendFailure(Component.literal(stormLabel(ws) + " is not phase 4+ -- chasing only applies from phase 4"));
         } else {
            ws.setChasing(chasing);
            source.sendSuccess(() -> {
               String var10000 = stormLabel(ws);
               return Component.literal(var10000 + ": chasing " + (chasing ? "§aENABLED" : "§cdisabled"));
            }, true);
            count++;
         }
      }

      return count;
   }

   private static int movementDistract(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;

      for (WitherStormEntity ws : getStorms(ctx)) {
         if (!ws.isPhase4()) {
            source.sendFailure(Component.literal(stormLabel(ws) + " is not phase 4+ -- distractions only apply from phase 4"));
         } else {
            ws.distractNow();
            source.sendSuccess(() -> Component.literal(stormLabel(ws) + ": distracted -- wandering off"), true);
            count++;
         }
      }

      return count;
   }

   private static int movementStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      int count = 0;
      int var10003 = WitherStormWorldConfig.TARGETING_LABELS.length;
      String targeting = WitherStormWorldConfig.TARGETING_LABELS[Math.max(0, Math.min(WitherStormConfigs.get(source.getLevel()).targetingMode, var10003 - 1))];

      for (WitherStormEntity ws : getStorms(ctx)) {
         String name = targetName(source, ws.getUltimateTargetUUID());
         StringBuilder line = new StringBuilder(stormLabel(ws))
            .append(": targeting=§e")
            .append(targeting)
            .append("§r mode=§e")
            .append(ws.getMoveMode())
            .append("§r phase=")
            .append(String.format("%.2f", ws.getPhase()))
            .append(" target=§e")
            .append(name)
            .append("§r")
            .append(ws.isUltimateTargetLocked() ? " (pinned)" : "");
         int chaseEta = ws.getChaseEtaSeconds();
         if (chaseEta >= 0) {
            line.append(" nextChase=").append(chaseEta / 60).append("m").append(chaseEta % 60).append("s");
         }

         int distractLeft = ws.getDistractionSecondsLeft();
         if (distractLeft >= 0) {
            line.append(" distractedFor=").append(distractLeft).append("s");
         }

         source.sendSuccess(() -> Component.literal(line.toString()), false);
         count++;
      }

      return count;
   }

   private static int testPortalProbe(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         source.sendFailure(Component.literal("Must be run by a player"));
         return 0;
      } else {
         ServerLevel found = player.level();
         if (!(found instanceof ServerLevel)) {
            source.sendFailure(Component.literal("Must be run in a server level"));
            return 0;
         } else {
            ServerLevel level = found;
            if (found.dimension() != Level.NETHER) {
               source.sendFailure(Component.literal("§cRun this in the NETHER, near the portal you want probed."));
               return 0;
            } else {
               BlockPos portal = null;

               label63:
               for (int r = 0; r <= 64 && portal == null; r += 2) {
                  for (int dx = -r; dx <= r; dx += 2) {
                     for (int dz = -r; dz <= r; dz += 2) {
                        for (int dy = -24; dy <= 24; dy += 2) {
                           BlockPos p = player.blockPosition().offset(dx, dy, dz);
                           if (level.getBlockState(p).is(Blocks.NETHER_PORTAL)) {
                              portal = p.immutable();
                              break label63;
                           }
                        }
                     }
                  }
               }

               if (portal == null) {
                  source.sendFailure(Component.literal("§cNo nether portal found within 64 blocks."));
                  return 0;
               } else if (!WitherStormEntity.spawnPortalProbe(level, portal, player)) {
                  source.sendFailure(Component.literal("§cCouldn't start the probe there."));
                  return 0;
               } else {
                  BlockPos probePortal = portal;
                  source.sendSuccess(() -> Component.literal("§aPortal probe started at " + probePortal.toShortString()), true);
                  return 1;
               }
            }
         }
      }
   }

   private static int testBowelsAdvance(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         source.sendFailure(Component.literal("Must be run by a player"));
         return 0;
      } else {
         ServerLevel var4 = player.level();
         if (var4 instanceof ServerLevel) {
            ServerLevel level = var4;
            if (BowelsGravity.isBowels(var4)) {
               List<BowelsHeartEntity> found = var4.getEntitiesOfClass(BowelsHeartEntity.class, player.getBoundingBox().inflate(128.0));
               if (found.isEmpty()) {
                  source.sendFailure(Component.literal("No command block loaded nearby"));
                  return 0;
               }

               BowelsHeartEntity heart = found.get(0);
               int cleared = 0;

               for (Mob mob : var4.getEntitiesOfClass(Mob.class, heart.getBoundingBox().inflate(64.0))) {
                  if (WitheredMobs.isWithered(mob)) {
                     mob.kill(level);
                     cleared++;
                  }
               }

               heart.clearHitGate();
               boolean took = heart.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
               if (!took) {
                  source.sendFailure(Component.literal("It refused the blow -- the wave is probably still standing"));
                  return 0;
               }

               int cracks = heart.getCracks();
               int fCleared = cleared;
               source.sendSuccess(() -> Component.literal("Bowels advanced: " + cracks + "/4 (" + fCleared + " Withered cleared)"), true);
               return 1;
            }
         }

         source.sendFailure(Component.literal("Must be run in the Bowels"));
         return 0;
      }
   }

   private static int testNetherScale(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         source.sendFailure(Component.literal("Must be run by a player"));
         return 0;
      } else {
         ServerLevel level = player.level();
         if (level instanceof ServerLevel) {
            if (level.dimension() != Level.NETHER) {
               source.sendSuccess(() -> Component.literal("§eHeads up: you're not in the Nether -- the scaling normally only fires there."), false);
            }

            boolean ok = NetherScaleManager.trigger(level, player.position());
            if (ok) {
               source.sendSuccess(() -> Component.literal("§aNether Scaling triggered!"), true);
               return 1;
            } else {
               source.sendFailure(Component.literal("Failed to trigger Nether Scaling"));
               return 0;
            }
         } else {
            source.sendFailure(Component.literal("Must be run in a server level"));
            return 0;
         }
      }
   }

   private static int bowelsEnter(CommandSourceStack source, Collection<? extends Entity> targets) {
      ServerLevel bowels = source.getServer().getLevel(BowelsGravity.BOWELS);
      if (bowels == null) {
         source.sendFailure(Component.literal("The Bowels is not loaded on this server."));
         return 0;
      } else {
         BowelsHallway.ensureBuilt(bowels);
         Vec3 mouth = BowelsEntry.arrival();
         int moved = 0;
         int spread = 0;
         int refused = 0;

         for (Entity entity : targets) {
            if (!entity.isRemoved()) {
               if (!(entity instanceof WitherStormEntity) && !(entity instanceof SeveredWitherStormEntity) && !(entity instanceof WitherStormHeadEntity)) {
                  BowelsGravity.release(entity);
                  double offset = spread * 2.0;
                  spread++;
                  if (entity.teleportTo(bowels, mouth.x, mouth.y, mouth.z + offset, Set.of(), BowelsEntry.arrivalYaw(), BowelsEntry.arrivalPitch(), false)) {
                     moved++;
                  }
               } else {
                  refused++;
               }
            }
         }

         if (refused > 0) {
            source.sendFailure(Component.literal("A Wither Storm cannot go inside itself (" + refused + " left behind)."));
         }

         if (moved == 0) {
            if (refused == 0) {
               source.sendFailure(Component.literal("Nothing went in."));
            }

            return 0;
         } else {
            int fMoved = moved;
            source.sendSuccess(() -> Component.literal(fMoved == 1 ? "Down you go." : "Down they go (" + fMoved + ")."), true);
            return moved;
         }
      }
   }

   private static boolean mayEditServerConfig(CommandSourceStack source) {
      ServerPlayer player = source.getPlayer();
      return player == null || SigeonNetwork.canEdit(player);
   }

   private static String formatValue(Key key, double value) {
      return key.integer() ? String.valueOf((long)value) : String.format("%.4g", value);
   }

   private static int listServerConfig(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      WitherStormWorldConfig cfg = WitherStormConfigs.get(source.getLevel());
      source.sendSuccess(() -> Component.literal("Wither Storm server config:").withStyle(ChatFormatting.GOLD), false);

      for (Key key : WitherStormWorldConfig.KEYS.values()) {
         source.sendSuccess(
            () -> Component.literal("  " + key.name() + " = ")
               .withStyle(ChatFormatting.GRAY)
               .append(Component.literal(formatValue(key, key.get().applyAsDouble(cfg))).withStyle(ChatFormatting.WHITE)),
            false
         );
      }

      return 1;
   }

   private static int getServerConfig(CommandContext<CommandSourceStack> ctx, String name) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      Key key = (Key)WitherStormWorldConfig.KEYS.get(name);
      if (key == null) {
         source.sendFailure(Component.literal("Unknown server config: " + name));
         return 0;
      } else {
         WitherStormWorldConfig cfg = WitherStormConfigs.get(source.getLevel());
         double value = key.get().applyAsDouble(cfg);
         source.sendSuccess(
            () -> Component.literal(name + " = ")
               .withStyle(ChatFormatting.GRAY)
               .append(Component.literal(formatValue(key, value)).withStyle(ChatFormatting.WHITE))
               .append(Component.literal("  (" + key.description() + ")").withStyle(ChatFormatting.DARK_GRAY)),
            false
         );
         return 1;
      }
   }

   private static int setServerConfig(CommandContext<CommandSourceStack> ctx, String name, double value) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      Key key = (Key)WitherStormWorldConfig.KEYS.get(name);
      if (key == null) {
         source.sendFailure(Component.literal("Unknown server config: " + name));
         return 0;
      } else {
         double clamped = key.clamp(value);
         WitherStormWorldConfig cfg = WitherStormConfigs.get(source.getLevel());
         key.set().accept(cfg, clamped);
         cfg.markChanged();
         SigeonNetwork.broadcastSync(source.getLevel());
         String suffix = clamped != value ? " (clamped to " + key.min() + ".." + key.max() + ")" : "";
         source.sendSuccess(
            () -> Component.literal("[server] " + name + " set to ")
               .withStyle(ChatFormatting.GRAY)
               .append(Component.literal(formatValue(key, clamped)).withStyle(ChatFormatting.GREEN))
               .append(Component.literal(suffix).withStyle(ChatFormatting.DARK_GRAY)),
            true
         );
         return 1;
      }
   }

   private static int clientConfigAction(CommandContext<CommandSourceStack> ctx, int mode, String key, double value) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         source.sendFailure(Component.literal("Client config can only be used by a player"));
         return 0;
      } else if ((mode == 0 || mode == 1) && !DabyWSClientConfig.KEYS.containsKey(key)) {
         source.sendFailure(Component.literal("Unknown client config: " + key));
         return 0;
      } else {
         ServerPlayNetworking.send(player, new ClientConfigCommandPayload(mode, key, value));
         return 1;
      }
   }

   private static int openConfigGui(CommandContext<CommandSourceStack> ctx) {
      return clientConfigAction(ctx, 3, "", 0.0);
   }

   private record PendingSpawn(ServerLevel level, Vec3 pos, CommandSourceStack source, long spawnAtTick) {
   }
}
