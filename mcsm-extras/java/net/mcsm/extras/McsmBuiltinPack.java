package net.mcsm.extras;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Devouring Storms: built-in visual resource packs ship INSIDE the mod jar and
 * are registered DEFAULT_ENABLED through Fabric resource-loader.
 *
 * Registered packs:
 *   - resourcepacks/storylook : Story Mode sky/light/cloud core-shader look
 *   - resourcepacks/ogs-cem   : restored OGS Wither Storm CEM/model assets
 *
 * Everything is invoked reflectively on purpose: the fabric-api generation
 * shipped for MC 26.2 changed the overload (older builds take
 * (ResourceLocation, ModContainer, predicate); newer ones take a leading
 * ResourcePackType). Reflection binds whichever signature actually exists at
 * runtime, and any failure degrades to a log line instead of a crash.
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
        // mega-phase 5b: the Iris shader pack that ships inside this jar
        // installs itself here, before Iris reads its config on the client.
        McsmShaderPackInstall.install();
        installResourcePack("/assets/dabywitherstormmod/resourcepacks/storylook.zip",
                "DevouringStorms-StoryLook.zip");
        installResourcePack("/assets/dabywitherstormmod/resourcepacks/ogs-cem.zip",
                "DevouringStorms-OGS-CEM.zip");
        deselectManagedVisualPacks();
        // Do not force-edit options.txt anymore: if a core-shader pack fails,
        // forcing it selected makes the whole resource reload fail. The fixed
        // zips are installed to resourcepacks/ and registered built-in; the
        // player can select Story Look/OGS manually while we keep startup safe.
        registerPack("storylook", "Story Look");
        registerPack("ogs-cem", "OGS CEM models");
    }

    private static void installResourcePack(String resource, String fileName) {
        try {
            File gameDir = gameDir();
            if (gameDir == null) {
                return;
            }
            File dir = new File(gameDir, "resourcepacks");
            File target = new File(dir, fileName);
            File marker = new File(dir, fileName + ".version");
            String have = read(marker);
            if (target.isFile() && McsmExtrasConfig.BUILD_VERSION.equals(have)) {
                return;
            }
            InputStream in = McsmBuiltinPack.class.getResourceAsStream(resource);
            if (in == null) {
                return;
            }
            try {
                dir.mkdirs();
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                in.close();
            }
            write(marker, McsmExtrasConfig.BUILD_VERSION);
            System.out.println("[ds] installed built-in resource pack: " + fileName);
        } catch (Throwable t) {
            warn(fileName, "resource-pack extraction failed: " + t);
        }
    }

    private static void deselectManagedVisualPacks() {
        try {
            File gameDir = gameDir();
            if (gameDir == null) {
                return;
            }
            File options = new File(gameDir, "options.txt");
            if (!options.isFile()) {
                return;
            }
            List<String> lines = Files.readAllLines(options.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.startsWith("resourcePacks:")) {
                    continue;
                }
                String v = line.substring("resourcePacks:".length());
                String nv = v
                        .replace(",\"file/DevouringStorms-StoryLook.zip\"", "")
                        .replace("\"file/DevouringStorms-StoryLook.zip\",", "")
                        .replace(",\"file/DevouringStorms-OGS-CEM.zip\"", "")
                        .replace("\"file/DevouringStorms-OGS-CEM.zip\",", "");
                if (!nv.equals(v)) {
                    lines.set(i, "resourcePacks:" + nv);
                    changed = true;
                }
            }
            if (changed) {
                Files.write(options.toPath(), lines, java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("[ds] deselected managed visual packs after previous failed reload; enable them manually to test");
            }
        } catch (Throwable t) {
            warn("resource pack selection", "options.txt cleanup failed: " + t);
        }
    }

    private static void selectResourcePacks() {
        try {
            File gameDir = gameDir();
            if (gameDir == null) {
                return;
            }
            File options = new File(gameDir, "options.txt");
            List<String> lines = options.isFile()
                    ? Files.readAllLines(options.toPath(), java.nio.charset.StandardCharsets.UTF_8)
                    : new ArrayList<>();
            String a = "file/DevouringStorms-StoryLook.zip";
            String b = "file/DevouringStorms-OGS-CEM.zip";
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.startsWith("resourcePacks:")) {
                    continue;
                }
                found = true;
                String v = line.substring("resourcePacks:".length());
                if (!v.contains(a)) {
                    v = appendPack(v, a);
                }
                if (!v.contains(b)) {
                    v = appendPack(v, b);
                }
                lines.set(i, "resourcePacks:" + v);
            }
            if (!found) {
                lines.add("resourcePacks:[\"vanilla\",\"" + a + "\",\"" + b + "\"]");
            }
            Files.write(options.toPath(), lines, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("[ds] selected Story Look + OGS resource packs in options.txt");
        } catch (Throwable t) {
            warn("resource pack selection", "options.txt update failed: " + t);
        }
    }

    private static String appendPack(String existing, String pack) {
        String e = existing == null ? "" : existing.trim();
        String q = "\"" + pack + "\"";
        if (e.startsWith("[") && e.endsWith("]")) {
            if (e.length() <= 2) {
                return "[\"vanilla\"," + q + "]";
            }
            return e.substring(0, e.length() - 1) + "," + q + "]";
        }
        return "[\"vanilla\"," + q + "]";
    }

    private static File gameDir() {
        try {
            Class<?> loaderCls = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderCls.getMethod("getInstance").invoke(null);
            Object path = loaderCls.getMethod("getGameDir").invoke(loader);
            return new File(path.toString());
        } catch (Throwable t) {
            return null;
        }
    }

    private static String read(File f) {
        try {
            if (!f.isFile()) {
                return "";
            }
            return new String(Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Throwable t) {
            return "";
        }
    }

    private static void write(File f, String s) {
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Throwable t) {
            // ignore
        }
    }

    private static void registerPack(String packPath, String label) {
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

            // 26.2 renamed ResourceLocation -> Identifier; support both
            Class<?> rlCls = null;
            for (String n : new String[] {
                    "net.minecraft.resources.Identifier",
                    "net.minecraft.resources.ResourceLocation" }) {
                try {
                    rlCls = Class.forName(n);
                    break;
                } catch (ClassNotFoundException ignored) {
                    // try the next name
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
                    id = m.invoke(null, "dabywitherstormmod", packPath);
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
                // fall through to the enum scan below
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
                    break; // legacy signature wins outright
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
            System.out.println("[ds] " + label + " built-in resource pack registered (default enabled): " + packPath);
        } catch (Throwable t) {
            warn(label, "unavailable: " + t);
        }
    }

    private static void warn(String label, String msg) {
        System.err.println("[ds] " + label + " built-in pack " + msg
                + " - install the release resource pack manually if the world looks vanilla");
    }
}
