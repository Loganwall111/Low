package net.dabicco.witherstormmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * StormVortexFX — the phase-4+ abduction vortex.
 *
 * <p>When the storm arrives it starts tearing the world apart from below:
 * block pellets rip off the ground and spiral upward into the storm like a
 * tornado. Two layers sell it:
 *
 * <ul>
 *   <li>A custom-geometry helix of small dark cubes (always visible, cheap,
 *       shader-proof) funneling from a wide ground ring into the storm's
 *       underside.</li>
 *   <li>Real block-crack particles sampled from the actual ground blocks
 *       under the storm, flung upward with tangential velocity (spawned in
 *       {@link #tick(Minecraft)}).</li>
 * </ul>
 */
public final class StormVortexFX {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/tractor_beam.png");
   private static final int FULL_BRIGHT = 15728880;
   private static final int VORTEX = 560;
   private static final float[] H0 = new float[VORTEX];
   private static final float[] ANG0 = new float[VORTEX];
   private static final float[] RADIAL = new float[VORTEX];
   private static final float[] SIZE = new float[VORTEX];
   private static final float[] SHADE = new float[VORTEX];
   private static final float[] SPEED = new float[VORTEX];
   private static final float[] WOB = new float[VORTEX];

   private StormVortexFX() {
   }

   public static void submit(LevelRenderContext ctx) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || ClientDistantStormManager.all().isEmpty() || !DabyWSClientConfig.vortexDebris) {
         return;
      }
      float strength = (float)Mth.clamp(DabyWSClientConfig.vortexStrength, 0.0, 2.0);
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
         if (centre.distanceToSqr(cam) > 810000.0) {
            continue; // ~900 blocks: past this the spiral is sub-pixel
         }
         // Thin funnel at phase 4 that thickens into a full tornado by phase 6+.
         float ramp = Mth.clamp((phase - 4.0F) / 2.0F, 0.0F, 1.0F);
         ramp = ramp * ramp * (3.0F - 2.0F * ramp);
         float density = Mth.clamp(strength * (0.25F + 0.75F * ramp), 0.0F, 2.0F);
         int keepOf100 = (int)(density * 50.0F);
         if (keepOf100 <= 0) {
            continue;
         }
         double groundY = d.dispY - bodyR * 1.7;
         double topY = d.dispY + bodyR * 0.35;
         double groundR = bodyR * (1.05 + 0.25 * ramp);
         double topR = bodyR * 0.22;
         final float fRamp = ramp;
         collector.submitCustomGeometry(poseStack, FoglessRenderTypes.bodyCutout(TEXTURE), (pose, consumer) -> {
            for (int i = 0; i < VORTEX; i++) {
               if (keepOf100 < 100 && (i * 61 % 100) >= keepOf100) {
                  continue;
               }
               float h = H0[i] + t * SPEED[i];
               h -= (float)Math.floor(h);
               double y = Mth.lerp(h, groundY, topY);
               double radius = Mth.lerp(h * h * (3.0 - 2.0 * h), groundR, topR) * RADIAL[i];
               // The funnel spins faster as it tightens, like a real tornado.
               float ang = ANG0[i] + t * (1.1F + 2.6F * h) + WOB[i] * Mth.sin(t * 0.9F + (float)i);
               double x = d.dispX + Math.cos(ang) * radius;
               double z = d.dispZ + Math.sin(ang) * radius;
               float size = SIZE[i] * (0.55F + 0.65F * h) * (0.7F + 0.6F * fRamp);
               float fade = Math.min(1.0F, h * 9.0F) * Math.min(1.0F, (1.0F - h) * 5.0F + 0.25F);
               if (fade <= 0.02F) {
                  continue;
               }
               int s = (int)(SHADE[i] * fade);
               int r;
               int g;
               int b;
               if (i % 9 == 0) {
                  // scattered violet-hot pellets feeding the storm's glow
                  r = (int)(150.0F * fade) + s;
                  g = (int)(60.0F * fade) + s / 2;
                  b = (int)(230.0F * fade) + s;
               } else {
                  r = (int)(s * 0.95F);
                  g = (int)(s * 0.78F);
                  b = (int)Math.min(255.0F, s * 1.35F);
               }
               cube(pose, consumer, (float)x, (float)y, (float)z, size, r, g, b, FULL_BRIGHT);
            }
         });
      }
   }

   /** Client tick: rips real block-crack particles off the ground under every late storm. */
   public static void tick(Minecraft mc) {
      ClientLevel level = mc.level;
      if (level == null || mc.isPaused() || !DabyWSClientConfig.vortexDebris) {
         return;
      }
      float strength = (float)Mth.clamp(DabyWSClientConfig.vortexStrength, 0.0, 2.0);
      if (strength <= 0.01F) {
         return;
      }
      for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
         float phase = d.phase;
         if (phase < 4.0F || d.collapsed) {
            continue;
         }
         double bodyR = StormPresenceFX.bodyRadius(phase);
         float ramp = Mth.clamp((phase - 4.0F) / 2.0F, 0.15F, 1.0F);
         int puffs = Math.max(1, (int)(strength * ramp * 3.0F));
         for (int k = 0; k < puffs; k++) {
            double a = level.random.nextDouble() * Math.PI * 2.0;
            double rr = bodyR * (0.5 + level.random.nextDouble() * 0.9);
            int bx = Mth.floor(d.dispX + Math.cos(a) * rr);
            int bz = Mth.floor(d.dispZ + Math.sin(a) * rr);
            BlockPos top;
            try {
               top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(bx, 0, bz));
            } catch (Exception ignored) {
               continue;
            }
            if (top.getY() <= level.getMinY() + 1) {
               continue;
            }
            BlockPos ground = top.below();
            BlockState state;
            try {
               state = level.getBlockState(ground);
            } catch (Exception ignored) {
               continue;
            }
            if (state.isAir() || !state.getFluidState().isEmpty()) {
               continue;
            }
            double px = bx + 0.15 + level.random.nextDouble() * 0.7;
            double py = ground.getY() + 1.05;
            double pz = bz + 0.15 + level.random.nextDouble() * 0.7;
            // tangential whip + hard upward yank: pellets join the spiral
            double tangential = 1.6 + level.random.nextDouble() * 2.2;
            double vx = -Math.sin(a) * tangential * 0.45;
            double vz = Math.cos(a) * tangential * 0.45;
            double vy = 1.4 + level.random.nextDouble() * 1.8;
            try {
               level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), px, py, pz, vx, vy, vz);
            } catch (Exception ignored) {
               // unloaded chunk / missing particle sprite: the geometry spiral carries on
            }
         }
      }
   }

   private static void cube(PoseStack.Pose pose, VertexConsumer c, float x, float y, float z, float h, int r, int g, int b, int light) {
      // axis-aligned pellet, 3 brightness-graded face pairs: crisp MCSM cubes
      face(pose, c, x - h, y - h, z - h, x + h, y - h, z - h, x + h, y - h, z + h, x - h, y - h, z + h, r / 2, g / 2, b / 2, light);
      face(pose, c, x - h, y + h, z + h, x + h, y + h, z + h, x + h, y + h, z - h, x - h, y + h, z - h, Math.min(255, r + 26), Math.min(255, g + 26), Math.min(255, b + 26), light);
      face(pose, c, x - h, y - h, z + h, x + h, y - h, z + h, x + h, y + h, z + h, x - h, y + h, z + h, r, g, b, light);
      face(pose, c, x + h, y - h, z - h, x - h, y - h, z - h, x - h, y + h, z - h, x + h, y + h, z - h, r, g, b, light);
      face(pose, c, x + h, y - h, z + h, x + h, y - h, z - h, x + h, y + h, z - h, x + h, y + h, z + h, (r * 3) / 4, (g * 3) / 4, (b * 3) / 4, light);
      face(pose, c, x - h, y - h, z - h, x - h, y - h, z + h, x - h, y + h, z + h, x - h, y + h, z - h, (r * 3) / 4, (g * 3) / 4, (b * 3) / 4, light);
   }

   private static void face(PoseStack.Pose pose, VertexConsumer c, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int r, int g, int b, int light) {
      vert(pose, c, ax, ay, az, 0.0F, 0.0F, r, g, b, light);
      vert(pose, c, bx, by, bz, 1.0F, 0.0F, r, g, b, light);
      vert(pose, c, cx, cy, cz, 1.0F, 1.0F, r, g, b, light);
      vert(pose, c, dx, dy, dz, 0.0F, 1.0F, r, g, b, light);
   }

   private static void vert(PoseStack.Pose pose, VertexConsumer c, float x, float y, float z, float u, float v, int r, int g, int b, int light) {
      c.addVertex(pose, x, y, z)
         .setColor(Math.min(255, r), Math.min(255, g), Math.min(255, b), 255)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   static {
      RandomSource random = RandomSource.create(424242L);
      for (int i = 0; i < VORTEX; i++) {
         H0[i] = random.nextFloat();
         ANG0[i] = random.nextFloat() * (float)Math.PI * 2.0F;
         RADIAL[i] = 0.75F + random.nextFloat() * 0.5F;
         SIZE[i] = 0.14F + random.nextFloat() * 0.30F;
         SHADE[i] = 16.0F + random.nextFloat() * 34.0F;
         SPEED[i] = 0.10F + random.nextFloat() * 0.16F;
         WOB[i] = (random.nextFloat() - 0.5F) * 0.6F;
      }
   }
}
