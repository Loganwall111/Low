package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * MCSM: fog tint that drives the shader storm gate — ONLY while a real
 * phase-5+ storm is nearby. Calm night stays deep blue; purple/pink/teal fog
 * is phase-locked to the user strips.
 */
public final class StormSkyDome {
   // phase 5 teal
   private static final float[] TEAL = new float[]{0.060F, 0.280F, 0.270F};
   // phase 5.4 purple
   private static final float[] PURP = new float[]{0.280F, 0.080F, 0.380F};
   // phase 5.5 pink-magenta
   private static final float[] PINK = new float[]{0.620F, 0.160F, 0.480F};
   // phase 6+ deep purple / rose
   private static final float[] SIX = new float[]{0.420F, 0.100F, 0.360F};
   private static final double RANGE = 900.0;
   private static float displayed;
   private static float displayedCore;
   private static float phaseSeen;

   private StormSkyDome() {
   }

   public static void update(Vec3 var0) {
      float var1 = 0.0F;
      float var2 = 0.0F;
      float var3 = 0.0F;

      for (ClientDistantStormManager.StormData var5 : ClientDistantStormManager.all()) {
         // only phase 5+ owns the fog/sky tint
         if (!(var5.phase < 4.95F)) {
            double var6 = var5.dispX - var0.x;
            double var8 = var5.dispY - var0.y;
            double var10 = var5.dispZ - var0.z;
            double var12 = Math.sqrt(var6 * var6 + var8 * var8 + var10 * var10);
            if (!(var12 > RANGE)) {
               double var14 = var12 / RANGE;
               float var16 = var14 <= 0.55 ? 1.0F : smooth((float)(1.0 - (var14 - 0.55) / 0.45));
               float var17 = ramp(var5.phase, 4.95F, 5.15F);
               float var18 = var16 * var17;
               if (var18 > var1) {
                  var1 = var18;
                  var3 = var5.phase;
               }

               var2 = Math.max(var2, var16 * ramp(var5.phase, 4.95F, 5.35F));
            }
         }
      }

      displayed = displayed + (var1 - displayed) * 0.05F;
      displayedCore = displayedCore + (var2 - displayedCore) * 0.05F;
      if (displayed < 0.002F) {
         displayed = 0.0F;
      }

      if (displayedCore < 0.002F) {
         displayedCore = 0.0F;
      }

      if (var3 > 0.0F) {
         phaseSeen = var3;
      } else if (displayed < 0.01F) {
         phaseSeen = 0.0F;
      }
   }

   public static float strength() {
      // MCSM: fog tint only while a real phase-5+ storm is nearby AND displayed.
      // Cap at 0.72 so residual fog cannot purple-wash the calm night vault.
      if (!DabyWSClientConfig.stormBackdrop) {
         return 0.0F;
      }
      if (phaseSeen < 4.95F) {
         return 0.0F;
      }
      return Mth.clamp(displayed * (float)DabyWSClientConfig.stormBackdropStrength * 0.55F, 0.0F, 0.55F);
   }

   public static float coreStrength() {
      return Mth.clamp(displayedCore, 0.0F, 1.0F);
   }

   public static float phase() {
      return phaseSeen;
   }

   public static void skyColor(float[] var0) {
      float p = phaseSeen;
      float wTeal = ramp(p, 4.95F, 5.10F) * (1.0F - ramp(p, 5.30F, 5.42F));
      float wPurp = ramp(p, 5.30F, 5.42F) * (1.0F - ramp(p, 5.52F, 5.65F));
      float wPink = ramp(p, 5.52F, 5.65F) * (1.0F - ramp(p, 5.92F, 6.10F));
      float wSix  = ramp(p, 5.92F, 6.15F);
      float tot = wTeal + wPurp + wPink + wSix;
      if (tot <= 1.0E-4F || p < 4.9F) {
         // no phase tint — leave fog alone (calm blue night)
         var0[0] = 0.012F;
         var0[1] = 0.035F;
         var0[2] = 0.360F;
      } else {
         var0[0] = (TEAL[0] * wTeal + PURP[0] * wPurp + PINK[0] * wPink + SIX[0] * wSix) / tot;
         var0[1] = (TEAL[1] * wTeal + PURP[1] * wPurp + PINK[1] * wPink + SIX[1] * wSix) / tot;
         var0[2] = (TEAL[2] * wTeal + PURP[2] * wPurp + PINK[2] * wPink + SIX[2] * wSix) / tot;
      }
   }

   private static float ramp(float var0, float var1, float var2) {
      if (var2 <= var1) {
         return var0 >= var2 ? 1.0F : 0.0F;
      } else {
         return smooth(Mth.clamp((var0 - var1) / (var2 - var1), 0.0F, 1.0F));
      }
   }

   private static float smooth(float var0) {
      var0 = Mth.clamp(var0, 0.0F, 1.0F);
      return var0 * var0 * (3.0F - 2.0F * var0);
   }
}
