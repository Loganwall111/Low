package net.dabicco.witherstormmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * StormHaloRings — the MCSM halo system, rebuilt from crisp cubes.
 *
 * <p>Story Mode never shows soft glowing balls: its halos are thin, hard,
 * geometric rings. Every ring here is a loop of small fullbright cubes — no
 * smooth curves, no blurry discs — so the system reads identically with or
 * without a shader pack (the additive glow pass falls back to the vanilla
 * eyes pipeline under Iris).
 *
 * <ul>
 *   <li>Phase 4+: thin white under-halo ring beneath the body.</li>
 *   <li>Phase 5.8+: blue-purple cataclysm ring around the whole area.</li>
 *   <li>Phase 6+: thin cube rings spiralling clockwise, diagonal and
 *       vertical, around the storm.</li>
 *   <li>Phase 7+: more rings; the wide outer ones counter-rotate.</li>
 *   <li>Phase 8-9: the cosmetic halo dies away (glare only) while three
 *       gigantic layered ring systems — ten nested rings each, thinner
 *       toward the top — engulf the sky as a spinning vortex. All three
 *       systems turn counter-clockwise and burn inferno red-orange.</li>
 * </ul>
 */
public final class StormHaloRings {
   private static final Identifier WHITE = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/misc/shield_white.png");
   private static final int FULL_BRIGHT = 15728880;

   private StormHaloRings() {
   }

   private record Ring(double yOff, double radius, double tiltX, double tiltZ, double yawSpin, double speed, int count, float cube, int r, int g, int b, float alpha) {
   }

