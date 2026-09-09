package net.mcsm.extras;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Devouring Storms built-in resource packs.
 *
 * Story Look stays default-enabled for the no-shader MCSM palette. The OGS CEM
 * pack is also registered as the default model/preset again per the user's
 * 1.9.198 correction: use Loganwall111/ogs-stuff/witherstormmod as the default
 * Wither Storm look instead of endlessly recolouring approximations. Shaderpacks
 * remain available but are not forced on because the user's machine hit
 * native/OpenGL/pagefile exhaustion when heavy shaders were active.
 */
public final class McsmBuiltinPack {

    private McsmBuiltinPack() {
    }

    private static boolean attempted = false;

    public static void register() {
        if (attempted) {
            return;
        }
        attempted = true;
        McsmShaderPackInstall.install();
        registerBuiltIn("storylook", "Story Look");
        registerBuiltIn("ogs-cem", "OGS CEM preset/model pack");
    }

    private static void registerBuiltIn(String packId, String label) {
        try {
            Class<?> loaderCls = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderCls.getMethod("getInstance").invoke(null);
            Object opt = loaderCls.getMethod("getModContainer", String.class)
                    .invoke(loader, "dabywitherstormmod");
            if (!(opt instanceof Optional<?>) || ((Optional<?>) opt).isEmpty()) {
                warn(label, "mod container not found");
                return;
            }
            Object modContainer = ((Optional<?>) opt).get();

            Class<?> rlCls = null;
            for (String n : new String[] {
                    "net.minecraft.resources.Identifier",
                    "net.minecraft.resources.ResourceLocation" }) {
                try {
                    rlCls = Class.forName(n);
                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (rlCls == null) {
                warn(label, "no Identifier/ResourceLocation class on this minecraft version");
                return;
            }

            Object id = null;
            for (Method m : rlCls.getMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == rlCls
                        && ps.length == 2 && ps[0] == String.class && ps[1] == String.class) {
                    id = m.invoke(null, "dabywitherstormmod", packId);
                    break;
                }
            }
            if (id == null) {
                warn(label, "no ResourceLocation(String,String) factory on this minecraft version");
                return;
            }

            Class<?> rmhCls = Class.forName("net.fabricmc.fabric.api.resource.ResourceManagerHelper");
            Class<?> predCls = Class.forName("net.fabricmc.fabric.api.resource.ResourcePackActivationPredicate");
            Object predicate = null;
            try {
                Field f = predCls.getField("DEFAULT_ENABLED");
                predicate = f.get(null);
            } catch (NoSuchFieldException ignored) {
            }
            if (predicate == null) {
                for (Object c : predCls.getEnumConstants()) {
                    if ("DEFAULT_ENABLED".equals(String.valueOf(c))) {
                        predicate = c;
                        break;
                    }
                }
            }
            if (predicate == null) {
                warn(label, "no DEFAULT_ENABLED activation predicate in this fabric-api");
                return;
            }

            Method target = null;
            Object packType = null;
            for (Method m : rmhCls.getMethods()) {
                if (!"registerBuiltinResourcePack".equals(m.getName())) {
                    continue;
                }
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 3 && ps[0] == rlCls) {
                    target = m;
                    packType = null;
                    break;
                }
                if (ps.length == 4 && ps[1] == rlCls && target == null) {
                    for (Object c : ps[0].getEnumConstants()) {
                        String name = String.valueOf(c);
                        if (name.contains("CLIENT") || name.contains("RESOURCE")) {
                            packType = c;
                            break;
                        }
                    }
                    if (packType != null) {
                        target = m;
                    }
                }
            }
            if (target == null) {
                warn(label, "no registerBuiltinResourcePack overload recognized");
                return;
            }
            if (target.getParameterCount() == 3) {
                target.invoke(null, id, modContainer, predicate);
            } else {
                target.invoke(null, packType, id, modContainer, predicate);
            }
            System.out.println("[ds] " + label + " built-in resource pack registered (default enabled): " + packId);
        } catch (Throwable t) {
            warn(label, "unavailable: " + t);
        }
    }

    private static void warn(String label, String msg) {
        System.err.println("[ds] " + label + " built-in pack " + msg
                + " - install the matching release zip manually if the world looks vanilla");
    }
}
