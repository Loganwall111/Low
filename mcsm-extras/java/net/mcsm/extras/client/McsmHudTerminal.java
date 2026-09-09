package net.mcsm.extras.client;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import net.mcsm.extras.McsmExtrasConfig;

/**
 * Devouring Storms: the Story Mode HUD.
 *
 * The hotbar itself moves to the TOP-LEFT and gets bigger, MCSM-style:
 * nine large slots with real item icons, counts and durability
 * decorations, and a bright selection frame. The vanilla bottom hotbar
 * is cancelled at engine level (McsmHotbarHideMixin on Hud#extractItemHotbar,
 * verified by the CI GUI-surface probe).
 * Beneath the big hotbar sits the holographic terminal panel (build stamp,
 * storm state, position, world time), and while the storm is active the
 * letterbox bars close in like the episode cutscenes.
 *
 * Drawn from the base mod's own HUD element (StormAtmosphereOverlay),
 * attached by McsmHudAttachMixin - all GuiGraphicsExtractor calls used
 * here (fill, text, item, itemDecorations, pose) are verified against the
 * 26.2 client jar by the CI GUI-surface probe.
 */
public final class McsmHudTerminal {

    private static final int SLOT = 26;
    private static final int SLOTS = 9;
    private static final float ICON_SCALE = 1.5F; // 16px icon -> 24px, fills the 26px slot

    // --- MCSM episode card state (client-only, no extra mixin needed) --------
    private static ClientLevel lastLevel;
    private static long cardStart = -1L;
    private static final long CARD_MS = 6500L;

    // --- mega-phase 4: N-flash pulse state (phase 7+) ------------------------
    private static long pulseNextAt = 0L;
    private static long pulseStartAt = 0L;
    private static long lastThumpAt = 0L;
    private static int thumpStep = 0;

    private McsmHudTerminal() {
    }

    public static void paint(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();

        // --- world-join detection: fire the MCSM episode title card ----------
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            cardStart = System.currentTimeMillis();
        }
        if (cardStart > 0L) {
            long t = System.currentTimeMillis() - cardStart;
            if (t > CARD_MS) {
                cardStart = -1L;
            } else {
                paintEpisodeCard(g, w, h, t);
                return; // the card owns the screen while it plays
            }
        }

        float storm = net.dabicco.witherstormmod.client.StormSkyDarken.factor();
        boolean active = storm > 0.04F;

        if (active) {
            int bar = Math.max(14, h / 12);
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }

        // --- MCSM story HUD: vertical inventory rail + top ability callouts ---
        Matrix3x2fStack pose = g.pose();
        int selected = player.getInventory().getSelectedSlot();
        int px = 12;
        int py = Math.max(34, h / 8);
        int railW = SLOT + 8;
        int railH = SLOTS * (SLOT + 3) + 5;

        // Left episode/action rail, matching the reference's stacked slots.
        g.fill(px - 3, py - 4, px + railW + 3, py + railH + 4, 0xAA05060A);
        g.fill(px - 3, py - 4, px - 1, py + railH + 4, 0xFF8195A6);
        g.fill(px + railW + 1, py - 4, px + railW + 3, py + railH + 4, 0xFF2B3543);
        g.fill(px - 3, py - 4, px + railW + 3, py - 2, 0xFFB7C5D8);
        g.fill(px - 3, py + railH + 2, px + railW + 3, py + railH + 4, 0xFF161A21);
        for (int i = 0; i < SLOTS; i++) {
            int sx = px + 4;
            int sy = py + 3 + i * (SLOT + 3);
            int bg = (i == selected) ? 0xDD242A33 : 0x99323841;
            g.fill(sx, sy, sx + SLOT, sy + SLOT, bg);
            g.fill(sx, sy, sx + SLOT, sy + 1, 0x66FFFFFF);
            g.fill(sx, sy, sx + 1, sy + SLOT, 0x66FFFFFF);
            g.fill(sx + SLOT - 1, sy, sx + SLOT, sy + SLOT, 0xAA05060A);
            g.fill(sx, sy + SLOT - 1, sx + SLOT, sy + SLOT, 0xAA05060A);
            if (i == selected) {
                // Cream-white Story Mode selection frame.
                g.fill(sx - 2, sy - 2, sx + SLOT + 2, sy, 0xFFFFF4C9);
                g.fill(sx - 2, sy + SLOT, sx + SLOT + 2, sy + SLOT + 2, 0xFFFFF4C9);
                g.fill(sx - 2, sy, sx, sy + SLOT, 0xFFFFF4C9);
                g.fill(sx + SLOT, sy, sx + SLOT + 2, sy + SLOT, 0xFFFFF4C9);
                g.fill(sx, sy, sx + SLOT, sy + SLOT, 0x226A8FF7);
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                pose.pushMatrix();
                pose.translate(sx + 1, sy + 1);
                pose.scale(ICON_SCALE);
                g.item(stack, 0, 0);
                g.itemDecorations(mc.font, stack, 0, 0);
                pose.popMatrix();
            }
        }

