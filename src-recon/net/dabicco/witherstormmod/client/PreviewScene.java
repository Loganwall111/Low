package net.dabicco.witherstormmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class PreviewScene {
   public float field = 60.0F;
   public float groundY = -8.0F;
   public int haze = -7362108;
   public float tile = 4.0F;
   public float sunX = 0.42F;
   public float sunY = 0.82F;
   public float sunZ = -0.29F;
   public float pitch;
   public boolean castShadow = true;
   public int beams;
   public int backdropIndex = 0;
   // Sky previews are native state previews now; no texture-pack backdrop is
   // loaded by the preview scene.
   public static final String[] BACKDROP_LABELS = new String[]{"Default Sky"};
   private static final Identifier GRASS_TOP = Identifier.withDefaultNamespace("textures/block/grass_block_top.png");
   private static final int[] GRASS_TINT = new int[]{124, 178, 94};
   private static final int CELLS_MAX = 30;
   private static final float HAZE_FROM = 0.35F;
   private static final int FULL_BRIGHT = 15728880;
   private static final Identifier SUN_SHEET = Identifier.withDefaultNamespace("textures/environment/celestial/sun.png");
   private static final float SUN_DIST = 0.88F;
   private static final float SUN_SIZE = 0.16F;
   public static final float SHADOW_LIFT = 0.05F;
   public static final int SHADOW_TINT = 940578856;
   private static final Identifier SHADOW_SHEET = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/tractor_beam.png");

   /** The preview uses the same native SkyRenderer state as the world. */
   public void submitSky(PoseStack poseStack, SubmitNodeCollector collector) {
      // Intentionally empty: never create a reachable sky billboard.
   }

   public void submitGround(PoseStack poseStack, SubmitNodeCollector collector) {
      if (!(this.field <= 0.0F)) {
         float step = Math.max(0.5F, this.tile);
         int cells = Math.max(1, Math.min(30, Mth.ceil(2.0F * this.field / step)));
         float half = cells * step * 0.5F;
         float y = this.groundY;
         int hazeColour = this.haze;
         collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(GRASS_TOP), (pose, consumer) -> {
            for (int ix = 0; ix < cells; ix++) {
               float x0 = -half + ix * step;
               float x1 = x0 + step;

               for (int iz = 0; iz < cells; iz++) {
                  float z0 = -half + iz * step;
                  float z1 = z0 + step;
                  fieldVertex(pose, consumer, x0, y, z0, 0.0F, 0.0F, half, hazeColour);
                  fieldVertex(pose, consumer, x0, y, z1, 0.0F, 1.0F, half, hazeColour);
                  fieldVertex(pose, consumer, x1, y, z1, 1.0F, 1.0F, half, hazeColour);
                  fieldVertex(pose, consumer, x1, y, z0, 1.0F, 0.0F, half, hazeColour);
               }
            }
         });
      }
   }

   private static void fieldVertex(Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, float halfWidth, int haze) {
      float d = Math.min(1.0F, (float)Math.sqrt(x * x + z * z) / halfWidth);
      float t = Mth.clamp((d - 0.35F) / 0.65F, 0.0F, 1.0F);
      t = t * t * (3.0F - 2.0F * t);
      vertex(
         pose,
         consumer,
         x,
         y,
         z,
         u,
         v,
         (int)Mth.lerp(t, GRASS_TINT[0], haze >> 16 & 0xFF),
         (int)Mth.lerp(t, GRASS_TINT[1], haze >> 8 & 0xFF),
         (int)Mth.lerp(t, GRASS_TINT[2], haze & 0xFF),
         255,
         0.0F,
         1.0F,
         0.0F
      );
   }

   public void submitSun(PoseStack poseStack, SubmitNodeCollector collector) {
      float dist = this.field * 0.88F;
      float half = this.field * 0.16F * 0.5F;
      if (!(dist <= 0.0F)) {
         float cx = this.sunX * dist;
         float cy = this.sunY * dist;
         float cz = this.sunZ * dist;
         float p = this.pitch * (float) (Math.PI / 180.0);
         float rx = -half;
         float uy = Mth.cos(p) * half;
         float uz = -Mth.sin(p) * half;
         collector.submitCustomGeometry(poseStack, RenderTypes.eyes(SUN_SHEET), (pose, consumer) -> {
            vertex(pose, consumer, cx - rx, cy - uy, cz - uz, 0.0F, 0.0F, 255, 255, 255, 255, 0.0F, 0.0F, 1.0F);
            vertex(pose, consumer, cx - rx, cy + uy, cz + uz, 0.0F, 1.0F, 255, 255, 255, 255, 0.0F, 0.0F, 1.0F);
            vertex(pose, consumer, cx + rx, cy + uy, cz + uz, 1.0F, 1.0F, 255, 255, 255, 255, 0.0F, 0.0F, 1.0F);
            vertex(pose, consumer, cx + rx, cy - uy, cz - uz, 1.0F, 0.0F, 255, 255, 255, 255, 0.0F, 0.0F, 1.0F);
         });
      }
   }

   public static RenderType shadowType() {
      return RenderTypes.entityShadow(SHADOW_SHEET);
   }

   public Matrix4f flatten() {
      float ly = Math.max(this.sunY, 0.15F);
      return new Matrix4f(1.0F, 0.0F, 0.0F, 0.0F, -this.sunX / ly, 0.0F, -this.sunZ / ly, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
   }

   public void pushShadow(PoseStack poseStack) {
      poseStack.pushPose();
      poseStack.translate(0.0F, this.groundY + 0.05F, 0.0F);
      poseStack.last().pose().mul(this.flatten());
      poseStack.translate(0.0F, -this.groundY, 0.0F);
   }

   static void vertex(Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, int r, int g, int b, int a, float nx, float ny, float nz) {
      consumer.addVertex(pose, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
   }
}
