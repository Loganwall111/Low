package net.dabicco.witherstormmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.network.WitherStormPositionPacket;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * StormShieldFX — the glowing, emissive, alpha-blended 3D spherical protective
 * shield halos around the Wither Storm.
 *
 * The barrier is a TRUE volumetric sphere, not a flat sprite:
 *  - the sphere is a procedural lat/long UV-sphere mesh (never a billboard);
 *  - its colour and alpha come 100% from per-vertex data — the pipeline is
 *    bound to a constant 1x1 white texel, so no 2D sprite icon is ever
 *    sampled. The flat blue_shield.png sprite texture has been discarded;
 *  - every vertex carries a view-dependent rim factor (1 - |normal.view|) so
 *    the shell fades in smoothly from its centre and brightens toward its
 *    silhouette — smooth edge-fading alpha blending with no hard sprite edge;
 *  - three passes: a depth-tested translucent shell, an additive emissive
 *    glow, and a bloom-source pass feeding the mod's real localized
 *    screen-space bloom chain (StormBloomTarget -> storm_bloom_extract /
 *    blur / combine).
 *
 * Timeline (chronological stage triggers):
 *  - Phase 4.0+: the blue shield sphere activates around the storm centre and
 *    stays persistently active across ALL subsequent phases (4, 5, 6, 7 and
 *    beyond — there is deliberately no upper bound on the phase gate).
 *  - Phase 6.0+ (split heads): the same 3D shield is cleanly mirrored across
 *    all three split heads — the main devourer body plus both severed halves
 *    reported by the server.
 *  - Phase 7.0+: periodic purple flare bursts (additive glow spheres) over the
 *    body, the severed heads and a sky-spanning glare bloom above the storm.
 */
public final class StormShieldFX {
   /** Constant 1x1 WHITE texel: the shield renders as pure vertex-coloured
    *  geometry. No sprite icon — colour/alpha come from the mesh vertices. */
   private static final Identifier WHITE = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/misc/shield_white.png");
   private static final int FULL_BRIGHT = 15728880;
   private static final int RINGS = 18;
   private static final int SEGS = 36;
   /** Phase 7 purple flare: spike every 300 ticks (15s) with a slow breathing base. */
   private static final long FLARE_PERIOD_TICKS = 300L;
   private static final long FLARE_SPIKE_TICKS = 45L;
   private static BakedMesh.Mesh sphere;

   private StormShieldFX() {
   }

   private static BakedMesh.Mesh sphere() {
      BakedMesh.Mesh s = sphere;
      if (s == null) {
         s = buildSphere();
         sphere = s;
      }
      return s;
   }

   /** Procedural UV sphere (lat/long) so the shield never depends on a baked asset. */
   private static BakedMesh.Mesh buildSphere() {
      List<Float> tris = new ArrayList<>(RINGS * SEGS * 18);
      List<Float> uvs = new ArrayList<>(RINGS * SEGS * 12);
      List<Float> normals = new ArrayList<>(RINGS * SEGS * 6);
      for (int ring = 0; ring < RINGS; ring++) {
         float phi0 = (float)Math.PI * ring / RINGS;
         float phi1 = (float)Math.PI * (ring + 1) / RINGS;
         for (int seg = 0; seg < SEGS; seg++) {
            float th0 = (float)(Math.PI * 2.0) * seg / SEGS;
            float th1 = (float)(Math.PI * 2.0) * (seg + 1) / SEGS;
            // four sphere points for this cell
            float[][] p = new float[][]{
               point(phi0, th0), point(phi0, th1), point(phi1, th0), point(phi1, th1)
            };
            float[][] uv = new float[][]{
               {th0 / (float)(Math.PI * 2.0), 1.0F - phi0 / (float)Math.PI},
               {th1 / (float)(Math.PI * 2.0), 1.0F - phi0 / (float)Math.PI},
               {th0 / (float)(Math.PI * 2.0), 1.0F - phi1 / (float)Math.PI},
               {th1 / (float)(Math.PI * 2.0), 1.0F - phi1 / (float)Math.PI}
            };
            // two triangles: (p0,p2,p1) and (p1,p2,p3), normals = positions
            addTri(tris, p[0], p[2], p[1]);
            addTri(tris, p[1], p[2], p[3]);
            for (int q = 0; q < 2; q++) {
               int[] idx = q == 0 ? new int[]{0, 2, 1} : new int[]{1, 2, 3};
               for (int k : idx) {
                  uvs.add(uv[k][0]);
                  uvs.add(uv[k][1]);
               }
               for (int k : idx) {
                  normals.add(p[k][0]);
                  normals.add(p[k][1]);
                  normals.add(p[k][2]);
               }
            }
         }
      }
      float[] t = new float[tris.size()];
      for (int i = 0; i < t.length; i++) t[i] = tris.get(i);
      float[] u = new float[uvs.size()];
      for (int i = 0; i < u.length; i++) u[i] = uvs.get(i);
      float[] n = new float[normals.size()];
      for (int i = 0; i < n.length; i++) n[i] = normals.get(i);
      return new BakedMesh.Mesh(t, u, n);
   }

