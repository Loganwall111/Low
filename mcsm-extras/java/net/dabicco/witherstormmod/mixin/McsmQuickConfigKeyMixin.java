package net.dabicco.witherstormmod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.mcsm.extras.client.McsmExtrasScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Devouring Storms quick menu hotkey.
 *
 * The release notes promised Shift+C, but no class was actually polling that
 * combo, so players could be on a new jar and still think it was old because
 * the Control Panel never opened. This tiny client mixin polls GLFW by
 * reflection on Minecraft.tick(): no registration bootstrap required, and if a
 * future LWJGL/window API changes it quietly disables only the shortcut.
 */
@Mixin(Minecraft.class)
public abstract class McsmQuickConfigKeyMixin {

    @Unique private static boolean mcsm$keyWasDown = false;
    @Unique private static long mcsm$lastOpenMs = 0L;

    @Inject(method = "tick", at = @At("TAIL"))
    private void mcsm$shiftCQuickPanel(CallbackInfo ci) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;

            boolean down = mcsm$isShiftCDown(mc);
            if (!down) {
                mcsm$keyWasDown = false;
                return;
            }
            if (mcsm$keyWasDown) return;
            mcsm$keyWasDown = true;

            long now = System.currentTimeMillis();
            if (now - mcsm$lastOpenMs < 350L) return;
            mcsm$lastOpenMs = now;

            // Only hijack normal in-game play. If the player is already in a
            // menu, the explicit Devouring Storms button inside the settings
            // screen still handles opening the panel.
            if (mcsm$currentScreen(mc) != null) return;

            mc.setScreenAndShow(new McsmExtrasScreen((Screen) null));
            System.err.println("[MCSM] Shift+C opened Devouring Storms Control Panel");
        } catch (Throwable t) {
            System.err.println("[MCSM] Shift+C quick panel failed: " + t);
        }
    }

    @Unique
    private static Screen mcsm$currentScreen(Minecraft mc) {
        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (Screen.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (Screen) f.get(mc);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Unique
    private static boolean mcsm$isShiftCDown(Minecraft mc) {
        try {
            Object windowObj = null;
            for (Method m : Minecraft.class.getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().equals("getWindow")) {
                    windowObj = m.invoke(mc);
                    break;
                }
            }
            if (windowObj == null) return false;

            long handle = 0L;
            for (Method m : windowObj.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().equals("getWindow")) {
                    Object v = m.invoke(windowObj);
                    if (v instanceof Number) {
                        handle = ((Number) v).longValue();
                    }
                    break;
                }
            }
            if (handle == 0L) return false;

            Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
            Method getKey = glfw.getMethod("glfwGetKey", long.class, int.class);
            int press = ((Number) glfw.getField("GLFW_PRESS").get(null)).intValue();
            int keyC = ((Number) glfw.getField("GLFW_KEY_C").get(null)).intValue();
            int leftShift = ((Number) glfw.getField("GLFW_KEY_LEFT_SHIFT").get(null)).intValue();
            int rightShift = ((Number) glfw.getField("GLFW_KEY_RIGHT_SHIFT").get(null)).intValue();

            boolean c = ((Number) getKey.invoke(null, handle, keyC)).intValue() == press;
            boolean shift = ((Number) getKey.invoke(null, handle, leftShift)).intValue() == press
                    || ((Number) getKey.invoke(null, handle, rightShift)).intValue() == press;
            return c && shift;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
