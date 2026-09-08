package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.util.Mth;

/** 1.9.153: phase fog palettes from user strips (teal / purple / pink / six). */
public final class StormPalettes {
   private static final float[] FOG_TEAL = new float[]{0.060F, 0.280F, 0.270F};
   private static final float[] FOG_PURPLE = new float[]{0.280F, 0.080F, 0.380F};
   private static final float[] FOG_PINK = new float[]{0.620F, 0.160F, 0.480F};
   private static final float[] FOG_SIX = new float[]{0.420F, 0.100F, 0.360F};
   private static final float[] PULSE_EARLY = new float[]{0.42F, 0.33F, 0.95F};
   private static final float[] PULSE_FIVE = new float[]{0.38F, 0.52F, 0.98F};
   private static final float[] PULSE_CATACLYSM = new float[]{0.48F, 0.2F, 0.72F};
   private static final float[] HALO_RING = new float[]{0.36F, 0.46F, 1.0F};
   private static final float[] HALO_UNDER = new float[]{0.92F, 0.94F, 1.0F};
   private static final float[] CLOUD_PURPLE = new float[]{0.115F, 0.095F, 0.135F};
   private static final float[] CLOUD_TURQUOISE = new float[]{0.05F, 0.22F, 0.2F};
   private static final float[] CLOUD_CATACLYSM = new float[]{0.045F, 0.03F, 0.08F};
   private static final float[] STAR_WHITE = new float[]{0.9F, 0.92F, 1.0F};
   private static final float[] STAR_PURPLE = new float[]{0.74F, 0.58F, 1.0F};
   private static final float[] STAR_TEAL = new float[]{0.45F, 0.95F, 0.85F};
   public static final float[] EJECTA_TEAL = new float[]{0.2F, 0.95F, 0.72F};
   public static final float[] EJECTA_GREEN = new float[]{0.35F, 0.88F, 0.3F};
   public static final float[] EJECTA_PALE = new float[]{0.8F, 0.68F, 1.0F};

   private StormPalettes() {
   }

   public static float strength() {
      return Mth.clamp((float)DabyWSClientConfig.paletteStrength, 0.0F, 1.0F);
   }

   /** w[0]=teal(p5), w[1]=purple(p5.4), w[2]=pink(p5.5), w[3]=six(p6+) */
   public static void stageWeights(double phase, float[] w) {
      float teal = smooth(clamp01((float)((phase - 4.95) / 0.20))) * (1.0F - smooth(clamp01((float)((phase - 5.30) / 0.12))));
      float purp = smooth(clamp01((float)((phase - 5.30) / 0.12))) * (1.0F - smooth(clamp01((float)((phase - 5.52) / 0.13))));
      float pink = smooth(clamp01((float)((phase - 5.52) / 0.13))) * (1.0F - smooth(clamp01((float)((phase - 5.92) / 0.18))));
      float six  = smooth(clamp01((float)((phase - 5.92) / 0.23)));
      float tot = teal + purp + pink + six;
      if (tot < 1.0E-4F) {
         w[0] = 0; w[1] = 0; w[2] = 0;
         if (w.length > 3) w[3] = 0;
         return;
      }
      w[0] = teal / tot;
      w[1] = purp / tot;
      w[2] = pink / tot;
      if (w.length > 3) w[3] = six / tot;
   }

   private static float clamp01(float v) {
      return Mth.clamp(v, 0.0F, 1.0F);
   }

   private static float smooth(float t) {
      t = clamp01(t);
      return t * t * (3.0F - 2.0F * t);
   }

   public static float[] fogColor(double phase, float[] out) {
      float[] w = new float[4];
      stageWeights(phase, w);
      float tot = w[0] + w[1] + w[2] + w[3];
      if (tot < 1.0E-4F) {
         out[0] = FOG_TEAL[0]; out[1] = FOG_TEAL[1]; out[2] = FOG_TEAL[2];
         return out;
      }
      out[0] = FOG_TEAL[0]*w[0] + FOG_PURPLE[0]*w[1] + FOG_PINK[0]*w[2] + FOG_SIX[0]*w[3];
      out[1] = FOG_TEAL[1]*w[0] + FOG_PURPLE[1]*w[1] + FOG_PINK[1]*w[2] + FOG_SIX[1]*w[3];
      out[2] = FOG_TEAL[2]*w[0] + FOG_PURPLE[2]*w[1] + FOG_PINK[2]*w[2] + FOG_SIX[2]*w[3];
      return out;
   }

   public static float[] pulseColor(double phase, float[] out) {
      float[] w = new float[4];
      stageWeights(phase, w);
      // map teal→five, purple/pink→cata, early fallback
      float early = Math.max(0.0F, 1.0F - (w[0]+w[1]+w[2]+w[3]));
      out[0] = PULSE_EARLY[0]*early + PULSE_FIVE[0]*w[0] + PULSE_CATACLYSM[0]*(w[1]+w[2]+w[3]);
      out[1] = PULSE_EARLY[1]*early + PULSE_FIVE[1]*w[0] + PULSE_CATACLYSM[1]*(w[1]+w[2]+w[3]);
      out[2] = PULSE_EARLY[2]*early + PULSE_FIVE[2]*w[0] + PULSE_CATACLYSM[2]*(w[1]+w[2]+w[3]);
      return out;
   }

   public static float[] cloudColor(double phase, float[] out) {
      float[] w = new float[4];
      stageWeights(phase, w);
      out[0] = CLOUD_TURQUOISE[0]*w[0] + CLOUD_PURPLE[0]*w[1] + CLOUD_CATACLYSM[0]*(w[2]+w[3]);
      out[1] = CLOUD_TURQUOISE[1]*w[0] + CLOUD_PURPLE[1]*w[1] + CLOUD_CATACLYSM[1]*(w[2]+w[3]);
      out[2] = CLOUD_TURQUOISE[2]*w[0] + CLOUD_PURPLE[2]*w[1] + CLOUD_CATACLYSM[2]*(w[2]+w[3]);
      return out;
   }

   public static float[] haloRingColor(float[] out) {
      out[0] = HALO_RING[0]; out[1] = HALO_RING[1]; out[2] = HALO_RING[2];
      return out;
   }

   public static float[] haloUnderColor(float[] out) {
      out[0] = HALO_UNDER[0]; out[1] = HALO_UNDER[1]; out[2] = HALO_UNDER[2];
      return out;
   }

   public static float[] starColor(int kind, float tealAmount, float[] out) {
      float[] base = kind == 1 ? STAR_PURPLE : STAR_WHITE;
      out[0] = Mth.lerp(tealAmount, base[0], STAR_TEAL[0]);
      out[1] = Mth.lerp(tealAmount, base[1], STAR_TEAL[1]);
      out[2] = Mth.lerp(tealAmount, base[2], STAR_TEAL[2]);
      return out;
   }
}
