package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Devouring Storms: mega-phase 8 - the PHASE SKY and the sky-glued HALO.
 *
 * Two orders from the reference frames are answered here.
 *
 * 1. THE PHASE SKY. The big glowing purple sky is not "night" - it is
 *    phases 5.4 to 5.9, and nothing else. The night and midnight gradients
 *    have been handed back to a true deep blue in both sky shaders; the
 *    purple vault and its fog now live here, on a dome the mod paints only
 *    while the storm is in that window. Past 5.9 the purple leaves and the
 *    vault turns LIGHT PINK, exactly as the frames end.
 *
 * 2. THE HALO. The old halo hung far off the storm and carried three heads
 *    and a symbol - that texture is gone. This one is pure colour palette:
 *    soft gradient geometry, no face, drawn in sky space at the storm's own
 *    bearing so it rides with both the sky and the storm, always facing the
 *    camera so it still reads when the player walks around behind it, and
 *    small enough that the ordinary sky stays visible around its rim.
 *
 *    Riding on it, for 5.5 to 5.9, is the silhouette stack the frames show:
 *    the mod's dark-blue over-storm light, a dark purple layer laid on top
 *    of it and wrapped around the back, then purple -> dark moon-blue ->
 *    a black glow capping the storm that takes the top of the ATMOSPHERE
 *    (not the storm) out of the picture entirely.
 *
 * Everything is drawn at a greater sky radius than McsmStormBlob's 220, so
 * the dome and the halo sit behind the silhouette, and the whole class is
 * wrapped in a catch so an unexpected surface degrades to no sky, never a
 * crash.
 */
public final class McsmPhaseSky {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");
    private static final Identifier GLARE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_glare.png");

    /** Sky radius for the dome. The halo is a thick multi-depth shell that
     *  sits just behind the body shell (McsmStormBlob placeDist ≤ ~280), so
     *  it reads as glued to the storm rather than a far disc. */
    private static final double DOME_R = 480.0;
    private static final double HALO_R = 260.0;
    /** Depth slices (along the view axis, in world units) that give the halo
     *  real thickness — visible from behind, moves with the storm. */
    private static final double[] HALO_DEPTH = { -40.0, -18.0, 0.0, 16.0, 32.0 };
    private static final float[] HALO_SCALE = { 1.28F, 1.14F, 1.00F, 1.10F, 1.22F };
    private static final float[] HALO_ALPHA = { 0.55F, 0.75F, 1.00F, 0.70F, 0.45F };

    private static final int LON = 28;
    private static final int LAT = 14;

    private McsmPhaseSky() {
    }

    private static float ramp(float v, float lo, float hi) {
        if (hi <= lo) {
            return v >= hi ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            submitInner(ctx);
        } catch (Throwable ignored) {
            // never take the level render down with us
        }
    }

    private static void submitInner(LevelRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ClientDistantStormManager.all().isEmpty()) {
            return;
        }
        Vec3 cam = ctx.levelState().cameraRenderState.pos;

