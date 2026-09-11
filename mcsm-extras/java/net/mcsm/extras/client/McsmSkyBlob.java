package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.mcsm.extras.McsmExtrasConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * 1.9.218 -- the INFINITE SKYBOX BLOB driver.
 *
 * The glare is an angular sky projection inside the MCSM Visual Shader
 * (lib/mcsm/skyBlob.glsl, painted in the lighting composite on sky pixels).
 * This class is the Java side: every frame it finds the strongest storm and
 * pushes its world position, phase and the player's Glare Size into the
 * shader via the Iris uniform API -- reflectively, so nothing breaks when
 * Iris/Oculus is absent and the shader simply runs its calm default.
 */
public final class McsmSkyBlob {

    private McsmSkyBlob() {
    }

    private static Object irisUniforms;
    private static boolean tried;

    public static void push() {
        try {
            if (!tried) {
                tried = true;
                Object api = Class.forName("net.irisshaders.iris.api.v0.IrisApi")
                        .getMethod("getInstance").invoke(null);
                if (api != null) {
                    try {
                        irisUniforms = api.getClass().getMethod("getIrisUniforms").invoke(api);
                    } catch (Throwable ignoredApiShape) {
                    }
                }
            }
            if (irisUniforms == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) {
                return;
            }
            float phase = 0.0F;
            double sx = 0.0D, sy = -100000.0D, sz = 0.0D;
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                if (d.phase > phase) {
                    phase = d.phase;
                    sx = d.dispX;
                    sy = d.dispY;
                    sz = d.dispZ;
                }
            }
            McsmExtrasConfig.load();
            float glare = (float) Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
            vec3(irisUniforms, "uStormPos", sx, sy, sz);
            float_(irisUniforms, "uStormPhase", phase);
            float_(irisUniforms, "uGlareSize", glare);
        } catch (Throwable ignored) {
            // the shader keeps its calm default; the blob fades in with phase
        }
    }

    /** try flat setter shapes first, then the uniforms.uniform(name).set() holder. */
    private static void vec3(Object holder, String name, double x, double y, double z) {
        for (String m : new String[]{"setVector3d", "setVec3d", "set"}) {
            try {
                holder.getClass().getMethod(m, String.class, double.class, double.class, double.class)
                        .invoke(holder, name, x, y, z);
                return;
            } catch (Throwable ignoredFlat) {
            }
        }
        try {
            Object u = holder.getClass().getMethod("uniform", String.class).invoke(holder, name);
            if (u != null) {
                for (String m : new String[]{"set", "setVector3", "setVec3", "setValue"}) {
                    try {
                        u.getClass().getMethod(m, double.class, double.class, double.class)
                                .invoke(u, x, y, z);
                        return;
                    } catch (Throwable ignoredSet) {
                    }
                }
            }
        } catch (Throwable ignoredHolder) {
        }
    }

    private static void float_(Object holder, String name, float v) {
        for (String m : new String[]{"setFloat", "set"}) {
            try {
                holder.getClass().getMethod(m, String.class, float.class).invoke(holder, name, v);
                return;
            } catch (Throwable ignoredFlat) {
            }
        }
        try {
            Object u = holder.getClass().getMethod("uniform", String.class).invoke(holder, name);
            if (u != null) {
                for (String m : new String[]{"set", "setValue"}) {
                    try {
                        u.getClass().getMethod(m, float.class).invoke(u, v);
                        return;
                    } catch (Throwable ignoredSet) {
                    }
                }
            }
        } catch (Throwable ignoredHolder) {
        }
    }
}
