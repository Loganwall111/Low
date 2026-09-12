package net.dabicco.witherstormmod.client.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.config.PendingWorldConfig;
import net.dabicco.witherstormmod.config.RequestWitherStormConfigPayload;
import net.dabicco.witherstormmod.config.UpdateWitherStormConfigPayload;
import net.dabicco.witherstormmod.config.WitherStormWorldConfig;
import net.dabicco.witherstormmod.config.WitherStormWorldConfig.Key;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

public class WitherStormConfigScreen extends Screen {
   private final Screen parent;
   private final WitherStormWorldConfig editing;
   private boolean serverTouched;
   private boolean requestedSync;
   private String statusText;
   private int statusColor;
   private AbstractWidget serverTabWidget;
   private WitherStormConfigScreen.Tab tab;
   private final int[] scrollOffset;
   private int maxScroll;
   private boolean draggingScrollbar;
   private final List<WitherStormConfigScreen.Row> rows;
   private String currentMasterTitle;
   private String currentSectionTitle;
   private EditBox searchBox;
   private String searchQuery = "";
   private int searchPos;
   private final List<AbstractWidget> chrome;
   private final List<String> tabKeys;
   private final Map<String, List<String>> masterKeys;
   private List<String> currentMasterKeys;
   private final Set<String> collapsed;
   private final Set<WitherStormConfigScreen.Tab> collapseInitialized;
   private boolean skipCurrentSection;
   private boolean skipCurrentMaster;
   private static final int PANEL_WIDTH = 300;
   private static final int ROW_HEIGHT = 26;
   private static final int HEADER_HEIGHT = 20;
   private static final int SECTION_INDENT = 10;
   private static final int PANEL_TOP = 58;
   private static final int PANEL_BOTTOM_MARGIN = 62;
   private static final int LIST_PAD = 8;
   private static final int SCROLLBAR_WIDTH = 6;
   private static final int TAB_WIDTH = 86;
   private static final int TAB_GAP = 4;
   private static final int PREVIEW_WIDTH_MIN = 132;
   private static final int PREVIEW_WIDTH_MAX = 460;
   private static final int PREVIEW_GAP = 8;
   private static final int PREVIEW_MIN_SCREEN = 448;
   private static final int PREVIEW_BAR = 50;
   private static final int COL_PANEL_BG = -435352812;
   private static final int COL_PANEL_BORDER = -12964270;
   private static final int COL_PANEL_INNER = 1075845683;
   private static final int COL_ACCENT = -5011201;
   private static final int COL_MASTER = -5011201;
   private static final int COL_HEADER = -16307;
   private static final int COL_HEADER_LINE = 872415231;
   private static final int COL_TRACK = 1711276032;
   private static final int COL_THUMB = -10859904;
   private static final int COL_THUMB_HI = -7702856;
   private static final Identifier SUN = Identifier.withDefaultNamespace("textures/environment/celestial/sun.png");
   private static final int COL_SKY_TOP = -12690298;
   private static final int COL_SKY_BOTTOM = -7362108;
   private final boolean worldCreation;
   private boolean previewShown;
   private int previewPhase;
   private int previewSub;
   private boolean phasePickerOpen;
   private boolean subPickerOpen;
   private float previewYaw;
   private float previewPitch;
   private float previewZoom;
   private float previewPanX;
   private float previewPanY;
   private boolean previewAutoSpin;
   private int previewDragButton;
   private boolean previewDragMoved;
   private float previewSunAzimuth;
   private float previewYawTarget;
   private long previewTurnMillis;
   private int previewBeamMask;
   private static final int HEAD_CLICK_RADIUS = 22;
   private static final String[] PHASE_LABELS = new String[]{"Phase 0", "Phase 1", "Phase 2", "Phase 3", "Phase 4", "Phase 5", "Phase 6", "Severed Half"};
   private boolean previewGigantic;
   private int previewBackdrop;
   private boolean backdropPickerOpen;
   private boolean confirmOpen;
   private int pendingPreset;
   private final List<String> presetTabKeys;
   private int presetSelection;
   private boolean presetPickerOpen;
   private static final String MASTER_LOOK = "The Storm's Look";
   private static final String MASTER_GAME = "Sound & Interface";
   private static final String MASTER_MOTION = "Animation & Motion Maths";
   private static final String MASTER_ADVANCED = "Rendering Internals";
   private static final String MASTER_SHADOW = "The Sun Shadow";
   private static final String MASTER_GEOMETRY = "Geometry Thinning";

   private int previewWidth() {
      return this.previewGigantic ? Math.max(132, this.width - 24) : Mth.clamp(this.width - 300 - 8 - 8, 132, 460);
   }

   public WitherStormConfigScreen(Screen parent) {
      this(parent, false);
   }

   public static WitherStormConfigScreen createGiganticPreview(Screen parent) {
      WitherStormConfigScreen screen = new WitherStormConfigScreen(parent);
      screen.previewShown = true;
      screen.previewGigantic = true;
      return screen;
   }

   public WitherStormConfigScreen(Screen parent, boolean worldCreation) {
      super(Component.literal(worldCreation ? "Wither Storm Settings For This World" : "Dabicco's Wither Storm Config"));
      this.editing = new WitherStormWorldConfig();
      this.serverTouched = false;
      this.requestedSync = false;
      this.statusText = "";
      this.statusColor = -6381922;
      this.tab = WitherStormConfigScreen.Tab.SERVER;
      this.scrollOffset = new int[WitherStormConfigScreen.Tab.values().length];
      this.maxScroll = 0;
      this.draggingScrollbar = false;
      this.rows = new ArrayList<>();
      this.chrome = new ArrayList<>();
      this.tabKeys = new ArrayList<>();
      this.masterKeys = new LinkedHashMap<>();
      this.currentMasterKeys = null;
      this.collapsed = new HashSet<>();
      this.collapseInitialized = new HashSet<>();
      this.skipCurrentSection = false;
      this.skipCurrentMaster = false;
      this.previewShown = true;
      this.previewPhase = 5;
      this.previewSub = 1;
      this.phasePickerOpen = false;
      this.subPickerOpen = false;
      this.previewYaw = 0.0F;
      this.previewPitch = 18.0F;
      this.previewZoom = 1.0F;
      this.previewPanX = 0.0F;
      this.previewPanY = 0.0F;
      this.previewAutoSpin = true;
      this.previewDragButton = -1;
      this.previewDragMoved = false;
      this.previewSunAzimuth = 35.0F;
      this.previewYawTarget = 0.0F;
      this.previewTurnMillis = 0L;
      this.previewBeamMask = 0;
      this.confirmOpen = false;
      this.pendingPreset = -1;
      this.presetTabKeys = new ArrayList<>();
      this.presetSelection = -1;
      this.presetPickerOpen = false;
      this.worldCreation = worldCreation;
      this.parent = parent;
      if (worldCreation) {
         this.tab = WitherStormConfigScreen.Tab.SERVER;
         this.editing.applyArray(PendingWorldConfig.getOrCreate().toArray());
      } else {
         if (!DabyWSClientConfig.configOpened) {
            DabyWSClientConfig.configOpened = true;
            DabyWSClientConfig.save();
         }

         this.editing.applyArray(net.dabicco.witherstormmod.client.ClientConfigCache.cfg.toArray());
      }
   }

   private boolean inWorld() {
      return Minecraft.getInstance().level != null;
   }

   private boolean previewShadow() {
      return DabyWSClientConfig.stormShadow;
   }

   private StormModelPreview.View previewView() {
      return StormModelPreview.view(
         this.previewWidth(),
         this.previewBottom() - 58,
         this.previewPhase,
         this.previewSub,
         this.previewPitch,
         this.previewZoom,
         this.previewPanX,
         this.previewPanY
      );
   }

   private float previewFacing() {
      return this.previewAutoSpin ? StormModelPreview.autoSpinDegrees() : this.previewYaw;
   }

   private boolean modalOpen() {
      return this.phasePickerOpen || this.subPickerOpen || this.backdropPickerOpen || this.presetPickerOpen || this.pendingPreset > 0 || this.confirmOpen;
   }

   private boolean overPreview(double x, double y) {
      return !this.modalOpen() && this.previewVisible() && x >= this.previewLeft() && x <= this.previewRight() && y >= 58.0 && y <= this.previewBottom();
   }

   private void resetPreviewCamera() {
      this.previewYaw = 0.0F;
      this.previewPitch = 18.0F;
      this.previewZoom = 1.0F;
      this.previewPanX = 0.0F;
      this.previewPanY = 0.0F;
      this.previewAutoSpin = true;
      this.previewYawTarget = 0.0F;
      this.previewBeamMask = 0;
   }

   private void stepPreviewTurn() {
      long now = Util.getMillis();
      float dt = this.previewTurnMillis == 0L ? 0.0F : Mth.clamp((float)(now - this.previewTurnMillis) / 1000.0F, 0.0F, 0.25F);
      this.previewTurnMillis = now;
      if (!this.previewAutoSpin && !(dt <= 0.0F)) {
         float settle = (float)Math.max(0.05, DabyWSClientConfig.yawSmoothTime);
         float a = 1.0F - (float)Math.exp(-dt / settle);
         this.previewYaw = Mth.wrapDegrees(this.previewYaw + Mth.degreesDifference(this.previewYaw, this.previewYawTarget) * a);
      }
   }

   private boolean previewVisible() {
      return this.previewShown && !this.worldCreation && this.tab != WitherStormConfigScreen.Tab.SERVER && this.width >= 448;
   }

   private int contentWidth() {
      if (this.previewGigantic) {
         return this.width - 24;
      } else {
         return this.previewVisible() ? 308 + this.previewWidth() : 300;
      }
   }

   private int panelLeft() {
      return this.width / 2 - this.contentWidth() / 2;
   }

   private int panelRight() {
      return this.panelLeft() + 300;
   }

   private int panelCentre() {
      return this.panelLeft() + 150;
   }

   private int panelBottom() {
      return this.height - 62;
   }

   private int viewTop() {
      return 66;
   }

   private int viewBottom() {
      return this.panelBottom() - 8;
   }

   private int rowX() {
      return this.panelLeft() + 8;
   }

   private int rowWidth() {
      return 273;
   }

   private int tabsLeft() {
      int count = WitherStormConfigScreen.Tab.values().length;
      return this.panelCentre() - (86 * count + 4 * (count - 1)) / 2;
   }

   private int previewLeft() {
      return this.previewGigantic ? 12 : this.panelRight() + 8;
   }

   private int previewRight() {
      return this.previewLeft() + this.previewWidth();
   }

   private int previewBottom() {
      return this.previewGigantic ? this.height - 58 : this.panelBottom() - 50;
   }

   private boolean canEditServer() {
      return this.worldCreation ? true : this.inWorld() && net.dabicco.witherstormmod.client.ClientConfigCache.canEditServer;
   }

