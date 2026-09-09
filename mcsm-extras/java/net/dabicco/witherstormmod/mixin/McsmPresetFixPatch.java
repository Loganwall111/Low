package net.dabicco.witherstormmod.mixin;

import java.lang.reflect.Field;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.Minecraft;

/**
 * Devouring Storms: mega-phase 10 - the visual PRESETS actually stick.
 *
 * The base mod ships five presets (Custom, MCSM OG Visuals, Legacy Java,
 * Cinematic, Netflix) but its own recogniser only ever scanned three:
 *
 *     for (int preset = 1; preset &lt;= 3; preset++)
 *
 * Preset 4 - the newest one - is missing from that loop, and refreshPreset()
 * runs at the end of load(). So picking it did apply its values, and then
 * the very next launch read them back, failed to recognise them, and set the
 * selector to "Custom". That is the "new model preset doesn't work" report:
 * the look was applied but the mod forgot which preset it was, so the GUI
 * offered no way back to it and any reset lost it.
 *
 * The same oversight hits isPresetKey(), which asks only PRESET_MCSM whether
 * a key belongs to a preset - so the keys unique to Cinematic and Netflix
 * were not treated as preset-owned at all.
 *
 * Both are re-implemented here across ALL FOUR preset maps. The comparison
 * reads the config's own public static fields by name (they are named after
 * their keys), handling double, float, int and boolean storage, and any key
 * it cannot resolve is skipped rather than counted as a mismatch.
 */
@Mixin(DabyWSClientConfig.class)
public abstract class McsmPresetFixPatch {

    @Shadow(remap = false) @Final private static Map<String, Double> PRESET_MCSM;
    @Shadow(remap = false) @Final private static Map<String, Double> PRESET_LEGACY;
    @Shadow(remap = false) @Final private static Map<String, Double> PRESET_CINEMATIC;
    @Shadow(remap = false) @Final private static Map<String, Double> PRESET_NETFLIX;

    @Unique
    private static Map<String, Double> dabyws$preset(int i) {
        switch (i) {
            case 1: return PRESET_MCSM;
            case 2: return PRESET_LEGACY;
            case 3: return PRESET_CINEMATIC;
            case 4: return PRESET_NETFLIX;
            default: return null;
        }
    }

    /** Current value of a config key, read off the field of the same name. */
    @Unique
    private static Double dabyws$current(String key) {
        try {
            Field f = DabyWSClientConfig.class.getField(key);
            Class<?> t = f.getType();
            if (t == double.class) {
                return f.getDouble(null);
            }
            if (t == float.class) {
                return (double) f.getFloat(null);
            }
            if (t == int.class) {
                return (double) f.getInt(null);
            }
            if (t == boolean.class) {
                return f.getBoolean(null) ? 1.0 : 0.0;
            }
        } catch (Throwable ignored) {
            // unresolvable key: not a mismatch, just not comparable
        }
        return null;
    }

    @Unique
    private static boolean dabyws$matches(Map<String, Double> preset) {
        if (preset == null || preset.isEmpty()) {
            return false;
        }
        int compared = 0;
        for (Map.Entry<String, Double> e : preset.entrySet()) {
            Double have = dabyws$current(e.getKey());
            if (have == null) {
                continue;
            }
            compared++;
            if (Math.abs(have - e.getValue()) > 1.0E-6) {
                return false;
            }
        }
        return compared > 0;
    }

    /**
     * Recognise all four presets, newest first, so Netflix and Cinematic can
     * survive a restart instead of decaying into "Custom".
     */
    @Inject(method = "refreshPreset", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dabyws$refreshAllPresets(CallbackInfo ci) {
        if (Minecraft.getInstance() == null) {
            return;
        }
        try {
            for (int i = 4; i >= 1; i--) {
                if (dabyws$matches(dabyws$preset(i))) {
                    DabyWSClientConfig.effectsPreset = i;
                    ci.cancel();
                    return;
                }
            }
            DabyWSClientConfig.effectsPreset = 0.0;
            ci.cancel();
        } catch (Throwable ignored) {
            // fall through to the base implementation
        }
    }

    /** A key owned by ANY preset, not just the MCSM one. */
    @Inject(method = "isPresetKey", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dabyws$anyPresetKey(String name, CallbackInfoReturnable<Boolean> cir) {
        try {
            for (int i = 1; i <= 4; i++) {
                Map<String, Double> p = dabyws$preset(i);
                if (p != null && p.containsKey(name)) {
                    cir.setReturnValue(Boolean.TRUE);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // fall through to the base implementation
        }
    }
}
