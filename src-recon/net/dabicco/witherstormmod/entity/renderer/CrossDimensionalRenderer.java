package net.dabicco.witherstormmod.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dabicco.witherstormmod.client.StormSkins;
import net.dabicco.witherstormmod.entity.model.ModEntityModelLayers;
import net.dabicco.witherstormmod.entity.model.Tentacle;
import net.dabicco.witherstormmod.entity.state.CrossDimensionalRenderState;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CrossDimensionalRenderer extends EntityRenderer<net.dabicco.witherstormmod.entity.CrossDimensionalEntity, CrossDimensionalRenderState> {
   private static final int FULL_BRIGHT = 15728880;
   private static final float MODEL_YAW_FIX = -90.0F;
   private final Tentacle tentacleModel;

   public CrossDimensionalRenderer(Context context) {
      super(context);
      this.tentacleModel = new Tentacle(context.bakeLayer(ModEntityModelLayers.TENTACLE));
   }

   public CrossDimensionalRenderState createRenderState() {
      return new CrossDimensionalRenderState();
   }

   protected AABB getBoundingBoxForCulling(net.dabicco.witherstormmod.entity.CrossDimensionalEntity entity) {
      return super.getBoundingBoxForCulling(entity).inflate(64.0);
   }

   public void extractRenderState(net.dabicco.witherstormmod.entity.CrossDimensionalEntity entity, CrossDimensionalRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.probeState = entity.getProbeState();
      state.extend = entity.getExtend();
      state.yaw = entity.getYRot();
      Vec3 tip = entity.getInterpolatedTip(partialTick);
      state.tipX = tip.x;
      state.tipY = tip.y;
      state.tipZ = tip.z;
      state.timeTicks = (float)(entity.level().getGameTime() % 100000L) + partialTick;
   }

   public void submit(CrossDimensionalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      if (!(state.extend <= 0.01F)) {
         double tipLen = Math.sqrt(state.tipX * state.tipX + state.tipZ * state.tipZ);
         float tipYaw = tipLen > 0.05 ? (float)(Mth.atan2(state.tipZ, state.tipX) * (180.0 / Math.PI)) - 90.0F : state.yaw;
         float yawLean = Mth.clamp(Mth.degreesDifference(state.yaw, tipYaw), -22.0F, 22.0F);
         float aimYaw = state.yaw + yawLean;
         float pitch = (float)(-(Mth.atan2(state.tipY, Math.max(tipLen, 0.01)) * (180.0 / Math.PI)));
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - aimYaw + -90.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(Mth.clamp(pitch, -70.0F, 70.0F)));
         poseStack.scale(-1.0F, -1.0F, 1.0F);
         this.tentacleModel.revealProgress = Mth.clamp(state.extend, 0.0F, 1.0F);
         WitherStormRenderState scratch = new WitherStormRenderState();
         scratch.stormId = 7777;
         scratch.idleTimeTicks = state.timeTicks;
         scratch.bodyRot = 0.0F;
         scratch.bodyRoll = 0.0F;
         scratch.xRot = 0.0F;
         scratch.phase = 5.0;
         collector.submitModel(
            this.tentacleModel, scratch, poseStack, RenderTypes.entityCutout(StormSkins.phase6Body()), 15728880, OverlayTexture.NO_OVERLAY, -1, null, 0, null
         );
         poseStack.popPose();
      }
   }
}
