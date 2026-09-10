package net.mcsm.extras;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Command-block story controls.
 *
 * User-facing rules:
 *  - shift + right-click a command block opens Devouring Storms config;
 *  - right-click a command block without shift snaps nearby entities out of the
 *    world without killing them (DISCARDED, not damage/death). This is the
 *    first gameplay pass for the MCSM command-block "white void" power.
 */
public final class McsmCommandBlockUse {
    private static boolean hooked = false;

    private McsmCommandBlockUse() {}

    public static synchronized void register() {
        if (hooked) return;
        hooked = true;
        try {
            Class<?> cb = Class.forName("net.fabricmc.fabric.api.event.player.UseBlockCallback");
            Object event = cb.getField("EVENT").get(null);
            InvocationHandler handler = (proxy, method, args) -> {
                if (!"interact".equals(method.getName()) || args == null) {
                    return defaultAnswer(method);
                }
                return onUse(args);
            };
            Object listener = Proxy.newProxyInstance(cb.getClassLoader(), new Class<?>[] { cb }, handler);
            for (Method m : event.getClass().getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isInstance(listener)) {
                    m.invoke(event, listener);
                    System.out.println("[ds] command block config/snap interaction registered");
                    return;
                }
            }
        } catch (Throwable t) {
            System.err.println("[ds] command block interaction unavailable: " + t);
        }
    }

    private static Object defaultAnswer(Method method) {
        Class<?> ret = method.getReturnType();
        if (ret == boolean.class) return Boolean.FALSE;
        if (ret == int.class) return Integer.valueOf(0);
        if (InteractionResult.class.isAssignableFrom(ret)) return InteractionResult.PASS;
        return null;
    }

    private static void openClientPanel() {
        try {
            Class<?> mcCls = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcCls.getMethod("getInstance").invoke(null);
            Object parent = null;
            try {
                java.lang.reflect.Field f = mcCls.getField("screen");
                parent = f.get(mc);
            } catch (Throwable ignored) {
            }
            Class<?> screenCls = Class.forName("net.mcsm.extras.client.McsmExtrasScreen");
            Class<?> baseScreen = Class.forName("net.minecraft.client.gui.screens.Screen");
            Object screen = screenCls.getConstructor(baseScreen).newInstance(parent);
            for (Method m : mcCls.getMethods()) {
                if (("setScreenAndShow".equals(m.getName()) || "setScreen".equals(m.getName()))
                        && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(baseScreen)) {
                    m.invoke(mc, screen);
                    return;
                }
            }
        } catch (Throwable t) {
            System.err.println("[ds] command block panel open failed: " + t);
        }
    }

    private static Object onUse(Object[] args) {
        Player player = null;
        Level level = null;
        BlockHitResult hit = null;
        for (Object a : args) {
            if (a instanceof Player p) player = p;
            if (a instanceof Level l) level = l;
            if (a instanceof BlockHitResult h) hit = h;
        }
        if (player == null || level == null || hit == null) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        if (!level.getBlockState(pos).is(Blocks.COMMAND_BLOCK)
                && !level.getBlockState(pos).is(Blocks.CHAIN_COMMAND_BLOCK)
                && !level.getBlockState(pos).is(Blocks.REPEATING_COMMAND_BLOCK)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                openClientPanel();
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        AABB box = new AABB(pos).inflate(18.0D, 12.0D, 18.0D);
        List<Entity> targets = level.getEntities(player, box,
                e -> e.isAlive() && !(e instanceof Player) && !e.isSpectator());
        int snapped = 0;
        for (Entity e : targets) {
            try {
                e.remove(Entity.RemovalReason.DISCARDED);
                snapped++;
            } catch (Throwable ignored) {
            }
        }
        final int n = snapped;
        player.sendSystemMessage(Component.literal("§f§lCommand Block§r§7 snapped " + n
                + " entit" + (n == 1 ? "y" : "ies") + " into the white void."));
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.55F);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.6F);
        return InteractionResult.SUCCESS;
    }
}