        ItemStack held = player.getInventory().getItem(selected);
        if (!held.isEmpty()) {
            String label = itemName(held);
            int lx = px + railW + 28;
            int ly = py + 3 + selected * (SLOT + 3) + 7;
            int lw = Math.min(150, 10 + mc.font.width(label));
            g.fill(lx - 7, ly - 5, lx + lw, ly + 12, 0x8C07070B);
            g.fill(lx - 7, ly - 5, lx - 4, ly + 12, 0xFFE9EEF8);
            g.text(mc.font, "\u00a7f" + label, lx, ly, 0xFFFFFFFF, true);
        }

        paintEffectCallouts(mc, player, g, w);
        paintStoryActionMeter(mc, player, g, w, h, active);

        // --- tiny diagnostics chip; the big debug panel is intentionally gone
        // so gameplay looks like the screenshot instead of a mod console.
        if (active) {
            String chip = "DS " + McsmExtrasConfig.BUILD_VERSION;
            g.fill(w - 76, 4, w - 4, 17, 0x88040610);
            g.fill(w - 76, 4, w - 74, 17, 0xFF6A8FF7);
            g.text(mc.font, chip, w - 70, 7, 0xFF9FB4D8, false);
        }

        // --- mega-phase 4: the N-flash pulse ---------------------------------
        // Phase 7+: every 30-50 s a 20 s purple gradient slowly appears and
        // disappears OVER the storm, with a heartbeat double-thump, exactly
        // like the story's N-flash beats.
        long nowMs = System.currentTimeMillis();
        boolean late = false;
        try {
            for (net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData d
                    : net.dabicco.witherstormmod.client.ClientDistantStormManager.all()) {
                if (d.phase >= 6.5F) {
                    late = true;
                    break;
                }
            }
        } catch (Throwable ignored) {
            // older base jar without the manager: no pulses, never a crash
        }
        if (late) {
            if (pulseStartAt > 0L) {
                long pt = nowMs - pulseStartAt;
                if (pt > 20000L) {
                    pulseStartAt = 0L;
                    pulseNextAt = nowMs + 30000L + (long) (Math.random() * 20000L);
                } else {
                    float env = (float) Math.sin(Math.PI * (pt / 20000.0D));
                    int aTop = (int) (env * 110.0F);
                    int aBot = aTop / 2;
                    g.fillGradient(0, 0, w, h,
                            (aTop << 24) | 0x6A2AC8, (aBot << 24) | 0x2A0A4A);
                    if (nowMs - lastThumpAt > (thumpStep == 0 ? 1500L : 260L)) {
                        thumpStep = (thumpStep + 1) % 2;
                        lastThumpAt = nowMs;
                        player.playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 0.42F, 0.42F);
                    }
                }
            } else if (nowMs >= pulseNextAt) {
                pulseStartAt = nowMs;
            }
        }

        // --- mega-phase 6b: portal glow + the warp entry sequence -----------
        paintPortal(mc, player, g, w, h);
        paintWarp(mc, player, g, w, h);
    }

    // --- mega-phase 6b state -------------------------------------------------
    private static float portalNear = 0.0F;
    private static float portalYaw = 0.0F;
    private static boolean warpSoundDone = false;
    private static boolean warpReleased = false;

    /**
     * Per-portal coloured bottom glow + coloured dust breathing at the
     * storm mouth (user order: "glowing lights at the bottom casting
     * different-coloured light per portal"). Teal while the plates are
     * still closing, magenta once the mouth is open, gold inside the
     * bowels hallway where the return mouth sits.
     */
    private static void paintPortal(Minecraft mc, LocalPlayer player,
            GuiGraphicsExtractor g, int w, int h) {
        portalNear = 0.0F;
        try {
            boolean inBowels = false;
            try {
                Class<?> bg = Class.forName("net.dabicco.witherstormmod.BowelsGravity");
                Object key = bg.getField("BOWELS").get(null);
                inBowels = player.level().dimension().equals(key);
            } catch (Throwable ignored) {
                // no bowels registry on this jar: overworld behaviour only
            }
            if (inBowels) {
                bottomGlow(g, w, h, 0xFFE666, 0.55F + 0.10F * (float) Math.sin(System.currentTimeMillis() * 0.004D));
                return;
            }
            net.dabicco.witherstormmod.entity.WitherStormEntity storm = null;
            double best = Double.MAX_VALUE;
            for (net.dabicco.witherstormmod.entity.WitherStormEntity e
                    : player.level().getEntitiesOfClass(
                            net.dabicco.witherstormmod.entity.WitherStormEntity.class,
                            player.getBoundingBox().inflate(48.0D))) {
                double d = e.distanceToSqr(player);
                if (d < best) {
                    best = d;
                    storm = e;
                }
            }
            if (storm == null) {
                return;
            }
            net.minecraft.world.phys.Vec3 mouth = mouthOf(storm);
            double d = mouth.distanceTo(player.position());
            if (d > 30.0D) {
                return;
            }
            float near = (float) (1.0D - d / 30.0D);
            portalNear = near;
            double dx = mouth.x - player.position().x;
            double dz = mouth.z - player.position().z;
            portalYaw = (float) Math.toDegrees(Math.atan2(-dx, -dz));
            boolean open = phaseOf(storm) >= 6.9D;
            int col = open ? 0xCC33FF : 0x33FFE6;
            bottomGlow(g, w, h, col, near);
            // coloured dust breathing up from the mouth bottom
            if (System.currentTimeMillis() % 100L < 50L) {
                net.minecraft.core.particles.DustParticleOptions dust =
                        new net.minecraft.core.particles.DustParticleOptions(col | 0xFF000000, 0.8F);
                for (int i = 0; i < 2; i++) {
                    player.level().addParticle(dust,
                            mouth.x + (Math.random() - 0.5) * 4.0D,
                            mouth.y - 2.0D + Math.random() * 1.2D,
                            mouth.z + (Math.random() - 0.5) * 4.0D,
                            0.0D, 0.06D, 0.0D);
                }
            }
        } catch (Throwable ignored) {
            // spectacle only - never crash the HUD
        }
    }

    private static String itemName(ItemStack stack) {
        try {
            Object c = stack.getClass().getMethod("getHoverName").invoke(stack);
            Object txt = c.getClass().getMethod("getString").invoke(c);
            if (txt instanceof String s && !s.isBlank()) {
                return s;
            }
        } catch (Throwable ignored) {
        }
        return "Item";
    }

    private static void paintEffectCallouts(Minecraft mc, LocalPlayer player,
            GuiGraphicsExtractor g, int w) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        try {
            Object effects = player.getClass().getMethod("getActiveEffects").invoke(player);
            if (effects instanceof Iterable<?> it) {
                for (Object effect : it) {
                    String name = effectName(effect);
                    int amp = effectAmplifier(effect) + 1;
                    if (!name.isBlank()) {
                        labels.add("+" + amp + " " + name);
                    }
                    if (labels.size() >= 3) {
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {
            // some loader/API builds hide the collection: no status text then
        }
        if (labels.isEmpty()) {
            return;
        }
        int y = 13;
        int x = w / 2;
        for (int i = 0; i < labels.size(); i++) {
            String text = "\u00a7l" + labels.get(i);
            int yy = y + i * 12;
            int col = i == 0 ? 0xFFFF5656 : (i == 1 ? 0xFFFF8B8B : 0xFF65DBFF);
            g.centeredText(mc.font, text, x, yy, col);
        }
    }

    private static String effectName(Object effect) {
        try {
            Object desc = effect.getClass().getMethod("getDescriptionId").invoke(effect);
            String raw = String.valueOf(desc);
            raw = raw.substring(raw.lastIndexOf('.') + 1).replace('_', ' ');
            if (raw.isBlank()) {
                return "Effect";
            }
            String[] parts = raw.split(" ");
            StringBuilder out = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            return out.toString();
        } catch (Throwable ignored) {
            return "Effect";
        }
    }

    private static int effectAmplifier(Object effect) {
        try {
            Object v = effect.getClass().getMethod("getAmplifier").invoke(effect);
            if (v instanceof Number n) {
                return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static void paintStoryActionMeter(Minecraft mc, LocalPlayer player,
            GuiGraphicsExtractor g, int w, int h, boolean active) {
        // Gold quick-action rails in the lower right, like Story Mode's
        // contextual prompts. They breathe subtly when the storm is active.
        int bars = 4;
        int bw = 46;
        int bh = 6;
        int gap = 3;
        int total = bars * bw + (bars - 1) * gap;
        int x = Math.max(86, w - total - 64);
        int y = h - Math.max(42, h / 12);
        float breath = active ? (0.85F + 0.15F * (float) Math.sin(System.currentTimeMillis() * 0.006D)) : 0.75F;
        int goldA = (int) (255.0F * breath);
        for (int i = 0; i < bars; i++) {
            int bx = x + i * (bw + gap);
            g.fill(bx, y, bx + bw, y + bh, 0x66332A00);
            g.fill(bx, y, bx + bw, y + 1, 0xFFFFF3AA);
            g.fill(bx, y + bh - 1, bx + bw, y + bh, 0xFF9C7614);
            g.fill(bx + 2, y + 2, bx + bw - 2, y + bh - 1, (goldA << 24) | 0xFFE55D);
        }
        int hookX = w - 38;
        int hookY = h - 32;
        int c = active ? 0xFF20D7F2 : 0xFF11889A;
        g.fill(hookX - 8, hookY + 11, hookX + 9, hookY + 14, 0x77000000);
        g.fill(hookX - 11, hookY + 7, hookX - 8, hookY + 12, c);
        g.fill(hookX + 8, hookY + 7, hookX + 11, hookY + 12, c);
        g.fill(hookX - 6, hookY + 12, hookX + 7, hookY + 15, c);
        g.text(mc.font, "↻", hookX - 6, hookY - 1, c, true);
    }

    private static void bottomGlow(GuiGraphicsExtractor g, int w, int h, int col, float near) {
        float s = Math.min(Math.max(near, 0.0F), 1.0F);
        int a1 = (int) (70.0F * s);
        int a2 = (int) (110.0F * s);
        int a3 = (int) (170.0F * s);
        g.fillGradient(0, h - (int) (h * 0.42 * s), w, h, (0 << 24) | col, (a1 << 24) | col);
        g.fillGradient(0, h - (int) (h * 0.24 * s), w, h, (0 << 24) | col, (a2 << 24) | col);
        g.fillGradient(0, h - (int) (h * 0.10 * s), w, h, (a2 << 24) | col, (a3 << 24) | col);
    }

    private static net.minecraft.world.phys.Vec3 mouthOf(
            net.dabicco.witherstormmod.entity.WitherStormEntity storm) {
        try {
            Class<?> bp = Class.forName("net.dabicco.witherstormmod.BowelsPortal");
            Object bb = bp.getMethod("mouth", net.minecraft.world.entity.Entity.class)
                    .invoke(null, storm);
            if (bb instanceof net.minecraft.world.phys.AABB b) {
                return b.getCenter();
            }
        } catch (Throwable ignored) {
            // fall through to the hardcoded mouth offset
        }
        // fromModel(26, -192, -32) at BODY_SCALE, turned by the storm yaw
        double lx = 1.7890D;
        double ly = 8.2580D;
        double lz = 2.2018D;
        double yaw = Math.toRadians(storm.getYRot());
        double c = Math.cos(yaw);
        double sn = Math.sin(yaw);
        return storm.position().add(new net.minecraft.world.phys.Vec3(
                lx * c - lz * sn, ly, lx * sn + lz * c));
    }

    private static double phaseOf(net.dabicco.witherstormmod.entity.WitherStormEntity storm) {
        try {
            Object o = storm.getClass().getMethod("getPhase").invoke(storm);
            if (o instanceof Number n) {
                return n.doubleValue();
            }
        } catch (Throwable ignored) {
            // unreadable phase: assume the mouth can open
        }
        return 7.0D;
    }

    /**
     * The warp entry sequence: converging letterbox, violet pull grade,
     * camera yaw dragged toward the mouth, white flash - then the server
     * thread runs the ORIGINAL bowels teleport once. No loading screen.
     */
    private static void paintWarp(Minecraft mc, LocalPlayer player,
            GuiGraphicsExtractor g, int w, int h) {
        java.util.UUID id = player.getUUID();
        if (!net.mcsm.extras.McsmWarp.warping(id)) {
            warpSoundDone = false;
            warpReleased = false;
            return;
        }
        float p = net.mcsm.extras.McsmWarp.progress(id);
        if (!warpSoundDone) {
            warpSoundDone = true;
            player.playSound(net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL, 1.0F, 0.8F);
        }
        int e = (int) (h * 0.5D * p * p);
        g.fill(0, 0, w, e, 0xFF000000);
        g.fill(0, h - e, w, h, 0xFF000000);
        int a = (int) (130.0F * p);
        g.fillGradient(0, 0, w, h, (a << 24) | 0x2A0A4A, (a << 24) | 0x6A2AC8);
        if (portalNear > 0.02F) {
            float cur = player.getYRot();
            float diff = portalYaw - cur;
            while (diff > 180.0F) {
                diff -= 360.0F;
            }
            while (diff < -180.0F) {
                diff += 360.0F;
            }
            player.setYRot(cur + diff * 0.10F * p);
        }
        if (p > 0.85F) {
            int fa = (int) (255.0F * (p - 0.85F) / 0.15F);
            g.fill(0, 0, w, h, (fa << 24) | 0xFFFFFF);
        }
        if (p >= 1.0F && !warpReleased) {
            warpReleased = true;
            // 26.2: the client reaches its integrated server through
            // getSingleplayerServer() (verified in ci/api/client.txt);
            // Minecraft.getServer() no longer exists.
            var srv = mc.getSingleplayerServer();
            if (srv != null) {
                srv.execute(() -> {
                    try {
                        net.minecraft.server.level.ServerPlayer sp =
                                srv.getPlayerList().getPlayer(id);
                        if (sp != null) {
                            net.mcsm.extras.McsmWarp.release(sp);
                        }
                    } catch (Throwable ignored) {
                        // never crash on the way through the portal
                    }
                });
            } else {
                net.mcsm.extras.McsmWarp.cancel(id);
            }
        }
    }

    /**
     * The MCSM episode title card: on every world join the screen closes to
     * a cinematic dark plate with heavy letterbox bars, "Episode One /
     * A NEW ORDER" fades in at center in big scaled type, the saga line
     * sits beneath, and a progress bar runs "Entering the story..." before
     * the whole card fades out into gameplay. Client-side only, drawn with
     * the verified extractor surface (fill, fillGradient, centeredText,
     * pose) - no new mixin, no unverified API.
     */
    private static void paintEpisodeCard(GuiGraphicsExtractor g, int w, int h, long t) {
        // fade in over 0.8s, out over the last 1.2s
        float fade = Math.min(1.0F, t / 800.0F)
                * Math.min(1.0F, (CARD_MS - t) / 1200.0F);
        if (fade <= 0.0F) {
            return;
        }
        int a = (int) (fade * 240.0F) << 24;

        // dark plate + heavy letterbox bars
        g.fill(0, 0, w, h, a | 0x050308);
        int bar = Math.max(24, h / 5);
        int barA = (int) (fade * 255.0F) << 24;
        g.fill(0, 0, w, bar, barA | 0x000000);
        g.fill(0, h - bar, w, h, barA | 0x000000);
        g.fillGradient(0, bar, w, bar + 2, a | 0x3F255A, a | 0x6A8FF7);
        g.fillGradient(0, h - bar - 2, w, h - bar, a | 0x6A8FF7, a | 0x3F255A);

        Minecraft mc = Minecraft.getInstance();
        Matrix3x2fStack pose = g.pose();
        int cx = w / 2;
        int cy = h / 2;

        // "Episode One" - big scaled type
        float s1 = Math.min(3.0F, w / 130.0F);
        pose.pushMatrix();
        pose.translate(cx, cy - 46);
        pose.scale(s1);
        g.centeredText(mc.font, "\u00a7f\u00a7lEpisode One", 0, 0, a | 0xEAF2FF);
        pose.popMatrix();

        // "A NEW ORDER"
        float s2 = Math.min(1.9F, w / 210.0F);
        pose.pushMatrix();
        pose.translate(cx, cy - 4);
        pose.scale(s2);
        g.centeredText(mc.font, "\u00a79\u00a7lA  N E W  O R D E R", 0, 0, a | 0xBFD3FF);
        pose.popMatrix();

        // saga line
        g.centeredText(mc.font,
                "\u00a78DEVOURING STORMS \u00a77\u00b7 \u00a78THE POINT OF NO RETURN",
                cx, cy + 34, a | 0x8FA3C8);

        // progress bar + entering text
        float prog = Math.min(1.0F, t / (float) CARD_MS);
        int bw = Math.min(260, w - 80);
        int bx = cx - bw / 2;
        int by = h - bar + 14;
        g.fill(bx, by, bx + bw, by + 3, (int) (fade * 90.0F) << 24 | 0x223355);
        g.fill(bx, by, bx + (int) (bw * prog), by + 3, a | 0x6A8FF7);
        g.centeredText(mc.font, "\u00a77Entering the story\u2026", cx, by + 10, a | 0x9FB4D8);
    }
}
