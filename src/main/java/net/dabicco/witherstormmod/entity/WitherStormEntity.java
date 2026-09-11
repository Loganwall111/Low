package net.dabicco.witherstormmod.entity;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.dabicco.witherstormmod.BowelsPortal;
import net.dabicco.witherstormmod.ModAdvancements;
import net.dabicco.witherstormmod.ModEffects;
import net.dabicco.witherstormmod.ModItems;
import net.dabicco.witherstormmod.ModSounds;
import net.dabicco.witherstormmod.StormProfiler;
import net.dabicco.witherstormmod.client.GroundProbe;
import net.dabicco.witherstormmod.config.WitherStormConfigs;
import net.dabicco.witherstormmod.config.WitherStormWorldConfig;
import net.dabicco.witherstormmod.entity.cluster.WitherStormClusterEntity;
import net.dabicco.witherstormmod.entity.withered.WitheredMobs;
import net.dabicco.witherstormmod.mixin.WitherBossAccessor;
import net.dabicco.witherstormmod.network.StormRemovedPacket;
import net.dabicco.witherstormmod.network.WitherStormPositionPacket;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class WitherStormEntity extends WitherBoss implements StormHeadHost {
   private int soundedPlates = -1;
   private boolean suppressLoot;
   private static final EntityDataAccessor<Float> PHASE_DATA;
   private static final EntityDataAccessor<Boolean> PHASE4_DATA;
   private static final EntityDataAccessor<Integer> SIEGE_STAGE;
   private static final EntityDataAccessor<Integer> SUBGROWTH_DATA;
   private static final EntityDataAccessor<Long> SPAWN_ANIM_GAME_TIME;
   private static final EntityDataAccessor<Long> COLLAPSE_GAME_TIME;
   private static final EntityDataAccessor<Long> PHASE5_ANIM_GAME_TIME;
   private static final EntityDataAccessor<Long> MINI_HEAD_ANIM_GAME_TIME;
   private static final EntityDataAccessor<Long> TENTACLE_ANIM_GAME_TIME;
   private static final EntityDataAccessor<Long> PHASE58_ANIM_GAME_TIME;
   private static final EntityDataAccessor<String> ULTIMATE_TARGET_UUID;
   private static final EntityDataAccessor<Float> TARGET_YAW;
   private static final EntityDataAccessor<Float> TARGET_PITCH;
   private static final EntityDimensions PHASE_4_DIMENSIONS;
   private static final double STANDOFF_MIN = (double)50.0F;
   private static final double STANDOFF_MAX = (double)100.0F;
   private static final double FAR_PLAYER_OVERRIDE_DIST = (double)48.0F;
   private static final EntityDataAccessor<Float> BODY_ROLL;
   private static final EntityDataAccessor<Integer> SNATCH_ID;
   private double phase = (double)0.0F;
   private int subGrowth = 0;
   private int clusterCooldown;
   private UUID ultimateTargetUUID;
   private int ultimateTargetCooldown = 0;
   private boolean ultimateTargetLocked = false;
   private MoveMode moveMode;
   private int chaseTimer;
   private int distractionTimer;
   private int distractionTicksLeft;
   private double distractX;
   private double distractZ;
   private static final int MIN_CHASE_TICKS = 300;
   private int chaseElapsed;
   private Player cachedNearestPlayer;
   private int nearestPlayerUpdateTimer;
   private boolean phase4;
   private int phase4Height;
   private int phase4HeightTimer;
   private Player cachedPhase4Target;
   private Vec3 cachedMoveGoal;
   private Vec3 smoothedMoveGoal;
   private Vec3 windOffset;
   private Vec3 lastTravelVel;
   private int grappleStartleTicks;
   private float startleYaw;
   private int spawnFreezeTicks;
   private int spawnFreezeTotalTicks;
   private boolean spawnWailPending;
   private LivingEntity cachedFaceEntity;
   private Vec3 lastFacePoint;
   private int phase4TargetUpdateTimer;
   private float bodyYawVel;
   private float bodyPitchVel;
   private float bodyRollVel;
   private float bodyRoll;
   private float idleFaceYaw;
   private float recoilYaw;
   private float recoilYawVel;
   private float leanSmoothed;
   private float filteredTargetYaw;
   private Player rotationLock;
   private int rotationLockTicks;
   private static final float BODY_TURN_SMOOTH_TIME = 1.8F;
   private static final float MAX_YAW_STEP_PER_TICK = 7.0F;
   private float bodyRollO;
   private static final int HEAD_COUNT = 3;
   public static final Vec3[] HEAD_OFFSETS;
   public static final float[] HEAD_YAW_OFFSETS;
   private static final float[] HEAD_REST_YAW;
   private static final float[] HEAD_REST_ROLL;
   private final UUID[] headUUIDs;
   private int headSpawnGraceTicks;
   private boolean loadingFromSave;
   private final int[] headSpawnDelay;
   private static final int[] HEAD_SPAWN_ORDER;
   private static final int HEAD_SPAWN_STAGGER_TICKS = 40;
   private final Set<ChunkPos> forcedByUs;
   private final Set<UUID> fleeOrdered;
   private int phaseDropoutTicks;
   private static final int DEVOURER_HEAD_RETURN_GAP = 45;
   private long devourerHeadsReturnTick;
   public static final double DEVOURER_HEADS_RETURN = 6.1;
   public static final double DEVOURER_SETTLE_PHASE = 6.15;
   private static final double DEVOURER_HEAD_BACK = (double)-0.5F;
   private static final double DEVOURER_HEAD_UP = (double)1.5F;
   public static final float HATCH_TICKS_SHARED = 55.0F;
   public static final float MINI_HEAD_ENTITY_SCALE = 1.35F;
   public static final double MINI_HEAD_WORLD_FORWARD = 0.14;
   private static final double TENTACLE_RADIUS = 2.6;
   private static final double TENTACLE_CARVE_PHASE = 5.1;
   private static final double TENTACLE_MAX_REACH = (double)90.0F;
   private static final int CARVE_INTERVAL = 2;
   private long lastCarveTick;
   private static String lastCarveStatus;
   private static final boolean CARVE_ENABLED = false;
   private static final double PHASE_CEILING_EPSILON = 0.001;
   public static final double MAX_NATURAL_PHASE = 5.9999;
   public static final double MAX_DEVOURER_PHASE = 6.99;
   public static final double DEVOURER_PHASE = (double)6.0F;
   public static final double STORM_COVER_LIFT = (double)6.0F;
   private int postBombChaseTicks;
   private boolean postBombPending;
   private static final int POST_BOMB_CHASE_TICKS = 2000;
   public final GroundProbe groundProbe;
   private boolean severedHalvesMissing;
   private final UUID[] severedUUIDs;
   private final int[] severedGrace;
   private static final int SEVERED_LOAD_GRACE = 600;
   private static final int MAX_ITEM_SIGHT_CHECKS_PER_PASS = 10;
   private final Set<Integer> sightBlockedItems;
   private int pendingClusterSpawns;
   private static final int MAX_PENDING_CLUSTERS = 24;
   private static final int MAX_LIVE_CLUSTERS = 90;
   private static final int CLUSTER_CENSUS_INTERVAL = 40;
   private int clusterCensus;
   private int clusterCensusAge;
   private boolean pendingClusterCocoon;
   private final Int2LongMap fleePathedAt;
   private static final long FLEE_REPATH_TICKS = 100L;
   private static final int FLEE_PATH_BUDGET = 6;
   private final Set<Integer> hookedItems;
   public static final double GROWTH_SLOWDOWN = (double)12.0F;
   public static final double EARLY_GROWTH_SLOWDOWN = (double)5.5F;
   public static final double MINI_HEAD_PHASE = (double)2.0F;
   public static final double FRONT_TENTACLE_PHASE = (double)3.0F;
   public static final int MINI_HEAD_CHOMP_TICKS = 26;
   private static final int MINI_HEAD_SKULL_AT = 66;
   private static final int MINI_HEAD_BITE_AT = 10;
   private boolean miniHeadSkullFired;
   private boolean miniHeadBitPlayed;
   public static final double MINI_HEAD_WORLD_Y = 3.05;
   private static final int ORBIT_AFTER_TICKS = 240;
   private static final double ORBIT_STILL_RADIUS = (double)14.0F;
   private static final float ORBIT_DEGREES_PER_TICK = 0.32F;
   private Vec3 lastGoalSeen;
   private int goalStillTicks;
   private float orbitAngle;
   private static final double COLLAPSE_KNOCK_BACK = (double)16.0F;
   public static final double COLLAPSE_REST_HEIGHT = (double)-1.5F;
   private static final int GROUND_SCAN_RADIUS = 12;
   private static final int GROUND_SCAN_STEP = 4;
   private static final int GROUND_SCAN_INTERVAL = 10;
   private static final int WATER_CLEARANCE = 9;
   private int groundScanTimer;
   private double cachedGroundY;
   public static final int SIEGE_NONE = 0;
   public static final int SIEGE_GROWING = 1;
   public static final int SIEGE_LULL = 2;
   public static final int SIEGE_ACTIVE = 3;
   public static final int SIEGE_OVER = 4;
   private static final int SIEGE_GROWING_TICKS = 1210;
   private static final int SIEGE_LULL_TICKS = 1090;
   private int siegeStage;
   private int siegeTicks;
   private int siegeSpawned;
   private int tentacleSlamCooldown;
   private int raidCooldown;
   private static final Map<UUID, Integer> CAUGHT_ENDERMEN;
   private static long caughtTickedAt;
   private static final int ENDERMAN_CAUGHT_TICKS = 34;
   private boolean collapseAwardGiven;
   private boolean chaseGoalWasPresent;
   private static final DustParticleOptions AURA_MOTE;
   public static final int SPAWN_ANIM_LENGTH_TICKS = 80;
   private static final float[] PRE_P4_TURN_TIME;
   private static final double[] PRE_P4_SPEED;
   private static final float[] PRE_P4_HEAD_RANGE;
   private static final float PRE_P4_ROLL_PER_TURN = 2.6F;
   private static final float PRE_P4_ROLL_MAX = 9.0F;
   private static final float PRE_P4_PITCH_MAX = 7.0F;
   private static final double PRE_P4_LEAN_FROM = 0.85;
   private float preP4Yaw;
   private float preP4YawVel;
   private float preP4Roll;
   private float preP4Pitch;
   private static final double PHASE3_HOVER_FROM = (double)3.0F;
   private static final double PHASE3_ALTITUDE = (double)11.0F;
   private static final double PHASE3_CLIMB = 0.09;
   private static final double PHASE3_HUNT_RANGE = (double)64.0F;
   private static final double PHASE3_STANDOFF = (double)14.0F;
   private LivingEntity phase3Quarry;
   private int phase3RepickAt;
   public static final double TENTACLE_NOTICE_RANGE = (double)40.0F;
   private static final double TENTACLE_SNATCH_RANGE = (double)18.0F;
   private static final double TENTACLE_HOVER = (double)7.0F;
   private static final double TENTACLE_REACH = (double)15.0F;
   private static final int SNATCH_DURATION = 110;
   private static final int SNATCH_HITS_TO_ESCAPE = 5;
   private Player snatchVictim;
   private int snatchTicks;
   private int snatchHits;
   private Vec3 lastSnatchPos;
   private GrabTentacleEntity grabTentacle;
   private static final double GRAB_BASE_OFFSET = (double)6.0F;
   private final List<LivingEntity> doomedMobs;
   private static final int PORTAL_SCAN_INTERVAL = 40;
   private static final double PORTAL_ARRIVE_DIST = (double)6.0F;
   private static final double PORTAL_DESCEND_DIST = (double)30.0F;
   private static final int PORTAL_LOCAL_RADIUS = 128;
   private static final int PORTAL_NEAR_STORM_RADIUS = 400;
   private BlockPos probePortal;
   private int probeCommitTicks;
   private static final int PROBE_COMMIT_TIMEOUT = 1800;
   private int portalScanTimer;
   private int portalScanParity;
   private int probeCooldown;
   private static final int MAX_NEW_FORCED_CHUNKS_PER_PASS = 4;
   private int lastForcedChunkX;
   private int lastForcedChunkZ;
   private static final int REASSERT_INTERVAL = 100;
   private static final double FACE_RING_ENTER = (double)36.0F;
   private static final double FACE_RING_LEAVE = (double)100.0F;
   private boolean faceRingHold;
   private final List<UUID> groupMembers;
   private int groupHiddenTicks;
   private static final double GROUP_CLUSTER_RADIUS = (double)28.0F;
   private static final double NATURAL_SEEK_RANGE = (double)96.0F;
   private static final double NATURAL_LOSE_RANGE = (double)192.0F;
   private static final double NATURAL_FAR_MIN = (double)200.0F;
   private static final double NATURAL_FAR_MAX = (double)520.0F;
   private static final double NATURAL_ARRIVE = (double)28.0F;
   private static final float NATURAL_WANDER_CHANCE = 0.45F;
   private static final float NATURAL_CHASE_CHANCE = 0.35F;
   private LivingEntity naturalPrey;
   private Vec3 naturalWander;
   private int naturalRepickAt;
   private static final int STRUCTURE_SEARCH_CHUNKS = 96;
   private static final double STRUCTURE_ARRIVE = (double)40.0F;
   private static final int STRUCTURE_DWELL_TICKS = 3600;
   private static final int STRUCTURE_SEARCH_INTERVAL = 400;
   private static final double STRUCTURE_DISTRACT_RANGE = (double)96.0F;
   private BlockPos structureTarget;
   private boolean structureArrived;
   private boolean structureDistracted;
   private int structureDwell;
   private int structureSearchAt;
   private final Set<Long> structuresVisited;
   private static final TagKey<Structure> STORM_TARGETS;
   private static final int STRUCTURE_PROBE_OFFSET = 640;

   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      if (!this.suppressLoot) {
         this.spawnAtLocation(level, new ItemStack(ModItems.WITHERED_NETHER_STAR));
      }
   }

   public void die(DamageSource source) {
      super.die(source);
      if (this.level() instanceof ServerLevel server) {
         this.deathBlast(server);
      }
   }

   private void deathBlast(ServerLevel server) {
      WitherStormWorldConfig cfg = WitherStormConfigs.get(server);
      if (cfg.deathBlast == 0) {
         return;
      }

      double radius = cfg.deathBlastRadius;
      Vec3 centre = this.position();
      this.carveSphere(server, centre, radius);
      server.playSound((Entity)null, centre.x, centre.y, centre.z, ModSounds.FORMIDIBOMB_EXPLOSION, SoundSource.HOSTILE, 8.0F, 1.0F);
      server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centre.x, centre.y, centre.z, 1, 0.0, 0.0, 0.0, 0.0);

      double rSq = radius * radius;
      for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius), (e) -> e != this && e.isAlive())) {
         double dx = victim.getX() - centre.x;
         double dz = victim.getZ() - centre.z;
         double d = dx * dx + dz * dz;
         if (d <= rSq && d > 1.0E-4) {
            double dist = Math.sqrt(d);
            double falloff = 1.0 - dist / radius;
            victim.hurtServer(server, server.damageSources().mobAttack(this), (float)(4.0 + 16.0 * falloff));
            victim.setDeltaMovement(victim.getDeltaMovement().add(dx / dist * 2.2 * falloff, 0.9 * falloff, dz / dist * 2.2 * falloff));
         }
      }
   }

   public int getCoverPlates() {
      return BowelsPortal.platesFor(this.getPhase());
   }

   public boolean hasCover() {
      return this.getPhase() >= 6.8;
   }

   private void tickCover() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel server) {
         if (!this.hasCover()) {
            this.soundedPlates = -1;
         } else {
            int plates = this.getCoverPlates();
            if (plates > this.soundedPlates) {
               if (this.soundedPlates >= 0) {
                  server.playSound((Entity)null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHULKER_OPEN, SoundSource.HOSTILE, 6.0F, 0.5F);
               }

               this.soundedPlates = plates;
            }

            BowelsPortal.tick(server, this, plates);
         }
      }
   }

   public void setSuppressLoot(boolean value) {
      this.suppressLoot = value;
   }

   private static double chaseReach(WitherStormWorldConfig cfg) {
      return Math.max(cfg.stormStandoff, (double)20.0F);
   }

   public void lockRotationOn(Player player) {
      this.rotationLock = player;
      this.rotationLockTicks = 10;
   }

   public float getBodyRoll() {
      return (Float)this.entityData.get(BODY_ROLL);
   }

   public float headYawOffsetFor(int index) {
      return this.isPhase4() ? restYawFor(index) : 0.0F;
   }

   public float headRollOffsetFor(int index) {
      return this.isPhase4() ? restRollFor(index) : 0.0F;
   }

   public static float restYawFor(int index) {
      return HEAD_REST_YAW[Mth.clamp(index, 0, HEAD_REST_YAW.length - 1)];
   }

   public static float restRollFor(int index) {
      return HEAD_REST_ROLL[Mth.clamp(index, 0, HEAD_REST_ROLL.length - 1)];
   }

   public WitherStormEntity(EntityType<? extends WitherStormEntity> entityType, Level world) {
      super(entityType, world);
      this.moveMode = WitherStormEntity.MoveMode.FOLLOW;
      this.chaseTimer = -1;
      this.distractionTimer = -1;
      this.distractionTicksLeft = 0;
      this.chaseElapsed = 0;
      this.cachedNearestPlayer = null;
      this.nearestPlayerUpdateTimer = 0;
      this.phase4 = false;
      this.phase4Height = 35;
      this.phase4HeightTimer = 0;
      this.cachedPhase4Target = null;
      this.cachedMoveGoal = null;
      this.smoothedMoveGoal = null;
      this.windOffset = Vec3.ZERO;
      this.lastTravelVel = Vec3.ZERO;
      this.grappleStartleTicks = 0;
      this.startleYaw = 0.0F;
      this.spawnFreezeTicks = 0;
      this.spawnFreezeTotalTicks = 0;
      this.spawnWailPending = false;
      this.cachedFaceEntity = null;
      this.lastFacePoint = null;
      this.phase4TargetUpdateTimer = 0;
      this.bodyYawVel = 0.0F;
      this.bodyPitchVel = 0.0F;
      this.bodyRollVel = 0.0F;
      this.bodyRoll = 0.0F;
      this.idleFaceYaw = Float.NaN;
      this.recoilYaw = 0.0F;
      this.recoilYawVel = 0.0F;
      this.leanSmoothed = 0.0F;
      this.filteredTargetYaw = Float.NaN;
      this.bodyRollO = 0.0F;
      this.headUUIDs = new UUID[3];
      this.headSpawnGraceTicks = 0;
      this.loadingFromSave = false;
      this.headSpawnDelay = new int[3];
      this.forcedByUs = new HashSet();
      this.fleeOrdered = new HashSet();
      this.devourerHeadsReturnTick = -1L;
      this.lastCarveTick = Long.MIN_VALUE;
      this.groundProbe = new GroundProbe();
      this.severedHalvesMissing = true;
      this.severedUUIDs = new UUID[2];
      this.severedGrace = new int[2];
      this.sightBlockedItems = new HashSet();
      this.fleePathedAt = new Int2LongOpenHashMap();
      this.hookedItems = new HashSet();
      this.miniHeadSkullFired = false;
      this.miniHeadBitPlayed = false;
      this.lastGoalSeen = null;
      this.goalStillTicks = 0;
      this.orbitAngle = 0.0F;
      this.groundScanTimer = 0;
      this.cachedGroundY = Double.NaN;
      this.siegeStage = 0;
      this.collapseAwardGiven = false;
      this.chaseGoalWasPresent = false;
      this.preP4Yaw = Float.NaN;
      this.preP4YawVel = 0.0F;
      this.preP4Roll = 0.0F;
      this.preP4Pitch = 0.0F;
      this.phase3Quarry = null;
      this.phase3RepickAt = 0;
      this.doomedMobs = new ArrayList();
      this.lastForcedChunkX = Integer.MIN_VALUE;
      this.lastForcedChunkZ = Integer.MIN_VALUE;
      this.faceRingHold = false;
      this.groupMembers = new ArrayList();
      this.groupHiddenTicks = 0;
      this.naturalPrey = null;
      this.naturalWander = null;
      this.naturalRepickAt = 0;
      this.structureTarget = null;
      this.structureArrived = false;
      this.structureDistracted = false;
      this.structureDwell = 0;
      this.structureSearchAt = 0;
      this.structuresVisited = new HashSet();
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return true;
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(PHASE_DATA, 0.0F);
      builder.define(COLLAPSE_GAME_TIME, -1L);
      builder.define(PHASE4_DATA, false);
      builder.define(SIEGE_STAGE, 0);
      builder.define(SUBGROWTH_DATA, 0);
      builder.define(SPAWN_ANIM_GAME_TIME, -1L);
      builder.define(PHASE5_ANIM_GAME_TIME, -1L);
      builder.define(MINI_HEAD_ANIM_GAME_TIME, -1L);
      builder.define(TENTACLE_ANIM_GAME_TIME, -1L);
      builder.define(PHASE58_ANIM_GAME_TIME, -1L);
      builder.define(ULTIMATE_TARGET_UUID, "");
      builder.define(TARGET_YAW, 0.0F);
      builder.define(TARGET_PITCH, 0.0F);
      builder.define(BODY_ROLL, 0.0F);
      builder.define(SNATCH_ID, -1);
   }

   public int getSnatchId() {
      return (Integer)this.entityData.get(SNATCH_ID);
   }

   public void clientSyncPose(float bodyRoll, boolean phase4) {
      this.entityData.set(BODY_ROLL, bodyRoll);
      this.entityData.set(PHASE4_DATA, phase4);
      this.entityData.set(SPAWN_ANIM_GAME_TIME, -1L);
   }

   public void clientSyncPhase(float phase) {
      this.phase = (double)phase;
      this.entityData.set(PHASE_DATA, phase);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (key == PHASE_DATA && this.level().isClientSide()) {
         this.phase = (double)(Float)this.entityData.get(PHASE_DATA);
      }

      if (key == PHASE4_DATA) {
         this.phase4 = (Boolean)this.entityData.get(PHASE4_DATA);
         this.refreshDimensions();
         if (!this.level().isClientSide() && !this.loadingFromSave) {
            if (this.phase4) {
               this.spawnHeadsIfNeeded();
            } else {
               this.removeHeads();
               this.despawnGrabTentacle();
            }
         }
      }

   }

   public double getSubPhase() {
      int mainPhase = (int)Math.floor(this.phase);
      int requirement = growthRequirement(mainPhase, WitherStormConfigs.get(this.level()));
      return Math.min((double)1.0F, (double)this.subGrowth / (double)requirement);
   }

   public void tick() {
      super.tick();
      this.tickCover();
      if (!this.level().isClientSide()) {
         if (CollapseAnim.isImpactTick(this.collapseTicks())) {
            this.level().playSound((Entity)null, this.getX(), this.getY(), this.getZ(), ModSounds.STORM_THUMP_LARGE, SoundSource.HOSTILE, 6.0F, 1.0F);
         }

         if (!this.isDeadOrDying()) {
            if (this.spawnFreezeTicks > 0) {
               --this.spawnFreezeTicks;
               this.setDeltaMovement(Vec3.ZERO);
               this.updateChunkLoading();
               if (this.spawnFreezeTotalTicks > 0) {
                  float p = 1.0F - (float)this.spawnFreezeTicks / (float)this.spawnFreezeTotalTicks;
                  ((WitherBossAccessor)this).getBossEvent().setProgress(Mth.clamp(p, 0.0F, 1.0F));
               }

            } else {
               this.updatePhase5Stamp(this.phase);
               if (this.spawnWailPending) {
                  this.spawnWailPending = false;
                  this.playGlobalSpawnWail();
               }

               if (this.clusterCooldown > 0) {
                  --this.clusterCooldown;
               }

               if (this.pendingClusterSpawns > 0 && !this.isCollapsed() && !this.clusterPopulationFull()) {
                  --this.pendingClusterSpawns;
                  long tCluster = StormProfiler.start();
                  if (this.pendingClusterCocoon) {
                     this.spawnCocoonCluster();
                  } else {
                     this.spawnBlockCluster();
                  }

                  StormProfiler.end("cluster spawn", tCluster);
               }

               boolean cocooning = !this.isPhase4() && this.phase >= 3.7;
               if (this.clusterCooldown <= 0) {
                  this.pendingClusterSpawns = Math.min(24, this.pendingClusterSpawns + (this.isPhase4() ? 3 : (cocooning ? 8 : 1)));
                  this.pendingClusterCocoon = cocooning;
                  WitherStormWorldConfig config = WitherStormConfigs.get(this.level());
                  this.clusterCooldown = this.isPhase4() ? Math.max(config.clusterCooldown / 4, 10) : (cocooning ? Math.max(config.clusterCooldown / 8, 5) : config.clusterCooldown);
               }

               WitherStormWorldConfig config = WitherStormConfigs.get(this.level());
               this.updateUltimateTarget();
               if (!this.isPhase4()) {
                  Vec3 goal = this.resolveMoveGoal(config);
                  Player nearest = this.nearestTargetable();
                  if (goal != null) {
                     boolean playerVeryFar = nearest != null && (double)this.distanceTo(nearest) > (double)48.0F;
                     if (playerVeryFar && this.getTarget() != null) {
                        this.setTarget((LivingEntity)null);
                     }

                     if (this.getTarget() == null || playerVeryFar) {
                        this.followGoal(goal);
                     }
                  }

                  this.tickMiniHeadEmergence();
                  Level var6 = this.level();
                  if (var6 instanceof ServerLevel) {
                     ServerLevel miniLevel = (ServerLevel)var6;
                     if (this.phase >= (double)2.0F) {
                        this.updateMiniHead(miniLevel);
                     } else if (this.headUUIDs[0] != null) {
                        this.removeMiniHead(miniLevel);
                     }
                  }

                  if (this.phase >= (double)3.0F) {
                     this.phase3Hover();
                  }

                  if (this.phase >= (double)3.0F) {
                     this.cocoonTick(nearest);
                  } else if ((Float)this.entityData.get(BODY_ROLL) != 0.0F) {
                     this.entityData.set(BODY_ROLL, 0.0F);
                  }
               }

               if (this.isPhase4()) {
                  this.phaseDropoutTicks = 100;
               } else if (this.phaseDropoutTicks > 0) {
                  --this.phaseDropoutTicks;
               }

               if (this.tickCount % 2 == 0) {
                  Level lvl = this.level();
                  if (lvl instanceof ServerLevel) {
                     ServerLevel sl = (ServerLevel)lvl;
                     long gameTime = sl.getGameTime();
                     WitherStormPositionPacket.HeadData[] heads = new WitherStormPositionPacket.HeadData[3];

                     for(int i = 0; i < 3; ++i) {
                        WitherStormHeadEntity head = this.getHead(sl, i);
                        if (head == null) {
                           heads[i] = WitherStormPositionPacket.HeadData.EMPTY;
                        } else {
                           long fireStart = head.getFireStartTime();
                           int fireElapsed = fireStart >= 0L && gameTime - fireStart < 25L ? (int)(gameTime - fireStart) : -1;
                           boolean beamActive = head.isBeamActive();
                           Vec3 beamCenter = beamActive ? head.getBeamEndExact() : Vec3.ZERO;
                           heads[i] = new WitherStormPositionPacket.HeadData(head.getLocalYaw(), head.getXRot(), fireElapsed, beamActive, beamCenter.x, beamCenter.y, beamCenter.z);
                        }
                     }

                     WitherStormPositionPacket pkt = new WitherStormPositionPacket(this.getId(), this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), (Float)this.entityData.get(BODY_ROLL), (float)this.phase, elapsedTicksSince(this.getPhase5AnimGameTime(), gameTime), elapsedTicksSince(this.getPhase58AnimGameTime(), gameTime), this.activeHeadCount(), heads, this.isCollapsed(), (int)Math.max(-1.0F, this.collapseTicks()), this.getSiegeStage(), this.siegeProgress(), this.collectSeveredData(sl));

                     for(ServerPlayer p : PlayerLookup.level(sl)) {
                        ServerPlayNetworking.send(p, pkt);
                     }
                  }
               }

               if (this.isCollapsed()) {
                  this.setTarget((LivingEntity)null);
                  this.setYRot(this.yRotO);
                  this.setYHeadRot(this.yHeadRotO);
                  this.setYBodyRot(this.yBodyRotO);
                  this.tickCollapseFall();
               } else if (this.isPhase4()) {
                  this.phase4Movement();
               }

               if (this.level() instanceof ServerLevel) {
                  ServerBossEvent bossBar = ((WitherBossAccessor)this).getBossEvent();
                  boolean shouldShow = !this.isCollapsed();
                  if (bossBar.isVisible() != shouldShow) {
                     bossBar.setVisible(shouldShow);
                  }
               }

               if (this.isPhase4() && this.snatchVictim == null) {
                  Level tSick = this.level();
                  if (tSick instanceof ServerLevel) {
                     ServerLevel reachLevel = (ServerLevel)tSick;
                     int reach = -1;

                     for(int i = 0; i < 3; ++i) {
                        WitherStormHeadEntity h = this.getHead(reachLevel, i);
                        if (h != null) {
                           int s = h.getSuckedId();
                           if (s >= 0 && reachLevel.getEntity(s) instanceof Player) {
                              reach = s;
                              break;
                           }
                        }
                     }

                     if ((Integer)this.entityData.get(SNATCH_ID) != reach) {
                        this.entityData.set(SNATCH_ID, reach);
                     }
                  }
               }

               this.tickDoomedMobs();
               if (this.isPhase4() && !this.isCollapsed()) {
                  Level var35 = this.level();
                  if (var35 instanceof ServerLevel) {
                     ServerLevel probeLevel = (ServerLevel)var35;
                     if (probeLevel.dimension() == Level.OVERWORLD) {
                        this.tickPortalProbe(probeLevel);
                     }
                  }
               }

               if (config.witherSickness != 0 && this.tickCount % 20 == 0 && !this.isCollapsed()) {
                  Level var36 = this.level();
                  if (var36 instanceof ServerLevel) {
                     ServerLevel sicknessLevel = (ServerLevel)var36;
                     long tSick = StormProfiler.start();
                     WitherSickness.serverTick(sicknessLevel, this);
                     StormProfiler.end("wither sickness", tSick);
                  }
               }

               if (this.isPhase4()) {
                  if (this.headSpawnGraceTicks > 0) {
                     --this.headSpawnGraceTicks;
                  }

                  for(int i = 0; i < 3; ++i) {
                     if (this.headSpawnDelay[i] > 0) {
                        int var10002 = this.headSpawnDelay[i]--;
                     }
                  }

                  long tHeads = StormProfiler.start();
                  this.updateHeads();
                  StormProfiler.end("update heads", tHeads);
               }

               if (!this.collapseAwardGiven && this.getCollapseGameTime() >= 0L && this.collapseTicks() >= 1170.0F) {
                  Level var38 = this.level();
                  if (var38 instanceof ServerLevel) {
                     ServerLevel riseLevel = (ServerLevel)var38;
                     this.collapseAwardGiven = true;
                     ModAdvancements.grantNearby(riseLevel, this, "weakened_arise");
                  }
               }

               if (this.isCollapsed()) {
                  this.postBombPending = true;
               } else if (this.postBombPending) {
                  this.postBombPending = false;
                  if (this.level() instanceof ServerLevel && WitherStormConfigs.get(this.level()).postFormidibombChase != 0) {
                     this.postBombChaseTicks = 2000;
                  }
               }

               if (this.postBombChaseTicks > 0) {
                  --this.postBombChaseTicks;
                  if (this.moveMode != WitherStormEntity.MoveMode.CHASING) {
                     this.startChasing();
                  }
               }

               Level var39 = this.level();
               if (var39 instanceof ServerLevel) {
                  ServerLevel siegeLevel = (ServerLevel)var39;
                  this.tickSiege(siegeLevel, WitherStormConfigs.get(siegeLevel));
               }

               if (this.severedHalvesMissing || this.tickCount % 20 == 0) {
                  this.updateSeveredHalves();
               }

               if (this.tickCount % 2 == 0 && !this.isCollapsed()) {
                  long tVac = StormProfiler.start();
                  this.vacuumItems(config);
                  StormProfiler.end("item vacuum", tVac);
               }

               if (config.mobsFlee != 0 && this.tickCount % 10 == 0 && !this.isCollapsed()) {
                  long tFlee = StormProfiler.start();
                  this.scareMobs();
                  StormProfiler.end("scare mobs", tFlee);
               }

               if (this.isPhase4()) {
                  var39 = this.level();
                  if (var39 instanceof ServerLevel) {
                     ServerLevel auraLevel = (ServerLevel)var39;
                     this.spawnAuraParticles(auraLevel);
                  }
               }

               this.resetFallDistance();
               long tChunks = StormProfiler.start();
               this.updateChunkLoading();
               StormProfiler.end("chunk force-loading", tChunks);
            }
         }
      }
   }

   private void cleanupEdgeClusters() {
      int cx = this.chunkPosition().x();
      int cz = this.chunkPosition().z();

      for(WitherStormClusterEntity cluster : this.level().getEntitiesOfClass(WitherStormClusterEntity.class, this.getBoundingBox().inflate((double)96.0F))) {
         int dcx = Math.abs(cluster.chunkPosition().x() - cx);
         int dcz = Math.abs(cluster.chunkPosition().z() - cz);
         if (Math.max(dcx, dcz) >= 4) {
            cluster.discard();
         }
      }

   }

   public Vec3 headOffsetFor(int index) {
      if (!this.isPhase4()) {
         return new Vec3((double)0.0F, 3.05, 0.14);
      } else {
         float h = this.hatchProgress();
         Vec3 full = headOffset(index, this.isDevourerForm());
         return h >= 0.999F ? full : new Vec3(Mth.lerp((double)h, (double)0.0F, full.x), Mth.lerp((double)h, 3.05, full.y), Mth.lerp((double)h, 0.14, full.z));
      }
   }

   public float headScaleFor(int index) {
      if (!this.isPhase4()) {
         return 1.35F;
      }
      float base = Mth.lerp(this.hatchProgress(), 1.35F, 6.0F);
      // The heads keep growing with the storm: 6.0 at phase 4, up to ~7.5 by
      // phase 7, plus a devourer bonus so phase 6 reads dramatically bigger.
      float grown = (float)Math.min(Math.max(this.phase - (double)4.0F, 0.0), 3.0) * 0.5F;
      return (base + grown) * (this.isDevourer() ? 1.06F : 1.0F);
   }

   public float hatchProgress() {
      if (!this.isPhase4()) {
         return 1.0F;
      } else {
         long stamp = this.getSpawnAnimGameTime();
         if (stamp < 0L) {
            return 1.0F;
         } else {
            float since = (float)(this.level().getGameTime() - stamp);
            float g = Mth.clamp(since / 55.0F, 0.0F, 1.0F);
            return g * g * (3.0F - 2.0F * g);
         }
      }
   }

   public float headYawRangeFor(int index) {
      if (!this.isPhase4()) {
         return 2.5F;
      } else {
         return index == 0 ? 52.0F : 50.0F;
      }
   }

   public boolean headBeamAllowed(int index) {
      if (this.isCollapsed()) {
         return false;
      } else {
         return this.isPhase4() || this.phase >= (double)3.0F;
      }
   }

   public float beamScaleFor(int index) {
      return this.isPhase4() ? 1.0F : 0.22500001F;
   }

   public float headPitchRangeFor(int index) {
      return this.isPhase4() ? 60.0F : 20.0F;
   }

   public boolean isDevourerForm() {
      return this.isDevourer();
   }

   public float headLitFor(int index) {
      return CollapseAnim.headLit(this.collapseTicks(), index);
   }

   public float attachPitch(float partialTick) {
      float t = this.collapseTicks();
      return t < 0.0F ? 0.0F : CollapseAnim.bodyPitch(t + partialTick);
   }

   public double attachPivotY() {
      return this.isPhase4() ? (double)17.0F : (double)0.0F;
   }

   public double attachDrop(float partialTick) {
      float t = this.collapseTicks();
      return t < 0.0F ? (double)0.0F : (double)18.5F * (double)CollapseAnim.down(t + partialTick);
   }

   private float voiceVolume(float fraction) {
      Level var5 = this.level();
      double var10000;
      if (var5 instanceof ServerLevel sl) {
         var10000 = WitherStormConfigs.get(sl).roarRange;
      } else {
         var10000 = (double)260.0F;
      }

      double range = var10000;
      return (float)Math.max((double)1.0F, range * (double)fraction / (double)16.0F);
   }

   public void roarAllHeads(boolean powerful) {
      Level var3 = this.level();
      if (var3 instanceof ServerLevel server) {
         for(int var5 = 0; var5 < 3; ++var5) {
            WitherStormHeadEntity head = this.getHead(server, var5);
            if (head != null && head.isAlive()) {
               head.triggerRoar(powerful);
            }
         }

      }
   }

   private void carveStatus(String reason) {
      if (!reason.equals(lastCarveStatus)) {
         lastCarveStatus = reason;
         System.out.println("[dabywitherstormmod][carve/server] " + reason);
      }
   }

   public void carveAlong(float[] points) {
   }

   private int carveSphere(ServerLevel server, Vec3 centre, double radius) {
      int broken = 0;
      int r = Mth.ceil(radius);
      BlockPos middle = BlockPos.containing(centre.x, centre.y, centre.z);
      double rSq = radius * radius;

      for(int dx = -r; dx <= r; ++dx) {
         for(int dy = -r; dy <= r; ++dy) {
            for(int dz = -r; dz <= r; ++dz) {
               if (!((double)(dx * dx + dy * dy + dz * dz) > rSq)) {
                  BlockPos pos = middle.offset(dx, dy, dz);
                  BlockState state = server.getBlockState(pos);
                  if (!state.isAir() && state.getBlock() != Blocks.BEDROCK && state.getFluidState().isEmpty() && !(state.getDestroySpeed(server, pos) < 0.0F)) {
                     server.destroyBlock(pos, false);
                     ++broken;
                  }
               }
            }
         }
      }

      return broken;
   }

   private void tentacleSlamTick(ServerLevel server) {
      WitherStormWorldConfig cfg = WitherStormConfigs.get(server);
      if (cfg.tentacleSlam == 0 || this.phase < 3.0) {
         this.tentacleSlamCooldown = 0;
         return;
      }

      if (this.tentacleSlamCooldown > 0) {
         --this.tentacleSlamCooldown;
         return;
      }

      int interval = cfg.tentacleSlamInterval;
      if (cfg.berserk != 0 && this.isDevourer() && this.getMaxHealth() > 0.0F && this.getHealth() / this.getMaxHealth() < (float)cfg.berserkHealth) {
         interval = cfg.berserkSlamInterval;
      }

      this.tentacleSlamCooldown = interval;
      double radius = cfg.tentacleSlamRadius;
      int bx = Mth.floor(this.getX());
      int bz = Mth.floor(this.getZ());
      int groundY = server.getHeight(Types.MOTION_BLOCKING, bx, bz);
      Vec3 centre = new Vec3(this.getX(), (double)groundY, this.getZ());
      this.carveSphere(server, centre, radius);
      server.playSound((Entity)null, centre.x, centre.y, centre.z, ModSounds.STORM_THUMP_LARGE, SoundSource.HOSTILE, 6.0F, 0.75F + this.random.nextFloat() * 0.3F);

      AABB box = new AABB(centre.x - radius, centre.y - 1.0, centre.z - radius, centre.x + radius, centre.y + radius * 1.6, centre.z + radius);
      List<LivingEntity> victims = server.getEntitiesOfClass(LivingEntity.class, box, (e) -> e != this && e.isAlive());

      for (LivingEntity victim : victims) {
         double dx = victim.getX() - centre.x;
         double dz = victim.getZ() - centre.z;
         double dist = Math.sqrt(dx * dx + dz * dz) + 1.0E-4;
         double falloff = Math.max(0.0, 1.0 - dist / radius);
         victim.hurtServer(server, server.damageSources().mobAttack(this), (float)(2.0 + 9.0 * falloff));
         victim.setDeltaMovement(victim.getDeltaMovement().add(dx / dist * 1.3 * falloff, 0.5 + 0.5 * falloff, dz / dist * 1.3 * falloff));
      }
   }

   public void forceTentacleSlam() {
      this.tentacleSlamCooldown = 0;
   }

   public int consumeBlocks(ServerLevel server, int radius) {
      int broken = this.carveSphere(server, this.position(), (double)radius);
      if (broken > 0) {
         this.addSubGrowth(broken / 4 + 1);
         server.playSound((Entity)null, this.getX(), this.getY(), this.getZ(), ModSounds.CLUSTER_FX, SoundSource.HOSTILE, 5.0F, 0.8F);
      }
      return broken;
   }

   private void phaseUpShockwave(ServerLevel server) {
      double radius = this.isDevourer() ? 20.0 : 12.0;
      double rSq = radius * radius;
      for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius), (e) -> e != this && e.isAlive())) {
         double dx = victim.getX() - this.getX();
         double dz = victim.getZ() - this.getZ();
         double d = dx * dx + dz * dz;
         if (d <= rSq && d > 1.0E-4) {
            double dist = Math.sqrt(d);
            double falloff = 1.0 - dist / radius;
            victim.hurtServer(server, server.damageSources().mobAttack(this), (float)(1.0 + 4.0 * falloff));
            victim.setDeltaMovement(victim.getDeltaMovement().add(dx / dist * 1.1 * falloff, 0.35 * falloff, dz / dist * 1.1 * falloff));
         }
      }
   }

   private void raidStructureTick(ServerLevel server) {
      WitherStormWorldConfig cfg = WitherStormConfigs.get(server);
      if (cfg.structureRaid == 0 || this.structureTarget == null || !this.structureArrived) {
         this.raidCooldown = 0;
         return;
      }

      if (this.raidCooldown > 0) {
         --this.raidCooldown;
         return;
      }

      this.raidCooldown = cfg.structureRaidInterval;
      double radius = 3.0 + this.random.nextDouble() * cfg.structureRaidRadius;
      Vec3 centre = new Vec3((double)this.structureTarget.getX() + 0.5, (double)this.structureTarget.getY() + 0.5, (double)this.structureTarget.getZ() + 0.5);
      this.carveSphere(server, centre, radius);
      server.playSound((Entity)null, centre.x, centre.y, centre.z, ModSounds.STORM_THUMP, SoundSource.HOSTILE, 5.0F, 0.8F);

      // rip chunks out of the building as flying clusters so the structure visibly
      // breaks open and its pieces climb to the storm instead of just vanishing
      int tears = Math.min(8, Math.max(0, cfg.structureTearClusters));

      for(int i = 0; i < tears; ++i) {
         BlockPos tear = this.structureTarget.offset(this.random.nextInt(9) - 4, this.random.nextInt(5), this.random.nextInt(9) - 4);
         if (!server.getBlockState(tear).isAir() && !(server.getBlockState(tear).getDestroySpeed(server, tear) < 0.0F)) {
            WitherStormClusterEntity cluster = new WitherStormClusterEntity(ModEntityTypes.WITHER_STORM_CLUSTER, server);
            cluster.setOrigin(tear);
            int clusterRadius = this.random.nextInt(3);
            cluster.setRadius(clusterRadius);
            BlockPos spawnPos = WitherStormClusterEntity.adjustSpawnOrigin(tear, clusterRadius);
            cluster.setPos((double)spawnPos.getX() + (double)0.5F, (double)spawnPos.getY() + (double)0.5F, (double)spawnPos.getZ() + (double)0.5F);
            cluster.absorbBlocks(tear);
            cluster.setTargetStorm(this);
            server.addFreshEntity(cluster);
            WitherStormClusterEntity.syncBlocksToTracking(cluster);
         }
      }
   }

   public static Vec3 headOffset(int index, boolean devourer) {
      Vec3 off = HEAD_OFFSETS[index];
      if (devourer) {
         return index == 1 ? off.add((double)-2.5F, (double)-2.0F, -4.2) : off.add((double)0.0F, (double)1.5F, (double)0.5F);
      } else {
         return off;
      }
   }

   private double growthCeiling() {
      WitherStormWorldConfig config = WitherStormConfigs.get(this.level());
      if (config.infinitePhases != 0) {
         return config.phaseCeiling;
      }
      return this.isDevourer() ? 6.99 : 5.9999;
   }

   public boolean isDevourer() {
      return this.phase >= (double)6.0F;
   }

   public long getCollapseGameTime() {
      return (Long)this.entityData.get(COLLAPSE_GAME_TIME);
   }

   public boolean isPostBombChasing() {
      return this.postBombChaseTicks > 0;
   }

   public float collapseTicks() {
      long start = this.getCollapseGameTime();
      return start < 0L ? -1.0F : (float)(this.level().getGameTime() - start);
   }

   public boolean isCollapsed() {
      return CollapseAnim.active(this.collapseTicks());
   }

   public double behaviourPhase() {
      return this.isDevourer() ? (double)5.0F : this.phase;
   }

   public boolean formidibombed(ServerLevel server) {
      if (!(this.phase < (double)5.0F) && !this.isDevourer()) {
         this.setPhaseExact((double)6.0F);
         this.roarAllHeads(true);
         server.getServer().getPlayerList().getPlayers().forEach((player) -> player.sendSystemMessage(Component.literal("Phase 6+ is very experimental and under heavy development. Bug reports are appreciated, though.").withStyle(ChatFormatting.RED)));
         this.entityData.set(COLLAPSE_GAME_TIME, server.getGameTime());
         this.splitApart(server);
         ModAdvancements.grantNearby(server, this, "strong_grows_weak");
         return true;
      } else {
         return false;
      }
   }

   private void splitApart(ServerLevel server) {
      Vec3 vel = this.getDeltaMovement();
      double fx;
      double fz;
      if (vel.horizontalDistanceSqr() > 1.0E-4) {
         Vec3 dir = (new Vec3(vel.x, (double)0.0F, vel.z)).normalize();
         fx = dir.x;
         fz = dir.z;
      } else {
         double rad = Math.toRadians((double)this.getYRot());
         fx = -Math.sin(rad);
         fz = Math.cos(rad);
      }

      double rx = -fz;
      double rz = fx;

      for(int i = 0; i < 2; ++i) {
         int side = i == 0 ? -1 : 1;
         SeveredWitherStormEntity half = (SeveredWitherStormEntity)ModEntityTypes.SEVERED_WITHER_STORM.create(server, EntitySpawnReason.EVENT);
         if (half != null) {
            half.bindTo(this, side);
            half.setPos(this.getX(), this.getY(), this.getZ());
            half.setYRot(this.getYRot());
            double lateral = 3.1 + this.random.nextDouble() * 0.4;
            double fore = (this.random.nextDouble() - (double)0.5F) * 0.7;
            half.launch(new Vec3(rx * lateral * (double)side + fx * fore, 1.2 + this.random.nextDouble() * 0.4, rz * lateral * (double)side + fz * fore));
            server.addFreshEntity(half);
            int headIndex = i + 1;
            WitherStormHeadEntity head = this.getHead(server, headIndex);
            if (head != null) {
               head.setStormData(half.getUUID(), 0);
               half.adoptHead(head);
            }

            this.headUUIDs[headIndex] = null;
         }
      }

      this.severedHalvesMissing = false;
   }

   private WitherStormPositionPacket.SeveredData[] collectSeveredData(ServerLevel server) {
      if (!this.isDevourer()) {
         return new WitherStormPositionPacket.SeveredData[0];
      } else {
         ArrayList<WitherStormPositionPacket.SeveredData> found = new ArrayList(2);

         for(UUID id : this.severedUUIDs) {
            if (id != null) {
               Entity var8 = server.getEntity(id);
               if (var8 instanceof SeveredWitherStormEntity) {
                  SeveredWitherStormEntity half = (SeveredWitherStormEntity)var8;
                  if (half.isAlive()) {
                     float[] hYaw = new float[3];
                     float[] hPitch = new float[3];
                     half.collectHeadAim(server, hYaw, hPitch);
                     int[] hFire = new int[3];
                     boolean[] hBeam = new boolean[3];
                     double[] hbx = new double[3];
                     double[] hby = new double[3];
                     double[] hbz = new double[3];
                     half.collectHeadState(server, hFire, hBeam, hbx, hby, hbz);
                     found.add(new WitherStormPositionPacket.SeveredData(half.getId(), half.getX(), half.getY(), half.getZ(), half.getYRot(), half.getSide(), half.activeHeadCount(), hYaw, hPitch, hFire, hBeam, hbx, hby, hbz));
                  }
               }
            }
         }

         return (WitherStormPositionPacket.SeveredData[])found.toArray(new WitherStormPositionPacket.SeveredData[0]);
      }
   }

   private void updateSeveredHalves() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel server) {
         if (!this.isDevourer()) {
            this.severedHalvesMissing = true;
            this.severedUUIDs[0] = null;
            this.severedUUIDs[1] = null;
         } else {
            boolean[] present = new boolean[2];
            UUID[] found = new UUID[2];

            for(SeveredWitherStormEntity half : server.getEntitiesOfClass(SeveredWitherStormEntity.class, this.getBoundingBox().inflate((double)232.0F), (h) -> h.isAlive() && this.getUUID().equals(h.getHostUUID()))) {
               int slot = half.isMirrored() ? 0 : 1;
               present[slot] = true;
               found[slot] = half.getUUID();
            }

            for(int i = 0; i < 2; ++i) {
               if (found[i] != null) {
                  this.severedUUIDs[i] = found[i];
                  this.severedGrace[i] = 0;
               } else if (this.severedUUIDs[i] != null && this.severedGrace[i] < 600) {
                  int[] var10000 = this.severedGrace;
                  var10000[i] += 20;
                  present[i] = true;
               } else {
                  this.severedUUIDs[i] = null;
               }
            }

            this.severedHalvesMissing = !present[0] || !present[1];

            for(int i = 0; i < 2; ++i) {
               if (!present[i]) {
                  int side = i == 0 ? -1 : 1;
                  SeveredWitherStormEntity half = (SeveredWitherStormEntity)ModEntityTypes.SEVERED_WITHER_STORM.create(server, EntitySpawnReason.EVENT);
                  if (half != null) {
                     half.bindTo(this, side);
                     double rad = Math.toRadians((double)this.getYRot());
                     double fx = -Math.sin(rad);
                     double fz = Math.cos(rad);
                     double rx = -fz;
                     half.setPos(this.getX() + rx * (double)58.0F * (double)side - fx * (double)18.0F, this.getY(), this.getZ() + fx * (double)58.0F * (double)side - fz * (double)18.0F);
                     half.setYRot(this.getYRot());
                     server.addFreshEntity(half);
                     present[i] = true;
                     this.severedUUIDs[i] = half.getUUID();
                     this.severedGrace[i] = 0;
                  }
               }
            }

            this.severedHalvesMissing = !present[0] || !present[1];
         }
      }
   }

   private boolean clusterPopulationFull() {
      if (--this.clusterCensusAge <= 0) {
         this.clusterCensusAge = 40;
         this.clusterCensus = this.level().getEntitiesOfClass(WitherStormClusterEntity.class, this.getBoundingBox().inflate((double)220.0F)).size();
      }

      return this.clusterCensus >= 90;
   }

   private void scareMobs() {
      Vec3 here = this.position();
      long now = (long)this.tickCount;
      int pathedThisSweep = 0;

      for(PathfinderMob mob : this.level().getEntitiesOfClass(PathfinderMob.class, this.getBoundingBox().inflate((double)64.0F, (double)200.0F, (double)64.0F), (m) -> m.isAlive() && !(m instanceof WitherStormEntity))) {
         if (mob instanceof IronGolem) {
            this.fleeOrdered.remove(mob.getUUID());
         } else if (!WitheredMobs.isWithered(mob) && !WitherSickness.isTurning(mob)) {
            PathNavigation nav = mob.getNavigation();
            long last = this.fleePathedAt.getOrDefault(mob.getId(), Long.MIN_VALUE);
            boolean running = !nav.isDone();
            if ((!running || now - last >= 100L) && pathedThisSweep < 6) {
               Vec3 away = new Vec3(mob.getX() - here.x, (double)0.0F, mob.getZ() - here.z);
               if (away.lengthSqr() < 1.0E-4) {
                  away = new Vec3(this.random.nextDouble() - (double)0.5F, (double)0.0F, this.random.nextDouble() - (double)0.5F);
               }

               away = away.normalize().scale((double)90.0F);
               nav.moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, 1.35);
               this.fleeOrdered.add(mob.getUUID());
               this.fleePathedAt.put(mob.getId(), now);
               ++pathedThisSweep;
            }
         } else if (this.fleeOrdered.remove(mob.getUUID())) {
            this.fleePathedAt.remove(mob.getId());
            mob.getNavigation().stop();
         }
      }

      if (this.fleeOrdered.size() > 512) {
         this.fleeOrdered.clear();
      }

      if (this.fleePathedAt.size() > 512) {
         this.fleePathedAt.clear();
      }

   }

   private void vacuumItems(WitherStormWorldConfig config) {
      double range = this.isPhase4() ? (double)config.pickupRange() * (double)3.0F : (double)72.0F;
      AABB body = this.getBoundingBox();
      Vec3 center = body.getCenter();
      AABB scan = body.inflate(range);
      int sightChecksLeft = 10;

      for(ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, scan)) {
         if (!item.getItem().is(ModItems.WITHER_FRAGMENT)) {
            Vec3 pull = center.subtract(item.position());
            double dist = pull.length();
            Vec3 ip = item.position();
            double bodyDist = (new Vec3(Math.max(body.minX - ip.x, Math.max((double)0.0F, ip.x - body.maxX)), Math.max(body.minY - ip.y, Math.max((double)0.0F, ip.y - body.maxY)), Math.max(body.minZ - ip.z, Math.max((double)0.0F, ip.z - body.maxZ)))).length();
            if (dist < (double)6.0F) {
               this.hookedItems.remove(item.getId());
               this.addSubGrowth(1);
               item.discard();
            } else {
               if (!this.hookedItems.contains(item.getId())) {
                  if (bodyDist > range || this.sightBlockedItems.contains(item.getId()) || sightChecksLeft <= 0) {
                     continue;
                  }

                  --sightChecksLeft;
                  if (!this.canReachItem(item, center)) {
                     this.sightBlockedItems.add(item.getId());
                     continue;
                  }

                  this.hookedItems.add(item.getId());
               }

               item.setDeltaMovement(item.getDeltaMovement().scale(0.7).add(pull.scale((double)1.0F / dist).scale(0.45)));
               item.hurtMarked = true;
            }
         }
      }

      if (this.hookedItems.size() > 512) {
         this.hookedItems.clear();
      }

      if (this.tickCount % 60 == 0) {
         this.sightBlockedItems.clear();
      }

   }

   private boolean canReachItem(ItemEntity item, Vec3 center) {
      BlockHitResult hit = this.level().clip(new ClipContext(item.position().add((double)0.0F, (double)0.25F, (double)0.0F), center, Block.COLLIDER, Fluid.NONE, item));
      return hit.getType() == Type.MISS;
   }

   protected float getSoundVolume() {
      return !this.isPhase4() ? super.getSoundVolume() : (float)Mth.clamp((double)4.0F + (this.phase - (double)4.0F) * (double)3.0F, (double)4.0F, (double)12.0F);
   }

   protected boolean canRide(Entity vehicle) {
      return !this.isPhase4() && super.canRide(vehicle);
   }

   public boolean canBeAffected(MobEffectInstance effect) {
      return this.isPhase4() && effect.getEffect() == MobEffects.WITHER ? false : super.canBeAffected(effect);
   }

   public void aiStep() {
      if (this.level().isClientSide()) {
         super.aiStep();
         if (this.isPhase4()) {
            float syncedYaw = (Float)this.entityData.get(TARGET_YAW);
            float syncedPitch = (Float)this.entityData.get(TARGET_PITCH);
            float smoothedYaw = Mth.rotLerp(0.3F, this.getYRot(), syncedYaw);
            this.setYRot(smoothedYaw);
            this.setYBodyRot(smoothedYaw);
            this.setYHeadRot(smoothedYaw);
            this.setXRot(Mth.rotLerp(0.3F, this.getXRot(), syncedPitch));
         }

      } else if (this.spawnFreezeTicks > 0) {
         this.setDeltaMovement(Vec3.ZERO);
      } else {
         if (this.level() instanceof ServerLevel server) {
            this.tentacleSlamTick(server);
            this.raidStructureTick(server);
         }

         if (!this.isPhase4()) {
            super.aiStep();
            if (!this.isCollapsed()) {
               this.preP4Rotation();
               this.preP4HeadLimits();
            }
         }

      }
   }

   protected void customServerAiStep(ServerLevel level) {
      if (!this.isPhase4() && this.phase < (double)3.0F) {
         super.customServerAiStep(level);
      }

   }

   public void performRangedAttack(LivingEntity target, float power) {
      if (!this.isPhase4() && this.phase < (double)3.0F) {
         super.performRangedAttack(target, power);
      }

   }

   public static AttributeSupplier.Builder createAttributes() {
      return WitherBoss.createAttributes().add(Attributes.MAX_HEALTH, (double)500.0F).add(Attributes.ATTACK_DAMAGE, (double)15.0F).add(Attributes.FOLLOW_RANGE, (double)64.0F);
   }

   private static int growthRequirement(int mainPhase, WitherStormWorldConfig config) {
      double mod = config.phaseRequirementModifier * (mainPhase < 4 ? (double)5.5F : (double)12.0F);
      int var10000;
      switch (mainPhase) {
         case 0 -> var10000 = (int)((double)25.0F * mod);
         case 1 -> var10000 = (int)((double)50.0F * mod);
         case 2 -> var10000 = (int)((double)100.0F * mod);
         case 3 -> var10000 = (int)((double)200.0F * mod);
         case 4 -> var10000 = (int)((double)config.phase4Requirement * mod);
         default -> var10000 = (int)((double)config.phase5Requirement * mod);
      }

      return var10000;
   }

   public void addSubGrowth(int amount) {
      if (!(this.phase >= this.growthCeiling() - 0.001)) {
         WitherStormWorldConfig config = WitherStormConfigs.get(this.level());
         if (config.fastGrowthToSixOne != 0 && this.phase >= (double)6.0F && this.phase < 6.1) {
            amount = Math.max(1, (int)Math.round((double)(amount * config.fastGrowthToSixOneSpeed) / (double)100.0F));
         }

         if (config.instantGrowth != 0) {
            amount = Math.max(1, (int)Math.round((double)amount * config.instantGrowthRate));
         }

         this.subGrowth += amount;
         this.entityData.set(SUBGROWTH_DATA, this.subGrowth);
         int mainPhase = (int)Math.floor(this.phase);
         int requirement = growthRequirement(mainPhase, config);
         double progress = (double)this.subGrowth / (double)requirement;
         double phaseBefore = this.phase;
         this.phase = (double)mainPhase + Math.min(progress, 0.99);
         if (this.phase < (double)4.0F) {
            Level var10 = this.level();
            if (var10 instanceof ServerLevel) {
               ServerLevel growLevel = (ServerLevel)var10;
               if (Math.floor(this.phase * (double)10.0F) > Math.floor(phaseBefore * (double)10.0F)) {
                  growLevel.playSound((Entity)null, this.getX(), this.getY(), this.getZ(), ModSounds.STORM_GROW, SoundSource.HOSTILE, 7.0F, 0.95F + this.random.nextFloat() * 0.1F);
               }
            }
         }

         if (this.subGrowth >= requirement) {
            if ((double)mainPhase + (double)1.0F >= this.growthCeiling()) {
               this.phase = this.growthCeiling();
               this.subGrowth = 0;
               this.entityData.set(SUBGROWTH_DATA, 0);
               this.entityData.set(PHASE_DATA, (float)this.phase);
               this.entityData.set(PHASE4_DATA, true);
               this.updatePhase5Stamp(this.phase);
               return;
            }

            this.phase = (double)mainPhase + (double)1.0F;
            this.roarAllHeads(true);
            if (this.phase >= (double)4.0F && !this.phase4) {
               this.enterPhase4();
            }

            if (this.level() instanceof ServerLevel phaseServer) {
               this.phaseUpShockwave(phaseServer);
            }

            this.subGrowth = 0;
            this.entityData.set(SUBGROWTH_DATA, 0);
         }

         this.entityData.set(PHASE_DATA, (float)this.phase);
         this.entityData.set(PHASE4_DATA, this.phase >= (double)4.0F);
         this.updatePhase5Stamp(this.phase);
      }
   }

   public void spawnScavengedCluster(Entity collector) {
      Level var3 = this.level();
      if (var3 instanceof ServerLevel server) {
         BlockPos target = this.findSurfaceBlockNear(collector.blockPosition());
         if (target != null) {
            WitherStormClusterEntity cluster = new WitherStormClusterEntity(ModEntityTypes.WITHER_STORM_CLUSTER, this.level());
            cluster.setOrigin(target);
            int radius = this.random.nextInt(2);
            cluster.setRadius(radius);
            BlockPos spawnPos = WitherStormClusterEntity.adjustSpawnOrigin(target, radius);
            cluster.setPos((double)spawnPos.getX() + (double)0.5F, (double)spawnPos.getY(), (double)spawnPos.getZ() + (double)0.5F);
            cluster.absorbBlocks(target);
            cluster.setTargetStorm(this);
            cluster.setScavengedBy(collector);
            server.addFreshEntity(cluster);
         }
      }
   }

   private BlockPos findSurfaceBlockNear(BlockPos around) {
      Level var3 = this.level();
      if (var3 instanceof ServerLevel server) {
         for(int var9 = 0; var9 < 8; ++var9) {
            int x = around.getX() + this.random.nextInt(25) - 12;
            int z = around.getZ() + this.random.nextInt(25) - 12;
            int y = server.getHeight(Types.MOTION_BLOCKING, x, z) - 1;
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = server.getBlockState(pos);
            if (!state.isAir() && !(state.getDestroySpeed(server, pos) < 0.0F) && state.getFluidState().isEmpty()) {
               return pos;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void spawnBlockCluster() {
      BlockPos target = this.findSurfaceBlock();
      if (target != null) {
         WitherStormClusterEntity cluster = new WitherStormClusterEntity(ModEntityTypes.WITHER_STORM_CLUSTER, this.level());
         cluster.setOrigin(target);
         int mainPhase = (int)Math.floor(this.phase);
         int maxRadius = WitherStormConfigs.get(this.level()).maxClusterRadiusFor(this.phase);
         int radius = maxRadius <= 0 ? 0 : this.random.nextInt(maxRadius + 1);
         cluster.setRadius(radius);
         BlockPos spawnPos = WitherStormClusterEntity.adjustSpawnOrigin(target, radius);
         cluster.setPos((double)spawnPos.getX() + (double)0.5F, (double)spawnPos.getY() + (double)0.5F, (double)spawnPos.getZ() + (double)0.5F);
         cluster.absorbBlocks(target);
         cluster.setTargetStorm(this);
         this.level().addFreshEntity(cluster);
         WitherStormClusterEntity.syncBlocksToTracking(cluster);
      }
   }

   private void spawnCocoonCluster() {
      BlockPos target = this.findSurfaceBlock();
      if (target != null) {
         WitherStormClusterEntity cluster = new WitherStormClusterEntity(ModEntityTypes.WITHER_STORM_CLUSTER, this.level());
         cluster.setOrigin(target);
         cluster.setRadius(0);
         BlockPos spawnPos = WitherStormClusterEntity.adjustSpawnOrigin(target, 0);
         cluster.setPos((double)spawnPos.getX() + (double)0.5F, (double)spawnPos.getY() + (double)0.5F, (double)spawnPos.getZ() + (double)0.5F);
         cluster.absorbBlocks(target);
         cluster.setTargetStorm(this);
         this.level().addFreshEntity(cluster);
         WitherStormClusterEntity.syncBlocksToTracking(cluster);
      }
   }

   public double getPhase() {
      return this.phase;
   }

   public void setPhase(double value) {
      boolean wasPhase4 = this.phase4;
      this.phase = value;
      if (value >= (double)4.0F) {
         this.phase4 = true;
         if (!wasPhase4) {
            this.enterPhase4();
         }
      } else {
         this.phase4 = false;
         if (wasPhase4) {
            this.resetToNormalWither();
         }
      }

      this.entityData.set(PHASE_DATA, (float)value);
      this.entityData.set(PHASE4_DATA, this.phase4);
      this.updatePhase5Stamp(value);
   }

   private long elapsedSince(long stamp) {
      return stamp < 0L ? -1L : Math.max(0L, this.level().getGameTime() - stamp);
   }

   private void restoreAnimStamp(EntityDataAccessor<Long> key, long savedElapsed) {
      this.entityData.set(key, savedElapsed < 0L ? -1L : this.level().getGameTime() - savedElapsed);
   }

   private void updatePhase5Stamp(double newPhase) {
      if (!this.level().isClientSide()) {
         long stamp = (Long)this.entityData.get(PHASE5_ANIM_GAME_TIME);
         if (newPhase >= (double)5.0F) {
            if (stamp < 0L) {
               this.entityData.set(PHASE5_ANIM_GAME_TIME, this.level().getGameTime());
            }
         } else if (stamp >= 0L) {
            this.entityData.set(PHASE5_ANIM_GAME_TIME, -1L);
         }

         long miniStamp = (Long)this.entityData.get(MINI_HEAD_ANIM_GAME_TIME);
         if (newPhase >= (double)2.0F) {
            if (miniStamp < 0L) {
               this.entityData.set(MINI_HEAD_ANIM_GAME_TIME, this.level().getGameTime());
               this.miniHeadSkullFired = false;
               this.miniHeadBitPlayed = false;
            }
         } else if (miniStamp >= 0L) {
            this.entityData.set(MINI_HEAD_ANIM_GAME_TIME, -1L);
            this.miniHeadSkullFired = false;
            this.miniHeadBitPlayed = false;
         }

         long tentacleStamp = (Long)this.entityData.get(TENTACLE_ANIM_GAME_TIME);
         if (newPhase >= (double)3.0F) {
            if (tentacleStamp < 0L) {
               this.entityData.set(TENTACLE_ANIM_GAME_TIME, this.level().getGameTime());
            }
         } else if (tentacleStamp >= 0L) {
            this.entityData.set(TENTACLE_ANIM_GAME_TIME, -1L);
         }

         long stamp58 = (Long)this.entityData.get(PHASE58_ANIM_GAME_TIME);
         if (newPhase >= 5.8) {
            if (stamp58 < 0L) {
               this.entityData.set(PHASE58_ANIM_GAME_TIME, this.level().getGameTime());
            }
         } else if (stamp58 >= 0L) {
            this.entityData.set(PHASE58_ANIM_GAME_TIME, -1L);
         }

      }
   }

   public long getFrontTentacleAnimGameTime() {
      return (Long)this.entityData.get(TENTACLE_ANIM_GAME_TIME);
   }

   public long getMiniHeadAnimGameTime() {
      return (Long)this.entityData.get(MINI_HEAD_ANIM_GAME_TIME);
   }

   private void tickMiniHeadEmergence() {
      if (!this.miniHeadSkullFired) {
         Level var2 = this.level();
         if (var2 instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)var2;
            long stamp = this.getMiniHeadAnimGameTime();
            if (stamp < 0L) {
               return;
            }

            long since = server.getGameTime() - stamp;
            if (!this.miniHeadBitPlayed && since >= 10L) {
               this.miniHeadBitPlayed = true;
               if (this.headUUIDs[0] != null) {
                  Entity var7 = server.getEntity(this.headUUIDs[0]);
                  if (var7 instanceof WitherStormHeadEntity) {
                     WitherStormHeadEntity h = (WitherStormHeadEntity)var7;
                     h.playBiteAnimation();
                  }
               }
            }

            if (since < 66L) {
               return;
            }

            Vec3 dir;
            Vec3 var10000;
            label39: {
               this.miniHeadSkullFired = true;
               double rad = Math.toRadians((double)this.getYRot());
               dir = new Vec3(-Math.sin(rad), (double)0.0F, Math.cos(rad));
               if (this.headUUIDs[0] != null) {
                  Entity var11 = server.getEntity(this.headUUIDs[0]);
                  if (var11 instanceof WitherStormHeadEntity) {
                     WitherStormHeadEntity h = (WitherStormHeadEntity)var11;
                     var10000 = h.position();
                     break label39;
                  }
               }

               var10000 = this.position().add((double)0.0F, 3.05, (double)0.0F);
            }

            Vec3 mouth;
            label34: {
               Vec3 from = var10000;
               mouth = from.add(dir.scale((double)3.0F));
               if (this.headUUIDs[0] != null) {
                  Entity var12 = server.getEntity(this.headUUIDs[0]);
                  if (var12 instanceof WitherStormHeadEntity) {
                     WitherStormHeadEntity firing = (WitherStormHeadEntity)var12;
                     firing.playFireAnimation();
                     break label34;
                  }
               }

               this.playSound(ModSounds.HEAD_SHOOT, this.voiceVolume(1.0F), 1.0F);
            }

            SuperSkullEntity skull = new SuperSkullEntity(ModEntityTypes.SUPER_SKULL, server);
            skull.setPos(mouth.x, mouth.y, mouth.z);
            skull.shoot(dir.add((double)0.0F, -0.05, (double)0.0F).normalize().scale(0.9));
            server.addFreshEntity(skull);
            return;
         }
      }

   }

   public long getPhase5AnimGameTime() {
      return (Long)this.entityData.get(PHASE5_ANIM_GAME_TIME);
   }

   public long getPhase58AnimGameTime() {
      return (Long)this.entityData.get(PHASE58_ANIM_GAME_TIME);
   }

   private static int elapsedTicksSince(long stamp, long gameTime) {
      return stamp < 0L ? -1 : (int)Math.min(2147483647L, Math.max(0L, gameTime - stamp));
   }

   public int getSubGrowth() {
      return this.subGrowth;
   }

   public void setPhaseExact(double value) {
      this.setPhase(value);
      int mainPhase = (int)Math.floor(value);
      double fraction = Mth.clamp(value - (double)mainPhase, (double)0.0F, 0.99);
      WitherStormWorldConfig config = WitherStormConfigs.get(this.level());
      int requirement = growthRequirement(mainPhase, config);
      this.subGrowth = (int)Math.round(fraction * (double)requirement);
      this.entityData.set(SUBGROWTH_DATA, this.subGrowth);
      this.phase = (double)mainPhase + fraction;
      this.entityData.set(PHASE_DATA, (float)this.phase);
   }

   public boolean isPhase4() {
      return (Boolean)this.entityData.get(PHASE4_DATA);
   }

   public boolean isPushable() {
      return !this.isPhase4() && super.isPushable();
   }

   public void push(double x, double y, double z) {
      if (!this.isPhase4()) {
         super.push(x, y, z);
      }
   }

   public void push(Entity other) {
      if (!this.isPhase4()) {
         super.push(other);
      }
   }

   private void resetToNormalWither() {
      this.bodyRoll = 0.0F;
      this.bodyRollVel = 0.0F;
      this.bodyYawVel = 0.0F;
      this.bodyPitchVel = 0.0F;
      this.recoilYaw = 0.0F;
      this.recoilYawVel = 0.0F;
      this.filteredTargetYaw = Float.NaN;
      this.moveMode = WitherStormEntity.MoveMode.FOLLOW;
      this.chaseTimer = -1;
      this.distractionTimer = -1;
      this.distractionTicksLeft = 0;
      this.setXRot(0.0F);
      this.entityData.set(BODY_ROLL, 0.0F);
      this.entityData.set(TARGET_PITCH, 0.0F);
      this.entityData.set(SPAWN_ANIM_GAME_TIME, -1L);
      this.setInvulnerable(false);
      this.refreshDimensions();
      this.updateBossBar();
   }

   private void phase4Movement() {
      WitherStormWorldConfig cfg = WitherStormConfigs.get(this.level());
      if (--this.phase4TargetUpdateTimer <= 0) {
         this.cachedMoveGoal = this.resolveMoveGoal(cfg);
         this.phase4TargetUpdateTimer = 20;
      }

      this.cachedMoveGoal = this.applyOrbit(cfg, this.cachedMoveGoal);
      if (this.cachedMoveGoal == null) {
         this.smoothedMoveGoal = null;
      } else if (this.smoothedMoveGoal == null) {
         this.smoothedMoveGoal = this.cachedMoveGoal;
      } else {
         this.smoothedMoveGoal = this.smoothedMoveGoal.lerp(this.cachedMoveGoal, 0.06);
      }

      if (--this.nearestPlayerUpdateTimer <= 0) {
         this.cachedNearestPlayer = this.nearestTargetable();
         this.cachedFaceEntity = this.resolveFaceEntity(cfg);
         this.nearestPlayerUpdateTimer = 20;
      }

      Vec3 goal = this.smoothedMoveGoal;
      if (--this.phase4HeightTimer <= 0) {
         this.phase4HeightTimer = 600;
         this.phase4Height = 25 + this.random.nextInt(26);
      }

      this.tickMoveMode(cfg, goal);
      double desiredY = this.highestGroundAround() + (double)cfg.phase4Altitude;
      if (this.probePortal != null) {
         goal = new Vec3((double)this.probePortal.getX() + (double)0.5F, (double)this.probePortal.getY(), (double)this.probePortal.getZ() + (double)0.5F);
         double pdx = goal.x - this.getX();
         double pdz = goal.z - this.getZ();
         double horiz = Math.sqrt(pdx * pdx + pdz * pdz);
         if (horiz < (double)30.0F) {
            double t = (double)1.0F - horiz / (double)30.0F;
            desiredY = Mth.lerp(t * t, desiredY, (double)this.probePortal.getY() + (double)20.0F);
         }
      }

      double vyGain = 0.05;
      double vyClamp = 0.1;
      if (this.phase >= (double)5.0F) {
         double grabY = this.tickTentacleGrab(cfg, desiredY);
         if (this.snatchVictim != null || grabY < desiredY - 0.01) {
            vyGain = 0.15;
            vyClamp = (double)0.75F;
         }

         desiredY = grabY;
      } else {
         if (this.snatchVictim != null) {
            this.endSnatch((ServerLevel)this.level(), true);
         }

         this.despawnGrabTentacle();
      }

      double climbNeed = desiredY - this.getY();
      if (climbNeed > (double)3.0F) {
         double urgency = Math.min((double)1.0F, (climbNeed - (double)3.0F) / (double)18.0F);
         vyGain = Mth.lerp(urgency, vyGain, 0.3);
         vyClamp = Math.max(vyClamp, Mth.lerp(urgency, vyClamp, 0.9));
      }

      double vy = Mth.clamp((desiredY - this.getY()) * vyGain, -vyClamp, vyClamp);
      Vec3 velocity = new Vec3((double)0.0F, vy, (double)0.0F);
      Vec3 faceOverride = null;
      if (this.moveMode == WitherStormEntity.MoveMode.DISTRACTED) {
         double dx = this.distractX - this.getX();
         double dz = this.distractZ - this.getZ();
         double horizDist = Math.sqrt(dx * dx + dz * dz);
         if (horizDist > (double)4.0F) {
            Vec3 horiz = (new Vec3(dx, (double)0.0F, dz)).normalize().scale(cfg.stormSpeed * this.siegeSpeedScale(cfg));
            velocity = new Vec3(horiz.x, vy, horiz.z);
         }

         faceOverride = new Vec3(this.distractX, this.getY(), this.distractZ);
      } else if (this.moveMode == WitherStormEntity.MoveMode.CHASING && goal != null) {
         double reach = chaseReach(cfg);
         double dx = goal.x - this.getX();
         double dz = goal.z - this.getZ();
         double horizDist = Math.sqrt(dx * dx + dz * dz);
         if (horizDist > reach) {
            double t = Mth.clamp((horizDist - reach) / reach, 0.15, (double)1.0F);
            double chase = cfg.chaseSpeed * this.siegeSpeedScale(cfg);
            if (this.isPostBombChasing()) {
               chase *= (double)cfg.postFormidibombChaseSpeed / (double)100.0F;
            }

            Vec3 horiz = (new Vec3(dx, (double)0.0F, dz)).normalize().scale(chase * t);
            velocity = new Vec3(horiz.x, vy, horiz.z);
         }
      } else if (goal != null) {
         boolean closeIn = cfg.targetingMode == 1 || this.probePortal != null;
         double standoffMin = closeIn ? (double)4.0F : cfg.stormStandoff;
         double standoffMax = closeIn ? (double)14.0F : cfg.stormStandoff * (double)2.0F;
         double dx = goal.x - this.getX();
         double dz = goal.z - this.getZ();
         double horizDist = Math.sqrt(dx * dx + dz * dz);
         if (horizDist > standoffMin) {
            double t = Mth.clamp((horizDist - standoffMin) / (standoffMax - standoffMin), (double)0.0F, (double)1.0F);
            Vec3 horiz = (new Vec3(dx, (double)0.0F, dz)).normalize().scale(cfg.stormSpeed * t * this.siegeSpeedScale(cfg));
            velocity = new Vec3(horiz.x, vy, horiz.z);
         }
      }

      Vec3 windDrift = Vec3.ZERO;
      Vec3 startleWind = Vec3.ZERO;
      boolean startled = false;
      if (this.behaviourPhase() >= 5.8 && cfg.phase58DriftStrength > (double)0.0F) {
         float t = (float)this.tickCount;
         double s = cfg.phase58DriftStrength * 0.07;
         double dxDrift = (double)(Mth.sin((double)(t * 0.171F)) + 0.7F * Mth.sin((double)(t * 0.083F + 1.3F)) + 0.5F * Mth.sin((double)(t * 0.037F + 2.7F))) * s;
         double dyDrift = (double)(Mth.sin((double)(t * 0.149F + 2.1F)) + 0.7F * Mth.sin((double)(t * 0.067F + 0.4F)) + 0.5F * Mth.sin((double)(t * 0.029F + 1.8F))) * s * 0.22;
         double dzDrift = (double)(Mth.cos((double)(t * 0.163F + 0.7F)) + 0.7F * Mth.cos((double)(t * 0.079F + 2.6F)) + 0.5F * Mth.cos((double)(t * 0.031F + 0.9F))) * s * 0.3;
         windDrift = new Vec3(dxDrift, dyDrift, dzDrift);
      }

      if (this.grappleStartleTicks > 0) {
         --this.grappleStartleTicks;
         startled = true;
         float t = (float)this.tickCount;
         double env = (double)this.grappleStartleTicks / (double)90.0F;
         double v = 0.45 * (0.35 + 0.65 * env);
         double gy = ((double)Mth.sin((double)(t * 0.9F)) + (double)0.5F * (double)Mth.sin((double)(t * 0.53F + 1.1F))) * v;
         double gx = (double)Mth.sin((double)(t * 0.31F + 0.7F)) * v * 0.08;
         double gz = (double)Mth.cos((double)(t * 0.29F + 2.1F)) * v * 0.08;
         startleWind = new Vec3(gx, gy, gz);
      }

      this.windOffset = this.windOffset.add(windDrift).scale(0.995);
      Vec3 prev = this.lastTravelVel;
      Vec3 travel = new Vec3(Mth.lerp(0.12, prev.x, velocity.x), velocity.y, Mth.lerp(0.12, prev.z, velocity.z));
      this.lastTravelVel = travel;
      this.setDeltaMovement(travel.add(windDrift).add(startleWind));
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.needsSync = true;
      if (startled) {
         this.idleFaceYaw = this.startleYaw;
         this.filteredTargetYaw = this.startleYaw;
         this.bodyYawVel = 0.0F;
         this.bodyPitchVel = 0.0F;
         this.bodyRoll = 0.0F;
         this.setYRot(this.startleYaw);
         this.setYBodyRot(this.startleYaw);
         this.setYHeadRot(this.startleYaw);
         this.entityData.set(TARGET_YAW, this.startleYaw);
         this.entityData.set(TARGET_PITCH, this.getXRot());
         this.entityData.set(BODY_ROLL, 0.0F);
      } else {
         Vec3 facePoint = faceOverride;
         if (faceOverride == null && this.cachedFaceEntity != null && this.cachedFaceEntity.isAlive() && !this.cachedFaceEntity.isRemoved()) {
            label151: {
               LivingEntity driftPitch = this.cachedFaceEntity;
               if (driftPitch instanceof Player) {
                  Player fp = (Player)driftPitch;
                  if (fp.isSpectator()) {
                     break label151;
                  }
               }

               facePoint = this.aimAt(this.cachedFaceEntity.position());
            }
         }

         if (facePoint == null && cfg.targetingMode == 4 && this.structureTarget != null) {
            facePoint = this.aimAt(Vec3.atCenterOf(this.structureTarget));
         }

         if (facePoint == null && this.probePortal != null) {
            facePoint = new Vec3((double)this.probePortal.getX() + (double)0.5F, this.getY(), (double)this.probePortal.getZ() + (double)0.5F);
         }

         if (this.rotationLockTicks > 0) {
            --this.rotationLockTicks;
            if (this.rotationLock != null && this.rotationLock.isAlive()) {
               facePoint = this.rotationLock.position();
            }
         } else {
            this.rotationLock = null;
         }

         if (facePoint == null && this.effectiveNatural(cfg)) {
            Vec3 natural = this.naturalFacePoint();
            if (natural != null) {
               facePoint = this.aimAt(natural);
            }
         }

         if (facePoint == null) {
            if (Float.isNaN(this.idleFaceYaw)) {
               this.idleFaceYaw = this.getYRot();
            }

            this.idleFaceYaw += 0.22F;
            float driftYaw = this.getYRot() + Mth.degreesDifference(this.getYRot(), this.idleFaceYaw) * 0.02F;
            this.bodyRoll += (0.0F - this.bodyRoll) * 0.05F;
            float driftPitch = this.getXRot() + (0.0F - this.getXRot()) * 0.05F;
            this.setYRot(driftYaw);
            this.setXRot(driftPitch);
            this.setYBodyRot(driftYaw);
            this.setYHeadRot(driftYaw);
            this.entityData.set(TARGET_YAW, driftYaw);
            this.entityData.set(TARGET_PITCH, driftPitch);
            this.entityData.set(BODY_ROLL, this.bodyRoll + this.idleSway());
         } else {
            Vec3 aimFrom = this.position().subtract(this.windOffset);
            Vec3 direction = facePoint.subtract(aimFrom).normalize();
            float targetYaw = (float)(Mth.atan2(direction.z, direction.x) * (180D / Math.PI)) - 90.0F;
            this.idleFaceYaw = targetYaw;
            if (Float.isNaN(this.filteredTargetYaw)) {
               this.filteredTargetYaw = targetYaw;
            }

            float aimErr = Mth.degreesDifference(this.filteredTargetYaw, targetYaw);
            float aimSlewCap = (float)(2.6 * Mth.clamp(this.behaviourPhase() >= 5.8 ? cfg.phase58TurnSpeed : (this.phase >= (double)5.0F ? cfg.phase5TurnSpeed : cfg.phase4TurnSpeed), (double)0.25F, (double)4.0F));
            float aimStep = Mth.clamp(aimErr * 0.25F, -aimSlewCap, aimSlewCap);
            this.filteredTargetYaw = Mth.wrapDegrees(this.filteredTargetYaw + aimStep);
            targetYaw = this.filteredTargetYaw;
            double horizSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            this.leanSmoothed += (float)((Mth.clamp(horizSpeed * (double)60.0F, (double)0.0F, (double)7.0F) - (double)this.leanSmoothed) * 0.04);
            float targetPitch = Mth.clamp(3.0F + this.leanSmoothed, 0.0F, 10.0F);
            double turnSpeed = this.behaviourPhase() >= 5.8 ? cfg.phase58TurnSpeed : (this.phase >= (double)5.0F ? cfg.phase5TurnSpeed : cfg.phase4TurnSpeed);
            float smoothTime = (float)((double)1.8F / Math.max(turnSpeed, 0.05));
            float[] yawBox = new float[]{this.bodyYawVel};
            float newYaw = this.smoothDampAngle(this.getYRot(), targetYaw, yawBox, smoothTime);
            this.bodyYawVel = yawBox[0];
            float[] pitchBox = new float[]{this.bodyPitchVel};
            float newPitch = this.smoothDampAngle(this.getXRot(), targetPitch, pitchBox, smoothTime * 2.5F);
            this.bodyPitchVel = pitchBox[0];
            float targetRoll = Mth.clamp(this.bodyYawVel * 0.6F, -30.0F, 30.0F);
            float[] rollBox = new float[]{this.bodyRollVel};
            this.bodyRoll = this.smoothDampAngle(this.bodyRoll, targetRoll, rollBox, 1.2F);
            this.bodyRollVel = rollBox[0];
            this.recoilYawVel += -this.recoilYaw * 0.04F;
            this.recoilYawVel *= 0.9F;
            this.recoilYaw += this.recoilYawVel;
            float displayYaw = newYaw + this.recoilYaw;
            float maxStep = (float)((double)7.0F * Mth.clamp(turnSpeed, 0.05, (double)4.0F));
            float yawStep = Mth.clamp(Mth.degreesDifference(this.getYRot(), displayYaw), -maxStep, maxStep);
            displayYaw = Mth.wrapDegrees(this.getYRot() + yawStep);
            this.setYRot(displayYaw);
            this.setXRot(newPitch);
            this.setYBodyRot(displayYaw);
            this.setYHeadRot(displayYaw);
            this.entityData.set(TARGET_YAW, displayYaw);
            this.entityData.set(TARGET_PITCH, newPitch);
            this.entityData.set(BODY_ROLL, this.bodyRoll + this.idleSway());
         }
      }
   }

   private Vec3 applyOrbit(WitherStormWorldConfig cfg, Vec3 goal) {
      if (goal != null && cfg.orbitStationaryTargets != 0) {
         if (this.naturalPrey != null && this.isValidPrey(this.naturalPrey)) {
            this.goalStillTicks = 0;
            return goal;
         } else if (this.lastGoalSeen != null && !(this.lastGoalSeen.distanceTo(goal) > (double)14.0F)) {
            ++this.goalStillTicks;
            if (this.goalStillTicks < 240) {
               return goal;
            } else {
               this.orbitAngle = Mth.wrapDegrees(this.orbitAngle + 0.32F);
               double rad = Math.toRadians((double)this.orbitAngle);
               double r = Math.max(cfg.stormStandoff, (double)24.0F);
               return goal.add(Math.cos(rad) * r, (double)0.0F, Math.sin(rad) * r);
            }
         } else {
            this.lastGoalSeen = goal;
            this.goalStillTicks = 0;
            return goal;
         }
      } else {
         this.goalStillTicks = 0;
         return goal;
      }
   }

   private void tickMoveMode(WitherStormWorldConfig cfg, Vec3 goal) {
      switch (this.moveMode.ordinal()) {
         case 0:
            if (this.chaseTimer < 0) {
               this.chaseTimer = cfg.chaseIntervalMinutes * 60 * 20;
            }

            if (--this.chaseTimer <= 0) {
               this.startChasing();
            }
            break;
         case 1:
            if (goal == null) {
               this.chaseGoalWasPresent = false;
               return;
            }

            if (!this.chaseGoalWasPresent) {
               this.chaseGoalWasPresent = true;
               this.chaseElapsed = 0;
            }

            ++this.chaseElapsed;
            if (this.isPostBombChasing()) {
               return;
            }

            if (this.chaseElapsed >= 300) {
               double reach = chaseReach(cfg);
               double dx = goal.x - this.getX();
               double dz = goal.z - this.getZ();
               if (dx * dx + dz * dz < reach * reach) {
                  this.setChasing(false);
                  return;
               }
            }

            if (this.distractionTimer < 0) {
               this.distractionTimer = cfg.distractionIntervalMinutes * 60 * 20;
            }

            if (--this.distractionTimer <= 0) {
               this.beginDistraction(cfg);
            }
            break;
         case 2:
            double dx = this.distractX - this.getX();
            double dz = this.distractZ - this.getZ();
            if (dx * dx + dz * dz < (double)64.0F) {
               this.pickDistractionPoint(cfg);
            }

            if (--this.distractionTicksLeft <= 0) {
               this.moveMode = WitherStormEntity.MoveMode.FOLLOW;
               this.chaseTimer = cfg.chaseIntervalMinutes * 60 * 20;
            }
      }

   }

   private void tickCollapseFall() {
      this.setDeltaMovement(Vec3.ZERO);
      double restY = this.highestGroundAround() + (double)-1.5F;
      float t = this.collapseTicks();
      if (t < 75.0F) {
         double drop = Math.min(this.getY() - restY, 0.06 * (double)t + 0.35);
         double slide = (double)16.0F * (double)(CollapseAnim.down(t) - CollapseAnim.down(t - 1.0F));
         double rad = Math.toRadians((double)this.getYRot());
         double backX = Math.sin(rad);
         double backZ = -Math.cos(rad);
         this.setPos(this.getX() + backX * slide, this.getY() - Math.max(drop, (double)0.0F), this.getZ() + backZ * slide);
      } else if (t >= 1000.0F) {
         double flyY = this.highestGroundAround() + (double)WitherStormConfigs.get(this.level()).phase4Altitude;
         double r = (double)Mth.clamp((t - 1000.0F) / 170.0F, 0.0F, 1.0F);
         double eased = r * r * ((double)3.0F - (double)2.0F * r);
         this.setPos(this.getX(), Mth.lerp(eased, restY, flyY), this.getZ());
      } else {
         this.setPos(this.getX(), restY, this.getZ());
      }

   }

   private double highestGroundAround() {
      if (--this.groundScanTimer > 0 && !Double.isNaN(this.cachedGroundY)) {
         return this.cachedGroundY;
      } else {
         this.groundScanTimer = 10;
         BlockPos here = this.blockPosition();
         int best = Integer.MIN_VALUE;
         boolean overWater = false;

         for(int dx = -12; dx <= 12; dx += 4) {
            for(int dz = -12; dz <= 12; dz += 4) {
               BlockPos at = here.offset(dx, 0, dz);
               if (this.level().hasChunkAt(at)) {
                  int y = this.level().getHeightmapPos(Types.MOTION_BLOCKING, at).getY();
                  if (y > best) {
                     best = y;
                     overWater = !this.level().getBlockState(at.atY(y - 1)).getFluidState().isEmpty();
                  }
               }
            }
         }

         if (best == Integer.MIN_VALUE) {
            best = this.level().getHeightmapPos(Types.MOTION_BLOCKING, here).getY();
         }

         this.cachedGroundY = overWater ? (double)(best + 9) : (double)best;
         return this.cachedGroundY;
      }
   }

   public int getSiegeStage() {
      return (Integer)this.entityData.get(SIEGE_STAGE);
   }

   public int siegeProgress() {
      if (this.getSiegeStage() == 3) {
         Level var2 = this.level();
         if (var2 instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)var2;
            int var3 = Math.max(1, WitherStormConfigs.get(sl).endermanSiegeSeconds * 20);
            return (int)Mth.clamp((long)this.siegeTicks * 100L / (long)var3, 0L, 100L);
         }
      }

      return 0;
   }

   public boolean isUnderSiege() {
      return this.getSiegeStage() == 3;
   }

   private double siegeSpeedScale(WitherStormWorldConfig cfg) {
      return this.isUnderSiege() ? (double)cfg.endermanSiegeSlowdown / (double)100.0F : (double)1.0F;
   }

   public boolean headsDistressed() {
      return this.isUnderSiege();
   }

   private void tickSiege(ServerLevel server, WitherStormWorldConfig cfg) {
      if (cfg.endermanSiege != 0) {
         if (this.siegeStage != 4) {
            if (this.siegeStage == 0) {
               if (this.isDevourer() && this.phase >= 6.1 && this.activeHeadCount() >= 2) {
                  this.siegeStage = 1;
                  this.siegeTicks = 0;
               }

               this.syncSiege();
            } else {
               ++this.siegeTicks;
               switch (this.siegeStage) {
                  case 1:
                     if (this.siegeTicks >= 1210) {
                        this.siegeStage = 2;
                        this.siegeTicks = 0;
                     }
                     break;
                  case 2:
                     if (this.siegeTicks >= 1090) {
                        this.siegeStage = 3;
                        this.siegeTicks = 0;
                        this.siegeSpawned = 0;
                     }
                     break;
                  case 3:
                     this.spawnSiegeEndermen(server, cfg);
                     if (cfg.endermanSiegeBeamEats != 0) {
                        this.eatEndermenInBeams(server);
                     }

                     if (this.siegeTicks >= cfg.endermanSiegeSeconds * 20) {
                        this.siegeStage = 4;
                        this.siegeTicks = 0;
                     }
               }

               this.syncSiege();
            }
         }
      }
   }

   private void syncSiege() {
      if ((Integer)this.entityData.get(SIEGE_STAGE) != this.siegeStage) {
         this.entityData.set(SIEGE_STAGE, this.siegeStage);
      }

   }

   private void spawnSiegeEndermen(ServerLevel server, WitherStormWorldConfig cfg) {
      if (this.siegeSpawned < cfg.endermanSiegeCount) {
         if (this.siegeTicks % 6 == 0) {
            double rad = Math.toRadians((double)this.getYRot());
            double fx = -Math.sin(rad);
            double fz = Math.cos(rad);
            double spread = Math.toRadians((double)70.0F);
            double a = (this.random.nextDouble() - (double)0.5F) * spread;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            double dx = fx * cos - fz * sin;
            double dz = fx * sin + fz * cos;
            double dist = (double)cfg.endermanSiegeDistance * ((double)0.75F + this.random.nextDouble() * (double)0.5F);
            double x = this.getX() + dx * dist;
            double z = this.getZ() + dz * dist;
            int y = server.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(x), (int)Math.floor(z));
            EnderMan enderman = (EnderMan)EntityTypes.ENDERMAN.create(server, EntitySpawnReason.EVENT);
            if (enderman != null) {
               enderman.absSnapTo(x, (double)y, z, (float)Math.toDegrees(Math.atan2(-dx, dz)) + 180.0F, 0.0F);
               enderman.setPersistenceRequired();
               server.addFreshEntity(enderman);
               ++this.siegeSpawned;
            }
         }
      }
   }

   private void eatEndermenInBeams(ServerLevel server) {
      tickCaughtEndermen(server);
      catchEndermenInBeams(server, this, 3);
   }

   public static void tickCaughtEndermen(ServerLevel server) {
      if (server.getGameTime() != caughtTickedAt) {
         caughtTickedAt = server.getGameTime();
         CAUGHT_ENDERMEN.entrySet().removeIf((entry) -> {
            Entity patt0$temp = server.getEntity((UUID)entry.getKey());
            if (patt0$temp instanceof EnderMan victim) {
               if (victim.isAlive()) {
                  int left = (Integer)entry.getValue() - 1;
                  entry.setValue(left);
                  victim.setDeltaMovement(Vec3.ZERO);
                  victim.setNoGravity(false);
                  victim.hurtMarked = true;
                  if (left <= 0) {
                     server.sendParticles(ParticleTypes.PORTAL, victim.getX(), victim.getY() + (double)1.0F, victim.getZ(), 60, 0.6, 1.2, 0.6, 0.6);
                     victim.discard();
                     return true;
                  }

                  return false;
               }
            }

            return true;
         });
      }
   }

   public static void catchEndermenInBeams(ServerLevel server, StormHeadHost host, int headCount) {
      for(int i = 0; i < headCount; ++i) {
         WitherStormHeadEntity head = host.hostHead(server, i);
         if (head != null && head.isBeamActive()) {
            Vec3 end = head.getBeamEndExact();
            double r = (double)WitherStormConfigs.get(server).beamGroundRadius;
            AABB box = new AABB(end.x - r, end.y - (double)4.0F, end.z - r, end.x + r, end.y + (double)12.0F, end.z + r);

            for(EnderMan victim : server.getEntitiesOfClass(EnderMan.class, box)) {
               if (!CAUGHT_ENDERMEN.containsKey(victim.getUUID())) {
                  CAUGHT_ENDERMEN.put(victim.getUUID(), 34);
                  victim.setNoAi(true);
                  victim.setDeltaMovement(Vec3.ZERO);
                  victim.setTarget((LivingEntity)null);
                  victim.getLookControl().setLookAt(head.getX(), head.getY(), head.getZ());
                  victim.lookAt(Anchor.EYES, head.position());
                  victim.setBeingStaredAt();
                  victim.playSound(SoundEvents.ENDERMAN_SCREAM, 2.4F, 0.85F);
               }
            }
         }
      }

   }

   public static boolean isHeldByBeam(Entity entity) {
      return CAUGHT_ENDERMEN.containsKey(entity.getUUID());
   }

   private void startChasing() {
      this.moveMode = WitherStormEntity.MoveMode.CHASING;
      this.chaseElapsed = 0;
      this.chaseGoalWasPresent = false;
      if (this.distractionTimer < 0 && this.level() instanceof ServerLevel) {
         this.distractionTimer = WitherStormConfigs.get(this.level()).distractionIntervalMinutes * 60 * 20;
      }

   }

   private void beginDistraction(WitherStormWorldConfig cfg) {
      this.moveMode = WitherStormEntity.MoveMode.DISTRACTED;
      this.pickDistractionPoint(cfg);
      this.distractionTicksLeft = cfg.distractionDurationSeconds * 20;
      this.distractionTimer = cfg.distractionIntervalMinutes * 60 * 20;
   }

   private void pickDistractionPoint(WitherStormWorldConfig cfg) {
      double angle = this.random.nextDouble() * Math.PI * (double)2.0F;
      double dist = (double)cfg.distractionRange * ((double)0.5F + this.random.nextDouble() * (double)0.5F);
      this.distractX = this.getX() + Math.cos(angle) * dist;
      this.distractZ = this.getZ() + Math.sin(angle) * dist;
   }

   public MoveMode getMoveMode() {
      return this.moveMode;
   }

   public boolean isChasing() {
      return this.moveMode == WitherStormEntity.MoveMode.CHASING;
   }

   public void setChasing(boolean chasing) {
      if (chasing) {
         this.startChasing();
      } else {
         this.moveMode = WitherStormEntity.MoveMode.FOLLOW;
         if (this.level() instanceof ServerLevel) {
            this.chaseTimer = WitherStormConfigs.get(this.level()).chaseIntervalMinutes * 60 * 20;
         }
      }

   }

   public void distractNow() {
      if (this.level() instanceof ServerLevel) {
         this.beginDistraction(WitherStormConfigs.get(this.level()));
      }

   }

   public UUID getUltimateTargetUUID() {
      return this.ultimateTargetUUID;
   }

   public boolean isUltimateTargetLocked() {
      return this.ultimateTargetLocked;
   }

   public void setUltimateTarget(Player player) {
      this.ultimateTargetUUID = player.getUUID();
      this.ultimateTargetLocked = true;
      this.entityData.set(ULTIMATE_TARGET_UUID, this.ultimateTargetUUID.toString());
   }

   public void clearUltimateTarget() {
      this.ultimateTargetUUID = null;
      this.ultimateTargetLocked = false;
      this.ultimateTargetCooldown = 0;
      this.entityData.set(ULTIMATE_TARGET_UUID, "");
   }

   public Player rerollUltimateTarget() {
      this.ultimateTargetUUID = null;
      this.ultimateTargetLocked = false;
      this.ultimateTargetCooldown = 0;
      this.updateUltimateTarget();
      return this.getUltimateTarget();
   }

   public int getChaseEtaSeconds() {
      return this.moveMode == WitherStormEntity.MoveMode.FOLLOW && this.chaseTimer >= 0 ? this.chaseTimer / 20 : -1;
   }

   public int getDistractionSecondsLeft() {
      return this.moveMode == WitherStormEntity.MoveMode.DISTRACTED ? Math.max(this.distractionTicksLeft, 0) / 20 : -1;
   }

   public void onHeadFired(int headIndex) {
      double sideX = HEAD_OFFSETS[headIndex].x;
      float side = sideX > (double)4.0F ? 1.0F : (sideX < (double)-4.0F ? -1.0F : 0.0F);
      if (side != 0.0F) {
         float phaseScale = this.behaviourPhase() >= 5.8 ? 0.0F : (this.behaviourPhase() >= (double)5.0F ? 0.5F : 1.0F);
         if (!(phaseScale <= 0.0F)) {
            this.recoilYawVel += side * (float)WitherStormConfigs.get(this.level()).recoilStrength * 0.15F * phaseScale;
         }
      }
   }

   private float smoothDampAngle(float current, float target, float[] velocity, float smoothTime) {
      float delta = Mth.wrapDegrees(target - current);
      target = current + delta;
      float omega = 2.0F / smoothTime;
      float x = omega * 0.05F;
      float exp = 1.0F / (1.0F + x + 0.48F * x * x + 0.235F * x * x * x);
      float change = current - target;
      float temp = (velocity[0] + omega * change) * 0.05F;
      velocity[0] = (velocity[0] - omega * temp) * exp;
      return target + (change + temp) * exp;
   }

   private void spawnAuraParticles(ServerLevel server) {
      if (this.random.nextInt(3) == 0) {
         double spread = (double)3.0F + this.getPhase() * 1.4;
         Vec3 c = this.getBoundingBox().getCenter();
         int motes = 1 + this.random.nextInt(2);

         for(int i = 0; i < motes; ++i) {
            double px = c.x + (this.random.nextDouble() - (double)0.5F) * (double)2.0F * spread;
            double py = c.y + (this.random.nextDouble() - (double)0.5F) * 1.6 * spread;
            double pz = c.z + (this.random.nextDouble() - (double)0.5F) * (double)2.0F * spread;
            server.sendParticles(AURA_MOTE, px, py, pz, 0, (this.random.nextDouble() - (double)0.5F) * 0.02, -0.015 - this.random.nextDouble() * 0.02, (this.random.nextDouble() - (double)0.5F) * 0.02, 0.6);
         }

      }
   }

   private void enterPhase4() {
      this.phase4 = true;
      this.setInvulnerable(true);
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)5000.0F);
      this.setHealth(5000.0F);
      this.entityData.set(SPAWN_ANIM_GAME_TIME, this.level().getGameTime());
      this.startChasing();
      this.refreshDimensions();
      this.updateBossBar();
   }

   private void updateBossBar() {
      if (!this.level().isClientSide()) {
         ServerBossEvent bar = ((WitherBossAccessor)this).getBossEvent();
         bar.setOverlay(this.phase4 ? BossBarOverlay.NOTCHED_10 : BossBarOverlay.PROGRESS);
         bar.setName(this.getDisplayName());
      }
   }

   protected SoundEvent getAmbientSound() {
      return this.isCollapsed() ? null : super.getAmbientSound();
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return this.isCollapsed() ? null : super.getHurtSound(source);
   }

   protected Component getTypeName() {
      return (Component)(this.phase4 ? Component.translatable("entity.dabywitherstormmod.wither_storm.phase4") : super.getTypeName());
   }

   public long getSpawnAnimGameTime() {
      return (Long)this.entityData.get(SPAWN_ANIM_GAME_TIME);
   }

   public boolean isPlayingSpawnAnimation() {
      long start = this.getSpawnAnimGameTime();
      if (start < 0L) {
         return false;
      } else {
         long elapsed = this.level().getGameTime() - start;
         return elapsed >= 0L && elapsed < 80L;
      }
   }

   public float getAnimationProgress() {
      long start = this.getSpawnAnimGameTime();
      if (start < 0L) {
         return 1.0F;
      } else {
         float elapsed = (float)(this.level().getGameTime() - start);
         return Mth.clamp(elapsed / 80.0F, 0.0F, 1.0F);
      }
   }

   protected EntityDimensions getDefaultDimensions(Pose pose) {
      return this.phase4 ? PHASE_4_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   protected AABB makeBoundingBox(Vec3 position) {
      float t = this.collapseTicks();
      float down = CollapseAnim.down(t);
      if (!(down <= 0.001F) && this.phase4) {
         EntityDimensions up = this.getDimensions(this.getPose());
         float w = Mth.lerp(down, up.width(), up.height() * 0.85F);
         float h = Mth.lerp(down, up.height(), up.width() * 1.2F);
         float half = w * 0.5F;
         return new AABB(position.x - (double)half, position.y, position.z - (double)half, position.x + (double)half, position.y + (double)h, position.z + (double)half);
      } else {
         return super.makeBoundingBox(position);
      }
   }

   private void followPlayer(Player player) {
      this.followGoal(player.position());
   }

   private static float preP4Lerp(float[] table, double phase) {
      double p = Mth.clamp(phase, (double)0.0F, (double)3.0F);
      int lo = (int)Math.floor(p);
      int hi = Math.min(lo + 1, table.length - 1);
      return Mth.lerp((float)(p - (double)lo), table[lo], table[hi]);
   }

   private static double preP4Lerp(double[] table, double phase) {
      double p = Mth.clamp(phase, (double)0.0F, (double)3.0F);
      int lo = (int)Math.floor(p);
      int hi = Math.min(lo + 1, table.length - 1);
      return Mth.lerp(p - (double)lo, table[lo], table[hi]);
   }

   private double preP4LeanAmount() {
      return Mth.clamp((this.phase - 0.85) / 0.15000000000000002, (double)0.0F, (double)1.0F);
   }

   private void followGoal(Vec3 goalPos) {
      Vec3 target = goalPos.add((double)0.0F, (double)8.0F, (double)0.0F);
      Vec3 diff = target.subtract(this.position());
      double dist = diff.length();
      double speedMul = preP4Lerp(PRE_P4_SPEED, this.phase);
      double standoff = (double)20.0F;
      if (dist > standoff) {
         double t = Mth.clamp((dist - standoff) / (double)30.0F, (double)0.0F, (double)1.0F);
         this.setDeltaMovement(diff.normalize().scale(0.08 * t * speedMul));
      } else {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
      }

      this.needsSync = true;
   }

   private void preP4Rotation() {
      float wanted = this.getYRot();
      wanted = this.yHeadRot;
      float before = this.preP4Yaw;
      if (Float.isNaN(this.preP4Yaw)) {
         this.preP4Yaw = wanted;
      }

      float[] vel = new float[]{this.preP4YawVel};
      this.preP4Yaw = this.smoothDampAngle(this.preP4Yaw, wanted, vel, preP4Lerp(PRE_P4_TURN_TIME, this.phase));
      this.preP4YawVel = vel[0];
      float turnRate = Float.isNaN(before) ? 0.0F : Mth.degreesDifference(before, this.preP4Yaw);
      this.setYRot(this.preP4Yaw);
      this.setYBodyRot(this.preP4Yaw);
      this.setYHeadRot(this.preP4Yaw);
      double lean = this.preP4LeanAmount();
      if (lean <= 0.001) {
         if ((Float)this.entityData.get(BODY_ROLL) != 0.0F) {
            this.entityData.set(BODY_ROLL, 0.0F);
         }

         this.setXRot(0.0F);
      } else {
         float wantRoll = Mth.clamp(-turnRate * 2.6F, -9.0F, 9.0F) * (float)lean;
         this.preP4Roll += (wantRoll - this.preP4Roll) * 0.08F;
         this.entityData.set(BODY_ROLL, this.preP4Roll);
         Vec3 v = this.getDeltaMovement();
         double speed = Math.sqrt(v.x * v.x + v.z * v.z);
         float wantPitch = (float)Mth.clamp(speed / 0.08, (double)0.0F, (double)1.0F) * 7.0F * (float)lean;
         this.preP4Pitch += (wantPitch - this.preP4Pitch) * 0.06F;
         this.setXRot(this.preP4Pitch);
      }
   }

   private void preP4HeadLimits() {
      float range = preP4Lerp(PRE_P4_HEAD_RANGE, this.phase);
      if (!(range >= 179.0F)) {
         float[] yRots = this.getHeadYRots();
         float[] xRots = this.getHeadXRots();

         for(int i = 1; i < Math.min(yRots.length, xRots.length); ++i) {
            yRots[i] = clampToBaseDegrees(yRots[i], this.getYRot(), range);
            xRots[i] = Mth.clamp(xRots[i], -range, range);
            if (range <= 0.5F && this.getAlternativeTarget(i - 1) != 0) {
               this.setAlternativeTarget(i - 1, 0);
            }
         }

      }
   }

   private static float clampToBaseDegrees(float wanted, float base, float maxDelta) {
      return base + Mth.clamp(Mth.wrapDegrees(wanted - base), -maxDelta, maxDelta);
   }

   private void phase3Hover() {
      double desiredY = this.highestGroundAround() + (double)11.0F;
      double dy = Mth.clamp(desiredY - this.getY(), -0.09, 0.09);
      if (Math.abs(dy) > 1.0E-4) {
         this.setPos(this.getX(), this.getY() + dy, this.getZ());
      }

      if (this.phase3Quarry != null && (!this.phase3Quarry.isAlive() || this.phase3Quarry.isRemoved() || this.phase3Quarry.level() != this.level() || this.phase3Quarry.distanceToSqr(this) > (double)16384.0F)) {
         this.phase3Quarry = null;
      }

      if (this.phase3Quarry == null && this.tickCount >= this.phase3RepickAt) {
         this.phase3RepickAt = this.tickCount + 40;
         this.phase3Quarry = this.pickPhase3Quarry();
      }

      LivingEntity quarry = this.phase3Quarry;
      if (quarry != null) {
         double dx = quarry.getX() - this.getX();
         double dz = quarry.getZ() - this.getZ();
         double horiz = Math.sqrt(dx * dx + dz * dz);
         if (horiz > (double)14.0F) {
            double speed = 0.08 * preP4Lerp(PRE_P4_SPEED, this.phase);
            double t = Mth.clamp((horiz - (double)14.0F) / (double)24.0F, (double)0.0F, (double)1.0F);
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(dx / horiz * speed * t, v.y, dz / horiz * speed * t);
            this.needsSync = true;
         }

      }
   }

   private LivingEntity pickPhase3Quarry() {
      LivingEntity best = this.nearestTargetable();
      double bestSq = best == null ? Double.MAX_VALUE : best.distanceToSqr(this);

      for(LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate((double)64.0F), (c) -> c != this && !(c instanceof Player) && !(c instanceof WitherStormEntity) && c.isAlive() && !c.isRemoved() && !this.isDoomed(c) && !WitheredMobs.isWithered(c))) {
         double d = e.distanceToSqr(this);
         if (d < bestSq) {
            bestSq = d;
            best = e;
         }
      }

      return best;
   }

   private float idleSway() {
      double p = this.phase;
      float amp;
      if (p >= (double)6.0F) {
         amp = 0.45F;
      } else if (p >= 5.8) {
         amp = 0.0F;
      } else if (p >= (double)5.5F) {
         amp = Mth.lerp((float)((p - (double)5.5F) / 0.3), 0.55F, 0.0F);
      } else if (p >= (double)5.0F) {
         amp = Mth.lerp((float)((p - (double)5.0F) / (double)0.5F), 1.25F, 0.55F);
      } else if (p >= (double)4.5F) {
         amp = Mth.lerp((float)((p - (double)4.5F) / (double)0.5F), 2.2F, 1.25F);
      } else {
         amp = Mth.lerp((float)Mth.clamp((p - (double)4.0F) / (double)0.5F, (double)0.0F, (double)1.0F), 3.2F, 2.2F);
      }

      if (amp <= 0.001F) {
         return 0.0F;
      } else {
         float t = (float)this.tickCount;
         return amp * (Mth.sin((double)(t * 0.0121F)) * 0.72F + Mth.sin((double)(t * 0.0074F + 1.9F)) * 0.28F);
      }
   }

   private void cocoonTick(Player ultimate) {
   }

   private double tickTentacleGrab(WitherStormWorldConfig cfg, double normalY) {
      Level var5 = this.level();
      if (var5 instanceof ServerLevel sl) {
         if (this.snatchVictim != null) {
            Player held = this.snatchVictim;
            this.tickSnatch();
            if (this.snatchVictim != null) {
               this.updateGrabTentacle(held, (double)1.0F, true);
               return this.getY();
            } else {
               return normalY;
            }
         } else {
            Vec3 c = this.getBoundingBox().getCenter();
            Player best = null;
            double bestSqr = (double)1600.0F;
            long gameTime = sl.getGameTime();

            for(Player p : this.level().players()) {
               if (this.isTargetable(p) && !p.isCreative() && !WitherStormHeadEntity.isForgiven(p, gameTime) && !ModEffects.isHyperInvisible(p) && !(p.getY() > this.getY())) {
                  double dx = p.getX() - c.x;
                  double dz = p.getZ() - c.z;
                  double horizSqr = dx * dx + dz * dz;
                  if (horizSqr < bestSqr) {
                     bestSqr = horizSqr;
                     best = p;
                  }
               }
            }

            if (best == null) {
               this.despawnGrabTentacle();
               return normalY;
            } else {
               double horizDist = Math.sqrt(bestSqr);
               if (horizDist < (double)18.0F && this.getY() - best.getY() < (double)15.0F) {
                  this.beginSnatch(best);
                  this.updateGrabTentacle(best, (double)1.0F, true);
                  return this.getY();
               } else {
                  double closeness = (double)1.0F - Mth.clamp(horizDist / (double)40.0F, (double)0.0F, (double)1.0F);
                  this.updateGrabTentacle(best, 0.35 + 0.65 * closeness, false);
                  double reachY = best.getY() + (double)7.0F;
                  double sinkY = Mth.lerp(closeness, normalY, reachY);
                  return Math.min(normalY, sinkY);
               }
            }
         }
      } else {
         return normalY;
      }
   }

   private void updateGrabTentacle(Player victim, double reach, boolean grabbed) {
      Level var6 = this.level();
      if (var6 instanceof ServerLevel sl) {
         if (this.grabTentacle == null || this.grabTentacle.isRemoved()) {
            this.grabTentacle = new GrabTentacleEntity(ModEntityTypes.GRAB_TENTACLE, sl);
            this.grabTentacle.setStormId(this.getId());
            this.grabTentacle.setPos(this.getX(), this.getY(), this.getZ());
            sl.addFreshEntity(this.grabTentacle);
         }

         this.grabTentacle.setVictimId(victim.getId());
         this.grabTentacle.setGrabbed(grabbed);
         Vec3 belly = new Vec3(this.getX(), this.getY() + (double)6.0F, this.getZ());
         Vec3 vpos = victim.position().add((double)0.0F, (double)victim.getBbHeight() * (double)0.5F, (double)0.0F);
         Vec3 tip = grabbed ? vpos : belly.add(vpos.subtract(belly).scale(Mth.clamp(reach, (double)0.0F, (double)1.0F)));
         this.grabTentacle.setPos(tip.x, tip.y, tip.z);
      }
   }

   private void despawnGrabTentacle() {
      if (this.grabTentacle != null) {
         this.grabTentacle.discard();
         this.grabTentacle = null;
      }

   }

   public void registerGrabHit(ServerLevel level, Player attacker) {
      if (this.snatchVictim != null && attacker == this.snatchVictim) {
         ItemStack held = attacker.getMainHandItem();
         boolean weapon = held.is(ItemTags.SWORDS) || held.is(ItemTags.AXES);
         if (weapon) {
            level.playSound((Entity)null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            this.playSound(ModSounds.HEAD_HURT, this.voiceVolume(0.75F), 0.92F - this.random.nextFloat() * 0.06F);
            if (++this.snatchHits >= 5) {
               this.playSound(ModSounds.HEAD_SNARL, this.voiceVolume(0.75F), 0.98F);
               this.endSnatch(level, true);
            }

         }
      }
   }

   private void beginSnatch(Player player) {
      this.snatchVictim = player;
      this.snatchTicks = 0;
      this.snatchHits = 0;
      this.lastSnatchPos = player.position();
      this.entityData.set(SNATCH_ID, player.getId());
      this.playSound(ModSounds.HEAD_SNARL, this.voiceVolume(0.75F), 0.96F);
   }

   private void tickSnatch() {
      Player p = this.snatchVictim;
      if (p != null) {
         Level var3 = this.level();
         if (var3 instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)var3;
            boolean pearled = this.lastSnatchPos != null && p.position().distanceToSqr(this.lastSnatchPos) > (double)36.0F;
            if (p.isAlive() && !p.isRemoved() && p.level() == this.level() && !p.isCreative() && !p.isSpectator() && !p.isFallFlying() && !pearled) {
               this.lastSnatchPos = p.position();
               ++this.snatchTicks;
               float prog = Math.min(1.0F, (float)this.snatchTicks / 110.0F);
               float eased = prog * prog * (3.0F - 2.0F * prog);
               WitherStormHeadEntity midHead = this.getHead(sl, 0);
               Vec3 dest;
               if (midHead != null) {
                  dest = midHead.position().add((double)0.0F, (double)-2.0F, (double)0.0F);
               } else {
                  dest = this.getBoundingBox().getCenter().add((double)0.0F, (double)5.0F, (double)0.0F);
               }

               Vec3 vel = dest.subtract(p.position());
               double speed = 0.12 + 0.55 * (double)eased;
               if (vel.length() > speed) {
                  vel = vel.normalize().scale(speed);
               }

               p.setDeltaMovement(vel);
               p.fallDistance = (double)0.0F;
               p.hurtMarked = true;
               if (p instanceof ServerPlayer) {
                  ServerPlayer sp = (ServerPlayer)p;
                  sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
               }

               this.lockRotationOn(p);
               if (prog >= 1.0F || prog > 0.5F && p.position().distanceTo(dest) < (double)3.5F) {
                  if (midHead != null) {
                     midHead.chompVictim(p);
                     this.endSnatch(sl, false);
                  } else {
                     Holder.Reference<DamageType> type = sl.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(WitherStormHeadEntity.CHOMP_DAMAGE);
                     p.hurtServer(sl, new DamageSource(type, this), Float.MAX_VALUE);
                     this.endSnatch(sl, p.isAlive());
                  }
               }

               return;
            }

            this.endSnatch(sl, p.isAlive());
            return;
         }
      }

   }

   private void endSnatch(ServerLevel sl, boolean survivorGetsForgiveness) {
      if (survivorGetsForgiveness && this.snatchVictim != null && this.snatchVictim.isAlive()) {
         WitherStormHeadEntity.forgive(sl, this.snatchVictim);
      }

      this.snatchVictim = null;
      this.snatchTicks = 0;
      this.snatchHits = 0;
      this.lastSnatchPos = null;
      this.entityData.set(SNATCH_ID, -1);
      this.despawnGrabTentacle();
   }

   public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
      this.resetFallDistance();
      return false;
   }

   public int getMaxFallDistance() {
      return Integer.MAX_VALUE;
   }

   public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
      return source.is(DamageTypeTags.IS_FALL) ? true : super.isInvulnerableTo(level, source);
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      Entity var5 = source.getEntity();
      if (var5 instanceof Player attacker) {
         this.registerGrabHit(level, attacker);
      }

      if (source.is(DamageTypeTags.IS_FALL)) {
         return false;
      } else {
         return this.isPhase4() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? false : super.hurtServer(level, source, amount);
      }
   }

   public boolean isPickable() {
      return this.isPhase4() ? false : super.isPickable();
   }

   public void doomMob(LivingEntity mob) {
      if (!WitheredMobs.isWithered(mob)) {
         if (!this.doomedMobs.contains(mob)) {
            this.doomedMobs.add(mob);
            if (mob instanceof Mob) {
               Mob m = (Mob)mob;
               m.setNoAi(true);
            }

            mob.noPhysics = true;
            mob.setNoGravity(true);
         }

      }
   }

   public boolean isDoomed(LivingEntity mob) {
      return this.doomedMobs.contains(mob);
   }

   public boolean isProbingPortal() {
      return this.probePortal != null;
   }

   public BlockPos getProbePortal() {
      return this.probePortal;
   }

   private void tickPortalProbe(ServerLevel server) {
      if (this.probeCooldown > 0) {
         --this.probeCooldown;
      }

      if (this.probePortal != null) {
         if (!server.getBlockState(this.probePortal).is(Blocks.NETHER_PORTAL)) {
            this.probePortal = null;
            this.probeCommitTicks = 0;
         } else if (!this.anyPlayerInNether(server)) {
            this.probePortal = null;
            this.probeCommitTicks = 0;
         } else if (++this.probeCommitTicks > 1800) {
            this.probePortal = null;
            this.probeCommitTicks = 0;
            this.probeCooldown = 600;
         } else {
            double dx = (double)this.probePortal.getX() + (double)0.5F - this.getX();
            double dz = (double)this.probePortal.getZ() + (double)0.5F - this.getZ();
            boolean overIt = dx * dx + dz * dz < (double)36.0F;
            boolean lowEnough = this.getY() - (double)this.probePortal.getY() < (double)26.0F;
            if (overIt && lowEnough) {
               this.punchThroughPortal(server, this.probePortal);
               this.probePortal = null;
               this.probeCommitTicks = 0;
               this.probeCooldown = 400;
            }

         }
      } else {
         if (this.probeCooldown <= 0) {
            BlockPos ultimatePortal = this.ultimateTargetNetherPortal(server);
            if (ultimatePortal != null) {
               this.probePortal = ultimatePortal;
               return;
            }
         }

         if (this.probeCooldown <= 0 && --this.portalScanTimer <= 0) {
            this.portalScanTimer = 40;
            if (this.anyPlayerInNether(server)) {
               this.portalScanParity ^= 1;
               BlockPos found = findSurfacePortal(server, this.blockPosition(), 400, this.portalScanParity);
               if (found == null) {
                  found = this.findTargetPortal(server);
               }

               if (found != null) {
                  this.probePortal = found;
               }

            }
         }
      }
   }

   private BlockPos ultimateTargetNetherPortal(ServerLevel server) {
      if (this.ultimateTargetUUID == null) {
         return null;
      } else {
         ServerLevel nether = server.getServer().getLevel(Level.NETHER);
         if (nether == null) {
            return null;
         } else {
            ServerPlayer prey = server.getServer().getPlayerList().getPlayer(this.ultimateTargetUUID);
            if (prey != null && prey.level() == nether && prey.isAlive() && !prey.isSpectator()) {
               BlockPos netherPortal = findPortalNear(nether, prey.blockPosition(), 64);
               BlockPos anchor = netherPortal != null ? netherPortal : prey.blockPosition();
               BlockPos linked = new BlockPos(anchor.getX() * 8, 0, anchor.getZ() * 8);
               return findSurfacePortal(server, linked, 128);
            } else {
               return null;
            }
         }
      }
   }

   private BlockPos findTargetPortal(ServerLevel server) {
      ServerLevel nether = server.getServer().getLevel(Level.NETHER);
      if (nether != null) {
         for(ServerPlayer p : nether.players()) {
            if (p.isAlive() && !p.isSpectator()) {
               BlockPos netherPortal = findPortalNear(nether, p.blockPosition(), 64);
               BlockPos anchor = netherPortal != null ? netherPortal : p.blockPosition();
               BlockPos linked = new BlockPos(anchor.getX() * 8, 0, anchor.getZ() * 8);
               BlockPos hit = findSurfacePortal(server, linked, 128);
               if (hit != null) {
                  return hit;
               }
            }
         }
      }

      return findSurfacePortal(server, this.blockPosition(), 128);
   }

   private static BlockPos findSurfacePortal(ServerLevel level, BlockPos centre, int radius) {
      return findSurfacePortal(level, centre, radius, 0);
   }

   private static BlockPos findSurfacePortal(ServerLevel level, BlockPos centre, int radius, int parity) {
      BlockPos best = null;
      double bestSqr = Double.MAX_VALUE;

      for(int dx = -radius + parity; dx <= radius; dx += 2) {
         for(int dz = -radius + parity; dz <= radius; dz += 2) {
            BlockPos column = centre.offset(dx, 0, dz);
            if (level.hasChunkAt(column)) {
               int surface = level.getHeightmapPos(Types.MOTION_BLOCKING, column).getY();

               for(int y = surface - 16; y <= surface + 24; ++y) {
                  BlockPos probe = new BlockPos(column.getX(), y, column.getZ());
                  if (level.getBlockState(probe).is(Blocks.NETHER_PORTAL)) {
                     double d = probe.distSqr(centre);
                     if (d < bestSqr) {
                        bestSqr = d;
                        best = probe.immutable();
                     }
                     break;
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean anyPlayerInNether(ServerLevel server) {
      ServerLevel nether = server.getServer().getLevel(Level.NETHER);
      if (nether == null) {
         return false;
      } else {
         for(ServerPlayer p : nether.players()) {
            if (p.isAlive() && !p.isSpectator()) {
               return true;
            }
         }

         return false;
      }
   }

   private void punchThroughPortal(ServerLevel server, BlockPos portal) {
      ServerLevel nether = server.getServer().getLevel(Level.NETHER);
      if (nether != null) {
         ServerPlayer prey = null;
         double bestSqr = Double.MAX_VALUE;

         for(ServerPlayer p : nether.players()) {
            if (p.isAlive() && !p.isSpectator()) {
               double d = p.position().distanceToSqr((double)portal.getX() / (double)8.0F, p.getY(), (double)portal.getZ() / (double)8.0F);
               if (d < bestSqr) {
                  bestSqr = d;
                  prey = p;
               }
            }
         }

         if (prey != null) {
            BlockPos netherPortal = findPortalNear(nether, prey.blockPosition(), 64);
            if (netherPortal == null) {
               BlockPos linked = new BlockPos(Mth.floor((double)portal.getX() / (double)8.0F), prey.blockPosition().getY(), Mth.floor((double)portal.getZ() / (double)8.0F));
               netherPortal = findPortalNear(nether, linked, 64);
            }

            if (netherPortal != null) {
               for(BlockPos pos : BlockPos.betweenClosed(portal.offset(-3, -2, -3), portal.offset(3, 4, 3))) {
                  if (server.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
                     server.removeBlock(pos, false);
                  }
               }

               server.playSound((Entity)null, portal, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 3.0F, 0.5F);
               CrossDimensionalEntity spawned = null;
               if (spawnPortalProbe(nether, netherPortal, prey)) {
                  for(CrossDimensionalEntity e : nether.getEntitiesOfClass(CrossDimensionalEntity.class, (new AABB(netherPortal)).inflate((double)6.0F))) {
                     if (e.tickCount == 0) {
                        spawned = e;
                        break;
                     }
                  }

                  if (spawned != null) {
                     spawned.setStormUUID(this.getUUID());
                  }
               }

            }
         }
      }
   }

   public static boolean spawnPortalProbe(ServerLevel nether, BlockPos netherPortal, Player prey) {
      if (nether != null && netherPortal != null && prey != null) {
         Vec3 mouth = portalCentre(nether, netherPortal);
         CrossDimensionalEntity probe = new CrossDimensionalEntity(ModEntityTypes.CROSS_DIMENSIONAL, nether);
         probe.setPos(mouth.x, mouth.y, mouth.z);
         BlockState portalState = nether.getBlockState(netherPortal);
         Direction.Axis planeAxis = portalState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS) ? (Direction.Axis)portalState.getValue(BlockStateProperties.HORIZONTAL_AXIS) : Axis.X;
         float yaw;
         if (planeAxis == Axis.X) {
            yaw = prey.getZ() >= mouth.z ? 0.0F : 180.0F;
         } else {
            yaw = prey.getX() >= mouth.x ? -90.0F : 90.0F;
         }

         probe.setYRot(yaw);
         probe.setYHeadRot(yaw);
         probe.setPortalAnchor(netherPortal);
         return nether.addFreshEntity(probe);
      } else {
         return false;
      }
   }

   private static Vec3 portalCentre(ServerLevel level, BlockPos seed) {
      Set<BlockPos> seen = new HashSet();
      ArrayDeque<BlockPos> queue = new ArrayDeque();
      queue.add(seed.immutable());
      seen.add(seed.immutable());
      double sx = (double)0.0F;
      double sy = (double)0.0F;
      double sz = (double)0.0F;
      int n = 0;

      while(!queue.isEmpty() && n < 128) {
         BlockPos p = (BlockPos)queue.poll();
         if (level.getBlockState(p).is(Blocks.NETHER_PORTAL)) {
            sx += (double)p.getX() + (double)0.5F;
            sy += (double)p.getY();
            sz += (double)p.getZ() + (double)0.5F;
            ++n;

            for(Direction d : Direction.values()) {
               BlockPos q = p.relative(d).immutable();
               if (seen.add(q)) {
                  queue.add(q);
               }
            }
         }
      }

      return n == 0 ? new Vec3((double)seed.getX() + (double)0.5F, (double)seed.getY() + (double)1.0F, (double)seed.getZ() + (double)0.5F) : new Vec3(sx / (double)n, sy / (double)n + (double)0.5F, sz / (double)n);
   }

   private static BlockPos findPortalNear(ServerLevel level, BlockPos origin, int radius) {
      for(int dx = -radius; dx <= radius; dx += 2) {
         for(int dz = -radius; dz <= radius; dz += 2) {
            for(int dy = -24; dy <= 24; dy += 2) {
               BlockPos probe = origin.offset(dx, dy, dz);
               if (level.hasChunkAt(probe) && level.getBlockState(probe).is(Blocks.NETHER_PORTAL)) {
                  return probe.immutable();
               }
            }
         }
      }

      return null;
   }

   private static void freeDoomedMob(LivingEntity mob) {
      mob.noPhysics = false;
      mob.setNoGravity(false);
      if (mob instanceof Mob m) {
         m.setNoAi(false);
      }

   }

   private void tickDoomedMobs() {
      if (!this.doomedMobs.isEmpty()) {
         Level var2 = this.level();
         if (var2 instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)var2;
            Vec3 var11 = this.getBoundingBox().getCenter();
            Iterator it = this.doomedMobs.iterator();

            while(it.hasNext()) {
               LivingEntity mob = (LivingEntity)it.next();
               if (mob.isAlive() && !mob.isRemoved() && mob.level() == this.level()) {
                  Vec3 pull = var11.subtract(mob.position());
                  double dist = pull.length();
                  if (dist > (double)96.0F) {
                     freeDoomedMob(mob);
                     it.remove();
                  } else if (dist < (double)4.5F) {
                     if (mob instanceof ArmorStand) {
                        ArmorStand stand = (ArmorStand)mob;
                        stand.discard();
                     } else {
                        Holder.Reference<DamageType> type = sl.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(WitherStormHeadEntity.CHOMP_DAMAGE);
                        mob.hurtServer(sl, new DamageSource(type, this), Float.MAX_VALUE);
                     }

                     this.addSubGrowth(2);
                     it.remove();
                  } else {
                     mob.noPhysics = true;
                     mob.setNoGravity(true);
                     if (mob instanceof Mob) {
                        Mob m = (Mob)mob;
                        if (!m.isNoAi()) {
                           m.setNoAi(true);
                        }
                     }

                     double step = Math.min(dist, Math.min(1.2, 0.4 + dist * 0.02));
                     Vec3 next = mob.position().add(pull.normalize().scale(step));
                     mob.setDeltaMovement(Vec3.ZERO);
                     mob.setPos(next.x, next.y, next.z);
                     mob.fallDistance = (double)0.0F;
                     mob.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                     WitherSickness.advance(mob, 60);
                  }
               } else {
                  it.remove();
               }
            }

            return;
         }
      }

   }

   private void updateHeads() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel server) {
         float bodyYaw = this.getYRot();
         double rad = Math.toRadians((double)bodyYaw);
         double cos = Math.cos(rad);
         double sin = Math.sin(rad);
         double rollRad = Math.toRadians((double)(Float)this.entityData.get(BODY_ROLL));
         double cosR = Math.cos(rollRad);
         double sinR = Math.sin(rollRad);
         double pitchRad = Math.toRadians((double)this.getXRot());
         double cosP = Math.cos(pitchRad);
         double sinP = Math.sin(pitchRad);
         int active = this.activeHeadCount();

         for(int i = 0; i < 3; ++i) {
            WitherStormHeadEntity head = this.getHead(server, i);
            if (i >= active) {
               if (head != null) {
                  head.discard();
                  this.headUUIDs[i] = null;
               }
            } else {
               if (head == null || head.isRemoved()) {
                  if (this.headSpawnGraceTicks > 0 || this.headSpawnDelay[i] > 0) {
                     continue;
                  }

                  head = this.spawnHead(server, i);
               }

               Vec3 offset = headOffset(i, this.isDevourer());
               double py = offset.y * cosP - offset.z * sinP;
               double pz = offset.y * sinP + offset.z * cosP;
               double rx = offset.x * cosR - py * sinR;
               double ry = offset.x * sinR + py * cosR;
               double worldX = this.getX() + (rx * cos - pz * sin);
               double worldZ = this.getZ() + rx * sin + pz * cos;
               double worldY = this.getY() + ry;
               head.setPos(worldX, worldY, worldZ);
               head.setBaseYaw(bodyYaw + this.headYawOffsetFor(i));
            }
         }

      }
   }

   public int activeHeadCount() {
      if (this.phase < (double)4.0F) {
         return 0;
      } else if (!this.isDevourer()) {
         return 3;
      } else if (this.phase < 6.1) {
         this.devourerHeadsReturnTick = -1L;
         return 1;
      } else {
         if (this.devourerHeadsReturnTick < 0L) {
            this.devourerHeadsReturnTick = (long)this.tickCount;
         }

         long since = (long)this.tickCount - this.devourerHeadsReturnTick;
         if (since >= 90L) {
            return 3;
         } else {
            return since >= 45L ? 2 : 1;
         }
      }
   }

   private void spawnHeadsIfNeeded() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel server) {
         int var9 = this.activeHeadCount();
         int slot = 0;

         for(int idx : HEAD_SPAWN_ORDER) {
            if (idx < var9) {
               WitherStormHeadEntity head = this.getHead(server, idx);
               if (head == null || head.isRemoved()) {
                  if (this.headSpawnDelay[idx] <= 0) {
                     this.headSpawnDelay[idx] = 1 + slot * 40;
                  }

                  ++slot;
               }
            }
         }

      }
   }

   private void removeHeads() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel server) {
         for(int var4 = 0; var4 < 3; ++var4) {
            WitherStormHeadEntity head = this.getHead(server, var4);
            if (head != null) {
               head.discard();
            }

            this.headUUIDs[var4] = null;
         }

      }
   }

   public Vec3 modelCenter() {
      return this.position().add((double)0.0F, (double)this.getBbHeight() * (double)0.5F, (double)0.0F);
   }

   public boolean isGrappleHittable(Vec3 point) {
      double phase = this.getPhase();
      double h = (double)this.getBbHeight();
      double yaw = Math.toRadians((double)this.getYRot());
      Vec3 forward = new Vec3(-Math.sin(yaw), (double)0.0F, Math.cos(yaw));
      Vec3 body = this.position().add((double)0.0F, h * 0.55, (double)0.0F);
      double bodyR;
      double backOff;
      double backR;
      double backBackOff;
      double backBackR;
      if (phase >= 5.8) {
         bodyR = (double)9.0F;
         backOff = (double)10.0F;
         backR = (double)11.0F;
         backBackOff = (double)24.0F;
         backBackR = (double)12.0F;
      } else if (phase >= (double)5.0F) {
         bodyR = (double)8.0F;
         backOff = (double)9.0F;
         backR = (double)9.0F;
         backBackOff = (double)0.0F;
         backBackR = (double)0.0F;
      } else if (phase >= (double)4.0F) {
         bodyR = (double)6.0F;
         backOff = (double)7.0F;
         backR = (double)6.0F;
         backBackOff = (double)0.0F;
         backBackR = (double)0.0F;
      } else {
         bodyR = (double)2.5F;
         backOff = 1.8;
         backR = 2.2;
         backBackOff = (double)0.0F;
         backBackR = (double)0.0F;
         body = this.position().add((double)0.0F, h * 0.6, (double)0.0F);
      }

      if (point.distanceTo(body) <= bodyR) {
         return true;
      } else if (point.distanceTo(body.subtract(forward.scale(backOff))) <= backR) {
         return true;
      } else {
         return backBackR > (double)0.0F && point.distanceTo(body.subtract(forward.scale(backBackOff))) <= backBackR;
      }
   }

   public void beginSpawnFreeze() {
      WitherStormWorldConfig cfg = WitherStormConfigs.get(this.level());
      this.spawnFreezeTicks = Math.max(0, cfg.spawnFreezeSeconds) * 20;
      this.spawnFreezeTotalTicks = this.spawnFreezeTicks;
      this.spawnWailPending = true;
      if (this.spawnFreezeTicks > 0) {
         ((WitherBossAccessor)this).getBossEvent().setProgress(0.0F);
      }

   }

   public void playGlobalSpawnWail() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel server) {
         Holder var5 = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WITHER_SPAWN);

         for(ServerPlayer p : server.players()) {
            p.connection.send(new ClientboundSoundPacket(var5, SoundSource.HOSTILE, p.getX(), p.getY(), p.getZ(), 0.55F, 0.6F, server.getRandom().nextLong()));
         }

      }
   }

   public void onGrappleHit(Vec3 point) {
      Level var3 = this.level();
      if (var3 instanceof ServerLevel server) {
         this.grappleStartleTicks = 90;
         this.startleYaw = this.getYRot();

         for(int var5 = 0; var5 < 3; ++var5) {
            WitherStormHeadEntity head = this.getHead(server, var5);
            if (head != null && head.isAlive()) {
               head.startle(point);
            }
         }

         server.playSound((Entity)null, this.getX(), this.modelCenter().y, this.getZ(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 32.0F, 0.5F);
         server.playSound((Entity)null, this.getX(), this.modelCenter().y, this.getZ(), ModSounds.HEAD_HURT, SoundSource.HOSTILE, 32.0F, 0.85F);
      }
   }

   public WitherStormHeadEntity getMiddleHead() {
      Level var2 = this.level();
      if (!(var2 instanceof ServerLevel server)) {
         return null;
      } else {
         WitherStormHeadEntity head = this.getHead(server, 0);
         return head != null && head.isAlive() ? head : null;
      }
   }

   public WitherStormHeadEntity getAnyHead() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel server) {
         for(int var4 = 0; var4 < 3; ++var4) {
            WitherStormHeadEntity head = this.getHead(server, var4);
            if (head != null && head.isAlive()) {
               return head;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public WitherStormHeadEntity hostHead(ServerLevel server, int index) {
      return this.getHead(server, index);
   }

   private WitherStormHeadEntity getHead(ServerLevel server, int index) {
      UUID id = this.headUUIDs[index];
      if (id == null) {
         return null;
      } else {
         Entity entity = server.getEntity(id);
         if (entity instanceof WitherStormHeadEntity) {
            WitherStormHeadEntity head = (WitherStormHeadEntity)entity;
            return head;
         } else {
            return null;
         }
      }
   }

   private WitherStormHeadEntity spawnHead(ServerLevel server, int index) {
      WitherStormHeadEntity head = new WitherStormHeadEntity(ModEntityTypes.WITHER_STORM_HEAD, server);
      head.setStormData(this.getUUID(), index);
      head.setPos(this.getX(), this.getY(), this.getZ());
      head.setBaseYaw(this.getYRot() + this.headYawOffsetFor(index));
      head.markJustSpawned();
      server.addFreshEntity(head);
      this.headUUIDs[index] = head.getUUID();
      return head;
   }

   private void updateMiniHead(ServerLevel server) {
      WitherStormHeadEntity var10000;
      label39: {
         if (this.headUUIDs[0] != null) {
            Entity var4 = server.getEntity(this.headUUIDs[0]);
            if (var4 instanceof WitherStormHeadEntity) {
               WitherStormHeadEntity h = (WitherStormHeadEntity)var4;
               var10000 = h;
               break label39;
            }
         }

         var10000 = null;
      }

      WitherStormHeadEntity head = var10000;
      if (head == null || head.isRemoved()) {
         if (this.headSpawnGraceTicks > 0) {
            --this.headSpawnGraceTicks;
            return;
         }

         head = this.spawnHead(server, 0);
         if (head == null) {
            return;
         }
      }

      Vec3 off = this.headOffsetFor(0);
      double rad = Math.toRadians((double)this.getYRot());
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);
      head.setPos(this.getX() + (off.x * cos - off.z * sin), this.getY() + off.y, this.getZ() + off.x * sin + off.z * cos);
      head.setBaseYaw(this.getYRot() + this.headYawOffsetFor(0));

      for(int i = 1; i < 3; ++i) {
         if (this.headUUIDs[i] != null) {
            Entity var12 = server.getEntity(this.headUUIDs[i]);
            if (var12 instanceof WitherStormHeadEntity) {
               WitherStormHeadEntity extra = (WitherStormHeadEntity)var12;
               extra.discard();
            }
         }

         this.headUUIDs[i] = null;
      }

   }

   private void removeMiniHead(ServerLevel server) {
      for(int i = 0; i < 3; ++i) {
         if (this.headUUIDs[i] != null) {
            Entity var4 = server.getEntity(this.headUUIDs[i]);
            if (var4 instanceof WitherStormHeadEntity) {
               WitherStormHeadEntity h = (WitherStormHeadEntity)var4;
               h.discard();
            }
         }

         this.headUUIDs[i] = null;
      }

   }

   private void updateChunkLoading() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel server) {
         int var12 = this.chunkPosition().x();
         int cz = this.chunkPosition().z();
         boolean movedChunk = var12 != this.lastForcedChunkX || cz != this.lastForcedChunkZ;
         if (movedChunk || this.tickCount % 5 == 0) {
            this.lastForcedChunkX = var12;
            this.lastForcedChunkZ = cz;
            Set<ChunkPos> desired = new HashSet();

            for(int x = -4; x <= 4; ++x) {
               for(int z = -4; z <= 4; ++z) {
                  desired.add(new ChunkPos(var12 + x, cz + z));
               }
            }

            boolean reassert = this.tickCount % 100 == 0;
            List<ChunkPos> toForce = new ArrayList();

            for(ChunkPos pos : desired) {
               if (reassert || !this.forcedByUs.contains(pos)) {
                  toForce.add(pos);
               }
            }

            toForce.sort(Comparator.comparingInt((p) -> Math.max(Math.abs(p.x() - var12), Math.abs(p.z() - cz))));
            int forcedThisPass = 0;

            for(ChunkPos pos : toForce) {
               boolean core = Math.max(Math.abs(pos.x() - var12), Math.abs(pos.z() - cz)) <= 2;
               if (!core && !reassert && forcedThisPass >= 4) {
                  break;
               }

               ChunkForceRegistry.acquire(server, this.getUUID(), pos);
               this.forcedByUs.add(pos);
               if (!core) {
                  ++forcedThisPass;
               }
            }

            Iterator<ChunkPos> iterator = this.forcedByUs.iterator();

            while(iterator.hasNext()) {
               ChunkPos pos = (ChunkPos)iterator.next();
               if (!desired.contains(pos)) {
                  ChunkForceRegistry.release(server, this.getUUID(), pos);
                  iterator.remove();
               }
            }

         }
      }
   }

   private void unforceAllChunks() {
      Level var2 = this.level();
      if (var2 instanceof ServerLevel server) {
         ChunkForceRegistry.releaseAll(server, this.getUUID(), this.forcedByUs);
         this.forcedByUs.clear();
      }
   }

   public void remove(Entity.RemovalReason reason) {
      Level var3 = this.level();
      if (var3 instanceof ServerLevel sl) {
         if (reason.shouldDestroy()) {
            this.removeHeads();
            this.despawnGrabTentacle();

            for(LivingEntity doomed : this.doomedMobs) {
               if (doomed.isAlive()) {
                  freeDoomedMob(doomed);
               }
            }

            this.doomedMobs.clear();
            StormRemovedPacket pkt = new StormRemovedPacket(this.getId());

            for(ServerPlayer p : PlayerLookup.level(sl)) {
               ServerPlayNetworking.send(p, pkt);
            }
         }

         if (reason != RemovalReason.UNLOADED_TO_CHUNK) {
            this.unforceAllChunks();
         }
      }

      super.remove(reason);
   }

   private BlockPos findSurfaceBlock() {
      BlockPos stormPos = this.blockPosition();
      int range = WitherStormConfigs.get(this.level()).pickupRange();

      for(int i = 0; i < 2; ++i) {
         int x = stormPos.getX() + this.random.nextInt(range * 2) - range;
         int z = stormPos.getZ() + this.random.nextInt(range * 2) - range;
         BlockPos surfacePos = this.level().getHeightmapPos(WitherStormConfigs.get(this.level()).groundHeightmap(), new BlockPos(x, 0, z)).below();
         BlockState state = this.level().getBlockState(surfacePos);
         if (!state.isAir() && state.getFluidState().isEmpty()) {
            return surfacePos;
         }
      }

      return null;
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      output.putDouble("Phase", this.phase);
      output.putInt("SubGrowth", this.subGrowth);
      output.putLong("SpawnAnimGameTime", (Long)this.entityData.get(SPAWN_ANIM_GAME_TIME));
      output.putInt("SpawnFreezeTicks", this.spawnFreezeTicks);
      output.putInt("SpawnFreezeTotalTicks", this.spawnFreezeTotalTicks);
      output.putBoolean("SpawnWailPending", this.spawnWailPending);
      if (this.ultimateTargetUUID != null) {
         output.putString("UltimateTarget", this.ultimateTargetUUID.toString());
      }

      for(int i = 0; i < 3; ++i) {
         output.putString("Head" + i, this.headUUIDs[i] != null ? this.headUUIDs[i].toString() : "");
      }

      output.putBoolean("UltimateTargetLocked", this.ultimateTargetLocked);
      output.putString("MoveMode", this.moveMode.name());
      output.putInt("ChaseTimer", this.chaseTimer);
      output.putInt("DistractionTimer", this.distractionTimer);
      output.putInt("DistractionTicksLeft", this.distractionTicksLeft);
      output.putDouble("DistractX", this.distractX);
      output.putDouble("DistractZ", this.distractZ);
      output.putLong("Phase5Elapsed", this.elapsedSince((Long)this.entityData.get(PHASE5_ANIM_GAME_TIME)));
      output.putLong("Phase58Elapsed", this.elapsedSince((Long)this.entityData.get(PHASE58_ANIM_GAME_TIME)));
      StringBuilder forcedChunks = new StringBuilder();

      for(ChunkPos pos : this.forcedByUs) {
         if (forcedChunks.length() > 0) {
            forcedChunks.append(",");
         }

         forcedChunks.append(pos.x()).append(":").append(pos.z());
      }

      output.putString("ForcedChunks", forcedChunks.toString());

      for(int i = 0; i < 2; ++i) {
         if (this.severedUUIDs[i] != null) {
            output.putString("Severed" + i, this.severedUUIDs[i].toString());
         }
      }

   }

   private void updateUltimateTarget() {
      if (--this.ultimateTargetCooldown <= 0) {
         this.ultimateTargetCooldown = 1200;
         if (!this.ultimateTargetLocked || this.ultimateTargetUUID == null) {
            if (this.ultimateTargetUUID != null) {
               Player currentTarget = this.level().getPlayerByUUID(this.ultimateTargetUUID);
               if (this.isTargetable(currentTarget)) {
                  return;
               }
            }

            Player closest = this.nearestTargetable();
            if (closest != null) {
               this.ultimateTargetUUID = closest.getUUID();
               this.entityData.set(ULTIMATE_TARGET_UUID, this.ultimateTargetUUID.toString());
            } else {
               this.ultimateTargetUUID = null;
               this.entityData.set(ULTIMATE_TARGET_UUID, "");
            }

         }
      }
   }

   private Player getUltimateTarget() {
      if (this.ultimateTargetUUID == null) {
         return null;
      } else {
         Level var2 = this.level();
         if (var2 instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)var2;
            return server.getServer().getPlayerList().getPlayer(this.ultimateTargetUUID);
         } else {
            return null;
         }
      }
   }

   private boolean isTargetable(Player p) {
      return p != null && p.isAlive() && !p.isSpectator();
   }

   private List<Player> targetablePlayers() {
      List<Player> out = new ArrayList();

      for(Player p : this.level().players()) {
         if (this.isTargetable(p)) {
            out.add(p);
         }
      }

      return out;
   }

   private Player nearestTargetable() {
      Player best = null;
      double bestSqr = Double.MAX_VALUE;

      for(Player p : this.level().players()) {
         if (this.isTargetable(p)) {
            double d = p.distanceToSqr(this);
            if (d < bestSqr) {
               bestSqr = d;
               best = p;
            }
         }
      }

      return best;
   }

   private boolean stormCanSee(Player p) {
      Vec3 from = this.getBoundingBox().getCenter();
      BlockHitResult hit = this.level().clip(new ClipContext(from, p.getEyePosition(), Block.COLLIDER, Fluid.NONE, this));
      return hit.getType() == Type.MISS;
   }

   private Vec3 aimAt(Vec3 point) {
      if (point == null) {
         return this.lastFacePoint;
      } else {
         double dx = point.x - this.getX();
         double dz = point.z - this.getZ();
         double d2 = dx * dx + dz * dz;
         if (d2 > (this.faceRingHold ? (double)100.0F : (double)36.0F)) {
            this.faceRingHold = false;
            this.lastFacePoint = point;
            return point;
         } else {
            this.faceRingHold = true;
            return this.lastFacePoint;
         }
      }
   }

   private Vec3 resolveMoveGoal(WitherStormWorldConfig cfg) {
      if (this.effectiveNatural(cfg)) {
         return this.naturalGoal();
      } else if (cfg.targetingMode == 4) {
         return this.structureGoal();
      } else if (!this.anyTargetableExists()) {
         return null;
      } else {
         Vec3 var10000;
         switch (cfg.targetingMode) {
            case 2:
               Player n = this.nearestTargetable();
               var10000 = n != null ? n.position() : null;
               break;
            case 3:
               var10000 = this.groupGoal();
               break;
            case 4:
               var10000 = this.structureGoal();
               break;
            default:
               Player u = this.getUltimateTarget();
               Player nn = this.nearestTargetable();
               var10000 = this.isTargetable(u) ? u.position() : (nn != null ? nn.position() : null);
         }

         return var10000;
      }
   }

   private boolean anyTargetableExists() {
      for(Player p : this.level().players()) {
         if (this.isTargetable(p)) {
            return true;
         }
      }

      return false;
   }

   private boolean effectiveNatural(WitherStormWorldConfig cfg) {
      if (cfg.targetingMode == 4) {
         return false;
      } else {
         return cfg.targetingMode == 1 || !this.anyTargetableExists();
      }
   }

   private LivingEntity resolveFaceEntity(WitherStormWorldConfig cfg) {
      if (!this.effectiveNatural(cfg)) {
         if (cfg.targetingMode == 4) {
            return this.structureDistracted ? this.nearestTargetable() : null;
         } else {
            return this.nearestTargetable();
         }
      } else {
         return this.naturalPrey != null && this.isValidPrey(this.naturalPrey) ? this.naturalPrey : null;
      }
   }

   private Vec3 naturalGoal() {
      if (this.naturalPrey != null && !this.isValidPrey(this.naturalPrey)) {
         this.naturalPrey = null;
         if (this.random.nextFloat() < 0.45F) {
            this.naturalWander = this.randomFarPoint();
            this.setChasing(this.random.nextFloat() < 0.35F);
         }
      }

      if (this.naturalWander != null && this.flatDistanceTo(this.naturalWander) < (double)28.0F) {
         this.naturalWander = null;
         this.setChasing(false);
         this.naturalRepickAt = 0;
      }

      if (this.naturalPrey == null && this.naturalWander == null && this.tickCount >= this.naturalRepickAt) {
         this.naturalRepickAt = this.tickCount + 40;
         this.naturalPrey = this.pickNaturalPrey();
         if (this.naturalPrey == null) {
            this.naturalWander = this.randomFarPoint();
         }
      }

      return this.naturalPrey != null ? this.naturalPrey.position() : this.naturalWander;
   }

   private Vec3 naturalFacePoint() {
      return this.naturalPrey != null && this.isValidPrey(this.naturalPrey) ? this.naturalPrey.position() : this.naturalWander;
   }

   private boolean isValidPrey(LivingEntity e) {
      return e.isAlive() && !e.isRemoved() && e.level() == this.level() && !this.isDoomed(e) && !WitheredMobs.isWithered(e) && e.distanceToSqr(this) < (double)36864.0F;
   }

   private double flatDistanceTo(Vec3 p) {
      return p.multiply((double)1.0F, (double)0.0F, (double)1.0F).distanceTo(this.position().multiply((double)1.0F, (double)0.0F, (double)1.0F));
   }

   private LivingEntity pickNaturalPrey() {
      List<LivingEntity> found = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate((double)96.0F), (ex) -> ex != this && !(ex instanceof Player) && !(ex instanceof WitherStormEntity) && ex.isAlive() && !ex.isRemoved() && !this.isDoomed(ex) && !WitheredMobs.isWithered(ex) && this.level().canSeeSky(ex.blockPosition()));
      LivingEntity best = null;
      double bestSq = Double.MAX_VALUE;

      for(LivingEntity e : found) {
         double d = e.distanceToSqr(this);
         if (d < bestSq) {
            bestSq = d;
            best = e;
         }
      }

      return best;
   }

   private Vec3 randomFarPoint() {
      double ang = this.random.nextDouble() * Math.PI * (double)2.0F;
      double dist = (double)200.0F + this.random.nextDouble() * (double)320.0F;
      return new Vec3(this.getX() + Math.cos(ang) * dist, this.getY(), this.getZ() + Math.sin(ang) * dist);
   }

   private Vec3 structureGoal() {
      Level var2 = this.level();
      if (!(var2 instanceof ServerLevel server)) {
         return null;
      } else {
         ArrayList<Player> var9 = new ArrayList();

         for(Player p : this.targetablePlayers()) {
            if (p.distanceToSqr(this) < (double)9216.0F) {
               var9.add(p);
            }
         }

         if (this.structureTarget != null && !this.structureArrived && this.distanceToSqr(Vec3.atCenterOf(this.structureTarget)) < (double)1600.0F) {
            this.structureArrived = true;
            this.structuresVisited.add(this.structureTarget.asLong());
            if (this.structuresVisited.size() > 64) {
               this.structuresVisited.clear();
            }
         }

         if (this.structureTarget != null && --this.structureDwell <= 0) {
            this.structureTarget = null;
            this.structureArrived = false;
         }

         this.structureDistracted = var9.size() >= 2;
         if (!this.structureDistracted) {
            if (this.structureTarget != null) {
               return Vec3.atCenterOf(this.structureTarget);
            } else if (this.tickCount < this.structureSearchAt) {
               return this.naturalGoal();
            } else {
               this.structureSearchAt = this.tickCount + 400;
               BlockPos found = this.findNextStructure(server);
               if (found == null) {
                  return this.naturalGoal();
               } else {
                  this.structureTarget = found;
                  this.structureDwell = 3600;
                  return Vec3.atCenterOf(found);
               }
            }
         } else {
            double x = (double)0.0F;
            double z = (double)0.0F;

            for(Player p : var9) {
               x += p.getX();
               z += p.getZ();
            }

            return new Vec3(x / (double)var9.size(), this.getY(), z / (double)var9.size());
         }
      }
   }

   private BlockPos findNextStructure(ServerLevel server) {
      Registry<Structure> registry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
      Optional<HolderSet.Named<Structure>> tag = registry.get(STORM_TARGETS);
      List<Holder<Structure>> wanted = new ArrayList();
      if (tag.isPresent()) {
         HolderSet.Named<Structure> named = (HolderSet.Named<Structure>)tag.get();
         Objects.requireNonNull(wanted);
         named.forEach(wanted::add);
      }

      if (wanted.isEmpty()) {
         return null;
      } else {
         HolderSet.Direct<Structure> set = HolderSet.direct(wanted);
         BlockPos here = this.blockPosition();
         BlockPos nearest = this.searchFrom(server, set, here);
         if (nearest != null && !this.structuresVisited.contains(nearest.asLong())) {
            return nearest;
         } else {
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;

            for(int i = 0; i < 4; ++i) {
               double ang = (double)i * (Math.PI / 2D);
               BlockPos from = here.offset((int)(Math.cos(ang) * (double)640.0F), 0, (int)(Math.sin(ang) * (double)640.0F));
               BlockPos hit = this.searchFrom(server, set, from);
               if (hit != null && !this.structuresVisited.contains(hit.asLong())) {
                  double d = hit.distSqr(here);
                  if (d < bestDist) {
                     bestDist = d;
                     best = hit;
                  }
               }
            }

            return best;
         }
      }
   }

   private BlockPos searchFrom(ServerLevel server, HolderSet<Structure> set, BlockPos from) {
      Pair<BlockPos, Holder<Structure>> hit = server.getChunkSource().getGenerator().findNearestMapStructure(server, set, from, 96, false);
      return hit == null ? null : (BlockPos)hit.getFirst();
   }

   private Vec3 groupGoal() {
      List<Player> current = new ArrayList();
      boolean anyVisible = false;

      for(UUID id : this.groupMembers) {
         Player p = this.level().getPlayerByUUID(id);
         if (this.isTargetable(p)) {
            current.add(p);
            if (this.stormCanSee(p)) {
               anyVisible = true;
            }
         }
      }

      if (anyVisible) {
         this.groupHiddenTicks = 0;
      } else {
         ++this.groupHiddenTicks;
      }

      if (!current.isEmpty() && this.groupHiddenTicks < 80) {
         return centroid(current);
      } else {
         List<Player> all = this.targetablePlayers();
         if (all.isEmpty()) {
            return null;
         } else {
            List<Player> best = null;

            for(Player seed : all) {
               List<Player> group = new ArrayList();

               for(Player p : all) {
                  if (p.distanceToSqr(seed) <= (double)784.0F) {
                     group.add(p);
                  }
               }

               boolean visible = group.stream().anyMatch(this::stormCanSee);
               if (visible && (best == null || group.size() > best.size())) {
                  best = group;
               }
            }

            if (best == null) {
               best = List.of((Player)all.get(0));
            }

            this.groupMembers.clear();

            for(Player p : best) {
               this.groupMembers.add(p.getUUID());
            }

            this.groupHiddenTicks = 0;
            return centroid(best);
         }
      }
   }

   private static Vec3 centroid(List<Player> players) {
      double x = (double)0.0F;
      double y = (double)0.0F;
      double z = (double)0.0F;

      for(Player p : players) {
         x += p.getX();
         y += p.getY();
         z += p.getZ();
      }

      int n = players.size();
      return new Vec3(x / (double)n, y / (double)n, z / (double)n);
   }

   public int getTargetingMode() {
      return this.level() instanceof ServerLevel ? WitherStormConfigs.get(this.level()).targetingMode : 0;
   }

   protected void readAdditionalSaveData(ValueInput input) {
      this.loadingFromSave = true;
      this.phase = input.getDoubleOr("Phase", (double)0.0F);
      this.subGrowth = input.getIntOr("SubGrowth", 0);
      this.phase4 = this.phase >= (double)4.0F;
      String uuid = input.getStringOr("UltimateTarget", "");
      if (!uuid.isEmpty()) {
         this.ultimateTargetUUID = UUID.fromString(uuid);
      }

      for(int i = 0; i < 3; ++i) {
         String headUuid = input.getStringOr("Head" + i, "");
         this.headUUIDs[i] = headUuid.isEmpty() ? null : UUID.fromString(headUuid);
      }

      this.ultimateTargetLocked = input.getBooleanOr("UltimateTargetLocked", false);

      try {
         this.moveMode = WitherStormEntity.MoveMode.valueOf(input.getStringOr("MoveMode", "FOLLOW"));
      } catch (IllegalArgumentException var13) {
         this.moveMode = WitherStormEntity.MoveMode.FOLLOW;
      }

      this.chaseTimer = input.getIntOr("ChaseTimer", -1);
      this.distractionTimer = input.getIntOr("DistractionTimer", -1);
      this.distractionTicksLeft = input.getIntOr("DistractionTicksLeft", 0);
      this.distractX = input.getDoubleOr("DistractX", (double)0.0F);
      this.distractZ = input.getDoubleOr("DistractZ", (double)0.0F);
      this.restoreAnimStamp(PHASE5_ANIM_GAME_TIME, input.getLongOr("Phase5Elapsed", -1L));
      this.restoreAnimStamp(PHASE58_ANIM_GAME_TIME, input.getLongOr("Phase58Elapsed", -1L));
      long longDone = this.level().getGameTime() - 1200L;
      if (this.phase >= (double)5.0F && (Long)this.entityData.get(PHASE5_ANIM_GAME_TIME) < 0L) {
         this.entityData.set(PHASE5_ANIM_GAME_TIME, longDone);
      }

      if (this.phase >= 5.8 && (Long)this.entityData.get(PHASE58_ANIM_GAME_TIME) < 0L) {
         this.entityData.set(PHASE58_ANIM_GAME_TIME, longDone);
      }

      this.forcedByUs.clear();

      for(int i = 0; i < 2; ++i) {
         String sev = input.getStringOr("Severed" + i, "");

         try {
            this.severedUUIDs[i] = sev.isEmpty() ? null : UUID.fromString(sev);
         } catch (IllegalArgumentException var12) {
            this.severedUUIDs[i] = null;
         }

         this.severedGrace[i] = 0;
      }

      String forcedChunks = input.getStringOr("ForcedChunks", "");
      if (!forcedChunks.isEmpty()) {
         for(String part : forcedChunks.split(",")) {
            try {
               String[] xz = part.split(":");
               this.forcedByUs.add(new ChunkPos(Integer.parseInt(xz[0]), Integer.parseInt(xz[1])));
            } catch (RuntimeException var11) {
            }
         }
      }

      this.entityData.set(PHASE_DATA, (float)this.phase);
      this.entityData.set(PHASE4_DATA, this.phase4);
      this.entityData.set(SUBGROWTH_DATA, this.subGrowth);
      if (this.ultimateTargetUUID != null) {
         this.entityData.set(ULTIMATE_TARGET_UUID, this.ultimateTargetUUID.toString());
      }

      this.entityData.set(SPAWN_ANIM_GAME_TIME, input.getLongOr("SpawnAnimGameTime", -1L));
      this.spawnFreezeTicks = input.getIntOr("SpawnFreezeTicks", 0);
      this.spawnFreezeTotalTicks = input.getIntOr("SpawnFreezeTotalTicks", this.spawnFreezeTicks);
      this.spawnWailPending = input.getBooleanOr("SpawnWailPending", false);
      if (this.phase4) {
         this.headSpawnGraceTicks = 100;
      }

      this.updateBossBar();
      this.loadingFromSave = false;
   }

   static {
      PHASE_DATA = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.FLOAT);
      PHASE4_DATA = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.BOOLEAN);
      SIEGE_STAGE = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.INT);
      SUBGROWTH_DATA = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.INT);
      SPAWN_ANIM_GAME_TIME = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.LONG);
      COLLAPSE_GAME_TIME = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.LONG);
      PHASE5_ANIM_GAME_TIME = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.LONG);
      MINI_HEAD_ANIM_GAME_TIME = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.LONG);
      TENTACLE_ANIM_GAME_TIME = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.LONG);
      PHASE58_ANIM_GAME_TIME = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.LONG);
      ULTIMATE_TARGET_UUID = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.STRING);
      TARGET_YAW = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.FLOAT);
      TARGET_PITCH = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.FLOAT);
      PHASE_4_DIMENSIONS = EntityDimensions.scalable(10.0F, 30.0F);
      BODY_ROLL = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.FLOAT);
      SNATCH_ID = SynchedEntityData.defineId(WitherStormEntity.class, EntityDataSerializers.INT);
      HEAD_OFFSETS = new Vec3[]{new Vec3((double)2.0F, (double)13.5F, 10.2), new Vec3((double)-6.5F, (double)20.0F, 14.57), new Vec3((double)11.5F, (double)19.0F, (double)8.5F)};
      HEAD_YAW_OFFSETS = new float[]{0.0F, 0.0F, 0.0F};
      HEAD_REST_YAW = new float[]{0.0F, 26.0F, -26.0F};
      HEAD_REST_ROLL = new float[]{0.0F, -13.0F, 13.0F};
      HEAD_SPAWN_ORDER = new int[]{1, 0, 2};
      lastCarveStatus = "";
      CAUGHT_ENDERMEN = new HashMap();
      caughtTickedAt = -1L;
      AURA_MOTE = new DustParticleOptions(0, 1.1F);
      PRE_P4_TURN_TIME = new float[]{0.28F, 0.55F, 0.95F, 1.6F};
      PRE_P4_SPEED = new double[]{(double)1.0F, 0.72, 0.48, 0.3};
      PRE_P4_HEAD_RANGE = new float[]{180.0F, 180.0F, 22.0F, 0.0F};
      STORM_TARGETS = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("dabywitherstormmod", "storm_targets"));
   }

   public static enum MoveMode {
      FOLLOW,
      CHASING,
      DISTRACTED;

      // $FF: synthetic method
      private static MoveMode[] $values() {
         return new MoveMode[]{FOLLOW, CHASING, DISTRACTED};
      }
   }
}
