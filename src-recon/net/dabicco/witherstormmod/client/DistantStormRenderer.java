package net.dabicco.witherstormmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.entity.ModEntityTypes;
import net.dabicco.witherstormmod.entity.SeveredWitherStormEntity;
import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.dabicco.witherstormmod.entity.model.ModEntityModelLayers;
import net.dabicco.witherstormmod.entity.model.WitherStormHead;
import net.dabicco.witherstormmod.entity.renderer.WitherStormHeadRenderer;
import net.dabicco.witherstormmod.entity.state.SeveredWitherStormRenderState;
import net.dabicco.witherstormmod.entity.state.WitherStormHeadRenderState;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.dabicco.witherstormmod.network.WitherStormPositionPacket.SeveredData;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class DistantStormRenderer {
   private static final double SQUASH_DISTANCE_MAX = 768.0;
   private static final float SMOOTH = 0.25F;
   private static final float SMOOTH_RATE = 3.5F;
   private static final float MAX_DISP_TURN_PER_TICK = 7.0F;
   private static final float SEVERED_SNAP_DEGREES = 40.0F;
   private static final double SEVERED_SNAP_BLOCKS = 24.0;
   private static final int FULL_BRIGHT = 15728880;
   private static WitherStormEntity proxyStorm;
   private static SeveredWitherStormEntity[] proxySevered;
   private static WitherStormHead headModel;
   private static WitherStormHead headGlowModel;

   private static double squashDistance(Minecraft mc) {
      double renderDistBlocks = mc.options.getEffectiveRenderDistance() * 16.0;
      double factor = DabyWSClientConfig.distantFog ? 0.995 : 0.55;
      return Mth.clamp(renderDistBlocks * factor, 64.0, 768.0);
   }

   public static void render(LevelRenderContext ctx) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         boolean renderDistantModels = DabyWSClientConfig.distantStorms;
         ensureInit(mc);
         Vec3 cam = ctx.levelState().cameraRenderState.pos;
         PoseStack pose = ctx.poseStack();
         SubmitNodeCollector consumers = ctx.submitNodeCollector();
         boolean optimize = DabyWSClientConfig.optimizeDistantAnimations;
         float frac = optimize ? 0.0F : mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
         long gt = mc.level.getGameTime();
         float now = (float)(gt % 100000L) + frac;
         EntityRenderer<WitherStormEntity, WitherStormRenderState> stormRenderer = (EntityRenderer<WitherStormEntity, WitherStormRenderState>) mc.getEntityRenderDispatcher().getRenderer(proxyStorm);
         float dt = Mth.clamp(mc.getDeltaTracker().getRealtimeDeltaTicks(), 0.0F, 4.0F);
         float ease = 1.0F - (float)Math.exp(-dt * 3.5F);
         float maxTurn = 7.0F * dt;

         for (net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData d : net.dabicco.witherstormmod.client.ClientDistantStormManager.all()) {
            d.dispX = Mth.lerp(ease, d.dispX, d.x);
            d.dispY = Mth.lerp(ease, d.dispY, d.y);
            d.dispZ = Mth.lerp(ease, d.dispZ, d.z);
            d.dispYaw = cappedRotLerp(ease, d.dispYaw, d.yaw, maxTurn);
            d.dispPitch = cappedRotLerp(ease, d.dispPitch, d.pitch, maxTurn);
            d.dispRoll = cappedRotLerp(ease, d.dispRoll, d.roll, maxTurn);

            for (int i = 0; i < 3; i++) {
               d.dispHeadYaw[i] = cappedRotLerp(ease, d.dispHeadYaw[i], d.headYaw[i], maxTurn);
               d.dispHeadPitch[i] = cappedRotLerp(ease, d.dispHeadPitch[i], d.headPitch[i], maxTurn);
            }

            Vec3 rel = new Vec3(d.dispX - cam.x, d.dispY - cam.y, d.dispZ - cam.z);
            double dist = rel.length();
            if (!(dist < 1.0)) {
               boolean legacy = DabyWSClientConfig.legacyDistantRenderer;
               double squashDist = squashDistance(mc);
               double squash = 1.0;
               Vec3 renderRel = rel;
               if (legacy && dist > squashDist) {
                  squash = squashDist / dist;
                  renderRel = rel.scale(squash);
               }

               boolean distant = mc.level.getEntity(d.entityId) == null;
               net.dabicco.witherstormmod.client.FoglessRenderTypes.setActive(distant);
               if (renderDistantModels && d.phase >= 6.0) {
                  net.dabicco.witherstormmod.client.FoglessRenderTypes.setActive(true);
                  submitDistantSevered(mc, d, pose, consumers, ctx, renderRel, squash, frac, now, ease, maxTurn);
                  net.dabicco.witherstormmod.client.FoglessRenderTypes.setActive(false);
               }

               if (renderDistantModels && distant) {
                  net.dabicco.witherstormmod.client.FoglessRenderTypes.setActive(true);
                  proxyStorm.setPos(d.dispX, d.dispY, d.dispZ);
                  proxyStorm.setYRot(d.dispYaw);
                  proxyStorm.setYBodyRot(d.dispYaw);
                  proxyStorm.setYHeadRot(d.dispYaw);
                  proxyStorm.setXRot(d.dispPitch);
                  proxyStorm.clientSyncPose(d.dispRoll, d.phase >= 4.0F);
                  proxyStorm.clientSyncPhase(d.phase);
                  proxyStorm.setOldPosAndRot();
                  proxyStorm.yBodyRotO = proxyStorm.yBodyRot;
                  proxyStorm.yHeadRotO = proxyStorm.yHeadRot;
                  WitherStormRenderState stormState = (WitherStormRenderState)stormRenderer.createRenderState(proxyStorm, frac);
                  stormState.lightCoords = 15728880;
                  stormState.stormId = d.entityId;
                  stormState.phase5ElapsedTicks = d.phase5Ticks < 0 ? -1.0F : d.phase5Ticks;
                  stormState.phase58ElapsedTicks = d.phase58Ticks < 0 ? -1.0F : d.phase58Ticks;
                  pose.pushPose();
                  pose.translate(renderRel.x, renderRel.y, renderRel.z);
                  pose.scale((float)squash, (float)squash, (float)squash);
                  stormRenderer.submit(stormState, pose, consumers, ctx.levelState().cameraRenderState);
                  pose.popPose();
                  double yawRad = Math.toRadians(d.dispYaw);
                  double cos = Math.cos(yawRad);
                  double sin = Math.sin(yawRad);
                  double rollRad = Math.toRadians(d.dispRoll);
                  double cosR = Math.cos(rollRad);
                  double sinR = Math.sin(rollRad);
                  double pitchRad = Math.toRadians(d.dispPitch);
                  double cosP = Math.cos(pitchRad);
                  double sinP = Math.sin(pitchRad);
                  int liveHeads = Mth.clamp(d.activeHeads, 0, 3);

                  for (int i = 0; i < liveHeads; i++) {
                     Vec3 off = WitherStormEntity.headOffset(i, d.phase >= 6.0);
                     double py = off.y * cosP - off.z * sinP;
                     double pz = off.y * sinP + off.z * cosP;
                     double rx = off.x * cosR - py * sinR;
                     double ry = off.x * sinR + py * cosR;
                     double hx = d.dispX + (rx * cos - pz * sin);
                     double hy = d.dispY + ry;
                     double hz = d.dispZ + rx * sin + pz * cos;
                     Vec3 hRel = new Vec3(hx - cam.x, hy - cam.y, hz - cam.z).scale(squash);
                     float headYaw = d.dispYaw + proxyStorm.headYawOffsetFor(i) + d.dispHeadYaw[i];
                     float headPitch = d.dispHeadPitch[i];
                     WitherStormHeadRenderState headState = new WitherStormHeadRenderState();
                     headState.lightCoords = 15728880;
                     headState.jawAngle = 0.0F;
                     headState.damaged = false;
                     headState.devourer = d.phase >= 6.0F;
                     headState.earlyPhase = d.phase < 4.0F;
                     headState.headScale = 6.0F;
                     headState.idleTimeTicks = now;
                     headState.spawnElapsedTicks = Float.MAX_VALUE;
                     headState.fireElapsedTicks = d.headFireStart[i] >= 0L ? (float)(gt - d.headFireStart[i]) + frac : -1.0F;
                     headState.hurtElapsedTicks = -1.0F;
                     pose.pushPose();
                     pose.translate(hRel.x, hRel.y, hRel.z);
                     pose.scale((float)squash, (float)squash, (float)squash);
                     if (d.beamActive[i] && d.beamEnd[i] != null) {
                        if (d.dispBeamEnd[i] == null) {
                           d.dispBeamEnd[i] = d.beamEnd[i];
                        } else {
                           d.dispBeamEnd[i] = d.dispBeamEnd[i].lerp(d.beamEnd[i], 0.06);
                        }

                        Vec3 beamRel = new Vec3(d.dispBeamEnd[i].x - hx, d.dispBeamEnd[i].y - hy, d.dispBeamEnd[i].z - hz);
                        headModel.setupAnim(headState);
                        net.dabicco.witherstormmod.client.TractorBeamRenderer.submitBeam(
                           pose,
                           consumers,
                           net.dabicco.witherstormmod.client.TractorBeamRenderer.computeEyeApex(headModel.upperJaw(), headYaw, headPitch),
                           beamRel,
                           net.dabicco.witherstormmod.client.ClientConfigCache.cfg.beamGroundRadius,
                           now
                        );
                     } else {
                        d.dispBeamEnd[i] = null;
                     }

                     pose.mulPose(Axis.YP.rotationDegrees(-headYaw));
                     pose.mulPose(Axis.XP.rotationDegrees(headPitch));
                     pose.scale(6.0F, 6.0F, 6.0F);
                     pose.translate(0.0, -1.25, 0.0);
                     consumers.submitModel(
                        headModel,
                        headState,
                        pose,
                        net.dabicco.witherstormmod.client.FoglessRenderTypes.bodyCutout(net.dabicco.witherstormmod.client.StormSkins.phase4()),
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        -1,
                        (TextureAtlasSprite)null,
                        0,
                        (CrumblingOverlay)null
                     );
                     consumers.submitModel(
                        headGlowModel,
                        headState,
                        pose,
                        net.dabicco.witherstormmod.client.FoglessRenderTypes.eyes(net.dabicco.witherstormmod.client.StormSkins.phase4()),
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        WitherStormHeadRenderer.glowTint(),
                        (TextureAtlasSprite)null,
                        0,
                        (CrumblingOverlay)null
                     );
                     pose.popPose();
                  }

                  net.dabicco.witherstormmod.client.FoglessRenderTypes.setActive(false);
               } else {
                  net.dabicco.witherstormmod.client.FoglessRenderTypes.setActive(false);
               }
            }
         }
      }
   }

   private static void submitDistantSevered(
      Minecraft mc,
      net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData d,
      PoseStack pose,
      SubmitNodeCollector consumers,
      LevelRenderContext ctx,
      Vec3 renderRel,
      double squash,
      float frac,
      float now,
      float ease,
      float maxTurn
   ) {
      EntityRenderer<SeveredWitherStormEntity, SeveredWitherStormRenderState> sevRenderer = (EntityRenderer<SeveredWitherStormEntity, SeveredWitherStormRenderState>) mc.getEntityRenderDispatcher().getRenderer(proxySevered[0]);

      for (SeveredData s : d.severed) {
         if (mc.level == null || mc.level.getEntity(s.entityId()) == null) {
            int side = s.side() < 0 ? -1 : 1;
            int si = side < 0 ? 0 : 1;
            SeveredWitherStormEntity half = proxySevered[si];
            boolean jumped = Math.abs(Mth.wrapDegrees(s.yaw() - d.sevDispYaw[si])) > 40.0F
               || Math.abs(s.x() - d.sevDispX[si]) > 24.0
               || Math.abs(s.z() - d.sevDispZ[si]) > 24.0;
            if (jumped) {
               d.sevDispX[si] = s.x();
               d.sevDispY[si] = s.y();
               d.sevDispZ[si] = s.z();
               d.sevDispYaw[si] = s.yaw();
            } else {
               d.sevDispX[si] = Mth.lerp(ease, d.sevDispX[si], s.x());
               d.sevDispY[si] = Mth.lerp(ease, d.sevDispY[si], s.y());
               d.sevDispZ[si] = Mth.lerp(ease, d.sevDispZ[si], s.z());
               d.sevDispYaw[si] = cappedRotLerp(ease, d.sevDispYaw[si], s.yaw(), maxTurn);
            }

            double hx = d.sevDispX[si];
            double hy = d.sevDispY[si];
            double hz = d.sevDispZ[si];
            float halfYaw = d.sevDispYaw[si];
            double rad = Math.toRadians(halfYaw);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            half.setPos(hx, hy, hz);
            half.setYRot(halfYaw);
            half.setYBodyRot(halfYaw);
            half.setYHeadRot(halfYaw);
            half.setOldPosAndRot();
            half.yBodyRotO = half.yBodyRot;
            half.yHeadRotO = half.yHeadRot;
            half.clientSyncSide(side);
            SeveredWitherStormRenderState st = (SeveredWitherStormRenderState)sevRenderer.createRenderState(half, frac);
            st.lightCoords = 15728880;
            st.bodyLight = 15728880;
            pose.pushPose();
            pose.translate(renderRel.x + (hx - d.dispX) * squash, renderRel.y + (hy - d.dispY) * squash, renderRel.z + (hz - d.dispZ) * squash);
            pose.scale((float)squash, (float)squash, (float)squash);
            sevRenderer.submit(st, pose, consumers, ctx.levelState().cameraRenderState);
            pose.popPose();
            int heads = Mth.clamp(s.heads(), 0, 3);

            for (int h = 0; h < heads; h++) {
               Vec3 off = half.headOffsetFor(h);
               double ox = off.x * cos - off.z * sin;
               double oz = off.x * sin + off.z * cos;
               WitherStormHeadRenderState hs = new WitherStormHeadRenderState();
               hs.lightCoords = 15728880;
               hs.idleTimeTicks = now;
               hs.spawnElapsedTicks = Float.MAX_VALUE;
               hs.fireElapsedTicks = s.headFireElapsed()[h];
               hs.hurtElapsedTicks = -1.0F;
               hs.roarElapsedTicks = -1.0F;
               hs.headScale = half.headScaleFor(h);
               hs.devourer = true;
               hs.beamActive = s.headBeamActive()[h];
               if (hs.beamActive) {
                  hs.beamDX = (s.headBeamX()[h] - (hx + ox)) * squash;
                  hs.beamDY = (s.headBeamY()[h] - (hy + off.y)) * squash;
                  hs.beamDZ = (s.headBeamZ()[h] - (hz + oz)) * squash;
               }

               pose.pushPose();
               pose.translate(
                  renderRel.x + (hx + ox - d.dispX) * squash, renderRel.y + (hy + off.y - d.dispY) * squash, renderRel.z + (hz + oz - d.dispZ) * squash
               );
               pose.scale((float)squash, (float)squash, (float)squash);
               float aimYaw = halfYaw + half.headYawOffsetFor(h) + s.headYaw()[h];
               if (hs.beamActive) {
                  headModel.setupAnim(hs);
                  Vec3 apex = net.dabicco.witherstormmod.client.TractorBeamRenderer.computeEyeApex(
                     headModel.upperJaw(), aimYaw, s.headPitch()[h], 0.0F, hs.headScale
                  );
                  net.dabicco.witherstormmod.client.TractorBeamRenderer.submitBeam(
                     pose,
                     consumers,
                     apex,
                     new Vec3(hs.beamDX, hs.beamDY, hs.beamDZ),
                     (float)(net.dabicco.witherstormmod.client.ClientConfigCache.cfg.beamGroundRadius * squash),
                     now,
                     (float)squash
                  );
               }

               pose.mulPose(Axis.YP.rotationDegrees(-aimYaw));
               pose.mulPose(Axis.XP.rotationDegrees(s.headPitch()[h]));
               pose.scale(hs.headScale, hs.headScale, hs.headScale);
               pose.translate(0.0, -1.25, 0.0);
               WitherStormHeadRenderer.setDevourerTint(true);
               consumers.submitModel(
                  headModel,
                  hs,
                  pose,
                  net.dabicco.witherstormmod.client.FoglessRenderTypes.bodyCutout(net.dabicco.witherstormmod.client.StormSkins.phase4()),
                  15728880,
                  OverlayTexture.NO_OVERLAY,
                  -1,
                  (TextureAtlasSprite)null,
                  0,
                  (CrumblingOverlay)null
               );
               consumers.submitModel(
                  headGlowModel,
                  hs,
                  pose,
                  net.dabicco.witherstormmod.client.FoglessRenderTypes.eyes(net.dabicco.witherstormmod.client.StormSkins.phase4()),
                  15728880,
                  OverlayTexture.NO_OVERLAY,
                  WitherStormHeadRenderer.glowTint(),
                  (TextureAtlasSprite)null,
                  0,
                  (CrumblingOverlay)null
               );
               WitherStormHeadRenderer.setDevourerTint(false);
               pose.popPose();
            }
         }
      }
   }

   private static void ensureInit(Minecraft mc) {
      if (proxyStorm == null || proxyStorm.level() != mc.level) {
         proxyStorm = new WitherStormEntity(ModEntityTypes.WITHER_STORM, mc.level);
         proxyStorm.setId(-123456789);
      }

      if (proxySevered == null || proxySevered[0].level() != mc.level) {
         proxySevered = new SeveredWitherStormEntity[2];

         for (int i = 0; i < 2; i++) {
            proxySevered[i] = new SeveredWitherStormEntity(ModEntityTypes.SEVERED_WITHER_STORM, mc.level);
            proxySevered[i].setId(-123456800 - i);
            proxySevered[i].clientSyncSide(i == 0 ? -1 : 1);
         }
      }

      if (headModel == null) {
         headModel = new WitherStormHead(mc.getEntityModels().bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD));
         headGlowModel = new WitherStormHead(mc.getEntityModels().bakeLayer(ModEntityModelLayers.WITHER_STORM_HEAD_GLOW));
      }
   }

   private static float rotLerp(float t, float from, float to) {
      return from + Mth.wrapDegrees(to - from) * t;
   }

   private static float cappedRotLerp(float t, float from, float to, float maxStep) {
      float step = Mth.wrapDegrees(to - from) * t;
      step = Mth.clamp(step, -maxStep, maxStep);
      return Mth.wrapDegrees(from + step);
   }
}
