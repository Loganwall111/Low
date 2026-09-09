package net.dabicco.witherstormmod.mixin;

import net.mcsm.extras.McsmExtrasConfig;

import net.dabicco.witherstormmod.client.gui.WitherStormConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.mcsm.extras.client.McsmExtrasScreen;

/**
 * MCSM - extras entry inside the mod's own config screen.
 *
 * MCSM 1.9.98 -- ROOT-CAUSED FIX for "clicking [+]/- shows no options"
 * (user screenshot 2026-09-04 145751). Their screen folds content by section
 * (collapsed set) and keys rows to tabs; rows we appended at init() TAIL were
 * laid out by repositionRows() BEFORE we added them, so the "MCSM extras"
 * header got a [-] state but its rows never received bounds -> empty section.
 * Two attempted generations of direct row injection (toggles/sliders inline)
 * hit exactly this.
 *
 * New contract, deliberately tiny: ONE header row + ONE button row. The button
 * opens our own full panel (net.mcsm.extras.client.McsmExtrasScreen), which we
 * control end to end -- no dependence on their fold internals at all. Then we
 * call their repositionRows() by exact name so the two rows get laid out
 * (verified from the shipped jar's method table: rebuild() regenerates and
 * would drop us -- never call it).
 *
 * MCSM 1.9.104: also adds a direct fixed-position button via Screen.addWidget
 * reflection. The row API can visually mis-layout at the bottom of this screen
 * on some GUI scales (black off-screen rectangle / no clickable panel). The
 * fixed button does not depend on their tab/row/fold machinery at all.
 * MCSM 1.9.105: render/click are injected directly too, so even if their
 * custom screen never draws normal child widgets the bottom-left button is
 * visible and opens from our own mouse handler.
 *
 * Fully silent on any failure: a future refactor of their GUI costs us the
 * injected controls, never a crash.
 */
@Mixin(WitherStormConfigScreen.class)
public abstract class McsmGuiExtrasRows {

    @Unique private int mcsm$lastMouseX = 0;
    @Unique private int mcsm$lastMouseY = 0;

    @Unique
    private static int mcsm$buttonX() { return 8; }

    @Unique
    private static int mcsm$buttonY(Screen sc) { return Math.max(8, sc.height - 58); }

    @Unique
    private static int mcsm$buttonW() { return 206; }

    @Unique
    private static int mcsm$buttonH() { return 20; }

    private static void mcsm$openPanel(Object self) {
        try {
            Screen panel = new McsmExtrasScreen((Screen) self);
            Minecraft.getInstance().setScreenAndShow(panel);
            System.err.println("[MCSM] extras panel opened via setScreenAndShow");
        } catch (Throwable t) {
            try {
                Minecraft.getInstance().gui.setScreen(new McsmExtrasScreen((Screen) self));
                System.err.println("[MCSM] extras panel opened via gui.setScreen fallback");
            } catch (Throwable t2) {
                System.err.println("[MCSM] extras panel open FAILED: " + t + " / " + t2);
            }
        }
    }

    private static void mcsm$addDirectButton(Object self) {
        // 1.9.183: no fixed overlay button. It covered the base screen's
        // own bottom controls at several GUI scales. Access is through the
        // single row button below or Shift+C.
    }

    @Inject(method = {"init"}, at = @At("TAIL"))
    private void mcsm$extrasRows(CallbackInfo ci) {
        try {
            McsmExtrasConfig.load();
            final Object self = this;
            mcsm$addDirectButton(self);
            Class<?> screen = WitherStormConfigScreen.class;
            Class<?> rowCls = Class.forName("net.dabicco.witherstormmod.client.gui.WitherStormConfigScreen$Row");
            Method mButton = rowCls.getDeclaredMethod("button", String.class, String.class, Runnable.class);
            Method mAdd = screen.getDeclaredMethod("addRowWidget", rowCls);
            for (Method m : new Method[]{mButton, mAdd}) m.setAccessible(true);

            // 1.9.183: one clean entry only. The previous header + button
            // looked like duplicate Devouring Storms rows and made the base
            // config screen feel broken.
            mAdd.invoke(self, mButton.invoke(null,
                    "Open Devouring Storms " + McsmExtrasConfig.BUILD_VERSION,
                    "Full Story Mode control panel: atmosphere, shaders, NPCs, storm VFX, world/story toggles.",
                    (Runnable) () -> mcsm$openPanel(self)));

            // exact-name relayout (see class doc for why repositionRows, not rebuild)
            try {
                Method mReposition = screen.getDeclaredMethod("repositionRows");
                mReposition.setAccessible(true);
                mReposition.invoke(self);
                System.err.println("[MCSM] extras rows: relayout OK (repositionRows)");
            } catch (Throwable t2) {
                System.err.println("[MCSM] extras rows relayout skipped: " + t2);
            }
        } catch (Throwable t) {
            System.err.println("[MCSM] extras GUI rows skipped: " + t);
        }
    }


    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
        at = @At("TAIL")
    )
    private void mcsm$renderDirectButton(GuiGraphicsExtractor g, int mouseX, int mouseY,
                                         float partialTick, CallbackInfo ci) {
        // Fixed overlay button removed in 1.9.183; keep the mixin method as a
        // harmless no-op so older configs do not get an overlapping button.
    }

    @Inject(
        method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void mcsm$clickDirectButton(MouseButtonEvent event, boolean doubleClick,
                                        CallbackInfoReturnable<Boolean> cir) {
        // Fixed overlay button removed in 1.9.183.
    }

}
