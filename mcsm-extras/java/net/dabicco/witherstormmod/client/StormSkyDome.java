package net.dabicco.witherstormmod.client;

import net.minecraft.world.phys.Vec3;

/**
 * Compatibility shell for older base callers. The former world-space dome
 * implementation is intentionally inert; McsmAtmosphericMeshComponent owns the
 * visible phase backdrop from the Wither Storm renderer now.
 */
public final class StormSkyDome {
    private StormSkyDome() {
    }

    public static void update(Vec3 cameraPosition) {
        // The entity-attached atmospheric component is the sole backdrop path.
    }

    public static float strength() {
        return 0.0F;
    }

    public static float coreStrength() {
        return 0.0F;
    }

    public static float phase() {
        return 0.0F;
    }

    public static void skyColor(float[] out) {
        if (out != null && out.length >= 3) {
            out[0] = 0.0F;
            out[1] = 0.0F;
            out[2] = 0.0F;
        }
    }
}
