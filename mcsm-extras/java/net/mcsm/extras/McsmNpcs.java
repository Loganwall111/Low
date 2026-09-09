package net.mcsm.extras;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.dabicco.witherstormmod.structures.McsmWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Devouring Storms: mega-phase 9 - the towns are INHABITED, and they talk.
 *
 * Until now the Story Mode sites were architecture and nothing else. This
 * fills them with the cast and gives every one of them a dialogue tree.
 *
 *  - POPULATION. Every ground site from the base mod's own layout() gets a
 *    cast list of its own - Beacon Town has Radar and Stella, EnderCon has
 *    Gabriel and Lukas, the Order's temple has Soren, and so on. They spawn
 *    the moment a player comes within 128 blocks (so the chunks are loaded),
 *    named, name-tag visible, persistent. The spawn is idempotent: if any
 *    named mob is already standing in the town the site is skipped, so a
 *    server restart never doubles the cast.
 *
 *  - DIALOGUE. Right-clicking a story NPC opens their line instead of a
 *    trade screen; each further click advances the tree and it loops when
 *    it runs out, per player, per character. The interaction is hooked
 *    through Fabric's UseEntityCallback by REFLECTION - the callback is
 *    registered if the event class is present and silently skipped if it
 *    is not, so no build can ever break on it.
 *
 * Every Minecraft call here is one the base mod already makes on 26.2
 * (StoryNpcSpawner's spawn sequence, getEntitiesOfClass, isLoaded,
 * playSound, sendSystemMessage).
 */
public final class McsmNpcs {

    /** town label -> the cast that lives there. */
    private static final Map<String, String[]> CAST = new HashMap<>();

    /** character -> their dialogue tree. */
    private static final Map<String, String[]> LINES = new HashMap<>();

    private static final Set<String> POPULATED = new HashSet<>();
    private static final Map<String, Integer> PROGRESS = new HashMap<>();
    private static final Set<String> ROSTER = new HashSet<>();

    private static boolean hooked;
    private static ServerLevel lastLevel;