   public static void submit(LevelRenderContext ctx) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || ClientDistantStormManager.all().isEmpty() || !DabyWSClientConfig.haloRings) {
         return;
      }
      float strength = (float)Mth.clamp(DabyWSClientConfig.haloRingStrength, 0.0, 2.0);
      if (strength <= 0.01F) {
         return;
      }
      float gt = (float)(mc.level.getGameTime() % 240000L) + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      float t = gt * 0.05F;
      Vec3 cam = ctx.levelState().cameraRenderState.pos;
      PoseStack poseStack = ctx.poseStack();
      SubmitNodeCollector collector = ctx.submitNodeCollector();

      for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
         float phase = d.phase;
         if (phase < 4.0F || d.collapsed) {
            continue;
         }
         double bodyR = StormPresenceFX.bodyRadius(phase);
         Vec3 centre = new Vec3(d.dispX, d.dispY, d.dispZ);
         if (centre.distanceToSqr(cam) > 1600000.0) {
            continue;
         }
         Ring[] rings = ringsFor(phase, bodyR, strength);
         if (rings.length == 0) {
            continue;
         }
         collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE), (pose, consumer) -> {
            for (Ring ring : rings) {
               emitRing(pose, consumer, centre, ring, t, d.entityId);
            }
         });
      }
   }

   private static Ring[] ringsFor(float phase, double bodyR, float strength) {
      // At phase 8-9 the cosmetic halo vanishes: glare only.
      float haloFade = 1.0F - Mth.clamp((phase - 7.6F) / 0.4F, 0.0F, 1.0F);
      java.util.ArrayList<Ring> out = new java.util.ArrayList<>();
      float haloRamp = Mth.clamp((phase - 3.8F) / 0.5F, 0.0F, 1.0F);

      // White under-halo: thin, flat, slow — the MCSM signature from phase 4.
      if (haloRamp > 0.01F && haloFade > 0.01F) {
         out.add(new Ring(-bodyR * 0.38, bodyR * 1.55, 0.0, 0.0, 0.0, 0.22, ringCount(bodyR * 1.55, 0.42), 0.42F, 235, 242, 255, 0.95F * haloRamp * haloFade * strength));
      }
      // Blue-purple cataclysm ring from phase 5.8 (keeps its own toggle + strength).
      if (phase >= 5.8F && haloFade > 0.01F && DabyWSClientConfig.cataclysmHalos) {
         float ramp = Mth.clamp((phase - 5.8F) / 0.4F, 0.0F, 1.0F) * (float)Mth.clamp(DabyWSClientConfig.haloStrength, 0.0, 2.0);
         out.add(new Ring(bodyR * 0.10, bodyR * 2.05, Math.toRadians(7.0), Math.toRadians(-4.0), 0.0, -0.16, ringCount(bodyR * 2.05, 0.55), 0.55F, 92, 118, 255, 0.85F * ramp * haloFade * strength));
      }
      // Phase 6: thin cube rings spiralling clockwise — diagonal + vertical.
      if (phase >= 6.0F) {
         float ramp = Mth.clamp((phase - 6.0F) / 0.5F, 0.0F, 1.0F);
         double[] radii = {1.85, 2.25, 2.65};
         double[] tilts = {24.0, 90.0, -38.0};
         for (int k = 0; k < 3; k++) {
            double rr = bodyR * radii[k];
            out.add(new Ring(bodyR * (0.05 + 0.12 * k), rr, Math.toRadians(tilts[k]), Math.toRadians(12.0 * (k - 1)), 0.05 + 0.02 * k, 0.55 + 0.12 * k, ringCount(rr, 0.5), 0.5F, 150, 96, 255, 0.8F * ramp * strength));
         }
      }
      // Phase 7: more rings, wide outer ones spinning counter-clockwise.
      if (phase >= 7.0F) {
         float ramp = Mth.clamp((phase - 7.0F) / 0.5F, 0.0F, 1.0F);
         double[] radii = {3.1, 3.7, 4.4};
         for (int k = 0; k < 3; k++) {
            double rr = bodyR * radii[k];
            out.add(new Ring(-bodyR * 0.1 + bodyR * 0.18 * k, rr, Math.toRadians(k == 1 ? 90.0 : 8.0 - 14.0 * k), Math.toRadians(6.0 * k), -0.04 - 0.015 * k, -0.42 - 0.08 * k, ringCount(rr, 0.62), 0.62F, 120, 235, 255, 0.7F * ramp * strength));
         }
      }
      // Phase 8-9: three gigantic layered systems of ten nested rings each,
      // engulfing the sky as a vortex. Thinner toward the top.
      if (phase >= 8.0F) {
         float ramp = Mth.clamp((phase - 8.0F) / 0.6F, 0.0F, 1.0F);
         double[][] systems = {
            // {baseRadiusMult, baseHeightMult, spinDir, yawSpin}
            {3.2, 0.9, -1.0, -0.030},
            {4.6, 1.7, -1.0, -0.022},
            {6.1, 2.6, -1.0, -0.016},
         };
         int[][] palette = {{255, 122, 28}, {232, 58, 14}, {255, 172, 84}};
         for (int s = 0; s < 3; s++) {
            for (int k = 0; k < 10; k++) {
               double frac = k / 9.0;
               double rr = bodyR * (systems[s][0] + frac * 1.9);
               double y = bodyR * (systems[s][1] + frac * 1.15);
               float cube = (float)(1.35 * (1.0 - 0.72 * frac));
               int count = ringCount(rr, cube);
               double speed = systems[s][2] * (0.30 - 0.13 * frac);
               int[] col = palette[s];
               float dim = (float)(1.0 - 0.35 * frac);
               out.add(new Ring(y, rr, Math.toRadians(4.0 + 3.0 * s), Math.toRadians(-3.0 * s), systems[s][3], speed, count, cube, (int)(col[0] * dim), (int)(col[1] * dim), (int)(col[2] * dim), 0.62F * ramp * strength));
            }
         }
      }
      return out.toArray(new Ring[0]);
   }

   private static int ringCount(double radius, double cube) {
      // keep cube spacing roughly constant so rings never look sparse or blobby
      return Mth.clamp((int)(Math.PI * 2.0 * radius / (cube * 2.6)), 24, 220);
   }

   private static void emitRing(PoseStack.Pose pose, VertexConsumer consumer, Vec3 centre, Ring ring, float t, int stormId) {
      int a = (int)Mth.clamp(ring.alpha(), 0.0F, 1.0F) * 255;
      if (a <= 2) {
         return;
      }
      double[] u = {1.0, 0.0, 0.0};
      double[] v = {0.0, 0.0, 1.0};
      rotX(u, ring.tiltX());
      rotX(v, ring.tiltX());
      rotZ(u, ring.tiltZ());
      rotZ(v, ring.tiltZ());
      double yaw = ring.yawSpin() * t + stormId;
      rotY(u, yaw);
      rotY(v, yaw);
      double off = ring.speed() * t;
      for (int i = 0; i < ring.count(); i++) {
         double ang = off + (Math.PI * 2.0 * i) / ring.count();
         double ca = Math.cos(ang);
         double sa = Math.sin(ang);
         double x = centre.x + (u[0] * ca + v[0] * sa) * ring.radius();
         double y = centre.y + ring.yOff() + (u[1] * ca + v[1] * sa) * ring.radius();
         double z = centre.z + (u[2] * ca + v[2] * sa) * ring.radius();
         cube(pose, consumer, (float)x, (float)y, (float)z, ring.cube(), ring.r(), ring.g(), ring.b(), a);
      }
   }

   private static void rotX(double[] p, double ang) {
      double c = Math.cos(ang);
      double s = Math.sin(ang);
      double y = p[1] * c - p[2] * s;
      double z = p[1] * s + p[2] * c;
      p[1] = y;
      p[2] = z;
   }

   private static void rotY(double[] p, double ang) {
      double c = Math.cos(ang);
      double s = Math.sin(ang);
      double x = p[0] * c + p[2] * s;
      double z = -p[0] * s + p[2] * c;
      p[0] = x;
      p[2] = z;
   }

   private static void rotZ(double[] p, double ang) {
      double c = Math.cos(ang);
      double s = Math.sin(ang);
      double x = p[0] * c - p[1] * s;
      double y = p[0] * s + p[1] * c;
      p[0] = x;
      p[1] = y;
   }

   private static void cube(PoseStack.Pose pose, VertexConsumer c, float x, float y, float z, float h, int r, int g, int b, int a) {
      float[][] corners = {
         {x - h, y - h, z - h}, {x + h, y - h, z - h}, {x + h, y - h, z + h}, {x - h, y - h, z + h},
         {x - h, y + h, z - h}, {x + h, y + h, z - h}, {x + h, y + h, z + h}, {x - h, y + h, z + h},
      };
      // top / bottom / sides: every ring cube is crisp and fullbright
      quad(pose, c, corners[4], corners[5], corners[6], corners[7], r, g, b, a);
      quad(pose, c, corners[3], corners[2], corners[1], corners[0], r, g, b, a);
      quad(pose, c, corners[0], corners[1], corners[5], corners[4], r, g, b, a);
      quad(pose, c, corners[2], corners[3], corners[7], corners[6], r, g, b, a);
      quad(pose, c, corners[1], corners[2], corners[6], corners[5], r, g, b, a);
      quad(pose, c, corners[3], corners[0], corners[4], corners[7], r, g, b, a);
   }

   private static void quad(PoseStack.Pose pose, VertexConsumer c, float[] p0, float[] p1, float[] p2, float[] p3, int r, int g, int b, int a) {
      vert(pose, c, p0, 0.0F, 0.0F, r, g, b, a);
      vert(pose, c, p1, 1.0F, 0.0F, r, g, b, a);
      vert(pose, c, p2, 1.0F, 1.0F, r, g, b, a);
      vert(pose, c, p3, 0.0F, 1.0F, r, g, b, a);
   }

   private static void vert(PoseStack.Pose pose, VertexConsumer c, float[] p, float u, float v, int r, int g, int b, int a) {
      c.addVertex(pose, p[0], p[1], p[2])
         .setColor(Math.min(255, r), Math.min(255, g), Math.min(255, b), Math.min(255, a))
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(FULL_BRIGHT)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }
}