        // the nearest storm owns the sky and the halo
        float phase = 0.0F;
        Vec3 bearing = null;
        double best = Double.MAX_VALUE;
        for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
            if (d.phase < 5.2F) {
                continue;
            }
            Vec3 c = new Vec3(d.dispX, d.dispY, d.dispZ);
            double dd = c.subtract(cam).length();
            if (dd < best) {
                best = dd;
                phase = d.phase;
                bearing = dd > 1.0E-4 ? c.subtract(cam).scale(1.0D / dd) : null;
            }
        }
        if (bearing == null) {
            return;
        }

        // the window: purple from 5.4, gone by 6.05, light pink after
        float wPurple = ramp(phase, 5.32F, 5.45F) * (1.0F - ramp(phase, 5.90F, 6.05F));
        float wPink = ramp(phase, 5.90F, 6.20F);
        float sky = Mth.clamp(wPurple + wPink, 0.0F, 1.0F);
        // the storm sits far enough away that the sky calms back down
        float near = 1.0F - Mth.clamp((float) ((best - 1400.0) / 900.0), 0.0F, 1.0F);
        sky *= near;
        if (sky <= 0.006F) {
            return;
        }

        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();

        dome(poseStack, collector, cam, bearing, wPurple * near, wPink * near, sky);
        halo(poseStack, collector, cam, bearing, phase, near);
    }

    /* ---- the phase vault ------------------------------------------------- */

    /**
     * Glowing purple sky for 5.4-5.9, light pink past 5.9. The gradient runs
     * zenith -> mid -> horizon and brightens toward the storm's bearing, so
     * the vault is lit FROM the creature rather than uniformly washed.
     */
    private static void dome(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cam,
            Vec3 bearing, float wPurple, float wPink, float sky) {
        // purple window (5.4-5.9): violet roof, magenta belly, glowing lilac rim
        final float[] pz = { 0.196F, 0.078F, 0.365F };
        final float[] pm = { 0.451F, 0.169F, 0.612F };
        final float[] ph = { 0.741F, 0.353F, 0.694F };
        // past 5.9: the vault turns light pink
        final float[] kz = { 0.549F, 0.376F, 0.573F };
        final float[] km = { 0.831F, 0.588F, 0.702F };
        final float[] kh = { 0.976F, 0.784F, 0.816F };

        final float pw = wPurple;
        final float kw = wPink;
        final float tot = Math.max(pw + kw, 1.0E-4F);
        final float[] zen = mix3(pz, kz, pw / tot, kw / tot);
        final float[] mid = mix3(pm, km, pw / tot, kw / tot);
        final float[] hor = mix3(ph, kh, pw / tot, kw / tot);
        final Vec3 bear = bearing;
        final float amp = sky;

        collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(WHITE),
                (pose, consumer) -> {
            for (int j = 0; j < LAT; j++) {
                double y0 = latY(j);
                double y1 = latY(j + 1);
                for (int i = 0; i < LON; i++) {
                    double a0 = i * 2.0 * Math.PI / LON;
                    double a1 = (i + 1) * 2.0 * Math.PI / LON;
                    Vec3 d00 = onDome(a0, y0);
                    Vec3 d10 = onDome(a1, y0);
                    Vec3 d11 = onDome(a1, y1);
                    Vec3 d01 = onDome(a0, y1);
                    domeVertex(pose, consumer, cam, d00, bear, zen, mid, hor, amp, 0.0F, 1.0F);
                    domeVertex(pose, consumer, cam, d10, bear, zen, mid, hor, amp, 1.0F, 1.0F);
                    domeVertex(pose, consumer, cam, d11, bear, zen, mid, hor, amp, 1.0F, 0.0F);
                    domeVertex(pose, consumer, cam, d01, bear, zen, mid, hor, amp, 0.0F, 0.0F);
                }
            }
        });
    }

    private static double latY(int j) {
        // -0.35 (below the horizon, so the fog band closes) up to the zenith
        double t = (double) j / LAT;
        return -0.35 + 1.35 * t;
    }

    private static Vec3 onDome(double az, double y) {
        double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        return new Vec3(Math.cos(az) * r, y, Math.sin(az) * r);
    }

    private static void domeVertex(Pose pose, VertexConsumer consumer, Vec3 cam, Vec3 dir,
            Vec3 bearing, float[] zen, float[] mid, float[] hor, float amp, float u, float v) {
        float y = (float) dir.y;
        float t = (float) Math.pow(1.0 - Mth.clamp(y, 0.0F, 1.0F), 1.30);
        float[] c = lerp3(zen, mid, smooth(t, 0.05F, 0.55F));
        c = lerp3(c, hor, smooth(t, 0.55F, 0.96F));

        // lit from the storm: the vault glows where the creature stands
        float toward = (float) (dir.x * bearing.x + dir.y * bearing.y + dir.z * bearing.z);
        float glow = Mth.clamp((toward - 0.10F) / 0.90F, 0.0F, 1.0F);
        glow = glow * glow * 0.55F;
        c = new float[] { c[0] * (1.0F + glow * 0.9F), c[1] * (1.0F + glow * 0.55F),
                c[2] * (1.0F + glow * 0.85F) };

        // the PHASE FOG: thickest at the horizon, thinning toward the roof, so
        // the vault swallows the distance and leaves the zenith readable
        float fog = 0.62F + 0.38F * smooth(t, 0.20F, 1.00F);
        float alpha = amp * fog * 250.0F;
        // fade out below the horizon rather than cutting off
        alpha *= Mth.clamp((y + 0.34F) / 0.30F, 0.0F, 1.0F);

        vertex(pose, consumer, cam.add(dir.scale(DOME_R)), u, v,
                (int) Mth.clamp(c[0] * 255.0F, 0.0F, 255.0F),
                (int) Mth.clamp(c[1] * 255.0F, 0.0F, 255.0F),
                (int) Mth.clamp(c[2] * 255.0F, 0.0F, 255.0F),
                (int) Mth.clamp(alpha, 0.0F, 255.0F));
    }

    /* ---- the halo and the silhouette stack -------------------------------- */

    /**
     * Pure colour palette, no heads. Three soft gradient discs in the phase's
     * own tones, sized to leave ordinary sky around the rim, then the 5.5-5.9
     * silhouette stack: dark blue over the storm, dark purple laid on top and
     * wrapped round the back, purple, dark moon-blue, and a black cap that
     * erases the top of the atmosphere.
     */
    private static void halo(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cam,
            Vec3 bearing, float phase, float near) {
        Vec3 at = cam.add(bearing.scale(HALO_R));

        float wTeal = ramp(phase, 5.20F, 5.35F) * (1.0F - ramp(phase, 5.38F, 5.50F));
        float wLav = ramp(phase, 5.32F, 5.45F) * (1.0F - ramp(phase, 5.50F, 5.62F));
        float wDeep = ramp(phase, 5.48F, 5.60F) * (1.0F - ramp(phase, 5.92F, 6.10F));
        float wRose = ramp(phase, 5.92F, 6.20F);
        float wsum = wTeal + wLav + wDeep + wRose;
        if (wsum < 0.004F) {
            return;
        }
        // sampled straight off the frames: 5.2 desaturated teal-grey, 5.4
        // lavender/periwinkle grey, 5.5-5.9 deep purple-magenta, 6+ rose mauve
        float hr = (0.451F * wTeal + 0.612F * wLav + 0.596F * wDeep + 0.831F * wRose) / wsum;
        float hg = (0.573F * wTeal + 0.588F * wLav + 0.239F * wDeep + 0.478F * wRose) / wsum;
        float hb = (0.588F * wTeal + 0.769F * wLav + 0.741F * wDeep + 0.635F * wRose) / wsum;

        float a = near * Mth.clamp(wsum, 0.0F, 1.0F);
        int r = (int) (hr * 255.0F);
        int g = (int) (hg * 255.0F);
        int b = (int) (hb * 255.0F);

        // thick multi-depth halo: concentric discs at several depths along the
        // storm ray so the aura has real thickness, still reads from behind,
        // and leaves ordinary sky around the rim (no heads, pure palette)
        for (int s = 0; s < HALO_DEPTH.length; s++) {
            Vec3 slice = cam.add(bearing.scale(HALO_R + HALO_DEPTH[s]));
            float sc = HALO_SCALE[s];
            float aa = HALO_ALPHA[s];
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), slice, bearing, 0.0, 0.0,
                    168.0 * sc, r, g, b, (int) (a * aa * 42.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), slice, bearing, 0.0, 0.0,
                    112.0 * sc, r, g, b, (int) (a * aa * 62.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), slice, bearing, 0.0, 0.0,
                    68.0 * sc, r, g, b, (int) (a * aa * 78.0F));
        }

        // ---- silhouette stack, 5.5 to 5.9 --------------------------------
        float sil = ramp(phase, 5.46F, 5.58F) * (1.0F - ramp(phase, 5.90F, 6.10F)) * near;
        if (sil <= 0.006F) {
            return;
        }
        // 1. the mod's dark-blue light over the storm, replicated
        disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                132.0, 28, 54, 128, (int) (sil * 96.0F));
        // 2. a dark purple layer on top of it, carried round the back
        disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 14.0,
                122.0, 74, 26, 112, (int) (sil * 104.0F));
        disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, -8.0,
                150.0, 62, 20, 96, (int) (sil * 62.0F));
        // 3. the second pair: purple over, then a dark moon-blue beneath it
        disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 26.0,
                96.0, 122, 44, 168, (int) (sil * 92.0F));
        disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 40.0,
                104.0, 34, 46, 104, (int) (sil * 88.0F));
        // 4. the black glow capping the storm - the top of the ATMOSPHERE goes
        //    out completely, which is why this one is alpha-blended, not additive
        disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, 92.0,
                190.0, 3, 2, 7, (int) (sil * 225.0F));
        disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, 148.0,
                240.0, 2, 1, 5, (int) (sil * 185.0F));
    }

    /* ---- primitives ------------------------------------------------------- */

    private static void disc(PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
            Vec3 at, Vec3 view, double dx, double dy, double radius, int r, int g, int b, int alpha) {
        if (alpha <= 2) {
            return;
        }
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            Vec3 upHint = Math.abs(view.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 right = view.cross(upHint).normalize();
            Vec3 up = right.cross(view).normalize();
            Vec3 c = at.add(right.scale(dx)).add(up.scale(dy));
            Vec3 rx = right.scale(radius * 1.12);
            Vec3 uy = up.scale(radius);
            int fa = Mth.clamp(alpha, 0, 255);
            vertex(pose, consumer, c.subtract(rx).subtract(uy), 0.0F, 1.0F, r, g, b, fa);
            vertex(pose, consumer, c.add(rx).subtract(uy), 1.0F, 1.0F, r, g, b, fa);
            vertex(pose, consumer, c.add(rx).add(uy), 1.0F, 0.0F, r, g, b, fa);
            vertex(pose, consumer, c.subtract(rx).add(uy), 0.0F, 0.0F, r, g, b, fa);
        });
    }

    private static float smooth(float x, float lo, float hi) {
        float t = Mth.clamp((x - lo) / Math.max(hi - lo, 1.0E-4F), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float[] lerp3(float[] a, float[] b, float t) {
        return new float[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t,
                a[2] + (b[2] - a[2]) * t };
    }

    private static float[] mix3(float[] a, float[] b, float wa, float wb) {
        return new float[] { a[0] * wa + b[0] * wb, a[1] * wa + b[1] * wb, a[2] * wa + b[2] * wb };
    }

    private static void vertex(Pose pose, VertexConsumer consumer, Vec3 at,
            float u, float v, int r, int g, int b, int a) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
