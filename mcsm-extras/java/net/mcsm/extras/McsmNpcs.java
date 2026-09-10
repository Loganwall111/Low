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
import net.minecraft.core.particles.DustParticleOptions;
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
 *  - POPULATION: Canonical Story Mode cast spawned at towns & sites.
 *  - DIALOGUE: Multi-line story dialogue trees with speech tones & dust motes.
 *  - AI & ANIMATION: Looking at players, speaking micro-hops, and wandering.
 */
public final class McsmNpcs {

    /** town label -> the cast that lives there. */
    private static final Map<String, String[]> CAST = new HashMap<>();

    /** character -> their dialogue tree. */
    private static final Map<String, String[]> LINES = new HashMap<>();

    private static final Set<String> POPULATED = new HashSet<>();
    private static final Map<String, Integer> PROGRESS = new HashMap<>();
    private static final Set<String> ROSTER = new HashSet<>();
    private static final Set<String> STORY_TOWNS = new HashSet<>();
    private static final String[] SPAWN_EGG_CAST = {
            "Jesse", "Petra", "Axel", "Olivia", "Lukas", "Radar", "Ivor", "Gabriel",
            "Ellegaard", "Magnus", "Soren", "Harper", "Stella", "Nurm", "Jack", "Binta",
            "Otto", "Hadrian", "Maya", "Stampy"
    };
    private static final int TOWN_RADIUS = 86;

    private static boolean hooked;
    private static ServerLevel lastLevel;

