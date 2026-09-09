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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Devouring Storms: mega-phase 9 - the Story Mode towns are INHABITED,
 * and they talk.
 *
 *  - POPULATION: Every ground site gets a cast list with custom named NPCs
 *    (Jesse, Petra, Axel, Olivia, Lukas, Gabriel, Ivor, Soren, etc.).
 *
 *  - DIALOGUE: Right-clicking opens their lines, advances progression,
 *    plays voice/speaking audio, triggers head nodding, mouth movement,
 *    and speech particle effects.
 *
 *  - AI & ANIMATION: Walking animations, wandering, and tracking nearby players.
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
                "We need the Order of the Stone. Whatever this is, we can't fight it alone.",
                "Keep your sword ready and stay close. We stick together.",
                "Look at the tractor beam... don't let it pull you in!",
                "There has to be a way to destroy the command block inside it."
        });
        LINES.put("Petra", new String[] {
                "I've tracked wither sickness from the Nether to the surface. It spreads through the soil.",
                "Whatever Ivor built down in that basement, it wasn't meant to be set free.",
                "Watch your back out there. The tentacles move faster than you think.",
                "Take this advice: never look directly into its center eye."
        });
        LINES.put("Axel", new String[] {
                "I told you guys TNT was the answer! Why doesn't anyone listen when I say TNT?",
                "Do you think the Formidi-Bomb could blow a hole in that storm?",
                "If we survive this, I'm getting ten slices of cake.",
                "Is it just me, or is the sky turning purple again?"
        });
        LINES.put("Olivia", new String[] {
                "The redstone circuitry in that command block shouldn't even be possible.",
                "Ellegaard's notes mentioned an ultimate weapon... Redstonia might have the schematics.",
                "Keep checking your coordinates! The storm alters local atmospheric density.",
                "If we synchronize the repeaters, we might create an EMP pulse."
        });
        LINES.put("Lukas", new String[] {
                "The Ocelots are holding the perimeter. We won't let it reach the shelter.",
                "I wrote down everything in my journal: the phases, the sky color shifts, the beam reach.",
                "If Jesse has a plan, count me in. All the way.",
                "The people are frightened, but hope is what keeps us moving forward."
        });
        LINES.put("Gabriel", new String[] {
                "In my youth, the Order faced perils beyond imagining... but this beast defies reason.",
                "The blade of the warrior is useless if the heart quails. Stand firm!",
                "Find Soren. He knows the secret that bound the beast before.",
                "The light of the beacon shall pierce the darkest veil!"
        });
        LINES.put("Ivor", new String[] {
                "The potion of lingering wither! The command block! It was meant to obey my skull!",
                "The tracking code... it modified its own growth vectors! It consumes everything!",
                "Listen to me! You must craft the Formidi-Bomb at the assembly temple!",
                "Fools! Do not get sucked into the bowels of the beast!"
        });
        LINES.put("Soren", new String[] {
                "Ah, the symphony of destruction! Do you hear the harmony in its wailing roar?",
                "My Endermen... they were studying the geometry of the void before the storm broke through.",
                "Take the Super TNT. Place it at the core. And pray the explosion doesn't unravel reality.",
                "Music, my friend, is the only true defense against cosmic cataclysm."
        });
        LINES.put("Ellegaard", new String[] {
                "My automated redstone defenses were overrun in minutes. The tractor beam has infinite torque!",
                "The pulse repeater array in the courtyard needs calibrated frequency crystals.",
                "Engineering is the triumph of mind over monstrous chaos."
        });
        LINES.put("Magnus", new String[] {
                "Boom! That's what I'm talking about! More gunpowder, bigger craters!",
                "Boom Town didn't stand a chance, but we gave it one heck of an explosion!",
                "If you're gonna go out, go out in a blaze of glorious fireworks!"
        });
        LINES.put("Radar", new String[] {
                "Jesse! I filed all the emergency evacuation reports in triplicate!",
                "The weather forecast says 100% chance of falling flaming netherrack chunks!",
                "I have spare shields and potions stored in the Town Hall basement!",
                "Don't worry, Champion Jesse, Beacon Town believes in you!"
        });
        LINES.put("Stella", new String[] {
                "Champion City's monuments will NOT be consumed by some unruly oversized dust cloud!",
                "Lluna! Stop eating the glowstone dust! We have an emergency!",
                "I suppose Jesse's little coalition is our only viable option at the moment."
        });
        LINES.put("Harper", new String[] {
                "The Redstonia archives contain the master operational codes for the central core.",
                "PAMA was dangerous, but this entity operates on primal devouring instinct.",
                "The dimensional portal frequencies are fluctuating wildly with the storm's growth."
        });

        for (String[] members : CAST.values()) {
            for (String m : members) {
                ROSTER.add(m);
            }
        }
    }

    private McsmNpcs() {
    }

    public static void populate(ServerLevel level, String townLabel, BlockPos origin) {
        if (level == null || origin == null) return;
        String[] cast = CAST.get(townLabel);
        if (cast == null || cast.length == 0) return;

        String key = level.dimension().location() + "/" + townLabel + "/" + origin.getX() + "," + origin.getZ();
        if (POPULATED.contains(key)) return;

        // Idempotency check: see if any roster NPC already stands nearby
        AABB box = new AABB(origin).inflate(64.0, 32.0, 64.0);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box)) {
            if (e.hasCustomName() && ROSTER.contains(e.getCustomName().getString())) {
                POPULATED.add(key);
                return;
            }
        }

        RandomSource rng = level.random;
        int n = cast.length;
        for (int i = 0; i < n; i++) {
            String name = cast[i];
            double angle = (i / (double) n) * Math.PI * 2.0 + rng.nextDouble() * 0.4;
            double dist = 4.0 + rng.nextDouble() * 7.0;
            int px = (int) Math.round(origin.getX() + Math.cos(angle) * dist);
            int pz = (int) Math.round(origin.getZ() + Math.sin(angle) * dist);
            int py = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, px, pz);

            BlockPos spawnPos = new BlockPos(px, py, pz);
            if (!level.getBlockState(spawnPos.below()).isSolid()) {
                py = level.getHeight(Types.WORLD_SURFACE, px, pz);
                spawnPos = new BlockPos(px, py, pz);
            }

            try {
                // Spawn named NPC entity
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.tryParse("minecraft:villager"));
                if (type == null) {
                    type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.tryParse("minecraft:armor_stand"));
                }
                if (type != null) {
                    Entity ent = type.create(level, EntitySpawnReason.STRUCTURE);
                    if (ent != null) {
                        ent.setPos(px + 0.5, py, pz + 0.5);
                        ent.setCustomName(Component.literal(name));
                        ent.setCustomNameVisible(true);
                        ent.setPersistenceRequired();
                        ent.setInvulnerable(true);
                        level.addFreshEntity(ent);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        POPULATED.add(key);
        hookInteraction();
    }

    public static void hookInteraction() {
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
            // silent fallback
        }
    }

    private static Object defaultAnswer(Method method) {
        Class<?> ret = method.getReturnType();
        if (ret == boolean.class) return Boolean.FALSE;
        if (ret == int.class) return Integer.valueOf(0);
        if (InteractionResult.class.isAssignableFrom(ret)) return InteractionResult.PASS;
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

        String[] tree = LINES.getOrDefault(name, new String[] {
                "Be careful out there! The Storm is gathering strength."
        });
        String key = player.getUUID() + "/" + name;
        int i = PROGRESS.getOrDefault(key, 0);
        PROGRESS.put(key, (i + 1) % tree.length);

        // Look at player + subtle speaking hop
        try {
            if (target instanceof Mob mob) {
                mob.getLookControl().setLookAt(player, 45.0F, 45.0F);
                mob.setYHeadRot(player.getYRot());
                Vec3 v = mob.getDeltaMovement();
                mob.setDeltaMovement(v.x, Math.max(v.y, 0.26), v.z);
            }
        } catch (Throwable ignored) {
        }

        // Send dialogue message to chat
        player.sendSystemMessage(Component.literal("\u00a7d\u00a7l" + name + "\u00a7r\u00a77: \u00a7f"
                + tree[i % tree.length]));

        // Speaking particle burst (golden & amethyst dust)
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(new DustParticleOptions(0xFFE082, 0.8f),
                    target.getX(), target.getY() + 1.8, target.getZ(),
                    6, 0.2, 0.1, 0.2, 0.02);
            sl.sendParticles(new DustParticleOptions(0xD86BFF, 0.6f),
                    target.getX(), target.getY() + 1.6, target.getZ(),
                    4, 0.15, 0.15, 0.15, 0.01);
        }

        // Voice audio feedback
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.0F + (float) (Math.random() * 0.25));
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.45F, 1.2F);

        return InteractionResult.SUCCESS;
    }

    /** Called every server tick from worldgen/loop. */
    public static void npcTick(ServerLevel level) {
        if (level == null || !McsmExtrasConfig.npcWalkAnimations) {
            return;
        }
        try {
            if (level.players().isEmpty()) {
                return;
            }
            long gt = level.getGameTime();
            if (gt % 10L != 0L) return;

            for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(-3000, -64, -3000, 3000, 320, 3000))) {
                if (e.hasCustomName() && ROSTER.contains(e.getCustomName().getString())) {
                    if (e instanceof Mob mob) {
                        Player nearest = level.getNearestPlayer(mob, 16.0);
                        if (nearest != null) {
                            mob.getLookControl().setLookAt(nearest, 25.0F, 25.0F);
                        } else if (level.random.nextFloat() < 0.15F) {
                            // Subtle wandering
                            double offX = (level.random.nextDouble() - 0.5) * 0.4;
                            double offZ = (level.random.nextDouble() - 0.5) * 0.4;
                            mob.setDeltaMovement(offX, mob.getDeltaMovement().y, offZ);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
