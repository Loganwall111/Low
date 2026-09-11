package net.dabicco.witherstormmod.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.Arrays;
import java.util.HashMap;
import net.dabicco.witherstormmod.BowelsPortal;
import net.dabicco.witherstormmod.client.CubeReveal;
import net.dabicco.witherstormmod.client.FoglessRenderTypes;
import net.dabicco.witherstormmod.client.GroundProbe;
import net.dabicco.witherstormmod.client.PreviewScene;
import net.dabicco.witherstormmod.client.StormDebris;
import net.dabicco.witherstormmod.client.StormGlowRenderer;
import net.dabicco.witherstormmod.client.StormShadowMap;
import net.dabicco.witherstormmod.client.TentacleMeasure;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.entity.CollapseAnim;
import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.dabicco.witherstormmod.entity.model.HugeAssBackModel;
import net.dabicco.witherstormmod.entity.model.HunchbackGrowth;
import net.dabicco.witherstormmod.entity.model.ModEntityModelLayers;
import net.dabicco.witherstormmod.entity.model.StormCoverModel;
import net.dabicco.witherstormmod.entity.model.Tentacle;
import net.dabicco.witherstormmod.entity.model.WitherCommandBlock;
import net.dabicco.witherstormmod.entity.model.WitherStormDevourer;
import net.dabicco.witherstormmod.entity.model.WitherStormGrowth5;
import net.dabicco.witherstormmod.entity.model.WitherStormHead;
import net.dabicco.witherstormmod.entity.model.WitherStormP4;
import net.dabicco.witherstormmod.entity.model.WitherStormTentacles5;
import net.dabicco.witherstormmod.entity.model.WitherStormTentaclesDevourer;
import net.dabicco.witherstormmod.entity.state.WitherStormHeadRenderState;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.dabicco.witherstormmod.client.StormSkins;

public class WitherStormRenderer extends MobRenderer<WitherStormEntity, WitherStormRenderState, EntityModel<WitherStormRenderState>> {
   private final StormCoverModel coverModel;
   private final WitherCommandBlock commandBlockModel;
   private final WitherStormP4 stormP4Model;
   private final WitherStormDevourer devourerModel;
   private final WitherStormTentaclesDevourer devourerTentaclesModel;
   private final WitherStormHead miniHeadModel;
   private final WitherStormHead miniHeadGlowModel;
   private final WitherStormGrowth5 growth5Model;
   private final Tentacle frontTentacleModel;
   private final HunchbackGrowth hunchbackModel;
   private final HugeAssBackModel hugeAssBackModel;
   private final WitherStormTentacles5 tentacles5Model;
   private final WitherStormGrowth5 growth5BackModel;
   private final HugeAssBackModel hugeAssBackMirrorModel;
   private final WitherCommandBlock commandBlockShadowModel;
   private final WitherStormP4 stormP4ShadowModel;
   private final WitherStormDevourer devourerShadowModel;
   private final WitherStormTentacles5 tentacles5ShadowModel;
   private final WitherStormTentaclesDevourer devourerTentaclesShadowModel;
   private final HunchbackGrowth hunchbackShadowModel;
   private final PreviewHeads previewHeads;
   private final Tentacle frontTentacleShadowModel;
   private static final float MINI_HEAD_Y = 3.05F;
   private static final float MINI_HEAD_SCALE = 1.55F;
   private static final float MINI_HEAD_BITE_ANGLE = 42.0F;
   private static final int FULL_BRIGHT = 15728880;
   private static final double COLLAPSE_PIVOT_Y = 17.0;
   public static final double COLLAPSE_PIVOT_Y_PUBLIC = 17.0;
   private static final double CHANGEOVER_FROM = 3.82;
   private static final float SHAKE_AMPLITUDE = 0.55F;
   private static final float HATCH_TICKS = 55.0F;
   private static final float HATCH_MIN = 0.22F;
   private static final float WHITEOUT_FADE_TICKS = 38.0F;
   public static final double COLLAPSE_DROP_Y = 18.5;
   private static final double GROWTH5_START_PHASE = 4.5;
   private static final float GROWTH5_SEED_SCALE = 0.06F;
   private static final float GROWTH5_MIRROR_PUSH = 0.85F;
   private static final float GROWTH5_MIRROR_UP = 0.0F;
   private static final float GROWTH5_MIRROR_LAG = 0.1F;
   private static final int HUGEBACK_GROUP_DEPTH = 3;
   private static final float HUGEBACK_MIRROR_LAG = 0.06F;
   private static final double HUGEBACK_START = 5.4;
   private static final double HUGEBACK_FULL = 5.8;
   private static final HashMap<Integer, float[]> GROWTH5_DISPLAY = new HashMap<>();
   private static final HashMap<Integer, float[]> YAW_SMOOTH = new HashMap<>();
   private static final int PORTAL_R = 148;
   private static final int PORTAL_G = 58;
   private static final int PORTAL_B = 224;
   private static final int PORTAL_ALPHA_SEALED = 105;
   private static final int PORTAL_ALPHA_OPEN = 170;
   private static final Identifier PORTAL_TEXTURE = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/tractor_beam.png");
   private SubmitNodeCollector frameCollector;
   private static final float FRONT_TENTACLE_SPROUT_TICKS = 70.0F;
   private static final float FRONT_TENTACLE_UP = -1.1F;
   private static final float FRONT_TENTACLE_FORWARD = -0.5F;
   private static final float FRONT_TENTACLE_SIZE = 0.5F;
   private static final float FRONT_TENTACLE_TURN_IN = -14.0F;
   private static final float FRONT_TENTACLE_ANGLE_OUT = 36.0F;
   private boolean previewShadowPass = false;
   private static final float[] NIGHT_LAYER_SIZES = new float[]{0.45F, 0.75F, 1.0F};
   private static final float[] NIGHT_LAYER_ALPHAS = new float[]{0.85F, 0.45F, 0.22F};
   private static final int[][] NIGHT_LAYER_COLOURS = new int[][]{{240, 232, 255}, {210, 185, 255}, {178, 140, 255}};
   private static final double NIGHT_RADIUS_BASE = 26.0;
   private static final double NIGHT_RADIUS_PER_PHASE = 9.0;