   protected void init() {
      this.rows.clear();
      this.chrome.clear();
      this.tabKeys.clear();
      this.masterKeys.clear();
      this.currentMasterKeys = null;
      if (!this.worldCreation && !this.requestedSync && this.inWorld() && ClientPlayNetworking.canSend(RequestWitherStormConfigPayload.TYPE)) {
         ClientPlayNetworking.send(new RequestWitherStormConfigPayload());
         this.requestedSync = true;
      }

      if (!this.canEditServer() && this.tab == WitherStormConfigScreen.Tab.SERVER) {
         this.tab = WitherStormConfigScreen.Tab.CLIENT;
      }

      if (this.phasePickerOpen) {
         int top = this.height / 2 - PHASE_LABELS.length * 22 / 2;

         for (int i = 0; i < PHASE_LABELS.length; i++) {
            final int fi = i;
            this.addChrome(Button.builder(Component.literal(PHASE_LABELS[i]), b -> {
               this.previewPhase = fi;
               this.phasePickerOpen = false;
               this.resetPreviewCamera();
               this.rebuild();
            }).bounds(this.width / 2 - 70, top + i * 22, 140, 20).build());
         }

         this.addChrome(Button.builder(Component.literal("Cancel"), b -> {
            this.phasePickerOpen = false;
            this.rebuild();
         }).bounds(this.width / 2 - 50, top + PHASE_LABELS.length * 22 + 8, 100, 20).build());
      } else if (this.subPickerOpen) {
         int top = this.height / 2 - StormModelPreview.SUBPHASE_LABELS.length * 22 / 2;

         for (int i = 0; i < StormModelPreview.SUBPHASE_LABELS.length; i++) {
            final int fi = i;
            String var28 = PHASE_LABELS[Mth.clamp(this.previewPhase, 0, PHASE_LABELS.length - 1)];
            this.addChrome(Button.builder(Component.literal(var28 + StormModelPreview.SUBPHASE_LABELS[i]), b -> {
               this.previewSub = fi;
               this.subPickerOpen = false;
               this.rebuild();
            }).bounds(this.width / 2 - 70, top + i * 22, 140, 20).build());
         }

         this.addChrome(Button.builder(Component.literal("Cancel"), b -> {
            this.subPickerOpen = false;
            this.rebuild();
         }).bounds(this.width / 2 - 50, top + StormModelPreview.SUBPHASE_LABELS.length * 22 + 8, 100, 20).build());
      } else if (this.backdropPickerOpen) {
         int top = this.height / 2 - net.dabicco.witherstormmod.client.PreviewScene.BACKDROP_LABELS.length * 22 / 2;

         for (int i = 0; i < net.dabicco.witherstormmod.client.PreviewScene.BACKDROP_LABELS.length; i++) {
            final int fi = i;
            this.addChrome(Button.builder(Component.literal(net.dabicco.witherstormmod.client.PreviewScene.BACKDROP_LABELS[i]), b -> {
               this.previewBackdrop = fi;
               this.backdropPickerOpen = false;
               this.rebuild();
            }).bounds(this.width / 2 - 70, top + i * 22, 140, 20).build());
         }

         this.addChrome(Button.builder(Component.literal("Cancel"), b -> {
            this.backdropPickerOpen = false;
            this.rebuild();
         }).bounds(this.width / 2 - 50, top + net.dabicco.witherstormmod.client.PreviewScene.BACKDROP_LABELS.length * 22 + 8, 100, 20).build());
      } else if (this.presetPickerOpen) {
         String[] labels = DabyWSClientConfig.PRESET_LABELS;
         int top = this.height / 2 - labels.length * 24 / 2;

         for (int i = 0; i < labels.length; i++) {
            final int fi = i;
            Button option = Button.builder(Component.literal(labels[i]), b -> {
               this.presetSelection = fi;
               this.presetPickerOpen = false;
               this.rebuild();
            }).bounds(this.width / 2 - 90, top + i * 24, 180, 20).build();
            option.active = i != 0;
            this.addChrome(option);
         }

         this.addChrome(Button.builder(Component.literal("Cancel"), b -> {
            this.presetPickerOpen = false;
            this.rebuild();
         }).bounds(this.width / 2 - 50, top + labels.length * 24 + 8, 100, 20).build());
      } else if (this.pendingPreset > 0) {
         this.addChrome(Button.builder(Component.literal("Cancel"), b -> {
            this.pendingPreset = -1;
            this.rebuild();
         }).bounds(this.width / 2 - 102, this.height / 2 + 10, 100, 20).build());
         this.addChrome(Button.builder(Component.literal("Apply Preset"), b -> {
            DabyWSClientConfig.applyPreset(this.pendingPreset, this.presetTabKeys);
            DabyWSClientConfig.save();
            this.setStatus("Applied the " + DabyWSClientConfig.PRESET_LABELS[this.pendingPreset] + " preset", -8591236);
            this.pendingPreset = -1;
            this.rebuild();
         }).bounds(this.width / 2 + 2, this.height / 2 + 10, 100, 20).build());
      } else if (this.confirmOpen) {
         this.addChrome(Button.builder(Component.literal("Discard Changes"), b -> {
            this.confirmOpen = false;
            this.serverTouched = false;
            this.editing.applyArray(net.dabicco.witherstormmod.client.ClientConfigCache.cfg.toArray());
            this.closeForReal();
         }).bounds(this.width / 2 - 102, this.height / 2 + 10, 100, 20).build());
         this.addChrome(Button.builder(Component.literal("Save & Quit"), b -> {
            this.confirmOpen = false;
            this.saveServer();
            this.closeForReal();
         }).bounds(this.width / 2 + 2, this.height / 2 + 10, 100, 20).build());
      } else if (this.worldCreation) {
         int centre = this.panelCentre();
         this.addChrome(
            Button.builder(Component.literal("Reset Defaults"), b -> this.resetServerDefaults()).bounds(centre - 102, this.height - 52, 100, 20).build()
         );
         this.addChrome(Button.builder(Component.literal("Done"), b -> this.onClose()).bounds(centre + 2, this.height - 52, 100, 20).build());
         this.skipCurrentSection = false;
         this.skipCurrentMaster = false;
         this.buildServerRows();
         this.collapseInitialized.add(this.tab);
         int height = 0;

         for (WitherStormConfigScreen.Row row : this.rows) {
            height += row.height();
         }

         this.maxScroll = Math.max(0, height - (this.viewBottom() - this.viewTop()));
         this.scrollOffset[this.tab.ordinal()] = Math.min(this.scrollOffset[this.tab.ordinal()], this.maxScroll);
         this.repositionRows();
      } else {
         int tabsLeft = this.tabsLeft();
         String[] labels = new String[]{"Server", "Client", "Experimental"};

         for (WitherStormConfigScreen.Tab which : WitherStormConfigScreen.Tab.values()) {
            int i = which.ordinal();
            Button button = Button.builder(Component.literal(labels[i]), b -> this.switchTab(which)).bounds(tabsLeft + 90 * i, 32, 86, 20).build();
            button.active = this.tab != which && (which != WitherStormConfigScreen.Tab.SERVER || this.canEditServer());
            if (which == WitherStormConfigScreen.Tab.SERVER) {
               this.serverTabWidget = button;
            }

            this.addChrome(button);
         }

         int barCentre = this.panelCentre();
         if (this.tab == WitherStormConfigScreen.Tab.SERVER) {
            Button save = Button.builder(Component.literal("Save to Server"), b -> this.saveServer())
               .bounds(barCentre - 102, this.height - 52, 100, 20)
               .build();
            save.active = this.inWorld();
            this.addChrome(save);
            this.addChrome(
               Button.builder(Component.literal("Reset Defaults"), b -> this.resetServerDefaults()).bounds(barCentre + 2, this.height - 52, 100, 20).build()
            );
            if (!this.inWorld() && this.statusText.isEmpty()) {
               this.setStatus("Open a world to change server settings", -6381922);
            }
         } else {
            this.addChrome(
               Button.builder(Component.literal("Reset Defaults"), b -> this.resetClientDefaults()).bounds(barCentre - 102, this.height - 52, 98, 20).build()
            );
            Button model = Button.builder(Component.literal(this.previewShown ? "Model: §aON" : "Model: §7OFF"), b -> {
               this.previewShown = !this.previewShown;
               if (!this.previewShown) {
                  this.previewGigantic = false;
               }

               this.rebuild();
            }).bounds(barCentre + 2, this.height - 52, 98, 20).build();
            model.active = this.width >= 448;
            this.addChrome(model);
            Button gigantic = Button.builder(Component.literal(this.previewGigantic ? "Gigantic: §aON" : "Gigantic: §7OFF"), b -> {
               this.previewGigantic = !this.previewGigantic;
               if (this.previewGigantic) {
                  this.previewShown = true;
               }

               this.rebuild();
            }).bounds(barCentre + 104, this.height - 52, 98, 20).build();
            gigantic.active = this.width >= 448;
            this.addChrome(gigantic);
         }

         this.addChrome(Button.builder(Component.literal("Done"), b -> this.onClose()).bounds(barCentre - 102, this.height - 27, 204, 20).build());
         if (this.searchBox == null) {
            this.searchBox = new EditBox(this.font, 0, 0, 204, 20, Component.literal("Find settings"));
            this.searchBox.setMaxLength(64);
            this.searchBox.setValue(this.searchQuery);
            this.searchBox.setHint(Component.literal("Find settings..."));
            this.searchBox.setResponder(text -> {
               if (!text.equals(this.searchQuery)) {
                  this.searchQuery = text;
                  this.rebuild();
               }
            });
            this.searchBox.setFocused(false);
            this.searchBox.setCanLoseFocus(true);
         }

         this.searchBox.setPosition(barCentre - 314, this.height - 27);
         this.addChrome(this.searchBox);
         if (this.previewVisible()) {
            if (this.previewGigantic) {
               int btnW = Math.max(70, Math.min(105, (this.previewWidth() - 28) / 6));
               int bx = this.previewLeft();
               this.addChrome(Button.builder(Component.literal(PHASE_LABELS[Mth.clamp(this.previewPhase, 0, PHASE_LABELS.length - 1)]), b -> {
                  this.phasePickerOpen = true;
                  this.rebuild();
               }).bounds(bx, this.previewBottom() + 4, btnW, 20).build());
               bx += btnW + 4;
               String var10001 = StormModelPreview.SUBPHASE_LABELS[Mth.clamp(this.previewSub, 0, 4)];
               this.addChrome(Button.builder(Component.literal("Sub: " + var10001), b -> {
                  this.subPickerOpen = true;
                  this.rebuild();
               }).bounds(bx, this.previewBottom() + 4, btnW, 20).build());
               this.chrome.get(this.chrome.size() - 1).active = this.previewPhase != 7;
               bx += btnW + 4;
               this.addChrome(Button.builder(Component.literal(net.dabicco.witherstormmod.client.PreviewScene.BACKDROP_LABELS[this.previewBackdrop]), b -> {
                  this.backdropPickerOpen = true;
                  this.rebuild();
               }).bounds(bx, this.previewBottom() + 4, btnW, 20).build());
               bx += btnW + 4;
               this.addChrome(Button.builder(Component.literal(this.previewAutoSpin ? "Spin: §aON" : "Spin: §7OFF"), b -> {
                  this.previewAutoSpin = !this.previewAutoSpin;
                  this.rebuild();
               }).bounds(bx, this.previewBottom() + 4, btnW, 20).build());
               bx += btnW + 4;
               this.addChrome(Button.builder(Component.literal(this.previewBeamMask != 0 ? "Beams: §aON" : "Beams: §7OFF"), b -> {
                  this.previewBeamMask = this.previewBeamMask != 0 ? 0 : 7;
                  this.rebuild();
               }).bounds(bx, this.previewBottom() + 4, btnW, 20).build());
               bx += btnW + 4;
               this.addChrome(Button.builder(Component.literal("Normal View"), b -> {
                  this.previewGigantic = false;
                  this.rebuild();
               }).bounds(bx, this.previewBottom() + 4, btnW, 20).build());
               this.addChrome(new WitherStormConfigScreen.SunSlider(this.previewLeft(), this.previewBottom() + 26, this.previewWidth(), 20, this));
            } else {
               int third = (this.previewWidth() - 8) / 3;
               this.addChrome(Button.builder(Component.literal(PHASE_LABELS[Mth.clamp(this.previewPhase, 0, PHASE_LABELS.length - 1)]), b -> {
                  this.phasePickerOpen = true;
                  this.rebuild();
               }).bounds(this.previewLeft(), this.previewBottom() + 4, third, 20).build());
               String var10001 = StormModelPreview.SUBPHASE_LABELS[Mth.clamp(this.previewSub, 0, 4)];
               this.addChrome(Button.builder(Component.literal("Sub: " + var10001), b -> {
                  this.subPickerOpen = true;
                  this.rebuild();
               }).bounds(this.previewLeft() + third + 4, this.previewBottom() + 4, third, 20).build());
               this.chrome.get(this.chrome.size() - 1).active = this.previewPhase != 7;
               this.addChrome(Button.builder(Component.literal(net.dabicco.witherstormmod.client.PreviewScene.BACKDROP_LABELS[this.previewBackdrop]), b -> {
                  this.backdropPickerOpen = true;
                  this.rebuild();
               }).bounds(this.previewLeft() + (third + 4) * 2, this.previewBottom() + 4, third, 20).build());
               this.addChrome(new WitherStormConfigScreen.SunSlider(this.previewLeft(), this.previewBottom() + 26, this.previewWidth(), 20, this));
            }
         }

         this.skipCurrentSection = false;
         this.skipCurrentMaster = false;
         if (this.tab == WitherStormConfigScreen.Tab.SERVER) {
            this.buildServerRows();
         } else if (this.tab == WitherStormConfigScreen.Tab.EXPERIMENTAL) {
            this.buildExperimentalRows();
         } else {
            this.buildClientRows();
         }

         this.collapseInitialized.add(this.tab);
         int contentHeight = 0;

         for (WitherStormConfigScreen.Row row : this.rows) {
            contentHeight += row.height();
         }

         this.maxScroll = Math.max(0, contentHeight - (this.viewBottom() - this.viewTop()));
         this.scrollOffset[this.tab.ordinal()] = Math.min(this.scrollOffset[this.tab.ordinal()], this.maxScroll);
         this.repositionRows();
      }
   }

