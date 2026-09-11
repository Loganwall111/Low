package net.mcsm.extras;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * MCSM 1.9.100 -- the briefing.
 *
 * The user asked for "MCSM instructions" in the mod. The Story Mode game states
 * them as Jesse's narration, and the mod has no place that does, so this adds
 * one: the first time a player gets close enough to a live storm, they are told
 * what they are looking at and what the rules are -- once, per player, per
 * session. Not a help book to go and read: the storm tells you.
 *
 * Deliberately server-side and deliberately plain (sendSystemMessage), so it
 * needs no client GUI and cannot desync.
 */
public final class McsmStory {

    /** Players who have already been briefed this session. */
    private static final Set<UUID> BRIEFED = new HashSet<>();

    private static final Component[] LINES = {
        line(ChatFormatting.DARK_PURPLE, "--- The Wither Storm ---"),
        line(ChatFormatting.LIGHT_PURPLE, "It grows through five phases, and every one is hungrier than the last."),
        line(ChatFormatting.GRAY, "It cannot be outrun. It can only be unmade."),
        line(ChatFormatting.GRAY, "Blocks it devours do not come back. Do not lead it home."),
        line(ChatFormatting.GOLD, "A lit beacon calls it. The storm beacon block calls it too."),
        line(ChatFormatting.RED, "Phase 4 is the point of no return."),
        line(ChatFormatting.DARK_PURPLE, "Survive until the tear closes. That is the whole story."),
        line(ChatFormatting.AQUA, "The story starts at the treehouse outside EnderCon:"),
        line(ChatFormatting.AQUA, "/ds towns start -- or /ds towns for all 35 sites + coords."),
        line(ChatFormatting.YELLOW, "The MCSM Visual Shader is the DEFAULT and self-installs."),
        line(ChatFormatting.YELLOW, "No resource packs, no downloads -- everything is in the mod."),
    };

    /** Brief a player once. Cheap: a set lookup after the first call. */
    public static void brief(ServerPlayer sp) {
        if (sp == null) {
            return;
        }
        UUID id = sp.getUUID();
        if (BRIEFED.contains(id)) {
            return;
        }
        BRIEFED.add(id);
        try {
            for (Component c : LINES) {
                sp.sendSystemMessage(c);
            }
        } catch (Throwable ignored) {
            // A chat API change must cost us the briefing, never the tick.
        }
    }

    /** Forget a player (logged out) so a fresh session is briefed again. */
    public static void forget(UUID id) {
        BRIEFED.remove(id);
    }

    private static Component line(ChatFormatting colour, String text) {
        return Component.literal(text).withStyle(colour);
    }

    private McsmStory() {}
}