   private static float[] point(float phi, float th) {
      float sinPhi = (float)Math.sin(phi);
      return new float[]{sinPhi * (float)Math.cos(th), (float)Math.cos(phi), sinPhi * (float)Math.sin(th)};
   }

   private static void addTri(List<Float> tris, float[] a, float[] b, float[] c) {
      for (float[] v : new float[][]{a, b, c}) {
         tris.add(v[0]);
         tris.add(v[1]);
         tris.add(v[2]);
      }
   }

   public static void submit(LevelRenderContext ctx) {
      Minecraft mc = Minecraft.getInstance();
      // The MCSM look has no dome covering the sky: the sphere only renders
      // when the legacy toggle is explicitly switched back on.
      if (mc.level == null || ClientDistantStormManager.all().isEmpty() || !DabyWSClientConfig.shieldDome) {
         return;
      }
      float gt = (float)(mc.level.getGameTime() % 240000L) + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      float nowSec = gt * 0.05F;
      Vec3 cam = ctx.levelState().cameraRenderState.pos;
      PoseStack poseStack = ctx.poseStack();
      SubmitNodeCollector collector = ctx.submitNodeCollector();

      float[] col = new float[3];
      StormPalettes.haloShieldColor(col);

      for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
         float phase = d.phase;
         if (phase < 4.0F) {
            continue; // shield timeline starts exactly at Phase 4
         }
         double bodyR = StormPresenceFX.bodyRadius(phase);
         double breathe = 0.75 + 0.25 * Math.sin(nowSec * 1.1 + d.entityId);
         float spin = nowSec * 6.0F + d.entityId;

         // Main shield: persistent from Phase 4 through every later phase.
         Vec3 centre = new Vec3(d.dispX, d.dispY, d.dispZ);
         shield(poseStack, collector, cam, centre, bodyR * 1.45, col, breathe, spin);

         // Phase 6 split-heads: cleanly mirror the same 3D shield across all
         // three heads (main body above + both severed halves) simultaneously.
         if (phase >= 6.0F) {
            for (WitherStormPositionPacket.SeveredData s : d.severed) {
               Vec3 head = new Vec3(s.x(), s.y(), s.z());
               shield(poseStack, collector, cam, head, bodyR * 0.85, col, breathe, spin + 90.0F);
            }
         }

         // Phase 7: maximize purple flares — periodic additive 3D glow-sphere
         // bursts over the main body, every severed head, plus a cinematic
         // sky-spanning purple glare bloom high above the storm.
         if (phase >= 7.0F) {
            long gtTicks = mc.level.getGameTime();
            long inWindow = gtTicks % FLARE_PERIOD_TICKS;
            float spike = (float)Math.exp(-(double)inWindow / (double)FLARE_SPIKE_TICKS);
            float base = 0.22F + 0.10F * (float)Math.sin(nowSec * 0.9 + d.entityId);
            int a = (int)(Mth.clamp(base + 0.55F * spike, 0.0F, 1.0F) * 255.0F);
            if (a > 4) {
               flare(poseStack, collector, cam, centre, bodyR * 2.4, 140, 51, 242, a, spin);
               flare(poseStack, collector, cam, centre, bodyR * 3.0, 170, 80, 250, (int)(a * 0.55F), spin);
               // Sky-spanning purple glare bloom high above the storm.
               Vec3 skyGlow = centre.add(0.0, bodyR * 4.2, 0.0);
               flare(poseStack, collector, cam, skyGlow, bodyR * 5.0, 120, 40, 235, (int)(a * 0.45F), spin);
               flare(poseStack, collector, cam, skyGlow, bodyR * 7.0, 90, 28, 220, (int)(a * 0.28F), spin);
               for (WitherStormPositionPacket.SeveredData s : d.severed) {
                  Vec3 head = new Vec3(s.x(), s.y(), s.z());
                  flare(poseStack, collector, cam, head, bodyR * 1.3, 140, 51, 242, (int)(a * 0.7F), spin + 90.0F);
               }
            }
         }
      }
   }

   /**
    * The 3D emissive shield: depth-tested translucent shell + additive glow +
    * bloom-source feed. `cam` drives the per-vertex rim so the shell fades
    * smoothly from centre to silhouette.
    */
   private static void shield(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cam, Vec3 at, double radius, float[] col, double breathe, float spin) {
      BakedMesh.Mesh m = sphere();
      if (m.tris().length == 0) {
         return;
      }
      int r = (int)(col[0] * 255.0F);
      int g = (int)(col[1] * 255.0F);
      int b = (int)(col[2] * 255.0F);
      int aShell = (int)(145.0 * breathe);
      int aGlow = (int)(72.0 * breathe);
      // Depth-tested translucent shell: true transparency, occluded by terrain.
      collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(WHITE), (pose, consumer) -> emit(consumer, pose, m, cam, at, radius, spin, r, g, b, aShell, 0.30F, 2.0F));
      // Additive emissive pass on top so the shield glows like a barrier.
      collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE), (pose, consumer) -> emit(consumer, pose, m, cam, at, radius, spin, r, g, b, aGlow, 0.20F, 1.5F));
      // Localized screen-space bloom: feed the halo into the mod's bloom
      // source target (storm_bloom_extract -> blur -> combine) so it bleeds
      // light into the frame exactly where the shield sits.
      collector.submitCustomGeometry(poseStack, GlowRenderTypes.bloomSource(WHITE), (pose, consumer) -> emit(consumer, pose, m, cam, at, radius, spin, r, g, b, aGlow, 0.20F, 1.5F));
   }

   /** Phase 7 purple flare: additive 3D glow sphere (volumetric, no sprite). */
   private static void flare(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cam, Vec3 at, double radius, int r, int g, int b, int alpha, float spin) {
      if (alpha <= 2) {
         return;
      }
      BakedMesh.Mesh m = sphere();
      if (m.tris().length == 0) {
         return;
      }
      collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE), (pose, consumer) -> emit(consumer, pose, m, cam, at, radius, spin, r, g, b, alpha, 0.55F, 1.0F));
      collector.submitCustomGeometry(poseStack, GlowRenderTypes.bloomSource(WHITE), (pose, consumer) -> emit(consumer, pose, m, cam, at, radius, spin, r, g, b, alpha, 0.55F, 1.0F));
   }

   /**
    * Emits the sphere mesh as REAL triangles (exactly 3 vertices each) with
    * per-vertex normals and a view-dependent rim factor:
    *   edgeAlpha = core + (1 - core) * pow(1 - |normal.view|, rimPow)
    * so the shell alpha blends smoothly from its centre to its silhouette —
    * no hard sprite edge, no degenerate quads, no UV tricks.
    */
   private static void emit(VertexConsumer c, PoseStack.Pose p, BakedMesh.Mesh m, Vec3 cam, Vec3 pos, double radius, float spin, int r, int g, int b, int a, float core, double rimPow) {
      double y = Math.toRadians(spin);
      double cos = Math.cos(y);
      double sin = Math.sin(y);
      float[] tris = m.tris();
      float[] normals = m.normals();
      int triCount = tris.length / 9;
      for (int i = 0; i < triCount; i++) {
         for (int q = 0; q < 3; q++) {
            int vi = i * 9 + q * 3;
            float px = tris[vi] * (float)radius;
            float py = tris[vi + 1] * (float)radius;
            float pz = tris[vi + 2] * (float)radius;
            float rx = (float)(px * cos - pz * sin);
            float rz = (float)(px * sin + pz * cos);
            float wx = (float)pos.x + rx;
            float wy = (float)pos.y + py;
            float wz = (float)pos.z + rz;
            // per-vertex sphere normal, spun with the shell
            int ni = i * 9 + q * 3;
            float nx = (float)(normals[ni] * cos - normals[ni + 2] * sin);
            float nz = (float)(normals[ni] * sin + normals[ni + 2] * cos);
            float ny = normals[ni + 1];
            // view direction from the camera to this vertex
            float vx = (float)(cam.x - wx);
            float vy = (float)(cam.y - wy);
            float vz = (float)(cam.z - wz);
            float vl = (float)Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (vl < 1.0E-4F) {
               vl = 1.0F;
            }
            vx /= vl;
            vy /= vl;
            vz /= vl;
            float ndv = Math.abs(nx * vx + ny * vy + nz * vz);
            float rim = 1.0F - ndv;
            float edge = core + (1.0F - core) * (float)Math.pow(rim, rimPow);
            int va = Mth.clamp((int)(a * edge + 0.5F), 0, 255);
            c.addVertex(p, wx, wy, wz)
               .setColor(r, g, b, va)
               .setUv(0.0F, 0.0F)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(FULL_BRIGHT)
               .setNormal(p, nx, ny, nz);
         }
      }
   }
}
