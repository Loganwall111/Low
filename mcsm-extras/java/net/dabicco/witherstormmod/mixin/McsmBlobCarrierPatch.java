package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.client.StormSkyGradient;
import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.mcsm.extras.McsmDiag;
import net.mcsm.extras.McsmExtrasConfig;
import net.mcsm.extras.McsmFxDriver;
import net.mcsm.extras.client.McsmClientChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * MCSM 1.9.71 -- fixes the missing black glare blob.
 *
 * The blob never rendered because the storm direction could not survive the
 * trip to the shader. The mod's own McsmFogCarrierMixin packs yaw+pitch into
 * FogData.cloudEnd as:
 *
 *     cloudEnd = 1200 + (yaw+180)*2 + (pitch+90)*0.5
 *
 * That is NOT invertible. The pitch term spans [0,90) while the yaw term steps
 * by 2, so 45 distinct (yaw,pitch) pairs collide on the same value -- verified
 * by brute force over the whole angle domain. Our decoder therefore recovered
 * garbage: pitch pinned to -90 (straight down) in almost every case and yaw off
 * by up to 34 degrees, so mcsm_blob() drew the glare below the world.
 *
 * Fix: re-stamp cloudEnd at TAIL (after the mod's HEAD write) with a strictly
 * invertible integer packing:
 *
 *     cloudEnd = 3000 + yawIdx*181 + pitchIdx
 *     yawIdx   = round(yaw)   + 180   in [0,360]
 *     pitchIdx = round(pitch) +  90   in [0,180]
 *
 * pitchIdx < 181 guarantees uniqueness. Max value 68340, well inside float32's
 * exact-integer range (2^24), so nothing is lost in the uniform upload.
 * Verified exhaustively: 65341 angle pairs, zero round-trip mismatches.
 *
 * MCSM 1.9.98 widened that to (3000 + yawIdx*181 + pitchIdx) * 16 + sizeIdx so
 * the user's glare-size setting rides along in the low nibble. The shader reads
 * both encodings, so an old jar-side writer degrades to the default size.
 *
 * ---------------------------------------------------------------------------
 * MCSM 1.9.109 -- THE DEATH CINEMATIC IS NOW WIRED UP.
 *
 * The whole dying sequence (world distortion -> white cracks crawling over the
 * sky -> the mass shaking and shedding layers into a white-hot point with
 * in-rushing motes -> full-screen flash -> six translucent supernova rings in
 * MCSM order purple/pink/blue/orange/green/yellow rolling out across the horizon
 * -> the dust cloud settling -> the sky easing back to normal) has been sitting
 * fully implemented in the shaders since 1.9.98, keyed off the FogSkyEnd band
 * 1906..2906 -- and nothing ever wrote that band, so it never ran once. That is
 * why "the shockwave" was invisible in every build the user tested: it was not
 * a config default, not a missing feature and not a stale jar. It was a
 * dormant shader waiting for a carrier that did not exist yet.
 *
 * This mixin already runs once per frame on the client with the FogData in
 * hand, so it is the natural place to drive it:
 *
 *   1. Latch: the first frame a WitherStormEntity is dead-or-dying starts a
 *      fixed-length sequence. It is LATCHED, not polled -- the storm is usually
 *      removed part-way through dying, and the cinematic must run to its end
 *      rather than cut off when the entity disappears.
 *   2. Stamp: skyEnd = 1906 + dt*994, so the shader's mcsm_death() decodes
 *      dt = 0.06 .. 1.00 across the sequence.
 *   3. Aim: the rings and the implosion need the storm direction, which normally
 *      comes from StormSkyGradient. Once the storm is gone the gradient drops
 *      out, so the last known yaw/pitch are cached and re-stamped for the rest
 *      of the sequence -- otherwise the finale would render unaimed.
 *
 * The skyEnd FIELD NAME is resolved reflectively (name match, then "any float
 * field containing sky", then "the float field currently holding the mod's own
 * phase stamp in 1300..1900") and cached, because FogData's field names are not
 * in the CI api dump and a wrong guess must never break a frame. If it cannot
 * be resolved the cinematic stays dormant exactly as before and says so once in
 * the log -- it can never crash the game.
 */
@Mixin(value = FogRenderer.class, priority = 1500)
public abstract class McsmBlobCarrierPatch {

    /** Length of the latched death sequence, in seconds. */
    private static final double DEATH_SECONDS = 16.0;

    /** Dying-storm scan cadence: every 5th frame keeps the cost off the hot path. */
    private static final int SCAN_EVERY = 5;

    private static long   mcsm$deathStartNs = 0L;
    private static int    mcsm$frame        = 0;
    private static boolean mcsm$dyingCache  = false;
    private static float  mcsm$lastYaw      = 0.0F;
    private static float  mcsm$lastPitch    = 0.0F;
    private static int    mcsm$lastSizeIdx  = 4;

    private static Field  mcsm$skyEndField   = null;
    private static boolean mcsm$skyEndFailed = false;

    // FogRenderer.updateBuffer is OVERLOADED -- there is also a private
    // updateBuffer(ByteBuffer,int,Vector4f,F,F,F,F,F,F). A bare "updateBuffer"
    // is ambiguous, so the full descriptor is mandatory here. The mod's own
    // McsmFogCarrierMixin targets it the same way.
    // require = 1: if this ever stops matching we want a hard startup failure,
    // not a silently missing glare blob.
    @Inject(
        method = "updateBuffer(Lnet/minecraft/client/renderer/fog/FogData;)V",
        at = @At("TAIL"),
        require = 1
    )
    private void mcsm$stampBlobCarrier(FogData data, CallbackInfo ci) {
        McsmExtrasConfig.load();

        boolean gradient = StormSkyGradient.fogStampActive();

        // glare-size nibble, retained for the optional death cinematic carrier
        int sizeIdx = mcsm$sizeIdx(McsmExtrasConfig.glareSize);
        mcsm$lastSizeIdx = sizeIdx;

        // The normal phase-5+ blob is now owned by McsmStormBlob's native
        // render-only radial material.  Do not write the old FogData carrier:
        // the built-in shader's procedural sky blob is a second, expensive
        // full-screen projection and was responsible for the giant faceted
        // black/blue wall seen when the player approached the storm.  The
        // carrier remains reserved for the death cinematic below.

        mcsm$driveDeathCinematic(data, gradient, sizeIdx);

        // Normal storm blob rendering stays native and render-only.  No
        // shader carrier is stamped here, so this pass cannot add a second
        // full-screen sky layer or its per-fragment noise cost.
    }

    /** Latch, advance and stamp the dying sequence. Never throws. */
    private void mcsm$driveDeathCinematic(FogData data, boolean gradient, int sizeIdx) {
        try {
            if (!McsmExtrasConfig.deathCinematic && !McsmExtrasConfig.supernovaRings) {
                if (mcsm$deathStartNs != 0L) {
                    mcsm$deathStartNs = 0L;
                    McsmDiag.death("off (disabled in the MCSM control panel)");
                }
                return;
            }

            boolean dying = mcsm$stormIsDying();
            long now = System.nanoTime();

            if (dying && mcsm$deathStartNs == 0L) {
                mcsm$deathStartNs = now;
                McsmDiag.death("START -- storm is dying; stamping the 1906..2906 sky band for "
                               + (int) DEATH_SECONDS + "s");
                McsmClientChat.say("[ds] death cinematic START: sky band 1906..2900 for "
                                   + (int) DEATH_SECONDS + "s");
            }
            if (mcsm$deathStartNs == 0L) {
                return;
            }

            double t = (now - mcsm$deathStartNs) / 1.0e9 / DEATH_SECONDS;
            if (t >= 1.0) {
                mcsm$deathStartNs = 0L;
                mcsm$dyingCache = false;
                McsmDiag.death("END -- sky band released, normal fog resumes");
                McsmClientChat.say("[ds] death cinematic END: sky band released");
                return;
            }
            if (t < 0.0) {
                t = 0.0;
            }

            // 1906..2900 -> the shader decodes dt = (skyEnd - 1900) * 0.01
            mcsm$stampSkyEnd(data, 1906.0F + (float) (t * 994.0));

            // Keep the aim alive after the gradient drops out with the entity,
            // so the implosion and the rings stay centred on where it died.
            if (!gradient) {
                data.cloudEnd = mcsm$pack(mcsm$lastYaw, mcsm$lastPitch, sizeIdx);
            }

            McsmDiag.deathProgress(t);
        } catch (Throwable ignored) {
            // A missing field or a vanished level must never break a frame.
        }
    }

    /** Is any storm within reach dead-or-dying? Scanned every SCAN_EVERY frames. */
    private boolean mcsm$stormIsDying() {
        // While a sequence is latched we keep the cached answer: the entity is
        // usually removed part-way through, and the latch owns the timeline.
        if (mcsm$deathStartNs != 0L) {
            return mcsm$dyingCache;
        }
        if ((mcsm$frame++ % SCAN_EVERY) != 0) {
            return mcsm$dyingCache;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer pl = mc.player;
            if (pl == null || mc.level == null) {
                mcsm$dyingCache = false;
                return false;
            }
            // MCSM 1.9.110 -- the single-player bridge to the server half.
            // /kill removes the storm without the client ever observing
            // isDeadOrDying(), so the scan below would find nothing and the
            // sky band would never start. The server stamps this clock when it
            // arms the death blast; a fresh stamp IS the death, observed.
            if (System.currentTimeMillis() - McsmFxDriver.lastDeathArmMs() < 1500L) {
                mcsm$dyingCache = true;
                return true;
            }
            // The storm is tracked out to ~1400 blocks by the gradient; 4096
            // leaves room for a dying storm that has already stopped updating.
            AABB box = new AABB(pl.getX() - 4096.0, -512.0, pl.getZ() - 4096.0,
                                pl.getX() + 4096.0, 2048.0, pl.getZ() + 4096.0);
            boolean found = false;
            for (Entity e : mc.level.getEntities(pl, box,
                    en -> en instanceof WitherStormEntity)) {
                // getEntities() is typed to Entity, and isDeadOrDying() lives on
                // LivingEntity -- hence the pattern match rather than a direct
                // call (javac caught this on the runner: cannot find symbol).
                if (e instanceof WitherStormEntity ws && ws.isDeadOrDying()) {
                    found = true;
                    break;
                }
            }
            mcsm$dyingCache = found;
        } catch (Throwable t) {
            mcsm$dyingCache = false;
        }
        return mcsm$dyingCache;
    }

    /** cloudEnd payload: invertible yaw/pitch plus the glare-size nibble. */
    private static float mcsm$pack(float yaw, float pitch, int sizeIdx) {
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
        // Max integer is 68340*16+15 = 1093455 < 2^24, still exact in float32.
        return (3000.0F + yawIdx * 181.0F + pitchIdx) * 16.0F + sizeIdx;
    }

    /** glare size -> nibble 0..15 covering x0.35..x3.05 (shader-side table). */
    private static int mcsm$sizeIdx(double size) {
        int idx = (int) Math.round((size - 0.35) / 0.18);
        if (idx < 0) {
            idx = 0;
        }
        if (idx > 15) {
            idx = 15;
        }
        return idx;
    }

    private static void mcsm$stampSkyEnd(FogData data, float value) {
        Field f = mcsm$skyEndField;
        if (f == null) {
            if (mcsm$skyEndFailed) {
                return;
            }
            f = mcsm$resolveSkyEnd(data);
            if (f == null) {
                mcsm$skyEndFailed = true;
                McsmDiag.death("no sky-end field found on FogData -- death cinematic stays dormant");
                return;
            }
            try {
                f.setAccessible(true);
            } catch (Throwable ignored) {
                mcsm$skyEndFailed = true;
                return;
            }
            mcsm$skyEndField = f;
            McsmDiag.death("FogData." + f.getName() + " carries the death band");
        }
        try {
            f.setFloat(data, value);
        } catch (Throwable ignored) {
            // leave the frame alone
        }
    }

    /**
     * Find the FogData field that carries FogSkyEnd. Three attempts, cheapest
     * and most certain first; the result is cached for the rest of the session.
     */
    private static Field mcsm$resolveSkyEnd(FogData data) {
        try {
            return FogData.class.getDeclaredField("skyEnd");
        } catch (Throwable ignored) {
            // fall through to the heuristics
        }
        for (Field f : FogData.class.getDeclaredFields()) {
            if (f.getType() == float.class && f.getName().toLowerCase().contains("sky")) {
                return f;
            }
        }
        // Last resort: the field currently holding the mod's own phase stamp
        // (skyEnd = 1000 + phase*100, phase 4.42..8.06 -> 1442..1806).
        for (Field f : FogData.class.getDeclaredFields()) {
            if (f.getType() != float.class) {
                continue;
            }
            try {
                f.setAccessible(true);
                float v = f.getFloat(data);
                if (v >= 1300.0F && v <= 1900.0F) {
                    return f;
                }
            } catch (Throwable ignored) {
                // keep looking
            }
        }
        return null;
    }
}
