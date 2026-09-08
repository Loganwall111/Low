package net.mcsm.extras.client;

import net.mcsm.extras.McsmExtrasConfig;
import net.mcsm.extras.McsmGate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * The MCSM Control Panel — our OWN screen so we never fight the mod config
 * screen's section-fold/tab machinery again (2026-09-04 bug: inline rows never
 * saw a relayout; the header showed [-] with an empty body).
 *
 * 26.2 NOTE (2026-09-05 compile audit): the 26.2 GUI refactor replaced the old
 * render() widget pipeline with extractRenderState(GuiGraphicsExtractor,...)
 * and exposes screen switching through Minecraft.setScreenAndShow(...). Every API used
 * below is the EXACT shape the mod's own (compiling) WitherStormConfigScreen
 * uses: Screen(Component), protected init(), clearWidgets(), addWidget(),
 * Button.builder(...).bounds(...).build(), b.setMessage(...), an
 * AbstractSliderButton subclass reading this.value with updateMessage()/
 * applyValue(), and a chrome list rendered via
 * widget.extractRenderState(g, mouseX, mouseY, partialTick). No
 * addRenderableWidget / CycleButton / isPauseScreen / this.minecraft — those
 * could not be verified against 26.2 and two of them are known-moved.
 *
 * Runtime-safe: on any failure the extras button in the mod's own screen
 * simply does nothing further.
 */
public final class McsmExtrasScreen extends Screen {

    private final Screen parent;
    private final List<AbstractWidget> chrome = new ArrayList<>();
    private final Map<AbstractWidget, Integer> baseY = new HashMap<>();
    private int scrollPx = 0;
    private int contentBottom = 0;