   private Identifier bodyTexture(WitherStormRenderState state) {
      return this.getTextureLocation(state);
   }

   protected AABB getBoundingBoxForCulling(WitherStormEntity entity) {
      return super.getBoundingBoxForCulling(entity).inflate(512.0);
   }

   protected int getModelTint(WitherStormRenderState state) {
      if (this.previewShadowPass) {
         return 940578856;
      } else {
         int argb = super.getModelTint(state);
         float w = Mth.clamp(state.changeover, 0.0F, 1.0F);
         if (w <= 0.001F) {
            return argb;
         } else {
            int a = argb >>> 24 & 0xFF;
            int r = (int)Mth.lerp(w, (float)(argb >> 16 & 0xFF), 255.0F);
            int g = (int)Mth.lerp(w, (float)(argb >> 8 & 0xFF), 255.0F);
            int b = (int)Mth.lerp(w, (float)(argb & 0xFF), 255.0F);
            return a << 24 | r << 16 | g << 8 | b;
         }
      }
   }

   private static void applyChangeoverShake(PoseStack poseStack, WitherStormRenderState state) {
      float w = Mth.clamp(state.changeover, 0.0F, 1.0F);
      if (!(w <= 0.001F)) {
         float amp = 0.55F * w * w * (float)DabyWSClientConfig.changeoverShake;
         float t = state.idleTimeTicks;
         poseStack.translate(Mth.sin((double)(t * 3.1F)) * amp, Mth.sin((double)(t * 4.7F + 1.3F)) * amp * 0.6F, Mth.sin((double)(t * 2.6F + 2.9F)) * amp);
      }
   }

   static void readGround(GroundProbe probe, Level level, WitherStormRenderState state, double x, double y, double z, double radius) {
      float down = CollapseAnim.down(state.collapseTicks);
      if (down <= 0.001F) {
         state.slopePitch = 0.0F;
         state.slopeRoll = 0.0F;
         Arrays.fill(state.groundBias, 0.0F);
      } else {
         probe.update(level, x, y, z, state.bodyRot, radius);
         state.slopePitch = probe.pitch * down;
         state.slopeRoll = probe.roll * down;

         for (int i = 0; i < state.groundBias.length; i++) {
            state.groundBias[i] = probe.bias(i, 14.0F) * down;
         }
      }
   }

   private static float growth5SmoothPerSec() {
      return (float)DabyWSClientConfig.growthSmoothRate;
   }

   private static float hugeBackProgress(double phase) {
      if (phase >= 6.0) {
         return 0.0F;
      } else if (phase >= 5.8) {
         return 1.0F;
      } else if (phase <= 5.4) {
         return 0.0F;
      } else {
         float p = (float)((phase - 5.4) / 0.39999999999999947);
         return p * p * (3.0F - 2.0F * p);
      }
   }

   protected RenderType getRenderType(WitherStormRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
      if (this.previewShadowPass) {
         return this.pieceType(this.getTextureLocation(state));
      } else {
         boolean ours = FoglessRenderTypes.fogless() || FoglessRenderTypes.reverseShading();
         return ours && showBody && !translucent
            ? FoglessRenderTypes.bodyCutout(this.getTextureLocation(state))
            : super.getRenderType(state, showBody, translucent, showOutline);
      }
   }

   static void applyCollapse(PoseStack poseStack, WitherStormRenderState state, double pivot) {
      float topple = CollapseAnim.bodyPitch(state.collapseTicks);
      if (topple != 0.0F) {
         float flat = CollapseAnim.down(state.collapseTicks);
         poseStack.translate(0.0, -18.5 * (double)flat, 0.0);
         poseStack.translate(0.0, pivot, 0.0);
         poseStack.mulPose(Axis.XP.rotationDegrees(-topple));
         poseStack.translate(0.0, -pivot, 0.0);
      }
   }

   private void submitMirroredGroups(
      SubmitNodeCollector collector, PoseStack poseStack, ModelPart root, WitherStormRenderState state, float progress, int groupDepth
   ) {
      if (!(progress <= 0.0F)) {
         int light = state.lightCoords;
         int tint = this.pieceTint();
         poseStack.pushPose();
         poseStack.translate(0.0, -0.0, 0.0);
         collector.submitCustomGeometry(poseStack, this.pieceType(this.bodyTexture(state)), (pose, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().pose().set(pose.pose());
            local.last().normal().set(pose.normal());
            CubeReveal.renderMirroredGroups(root, local, consumer, light, OverlayTexture.NO_OVERLAY, tint, progress, true, 0.85F, groupDepth);
         });
         poseStack.popPose();
      }
   }

