package net.dabicco.witherstormmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dabicco.witherstormmod.client.gui.WitherStormConfigScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Devouring Storms config-screen reskin.
 *
 * 1.9.183 clean restart: do not draw a second Devouring Storms banner over the
 * base title. Keep the Story Mode frame/borders and moody plate, but leave the
 * top text area alone so the screen no longer looks doubled or mis-layered.
 */
@Mixin(WitherStormConfigScreen.class)
public abstract class McsmConfigReskinMixin {

    @Shadow private boolean previewShown;

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("RETURN"), remap = false, require = 0)
    private void dabyws$previewOffByDefault(CallbackInfo ci) {
        // The live Wither Storm model preview can allocate a burst of dynamic
        // GL buffers before the game world has settled. Keep the button, but
        // start it OFF so opening the config cannot trigger GL_OUT_OF_MEMORY.
        this.previewShown = false;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), remap = false)
    private void dabyws$mcsPlate(GuiGraphicsExtractor g, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        WitherStormConfigScreen self = (WitherStormConfigScreen) (Object) this;
        int w = self.width;
        int h = self.height;
        g.fillGradient(0, 0, w, h, 0xFF120A1E, 0xFF05030A);
        g.fillGradient(0, h * 3 / 4, w, h, 0x00000000, 0x443F255A);
        // subtle side panels like the Telltale UI frame
        g.fillGradient(0, 0, 18, h, 0xAA0A0612, 0x22140622);
        g.fillGradient(w - 18, 0, w, h, 0x22140622, 0xAA0A0612);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), remap = false)
    private void dabyws$mcsChrome(GuiGraphicsExtractor g, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        WitherStormConfigScreen self = (WitherStormConfigScreen) (Object) this;
        int w = self.width;
        int h = self.height;
        // cinematic accent frame only; no duplicate top banner text
        g.fillGradient(0, 0, w, 3, 0xFF6A8FF7, 0xFF3F255A);
        g.fillGradient(0, h - 3, w, h, 0xFF3F255A, 0xFF6A8FF7);
        g.fillGradient(0, 0, 4, h, 0xCC6A8FF7, 0x223F255A);
        g.fillGradient(w - 4, 0, w, h, 0x223F255A, 0xCC6A8FF7);
        g.fillGradient(18, 54, 20, h - 40, 0x663F255A, 0x226A8FF7);
        g.fillGradient(w - 20, 54, w - 18, h - 40, 0x226A8FF7, 0x663F255A);
    }
}