    public McsmExtrasScreen(Screen parent) {
        super(Component.literal("MCSM Storm Control Panel"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.chrome.clear();
        this.baseY.clear();
        this.contentBottom = 0;
        McsmExtrasConfig.load();

        // two columns, scrollable. The previous fixed layout let lower rows
        // disappear behind Done on normal GUI scales, which made sliders feel
        // like +/- only controls with no way to reach the rest of the menu.
        int rowH = 22;
        int gap = 10;
        int colW = Math.min(240, (this.width - 24 - gap) / 2);
        if (colW < 100) { colW = 100; }
        int left = (this.width - (colW * 2 + gap)) / 2;
        int top = 34;
        final int fColW = colW;

        // MCSM -- the drawn header at y=12 never appeared in the
        // player's screenshots, so the build number also rides as a widget
        // row: widgets demonstrably render, and "which build is this panel?"
        // becomes answerable at a glance, spanning both columns.
        Button ver = Button.builder(
                Component.literal("Devouring Storms " + McsmExtrasConfig.BUILD_VERSION
                                  + "  -- this build"), b -> { })
                .bounds(left, top, fColW * 2 + gap, 20).build();
        ver.active = false;
        this.addWidget(ver);
        this.chrome.add(ver);
        this.baseY.put(ver, top);

        // ---- column 1: visuals ----------------------------------------------
        addSlider(0, 1, fColW, gap, left, top, rowH, "Glare Size", "%.2fx",
                0.25, 3.05, () -> McsmExtrasConfig.glareSize, v -> McsmExtrasConfig.glareSize = v);
        addToggle(0, 2, fColW, gap, left, top, rowH, "In-Mod Aurora",
                () -> McsmExtrasConfig.auroraEnabled, v -> McsmExtrasConfig.auroraEnabled = v);
        addToggle(0, 3, fColW, gap, left, top, rowH, "Death Cinematic",
                () -> McsmExtrasConfig.deathCinematic, v -> McsmExtrasConfig.deathCinematic = v);
        addToggle(0, 4, fColW, gap, left, top, rowH, "Supernova Rings",
                () -> McsmExtrasConfig.supernovaRings, v -> McsmExtrasConfig.supernovaRings = v);
        addToggle(0, 5, fColW, gap, left, top, rowH, "Smoke Screen + Sparks",
                () -> McsmExtrasConfig.smokeScreen, v -> McsmExtrasConfig.smokeScreen = v);
        addToggle(0, 6, fColW, gap, left, top, rowH, "Purple Sky (5.5+)",
                () -> McsmExtrasConfig.purpleSky, v -> McsmExtrasConfig.purpleSky = v);
        addToggle(0, 7, fColW, gap, left, top, rowH, "Dust Waves",
                () -> McsmExtrasConfig.dustWaves, v -> McsmExtrasConfig.dustWaves = v);
        addToggle(0, 8, fColW, gap, left, top, rowH, "Reality Tear",
                () -> McsmExtrasConfig.realityTear, v -> McsmExtrasConfig.realityTear = v);
        addToggle(0, 9, fColW, gap, left, top, rowH, "OG CEM Models",
                () -> McsmExtrasConfig.ogCemModels, v -> McsmExtrasConfig.ogCemModels = v);
        addSlider(0, 10, fColW, gap, left, top, rowH, "Smudge Scale", "%.2fx",
                0.10, 2.00, () -> McsmExtrasConfig.smudgeScale, v -> McsmExtrasConfig.smudgeScale = v);
        // MCSM -- the shader pack now ships inside the mod and picks
        // itself in Iris at launch; this is the on/off the user asked for.
        addToggle(0, 11, fColW, gap, left, top, rowH, "Built-in Shader Pack",
                () -> McsmExtrasConfig.embeddedShaderPack, v -> McsmExtrasConfig.embeddedShaderPack = v);

        // ---- column 2: gameplay ---------------------------------------------
        addToggle(1, 0, fColW, gap, left, top, rowH, "Obliterate Flash",
                () -> McsmExtrasConfig.obliterateFlash, v -> McsmExtrasConfig.obliterateFlash = v);
        addToggle(1, 1, fColW, gap, left, top, rowH, "Obliterate Kicks Players",
                () -> McsmExtrasConfig.obliterateKick, v -> McsmExtrasConfig.obliterateKick = v);
        addToggle(1, 2, fColW, gap, left, top, rowH, "Tentacle Grab",
                () -> McsmExtrasConfig.enableTentacleGrab, v -> McsmExtrasConfig.enableTentacleGrab = v);
        addSlider(1, 3, fColW, gap, left, top, rowH, "Grab Interval", "%.1f s",
                0.0, 30.0, () -> McsmExtrasConfig.grabIntervalSeconds, v -> McsmExtrasConfig.grabIntervalSeconds = v);
        addToggle(1, 4, fColW, gap, left, top, rowH, "Lit Beacon Relay",
                () -> McsmExtrasConfig.enableBeaconStorm, v -> McsmExtrasConfig.enableBeaconStorm = v);
        addSlider(1, 5, fColW, gap, left, top, rowH, "Beacon Cooldown", "%.0f s",
                2.0, 120.0, () -> McsmExtrasConfig.beaconCooldownSeconds, v -> McsmExtrasConfig.beaconCooldownSeconds = v);
        addToggle(1, 6, fColW, gap, left, top, rowH, "Storm Beacon Block",
                () -> McsmExtrasConfig.enableBeaconBlock, v -> McsmExtrasConfig.enableBeaconBlock = v);
        addToggle(1, 7, fColW, gap, left, top, rowH, "Rise Ground FX",
                () -> McsmExtrasConfig.enableRiseFx, v -> McsmExtrasConfig.enableRiseFx = v);
        addToggle(1, 8, fColW, gap, left, top, rowH, "Counterclockwise Spiral",
                () -> McsmExtrasConfig.spiralCounterClockwise, v -> McsmExtrasConfig.spiralCounterClockwise = v);
        addToggle(1, 9, fColW, gap, left, top, rowH, "Force MCSM Look",
                () -> McsmExtrasConfig.forceMcsmLook, v -> McsmExtrasConfig.forceMcsmLook = v);
        addToggle(1, 10, fColW, gap, left, top, rowH, "Force MCSM World",
                () -> McsmExtrasConfig.forceMcsmWorld, v -> McsmExtrasConfig.forceMcsmWorld = v);
        addToggle(1, 11, fColW, gap, left, top, rowH, "Command Block Wire",
                () -> McsmExtrasConfig.commandWire, v -> McsmExtrasConfig.commandWire = v);
        addToggle(1, 12, fColW, gap, left, top, rowH, "MCSM Instructions",
                () -> McsmExtrasConfig.mcsmInstructions, v -> McsmExtrasConfig.mcsmInstructions = v);
        // MCSM -- hands the shader-pack answer back to the mod so the
        // look presets can be A/B tested; see McsmShaderGatePatch.
        addToggle(1, 13, fColW, gap, left, top, rowH, "Shader Pack Gate",
                () -> McsmExtrasConfig.shaderPackGate, v -> McsmExtrasConfig.shaderPackGate = v);
        // MCSM -- the gate now respects anything changed after its
        // first pass, so look presets applied in the mod's own screen survive
        // this panel being opened and clicked. Forcing the full MCSM look
        // again mid-session is therefore an explicit act: this button.
        Button reapply = Button.builder(Component.literal("Re-apply MCSM Look now"), b -> {
            McsmGate.clearMemory();
            McsmGate.reset();
        }).bounds(left + fColW + gap, top + 14 * rowH, fColW, 20).build();
        this.addWidget(reapply);
        this.chrome.add(reapply);
        this.baseY.put(reapply, top + 14 * rowH);

        this.contentBottom = top + 15 * rowH + 4;
        applyScrollLayout();

        Button done = Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build();
        this.addWidget(done);
        this.chrome.add(done);
    }

    private static Component toggleLabel(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private void addToggle(int col, int row, int colW, int gap, int left, int top, int rowH,
                           String label, BooleanSupplier get, Consumer<Boolean> set) {
        int x = left + col * (colW + gap);
        int y = top + row * rowH;
        Button b = Button.builder(toggleLabel(label, get.getAsBoolean()), btn -> {
            set.accept(!get.getAsBoolean());
            McsmExtrasConfig.save();
            McsmGate.reset();
            btn.setMessage(toggleLabel(label, get.getAsBoolean()));
        }).bounds(x, y, colW, 20).build();
        this.addWidget(b);
        this.chrome.add(b);
        this.baseY.put(b, y);
        this.contentBottom = Math.max(this.contentBottom, y + 20);
    }

    private void addSlider(int col, int row, int colW, int gap, int left, int top, int rowH,
                           String label, String fmt, double lo, double hi,
                           DoubleSupplier get, Consumer<Double> set) {
        int x = left + col * (colW + gap);
        int y = top + row * rowH;
        Slider s = new Slider(x, y, colW, label, fmt, lo, hi, get, set);
        this.addWidget(s);
        this.chrome.add(s);
        this.baseY.put(s, y);
        this.contentBottom = Math.max(this.contentBottom, y + 20);
    }

    /**
     * Same shape as the mod's own (compiling) WitherStormConfigScreen$ConfigSlider:
     * AbstractSliderButton(int,int,int,int,Component,double) with this.value,
     * updateMessage() and applyValue().
     */
    private static final class Slider extends AbstractSliderButton {
        private final String label;
        private final String fmt;
        private final double lo;
        private final double hi;
        private final DoubleSupplier get;
        private final Consumer<Double> set;

        Slider(int x, int y, int w, String label, String fmt, double lo, double hi,
               DoubleSupplier get, Consumer<Double> set) {
            super(x, y, w, 20,
                    Component.literal(label + ": " + String.format(fmt, clamp(get.getAsDouble(), lo, hi))),
                    (clamp(get.getAsDouble(), lo, hi) - lo) / (hi - lo));
            this.label = label;
            this.fmt = fmt;
            this.lo = lo;
            this.hi = hi;
            this.get = get;
            this.set = set;
        }

        private static double clamp(double v, double lo, double hi) {
            return v < lo ? lo : (v > hi ? hi : v);
        }

        private double actual() {
            return this.lo + (this.hi - this.lo) * this.value;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(this.label + ": \u00a7e" + String.format(this.fmt, this.actual())));
        }

        @Override
        protected void applyValue() {
            this.set.accept(this.actual());
            McsmExtrasConfig.save();
            McsmGate.reset();
        }
    }

    private void applyScrollLayout() {
        int max = Math.max(0, this.contentBottom - (this.height - 36));
        if (this.scrollPx < 0) this.scrollPx = 0;
        if (this.scrollPx > max) this.scrollPx = max;
        for (Map.Entry<AbstractWidget, Integer> e : this.baseY.entrySet()) {
            AbstractWidget w = e.getKey();
            int y = e.getValue() - this.scrollPx;
            w.setY(y);
            boolean show = y >= 28 && y <= this.height - 42;
            w.visible = show;
            w.active = show;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollPx -= (int) Math.round(scrollY * 24.0);
        applyScrollLayout();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xB0101010);
        applyScrollLayout();
        g.centeredText(this.font, "Devouring Storms  --  The Point of No Return  " + McsmExtrasConfig.BUILD_VERSION, this.width / 2, 12, 0xFFFFFF);
        g.centeredText(this.font, "Scroll wheel moves this panel. Changes save instantly.", this.width / 2, 23, 0xA0A0A0);
        if (this.contentBottom > this.height - 36) {
            g.centeredText(this.font, "scroll " + this.scrollPx + "/" + Math.max(0, this.contentBottom - (this.height - 36)), this.width - 62, 12, 0xA0A0A0);
        }
        for (AbstractWidget widget : this.chrome) {
            widget.extractRenderState(g, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void onClose() {
        McsmExtrasConfig.save();
        Minecraft.getInstance().setScreenAndShow(this.parent);
    }
}