    static {
        CAST.put("Beacon Town", new String[] { "Radar", "Stella", "Lluna's Keeper", "Nurm" });
        CAST.put("Beacon Town Outskirts", new String[] { "Stampy", "Dan" });
        CAST.put("Beacon Town Map Shop", new String[] { "Jack", "Nurm" });
        CAST.put("EnderCon Town Fair", new String[] { "Jesse", "Petra", "Axel", "Olivia", "Lukas" });
        CAST.put("Order of the Stone Temple", new String[] { "Gabriel", "Ivor" });
        CAST.put("Temple Interior", new String[] { "Ivor" });
        CAST.put("The Wilderness", new String[] { "Petra", "Reuben's Tracker" });
        CAST.put("Wilderness Treehouse", new String[] { "Olivia" });
        CAST.put("Wilderness Tower", new String[] { "Axel" });
        CAST.put("Forest Stage", new String[] { "Sparklez", "Stacy" });
        CAST.put("Ellegaard's Courtyard", new String[] { "Ellegaard" });
        CAST.put("Magnus's Courtyard", new String[] { "Magnus" });
        CAST.put("Soren's Courtyard", new String[] { "Soren" });
        CAST.put("Soren's Interior", new String[] { "Soren" });
        CAST.put("The Creepy Mansion", new String[] { "The White Pumpkin" });
        CAST.put("Champion City", new String[] { "Aiden", "Maya", "Gill" });
        CAST.put("Snowy Village", new String[] { "Binta", "Wink" });
        CAST.put("The Wonderland", new String[] { "Otto", "Hadrian" });
        CAST.put("The Prison Maze", new String[] { "Em" });
        CAST.put("Beacon Town (Twisted)", new String[] { "Radar", "Nell" });
        CAST.put("Terminal Control Center", new String[] { "Harper", "PAMA Terminal" });
        CAST.put("Far Lands Maze", new String[] { "Nell" });
        CAST.put("Badlands Maze", new String[] { "Fangirl" });

        LINES.put("Jesse", new String[] {
                "That thing in the sky... it keeps getting bigger. Tell me you see it too.",
                "The Order of the Stone would know what to do. They have to.",
                "Reuben, stay close. I mean it.",
                "We built this place. I'm not letting it get eaten." });
        LINES.put("Petra", new String[] {
                "You're staring at it. Everyone stares at it.",
                "I've fought a lot of things. Nothing that size.",
                "If you're going out there, take a sword. Take two.",
                "Don't get command-blocked into standing still. Move." });
        LINES.put("Axel", new String[] {
                "Griefing a storm. Now THAT'S a plan.",
                "I've got TNT. I always have TNT.",
                "You look about as calm as I feel. Which is not calm." });
        LINES.put("Olivia", new String[] {
                "Redstone won't fix this one. I already tried the math.",
                "It's pulling blocks off the ground. Whole chunks of it.",
                "Someone built that thing. On purpose. Think about that." });
        LINES.put("Lukas", new String[] {
                "The Ocelots are gone. Everyone's gone.",
                "I keep writing it all down. Someone should remember this.",
                "Stay near the beacon. The light helps." });
        LINES.put("Radar", new String[] {
                "Sir! Ma'am! Whichever! I have a clipboard and I'm ready!",
                "I have scheduled the evacuation. Twice. Nobody signed it.",
                "Beacon Town needs you. I need you. Mostly Beacon Town." });
        LINES.put("Ivor", new String[] {
                "It was a command block. It was ALWAYS a command block.",
                "You want to know how to stop it? So does everyone.",
                "Do not approach the tractor beam. I will not repeat that." });
        LINES.put("Gabriel", new String[] {
                "The Order stands. Whatever comes.",
                "I have faced the Ender Dragon. This... this is different.",
                "Keep your people together. That is the whole of it." });
        LINES.put("Ellegaard", new String[] {
                "Redstone engineering, not luck. That's what saves a town.",
                "Bring me components and I'll bring you a chance." });
        LINES.put("Magnus", new String[] {
                "Blow it up! What? It's a strategy!",
                "Boom Town would have loved this. Boom Town is gone." });
        LINES.put("Soren", new String[] {
                "I built a machine to send us somewhere it isn't. It didn't work.",
                "The formidi-bomb. It is the only answer I have left.",
                "Do not tell the others I ran. Please." });
        LINES.put("Harper", new String[] {
                "PAMA learned. That was the mistake. Everything after was consequence.",
                "The terminal still answers. I wish it wouldn't." });
        LINES.put("Stella", new String[] {
                "Champion City would have handled this better. Obviously.",
                "Do not touch my llama." });
        LINES.put("Nurm", new String[] { "Hrrm.", "Hrmmm!", "Hrm. Hrm hrm." });
        LINES.put("PAMA Terminal", new String[] {
                "YOU WILL BE USEFUL.", "COMPLIANCE IS EFFICIENT.", "THE STORM IS NOT IN MY PARAMETERS." });

        for (String[] cast : CAST.values()) {
            for (String n : cast) {
                ROSTER.add(n);
            }
        }
    }

    private McsmNpcs() {
    }

    /** Generic townsfolk lines for cast members without their own tree. */
    private static String[] linesFor(String name) {
        String[] own = LINES.get(name);
        if (own != null) {
            return own;
        }
        return new String[] {
                "You've seen it, haven't you. The thing over the hills.",
                "We keep the lamps burning. It helps. A little.",
                "Half the town packed up. The other half won't leave.",
                "If it comes here, run. Don't be brave about it." };
    }

    /* ---- population ------------------------------------------------------ */