   private void submitScaled(PoseStack poseStack, ModelPart root, float grow, Runnable body) {
      float[] b = CubeReveal.bounds(root);
      float cx = (b[0] + b[1]) * 0.5F;
      float cy = (b[2] + b[3]) * 0.5F;
      float anchorZ = b[4];
      float s = Math.max(grow, 0.02F);
      poseStack.pushPose();
      poseStack.translate(cx, cy, anchorZ);
      poseStack.scale(s, s, s);
      poseStack.translate(-cx, -cy, -anchorZ);
      body.run();
      poseStack.popPose();
   }

   private void submitMirroredBack(SubmitNodeCollector collector, PoseStack poseStack, ModelPart root, WitherStormRenderState state, float progress) {
      poseStack.pushPose();
      this.applyBackMirror(poseStack, root);
      this.submitCubeReveal(collector, poseStack, root, state, progress, true);
      poseStack.popPose();
   }

   private void applyBackMirror(PoseStack poseStack, ModelPart root) {
      float[] b = CubeReveal.bounds(root);
      float zCentre = (b[4] + b[5]) * 0.5F;
      float depth = b[5] - b[4];
      float zPlane = zCentre + depth * 0.5F * 0.85F;
      float xCentre = (b[0] + b[1]) * 0.5F;
      poseStack.translate(0.0, -0.0, 0.0);
      poseStack.translate(0.0, 0.0, (double)zPlane);
      poseStack.scale(1.0F, 1.0F, -1.0F);
      poseStack.translate(0.0, 0.0, (double)(-zPlane));
      poseStack.translate((double)xCentre, 0.0, 0.0);
      poseStack.scale(-1.0F, 1.0F, 1.0F);
      poseStack.translate((double)(-xCentre), 0.0, 0.0);
   }

   private void submitCubeReveal(SubmitNodeCollector collector, PoseStack poseStack, ModelPart root, WitherStormRenderState state, float progress) {
      this.submitCubeReveal(collector, poseStack, root, state, progress, false);
   }

   private void submitCubeReveal(
      SubmitNodeCollector collector, PoseStack poseStack, ModelPart root, WitherStormRenderState state, float progress, boolean dropSheets
   ) {
      int light = state.lightCoords;
      int tint = this.pieceTint();
      collector.submitCustomGeometry(
         poseStack, this.pieceType(this.bodyTexture(state), !dropSheets && DabyWSClientConfig.stormBackfaceCull), (pose, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().pose().set(pose.pose());
            local.last().normal().set(pose.normal());
            CubeReveal.render(root, local, consumer, light, OverlayTexture.NO_OVERLAY, tint, progress, true, dropSheets);
         }
      );
   }

   private static float yawSmoothTime() {
      return (float)DabyWSClientConfig.yawSmoothTime;
   }

   private static float yawSnapDegrees() {
      return (float)DabyWSClientConfig.yawSnapDegrees;
   }

   public static float smoothBodyYaw(int stormId, float target) {
      long nowMs = Util.getMillis();
      float[] slot = YAW_SMOOTH.get(stormId);
      if (slot == null) {
         slot = new float[]{target, 0.0F, (float)nowMs};
         YAW_SMOOTH.put(stormId, slot);
         if (YAW_SMOOTH.size() > 64) {
            YAW_SMOOTH.clear();
         }

         return target;
      } else {
         float diff = Mth.degreesDifference(slot[0], target);
         if (Math.abs(diff) > yawSnapDegrees()) {
            slot[0] = target;
            slot[1] = 0.0F;
            slot[2] = (float)nowMs;
            return target;
         } else {
            float dt = Mth.clamp(((float)nowMs - slot[2]) / 1000.0F, 0.0F, 0.25F);
            slot[2] = (float)nowMs;
            if (dt <= 0.0F) {
               return slot[0];
            } else {
               float omega = 2.0F / Math.max(0.02F, yawSmoothTime());
               float x = omega * dt;
               float expo = 1.0F / (1.0F + x + 0.48F * x * x + 0.235F * x * x * x);
               float change = -diff;
               float temp = (slot[1] + omega * change) * dt;
               slot[1] = (slot[1] - omega * temp) * expo;
               slot[0] = Mth.wrapDegrees(target + (change + temp) * expo);
               return slot[0];
            }
         }
      }
   }

   private static float debrisSettle(double phase) {
      double full = 6.15;
      double start = full - 0.05;
      if (phase < start) {
         return 0.0F;
      } else {
         float t = (float)Mth.clamp((phase - start) / (full - start), 0.0, 1.0);
         return t * t * (3.0F - 2.0F * t);
      }
   }

   private static float growth5Target(double phase) {
      if (phase >= 5.0) {
         return 1.0F;
      } else {
         return phase <= 4.5 ? 0.0F : (float)((phase - 4.5) / 0.5);
      }
   }

   private static float growth5Smoothed(int stormId, double phase) {
      float target = growth5Target(phase);
      long nowMs = Util.getMillis();
      float[] slot = GROWTH5_DISPLAY.get(stormId);
      if (slot == null) {
         slot = new float[]{target, (float)nowMs};
         GROWTH5_DISPLAY.put(stormId, slot);
         if (GROWTH5_DISPLAY.size() > 64) {
            GROWTH5_DISPLAY.clear();
         }

         return target;
      } else {
         float dt = Mth.clamp(((float)nowMs - slot[1]) / 1000.0F, 0.0F, 0.25F);
         slot[1] = (float)nowMs;
         float a = 1.0F - (float)Math.exp((double)(-dt * growth5SmoothPerSec()));
         slot[0] += (target - slot[0]) * a;
         return slot[0];
      }
   }

