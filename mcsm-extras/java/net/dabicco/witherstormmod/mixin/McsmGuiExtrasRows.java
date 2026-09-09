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
        try {
            Screen sc = (Screen) self;
            Button direct = Button.builder(
                    Component.literal("Devouring Storms " + McsmExtrasConfig.BUILD_VERSION),
                    b -> mcsm$openPanel(self))
                .bounds(mcsm$buttonX(), mcsm$buttonY(sc), mcsm$buttonW(), mcsm$buttonH())
                .build();

            // 26.2 keeps addWidget protected/non-public. Search by shape so a
            // descriptor change from AbstractWidget to GuiEventListener does not
            // break compilation or runtime discovery.
            Method add = null;
            Class<?> k = Screen.class;
            while (k != null && add == null) {
                for (Method m : k.getDeclaredMethods()) {
                    if (!m.getName().equals("addWidget") || m.getParameterCount() != 1) continue;
                    Class<?> pt = m.getParameterTypes()[0];
                    if (pt.isAssignableFrom(Button.class) || pt.isAssignableFrom(direct.getClass())
                            || pt.getName().contains("GuiEventListener") || pt == Object.class) {
                        add = m;
                        break;
                    }
                }
                k = k.getSuperclass();
            }
            if (add != null) {
                add.setAccessible(true);
                add.invoke(sc, direct);
                System.err.println("[MCSM] direct MCSM Extras button added");
            } else {
                System.err.println("[MCSM] direct MCSM Extras button skipped: Screen.addWidget not found");
            }
        } catch (Throwable t) {
            System.err.println("[MCSM] direct MCSM Extras button failed: " + t);
        }
    }

    @Inject(method = {"init"}, at = @At("TAIL"))
    private void mcsm$extrasRows(CallbackInfo ci) {
        try {
            McsmExtrasConfig.load();
            final Object self = this;
            mcsm$addDirectButton(self);
            Class<?> screen = WitherStormConfigScreen.class;
            Class<?> rowCls = Class.forName("net.dabicco.witherstormmod.client.gui.WitherStormConfigScreen$Row");
            Method mHeader = rowCls.getDeclaredMethod("header", String.class, int.class);
            Method mButton = rowCls.getDeclaredMethod("button", String.class, String.class, Runnable.class);
            Method mAdd = screen.getDeclaredMethod("addRowWidget", rowCls);
            for (Method m : new Method[]{mHeader, mButton, mAdd}) m.setAccessible(true);

            // MCSM 1.9.109 -- single-sourced version: this header used to be a
            // hand-typed literal and lagged the jar by three builds, so the
            // screen always claimed to be an older version than the file the
            // user had just installed.
            mAdd.invoke(self, mHeader.invoke(null,
                    "Devouring Storms " + McsmExtrasConfig.BUILD_VERSION, 0));
            mAdd.invoke(self, mButton.invoke(null,
                    "Open the MCSM Control Panel",
                    "Glare size, aurora, death cinematic, supernova rings, smoke screen, purple sky, dust waves, reality tear, obliterate flash, and the gameplay patches.",
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
        try {
            Screen sc = (Screen) (Object) this;
            this.mcsm$lastMouseX = mouseX;
            this.mcsm$lastMouseY = mouseY;
            int x = mcsm$buttonX();
            int y = mcsm$buttonY(sc);
            int w = mcsm$buttonW();
            int h = mcsm$buttonH();
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFFB0A0C8);
            g.fill(x, y, x + w, y + h, hover ? 0xFF6E5A86 : 0xFF4E425E);
            g.centeredText(sc.getFont(), "Devouring Storms " + McsmExtrasConfig.BUILD_VERSION, x + w / 2, y + 6, hover ? 0xFFFFE680 : 0xFFFFFFFF);
        } catch (Throwable t) {
            System.err.println("[MCSM] direct MCSM Extras render failed: " + t);
        }
    }

    @Inject(
        method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void mcsm$clickDirectButton(MouseButtonEvent event, boolean doubleClick,
                                        CallbackInfoReturnable<Boolean> cir) {
        try {
            Screen sc = (Screen) (Object) this;
            int x = mcsm$buttonX();
            int y = mcsm$buttonY(sc);
            int mx = this.mcsm$lastMouseX;
            int my = this.mcsm$lastMouseY;
            if (mx >= x && mx < x + mcsm$buttonW() && my >= y && my < y + mcsm$buttonH()) {
                mcsm$openPanel(this);
                cir.setReturnValue(Boolean.TRUE);
            }
        } catch (Throwable t) {
            System.err.println("[MCSM] direct MCSM Extras click failed: " + t);
        }
    }

}