   private void rebuild() {
      this.clearWidgets();
      this.init();
   }

   private void addChrome(AbstractWidget widget) {
      this.chrome.add(widget);
      this.addWidget(widget);
   }

   private void switchTab(WitherStormConfigScreen.Tab newTab) {
      if (this.tab != newTab) {
         this.tab = newTab;
         this.statusText = "";
         this.rebuild();
      }
   }

   private void setStatus(String text, int color) {
      this.statusText = text;
      this.statusColor = color;
   }

   private void buildServerRows() {
      this.serverSection("Spawn & Growth", "spawnFreezeSeconds", "phaseRequirementModifier", "phase4Requirement", "phase5Requirement");
      this.serverSection(
         "Movement",
         "stormSpeed",
         "stormStandoff",
         "cruiseAltitude",
         "recoilStrength",
         "phase4TurnSpeed",
         "phase5TurnSpeed",
         "phase58TurnSpeed",
         "phase58DriftStrength"
      );
      this.serverSection(
         "Chasing & Distractions", "orbitStationaryTargets", "chaseSpeed", "chaseInterval", "distractionInterval", "distractionDuration", "distractionRange"
      );
      this.serverSection("Targeting", "targetingMode");
      this.serverSection(
         "Heads & Tractor Beams",
         "headFireInterval",
         "headTargetRange",
         "headForgiveSeconds",
         "beamClusterInterval",
         "beamGroundRadius",
         "beamShutoff",
         "mobPickup",
         "castThroughWater",
         "beamImpactLight",
         "roarRange",
         "beamSoundRange"
      );
      this.serverSection("Tractor Beam Physics", "tractorBeamPullPower", "tractorBeamLiftSpeed");
      this.serverSection(
         "Debris Clusters",
         "clustersTakeLiquids",
         "severedScavenge",
         "severedScavengeInterval",
         "spiralStrength",
         "clusterSpeed",
         "clusterCooldown",
         "absorptionRadius",
         "pickupRangeModifier"
      );
      String[] stageKeys = new String[WitherStormWorldConfig.CLUSTER_STAGES.length];

      for (int i = 0; i < stageKeys.length; i++) {
         stageKeys[i] = WitherStormWorldConfig.CLUSTER_STAGES[i].key();
      }

      this.serverSection("Max Cluster Size by Stage", stageKeys);
      this.serverSection(
         "Debris Vortex & Lightning",
         "debrisTornadoSpeed",
         "debrisDamageMultiplier",
         "superCataclysmLightning",
         "lightningDischargeInterval",
         "lightningDamage"
      );
      this.serverSection("House & Village Destruction", "buildingDestruction", "buildingTearRadius", "buildingTearInterval", "buildingClusterSize");
      this.serverSection("Corruption & Behavior", "mobsFlee", "tentacleAwareness", "witherSickness", "witheredMobs", "witheredMax", "witheredMaxCaves");
      this.serverSection("Atmosphere", "worldDarkening");
      this.serverSection("Formidibomb Aftermath", "postFormidibombChase", "postFormidibombChaseSpeed", "fastGrowthToSixOne", "fastGrowthToSixOneSpeed");
      this.serverSection(
         "The Enderman Siege",
         "endermanSiege",
         "endermanSiegeCount",
         "endermanSiegeSeconds",
         "endermanSiegeDistance",
         "endermanSiegeSlowdown",
         "endermanSiegeTentacleSpeed",
         "endermanSiegeBeamEats"
      );
      this.serverSection("Cave Rumble", "caveRumble", "caveRumbleInterval", "caveRumbleDuration", "caveRumbleIntensity");
      this.serverSection("Nether Scaling", "netherScale", "netherScaleInterval", "netherScaleRandom");
      this.serverSection("New Growth Features", "instantGrowth", "instantGrowthRate", "infinitePhases", "phaseCeiling");
      this.serverSection("Story-Mode Towns", "townNpcPopulation");
      this.serverSection(
         "Tentacle Swoops & Fling Attacks",
         "tentacleSwoopSpeed",
         "tentacleThrowPower",
         "tentacleChompDamage",
         "tentacleSnatchRange",
         "tentacleTargetMobs",
         "tentacleEscapeHits"
      );
      this.serverSection(
         "Tentacle Slams & Shockwaves",
         "tentacleSlam",
         "tentacleSlamInterval",
         "tentacleSlamRadius",
         "groundShakeOnSlam",
         "groundShakeRadius",
         "groundShockwaveParticles"
      );
      this.serverSection("Combat & Damage Multipliers", "bossHealthMultiplier", "bossAttackDamageMultiplier");
      this.serverSection("Death & Berserk", "deathBlast", "deathBlastRadius", "berserk", "berserkHealth", "berserkSlamInterval");
   }

   private void master(String title) {
      if (!this.collapseInitialized.contains(this.tab)) {
         this.collapsed.add(title);
      }

      this.rows.add(WitherStormConfigScreen.Row.header(title, 0));
      this.skipCurrentMaster = this.collapsed.contains(title);
      this.skipCurrentSection = true;
      this.currentMasterTitle = title;
      this.currentSectionTitle = title;
      this.currentMasterKeys = this.masterKeys.computeIfAbsent(title, k -> new ArrayList<>());
   }

   private void header(String title) {
      if (!this.collapseInitialized.contains(this.tab)) {
         this.collapsed.add(title);
      }

      if (this.skipCurrentMaster) {
         this.skipCurrentSection = true;
      } else {
         this.rows.add(WitherStormConfigScreen.Row.header(title, this.currentMasterKeys == null ? 0 : 1));
         this.skipCurrentSection = this.collapsed.contains(title);
      }

      this.currentSectionTitle = title;
   }

   private void serverSection(String title, String... keyNames) {
      this.header(title);

      for (String name : keyNames) {
         Key key = (Key)WitherStormWorldConfig.KEYS.get(name);
         DoubleSupplier get = () -> key.get().applyAsDouble(this.editing);
         DoubleConsumer set = v -> {
            key.set().accept(this.editing, key.clamp(v));
            this.serverTouched = true;
         };

         this.addRowWidget(
            switch (key.widget()) {
               case TOGGLE -> WitherStormConfigScreen.Row.toggle(prettify(name), key.description(), get, set, (Runnable)null);
               case CYCLE -> WitherStormConfigScreen.Row.cycle(prettify(name), key.description(), key.cycleLabels(), get, set, (Runnable)null);
               case SLIDER -> WitherStormConfigScreen.Row.slider(
                  prettify(name),
                  key.description(),
                  key.min(),
                  key.max(),
                  key.integer() ? "%.0f" : (key.max() <= 0.5 ? "%.4f" : "%.2f"),
                  get,
                  set,
                  (Runnable)null
               );
               default -> throw new MatchException((String)null, (Throwable)null);
            }
         );
      }
   }