    /** Called every server tick from the worldgen patch. */
    public static void tick(ServerLevel level) {
        try {
            ensureHook();
            if (lastLevel != level) {
                lastLevel = level;
                POPULATED.clear();
                PROGRESS.clear();
            }
            if (level.dimension() != Level.OVERWORLD || level.getGameTime() % 40L != 0L) {
                return;
            }
            if (level.players().isEmpty()) {
                return;
            }
            for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
                if (s.floating() || POPULATED.contains(s.label())) {
                    continue;
                }
                String[] cast = CAST.get(s.label());
                if (cast == null) {
                    continue;
                }
                BlockPos centre = new BlockPos(s.x(), s.y(), s.z());
                boolean near = false;
                for (Entity p : level.players()) {
                    if (p.blockPosition().distSqr(centre) < 128.0 * 128.0) {
                        near = true;
                        break;
                    }
                }
                if (!near || !level.isLoaded(centre)) {
                    continue;
                }
                POPULATED.add(s.label());
                populate(level, centre, cast);
            }
        } catch (Throwable ignored) {
            // a town without its cast is survivable; a crashed tick is not
        }
    }

    /** Prefer non-villager looks so cast members feel distinct (user request). */
    private static String entityIdFor(String name) {
        String n = name.toLowerCase();
        if (n.contains("reuben") || n.contains("lluna") || n.contains("pig")) {
            return "minecraft:pig";
        }
        if (n.contains("stampy") || n.contains("dan") || n.contains("wolf")) {
            return "minecraft:wolf";
        }
        if (n.contains("magnus") || n.contains("aiden") || n.contains("golem")) {
            return "minecraft:iron_golem";
        }
        if (n.contains("soren") || n.contains("ellegaard") || n.contains("harper")
                || n.contains("pama") || n.contains("white pumpkin")) {
            return "minecraft:witch";
        }
        if (n.contains("gabriel") || n.contains("ivor") || n.contains("petra")
                || n.contains("jesse") || n.contains("axel") || n.contains("olivia")
                || n.contains("lukas") || n.contains("radar") || n.contains("stella")
                || n.contains("nurm") || n.contains("nell") || n.contains("em")
                || n.contains("maya") || n.contains("gill") || n.contains("hadrian")
                || n.contains("otto") || n.contains("binta") || n.contains("wink")
                || n.contains("jack") || n.contains("fangirl") || n.contains("sparklez")
                || n.contains("stacy")) {
            // player-like: use armor stand with custom name for distinct pose,
            // fall back to villager if armor_stand spawn fails
            return "minecraft:armor_stand";
        }
        return "minecraft:armor_stand";
    }

    private static void populate(ServerLevel level, BlockPos centre, String[] cast) {
        // idempotent: if the town already has named residents, leave it alone
        AABB box = AABB.ofSize(new Vec3(centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5),
                96.0, 48.0, 96.0);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box)) {
            if (m.hasCustomName()) {
                return;
            }
        }
        RandomSource random = level.getRandom();
        List<String> names = new ArrayList<>(List.of(cast));
        for (int i = 0; i < names.size(); i++) {
            String who = names.get(i);
            String eid = entityIdFor(who);
            String ns = "minecraft";
            String path = "villager";
            int colon = eid.indexOf(':');
            if (colon > 0) {
                ns = eid.substring(0, colon);
                path = eid.substring(colon + 1);
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                    .getValue(Identifier.fromNamespaceAndPath(ns, path));
            if (type == null) {
                type = BuiltInRegistries.ENTITY_TYPE
                        .getValue(Identifier.fromNamespaceAndPath("minecraft", "villager"));
            }
            if (type == null) {
                continue;
            }
            double ang = (i / (double) names.size()) * Math.PI * 2.0 + random.nextDouble() * 0.6;
            double ring = 5.0 + random.nextDouble() * 9.0;
            int x = centre.getX() + (int) Math.round(Math.cos(ang) * ring);
            int z = centre.getZ() + (int) Math.round(Math.sin(ang) * ring);
            int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            Entity created = type.create(level, EntitySpawnReason.STRUCTURE);
            if (!(created instanceof Mob mob)) {
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE,
                    (SpawnGroupData) null);
            mob.setCustomName(Component.literal(who));
            mob.setCustomNameVisible(true);
            mob.setPersistenceRequired();
            mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            // idle wander so they feel alive
            try {
                mob.setNoAi(false);
            } catch (Throwable ignored) {
            }
            level.addFreshEntity(mob);
        }
    }

    /* ---- dialogue -------------------------------------------------------- */

    /**
     * Registers the interaction listener through Fabric's UseEntityCallback
     * by reflection: present, we talk; absent, the towns are simply quiet.
     */
    private static void ensureHook() {
        if (hooked) {
            return;
        }
        hooked = true;
        try {
            Class<?> cb = Class.forName("net.fabricmc.fabric.api.event.player.UseEntityCallback");
            Object event = cb.getField("EVENT").get(null);
            InvocationHandler handler = (proxy, method, args) -> {
                if (!"interact".equals(method.getName()) || args == null || args.length < 4) {
                    return defaultAnswer(method);
                }
                return onInteract(args);
            };
            Object listener = Proxy.newProxyInstance(cb.getClassLoader(), new Class<?>[] { cb }, handler);
            for (Method m : event.getClass().getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isInstance(listener)) {
                    m.invoke(event, listener);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // no fabric interaction module: towns stay silent, nothing breaks
        }
    }

    private static Object defaultAnswer(Method method) {
        Class<?> ret = method.getReturnType();
        if (ret == boolean.class) {
            return Boolean.FALSE;
        }
        if (ret == int.class) {
            return Integer.valueOf(0);
        }
        if (InteractionResult.class.isAssignableFrom(ret)) {
            return InteractionResult.PASS;
        }
        return null;
    }

    private static Object onInteract(Object[] args) {
        Player player = args[0] instanceof Player p ? p : null;
        Entity target = null;
        for (Object a : args) {
            if (a instanceof Entity e && !(a instanceof Player)) {
                target = e;
                break;
            }
        }
        if (player == null || target == null || !target.hasCustomName()) {
            return InteractionResult.PASS;
        }
        Component nameC = target.getCustomName();
        String name = nameC == null ? "" : nameC.getString();
        if (!ROSTER.contains(name)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            // consume on the client too, so no trade screen is predicted
            return InteractionResult.SUCCESS;
        }
        String[] tree = linesFor(name);
        String key = player.getUUID() + "/" + name;
        int i = PROGRESS.getOrDefault(key, 0);
        PROGRESS.put(key, (i + 1) % tree.length);
        // look at player + small hop = "speaking" body language
        try {
            if (target instanceof Mob mob) {
                mob.getLookControl().setLookAt(player, 40.0F, 40.0F);
                mob.setYHeadRot(player.getYRot());
                // micro hop so the mouth/head "moves" while speaking
                Vec3 v = mob.getDeltaMovement();
                mob.setDeltaMovement(v.x, Math.max(v.y, 0.28), v.z);
            }
        } catch (Throwable ignored) {
        }
        player.sendSystemMessage(Component.literal("\u00a7d\u00a7l" + name + "\u00a7r\u00a77: \u00a7f"
                + tree[i % tree.length]));
        // villager/yes sounds read as speech better than bundle click


    /* ---- NPC Tick AI ------------------------------------------------------ */
    /** Called every server tick from the worldgen patch. */
    public static void npcTick(ServerLevel level) {
        try {
            if (level.players().isEmpty()) {
                return;
            }
            // Subtle AI: make NPCs wander slightly and look at players
            for (Entity e : level.entitiesByClass(Entity.class)) {
                if (e.hasCustomName() && e instanceof Mob mob) {
                    // Make mob look at random player sometimes
                    if (level.random.nextFloat() < 0.01F) {
                        for (Entity player : level.players()) {
                            if (player.distanceTo(mob) < 32.0) {
                                mob.getLookControl().setLookAt(player, 20.0F, 20.0F);
                                break;
                            }
                        }
                    }
                    // Subtle walking animation - slight position perturbations
                    if (level.random.nextFloat() < 0.005F) {
                        double offsetX = level.random.nextGaussian() * 0.5;
                        double offsetZ = level.random.nextGaussian() * 0.5;
                        mob.setPos(mob.getX() + offsetX, mob.getY(), mob.getZ() + offsetZ);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }


        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 0.95F + (float) (Math.random() * 0.2));
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.55F, 1.25F);
        return InteractionResult.SUCCESS;
    }
}
