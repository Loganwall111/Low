package net.mcsm.extras.client;

import net.mcsm.extras.McsmFxDriver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * MCSM -- the client half of the expanding blasts.
 *
 * McsmFxDriver arms a blast from the storm's own tick (the die() and
 * addSubGrowth() hooks), and that tick stops being called the moment the storm
 * is removed. A death sequence driven only from there freezes about a second
 * after it starts, which is the "one-tick puff" that kept being reported no
 * matter how the geometry was tuned. This class is driven instead from
 * LevelRenderer.render -- once per rendered frame, for as long as the player is
 * in a world -- and spawns its particles locally, so the front keeps travelling
 * for the full five seconds whether or not the entity still exists.
 *
 * It lives in the client package and is reachable only from a client mixin,
 * because McsmFxDriver is loaded on a dedicated server as well and must never
 * have to resolve Minecraft or ClientLevel. In single-player the client and the
 * integrated server share one JVM, so the blasts the server armed are the ones
 * this draws; stepping here also stamps McsmFxDriver's client clock, which is
 * how the server half knows to stand down and no particle is spawned twice.
 *
 * Wrapped end to end: a visual can never break a frame.
 */
public final class McsmClientBlasts {

    private McsmClientBlasts() {}

    /** Advances any armed blast and draws this tick's frame. Call once a frame. */
    public static void tick() {
        try {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc == null ? null : mc.level;
            if (level == null) {
                return;
            }
            McsmFxDriver.Sink sink = (options, x, y, z, count, dx, dy, dz, speed) -> {
                int n = Math.max(1, count);
                for (int i = 0; i < n; i++) {
                    // Mirrors the server's own count>0 branch: the position
                    // jitters inside the offset triple and the velocity is that
                    // jitter scaled by speed, so a locally spawned particle and
                    // a forced server one look identical.
                    double jx = (Math.random() * 2.0D - 1.0D) * dx;
                    double jy = (Math.random() * 2.0D - 1.0D) * dy;
                    double jz = (Math.random() * 2.0D - 1.0D) * dz;
                    level.addParticle(options, x + jx, y + jy, z + jz,
                                      jx * speed, jy * speed, jz * speed);
                }
            };
            McsmFxDriver.stepBlasts(sink, level.getGameTime());
        } catch (Throwable ignored) {
            // a visual must never break a frame
        }
    }
}