   public WitherStormRenderer(Context context) {
      super(context, new WitherCommandBlock(context.bakeLayer(ModEntityModelLayers.WITHER_STORM)), 1.0F);
      this.coverModel = new StormCoverModel(context.bakeLayer(ModEntityModelLayers.STORM_COVER));
      this.commandBlockModel = new WitherCommandBlock(context.bakeLayer(ModEntityModelLayers.WITHER_STORM));
      this.stormP4Model = new WitherStormP4(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_P4));
      this.miniHeadModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD));
      this.miniHeadGlowModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_GLOW));
      this.growth5Model = new WitherStormGrowth5(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_GROWTH5));
      this.frontTentacleModel = new Tentacle(context.bakeLayer(ModEntityModelLayers.TENTACLE));
      this.hunchbackModel = new HunchbackGrowth(context.bakeLayer(ModEntityModelLayers.HUNCHBACK_GROWTH));
      this.hugeAssBackModel = new HugeAssBackModel(context.bakeLayer(ModEntityModelLayers.HUGE_ASS_BACK));
      this.devourerModel = new WitherStormDevourer(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_DEVOURER));
      this.devourerTentaclesModel = new WitherStormTentaclesDevourer(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_TENTACLES_DEVOURER));
      this.tentacles5Model = new WitherStormTentacles5(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_TENTACLES5));
      this.growth5BackModel = new WitherStormGrowth5(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_GROWTH5));
      this.hugeAssBackMirrorModel = new HugeAssBackModel(context.bakeLayer(ModEntityModelLayers.HUGE_ASS_BACK));
      this.previewHeads = new PreviewHeads(context);
      this.commandBlockShadowModel = new WitherCommandBlock(context.bakeLayer(ModEntityModelLayers.WITHER_STORM));
      this.stormP4ShadowModel = new WitherStormP4(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_P4));
      this.devourerShadowModel = new WitherStormDevourer(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_DEVOURER));
      this.tentacles5ShadowModel = new WitherStormTentacles5(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_TENTACLES5));
      this.devourerTentaclesShadowModel = new WitherStormTentaclesDevourer(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_TENTACLES_DEVOURER));
      this.hunchbackShadowModel = new HunchbackGrowth(context.bakeLayer(ModEntityModelLayers.HUNCHBACK_GROWTH));
      this.frontTentacleShadowModel = new Tentacle(context.bakeLayer(ModEntityModelLayers.TENTACLE));
   }

   private void submitCover(PoseStack poseStack, SubmitNodeCollector collector, WitherStormRenderState state) {
      if (!(state.phase < 6.8)) {
         this.coverModel.applyPeel(BowelsPortal.platesFor(state.phase));
         RenderType type = this.getRenderType(state, true, false, false);
         if (type != null) {
            collector.submitModelPart(this.coverModel.back(), poseStack, type, state.lightCoords, OverlayTexture.NO_OVERLAY, null);

            for (int i = 1; i <= 4; i++) {
               ModelPart plate = this.coverModel.cover(i);
               if (plate.visible) {
                  collector.submitModelPart(plate, poseStack, type, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
               }
            }
         }
      }
   }

   private void submitPortal(PoseStack poseStack, SubmitNodeCollector collector, WitherStormRenderState state) {
      Vec3[] c = state.portalCorners;
      if (c != null && !this.previewShadowPass) {
         int alpha = state.portalOpen ? 170 : 105;
         collector.submitCustomGeometry(poseStack, FoglessRenderTypes.entityTranslucentEmissive(PORTAL_TEXTURE), (pose, buffer) -> {
            portalQuad(pose, buffer, c[0], c[1], c[2], c[3], alpha);
            portalQuad(pose, buffer, c[3], c[2], c[1], c[0], alpha);
         });
      }
   }

   private static void portalQuad(Pose pose, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int alpha) {
      portalVertex(pose, buffer, a, 0.0F, 0.0F, alpha);
      portalVertex(pose, buffer, b, 1.0F, 0.0F, alpha);
      portalVertex(pose, buffer, c, 1.0F, 1.0F, alpha);
      portalVertex(pose, buffer, d, 0.0F, 1.0F, alpha);
   }

   private static void portalVertex(Pose pose, VertexConsumer buffer, Vec3 at, float u, float v, int alpha) {
      buffer.addVertex(pose, (float)at.x, (float)at.y, (float)at.z)
         .setColor(148, 58, 224, alpha)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   public WitherStormRenderState createRenderState() {
      return new WitherStormRenderState();
   }

   public void extractRenderState(WitherStormEntity entity, WitherStormRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.phase4 = entity.isPhase4();
      state.devourer = entity.isDevourer();
      state.phase = entity.getPhase();
      if (entity.hasCover()) {
         state.portalCorners = BowelsPortal.corners(entity.getYRot());
         state.portalOpen = BowelsPortal.open(state.phase);
      } else {
         state.portalCorners = null;
         state.portalOpen = false;
      }

      state.nightFactor = StormGlowRenderer.nightFactor(entity.level());
      state.worldX = Mth.lerp((double)partialTick, entity.xOld, entity.getX());
      state.worldY = Mth.lerp((double)partialTick, entity.yOld, entity.getY());
      state.worldZ = Mth.lerp((double)partialTick, entity.zOld, entity.getZ());
      state.bodyRoll = entity.getBodyRoll();
      state.bodyRoll = state.bodyRoll * (float)DabyWSClientConfig.bodyBankGain;
      state.xRot = state.xRot * (float)DabyWSClientConfig.bodyLeanGain;
      state.underSiege = entity.isUnderSiege();
      state.siegeProgress = (float)entity.siegeProgress() / 100.0F;
      state.stormId = entity.getId();
      long collapse = entity.getCollapseGameTime();
      state.collapseTicks = collapse < 0L ? -1.0F : (float)(entity.level().getGameTime() - collapse) + partialTick;
      state.slopePitch = 0.0F;
      state.slopeRoll = 0.0F;
      Arrays.fill(state.groundBias, 0.0F);
      state.bodyRot = smoothBodyYaw(entity.getId(), state.bodyRot);
      Vec3 vel = entity.getDeltaMovement();
      state.velX = vel.x;
      state.velY = vel.y;
      state.velZ = vel.z;
      long gt = entity.level().getGameTime();
      float now = (float)(gt % 100000L) + partialTick;
      if (CollapseAnim.down(state.collapseTicks) >= 0.999F) {
         state.idleTimeTicks = 0.0F;
      } else {
         state.idleTimeTicks = now;
      }

      long phase5Start = entity.getPhase5AnimGameTime();
      state.phase5ElapsedTicks = phase5Start < 0L ? -1.0F : (float)(gt - phase5Start) + partialTick;
      long phase58Start = entity.getPhase58AnimGameTime();
      state.phase58ElapsedTicks = phase58Start < 0L ? -1.0F : (float)(gt - phase58Start) + partialTick;
      long spawnStart = entity.getSpawnAnimGameTime();
      if (state.phase4 && spawnStart >= 0L) {
         state.spawnElapsedTicks = (float)(gt - spawnStart) + partialTick;
         state.playingSpawnAnimation = state.spawnElapsedTicks >= 0.0F && state.spawnElapsedTicks < 80.0F;
      } else {
         state.spawnElapsedTicks = Float.MAX_VALUE;
         state.playingSpawnAnimation = false;
      }

      if (!state.phase4) {
         state.changeover = (float)Mth.clamp((state.phase - 3.82) / 0.18000000000000016, 0.0, 1.0);
         state.hatch = 1.0F;
      } else {
         float since = state.spawnElapsedTicks;
         float grow = since < 0.0F ? 1.0F : Mth.clamp(since / 55.0F, 0.0F, 1.0F);
         state.hatch = 0.22F + 0.78F * grow * grow * (3.0F - 2.0F * grow);
         state.changeover = since < 0.0F ? 0.0F : 1.0F - Mth.clamp(since / 38.0F, 0.0F, 1.0F);
      }

      long tentacleStamp = entity.getFrontTentacleAnimGameTime();
      state.frontTentacleElapsedTicks = tentacleStamp < 0L ? -1.0F : (float)(gt - tentacleStamp) + partialTick;
      long miniStamp = entity.getMiniHeadAnimGameTime();
      state.miniHeadElapsedTicks = miniStamp < 0L ? -1.0F : (float)(gt - miniStamp) + partialTick;
      float[] headXRots = entity.getHeadXRots();
      float[] headYRots = entity.getHeadYRots();

      for (int i = 0; i < 2; i++) {
         state.headXRot[i] = headXRots[i];
         state.headYRot[i] = headYRots[i];
      }

      state.snatchActive = false;
      int snatchId = entity.getSnatchId();
      if (snatchId >= 0 && entity.level().getEntity(snatchId) instanceof LivingEntity victim) {
         double sx = Mth.lerp((double)partialTick, entity.xOld, entity.getX());
         double sy = Mth.lerp((double)partialTick, entity.yOld, entity.getY());
         double sz = Mth.lerp((double)partialTick, entity.zOld, entity.getZ());
         state.snatchActive = true;
         state.snatchRelX = Mth.lerp((double)partialTick, victim.xOld, victim.getX()) - sx;
         state.snatchRelY = Mth.lerp((double)partialTick, victim.yOld, victim.getY()) - sy;
         state.snatchRelZ = Mth.lerp((double)partialTick, victim.zOld, victim.getZ()) - sz;
      }
   }

   public void submit(WitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
      poseStack.pushPose();
      applyChangeoverShake(poseStack, state);
      this.frameCollector = submitNodeCollector;
      this.submitPortal(poseStack, submitNodeCollector, state);

      try {
         if (state.preview != null && !this.previewShadowPass) {
            state.preview.submitGround(poseStack, submitNodeCollector);
            state.preview.submitSun(poseStack, submitNodeCollector);
            if (state.preview.castShadow) {
               this.previewShadowPass = true;
               state.preview.pushShadow(poseStack);

               try {
                  this.submit(state, poseStack, submitNodeCollector, camera);
               } finally {
                  poseStack.popPose();
                  this.previewShadowPass = false;
               }
            }
         }

         if (state.preview != null) {
            this.submitPreviewHeads(state, poseStack, submitNodeCollector);
         }

         this.model = (EntityModel)(this.previewShadowPass
            ? (state.devourer ? this.devourerShadowModel : (state.phase4 ? this.stormP4ShadowModel : this.commandBlockShadowModel))
            : (state.devourer ? this.devourerModel : (state.phase4 ? this.stormP4Model : this.commandBlockModel)));
         if (state.phase4) {
            float debris58 = state.devourer ? -1.0F : state.phase58ElapsedTicks;
            if (CollapseAnim.down(state.collapseTicks) < 0.999F && !this.previewShadowPass) {
               StormDebris.submit(
                  poseStack,
                  submitNodeCollector,
                  state.idleTimeTicks,
                  state.lightCoords,
                  state.phase5ElapsedTicks,
                  debris58,
                  state.stormId,
                  state.devourer,
                  debrisSettle(state.phase),
                  state.preview != null,
                  (float)state.phase
               );
            }

            if (state.preview == null) {
               this.submitNightLight(state, poseStack, submitNodeCollector, camera);
            }

            if (state.phase >= 4.5) {
               this.submitGrowth5(state, poseStack, submitNodeCollector);
            }

            if (state.phase >= 5.0) {
               this.submitTentacles5(state, poseStack, submitNodeCollector);
            }
         } else {
            this.submitHunchback(state, poseStack, submitNodeCollector);
            if (state.phase >= 3.0 && state.phase < 4.0) {
               this.submitFrontTentacle(state, poseStack, submitNodeCollector);
            }

            if (!this.previewShadowPass) {
               StormDebris.submitEarly(poseStack, submitNodeCollector, state.idleTimeTicks, state.lightCoords, (float)state.phase, state.stormId);
            }
         }

         super.submit(state, poseStack, submitNodeCollector, camera);
      } finally {
         poseStack.popPose();
      }
   }

   private void submitHunchback(WitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
      if (!(state.phase < 0.2)) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
         applyCollapse(poseStack, state, 17.0);
         if (state.bodyRoll != 0.0F) {
            poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
         }

         poseStack.scale(-2.0F, -2.0F, 2.0F);
         poseStack.translate(0.0, -1.501, 0.0);
         submitNodeCollector.submitModel(
            this.previewShadowPass ? this.hunchbackShadowModel : this.hunchbackModel,
            state,
            poseStack,
            this.pieceType(StormSkins.phase4()),
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            this.pieceTint(),
            null,
            0,
            null
         );
         poseStack.popPose();
      }
   }

   private void submitFrontTentacle(WitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
      float since = state.frontTentacleElapsedTicks;
      if (!(since < 0.0F)) {
         Tentacle tentacle = this.previewShadowPass ? this.frontTentacleShadowModel : this.frontTentacleModel;
         tentacle.revealProgress = 1.0F;
         tentacle.sprout = Mth.clamp(since / 70.0F, 0.0F, 1.0F);
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
         applyCollapse(poseStack, state, 17.0);
         if (state.bodyRoll != 0.0F) {
            poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
         }

         poseStack.scale(-2.0F, -2.0F, 2.0F);
         poseStack.translate(0.0, -1.501, 0.0);
         poseStack.translate(0.0, -1.1F, -0.5);
         poseStack.mulPose(Axis.YP.rotationDegrees(-14.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(36.0F));
         poseStack.scale(FRONT_TENTACLE_SIZE, FRONT_TENTACLE_SIZE, FRONT_TENTACLE_SIZE);
         submitNodeCollector.submitModel(
            tentacle, state, poseStack, this.pieceType(StormSkins.phase4()), state.lightCoords, OverlayTexture.NO_OVERLAY, this.pieceTint(), null, 0, null
         );
         poseStack.popPose();
      }
   }

   private RenderType pieceType(Identifier texture) {
      return this.previewShadowPass ? PreviewScene.shadowType() : FoglessRenderTypes.bodyCutout(texture);
   }

   private RenderType pieceType(Identifier texture, boolean cull) {
      return this.previewShadowPass ? PreviewScene.shadowType() : FoglessRenderTypes.bodyCutout(texture, cull);
   }

   private int pieceTint() {
      return this.previewShadowPass ? 940578856 : -1;
   }

   private void submitPreviewHeads(WitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
      int count = previewHeadCount(state.phase);
      if (count > 0) {
         boolean early = !state.phase4;
         float scale = early ? 1.35F : (6.0F + (float)Math.min(Math.max(state.phase - 4.0, 0.0), 3.0) * 0.5F) * (state.devourer ? 1.06F : 1.0F);
         Vec3[] offsets = new Vec3[count];
         float[] scales = new float[count];
         float[] restYaw = new float[count];
         float[] restRoll = new float[count];

         for (int i = 0; i < count; i++) {
            restYaw[i] = early ? 0.0F : WitherStormEntity.restYawFor(i);
            restRoll[i] = early ? 0.0F : WitherStormEntity.restRollFor(i);
            offsets[i] = early ? new Vec3(0.0, 3.05, 0.14) : WitherStormEntity.headOffset(i, state.devourer);
            scales[i] = scale;
         }

         this.previewHeads
            .submit(
               state.preview,
               poseStack,
               collector,
               this.previewShadowPass,
               offsets,
               scales,
               restYaw,
               restRoll,
               state.bodyRot,
               state.idleTimeTicks,
               state.devourer,
               early,
               StormSkins.phase4()
            );
      }
   }

   private static int previewHeadCount(double phase) {
      if (phase < 2.0) {
         return 0;
      } else {
         return phase < 4.0 ? 1 : 3;
      }
   }

   private void submitNightLight(WitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      float night = Math.min(state.nightFactor, 1.0F);
      float strength = (float)DabyWSClientConfig.stormGlowStrength;
      if (!(night <= 0.01F) && !(strength <= 0.01F)) {
         float pulse = 0.92F + 0.08F * Mth.sin((double)(state.idleTimeTicks * 0.05F));
         float amount = night * strength * pulse;
         Vec3 view = new Vec3(state.worldX - camera.pos.x, state.worldY - camera.pos.y, state.worldZ - camera.pos.z);
         double dist = view.length();
         if (!(dist < 1.0E-4)) {
            view = view.scale(1.0 / dist);
            double radius = 26.0 + 9.0 * Math.max(0.0, state.phase - 4.0);
            Vec3 centre = new Vec3(0.0, radius * 0.55, 0.0).add(view.scale(radius * 0.9));
            StormGlowRenderer.submitLight(poseStack, collector, centre, view, radius, NIGHT_LAYER_SIZES, NIGHT_LAYER_ALPHAS, NIGHT_LAYER_COLOURS, amount);
         }
      }
   }

   private void submitGrowth5(WitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
      float GROWTH5_SCALE = 4.0F;
      float HUGE_BACK_SCALE = 1.72F;
      float GROWTH5_WIDE = 1.0F;
      float GROWTH5_UP = 4.0F;
      float GROWTH5_FORWARD = -6.5F;
      float GROWTH5_YAW_TRIM = 0.0F;
      boolean filled = DabyWSClientConfig.filledSubphases;
      float grow;
      if (filled) {
         float p = growth5Smoothed(state.stormId, state.phase);
         grow = p * p * (3.0F - 2.0F * p);
      } else {
         grow = state.phase >= 5.0 ? 1.0F : 0.0F;
      }

      boolean scaled = filled && DabyWSClientConfig.scaledSubphaseGrowth;
      if (!(grow <= 0.0F) || !((filled ? hugeBackProgress(state.phase) : 0.0F) <= 0.0F) || filled || !(state.phase < 5.0)) {
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot + 0.0F));
         applyCollapse(poseStack, state, 17.0);
         poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
         poseStack.translate(0.0, 4.0, -6.5);
         poseStack.scale(-4.0F, -4.0F, 4.0F);
         if (state.preview == null && StormShadowMap.wanted()) {
            StormShadowMap.capture(poseStack, this.growth5Model.root());
            if (DabyWSClientConfig.flatbackFlipFix) {
               poseStack.pushPose();
               this.applyBackMirror(poseStack, this.growth5BackModel.root());
               StormShadowMap.capture(poseStack, this.growth5BackModel.root());
               poseStack.popPose();
            }

            if (hugeBackProgress(state.phase) > 0.0F) {
               poseStack.pushPose();
               poseStack.scale(1.72F, 1.72F, 1.72F);
               StormShadowMap.capture(poseStack, this.hugeAssBackModel.root());
               poseStack.pushPose();
               this.applyBackMirror(poseStack, this.hugeAssBackMirrorModel.root());
               StormShadowMap.capture(poseStack, this.hugeAssBackMirrorModel.root());
               poseStack.popPose();
               poseStack.popPose();
            }
         }

         if (scaled) {
            this.submitScaled(
               poseStack, this.growth5Model.root(), grow, () -> this.submitCubeReveal(submitNodeCollector, poseStack, this.growth5Model.root(), state, 1.0F)
            );
         } else {
            this.submitCubeReveal(submitNodeCollector, poseStack, this.growth5Model.root(), state, grow);
         }

         boolean flip = DabyWSClientConfig.flatbackFlipFix && !this.previewShadowPass;
         if (flip) {
            if (scaled) {
               this.submitScaled(
                  poseStack,
                  this.growth5BackModel.root(),
                  grow,
                  () -> this.submitMirroredBack(submitNodeCollector, poseStack, this.growth5BackModel.root(), state, 1.0F)
               );
            } else {
               float mirrorGrow = Math.max(0.0F, grow - 0.1F) / 0.9F;
               this.submitMirroredBack(submitNodeCollector, poseStack, this.growth5BackModel.root(), state, mirrorGrow);
            }
         }

         float hugeGrow = filled ? hugeBackProgress(state.phase) : (state.phase >= 5.8 && state.phase < 6.0 ? 1.0F : 0.0F);
         if (hugeGrow > 0.0F) {
            poseStack.pushPose();
            poseStack.scale(1.72F, 1.72F, 1.72F);
            if (scaled) {
               this.submitScaled(
                  poseStack,
                  this.hugeAssBackModel.root(),
                  hugeGrow,
                  () -> this.submitCubeReveal(submitNodeCollector, poseStack, this.hugeAssBackModel.root(), state, 1.0F)
               );
            } else {
               this.submitCubeReveal(submitNodeCollector, poseStack, this.hugeAssBackModel.root(), state, hugeGrow);
            }

            if (flip) {
               if (scaled) {
                  this.submitScaled(
                     poseStack,
                     this.hugeAssBackMirrorModel.root(),
                     hugeGrow,
                     () -> this.submitMirroredGroups(submitNodeCollector, poseStack, this.hugeAssBackMirrorModel.root(), state, 1.0F, 3)
                  );
               } else {
                  float hugeMirror = Math.max(0.0F, hugeGrow - 0.06F) / 0.94F;
                  this.submitMirroredGroups(submitNodeCollector, poseStack, this.hugeAssBackMirrorModel.root(), state, hugeMirror, 3);
               }
            }

            poseStack.popPose();
         }

         poseStack.popPose();
      }
   }

   private void submitTentacles5(WitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
      float UP = 6.0F;
      float BACK = 1.25F;
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
      applyCollapse(poseStack, state, 17.0);
      poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
      if (state.devourer) {
         float DEV_UP = -9.0F;
         float DEV_BACK = -2.0F;
         float DEV_SCALE = 0.98F;
         poseStack.translate(0.0, -9.0, -2.0);
         poseStack.scale(-0.98F, -0.98F, 0.98F);
         WitherStormTentaclesDevourer devTentacles = this.previewShadowPass ? this.devourerTentaclesShadowModel : this.devourerTentaclesModel;
         devTentacles.setupAnim(state);
         if (state.preview == null) {
            StormShadowMap.capture(poseStack, this.devourerTentaclesModel);
         }

         TentacleMeasure.measure(poseStack, this.devourerTentaclesModel, state.stormId);
         submitNodeCollector.submitModel(
            devTentacles, state, poseStack, this.pieceType(StormSkins.devourer()), state.lightCoords, OverlayTexture.NO_OVERLAY, this.pieceTint(), null, 0, null
         );
      } else {
         poseStack.translate(0.0, 6.0, 1.25);
         float ts = 6.25F;
         poseStack.scale(-ts, -ts, ts);
         if (state.preview == null) {
            StormShadowMap.capture(poseStack, this.tentacles5Model);
         }

         TentacleMeasure.measure(poseStack, this.tentacles5Model, state.stormId);
         submitNodeCollector.submitModel(
            this.previewShadowPass ? this.tentacles5ShadowModel : this.tentacles5Model,
            state,
            poseStack,
            this.pieceType(this.bodyTexture(state)),
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            this.pieceTint(),
            null,
            0,
            null
         );
      }

      poseStack.popPose();
   }

   private void submitMiniHead(WitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
      WitherStormHeadRenderState headState = new WitherStormHeadRenderState();
      headState.lightCoords = state.lightCoords;
      headState.idleTimeTicks = state.idleTimeTicks;
      headState.spawnElapsedTicks = Float.MAX_VALUE;
      headState.fireElapsedTicks = -1.0F;
      headState.hurtElapsedTicks = -1.0F;
      headState.roarElapsedTicks = -1.0F;
      headState.jawAngle = 0.0F;
      headState.damaged = false;
      float worldYaw = state.bodyRot + state.yRot;
      poseStack.pushPose();
      if (state.bodyRoll != 0.0F) {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
         applyCollapse(poseStack, state, 17.0);
         poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
         poseStack.mulPose(Axis.YP.rotationDegrees(-(180.0F - state.bodyRot)));
      }

      poseStack.translate(0.0, 3.05F, 0.0);
      poseStack.mulPose(Axis.YP.rotationDegrees(-worldYaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
      poseStack.scale(1.55F, 1.55F, 1.55F);
      poseStack.translate(0.0, -1.25, 0.0);
      submitNodeCollector.submitModel(
         this.miniHeadModel,
         headState,
         poseStack,
         FoglessRenderTypes.bodyCutout(StormSkins.phase4()),
         state.lightCoords,
         OverlayTexture.NO_OVERLAY,
         -1,
         null,
         0,
         null
      );
      submitNodeCollector.submitModel(
         this.miniHeadGlowModel,
         headState,
         poseStack,
         RenderTypes.eyes(StormSkins.phase4()),
         15728880,
         OverlayTexture.NO_OVERLAY,
         WitherStormHeadRenderer.glowTint(),
         null,
         0,
         null
      );
      poseStack.popPose();
   }

   protected void scale(WitherStormRenderState state, PoseStack poseStack) {
      if (state.phase4) {
         poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
         poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
         float topple = CollapseAnim.bodyPitch(state.collapseTicks);
         float flat = CollapseAnim.down(state.collapseTicks);
         if (flat > 0.0F) {
            poseStack.translate(0.0, 18.5 * (double)flat, 0.0);
         }

         if (topple != 0.0F) {
            poseStack.translate(0.0, -17.0, 0.0);
            poseStack.mulPose(Axis.XP.rotationDegrees(topple));
            poseStack.translate(0.0, 17.0, 0.0);
         }

         float phaseGrowth = state.devourer ? 1.35F : 2.0F + (float)Math.min(Math.max(state.phase - 4.0, 0.0), 2.0) * 0.3F;
         float bodyScale = phaseGrowth * state.hatch;
         poseStack.scale(bodyScale, bodyScale, bodyScale);
         poseStack.translate(0.0, 6.0, 0.0);
      } else {
         if (state.bodyRoll != 0.0F) {
            poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
         }

         if (state.xRot != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
         }

         poseStack.scale(2.0F, 2.0F, 2.0F);
      }

      if (state.phase4 && state.preview == null && StormShadowMap.wanted()) {
         poseStack.pushPose();
         poseStack.translate(0.0F, -1.501F, 0.0F);
         this.model.setupAnim(state);
         StormShadowMap.capture(poseStack, this.model);
         poseStack.popPose();
      }

      if (this.frameCollector != null) {
         poseStack.pushPose();
         poseStack.translate(0.0F, -1.501F, 0.0F);
         this.submitCover(poseStack, this.frameCollector, state);
         poseStack.popPose();
      }
   }

   public Identifier getTextureLocation(WitherStormRenderState state) {
      if (state.devourer) {
         return StormSkins.devourer();
      } else {
         return state.phase4 ? StormSkins.phase4() : StormSkins.legacy();
      }
   }
}
