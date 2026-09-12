package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.mcsm.extras.McsmDiag;
import net.mcsm.extras.McsmExtrasConfig;

/**
 * MCSM 1.9.201 -- ATMOSPHERIC W'S CLOUD  (Java half of u_StormPos).
 *
 * Telltale's glare in Minecraft: Story Mode is not a 3D volume, not a
 * billboard and not a cloud layer: it is an INFINITE skybox projection
 * tethered to the Wither Storm (the same trick as the infinite-hallway
 * portals). The GLSL half lives in mcsm_infinite_blob()/mcsm_blob() inside
 * the sky pass (gbuffers_sky / sky.fsh); this class is the Java driver that
 * feeds it once per frame:
 *
 *   * tracks the nearest storm and hands the shader its world position --
 *     that vector IS the shader's u_StormPos (witherstorm_BossPos), shipped
 *     through the invertible yaw/pitch carrier in FogData.cloudEnd (the same
 *     3000..1093455 band the shader already decodes, plus the glare-size
 *     nibble), so the blob is pinned to the storm and travels with it;
 *   * 25 % per frame temporal smoothing so blob and storm glide as one sky
 *     element instead of snapping;
 *   * DISTANCE FADE: full presence inside 700 blocks, then the blob slowly
 *     shrinks and finally stops being stamped by 1600 blocks -- "go
 *     extremely far away and the sky slowly changes back to regular
 *     vanilla". Inside the fade zone the size nibble is scaled by presence,
 *     so the receding blob reads as the storm shrinking over the horizon
 *     rather than popping out.
 *
 * McsmBlobCarrierPatch calls {@link #stamp(FogData)} at the TAIL of
 * FogRenderer.updateBuffer every frame (never while the death cinematic
 * runs, which owns the band for its duration), so this is the last word for
 * the blob carrier.
 */
public final class McsmInfiniteSkyboxBlob {

    /** Full blob presence inside this range. */
    private static final double FULL_RANGE = 700.0;

    /** The blob is fully gone beyond this range; the vanilla sky resumes. */
    private static final double MAX_RANGE = 1600.0;

    /** Temporal smoothing (25 % per frame), matching the old StormBlob glide. */
    private static double smoothYaw = Double.NaN;
    private static double smoothPitch = Double.NaN;

    private static boolean reported = false;

    private McsmInfiniteSkyboxBlob() {
    }

    /**
     * World position of the storm that owns the infinite blob, or null when
     * no phase-5+ storm is within range. This is the u_StormPos analogue the
     * shader receives through the carrier.
     */
    public static Vec3 stormWorldPos() {
        ClientDistantStormManager.StormData storm = nearestStorm();
        if (storm == null) {
            return null;
        }
        return new Vec3(storm.dispX, storm.dispY, storm.dispZ);
    }

    /** Nearest phase-5+ storm within MAX_RANGE, or null. */
    private static ClientDistantStormManager.StormData nearestStorm() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                return null;
            }
            Vec3 pos = mc.player.position();
            ClientDistantStormManager.StormData best = null;
            double bestD = Double.MAX_VALUE;
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                if (d.phase < 5.0F) {
                    continue;
                }
                double dx = d.dispX - pos.x;
                double dy = d.dispY - pos.y;
                double dz = d.dispZ - pos.z;
                double dd = dx * dx + dy * dy + dz * dz;
                if (dd < bestD) {
                    bestD = dd;
                    best = d;
                }
            }
            if (best == null || bestD > MAX_RANGE * MAX_RANGE) {
                return null;
            }
            return best;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 1 inside FULL_RANGE, smoothly 0 at MAX_RANGE (smoothstep). */
    private static float distancePresence(double dist) {
        float t = (float) Mth.clamp((dist - FULL_RANGE) / (MAX_RANGE - FULL_RANGE), 0.0, 1.0);
        return 1.0F - t * t * (3.0F - 2.0F * t);
    }

    /**
     * Stamp the blob aim carrier. No storm in range -> no write, and the
     * sky pass keeps its vanilla behaviour. Never throws.
     */
    public static void stamp(FogData data) {
        try {
            ClientDistantStormManager.StormData storm = nearestStorm();
            if (storm == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            Vec3 cam = mc.player.position();
            double dx = storm.dispX - cam.x;
            double dy = storm.dispY - cam.y;
            double dz = storm.dispZ - cam.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float presence = distancePresence(dist);
            if (presence <= 0.005F) {
                return; // effectively vanilla: no blob carrier this frame
            }

            // Carrier yaw/pitch. The shader decodes the packed angle pair and
            // then NEGATES it (antipode fix, MCSM 1.9.96), so what must be
            // packed here is already camera->storm:
            //   packed y = atan2(-dz, -dx), p = atan2(-dy, horiz)
            double yaw = Math.toDegrees(Math.atan2(-dz, -dx));
            double horiz = Math.sqrt(dx * dx + dz * dz);
            double pitch = Math.toDegrees(Math.atan2(-dy, horiz));

            // 25 % per frame smoothing so the blob glides with the storm.
            if (Double.isNaN(smoothYaw)) {
                smoothYaw = yaw;
                smoothPitch = pitch;
            } else {
                smoothYaw += (yaw - smoothYaw) * 0.25;
                smoothPitch += (pitch - smoothPitch) * 0.25;
            }

            McsmExtrasConfig.load();
            int idx = sizeIdx(McsmExtrasConfig.glareSize);
            // Distance fade: the size nibble shrinks with presence, so the
            // blob recedes as the player leaves instead of popping out.
            int faded = Math.max(1, Math.round(idx * presence));
            data.cloudEnd = pack((float) smoothYaw, (float) smoothPitch, faded);

            if (!reported) {
                reported = true;
                McsmDiag.say("Infinite Skybox Blob: aim carrier live (storm at "
                        + (int) storm.dispX + ", " + (int) storm.dispY + ", " + (int) storm.dispZ
                        + "; phase " + storm.phase + "; sizeIdx " + faded + ")");
            }
        } catch (Throwable ignored) {
            // a missing surface must never break a frame
        }
    }

    /**
     * cloudEnd payload, byte-identical to McsmBlobCarrierPatch.mcsm$pack:
     * (3000 + yawIdx*181 + pitchIdx) * 16 + sizeIdx. Max 1093455 < 2^24, so
     * the value is exact in float32 and the shader decode stays lossless.
     */
    private static float pack(float yaw, float pitch, int sizeIdx) {
        int yawIdx = Math.round(yaw) + 180;
        int pitchIdx = Math.round(pitch) + 90;
        if (yawIdx < 0) {
            yawIdx = 0;
        }
        if (yawIdx > 360) {
            yawIdx = 360;
        }
        if (pitchIdx < 0) {
            pitchIdx = 0;
        }
        if (pitchIdx > 180) {
            pitchIdx = 180;
        }
        return (3000.0F + yawIdx * 181.0F + pitchIdx) * 16.0F + sizeIdx;
    }

    /** glare size -> nibble 0..15 covering x0.35..x3.05 (shader-side table). */
    private static int sizeIdx(double size) {
        int idx = (int) Math.round((size - 0.35) / 0.18);
        if (idx < 0) {
            idx = 0;
        }
        if (idx > 15) {
            idx = 15;
        }
        return idx;
    }
}
