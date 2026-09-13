package net.dabicco.witherstormmod.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.dabicco.witherstormmod.client.FoglessRenderTypes;
import net.dabicco.witherstormmod.client.PreviewScene;
import net.dabicco.witherstormmod.client.StormDebris;
import net.dabicco.witherstormmod.client.StormSkins;
import net.dabicco.witherstormmod.entity.model.ModEntityModelLayers;
import net.dabicco.witherstormmod.entity.model.SeveredWitherStorm;
import net.dabicco.witherstormmod.entity.state.SeveredWitherStormRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;

public class SeveredWitherStormRenderer
   extends MobRenderer<net.dabicco.witherstormmod.entity.SeveredWitherStormEntity, SeveredWitherStormRenderState, SeveredWitherStorm> {
   private static final float BODY_SCALE = 1.1009175F;
   private static final double COLLAPSE_PIVOT_Y = 12.0;
   private static final float DEBRIS_SCALE = 0.42F;
   private static final float DEBRIS_X = 0.0F;
   private static final float DEBRIS_Y = 7.0F;
   private static final float DEBRIS_Z = 3.0F;
   private final SeveredWitherStorm bodyModel;
   private final SeveredWitherStorm shadowModel;
   private final PreviewHeads previewHeads;
   private static final float SLOPE_DEADBAND = 4.0F;
   private static final float SLOPE_MAX = 22.0F;
   private static final Map<Long, float[]> SLOPE_EASE = new HashMap<>();
   private static final int SLOPE_ROLL_SLOT = 16777216;
   private boolean previewShadowPass = false;

   public SeveredWitherStormRenderer(Context context) {
      super(context, new SeveredWitherStorm(context.bakeLayer(ModEntityModelLayers.SEVERED_WITHER_STORM)), 1.0F);
      this.bodyModel = (SeveredWitherStorm)(Object)(Object)this.model;
      this.shadowModel = new SeveredWitherStorm(context.bakeLayer(ModEntityModelLayers.SEVERED_WITHER_STORM));
      this.previewHeads = new PreviewHeads(context);
   }

   protected AABB getBoundingBoxForCulling(net.dabicco.witherstormmod.entity.SeveredWitherStormEntity entity) {
      return super.getBoundingBoxForCulling(entity).inflate(512.0);
   }

   public SeveredWitherStormRenderState createRenderState() {
      return new SeveredWitherStormRenderState();
   }

   public void extractRenderState(net.dabicco.witherstormmod.entity.SeveredWitherStormEntity entity, SeveredWitherStormRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.phase = entity.getPhase();
      state.mirrored = entity.isMirrored();
      state.bodyRoll = settleSlope(entity.getId(), false, entity.getBodyRoll());
      state.stormId = entity.getId();
      state.bodyRot = WitherStormRenderer.smoothBodyYaw(entity.getId(), state.bodyRot);
      long gt = entity.level().getGameTime();
      if (net.dabicco.witherstormmod.entity.CollapseAnim.down(state.collapseTicks) >= 0.999F) {
         state.idleTimeTicks = 0.0F;
      } else {
         state.idleTimeTicks = (float)(gt % 100000L) + partialTick;
      }

      state.bodyLight = state.lightCoords;
      state.collapseTicks = entity.hostCollapseTicks(partialTick);
      state.droop = net.dabicco.witherstormmod.entity.CollapseAnim.droop(state.collapseTicks);
      state.side = entity.getSide();
      if (state.droop <= 0.001F) {
         state.slopePitch = 0.0F;
         state.slopeRoll = 0.0F;
         Arrays.fill(state.groundBias, 0.0F);
      } else {
         entity.groundProbe.update(entity.level(), entity.getX(), entity.getY(), entity.getZ(), state.bodyRot, 14.0);
         state.slopePitch = settleSlope(entity.getId(), true, softSlope(entity.groundProbe.pitch) * state.droop);
         state.slopeRoll = settleSlope(entity.getId() | 16777216, false, softSlope(entity.groundProbe.roll) * state.droop);

         for (int i = 0; i < state.groundBias.length; i++) {
            state.groundBias[i] = entity.groundProbe.bias(i, 10.0F) * state.droop;
         }
      }
   }

   private static float softSlope(float degrees) {
      float mag = Math.abs(degrees) - 4.0F;
      return mag <= 0.0F ? 0.0F : Math.signum(degrees) * Math.min(mag, 22.0F);
   }

   private static float settleSlope(int entityId, boolean pitch, float target) {
      long key = (long)entityId << 2 | (pitch ? 1L : 0L);
      long now = Util.getMillis();
      float[] slot = SLOPE_EASE.get(key);
      if (slot == null) {
         slot = new float[]{target, (float)now};
         if (SLOPE_EASE.size() > 32) {
            SLOPE_EASE.clear();
         }

         SLOPE_EASE.put(key, slot);
         return target;
      } else {
         float dt = Mth.clamp(((float)now - slot[1]) / 1000.0F, 0.0F, 0.25F);
         slot[1] = (float)now;
         float ease = 1.0F - (float)Math.exp(-dt * 2.5F);
         slot[0] += (target - slot[0]) * ease;
         return slot[0];
      }
   }

   public void submit(SeveredWitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      if (state.preview != null && !this.previewShadowPass) {
         state.preview.submitSky(poseStack, collector);
         state.preview.submitGround(poseStack, collector);
         state.preview.submitSun(poseStack, collector);
         if (state.preview.castShadow) {
            this.previewShadowPass = true;
            state.preview.pushShadow(poseStack);

            try {
               this.submit(state, poseStack, collector, camera);
            } finally {
               poseStack.popPose();
               this.previewShadowPass = false;
            }
         }
      }

      this.model = this.previewShadowPass ? this.shadowModel : this.bodyModel;
      super.submit(state, poseStack, collector, camera);
      if (!this.previewShadowPass) {
         this.submitDebris(state, poseStack, collector);
      }

      if (state.preview != null) {
         this.previewHeads
            .submit(
               state.preview,
               poseStack,
               collector,
               this.previewShadowPass,
               net.dabicco.witherstormmod.entity.SeveredWitherStormEntity.previewHeadOffsets(state.mirrored),
               net.dabicco.witherstormmod.entity.SeveredWitherStormEntity.previewHeadScales(),
               new float[3],
               new float[3],
               state.bodyRot,
               state.idleTimeTicks,
               true,
               false,
               StormSkins.devourer()
            );
      }
   }

   private void submitDebris(SeveredWitherStormRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
      if (!(state.collapseTicks >= 0.0F) || !(state.droop >= 0.999F)) {
         poseStack.pushPose();
         float bodyYaw = 180.0F - state.bodyRot;
         poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
         poseStack.translate(state.mirrored ? -0.0F : 0.0F, 7.0F, 3.0F);
         poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
         poseStack.scale(0.42F, 0.42F, 0.42F);
         StormDebris.submitSeveredCloud(poseStack, collector, state.idleTimeTicks, state.bodyLight, state.stormId, 2.3809524F, state.preview != null);
         poseStack.popPose();
      }
   }

   protected void scale(SeveredWitherStormRenderState state, PoseStack poseStack) {
      poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
      float topple = net.dabicco.witherstormmod.entity.CollapseAnim.severedPitch(state.collapseTicks);
      float spin = net.dabicco.witherstormmod.entity.CollapseAnim.severedSpin(state.collapseTicks, state.side);
      float sideRoll = net.dabicco.witherstormmod.entity.CollapseAnim.severedRoll(state.collapseTicks, state.side);
      if (topple != 0.0F || spin != 0.0F) {
         poseStack.translate(0.0, -12.0, 0.0);
         poseStack.mulPose(Axis.YP.rotationDegrees(spin));
         poseStack.mulPose(Axis.XP.rotationDegrees(topple));
         poseStack.mulPose(Axis.ZN.rotationDegrees(sideRoll));
         poseStack.translate(0.0, 12.0, 0.0);
      }

      if (state.droop > 0.0F) {
         poseStack.translate(0.0, 2.0 * state.droop, 0.0);
      }

      poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
      poseStack.scale(1.1009175F, 1.1009175F, 1.1009175F);
      poseStack.translate(0.0, 6.0, 0.0);
      if (state.mirrored) {
         poseStack.scale(-1.0F, 1.0F, 1.0F);
      }
   }

   protected RenderType getRenderType(SeveredWitherStormRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
      if (this.previewShadowPass) {
         return PreviewScene.shadowType();
      } else {
         boolean ours = FoglessRenderTypes.fogless() || FoglessRenderTypes.reverseShading();
         return ours && showBody && !translucent
            ? FoglessRenderTypes.bodyCutout(this.getTextureLocation(state))
            : super.getRenderType(state, showBody, translucent, showOutline);
      }
   }

   public Identifier getTextureLocation(SeveredWitherStormRenderState state) {
      return StormSkins.devourer();
   }
}
