package net.dabicco.witherstormmod.client.particle;

import net.dabicco.witherstormmod.entity.WitherStormHeadEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class BeamMoteParticle extends SingleQuadParticle {
   public static WitherStormHeadEntity pendingHead;
   public static double pendingAngle;
   public static double pendingRadialFrac;
   public static double pendingAxisT;
   public static double pendingClimbPerTick;
   public static double pendingBaseRadius;
   public static float pendingBeamScale = 1.0F;
   public static float pendingR = 0.55F;
   public static float pendingG = 0.35F;
   public static float pendingB = 0.98F;
   private final WitherStormHeadEntity head;
   private final double angle;
   private final double radialFrac;
   private final double climbPerTick;
   private final double baseRadius;
   private final float beamScale;
   private double axisT;
   private final float baseSize;
   private int detachedTicks = -1;

   private BeamMoteParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
      super(level, x, y, z, sprite);
      this.head = pendingHead;
      this.angle = pendingAngle;
      this.radialFrac = pendingRadialFrac;
      this.axisT = pendingAxisT;
      this.climbPerTick = pendingClimbPerTick;
      this.baseRadius = pendingBaseRadius;
      this.beamScale = pendingBeamScale;
      float pr = pendingR;
      float pg = pendingG;
      float pb = pendingB;
      pendingHead = null;
      pendingBeamScale = 1.0F;
      pendingR = 0.55F;
      pendingG = 0.35F;
      pendingB = 0.98F;
      this.xd = 0.0;
      this.yd = 0.0;
      this.zd = 0.0;
      this.friction = 1.0F;
      this.gravity = 0.0F;
      this.hasPhysics = false;
      this.lifetime = 400;
      // MCSM ref: fine translucent sparkles inside the beam (NOT chunky cubes)
      this.baseSize = (0.032F + this.random.nextFloat() * 0.028F) * Math.max(this.beamScale, 0.22F);
      this.quadSize = this.baseSize;
      this.rCol = pr;
      this.gCol = pg;
      this.bCol = pb;
      this.alpha = 0.0F;
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public int getLightCoords(float partialTick) {
      return 15728880;
   }

   private boolean beamStillThere() {
      return this.head != null && this.head.isAlive() && this.head.isBeamActive() ? this.level.getEntity(this.head.getId()) == this.head : false;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         Vec3 ground = this.beamStillThere() ? (this.head.clientBeamEnd != null ? this.head.clientBeamEnd : this.head.getBeamEndExact()) : null;
         if (ground == null) {
            if (this.detachedTicks < 0) {
               this.detachedTicks = 0;
            }

            this.alpha = Math.max(0.0F, this.alpha - 0.15F);
            if (++this.detachedTicks > 8) {
               this.remove();
            }
         } else {
            this.detachedTicks = -1;
            this.axisT = Math.min(this.axisT + this.climbPerTick, 1.0);
            if (this.axisT >= 0.92) {
               this.remove();
            } else {
               Vec3 published = net.dabicco.witherstormmod.client.TractorBeamRenderer.eyeWorld(this.head.getId());
               Vec3 tip = published != null ? published : this.head.position();
               Vec3 axis = tip.subtract(ground);
               if (!(axis.lengthSqr() < 1.0E-4)) {
                  Vec3 d = axis.normalize();
                  Vec3 up = Math.abs(d.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
                  Vec3 right = d.cross(up).normalize();
                  Vec3 upB = right.cross(d);
                  double radius = Mth.lerp(
                        this.axisT, net.dabicco.witherstormmod.client.TractorBeamRenderer.baseHalfWidth((float)this.baseRadius), 0.3F * this.beamScale
                     )
                     * this.radialFrac;
                  Vec3 p = ground.add(axis.scale(this.axisT)).add(right.scale(Math.cos(this.angle) * radius)).add(upB.scale(Math.sin(this.angle) * radius));
                  this.setPos(p.x, p.y, p.z);
                  float in = Math.min(this.age / 5.0F, 1.0F);
                  float out = this.axisT < 0.62 ? 1.0F : (float)(1.0 - (this.axisT - 0.62) / 0.3);
                  this.alpha = 0.42F * in * Math.max(out, 0.0F); // translucent sparkle, not solid cubes
                  this.quadSize = this.baseSize * (1.0F - 0.3F * (float)this.axisT);
               }
            }
         }
      }
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz, RandomSource random
      ) {
         return new BeamMoteParticle(level, x, y, z, this.sprites.get(random));
      }
   }
}
