package net.dabicco.witherstormmod.mixin;

import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.mcsm.extras.McsmExtrasConfig;

/**
 * Devouring Storms: the MCSM-style main menu overhaul.
 *
 * Telltale's menu is a dark cinematic plate with a big centered logo - so
 * the vanilla panorama is cancelled and replaced with a deep violet night
 * gradient, a storm-glow horizon and softly twinkling stars; a near-opaque
 * logo strip carries "DEVOURING STORMS / THE POINT OF NO RETURN" scaled up
 * at top center, and a matching cinematic bar sits along the bottom with
 * the episode tagline and build stamp. The base mod's own Story Mode
 * buttons (Storm Config / 3D Storm Preview, top-right) stay untouched -
 * this mixin's injectors are appended after the base's, so the strip
 * cleanly covers the base's old banner text.
 *
 * Every call is verified against 26.2: TitleScreen#extractBackground /
 * #extractRenderState(GuiGraphicsExtractor, int, int, float) from the CI
 * probe; fillGradient / fill / centeredText / pose() from the extractor
 * dump; pose pushMatrix/translate/scale from the shipped hotbar code.
 */
@Mixin(TitleScreen.class)
public abstract class McsmTitleOverhaulMixin extends Screen {

    protected McsmTitleOverhaulMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void dabyws$stormBackdrop(GuiGraphicsExtractor g, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        ci.cancel();
        int w = this.width;
        int h = this.height;
        // deep violet night -> near black
        g.fillGradient(0, 0, w, h, 0xFF120A1E, 0xFF05030A);
        // storm glow on the horizon
        g.fillGradient(0, h * 2 / 3, w, h, 0x00000000, 0x553F255A);
        // deterministic twinkling stars over the upper two thirds
        long seed = 20260906L;
        for (int i = 0; i < 120; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sx = (int) Math.floorMod(seed >> 33, Math.max(1, w));
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sy = (int) Math.floorMod(seed >> 33, Math.max(1, h * 2 / 3));
            double tw = Math.sin(System.currentTimeMillis() * 0.0011D + i * 1.7D) * 0.5D + 0.5D;
            int alpha = 60 + (int) (tw * 110);
            g.fill(sx, sy, sx + 1, sy + 1, (alpha << 24) | 0xC8D8FF);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dabyws$mcsMenuChrome(GuiGraphicsExtractor g, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        int w = this.width;
        int h = this.height;
        Font font = Minecraft.getInstance().font;

        // 1.9.183: do not draw a second Devouring Storms logo over the
        // base/title resources. Keep cinematic borders only; one logo is
        // enough and avoids the doubled-title look from the screenshots.
        g.fillGradient(0, 0, w, 3, 0xFF6A8FF7, 0xFF3F255A);
        g.fillGradient(0, 0, 4, h, 0xAA6A8FF7, 0x223F255A);
        g.fillGradient(w - 4, 0, w, h, 0x223F255A, 0xAA6A8FF7);

        // --- bottom cinematic bar -------------------------------------------
        g.fill(0, h - 34, w, h, 0xF20A0612);
        g.fillGradient(0, h - 36, w, h - 34, 0xFF3F255A, 0xFF6A8FF7);
        g.centeredText(font,
                "\u00a77An Episode in Five Acts \u00a78\u2014 \u00a75The Wither Storm Saga",
                w / 2, h - 28, 0xFFB9C6E2);
        g.centeredText(font,
                "\u00a78build " + McsmExtrasConfig.BUILD_VERSION + " \u00a77\u00b7 \u00a78MCSM menu",
                w / 2, h - 16, 0xFF7F8CA8);
    }
}
