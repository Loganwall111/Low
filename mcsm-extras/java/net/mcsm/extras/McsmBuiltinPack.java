package net.mcsm.extras;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Devouring Storms: built-in resource packs ship INSIDE the mod jar under
 * resourcepacks/&lt;name&gt;/ and turn themselves on via Fabric's resource-loader
 * registerBuiltinResourcePack with DEFAULT_ENABLED.
 *
 * Packs registered here:
 *   storylook — MCSM pastel skies, cloud decks, soft lavender shadows
 *   ogs-cem   — Totally Accurate / MCSM OG CEM models + textures (1.9.151+)
 *
 * Everything is invoked reflectively: fabric-api for MC 26.2 changed the
 * overload (older: (ResourceLocation, ModContainer, predicate); newer: leading
 * ResourcePackType). Failure degrades to a log line, never a crash.
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
        try {
            Class<?> loaderCls = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderCls.getMethod("getInstance").invoke(null);
            Object opt = loaderCls.getMethod("getModContainer", String.class)
                    .invoke(loader, "dabywitherstormmod");
            if (!(opt instanceof Optional<?>) || ((Optional<?>) opt).isEmpty()) {
                warn("mod container not found");
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
                warn("no Identifier/ResourceLocation class on this minecraft version");
                return;
            }

            Method idFactory = null;
            for (Method m : rlCls.getMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == rlCls
                        && ps.length == 2 && ps[0] == String.class && ps[1] == String.class) {
                    idFactory = m;
                    break;
                }
            }
            if (idFactory == null) {
                warn("no ResourceLocation(String,String) factory on this minecraft version");
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
            if (predicate == null && predCls.getEnumConstants() != null) {
                for (Object c : predCls.getEnumConstants()) {
                    if ("DEFAULT_ENABLED".equals(String.valueOf(c))) {
                        predicate = c;
                        break;
                    }
                }
            }
            if (predicate == null) {
                warn("no DEFAULT_ENABLED activation predicate in this fabric-api");
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
                warn("no registerBuiltinResourcePack overload recognized");
                return;
            }

            // 1.9.151: register BOTH built-ins (storylook + Totally Accurate CEM)
            for (String packId : new String[] { "storylook", "ogs-cem" }) {
                try {
                    Object id = idFactory.invoke(null, "dabywitherstormmod", packId);
                    if (target.getParameterCount() == 3) {
                        target.invoke(null, id, modContainer, predicate);
                    } else {
                        target.invoke(null, packType, id, modContainer, predicate);
                    }
                    System.out.println("[ds] built-in resource pack registered (default ON): " + packId);
                } catch (Throwable t) {
                    warn("failed to register " + packId + ": " + t);
                }
            }
        } catch (Throwable t) {
            warn("unavailable: " + t);
        }
    }

    private static void warn(String msg) {
        System.err.println("[ds] built-in pack " + msg
                + " - install the storylook / ogs-cem zip manually if needed");
    }
}