    static {
        CAST.put("Beacon Town", new String[] { "Radar", "Stella", "Nurm" });
        CAST.put("Beacon Town Outskirts", new String[] { "Stampy" });
        CAST.put("Beacon Town Map Shop", new String[] { "Jack" });
        CAST.put("EnderCon Town Fair", new String[] { "Jesse", "Petra", "Axel" });
        // Non-town story sites no longer receive ambient population by default.
        CAST.put("Champion City", new String[] { "Aiden", "Maya" });
        CAST.put("Snowy Village", new String[] { "Binta", "Wink" });
        CAST.put("The Wonderland", new String[] { "Otto", "Hadrian" });
        CAST.put("Beacon Town (Twisted)", new String[] { "Radar" });
        STORY_TOWNS.addAll(CAST.keySet());

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

        // Keep every older cast name in the managed roster so 1.9.186 can clean
        // overpopulated/out-of-town NPCs left behind by 1.9.185 and earlier.
        ROSTER.addAll(LINES.keySet());
        ROSTER.add("Reuben's Tracker");
        ROSTER.add("Lluna's Keeper");
        ROSTER.add("Stampy");
        ROSTER.add("Dan");
        ROSTER.add("Jack");
        ROSTER.add("Maya");
        ROSTER.add("Gill");
        ROSTER.add("Binta");
        ROSTER.add("Wink");
        ROSTER.add("Otto");
        ROSTER.add("Hadrian");
        ROSTER.add("Em");
        ROSTER.add("Nell");
        ROSTER.add("Fangirl");
        ROSTER.add("The White Pumpkin");
        ROSTER.add("Sparklez");
        ROSTER.add("Stacy");

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
            pruneLegacyOverpopulation(level);
            for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
                if (s.floating() || POPULATED.contains(s.label()) || !STORY_TOWNS.contains(s.label())) {
                    continue;
                }
                String[] cast = CAST.get(s.label());
                if (cast == null) {
                    continue;
                }
                BlockPos centre = new BlockPos(s.x(), s.y(), s.z());
                boolean near = false;
                for (Entity p : level.players()) {
                    if (p.blockPosition().distSqr(centre) < TOWN_RADIUS * TOWN_RADIUS) {
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

    public static String[] storyCharacterNames() {
        return SPAWN_EGG_CAST.clone();
    }

    public static Mob spawnStoryCharacter(ServerLevel level, BlockPos at, String who, boolean manual) {
        if (level == null || at == null || who == null || who.isBlank()) {
            return null;
        }
        try {
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
                return null;
            }
            Entity created = type.create(level, manual ? EntitySpawnReason.COMMAND : EntitySpawnReason.STRUCTURE);
            if (!(created instanceof Mob mob)) {
                return null;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at),
                    manual ? EntitySpawnReason.COMMAND : EntitySpawnReason.STRUCTURE, (SpawnGroupData) null);
            applyHumanVariant(mob, who, Math.floorMod(who.hashCode(), 16));
            mob.setCustomName(Component.literal(who));
            mob.setCustomNameVisible(true);
            mob.setPersistenceRequired();
            mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
            try {
                mob.setNoAi(false);
            } catch (Throwable ignored) {
            }
            level.addFreshEntity(mob);
            return mob;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Prefer non-villager looks so cast members feel distinct. */
    private static String entityIdFor(String name) {
        String n = name.toLowerCase();
        if (n.contains("reuben") || n.contains("lluna") || n.contains("pig")) {
            return "minecraft:pig";
        }
        // Human story cast should look human; renderer/resource overrides turn
        // villager bodies into Story Mode people while preserving vanilla AI.
        return "minecraft:villager";
    }

    private static boolean isNearStoryTown(BlockPos pos) {
        for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
            if (!s.floating() && STORY_TOWNS.contains(s.label())) {
                BlockPos centre = new BlockPos(s.x(), s.y(), s.z());
                if (pos.distSqr(centre) <= TOWN_RADIUS * TOWN_RADIUS) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void pruneLegacyOverpopulation(ServerLevel level) {
        try {
            for (Player p : level.players()) {
                AABB scan = p.getBoundingBox().inflate(144.0D, 64.0D, 144.0D);
                Map<String, Integer> seenByTown = new HashMap<>();
                for (Mob mob : level.getEntitiesOfClass(Mob.class, scan)) {
                    if (!isManagedCast(mob)) {
                        continue;
                    }
                    BlockPos bp = mob.blockPosition();
                    String town = nearestStoryTown(bp);
                    if (town == null || !isNearStoryTown(bp)) {
                        discard(mob);
                        continue;
                    }
                    int seen = seenByTown.getOrDefault(town, 0);
                    int allowed = CAST.containsKey(town) ? CAST.get(town).length : 0;
                    if (seen >= Math.max(1, allowed)) {
                        discard(mob);
                    } else {
                        seenByTown.put(town, seen + 1);
                    }
                }
            }
        } catch (Throwable ignored) {
            // population cleanup must never break the server tick
        }
    }

    private static boolean isManagedCast(Mob mob) {
        if (!mob.hasCustomName()) {
            return false;
        }
        Component c = mob.getCustomName();
        return c != null && ROSTER.contains(c.getString());
    }

    private static String nearestStoryTown(BlockPos pos) {
        String bestLabel = null;
        double best = Double.MAX_VALUE;
        for (McsmWorldgen.Site s : McsmWorldgen.layout()) {
            if (!s.floating() && STORY_TOWNS.contains(s.label())) {
                BlockPos centre = new BlockPos(s.x(), s.y(), s.z());
                double d = pos.distSqr(centre);
                if (d < best) {
                    best = d;
                    bestLabel = s.label();
                }
            }
        }
        return best <= TOWN_RADIUS * TOWN_RADIUS ? bestLabel : null;
    }

    private static void discard(Entity entity) {
        try {
            Class<?> reason = Class.forName("net.minecraft.world.entity.Entity$RemovalReason");
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Class enumClass = reason.asSubclass(Enum.class);
            Object discarded = Enum.valueOf(enumClass, "DISCARDED");
            entity.getClass().getMethod("remove", reason).invoke(entity, discarded);
        } catch (Throwable ignored) {
            try {
                entity.getClass().getMethod("discard").invoke(entity);
            } catch (Throwable ignoredToo) {
                // If neither removal path exists, leave the legacy NPC alone.
            }
        }
    }

    private static void populate(ServerLevel level, BlockPos centre, String[] cast) {
        AABB box = AABB.ofSize(new Vec3(centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5),
                96.0, 48.0, 96.0);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box)) {
            if (isManagedCast(m)) {
                return;
            }
        }
        RandomSource random = level.getRandom();
        List<String> names = new ArrayList<>(List.of(cast));
        for (int i = 0; i < names.size(); i++) {
            String who = names.get(i);
            double ang = (i / (double) names.size()) * Math.PI * 2.0 + random.nextDouble() * 0.6;
            double ring = 5.0 + random.nextDouble() * 9.0;
            int x = centre.getX() + (int) Math.round(Math.cos(ang) * ring);
            int z = centre.getZ() + (int) Math.round(Math.sin(ang) * ring);
            int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            spawnStoryCharacter(level, new BlockPos(x, y, z), who, false);
        }
    }

    private static void applyHumanVariant(Mob mob, String who, int index) {
        // Reflective on purpose: 26.x mappings moved villager data classes a few
        // times. If the methods/classes are missing, the NPC still walks/talks.
        try {
            Class<?> villager = Class.forName("net.minecraft.world.entity.npc.Villager");
            if (!villager.isInstance(mob)) {
                return;
            }
            Object data = villager.getMethod("getVillagerData").invoke(mob);
            Class<?> vtype = Class.forName("net.minecraft.world.entity.npc.VillagerType");
            Class<?> prof = Class.forName("net.minecraft.world.entity.npc.VillagerProfession");
            // 1.9.201: deterministic outfit per character (the type textures are
            // the Story Look skins), not a hash roulette.
            Object type = namedRegistryValue(vtype, typeFor(who));
            Object profession = namedRegistryValue(prof, profFor(who));
            if (type != null) {
                data = data.getClass().getMethod("setType", vtype).invoke(data, type);
            }
            if (profession != null) {
                data = data.getClass().getMethod("setProfession", prof).invoke(data, profession);
            }
            villager.getMethod("setVillagerData", data.getClass()).invoke(mob, data);
        } catch (Throwable ignored) {
        }
    }

    /** Story Look villager-type skin each named character always wears. */
    private static String typeFor(String who) {
        return switch (who) {
            case "Jesse", "Stampy", "Jack", "Aiden" -> "plains";
            case "Petra", "Stella", "Radar" -> "savanna";
            case "Axel", "Magnus", "Ellegaard" -> "desert";
            case "Olivia", "Harper", "Maya" -> "jungle";
            case "Lukas", "Nurm" -> "taiga";
            case "Ivor", "Soren", "Gabriel", "Binta", "Wink" -> "snow";
            case "Otto", "Hadrian", "Dan" -> "swamp";
            default -> switch (Math.floorMod(who.hashCode(), 6)) {
                case 0 -> "plains";
                case 1 -> "savanna";
                case 2 -> "taiga";
                case 3 -> "snow";
                case 4 -> "desert";
                default -> "jungle";
            };
        };
    }

    /** Matching profession so hats/robes do not fight the outfit. */
    private static String profFor(String who) {
        return switch (who) {
            case "Ivor", "Soren" -> "cleric";
            case "Olivia", "Harper" -> "toolsmith";
            case "Ellegaard", "Magnus" -> "mason";
            case "Radar", "Jack" -> "cartographer";
            default -> "none";
        };
    }

    private static Object namedRegistryValue(Class<?> holder, String name) {
        try {
            for (String field : new String[] { name.toUpperCase(), name.toUpperCase().replace('-', '_') }) {
                try {
                    return holder.getField(field).get(null);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /* ---- dialogue & interaction ------------------------------------------ */

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
            // no fabric interaction module: silent fallback
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
            return InteractionResult.SUCCESS;
        }
        String[] tree = linesFor(name);
        String key = player.getUUID() + "/" + name;
        int i = PROGRESS.getOrDefault(key, 0);
        PROGRESS.put(key, (i + 1) % tree.length);

        // look at player + speaking micro-hop
        try {
            if (target instanceof Mob mob) {
                mob.getLookControl().setLookAt(player, 40.0F, 40.0F);
                mob.setYHeadRot(player.getYRot());
                // Speaking/waving animation: vanilla swing if present, plus the
                // tiny Story Mode hop already used for dialogue emphasis.
                try {
                    Class<?> hand = Class.forName("net.minecraft.world.InteractionHand");
                    Object main = hand.getField("MAIN_HAND").get(null);
                    mob.getClass().getMethod("swing", hand).invoke(mob, main);
                } catch (Throwable ignoredSwing) {
                }
                Vec3 v = mob.getDeltaMovement();
                mob.setDeltaMovement(v.x, Math.max(v.y, 0.28), v.z);
            }
        } catch (Throwable ignored) {
        }
        player.sendSystemMessage(Component.literal("\u00a7d\u00a7l" + name + "\u00a7r\u00a77: \u00a7f"
                + tree[i % tree.length]));

        // Speaking dust sparkles
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(new DustParticleOptions(0xFFE082, 0.8f),
                    target.getX(), target.getY() + 1.8, target.getZ(),
                    4, 0.2, 0.1, 0.2, 0.02);
        }

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 0.95F + (float) (Math.random() * 0.2));
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.55F, 1.25F);
        return InteractionResult.SUCCESS;
    }
}
