package net.dabicco.witherstormmod.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dabicco.witherstormmod.client.FoglessRenderTypes;
import net.dabicco.witherstormmod.client.StormSkins;
import net.dabicco.witherstormmod.entity.model.ModEntityModelLayers;
import net.dabicco.witherstormmod.entity.model.WitherStormTentacles5;
import net.dabicco.witherstormmod.entity.state.NetherScaleRenderState;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NetherScaleRenderer extends EntityRenderer<net.dabicco.witherstormmod.entity.NetherScaleEntity, NetherScaleRenderState> {
   private static final int FULL_BRIGHT = 15728880;
   private static final float VISUAL_LIFT = 70.0F;
   private static final float ROOT_OFFSET_X = 2.3125F;
   private static final float ROOT_OFFSET_Z = -0.5F;
   private static final float FINE_X = -0.25F;
   private static final float MAX_CURL = 0.09F;
   private final WitherStormTentacles5 tentaclesModel;

   public NetherScaleRenderer(Context context) {
      super(context);
      this.tentaclesModel = new WitherStormTentacles5(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_TENTACLES5));
      this.tentaclesModel.bigTentaclesOnly = true;
      this.tentaclesModel.singleBigTentacle = true;
      this.tentaclesModel.staticPose = true;
      this.tentaclesModel.staticPoseXRot = 0.0F;
      this.tentaclesModel.staticPoseZRot = (float) (Math.PI / 2);
   }

   public NetherScaleRenderState createRenderState() {
      return new NetherScaleRenderState();
   }

   protected AABB getBoundingBoxForCulling(net.dabicco.witherstormmod.entity.NetherScaleEntity entity) {
      return super.getBoundingBoxForCulling(entity).inflate(256.0);
   }

   public void extractRenderState(net.dabicco.witherstormmod.entity.NetherScaleEntity entity, NetherScaleRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.progress = entity.getProgress();
      state.scale = entity.getScale();
      state.entityId = entity.getId();
      Vec3 tip = entity.getTipOffset();
      state.tipX = tip.x;
      state.tipY = tip.y;
      state.tipZ = tip.z;
      double dx = entity.getX() - entity.xOld;
      double dz = entity.getZ() - entity.zOld;
      state.speed = (float)Math.sqrt(dx * dx + dz * dz);
      state.timeTicks = (float)(entity.level().getGameTime() % 100000L) + partialTick;
   }

   public void submit(NetherScaleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      if (!(state.progress <= 0.001F) && !(state.progress >= 0.999F)) {
         poseStack.pushPose();
         poseStack.translate(0.0, 70.0, 0.0);
         this.tentaclesModel.staticCurl = 0.09F * (float)Math.sin(state.progress * Math.PI);
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         float s = state.scale;
         poseStack.scale(-s, -s, s);
         poseStack.translate(-2.5625, 0.0, 0.5);
         WitherStormRenderState scratch = new WitherStormRenderState();
         scratch.stormId = state.entityId;
         scratch.idleTimeTicks = state.timeTicks;
         scratch.phase = 5.5;
         scratch.phase5ElapsedTicks = 400.0F;
         scratch.phase58ElapsedTicks = -1.0F;
         scratch.bodyRot = 0.0F;
         scratch.bodyRoll = 0.0F;
         scratch.xRot = 0.0F;
         scratch.lightCoords = 15728880;
         collector.submitModel(
            this.tentaclesModel, scratch, poseStack, FoglessRenderTypes.bodyCutout(StormSkins.phase6Body()), 15728880, OverlayTexture.NO_OVERLAY, -1, null, 0, null
         );
         poseStack.popPose();
      }
   }
}
