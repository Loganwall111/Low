package net.mcsm.extras.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1.9.168: GLARE WIPE.
 *
 * Previous builds drew nested translucent spheres on the storm (LON/LAT mesh).
 * In-game that read as a floating ball with grid lines + centre dots — the
 * exact artifact the user circled and rejected. All glare is gone until we
 * rebuild a thick MCSM volume from the stills. This class only keeps
 * swayOffset so body detail (teeth/beams) can stay glued if re-enabled.
 */
public final class McsmPhaseSky {

    private McsmPhaseSky() {
    }

    public static Vec3 swayOffset(float phase, float timeSec, double bodyR) {
        if (phase < 4.0F || bodyR <= 0.0) {
            return Vec3.ZERO;
        }
        float amp = (float) (bodyR * (0.04 + 0.03 * Mth.clamp((phase - 4.0F) / 3.0F, 0.0F, 1.0F)));
        float x = Mth.sin(timeSec * 0.22F) * amp;
        float z = Mth.sin(timeSec * 0.17F + 1.3F) * amp * 0.55F;
        float y = Mth.sin(timeSec * 0.13F) * amp * 0.18F;
        return new Vec3(x, y, z);
    }

    /** No-op: glare spheres wiped (user 1.9.168). */
    public static void submit(LevelRenderContext ctx) {
        // intentionally empty
    }
}