   private void buildClientRows() {
      this.master("The Storm's Look");
      this.header("Preset");
      if (this.presetSelection < 0) {
         this.presetSelection = (int)Math.round(DabyWSClientConfig.effectsPreset);
      }

      String[] var10001 = DabyWSClientConfig.PRESET_LABELS;
      this.addRowWidget(
         WitherStormConfigScreen.Row.button(
            "Preset: " + var10001[Mth.clamp(this.presetSelection, 0, DabyWSClientConfig.PRESET_LABELS.length - 1)],
            "A whole look in one setting. MCSM is the default. Legacy Java is the older, brighter look. Changing anything a preset covers drops you to Custom.",
            () -> {
               this.presetPickerOpen = true;
               this.rebuild();
            }
         )
      );
      this.addRowWidget(WitherStormConfigScreen.Row.button("Apply Preset", "Asks first: it overwrites every look setting, not just the ones it names.", () -> {
         if (this.presetSelection > 0) {
            this.presetTabKeys.clear();
            this.presetTabKeys.addAll(this.masterKeys.getOrDefault("The Storm's Look", List.of()));
            this.pendingPreset = this.presetSelection;
            this.rebuild();
         } else {
            this.setStatus("Pick MCSM or Legacy Java first. Custom is just your own settings", -16307);
         }
      }));
      this.header("Storm Rendering");
      this.clientRow("stormSkin", "Storm Skin", (BooleanSupplier)null);
      this.clientRow("legacyHeads", "Legacy Heads", (BooleanSupplier)null);
      this.clientRow("distantStorms", "Distant Storms", (BooleanSupplier)null);
      this.clientRow("distantFog", "Distant Storm Haze", (BooleanSupplier)null);
      this.clientRow("clusterVolumetricLighting", "Volumetric Cluster Lighting", (BooleanSupplier)null);
      this.header("Back Growth");
      this.clientRow("filledSubphases", "Filled (Unlimited) Subphases", (BooleanSupplier)null);
      this.clientRow("scaledSubphaseGrowth", "Scaled Subphase Growth", () -> DabyWSClientConfig.isLocked("scaledSubphaseGrowth"));
      this.clientRow("flatbackFlipFix", "Flatback Flip Fix", (BooleanSupplier)null);
      this.header("Shading & Shadows");
      this.clientRow("stormModelShading", "Shade The Storm's Model", (BooleanSupplier)null);
      this.clientRow("reverseShading", "Reverse Shading", (BooleanSupplier)null);
      this.clientRow("stormSelfShadow", "Storm Shades Itself", (BooleanSupplier)null);
      this.clientRow("stormShadingContrast", "Self-Shading Contrast", () -> !DabyWSClientConfig.stormSelfShadow);
      this.clientRow("stormShadow", "Cast A Sun Shadow", (BooleanSupplier)null);
      this.clientRow("stormShadowStrength", "Shadow Darkness", () -> noShadow());
      this.clientRow("stormShadowTerrain", "Terrain Detail", () -> noShadow());
      this.clientRow("stormShadowR", "Shadow Colour: Red", () -> noShadow());
      this.clientRow("stormShadowG", "Shadow Colour: Green", () -> noShadow());
      this.clientRow("stormShadowB", "Shadow Colour: Blue", () -> noShadow());
      this.header("Sky & Fog");
      this.clientRow("skyDarkenIntensity", "Sky Darkening", (BooleanSupplier)null);
      this.clientRow("skyDarkenLighting", "Darken World Lighting", (BooleanSupplier)null);
      this.clientRow("skyDarkenR", "Sky Colour: Red", (BooleanSupplier)null);
      this.clientRow("skyDarkenG", "Sky Colour: Green", (BooleanSupplier)null);
      this.clientRow("skyDarkenB", "Sky Colour: Blue", (BooleanSupplier)null);
      this.clientRow("separateFogColor", "Separate Fog Colour", (BooleanSupplier)null);
      this.clientRow("fogColorR", "Fog Colour: Red", () -> !DabyWSClientConfig.separateFogColor);
      this.clientRow("fogColorG", "Fog Colour: Green", () -> !DabyWSClientConfig.separateFogColor);
      this.clientRow("fogColorB", "Fog Colour: Blue", () -> !DabyWSClientConfig.separateFogColor);
      this.clientRow("stormFog", "Storm Proximity Fog", (BooleanSupplier)null);
      this.clientRow("stormFogStrength", "Storm Fog Strength", () -> !DabyWSClientConfig.stormFog);
      this.clientRow("farLandsHaze", "Far-Lands Haze", (BooleanSupplier)null);
      this.clientRow("farLandsDistance", "Far-Lands Distance", () -> !DabyWSClientConfig.farLandsHaze);
      this.clientRow("farLandsStrength", "Far-Lands Strength", () -> !DabyWSClientConfig.farLandsHaze);
      this.clientRow("biomeFogTint", "Biome-Tinted Storm Fog", (BooleanSupplier)null);
      this.clientRow("biomeFogStrength", "Biome Fog Strength", () -> !DabyWSClientConfig.biomeFogTint);
      this.clientRow("cloudDarkenStrength", "Darken Clouds", (BooleanSupplier)null);
      this.clientRow("cloudColorR", "Cloud Colour: Red", () -> DabyWSClientConfig.cloudDarkenStrength <= 0.0);
      this.clientRow("cloudColorG", "Cloud Colour: Green", () -> DabyWSClientConfig.cloudDarkenStrength <= 0.0);
      this.clientRow("cloudColorB", "Cloud Colour: Blue", () -> DabyWSClientConfig.cloudDarkenStrength <= 0.0);
      this.header("Sun Glow");
      this.clientRow("sunGlow", "Sun Burns Through Gloom", (BooleanSupplier)null);
      this.clientRow("sunGlowStrength", "Glow Strength", () -> !DabyWSClientConfig.sunGlow);
      this.clientRow("sunGlowR", "Glow Colour: Red", () -> !DabyWSClientConfig.sunGlow);
      this.clientRow("sunGlowG", "Glow Colour: Green", () -> !DabyWSClientConfig.sunGlow);
      this.clientRow("sunGlowB", "Glow Colour: Blue", () -> !DabyWSClientConfig.sunGlow);
      this.header("Eye & Teeth Glow");
      this.clientRow("glowStrength", "Glow Strength", (BooleanSupplier)null);
      this.clientRow("eyeColorR", "Glow Colour: Red", (BooleanSupplier)null);
      this.clientRow("eyeColorG", "Glow Colour: Green", (BooleanSupplier)null);
      this.clientRow("eyeColorB", "Glow Colour: Blue", (BooleanSupplier)null);
      this.header("Night Glow");
      this.clientRow("stormGlowStrength", "Silhouette Glow", (BooleanSupplier)null);
      this.header("Bloom");
      this.clientRow("bloomStrength", "Bloom", (BooleanSupplier)null);
      this.clientRow("bloomMaskToStorm", "Bloom Only The Storm", () -> DabyWSClientConfig.bloomStrength <= 0.0);
      this.header("Tractor Beam");
      this.clientRow("beamOpacity", "Beam Opacity", (BooleanSupplier)null);
      this.clientRow("beamEndFade", "Beam End Fade", (BooleanSupplier)null);
      this.clientRow("beamColorR", "Beam Colour: Red", (BooleanSupplier)null);
      this.clientRow("beamColorG", "Beam Colour: Green", (BooleanSupplier)null);
      this.clientRow("beamColorB", "Beam Colour: Blue", (BooleanSupplier)null);
      this.clientRow("beamInnerFaces", "Show Inner Faces", (BooleanSupplier)null);
      this.header("Beam Impact Light");
      this.clientRow("impactLight", "Light The Ground", (BooleanSupplier)null);
      this.clientRow("impactLightSize", "Light Size", () -> !DabyWSClientConfig.impactLight);
      this.clientRow("impactLightBrightness", "Light Brightness", () -> !DabyWSClientConfig.impactLight);
      this.clientRow("impactLightRange", "Light Range", () -> !DabyWSClientConfig.impactLight);
      this.clientRow("impactLightUseBeamColor", "Tint With Beam Colour", () -> !DabyWSClientConfig.impactLight);
      this.header("Debris");
      this.clientRow("debrisAmount", "Debris Amount", (BooleanSupplier)null);
      this.clientRow("debrisSize", "Debris Size", (BooleanSupplier)null);
      this.clientRow("devourerDebrisGlow", "Devourer Debris Glow", (BooleanSupplier)null);
      this.master("Sound & Interface");
      this.header("Audio: Storm");
      this.clientRow("stormAmbience", "Storm Ambience", (BooleanSupplier)null);
      this.clientRow("ambienceVolume", "Ambience Volume", () -> !DabyWSClientConfig.stormAmbience);
      this.clientRow("headSoundsVolume", "Head Volume", (BooleanSupplier)null);
      this.header("Audio: Tractor Beam");
      this.clientRow("beamSoundsVolume", "Beam Volume", (BooleanSupplier)null);
      this.clientRow("beamDeactivateSound", "Beam Switch-Off Sound", (BooleanSupplier)null);
      this.clientRow("beamHum", "Beam Hum", (BooleanSupplier)null);
      this.clientRow("beamHumVolume", "Hum Volume", () -> !DabyWSClientConfig.beamHum);
      this.clientRow("beamHumRange", "Hum Range", () -> !DabyWSClientConfig.beamHum);
      this.header("Audio: Infection");
      this.clientRow("infectedMobSound", "Infected Mob Sound", (BooleanSupplier)null);
      this.clientRow("infectedMobSoundVolume", "Infected Mob Volume", () -> !DabyWSClientConfig.infectedMobSound);
      this.header("Boss Bar & Name");
      this.clientRow("bossbarNotched", "Notched Boss Bar", (BooleanSupplier)null);
      this.clientRow("bossbarColor", "Boss Bar Colour", (BooleanSupplier)null);
      this.clientRow("nameStyle", "Name Style", (BooleanSupplier)null);
      this.header("Music");
      this.clientRow("stormMusic", "Storm Music", (BooleanSupplier)null);
      this.clientRow("stormMusicVolume", "Music Volume", () -> !DabyWSClientConfig.stormMusic);
      this.clientRow("stormMusicRange", "Music Range", () -> !DabyWSClientConfig.stormMusic);
      this.clientRow("stormMusicCaveCutoff", "Cave Silence", () -> !DabyWSClientConfig.stormMusic);
      this.master("Skybox & Atmosphere");
      this.header("Skybox Stars");
      this.clientRow("stormStars", "Skybox Stars", (BooleanSupplier)null);
      this.clientRow("starDensity", "Star Density", (BooleanSupplier)null);
      this.clientRow("starTwinkleSpeed", "Twinkle Speed", (BooleanSupplier)null);
      this.clientRow("starBrightness", "Star Brightness", (BooleanSupplier)null);
      this.header("MCSM Cloud Deck");
      this.clientRow("stormCloudDeck", "Cloud Deck", (BooleanSupplier)null);
      this.clientRow("stormCloudCoverage", "Deck Coverage", (BooleanSupplier)null);
      this.clientRow("stormCloudAltitude", "Deck Altitude", (BooleanSupplier)null);
      this.clientRow("stormCloudPaletteMix", "Deck Palette Mix", (BooleanSupplier)null);
      this.header("The Pulse");
      this.clientRow("atmospherePulse", "Atmospheric Pulse", (BooleanSupplier)null);
      this.clientRow("pulseStrength", "Pulse Strength", (BooleanSupplier)null);
      this.clientRow("pulsePeriod", "Pulse Period", (BooleanSupplier)null);
      this.clientRow("pulseSize", "Pulse Reach", (BooleanSupplier)null);
      this.clientRow("pulseHeartbeat", "Pulse Heartbeat", (BooleanSupplier)null);
      this.clientRow("pulseHeartbeatVolume", "Heartbeat Volume", () -> !DabyWSClientConfig.pulseHeartbeat);
      this.clientRow("pulseHeartbeatRange", "Heartbeat Range", () -> !DabyWSClientConfig.pulseHeartbeat);
      this.header("Storm Backdrop (the sky behind the storm)");
      this.clientRow("stormBackdrop", "Storm Backdrop", (BooleanSupplier)null);
      this.clientRow("stormBackdropStrength", "Backdrop Opacity", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropSize", "Backdrop Size", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropPulse", "Backdrop Breathing", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropGrow", "Grow With Storm", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropBlack", "Central Black Blur", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropBlackStrength", "Black Blur Strength", () -> !DabyWSClientConfig.stormBackdrop || !DabyWSClientConfig.stormBackdropBlack);
      this.clientRow("stormBackdropPhase4", "Phase 4 Blue Glow", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropPhase4Strength", "Phase 4 Brightness", () -> !DabyWSClientConfig.stormBackdrop || !DabyWSClientConfig.stormBackdropPhase4);
      this.clientRow("stormBackdropTurquoise", "Phase 4.5 Turquoise", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropPurple", "Phase 5.1 Purple", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropPink", "Phase 5.5 Purple + Pink", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropEmber", "Ember Tint", () -> !DabyWSClientConfig.stormBackdrop);
      this.clientRow("stormBackdropEmberStrength", "Ember Strength", () -> !DabyWSClientConfig.stormBackdrop || !DabyWSClientConfig.stormBackdropEmber);
      this.header("Story Mode: Per-Phase Sky");
      this.clientRow("phaseSky45Enabled", "Phase 4.5 Sky", (BooleanSupplier)null);
      this.clientRow("phaseSky45R", "Phase 4.5 Sky R", (BooleanSupplier)null);
      this.clientRow("phaseSky45G", "Phase 4.5 Sky G", (BooleanSupplier)null);
      this.clientRow("phaseSky45B", "Phase 4.5 Sky B", (BooleanSupplier)null);
      this.clientRow("phaseSky50Enabled", "Phase 5 Sky", (BooleanSupplier)null);
      this.clientRow("phaseSky50R", "Phase 5 Sky R", (BooleanSupplier)null);
      this.clientRow("phaseSky50G", "Phase 5 Sky G", (BooleanSupplier)null);
      this.clientRow("phaseSky50B", "Phase 5 Sky B", (BooleanSupplier)null);
      this.clientRow("phaseSky60Enabled", "Phase 6 Sky", (BooleanSupplier)null);
      this.clientRow("phaseSky60R", "Phase 6 Sky R", (BooleanSupplier)null);
      this.clientRow("phaseSky60G", "Phase 6 Sky G", (BooleanSupplier)null);
      this.clientRow("phaseSky60B", "Phase 6 Sky B", (BooleanSupplier)null);
      this.clientRow("phaseSky65Enabled", "Phase 6.5 Sky", (BooleanSupplier)null);
      this.clientRow("phaseSky65R", "Phase 6.5 Sky R", (BooleanSupplier)null);
      this.clientRow("phaseSky65G", "Phase 6.5 Sky G", (BooleanSupplier)null);
      this.clientRow("phaseSky65B", "Phase 6.5 Sky B", (BooleanSupplier)null);
      this.clientRow("phaseSky70Enabled", "Phase 7 Sky", (BooleanSupplier)null);
      this.clientRow("phaseSky70R", "Phase 7 Sky R", (BooleanSupplier)null);
      this.clientRow("phaseSky70G", "Phase 7 Sky G", (BooleanSupplier)null);
      this.clientRow("phaseSky70B", "Phase 7 Sky B", (BooleanSupplier)null);
      this.clientRow("phaseSkyRedEnabled", "Embedded Red", (BooleanSupplier)null);
      this.clientRow("phaseSkyRedR", "Embedded Red R", (BooleanSupplier)null);
      this.clientRow("phaseSkyRedG", "Embedded Red G", (BooleanSupplier)null);
      this.clientRow("phaseSkyRedB", "Embedded Red B", (BooleanSupplier)null);
      this.clientRow("todSkyDayEnabled", "Day Sky", (BooleanSupplier)null);
      this.clientRow("todSkyDayR", "Day Sky R", (BooleanSupplier)null);
      this.clientRow("todSkyDayG", "Day Sky G", (BooleanSupplier)null);
      this.clientRow("todSkyDayB", "Day Sky B", (BooleanSupplier)null);
      this.clientRow("todSkyDuskEnabled", "Dusk Sky", (BooleanSupplier)null);
      this.clientRow("todSkyDuskR", "Dusk Sky R", (BooleanSupplier)null);
      this.clientRow("todSkyDuskG", "Dusk Sky G", (BooleanSupplier)null);
      this.clientRow("todSkyDuskB", "Dusk Sky B", (BooleanSupplier)null);
      this.clientRow("todSkyNightEnabled", "Night Sky", (BooleanSupplier)null);
      this.clientRow("todSkyNightR", "Night Sky R", (BooleanSupplier)null);
      this.clientRow("todSkyNightG", "Night Sky G", (BooleanSupplier)null);
      this.clientRow("todSkyNightB", "Night Sky B", (BooleanSupplier)null);
      this.clientRow("todSkyDawnEnabled", "Dawn Sky", (BooleanSupplier)null);
      this.clientRow("todSkyDawnR", "Dawn Sky R", (BooleanSupplier)null);
      this.clientRow("todSkyDawnG", "Dawn Sky G", (BooleanSupplier)null);
      this.clientRow("todSkyDawnB", "Dawn Sky B", (BooleanSupplier)null);
      this.clientRow("todHorizonDayEnabled", "Day Horizon", (BooleanSupplier)null);
      this.clientRow("todHorizonDayR", "Day Horizon R", (BooleanSupplier)null);
      this.clientRow("todHorizonDayG", "Day Horizon G", (BooleanSupplier)null);
      this.clientRow("todHorizonDayB", "Day Horizon B", (BooleanSupplier)null);
      this.clientRow("todHorizonDuskEnabled", "Dusk Horizon", (BooleanSupplier)null);
      this.clientRow("todHorizonDuskR", "Dusk Horizon R", (BooleanSupplier)null);
      this.clientRow("todHorizonDuskG", "Dusk Horizon G", (BooleanSupplier)null);
      this.clientRow("todHorizonDuskB", "Dusk Horizon B", (BooleanSupplier)null);
      this.clientRow("todHorizonNightEnabled", "Night Horizon", (BooleanSupplier)null);
      this.clientRow("todHorizonNightR", "Night Horizon R", (BooleanSupplier)null);
      this.clientRow("todHorizonNightG", "Night Horizon G", (BooleanSupplier)null);
      this.clientRow("todHorizonNightB", "Night Horizon B", (BooleanSupplier)null);
      this.clientRow("todHorizonDawnEnabled", "Dawn Horizon", (BooleanSupplier)null);
      this.clientRow("todHorizonDawnR", "Dawn Horizon R", (BooleanSupplier)null);
      this.clientRow("todHorizonDawnG", "Dawn Horizon G", (BooleanSupplier)null);
      this.clientRow("todHorizonDawnB", "Dawn Horizon B", (BooleanSupplier)null);
      this.clientRow("todCloudDayEnabled", "Day Clouds", (BooleanSupplier)null);
      this.clientRow("todCloudDayR", "Day Clouds R", (BooleanSupplier)null);
      this.clientRow("todCloudDayG", "Day Clouds G", (BooleanSupplier)null);
      this.clientRow("todCloudDayB", "Day Clouds B", (BooleanSupplier)null);
      this.clientRow("todCloudDuskEnabled", "Dusk Clouds", (BooleanSupplier)null);
      this.clientRow("todCloudDuskR", "Dusk Clouds R", (BooleanSupplier)null);
      this.clientRow("todCloudDuskG", "Dusk Clouds G", (BooleanSupplier)null);
      this.clientRow("todCloudDuskB", "Dusk Clouds B", (BooleanSupplier)null);
      this.clientRow("todCloudNightEnabled", "Night Clouds", (BooleanSupplier)null);
      this.clientRow("todCloudNightR", "Night Clouds R", (BooleanSupplier)null);
      this.clientRow("todCloudNightG", "Night Clouds G", (BooleanSupplier)null);
      this.clientRow("todCloudNightB", "Night Clouds B", (BooleanSupplier)null);
      this.clientRow("todCloudDawnEnabled", "Dawn Clouds", (BooleanSupplier)null);
      this.clientRow("todCloudDawnR", "Dawn Clouds R", (BooleanSupplier)null);
      this.clientRow("todCloudDawnG", "Dawn Clouds G", (BooleanSupplier)null);
      this.clientRow("todCloudDawnB", "Dawn Clouds B", (BooleanSupplier)null);
      this.clientRow("todLightDayEnabled", "Day Light", (BooleanSupplier)null);
      this.clientRow("todLightDayR", "Day Light R", (BooleanSupplier)null);
      this.clientRow("todLightDayG", "Day Light G", (BooleanSupplier)null);
      this.clientRow("todLightDayB", "Day Light B", (BooleanSupplier)null);
      this.clientRow("todLightDuskEnabled", "Dusk Light", (BooleanSupplier)null);
      this.clientRow("todLightDuskR", "Dusk Light R", (BooleanSupplier)null);
      this.clientRow("todLightDuskG", "Dusk Light G", (BooleanSupplier)null);
      this.clientRow("todLightDuskB", "Dusk Light B", (BooleanSupplier)null);
      this.clientRow("todLightNightEnabled", "Night Light", (BooleanSupplier)null);
      this.clientRow("todLightNightR", "Night Light R", (BooleanSupplier)null);
      this.clientRow("todLightNightG", "Night Light G", (BooleanSupplier)null);
      this.clientRow("todLightNightB", "Night Light B", (BooleanSupplier)null);
      this.clientRow("todLightDawnEnabled", "Dawn Light", (BooleanSupplier)null);
      this.clientRow("todLightDawnR", "Dawn Light R", (BooleanSupplier)null);
      this.clientRow("todLightDawnG", "Dawn Light G", (BooleanSupplier)null);
      this.clientRow("todLightDawnB", "Dawn Light B", (BooleanSupplier)null);
      this.clientRow("biomeSkyPlainsEnabled", "Plains Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkyPlainsR", "Plains Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkyPlainsG", "Plains Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkyPlainsB", "Plains Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkyDesertEnabled", "Desert Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkyDesertR", "Desert Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkyDesertG", "Desert Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkyDesertB", "Desert Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkySnowyEnabled", "Snowy Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkySnowyR", "Snowy Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkySnowyG", "Snowy Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkySnowyB", "Snowy Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkySwampEnabled", "Swamp Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkySwampR", "Swamp Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkySwampG", "Swamp Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkySwampB", "Swamp Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkyJungleEnabled", "Jungle Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkyJungleR", "Jungle Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkyJungleG", "Jungle Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkyJungleB", "Jungle Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkySavannaEnabled", "Savanna Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkySavannaR", "Savanna Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkySavannaG", "Savanna Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkySavannaB", "Savanna Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkyBadlandsEnabled", "Badlands Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkyBadlandsR", "Badlands Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkyBadlandsG", "Badlands Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkyBadlandsB", "Badlands Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkyOceanEnabled", "Ocean Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkyOceanR", "Ocean Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkyOceanG", "Ocean Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkyOceanB", "Ocean Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkyMushroomEnabled", "Mushroom Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkyMushroomR", "Mushroom Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkyMushroomG", "Mushroom Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkyMushroomB", "Mushroom Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkyNetherEnabled", "Nether Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkyNetherR", "Nether Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkyNetherG", "Nether Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkyNetherB", "Nether Sky B", (BooleanSupplier)null);
      this.clientRow("biomeSkyEndEnabled", "End Sky", (BooleanSupplier)null);
      this.clientRow("biomeSkyEndR", "End Sky R", (BooleanSupplier)null);
      this.clientRow("biomeSkyEndG", "End Sky G", (BooleanSupplier)null);
      this.clientRow("biomeSkyEndB", "End Sky B", (BooleanSupplier)null);
      this.header("Story Mode: Per-Phase Fog");
      this.clientRow("phaseFog45", "Phase 4.5 Fog Density", (BooleanSupplier)null);
      this.clientRow("phaseFog50", "Phase 5 Fog Density", (BooleanSupplier)null);
      this.clientRow("phaseFog60", "Phase 6 Fog Density", (BooleanSupplier)null);
      this.clientRow("phaseFog65", "Phase 6.5 Fog Density", (BooleanSupplier)null);
      this.clientRow("phaseFog70", "Phase 7 Fog Density", (BooleanSupplier)null);
      this.header("Story Mode: Dynamic Sky Behaviour");
      this.clientRow("stormSkyRange", "Sky Reaction Range", (BooleanSupplier)null);
      this.clientRow("stormSkyFalloff", "Sky Falloff", (BooleanSupplier)null);
      this.clientRow("stormSkyCore", "Black Core Size", (BooleanSupplier)null);
      this.clientRow("stormSkySmooth", "Sky Blend Speed", (BooleanSupplier)null);
      this.header("Story Mode Clouds");
      this.clientRow("storyModeClouds", "Flat Story Mode Clouds", (BooleanSupplier)null);
      this.clientRow("storyModeCloudStrength", "Cloud Colour Strength", () -> !DabyWSClientConfig.storyModeClouds);
      this.clientRow("storyModeCloudFade", "Fade Near Storm", (BooleanSupplier)null);
      this.clientRow("storyModeCloudFadeAmount", "Fade Amount", () -> !DabyWSClientConfig.storyModeCloudFade);
      this.header("Story Mode Sky & Lighting");
      this.clientRow("storyModeSky", "Story Mode Sky & Fog", (BooleanSupplier)null);
      this.clientRow("storyModeSkyStrength", "Sky Strength", () -> !DabyWSClientConfig.storyModeSky);
      this.clientRow("storyModeFogStrength", "Fog Tint (keep low)", () -> !DabyWSClientConfig.storyModeSky);
      this.clientRow("storyModeLighting", "Coloured Lighting", (BooleanSupplier)null);
      this.clientRow("storyModeLightingStrength", "Lighting Strength", () -> !DabyWSClientConfig.storyModeLighting);
      this.header("Turquoise Teeth");
      this.clientRow("turquoiseTeeth", "Turquoise Teeth Glow", (BooleanSupplier)null);
      this.clientRow("turquoiseTeethIntensity", "Teeth Brightness", () -> !DabyWSClientConfig.turquoiseTeeth);
      this.header("Cataclysm Halos (Phase 5.8+)");
      this.clientRow("cataclysmHalos", "Cataclysm Halo Pair", (BooleanSupplier)null);
      this.clientRow("haloStrength", "Halo Strength", (BooleanSupplier)null);
      this.header("Black Glare & Ejecta");
      this.clientRow("blackGlare", "Black Glare Ring", (BooleanSupplier)null);
      this.clientRow("blackGlareStrength", "Glare Strength", (BooleanSupplier)null);
      this.clientRow("glareEjecta", "Cluster Ejecta", (BooleanSupplier)null);
      this.clientRow("ejectaRate", "Ejecta Rate", (BooleanSupplier)null);
      this.clientRow("ejectaBrightness", "Ejecta Brightness", (BooleanSupplier)null);
      this.header("Phase Colour Palettes");
      this.clientRow("phaseFogPalettes", "Phase Fog Palettes", (BooleanSupplier)null);
      this.clientRow("paletteStrength", "Palette Strength", (BooleanSupplier)null);
      this.clientRow("turquoiseFogR", "Phase-5 Fog: Red", (BooleanSupplier)null);
      this.clientRow("turquoiseFogG", "Phase-5 Fog: Green", (BooleanSupplier)null);
      this.clientRow("turquoiseFogB", "Phase-5 Fog: Blue", (BooleanSupplier)null);
      this.clientRow("cataclysmFogR", "Phase-5.8 Fog: Red", (BooleanSupplier)null);
      this.clientRow("cataclysmFogG", "Phase-5.8 Fog: Green", (BooleanSupplier)null);
      this.clientRow("cataclysmFogB", "Phase-5.8 Fog: Blue", (BooleanSupplier)null);
      this.header("Cinematic Overlays & HUD");
      this.clientRow("storyModeBossbar", "Story Mode Boss Health Bar", (BooleanSupplier)null);
      this.clientRow("storyModeTitleScreen", "Story Mode Main Menu Theme", (BooleanSupplier)null);
      this.clientRow("stormProximityVignette", "Storm Proximity Dark Vignette", (BooleanSupplier)null);
      this.clientRow("vignetteIntensity", "Vignette Darkness Intensity", () -> !DabyWSClientConfig.stormProximityVignette);
      this.clientRow("sicknessVeinOverlay", "Wither Sickness Creeping Veins", (BooleanSupplier)null);
      this.clientRow("sicknessVeinIntensity", "Vein Overlay Intensity", () -> !DabyWSClientConfig.sicknessVeinOverlay);
      this.clientRow("chromaticGlitchStrength", "Roar Chromatic Shockwave Glitch", (BooleanSupplier)null);
      this.header("Atmospheric FX & Particles");
      this.clientRow("trailerShadows", "Cinematic Trailer Shadow Casting", (BooleanSupplier)null);
      this.clientRow("groundShakingTremors", "Earthquake Camera Tremors", (BooleanSupplier)null);
      this.clientRow("screenTremorIntensity", "Camera Shake Intensity", () -> !DabyWSClientConfig.groundShakingTremors);
      this.clientRow("dynamicScreenShake", "Storm Footstep Ground Vibrations", (BooleanSupplier)null);
      this.clientRow("cloudDeckLayer", "Story Mode Volumetric Cloud Deck", (BooleanSupplier)null);
      this.clientRow("regionalBiomeFog", "Regional Biome Atmospheric Fog", (BooleanSupplier)null);
      this.clientRow("purpleLightningSparks", "Ambient Purple Lightning Spikes", (BooleanSupplier)null);
      this.clientRow("debrisDustParticles", "Debris Dust & Splinter Particles", (BooleanSupplier)null);
      this.clientRow("headEyeGlow", "Colossal Head Eye Lens Flare Glow", (BooleanSupplier)null);
      this.clientRow("volumetricFogDensity", "Volumetric Mist & Haze Density", (BooleanSupplier)null);
      this.buildPerformanceRows();
   }

   private static boolean noShadow() {
      return !DabyWSClientConfig.stormShadow && !DabyWSClientConfig.stormSelfShadow;
   }

   private void buildExperimentalRows() {
      this.master("Animation & Motion Maths");
      this.header("Per-Phase Animation");
      this.clientRow("phaseAnim", "Enable Per-Phase Animation", (BooleanSupplier)null);
      this.clientRow("phaseAnimStrength", "Profile Strength", () -> !DabyWSClientConfig.phaseAnim);
      this.header("Tentacle Motion");
      this.clientRow("tentacleIdleSpeed", "Idle Speed", (BooleanSupplier)null);
      this.clientRow("tentacleWaveTravel", "Wave Travel", (BooleanSupplier)null);
      this.clientRow("tentacleCurlDepth", "Small: Curl Depth", (BooleanSupplier)null);
      this.clientRow("tentacleCrossAxis", "Small: Second Axis", (BooleanSupplier)null);
      this.clientRow("bigTentacleCurlDepth", "Big: Curl Depth", (BooleanSupplier)null);
      this.clientRow("bigTentacleHangBreath", "Big: Hang Breathing", (BooleanSupplier)null);
      this.clientRow("bigTentacleSideSweep", "Big: Side Sweep", (BooleanSupplier)null);
      this.clientRow("lateGrowthWrithe", "Phase 5.8 Speed-Up", (BooleanSupplier)null);
      this.header("Tentacle Physics (Verlet)");
      this.clientRow("tentaclePhysics", "Simulate Tentacles", (BooleanSupplier)null);
      this.clientRow("verletGravity", "Gravity", () -> !DabyWSClientConfig.tentaclePhysics);
      this.clientRow("verletSway", "Motion Reaction", () -> !DabyWSClientConfig.tentaclePhysics);
      this.clientRow("verletDamping", "Damping", () -> !DabyWSClientConfig.tentaclePhysics);
      this.clientRow("verletWrithe", "Writhe Strength", () -> !DabyWSClientConfig.tentaclePhysics);
      this.clientRow("verletWritheSpeed", "Writhe Speed", () -> !DabyWSClientConfig.tentaclePhysics);
      this.header("Body Rotation");
      this.clientRow("yawSmoothTime", "Yaw Settle Time", (BooleanSupplier)null);
      this.clientRow("yawSnapDegrees", "Yaw Snap Threshold", (BooleanSupplier)null);
      this.clientRow("bodyBankGain", "Bank Into Turns", (BooleanSupplier)null);
      this.clientRow("bodyLeanGain", "Lean Into Travel", (BooleanSupplier)null);
      this.header("Growth & Transformation");
      this.clientRow("growthSmoothRate", "Back Growth Smoothing", (BooleanSupplier)null);
      this.clientRow("changeoverShake", "Changeover Rattle", (BooleanSupplier)null);
      this.header("Head Motion");
      this.clientRow("jawLagGain", "Jaw Trail", (BooleanSupplier)null);
      this.clientRow("jawLagMax", "Jaw Trail Limit", (BooleanSupplier)null);
      this.clientRow("jawLagCatchup", "Jaw Catch-Up", (BooleanSupplier)null);
      this.master("Rendering Internals");
      this.header("Distant Storms");
      this.clientRow("legacyDistantRenderer", "Legacy Distant Renderer", (BooleanSupplier)null);
      this.clientRow("optimizeDistantAnimations", "Tick-Rate Distant Animation", (BooleanSupplier)null);
      this.header("Diagnostics");
      this.clientRow("stormGlowFlip", "Invert Storm Glow", (BooleanSupplier)null);
      this.clientRow("bloomDebug", "Bloom Debug View", (BooleanSupplier)null);
      this.clientRow("stormRenderStats", "Log Render Stats", (BooleanSupplier)null);
      this.clientRow("bowelsFrameHud", "Bowels Frame Readout", (BooleanSupplier)null);
   }

   private void buildPerformanceRows() {
      this.master("The Sun Shadow");
      this.header("Shadow Cost");
      this.clientRow("shadowMapResolution", "Shadow Map Resolution", () -> noShadow());
      this.clientRow("stormShadowSoftEdge", "Soft Shadow Edge", () -> noShadow());
      this.clientRow("shadowCullBackFaces", "Cull Shadow Back Faces", () -> noShadow());
      this.clientRow("stormShadowHeightmap", "Heightmap-Only Ground", () -> noShadow());
      this.master("Geometry Thinning");
      this.header("Geometry");
      this.clientRow("stormBackfaceCull", "Cull The Storm's Back Faces", (BooleanSupplier)null);
      this.clientRow("mirrorBackDetail", "Back-Fill Detail", () -> !DabyWSClientConfig.flatbackFlipFix);
   }

   private void clientRow(String name) {
      this.clientRow(name, (String)null, (BooleanSupplier)null);
   }

   private void clientRow(String name, String title, BooleanSupplier locked) {
      net.dabicco.witherstormmod.config.DabyWSClientConfig.Key key = (net.dabicco.witherstormmod.config.DabyWSClientConfig.Key)DabyWSClientConfig.KEYS
         .get(name);
      this.tabKeys.add(name);
      if (this.currentMasterKeys != null) {
         this.currentMasterKeys.add(name);
      }

      DoubleSupplier get = () -> key.get().getAsDouble();
      String label = title != null ? title : prettify(name);
      WitherStormConfigScreen.Row row;
      if (key.cycle()) {
         row = WitherStormConfigScreen.Row.cycle(label, key.description(), key.cycleLabels(), get, v -> {
            key.set().accept(v);
            this.presetTouched(name);
         }, DabyWSClientConfig::save);
      } else if (key.toggle()) {
         row = WitherStormConfigScreen.Row.toggle(label, key.description(), get, v -> {
            key.set().accept(v);
            this.presetTouched(name);
         }, DabyWSClientConfig::save);
      } else {
         row = WitherStormConfigScreen.Row.slider(
            label, key.description(), key.min(), key.max(), key.max() >= 100.0 ? "%.0f" : (key.max() <= 0.5 ? "%.3f" : "%.2f"), get, v -> {
               key.set().accept(key.clamp(v));
               this.presetTouched(name);
            }, DabyWSClientConfig::save
         );
      }

      row.locked = locked;
      this.addRowWidget(row);
   }

   private void presetTouched(String name) {
      if (DabyWSClientConfig.isPresetKey(name)) {
         DabyWSClientConfig.refreshPreset();
      }
   }

   private void addRowWidget(WitherStormConfigScreen.Row row) {
      if (this.searching() ? this.rowMatchesSearch(row) : !this.skipCurrentMaster && !this.skipCurrentSection) {
         this.rows.add(row);
         int indent = this.currentMasterKeys == null ? 0 : 10;
         row.createWidget(this.rowX() + indent, this.rowWidth() - indent);
         this.addWidget(row.widget);
      }
   }

   private boolean searching() {
      return !this.searchQuery.isBlank();
   }

   private boolean rowMatchesSearch(WitherStormConfigScreen.Row row) {
      String q = this.searchQuery.toLowerCase(Locale.ROOT);
      if (row.label != null && row.label.toLowerCase(Locale.ROOT).contains(q)) {
         return true;
      } else if (row.desc != null && row.desc.toLowerCase(Locale.ROOT).contains(q)) {
         return true;
      } else {
         return this.currentSectionTitle != null && this.currentSectionTitle.toLowerCase(Locale.ROOT).contains(q)
            ? true
            : this.currentMasterTitle != null && this.currentMasterTitle.toLowerCase(Locale.ROOT).contains(q);
      }
   }

   private static String prettify(String key) {
      StringBuilder out = new StringBuilder();

      for (int i = 0; i < key.length(); i++) {
         char c = key.charAt(i);
         if (i == 0) {
            out.append(Character.toUpperCase(c));
         } else {
            if (Character.isUpperCase(c)) {
               out.append(' ');
            }

            out.append(c);
         }
      }

      return out.toString();
   }

   private void saveServer() {
      if (this.inWorld() && ClientPlayNetworking.canSend(UpdateWitherStormConfigPayload.TYPE)) {
         ClientPlayNetworking.send(new UpdateWitherStormConfigPayload(this.editing.toArray()));
         net.dabicco.witherstormmod.client.ClientConfigCache.cfg.applyArray(this.editing.toArray());
         this.serverTouched = false;
         this.setStatus("Saved and synced to everyone in this world", -8591236);
      } else {
         this.setStatus("Not connected to a world", -37266);
      }
   }

   private void resetServerDefaults() {
      this.editing.applyArray(new WitherStormWorldConfig().toArray());
      this.serverTouched = true;
      this.setStatus(this.worldCreation ? "Reset to defaults" : "Reset to defaults (not saved yet)", -16307);
      this.rebuild();
   }

   private void resetClientDefaults() {
      DabyWSClientConfig.resetDefaults(this.tabKeys);
      DabyWSClientConfig.save();
      this.setStatus((this.tab == WitherStormConfigScreen.Tab.EXPERIMENTAL ? "Experimental" : "Client") + " settings reset to defaults", -8591236);
      this.rebuild();
   }

   public void onServerConfigSynced() {
      if (!this.serverTouched) {
         this.editing.applyArray(net.dabicco.witherstormmod.client.ClientConfigCache.cfg.toArray());
         if (this.tab == WitherStormConfigScreen.Tab.SERVER) {
            this.rebuild();
         }
      }
   }

   private void repositionRows() {
      int y = this.viewTop() - this.scrollOffset[this.tab.ordinal()];

      for (WitherStormConfigScreen.Row row : this.rows) {
         row.y = y;
         if (row.widget != null) {
            row.widget.setY(y);
            int bottom = y + row.widget.getHeight();
            boolean anyVisible = bottom > this.viewTop() && y < this.viewBottom();
            boolean clickable = y >= this.viewTop() - 6 && bottom <= this.viewBottom() + 6;
            boolean locked = row.locked != null && row.locked.getAsBoolean();
            row.widget.visible = anyVisible;
            row.widget.active = anyVisible && clickable && !locked;
            if (row.toggle && row.widget instanceof Button b) {
               b.setMessage(row.toggleMessage());
            }
         }

         y += row.height();
      }
   }

   private void setScroll(int value) {
      this.scrollOffset[this.tab.ordinal()] = Math.max(0, Math.min(value, this.maxScroll));
      this.repositionRows();
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.overPreview(mouseX, mouseY)) {
         this.previewZoom = Mth.clamp((float)(this.previewZoom * Math.pow(1.12, scrollY)), 0.35F, 4.0F);
         return true;
      } else if (this.maxScroll > 0) {
         this.setScroll(this.scrollOffset[this.tab.ordinal()] - (int)(scrollY * 18.0));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   private int scrollbarX() {
      return this.panelRight() - 6 - 5;
   }

   private int thumbHeight() {
      int viewH = this.viewBottom() - this.viewTop();
      int contentH = viewH + this.maxScroll;
      return Math.max(24, viewH * viewH / contentH);
   }

   private int thumbY() {
      int viewH = this.viewBottom() - this.viewTop();
      int travel = viewH - this.thumbHeight();
      return this.viewTop() + (this.maxScroll == 0 ? 0 : travel * this.scrollOffset[this.tab.ordinal()] / this.maxScroll);
   }

   private void scrollToMouse(double mouseY) {
      int viewH = this.viewBottom() - this.viewTop();
      int travel = viewH - this.thumbHeight();
      if (travel > 0) {
         double frac = (mouseY - this.viewTop() - this.thumbHeight() / 2.0) / travel;
         this.setScroll((int)Math.round(frac * this.maxScroll));
      }
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      if (!this.overPreview(event.x(), event.y()) || event.button() != 0 && event.button() != 1) {
         if (event.button() == 0
            && this.maxScroll > 0
            && event.x() >= this.scrollbarX() - 2
            && event.x() <= this.scrollbarX() + 6 + 2
            && event.y() >= this.viewTop()
            && event.y() <= this.viewBottom()) {
            this.draggingScrollbar = true;
            this.scrollToMouse(event.y());
            return true;
         } else {
            if (event.button() == 0
               && event.x() >= this.rowX()
               && event.x() <= this.rowX() + this.rowWidth()
               && event.y() >= this.viewTop()
               && event.y() <= this.viewBottom()) {
               String hit = null;

               for (WitherStormConfigScreen.Row row : this.rows) {
                  if (row.widget == null && event.y() >= row.y && event.y() < row.y + 20) {
                     hit = row.label;
                     break;
                  }
               }

               if (hit != null) {
                  if (!this.collapsed.remove(hit)) {
                     this.collapsed.add(hit);
                  }

                  this.rebuild();
                  this.setScroll(this.scrollOffset[this.tab.ordinal()]);
                  return true;
               }
            }

            return super.mouseClicked(event, doubled);
         }
      } else {
         if (doubled) {
            this.resetPreviewCamera();
         } else {
            this.previewDragButton = event.button();
            this.previewDragMoved = false;
            if (this.previewAutoSpin) {
               this.previewYaw = StormModelPreview.autoSpinDegrees();
               this.previewYawTarget = this.previewYaw;
               this.previewAutoSpin = false;
            }
         }

         return true;
      }
   }

   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      if (this.previewDragButton >= 0 && (Math.abs(dragX) > 0.5 || Math.abs(dragY) > 0.5)) {
         this.previewDragMoved = true;
      }

      if (this.previewDragButton == 0) {
         this.previewYaw = Mth.wrapDegrees(this.previewYaw - (float)dragX * 0.7F);
         this.previewYawTarget = this.previewYaw;
         this.previewPitch = Mth.clamp(this.previewPitch + (float)dragY * 0.4F, -25.0F, 80.0F);
         return true;
      } else if (this.previewDragButton == 1) {
         float per = 1.0F / Math.max(1, this.previewBottom() - 58);
         this.previewPanX = Mth.clamp(this.previewPanX + (float)dragX * per / this.previewZoom, -1.5F, 1.5F);
         this.previewPanY = Mth.clamp(this.previewPanY + (float)dragY * per / this.previewZoom, -1.5F, 1.5F);
         return true;
      } else if (this.draggingScrollbar) {
         this.scrollToMouse(event.y());
         return true;
      } else {
         return super.mouseDragged(event, dragX, dragY);
      }
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      this.draggingScrollbar = false;
      if (this.previewDragButton == 0 && !this.previewDragMoved) {
         this.previewClick(event.x(), event.y());
      }

      this.previewDragButton = -1;
      return super.mouseReleased(event);
   }

   private void previewClick(double mouseX, double mouseY) {
      int left = this.previewLeft();
      int right = this.previewRight();
      int bottom = this.previewBottom();
      StormModelPreview.View v = this.previewView();
      float facing = this.previewFacing();
      int heads = StormModelPreview.headCount(this.previewPhase, this.previewSub);

      for (int i = 0; i < heads; i++) {
         Vec3 pos = StormModelPreview.headPosition(this.previewPhase, this.previewSub, facing, i);
         float[] at = StormModelPreview.project(v, left, 58, right, bottom, pos.x, pos.y, pos.z);
         double dx = at[0] - mouseX;
         double dy = at[1] - mouseY;
         if (dx * dx + dy * dy <= 484.0) {
            this.previewBeamMask ^= 1 << i;
            return;
         }
      }

      double[] ground = StormModelPreview.groundPoint(v, left, 58, right, bottom, mouseX, mouseY);
      if (ground != null) {
         this.previewYawTarget = StormModelPreview.yawToward(ground[0], ground[1]);
         this.previewAutoSpin = false;
      }
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      if (this.phasePickerOpen) {
         int w2 = this.width / 2;
         int top = this.height / 2 - PHASE_LABELS.length * 22 / 2;
         g.fill(0, 0, this.width, this.height, -1879048192);
         g.fill(w2 - 80, top - 30, w2 + 80, top + PHASE_LABELS.length * 22 + 36, -435352812);
         g.fill(w2 - 81, top - 31, w2 + 81, top - 30, -12964270);
         g.centeredText(this.font, "Show which phase?", w2, top - 22, -16307);

         for (AbstractWidget widget : this.chrome) {
            widget.extractRenderState(g, mouseX, mouseY, partialTick);
         }
      } else if (this.subPickerOpen) {
         int w2 = this.width / 2;
         int top = this.height / 2 - StormModelPreview.SUBPHASE_LABELS.length * 22 / 2;
         g.fill(0, 0, this.width, this.height, -1879048192);
         g.fill(w2 - 80, top - 30, w2 + 80, top + StormModelPreview.SUBPHASE_LABELS.length * 22 + 36, -435352812);
         g.fill(w2 - 81, top - 31, w2 + 81, top - 30, -12964270);
         g.centeredText(this.font, "How far into it?", w2, top - 22, -16307);

         for (AbstractWidget widget : this.chrome) {
            widget.extractRenderState(g, mouseX, mouseY, partialTick);
         }
      } else if (this.backdropPickerOpen) {
         int w2 = this.width / 2;
         int top = this.height / 2 - net.dabicco.witherstormmod.client.PreviewScene.BACKDROP_LABELS.length * 22 / 2;
         g.fill(0, 0, this.width, this.height, -1879048192);
         g.fill(w2 - 80, top - 30, w2 + 80, top + net.dabicco.witherstormmod.client.PreviewScene.BACKDROP_LABELS.length * 22 + 36, -435352812);
         g.fill(w2 - 81, top - 31, w2 + 81, top - 30, -12964270);
         g.centeredText(this.font, "Choose a Skybox Backdrop", w2, top - 22, -16307);

         for (AbstractWidget widget : this.chrome) {
            widget.extractRenderState(g, mouseX, mouseY, partialTick);
         }
      } else if (this.presetPickerOpen) {
         int w2 = this.width / 2;
         int top = this.height / 2 - DabyWSClientConfig.PRESET_LABELS.length * 24 / 2;
         g.fill(0, 0, this.width, this.height, -1879048192);
         g.fill(w2 - 100, top - 30, w2 + 100, top + DabyWSClientConfig.PRESET_LABELS.length * 24 + 36, -435352812);
         g.fill(w2 - 101, top - 31, w2 + 101, top - 30, -12964270);
         g.centeredText(this.font, "Choose a preset", w2, top - 22, -16307);

         for (AbstractWidget widget : this.chrome) {
            widget.extractRenderState(g, mouseX, mouseY, partialTick);
         }
      } else if (this.pendingPreset > 0) {
         int w2 = this.width / 2;
         int h2 = this.height / 2;
         g.fill(0, 0, this.width, this.height, -1879048192);
         g.fill(w2 - 150, h2 - 52, w2 + 150, h2 + 42, -435352812);
         g.fill(w2 - 151, h2 - 53, w2 + 151, h2 - 52, -12964270);
         g.fill(w2 - 151, h2 + 42, w2 + 151, h2 + 43, -12964270);
         g.fill(w2 - 151, h2 - 52, w2 - 150, h2 + 42, -12964270);
         g.fill(w2 + 150, h2 - 52, w2 + 151, h2 + 42, -12964270);
         g.centeredText(this.font, "Apply the " + DabyWSClientConfig.PRESET_LABELS[this.pendingPreset] + " preset?", w2, h2 - 40, -16307);
         g.centeredText(this.font, "Every setting under The Storm's Look goes back to", w2, h2 - 24, -2236963);
         g.centeredText(this.font, "its default, then the preset is laid on top.", w2, h2 - 12, -2236963);
         g.centeredText(this.font, "Sound, boss bar and Experimental are left alone.", w2, h2 + 2, -6381922);

         for (AbstractWidget widget : this.chrome) {
            widget.extractRenderState(g, mouseX, mouseY, partialTick);
         }
      } else if (this.confirmOpen) {
         int w2 = this.width / 2;
         int h2 = this.height / 2;
         g.fill(0, 0, this.width, this.height, -1879048192);
         g.fill(w2 - 140, h2 - 44, w2 + 140, h2 + 42, -435352812);
         g.fill(w2 - 141, h2 - 45, w2 + 141, h2 - 44, -12964270);
         g.fill(w2 - 141, h2 + 42, w2 + 141, h2 + 43, -12964270);
         g.fill(w2 - 141, h2 - 44, w2 - 140, h2 + 42, -12964270);
         g.fill(w2 + 140, h2 - 44, w2 + 141, h2 + 42, -12964270);
         g.centeredText(this.font, "You have unsaved changes!", w2, h2 - 32, -16307);
         g.centeredText(this.font, "Save them to the server before leaving?", w2, h2 - 16, -2236963);

         for (AbstractWidget widget : this.chrome) {
            widget.extractRenderState(g, mouseX, mouseY, partialTick);
         }
      } else if (this.previewGigantic && this.previewVisible()) {
         g.fill(0, 0, this.width, this.height, -16777216);
         this.drawPreview(g);

         for (AbstractWidget widget : this.chrome) {
            widget.extractRenderState(g, mouseX, mouseY, partialTick);
         }
      } else {
         int left = this.panelLeft();
         int right = this.panelRight();
         int bottom = this.panelBottom();
         g.fill(left, 58, right, bottom, -435352812);
         g.fill(left + 1, 59, right - 1, 60, 1075845683);
         g.fill(left - 1, 57, right + 1, 58, -12964270);
         g.fill(left - 1, bottom, right + 1, bottom + 1, -12964270);
         g.fill(left - 1, 58, left, bottom, -12964270);
         g.fill(right, 58, right + 1, bottom, -12964270);
         int centre = this.panelCentre();
         g.centeredText(this.font, this.worldCreation ? "Wither Storm: This World's Settings" : "Dabicco's Wither Storm", centre, 10, -1);
         g.fill(centre - 90, 21, centre + 90, 22, -2130722739);
         if (this.worldCreation) {
            g.centeredText(this.font, "Applied when the world is created", centre, 38, -6381922);
         }

         if (!this.worldCreation) {
            int tabX = this.tabsLeft() + 90 * this.tab.ordinal();
            g.fill(tabX + 2, 52, tabX + 86 - 2, 54, -5011201);
         }

         if (this.previewVisible()) {
            this.drawPreview(g);
         }

         for (AbstractWidget widget : this.chrome) {
            widget.extractRenderState(g, mouseX, mouseY, partialTick);
         }

         g.enableScissor(left + 1, this.viewTop() - 4, right - 1, this.viewBottom() + 4);
         WitherStormConfigScreen.Row hovered = null;

         for (WitherStormConfigScreen.Row row : this.rows) {
            if (row.y + row.height() >= this.viewTop() - 26 && row.y <= this.viewBottom() + 26) {
               if (row.widget == null) {
                  boolean isMaster = row.depth == 0;
                  String var10000 = this.collapsed.contains(row.label) ? "[+] " : "[-] ";
                  String headerText = var10000 + row.label;
                  int x = this.rowX() + 2 + (isMaster ? 0 : 10);
                  int textY = row.y + 6;
                  boolean hoverHeader = mouseX >= this.rowX() && mouseX <= this.rowX() + this.rowWidth() && mouseY >= row.y && mouseY < row.y + 20;
                  int colour = hoverHeader ? -1 : (isMaster ? -5011201 : -16307);
                  g.text(this.font, headerText, x, textY, colour, isMaster);
                  int lineX = x + this.font.width(headerText) + (isMaster ? 9 : 8);
                  g.fill(lineX, row.y + 10, this.rowX() + this.rowWidth(), row.y + 10 + 1, isMaster ? 1437829375 : 872415231);
               } else if (row.widget.visible) {
                  row.widget.extractRenderState(g, mouseX, mouseY, partialTick);
                  if (row.widget.isMouseOver(mouseX, mouseY)) {
                     hovered = row;
                  }
               }
            }
         }

         g.disableScissor();
         if (this.maxScroll > 0) {
            int sx = this.scrollbarX();
            g.fill(sx, this.viewTop(), sx + 6, this.viewBottom(), 1711276032);
            int ty = this.thumbY();
            int th = this.thumbHeight();
            g.fill(sx, ty, sx + 6, ty + th, -10859904);
            g.fill(sx + 1, ty + 1, sx + 6 - 1, ty + 2, -7702856);
         }

         if (!this.statusText.isEmpty()) {
            g.centeredText(this.font, this.statusText, centre, bottom + 4, this.statusColor);
         }

         if (hovered != null && hovered.desc != null && !hovered.desc.isEmpty()) {
            this.wrappedTooltip(g, hovered.desc, mouseX, mouseY);
         }

         if (!this.worldCreation && !this.canEditServer() && this.serverTabWidget != null && this.serverTabWidget.isMouseOver(mouseX, mouseY)) {
            this.wrappedTooltip(
               g, !this.inWorld() ? "You need to be in game for this." : "You need to be an Administrator (OP) to change server settings.", mouseX, mouseY
            );
         }
      }
   }

   private void drawPreview(GuiGraphicsExtractor g) {
      int left = this.previewLeft();
      int right = this.previewRight();
      int bottom = this.previewBottom();
      this.stepPreviewTurn();
      int bands = 14;
      if (this.previewBackdrop == 0) {
         for (int i = 0; i < bands; i++) {
            int y0 = 58 + (bottom - 58) * i / bands;
            int y1 = 58 + (bottom - 58) * (i + 1) / bands;
            g.fill(left, y0, right, y1, lerpColor(-12690298, -7362108, (float)i / (bands - 1)));
         }
      } else {
         g.fill(left, 58, right, bottom, -16777216);
      }

      StormModelPreview.render(
         g,
         left,
         58,
         right,
         bottom,
         this.previewView(),
         this.previewPhase,
         this.previewSub,
         this.previewFacing(),
         this.previewSunAzimuth,
         this.previewBeamMask,
         -7362108,
         this.previewShadow(),
         this.previewBackdrop
      );
      g.fill(left - 1, 57, right + 1, 58, -12964270);
      g.fill(left - 1, bottom, right + 1, bottom + 1, -12964270);
      g.fill(left - 1, 58, left, bottom, -12964270);
      g.fill(right, 58, right + 1, bottom, -12964270);
      if (this.previewGigantic) {
         g.centeredText(
            this.font,
            "§5§lGigantic Preview §7— Left-Drag: Rotate | Right-Drag: Pan | Scroll: Zoom ("
               + String.format("%.1fx", this.previewZoom)
               + ") | Click Heads: Toggle Beams",
            (left + right) / 2,
            40,
            -1
         );
      } else {
         g.centeredText(this.font, "Bloom is fullscreen; not shown here", (left + right) / 2, bottom - 11, -7695448);
      }
   }

   private void drawPreviewSun(GuiGraphicsExtractor g, int left, int right, int bottom) {
      StormModelPreview.View live = this.previewView();
      StormModelPreview.View v = new StormModelPreview.View(live.scale(), live.tx(), live.ty(), 18.0F, live.groundY(), live.field(), live.tall(), live.tile());
      float el = 0.9075712F;
      float az = this.previewSunAzimuth * (float) (Math.PI / 180.0);
      float[] dir = StormModelPreview.project(v, 0, 0, 0, 0, Mth.cos(el) * Mth.cos(az), Mth.sin(el), Mth.cos(el) * Mth.sin(az));
      float dx = dir[0] - v.scale() * v.tx();
      float dy = dir[1] - v.scale() * v.ty();
      float len = Mth.sqrt(dx * dx + dy * dy);
      if (!(len < 1.0E-4F)) {
         float reach = Math.min(right - left, bottom - 58) * 0.36F;
         int size = 30;
         int cx = (left + right) / 2 + Math.round(dx / len * reach);
         int cy = (58 + bottom) / 2 + Math.round(dy / len * reach);
         cx = Mth.clamp(cx, left + size / 2, right - size / 2);
         cy = Mth.clamp(cy, 58 + size / 2, bottom - size / 2);
         g.blit(RenderPipelines.GUI_TEXTURED, SUN, cx - size / 2, cy - size / 2, 0.0F, 0.0F, size, size, 32, 32);
      }
   }

   private static int lerpColor(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int r = (int)Mth.lerp(t, ar, br);
      int gg = (int)Mth.lerp(t, ag, bg);
      int bl = (int)Mth.lerp(t, ab, bb);
      return 0xFF000000 | r << 16 | gg << 8 | bl;
   }

   private void wrappedTooltip(GuiGraphicsExtractor g, String text, int mouseX, int mouseY) {
      int maxWidth = Math.max(140, Math.min(280, this.width - 60));
      g.setTooltipForNextFrame(this.font, this.font.split(Component.literal(text), maxWidth), mouseX, mouseY);
   }

   public void onClose() {
      if (this.worldCreation) {
         PendingWorldConfig.set(this.editing);
         this.closeForReal();
      } else if (!this.confirmOpen && this.tab == WitherStormConfigScreen.Tab.SERVER && this.serverTouched && this.inWorld() && this.canEditServer()) {
         this.confirmOpen = true;
         this.rebuild();
      } else {
         this.closeForReal();
      }
   }

   private void closeForReal() {
      DabyWSClientConfig.save();
      Minecraft.getInstance().gui.setScreen(this.parent);
   }

   private static class ConfigSlider extends AbstractSliderButton {
      private final WitherStormConfigScreen.Row row;

      ConfigSlider(int x, int y, int width, int height, WitherStormConfigScreen.Row row) {
         String var10005 = row.label;
         super(
            x,
            y,
            width,
            height,
            Component.literal(var10005 + ": " + String.format(row.fmt, row.get.getAsDouble())),
            (row.get.getAsDouble() - row.min) / (row.max - row.min)
         );
         this.row = row;
      }

      private double actual() {
         return this.row.min + (this.row.max - this.row.min) * this.value;
      }

      protected void updateMessage() {
         String var10001 = this.row.label;
         this.setMessage(Component.literal(var10001 + ": §e" + String.format(this.row.fmt, this.actual())));
      }

      protected void applyValue() {
         this.row.set.accept(this.actual());
      }

      public void onRelease(MouseButtonEvent event) {
         super.onRelease(event);
         if (this.row.onCommit != null) {
            this.row.onCommit.run();
         }
      }
   }

   private static class Row {
      final String label;
      final String desc;
      final double min;
      final double max;
      final String fmt;
      final DoubleSupplier get;
      final DoubleConsumer set;
      final Runnable onCommit;
      final boolean toggle;
      final String[] cycleLabels;
      int depth;
      AbstractWidget widget;
      Runnable action;
      int y;
      BooleanSupplier locked;

      private Row(
         String label,
         String desc,
         double min,
         double max,
         String fmt,
         DoubleSupplier get,
         DoubleConsumer set,
         Runnable onCommit,
         boolean toggle,
         String[] cycleLabels
      ) {
         this.label = label;
         this.desc = desc;
         this.min = min;
         this.max = max;
         this.fmt = fmt;
         this.get = get;
         this.set = set;
         this.onCommit = onCommit;
         this.toggle = toggle;
         this.cycleLabels = cycleLabels;
      }

      static WitherStormConfigScreen.Row header(String label, int depth) {
         WitherStormConfigScreen.Row row = new WitherStormConfigScreen.Row(
            label, (String)null, 0.0, 0.0, "", (DoubleSupplier)null, (DoubleConsumer)null, (Runnable)null, false, (String[])null
         );
         row.depth = depth;
         return row;
      }

      static WitherStormConfigScreen.Row slider(
         String label, String desc, double min, double max, String fmt, DoubleSupplier get, DoubleConsumer set, Runnable onCommit
      ) {
         return new WitherStormConfigScreen.Row(label, desc, min, max, fmt, get, set, onCommit, false, (String[])null);
      }

      static WitherStormConfigScreen.Row toggle(String label, String desc, DoubleSupplier get, DoubleConsumer set, Runnable onCommit) {
         return new WitherStormConfigScreen.Row(label, desc, 0.0, 1.0, "", get, set, onCommit, true, (String[])null);
      }

      static WitherStormConfigScreen.Row button(String label, String desc, Runnable onClick) {
         WitherStormConfigScreen.Row row = new WitherStormConfigScreen.Row(label, desc, 0.0, 0.0, "", () -> 0.0, v -> {}, onClick, false, (String[])null);
         row.action = onClick;
         return row;
      }

      static WitherStormConfigScreen.Row cycle(String label, String desc, String[] labels, DoubleSupplier get, DoubleConsumer set, Runnable onCommit) {
         return new WitherStormConfigScreen.Row(label, desc, 0.0, labels.length - 1, "", get, set, onCommit, false, labels);
      }

      int height() {
         return this.get == null ? 20 : 26;
      }

      void createWidget(int x, int width) {
         if (this.action != null) {
            this.widget = Button.builder(Component.literal(this.label), b -> this.action.run()).bounds(x, 0, width, 20).build();
         } else if (this.cycleLabels != null) {
            Button button = Button.builder(this.cycleMessage(), b -> {
               int cur = (int)Math.round(this.get.getAsDouble());
               this.set.accept((cur + 1) % this.cycleLabels.length);
               if (this.onCommit != null) {
                  this.onCommit.run();
               }

               b.setMessage(this.cycleMessage());
            }).bounds(x, 0, width, 20).build();
            this.widget = button;
         } else if (this.toggle) {
            Button button = Button.builder(this.toggleMessage(), b -> {
               this.set.accept(this.get.getAsDouble() >= 0.5 ? 0.0 : 1.0);
               if (this.onCommit != null) {
                  this.onCommit.run();
               }

               b.setMessage(this.toggleMessage());
            }).bounds(x, 0, width, 20).build();
            this.widget = button;
         } else {
            this.widget = new WitherStormConfigScreen.ConfigSlider(x, 0, width, 20, this);
         }
      }

      Component toggleMessage() {
         boolean on = this.get.getAsDouble() >= 0.5;
         return Component.literal(this.label + ": " + (on ? "§aON" : "§7OFF"));
      }

      Component cycleMessage() {
         int idx = Mth.clamp((int)Math.round(this.get.getAsDouble()), 0, this.cycleLabels.length - 1);
         return Component.literal(this.label + ": §e" + this.cycleLabels[idx]);
      }
   }

   private static class SunSlider extends AbstractSliderButton {
      private final WitherStormConfigScreen screen;

      SunSlider(int x, int y, int width, int height, WitherStormConfigScreen screen) {
         super(x, y, width, height, Component.empty(), screen.previewSunAzimuth / 360.0);
         this.screen = screen;
         this.updateMessage();
      }

      protected void updateMessage() {
         this.setMessage(Component.literal("Sun: §e" + Math.round(this.value * 360.0) + "°"));
      }

      protected void applyValue() {
         this.screen.previewSunAzimuth = (float)(this.value * 360.0);
      }
   }

   private static enum Tab {
      SERVER,
      CLIENT,
      EXPERIMENTAL;

      private static WitherStormConfigScreen.Tab[] $values() {
         return new WitherStormConfigScreen.Tab[]{SERVER, CLIENT, EXPERIMENTAL};
      }
   }
}
