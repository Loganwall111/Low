package net.dabicco.witherstormmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class TractorBeamRenderer {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/tractor_beam.png");
   private static final int CORE_ALPHA = 50;
   private static final int FULL_BRIGHT = 15728880;
   public static final float TOP_HALF = 0.3F;
   private static final float EXTEND_BELOW = 6.0F;
   private static final Vector3f EYE_IN_PART = new Vector3f(-0.3125F, -0.125F, -0.753125F);
   private static final Map<Integer, Vec3> EYE_WORLD = new HashMap<>();
   private static final int MOTE_COUNT = 72;
   private static final float MOTE_CLIMB = 0.0145F;
   private static final float MOTE_SIZE = 0.28F;
   private static final float[] MOTE_COLOUR = new float[]{0.62F, 0.28F, 0.95F};

   private static int beamR() {
      return (int)(Mth.clamp(DabyWSClientConfig.beamColorR, 0.0, 1.0) * 255.0);
   }

   private static int beamG() {
      return (int)(Mth.clamp(DabyWSClientConfig.beamColorG, 0.0, 1.0) * 255.0);
   }

   private static int beamB() {
      return (int)(Mth.clamp(DabyWSClientConfig.beamColorB, 0.0, 1.0) * 255.0);
   }

   public static float baseHalfWidth(float groundRadius) {
      return groundRadius + 0.75F;
   }

   private TractorBeamRenderer() {
   }

   public static void publishEye(int headId, Vec3 world) {
      if (EYE_WORLD.size() > 64) {
         EYE_WORLD.clear();
      }

      EYE_WORLD.put(headId, world);
   }

   public static Vec3 eyeWorld(int headId) {
      return EYE_WORLD.get(headId);
   }

   public static Vec3 computeEyeApex(ModelPart upperJaw, float yRotDegrees, float xRotDegrees) {
      return computeEyeApex(upperJaw, yRotDegrees, xRotDegrees, 6.0F);
   }

   public static Vec3 computeEyeApex(ModelPart upperJaw, float yRotDegrees, float xRotDegrees, float headScale) {
      return computeEyeApex(upperJaw, yRotDegrees, xRotDegrees, 0.0F, headScale);
   }

   public static Vec3 computeEyeApex(ModelPart upperJaw, float yRotDegrees, float xRotDegrees, float zRotDegrees, float headScale) {
      return computeEyeApex(upperJaw, yRotDegrees, xRotDegrees, zRotDegrees, headScale, 0.0, -1.25, 0.0);
   }

   public static Vec3 computeEyeApex(
      ModelPart upperJaw, float yRotDegrees, float xRotDegrees, float zRotDegrees, float headScale, double offX, double offY, double offZ
   ) {
      PoseStack ps = new PoseStack();
      ps.mulPose(Axis.YP.rotationDegrees(-yRotDegrees));
      ps.mulPose(Axis.XP.rotationDegrees(xRotDegrees));
      ps.mulPose(Axis.ZP.rotationDegrees(zRotDegrees));
      ps.scale(headScale, headScale, headScale);
      ps.translate(offX, offY, offZ);
      upperJaw.translateAndRotate(ps);
      Vector3f p = ps.last().pose().transformPosition(new Vector3f(EYE_IN_PART));
      return new Vec3(p.x(), p.y(), p.z());
   }

   public static void submitBeam(PoseStack poseStack, SubmitNodeCollector collector, Vec3 apex, Vec3 relEnd, float groundRadius, float timeTicks) {
      submitBeam(poseStack, collector, apex, relEnd, groundRadius, timeTicks, 1.0F);
   }

   public static void submitBeam(
      PoseStack poseStack, SubmitNodeCollector collector, Vec3 apex, Vec3 relEnd, float groundRadius, float timeTicks, float beamScale
   ) {
      float pulse = 0.85F + 0.15F * Mth.sin(timeTicks * 0.25F);
      float opacity = (float)Mth.clamp(DabyWSClientConfig.beamOpacity, 0.0, 2.0);
      int alpha = (int)(50.0F * pulse * opacity);
      if (alpha > 1) {
         int topAlpha = Math.min(255, alpha + (int)(30.0F * opacity));
         float endFade = (float)Mth.clamp(DabyWSClientConfig.beamEndFade, 0.0, 1.0);
         int bottomAlpha = Math.max(0, (int)(alpha * (1.0F - endFade)));
         Vec3 d = relEnd.subtract(apex);
         if (!(d.lengthSqr() < 1.0E-4)) {
            d = d.normalize();
            Vec3 up = new Vec3(0.0, 1.0, 0.0);
            Vec3 right = d.cross(up);
            if (right.lengthSqr() < 1.0E-4) {
               right = new Vec3(1.0, 0.0, 0.0);
            }

            right = right.normalize();
            Vec3 upB = right.cross(d).normalize();
            Vec3 fwdH = up.cross(right).normalize();
            Vec3[] top = new Vec3[4];
            Vec3[] bottom = new Vec3[4];
            float r = baseHalfWidth(groundRadius);
            int[][] signs = new int[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};

            for (int i = 0; i < 4; i++) {
               int a = signs[i][0];
               int b = signs[i][1];
               top[i] = apex.add(right.scale(a * 0.3F * beamScale)).add(upB.scale(b * 0.3F * beamScale));
               bottom[i] = relEnd.add(right.scale(a * r)).add(upB.scale(b * r)).add(d.scale(6.0));
            }

            collector.submitCustomGeometry(
               poseStack,
               net.dabicco.witherstormmod.client.FoglessRenderTypes.entityTranslucentEmissive(TEXTURE),
               (pose, consumer) -> emitWalls(pose, consumer, top, bottom, topAlpha, bottomAlpha, beamR(), beamG(), beamB())
            );
         }
      }
   }

   private static void emitWalls(Pose pose, VertexConsumer consumer, Vec3[] top, Vec3[] bottom, int topAlpha, int alpha, int r, int g, int b) {
      for (int i = 0; i < 4; i++) {
         int j = (i + 1) % 4;
         emitQuad(pose, consumer, r, g, b, top[i], topAlpha, top[j], topAlpha, bottom[j], alpha, bottom[i], alpha);
         if (DabyWSClientConfig.beamInnerFaces) {
            emitQuad(pose, consumer, r, g, b, top[j], topAlpha, top[i], topAlpha, bottom[i], alpha, bottom[j], alpha);
         }
      }
   }

   private static void emitQuad(Pose pose, VertexConsumer consumer, int r, int g, int b, Vec3 v0, int a0, Vec3 v1, int a1, Vec3 v2, int a2, Vec3 v3, int a3) {
      vertex(pose, consumer, v0.x, v0.y, v0.z, 0.0F, 0.0F, r, g, b, a0);
      vertex(pose, consumer, v1.x, v1.y, v1.z, 1.0F, 0.0F, r, g, b, a1);
      vertex(pose, consumer, v2.x, v2.y, v2.z, 1.0F, 1.0F, r, g, b, a2);
      vertex(pose, consumer, v3.x, v3.y, v3.z, 0.0F, 1.0F, r, g, b, a3);
   }

   private static void vertex(Pose pose, VertexConsumer consumer, double x, double y, double z, float u, float v, int r, int g, int b, int alpha) {
      consumer.addVertex(pose, (float)x, (float)y, (float)z)
         .setColor(r, g, b, alpha)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   public static void submitPreviewMotes(
      PoseStack poseStack, SubmitNodeCollector collector, Vec3 apex, Vec3 relEnd, float groundRadius, float timeTicks, float beamScale
   ) {
      float baseHalf = baseHalfWidth(groundRadius) * beamScale;
      float alpha = (float)Mth.clamp(DabyWSClientConfig.beamOpacity, 0.0, 1.0);
      if (!(alpha <= 0.01F)) {
         collector.submitCustomGeometry(
            poseStack, net.dabicco.witherstormmod.client.FoglessRenderTypes.entityTranslucentEmissive(TEXTURE), (pose, consumer) -> {
               for (int i = 0; i < 34; i++) {
                  float seed = i * 0.6180339F;
                  float angle = seed % 1.0F * (float) (Math.PI * 2);
                  float radial = Mth.sqrt(i * 0.381966F % 1.0F);
                  float t = (seed * 3.7F % 1.0F + timeTicks * 0.0082F) % 1.0F;
                  float r = baseHalf * radial * (1.0F - 0.82F * t);
                  double px = Mth.lerp(t, relEnd.x, apex.x) + Mth.cos(angle) * r;
                  double py = Mth.lerp(t, relEnd.y, apex.y);
                  double pz = Mth.lerp(t, relEnd.z, apex.z) + Mth.sin(angle) * r;
                  float fade = Math.min(1.0F, t * 12.0F) * Math.min(1.0F, (1.0F - t) * 6.0F);
                  int a = (int)(alpha * fade * 230.0F);
                  if (a > 2) {
                     float s = 0.16F * beamScale * (1.0F - 0.3F * t);
                     moteCube(pose, consumer, (float)px, (float)py, (float)pz, s, a);
                  }
               }
            }
         );
      }
   }

   private static void moteCube(Pose pose, VertexConsumer consumer, float x, float y, float z, float s, int a) {
      int r = beamR();
      int g = beamG();
      int b = beamB();
      float[][] faces = new float[][]{
         {0.0F, 0.0F, 1.0F}, {0.0F, 0.0F, -1.0F}, {1.0F, 0.0F, 0.0F}, {-1.0F, 0.0F, 0.0F}, {0.0F, 1.0F, 0.0F}, {0.0F, -1.0F, 0.0F}
      };

      for (float[] n : faces) {
         float ax;
         float ay;
         float az;
         float bx;
         float by;
         float bz;
         if (n[1] != 0.0F) {
            ax = 1.0F;
            ay = 0.0F;
            az = 0.0F;
            bx = 0.0F;
            by = 0.0F;
            bz = 1.0F;
         } else if (n[0] != 0.0F) {
            ax = 0.0F;
            ay = 1.0F;
            az = 0.0F;
            bx = 0.0F;
            by = 0.0F;
            bz = 1.0F;
         } else {
            ax = 1.0F;
            ay = 0.0F;
            az = 0.0F;
            bx = 0.0F;
            by = 1.0F;
            bz = 0.0F;
         }

         float cx = x + n[0] * s;
         float cy = y + n[1] * s;
         float cz = z + n[2] * s;
         moteVertex(pose, consumer, cx - ax * s - bx * s, cy - ay * s - by * s, cz - az * s - bz * s, 0.0F, 0.0F, r, g, b, a, n);
         moteVertex(pose, consumer, cx - ax * s + bx * s, cy - ay * s + by * s, cz - az * s + bz * s, 0.0F, 1.0F, r, g, b, a, n);
         moteVertex(pose, consumer, cx + ax * s + bx * s, cy + ay * s + by * s, cz + az * s + bz * s, 1.0F, 1.0F, r, g, b, a, n);
         moteVertex(pose, consumer, cx + ax * s - bx * s, cy + ay * s - by * s, cz + az * s - bz * s, 1.0F, 0.0F, r, g, b, a, n);
      }
   }

   private static void moteVertex(Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, int r, int g, int b, int a, float[] n) {
      consumer.addVertex(pose, x, y, z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, n[0], n[1], n[2]);
   }
}
