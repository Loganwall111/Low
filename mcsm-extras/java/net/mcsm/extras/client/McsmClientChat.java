package net.mcsm.extras.client;

import net.mcsm.extras.McsmExtrasConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/**
 * MCSM -- the build announces itself IN CHAT.
 *
 * Four rounds of "I see none of the changes" ended the same way: the evidence
 * that would have settled it lived in latest.log and in the mods list, i.e. in
 * files the player has to go and dig out while holding a suspicion. Chat is
 * where the player already is -- they screenshot it -- so the build number is
 * now spoken there once per world load, and the death/rise sequences report
 * when they arm. A screenshot of chat after this build therefore answers both
 * "which jar is loaded?" and "did the hook fire at all?" with no file hunting.
 *
 * These lines are ours and only ours: Dabicco's mod prints nothing like them,
 * so seeing them is itself proof the MCSM jar is the one running.
 *
 * Client-only class, called only from client mixins.
 */
public final class McsmClientChat {

    /** The world we last announced in; a new world announces again. */
    private static ClientLevel lastLevel = null;

    private McsmClientChat() {}

    /** One plain chat line to the local player. Never throws. */
    public static void say(String msg) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                return;
            }
            mc.player.sendSystemMessage(Component.literal(msg));
        } catch (Throwable ignored) {
            // chat must never break a frame
        }
    }

    /**
     * The loaded-banner line, once per world load: [mcsm] MCSM extras, then the
     * build number. If the mods list or the Extras header shows a different
     * number, an older jar is still sitting in mods/ and is the one running.
     */
    public static void announceBuildOnce() {
        try {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc == null ? null : mc.level;
            if (level == null || mc.player == null) {
                lastLevel = null;
                return;
            }
            if (level == lastLevel) {
                return;
            }
            lastLevel = level;
            say("[ds] Devouring Storms " + McsmExtrasConfig.BUILD_VERSION
                + " loaded (base: Dabicco's Wither Storm Mod). Mods list and"
                + " panel header must read the same number, or an older jar"
                + " is still in mods/.");
        } catch (Throwable ignored) {
            // chat must never break a frame
        }
    }
}
