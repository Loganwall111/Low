package net.mcsm.extras;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Mega-phase 5b/1.9.176: the managed Super Duper + Devouring Storms shader pack ships INSIDE the mod jar
 * (assets/dabywitherstormmod/shaderpacks/devouringstorms.zip) and installs
 * itself on launch - user order: "shader pack merged into the mod, on/off
 * toggle, DEFAULT ON".
 *
 * On every launch (hooked from McsmBuiltinPack.register(), which the mod's
 * own initializer mixin calls before any client mod - Iris included - has
 * read its config):
 *
 *   1. toggle ON  -> extract the embedded zip into shaderpacks/ whenever it
 *                    is missing or was written by an older build (a marker
 *                    file carries the build version), then select it in
 *                    Iris's config IF no other pack is currently selected.
 *                    A player who picked their own pack keeps it - ours just
 *                    sits in the folder, selectable.
 *   2. toggle OFF -> delete our zip + marker, and if Iris currently selects
 *                    OUR pack, hand the selection back to (internal). A pack
 *                    the player selected themselves is never touched.
 *
 * Everything is best-effort and Throwable-guarded: no Iris, no write access
 * or a jar without the embedded zip degrades to "nothing happens" - the
 * mod's own core-shader Story Look keeps running as the no-Iris fallback.
 */
public final class McsmShaderPackInstall {

    private static final String PACK_RES  = "/assets/dabywitherstormmod/shaderpacks/devouringstorms.zip";
    private static final String PACK_NAME = "DevouringStorms-SuperDuperDefault.zip";
    private static final String MARKER    = "DevouringStorms.version";

    private static boolean attempted = false;

    private McsmShaderPackInstall() {
    }

    public static void install() {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            McsmExtrasConfig.load();
            File gameDir = gameDir();
            if (gameDir == null) {
                return;
            }
            File packs  = new File(gameDir, "shaderpacks");
            File target = new File(packs, PACK_NAME);
            File marker = new File(packs, MARKER);
            boolean want = McsmExtrasConfig.embeddedShaderPack;

            if (want) {
                String have = read(marker);
                if (!target.isFile() || !McsmExtrasConfig.BUILD_VERSION.equals(have)) {
                    InputStream in = McsmShaderPackInstall.class.getResourceAsStream(PACK_RES);
                    if (in == null) {
                        return; // jar without the embedded pack: nothing to do
                    }
                    try {
                        packs.mkdirs();
                        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } finally {
                        in.close();
                    }
                    clearManagedShaderOptions(packs);
                    write(marker, McsmExtrasConfig.BUILD_VERSION);
                }
            } else {
                if (target.isFile()) {
                    target.delete();
                }
                if (marker.isFile()) {
                    marker.delete();
                }
            }
            selectIris(gameDir, want);
        } catch (Throwable t) {
            // never crash the game over a shader pack
        }
    }

    /**
     * Writes the pack selection into Iris's own config file. Runs during the
     * mod's common initialization, i.e. before Iris reads that file on the
     * client side, so the choice normally applies to the very same launch.
     */
    private static void selectIris(File gameDir, boolean want) {
        try {
            File cfgDir = new File(gameDir, "config");
            File propsFile = new File(cfgDir, "iris.properties");
            Properties p = new Properties();
            if (propsFile.isFile()) {
                try (InputStream in = new FileInputStream(propsFile)) {
                    p.load(in);
                }
            }
            String cur = p.getProperty("shaderPack", "").trim();
            boolean ours = cur.contains("DevouringStorms");
            boolean free = cur.isEmpty() || "(internal)".equals(cur) || "none".equalsIgnoreCase(cur);
            if (want) {
                if (!ours && !free) {
                    return; // the player chose another pack - respect it
                }
                p.setProperty("shaderPack", PACK_NAME);
                p.setProperty("enableShaders", "true");
            } else {
                if (!ours) {
                    return; // not ours to remove
                }
                p.setProperty("shaderPack", "(internal)");
                p.setProperty("enableShaders", "false");
            }
            cfgDir.mkdirs();
            try (OutputStream out = new FileOutputStream(propsFile)) {
                p.store(out, "Iris config (Devouring Storms manages the shaderPack line for its built-in pack; MCSM Control Panel toggle)");
            }
        } catch (Throwable t) {
            // best-effort only
        }
    }

    private static void clearManagedShaderOptions(File packs) {
        // Iris keeps per-pack option overrides outside the zip on some builds.
        // If a user previously had a heavy Custom profile, replacing only the
        // zip can still leave SSR/shadows/volumetrics enabled. When our managed
        // pack updates, remove only option sidecars whose names clearly belong
        // to this pack so the safe defaults inside the new zip win again.
        try {
            File[] files = packs.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                String n = f.getName();
                if (!n.startsWith(PACK_NAME) || n.equals(PACK_NAME)) {
                    continue;
                }
                if (n.endsWith(".txt") || n.endsWith(".properties") || n.endsWith(".json")) {
                    f.delete();
                }
            }
        } catch (Throwable ignored) {
        }
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
            return new String(Files.readAllBytes(f.toPath())).trim();
        } catch (Throwable t) {
            return "";
        }
    }

    private static void write(File f, String s) {
        try {
            try (OutputStream out = new FileOutputStream(f)) {
                out.write(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Throwable t) {
            // ignore
        }
    }
}
