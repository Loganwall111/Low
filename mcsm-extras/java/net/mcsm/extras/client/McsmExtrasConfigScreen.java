package net.mcsm.extras.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.client.gui.screen.ScreenSpark;
import net.fabricmc.fabric.client.gui.screen.ConfigScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Slider;
import net.minecraft.client.gui.layouts.ButtonList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

import net.mcsm.extras.McsmExtrasConfig;

@Env(EnvType.CLIENT)
public class McsmExtrasConfigScreen extends ConfigScreen<McsmExtrasConfig> {

    public McsmExtrasConfigScreen(McsmExtrasConfig config, Supplier<Screen> back) {
        super(config, back, Component.literal("MCSM Storm Extras v1.9.171"), 250, 180);
    }
    
    @Override
    protected void init() {
        super.init();
        // Add config controls here
        // Add Witherstorm settings button
        this.addButton(new Button(this.width / 2 - 100, 20, 200, 20, 
            Component.literal("Witherstorm settings"), press -> {
                // Open sub-menu or toggle config
            })
        )
        // Shift+C hint: open holographic config
        // Sliders for: nightSkyOpacity, phase55Threshold, phase5_9PinkIntensity
        // Toggles for: glareAnimPhase, bodyAnimPulse, glareAnimIntensity
        // cloudAlpha, cloudSpeed, precipitationIntensity
    }
    
    @Override
    protected void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render Shift+C hint
        this.font.draw(this.getMinecraft().font, "Shift + C: Holographic config", 4, this.height - 12, 16777215);
    } {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void childrenResized(int width, int height) {
        super.childrenResized(width, height);
    }
}