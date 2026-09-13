package net.dabicco.witherstormmod.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.dabicco.witherstormmod.client.ClientConfigCache;
import net.dabicco.witherstormmod.client.FoglessRenderTypes;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.dabicco.witherstormmod.client.ShaderPackCompat;
import net.dabicco.witherstormmod.client.StormBloom;
import net.dabicco.witherstormmod.client.StormShadowMap;
import net.dabicco.witherstormmod.client.StormSkins;
import net.dabicco.witherstormmod.client.TractorBeamRenderer;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.entity.model.ModEntityModelLayers;
import net.dabicco.witherstormmod.entity.model.WitherStormHead;
import net.dabicco.witherstormmod.entity.state.WitherStormHeadRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WitherStormHeadRenderer extends EntityRenderer<net.dabicco.witherstormmod.entity.WitherStormHeadEntity, WitherStormHeadRenderState> {
   private final WitherStormHead model;
   private final WitherStormHead glowModel;
   private final WitherStormHead teethBoostModel;
   private final WitherStormHead eyeGlowModel;
   private final WitherStormHead eyeBoostModel;
   private final WitherStormHead teethBloomModel;
   private final WitherStormHead eyeBloomModel;
   private final WitherStormHead bloomOccluderModel;
   private final WitherStormHead sceneEraseModel;
   private final WitherStormHead teethBloom2Model;
   private final WitherStormHead eyeBloom2Model;
   private static final int FULL_BRIGHT = 15728880;
   private static final float DEAD_JAW_SLACK = 24.0F;
   private static final float DEAD_POSE_TIME = 0.0F;
   public static final int GLOW_TINT = -3153665;
   private static boolean devourerTint;
   private static boolean earlyTint;
   public static final int DAMAGED_GLOW_TINT = -15066590;
   private static final Map<Integer, float[]> JAW_LAG = new HashMap<>();

   private static float shaderGlowGain() {
      if (!ShaderPackCompat.active()) {
         return 1.0F;
      } else {
         int level = (int)Math.round(DabyWSClientConfig.bloomStrength);
         switch (level) {
            case 0:
               return 0.75F;
            case 1:
               return 1.15F;
            case 2:
            default:
               return 1.55F;
            case 3:
               return 2.1F;
         }
      }
   }

   public static void setDevourerTint(boolean on) {
      devourerTint = on;
   }

   public static void setEarlyTint(boolean on) {
      earlyTint = on;
   }

   private static int earlyShift(int argb) {
      if (!earlyTint) {
         return argb;
      } else {
         int r = argb >> 16 & 0xFF;
         int g = argb >> 8 & 0xFF;
         int b = argb & 0xFF;
         r = (int)(r * 0.38F);
         g = (int)(g * 0.92F);
         b = (int)(b * 0.98F);
         return argb & 0xFF000000 | r << 16 | g << 8 | b;
      }
   }

   private static int devourerShift(int argb) {
      if (!devourerTint) {
         return argb;
      } else {
         int r = argb >> 16 & 0xFF;
         int g = argb >> 8 & 0xFF;
         int b = argb & 0xFF;
         r = (int)(r * 0.82F);
         g = (int)(g * 0.74F);
         b = (int)(b * 0.34F);
         return argb & 0xFF000000 | r << 16 | g << 8 | b;
      }
   }

   /** Body vertices stay neutral; phase colour belongs only to eyes/teeth FX. */
   private static int deadTint(float lit) {
      return -1;
   }

   public static int glowTint() {
      int r = (int)(Mth.clamp(DabyWSClientConfig.eyeColorR, 0.0, 1.0) * 255.0);
      int g = (int)(Mth.clamp(DabyWSClientConfig.eyeColorG, 0.0, 1.0) * 255.0);
      int b = (int)(Mth.clamp(DabyWSClientConfig.eyeColorB, 0.0, 1.0) * 255.0);
      r = (int)(r * 0.96F);
      g = Math.min(255, (int)(g * 1.02F));
      b = Math.min(255, (int)(b * 1.03F));
      return earlyShift(devourerShift(scaleTint(0xFF000000 | r << 16 | g << 8 | b, shaderGlowGain())));
   }

   public static int eyeTint() {
      int r = (int)(Mth.clamp(DabyWSClientConfig.beamColorR, 0.0, 1.0) * 255.0);
      int g = (int)(Mth.clamp(DabyWSClientConfig.beamColorG, 0.0, 1.0) * 255.0);
      int b = (int)(Mth.clamp(DabyWSClientConfig.beamColorB, 0.0, 1.0) * 255.0);
      int mx = Math.max(1, Math.max(r, Math.max(g, b)));
      float lift = 226.0F / mx;
      r = Math.min(255, (int)(r * lift) + 18);
      g = Math.min(255, (int)(g * lift) + 18);
      b = Math.min(255, (int)(b * lift) + 18);
      return earlyShift(devourerShift(scaleTint(0xFF000000 | r << 16 | g << 8 | b, shaderGlowGain())));
   }

   private static int scaleTint(int argb, float f) {
      int r = Math.min(255, (int)((argb >> 16 & 0xFF) * f));
      int g = Math.min(255, (int)((argb >> 8 & 0xFF) * f));
      int b = Math.min(255, (int)((argb & 0xFF) * f));
      return argb & 0xFF000000 | r << 16 | g << 8 | b;
   }

   public WitherStormHeadRenderer(Context context) {
      super(context);
      this.model = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD));
      this.glowModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_GLOW));
      this.teethBoostModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_GLOW));
      this.eyeGlowModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_EYE_GLOW));
      this.eyeBoostModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_EYE_GLOW));
      this.teethBloomModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_GLOW));
      this.eyeBloomModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_EYE_GLOW));
      this.sceneEraseModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD));
      this.bloomOccluderModel = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD));
      this.teethBloom2Model = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_GLOW));
      this.eyeBloom2Model = new WitherStormHead(context.bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_EYE_GLOW));
   }

   protected AABB getBoundingBoxForCulling(net.dabicco.witherstormmod.entity.WitherStormHeadEntity entity) {
      return super.getBoundingBoxForCulling(entity).inflate(512.0);
   }

   public WitherStormHeadRenderState createRenderState() {
      return new WitherStormHeadRenderState();
   }

   private static float jawLagGain() {
      return (float)DabyWSClientConfig.jawLagGain;
   }

   private static float jawLagMax() {
      return (float)DabyWSClientConfig.jawLagMax;
   }

   private static float jawLagCatchup() {
      return (float)DabyWSClientConfig.jawLagCatchup;
   }

   private static void updateJawLag(WitherStormHeadRenderState state, float yaw, float pitch, float roll) {
      float[] st = JAW_LAG.computeIfAbsent(state.headId, k -> new float[]{yaw, pitch, roll, 0.0F, 0.0F, 0.0F});
      float dYaw = Mth.degreesDifference(st[0], yaw);
      float dPitch = Mth.degreesDifference(st[1], pitch);
      float dRoll = Mth.degreesDifference(st[2], roll);
      st[0] = yaw;
      st[1] = pitch;
      st[2] = roll;
      float gain = jawLagGain();
      float max = jawLagMax();
      float catchup = jawLagCatchup();
      st[3] = Mth.clamp(st[3] - dYaw * gain, -max, max);
      st[4] = Mth.clamp(st[4] - dPitch * gain * 0.7F, -max, max);
      st[5] = Mth.clamp(st[5] - dRoll * gain * 1.2F, -max, max);
      st[3] -= st[3] * catchup;
      st[4] -= st[4] * catchup;
      st[5] -= st[5] * catchup;
      state.jawLagYaw = st[3];
      state.jawLagPitch = st[4];
      state.jawLagRoll = st[5];
      if (JAW_LAG.size() > 64) {
         JAW_LAG.keySet().removeIf(id -> id != state.headId);
      }
   }

   public void extractRenderState(net.dabicco.witherstormmod.entity.WitherStormHeadEntity entity, WitherStormHeadRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.headId = entity.getId();
      state.beamScale = entity.beamScale();
      state.jawAngle = entity.getJawAngle();
      state.lit = entity.getLit();
      state.jawAngle = state.jawAngle + (1.0F - state.lit) * 24.0F;
      state.damaged = entity.isDamaged();
      state.eyeDark = state.damaged && !entity.isBeamActive();
      long gt = entity.level().getGameTime();
      float now = (float)(gt % 100000L) + partialTick;
      state.idleTimeTicks = now;
      state.spawnElapsedTicks = elapsedOrNever(gt, partialTick, entity.getSpawnGameTime());
      state.fireElapsedTicks = elapsedOrNever(gt, partialTick, entity.getFireStartTime());
      state.hurtElapsedTicks = elapsedOrNever(gt, partialTick, entity.getHurtStartTime());
      state.roarElapsedTicks = elapsedOrNever(gt, partialTick, entity.getRoarStartTime());
      boolean legacy = DabyWSClientConfig.legacyHeads;
      long nowMs = Util.getMillis();
      float dtSec = entity.clientSmoothLastMillis == 0L ? 0.05F : Mth.clamp((float)(nowMs - entity.clientSmoothLastMillis) / 1000.0F, 0.0F, 0.25F);
      entity.clientSmoothLastMillis = nowMs;
      float smooth = 1.0F - (float)Math.exp(-dtSec * 10.0);
      float rawYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
      state.yRot = rawYaw;
      float rawPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
      if (Float.isNaN(entity.clientPitch)) {
         entity.clientPitch = rawPitch;
      }

      entity.clientPitch = entity.clientPitch + (rawPitch - entity.clientPitch) * smooth;
      state.xRot = entity.clientPitch;
      state.zRot = entity.getRoll();
      state.upsideDown = entity.extraRoll();
      Vec3 mo = entity.modelOffset();
      state.modelOffX = mo.x;
      state.modelOffY = mo.y;
      state.modelOffZ = mo.z;
      if (legacy) {
         if (Float.isNaN(entity.clientLegacyYaw)) {
            entity.clientLegacyYaw = rawYaw;
         }

         entity.clientLegacyYaw = Mth.wrapDegrees(entity.clientLegacyYaw + Mth.degreesDifference(entity.clientLegacyYaw, rawYaw) * smooth);
         state.yRot = entity.clientLegacyYaw;
      }

      double hx = Mth.lerp(partialTick, entity.xOld, entity.getX());
      double hy = Mth.lerp(partialTick, entity.yOld, entity.getY());
      double hz = Mth.lerp(partialTick, entity.zOld, entity.getZ());
      double renderX = hx;
      double renderY = hy;
      double renderZ = hz;
      state.hasAttach = false;
      Entity stormE = entity.level().getEntity(entity.getStormId());
      if (stormE instanceof net.dabicco.witherstormmod.entity.StormHeadHost host) {
         state.devourer = host.isDevourerForm();
         boolean var10001;
         if (stormE instanceof net.dabicco.witherstormmod.entity.WitherStormEntity ws && !ws.isPhase4()) {
            var10001 = true;
         } else {
            var10001 = false;
         }

         state.earlyPhase = var10001;
         state.headScale = host.headScaleFor(entity.getHeadIndex());
         double sx = Mth.lerp(partialTick, stormE.xOld, stormE.getX());
         double sy = Mth.lerp(partialTick, stormE.yOld, stormE.getY());
         double sz = Mth.lerp(partialTick, stormE.zOld, stormE.getZ());
         float rawBodyYaw = stormE instanceof LivingEntity le
            ? Mth.rotLerp(partialTick, le.yBodyRotO, le.yBodyRot)
            : Mth.rotLerp(partialTick, stormE.yRotO, stormE.getYRot());
         float yaw = WitherStormRenderer.smoothBodyYaw(~entity.getId(), rawBodyYaw) + host.attachYaw(partialTick);
         float roll = host.getBodyRoll() + host.attachRoll(partialTick);
         float hostPitch = host.attachPitch(partialTick);
         float bodyPitch = Mth.lerp(partialTick, stormE.xRotO, stormE.getXRot()) + hostPitch;
         if (!legacy) {
            float localTarget = entity.getLocalYaw();
            if (Float.isNaN(entity.clientLocalYaw)) {
               entity.clientLocalYaw = localTarget;
            }

            entity.clientLocalYaw = Mth.wrapDegrees(entity.clientLocalYaw + Mth.degreesDifference(entity.clientLocalYaw, localTarget) * smooth);
            state.yRot = yaw + host.headYawOffsetFor(entity.getHeadIndex()) + entity.clientLocalYaw;
         }

         updateJawLag(state, state.yRot, state.xRot, state.zRot);
         double rad = Math.toRadians(yaw);
         double cos = Math.cos(rad);
         double sin = Math.sin(rad);
         double rollRad = Math.toRadians(roll);
         double cosR = Math.cos(rollRad);
         double sinR = Math.sin(rollRad);
         double pitchRad = Math.toRadians(bodyPitch);
         double cosP = Math.cos(pitchRad);
         double sinP = Math.sin(pitchRad);
         Vec3 off = host.headOffsetFor(entity.getHeadIndex());
         double pivot = host.attachPivotY();
         double oy = off.y - pivot;
         double py = oy * cosP - off.z * sinP + pivot - host.attachDrop(partialTick);
         double pz = oy * sinP + off.z * cosP;
         double rx = off.x * cosR - py * sinR;
         double ry = off.x * sinR + py * cosR;
         double worldX = sx + (rx * cos - pz * sin);
         double worldY = sy + ry;
         double worldZ = sz + rx * sin + pz * cos;
         state.attachDX = worldX - hx;
         state.attachDY = worldY - hy;
         state.attachDZ = worldZ - hz;
         state.hasAttach = true;
         renderX = worldX;
         renderY = worldY;
         renderZ = worldZ;
         state.xRot += bodyPitch;
         state.zRot += roll;
      }

      state.beamActive = entity.isBeamActive();
      if (state.beamActive) {
         Vec3 target = entity.getBeamEndExact();
         if (entity.clientBeamEnd == null) {
            entity.clientBeamEnd = target;
         } else {
            entity.clientBeamEnd = entity.clientBeamEnd.lerp(target, legacy ? 0.15 : 0.06);
         }

         state.beamDX = entity.clientBeamEnd.x - renderX;
         state.beamDY = entity.clientBeamEnd.y - renderY;
         state.beamDZ = entity.clientBeamEnd.z - renderZ;
      } else {
         entity.clientBeamEnd = null;
      }

      if (legacy && state.beamActive) {
         double bx = state.beamDX;
         double by = state.beamDY;
         double bz = state.beamDZ;
         double horiz = Math.sqrt(bx * bx + bz * bz);
         if (horiz > 1.0E-4 || Math.abs(by) > 1.0E-4) {
            float targetYaw = (float)(Mth.atan2(bz, bx) * 180.0 / (float) Math.PI) - 90.0F;
            float targetPitch = (float)(-(Mth.atan2(by, horiz) * 180.0 / (float) Math.PI));
            entity.clientLegacyYaw = Mth.wrapDegrees(entity.clientLegacyYaw + Mth.degreesDifference(entity.clientLegacyYaw, targetYaw) * smooth);
            entity.clientPitch = entity.clientPitch + (targetPitch - entity.clientPitch) * smooth;
            state.yRot = entity.clientLegacyYaw;
            state.xRot = entity.clientPitch;
            updateJawLag(state, state.yRot, state.xRot, state.zRot);
         }
      }

      if (state.lit < 0.999F) {
         state.idleTimeTicks = 0.0F;
         state.fireElapsedTicks = -1.0F;
         state.hurtElapsedTicks = -1.0F;
         state.roarElapsedTicks = -1.0F;
         state.spawnElapsedTicks = Float.MAX_VALUE;
         state.beamActive = false;
      }
   }

   private static float elapsedOrNever(long gameTime, float partialTick, long startGameTime) {
      return startGameTime < 0L ? -1.0F : (float)(gameTime - startGameTime) + partialTick;
   }

   public void submit(WitherStormHeadRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
      devourerTint = state.devourer;
      earlyTint = state.earlyPhase;
      poseStack.pushPose();
      if (state.hasAttach) {
         poseStack.translate(state.attachDX, state.attachDY, state.attachDZ);
      }

      if (state.beamActive) {
         this.model.setupAnim(state);
         Vec3 apex = TractorBeamRenderer.computeEyeApex(
            this.model.upperJaw(), state.yRot, state.xRot, state.zRot, state.headScale, state.modelOffX, state.modelOffY, state.modelOffZ
         );
         TractorBeamRenderer.publishEye(
            state.headId,
            new Vec3(
               state.x + (state.hasAttach ? state.attachDX : 0.0) + apex.x,
               state.y + (state.hasAttach ? state.attachDY : 0.0) + apex.y,
               state.z + (state.hasAttach ? state.attachDZ : 0.0) + apex.z
            )
         );
         TractorBeamRenderer.submitBeam(
            poseStack,
            submitNodeCollector,
            apex,
            new Vec3(state.beamDX, state.beamDY, state.beamDZ),
            ClientConfigCache.cfg.beamGroundRadius * state.beamScale,
            state.idleTimeTicks,
            state.beamScale
         );
      }

      poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
      poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
      poseStack.mulPose(Axis.ZP.rotationDegrees(state.zRot));
      float scale = state.headScale;
      poseStack.scale(scale, scale, scale);
      poseStack.translate(state.modelOffX, state.modelOffY, state.modelOffZ);
      StormShadowMap.capture(poseStack, this.model);
      submitNodeCollector.submitModel(
         this.model,
         state,
         poseStack,
         FoglessRenderTypes.bodyCutout(StormSkins.phase6Body()),
         state.lightCoords,
         OverlayTexture.NO_OVERLAY,
         deadTint(state.lit),
         null,
         state.outlineColor,
         null
      );
      float emit = (float)Mth.clamp(DabyWSClientConfig.glowStrength, 0.0, 1.0) * state.lit;
      if (emit > 0.02F) {
         submitNodeCollector.submitModel(
            this.glowModel,
            state,
            poseStack,
            RenderTypes.eyes(StormSkins.phase6Body()),
            15728880,
            OverlayTexture.NO_OVERLAY,
            scaleTint(glowTint(), emit),
            null,
            state.outlineColor,
            null
         );
         submitNodeCollector.submitModel(
            this.teethBoostModel,
            state,
            poseStack,
            RenderTypes.eyes(StormSkins.phase6Body()),
            15728880,
            OverlayTexture.NO_OVERLAY,
            scaleTint(glowTint(), emit),
            null,
            state.outlineColor,
            null
         );
      }

      if (state.lit >= 0.999F) {
         submitNodeCollector.order(3)
            .submitModel(
               this.eyeGlowModel,
               state,
               poseStack,
               RenderTypes.eyes(StormSkins.phase6Body()),
               15728880,
               OverlayTexture.NO_OVERLAY,
               state.eyeDark ? -15066590 : eyeTint(),
               null,
               state.outlineColor,
               null
            );
         if (!state.eyeDark) {
            submitNodeCollector.order(3)
               .submitModel(
                  this.eyeBoostModel,
                  state,
                  poseStack,
                  RenderTypes.eyes(StormSkins.phase6Body()),
                  15728880,
                  OverlayTexture.NO_OVERLAY,
                  eyeTint(),
                  null,
                  state.outlineColor,
                  null
               );
         }
      }

      if (state.lit >= 0.999F && StormBloom.wantsEntityTarget()) {
         RenderType into = RenderTypes.eyes(StormSkins.phase6Body());
         submitNodeCollector.order(1).submitModel(this.teethBloomModel, state, poseStack, into, 15728880, OverlayTexture.NO_OVERLAY, glowTint(), null, 0, null);
         submitNodeCollector.order(1)
            .submitModel(this.teethBloom2Model, state, poseStack, into, 15728880, OverlayTexture.NO_OVERLAY, glowTint(), null, 0, null);
         if (!state.eyeDark) {
            submitNodeCollector.order(1).submitModel(this.eyeBloomModel, state, poseStack, into, 15728880, OverlayTexture.NO_OVERLAY, eyeTint(), null, 0, null);
            submitNodeCollector.order(1)
               .submitModel(this.eyeBloom2Model, state, poseStack, into, 15728880, OverlayTexture.NO_OVERLAY, eyeTint(), null, 0, null);
         }

         submitNodeCollector.order(2)
            .submitModel(
               this.bloomOccluderModel,
               state,
               poseStack,
               GlowRenderTypes.bloomOccluder(StormSkins.phase6Body()),
               state.lightCoords,
               OverlayTexture.NO_OVERLAY,
               -1,
               null,
               0,
               null
            );
         submitNodeCollector.order(4)
            .submitModel(
               this.sceneEraseModel,
               state,
               poseStack,
               GlowRenderTypes.bloomEraseOccluded(StormSkins.phase6Body()),
               state.lightCoords,
               OverlayTexture.NO_OVERLAY,
               -1,
               null,
               0,
               null
            );
      }

      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, camera);
      devourerTint = false;
      earlyTint = false;
   }
}
