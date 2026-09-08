package net.mcsm.extras;

import net.dabicco.witherstormmod.WitherStormSummon;
import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * MCSM - shared "storm relay" burst used by lit vanilla beacons and by the
 * storm beacon block: shockwave ring + thunder, then the position goes to
 * WitherStormSummon.trySpawn (the mod's only summon route). If no skull
 * formation is present, an awake storm nearby is provoked instead.
 */
public final class McsmStormFx {

    // MCSM -- 26.2 spelling of the particle call: options, not types.
    private static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(rgb, scale);
    }

    public static void fire(Level world, BlockPos pos) {
        if (world.isClientSide()) return;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 1.4, cz = pos.getZ() + 0.5;
        long gt = world.getGameTime();

        if (world instanceof ServerLevel sl) {
            long seed = gt * 2654435761L;
            for (int i = 0; i < 26; i++) {
                double a = (i / 26.0) * Math.PI * 2.0;
                for (int k = 0; k < 3; k++) {
                    double r = 5.0 + k * 4.5 + ((seed >> (i & 31)) & 3) * 0.4;
                    double px = cx + Math.cos(a) * r, pz = cz + Math.sin(a) * r;
                    sl.sendParticles(dust(0xd8e6ff, 0.7f), px, cy + 0.5 + k * 0.8, pz, 1, 0.12, 0.35, 0.12, 0.06);
                    sl.sendParticles(dust(0xfff3e0, 3.0f), px, cy + k, pz, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            sl.sendParticles(dust(0xfff3e0, 3.0f), cx, cy + 1.0, cz, 1, 0.0, 0.0, 0.0, 0.0);
        }
        world.playSound(null, cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.2f, 0.85f);

        boolean summoned = false;
        try {
            summoned = WitherStormSummon.trySpawn(world, pos.above());
        } catch (Throwable ignored) { }
        if (!summoned) {
            try {
                List<WitherStormEntity> storms = world.getEntitiesOfClass(WitherStormEntity.class,
                        new AABB(pos).inflate(96.0, 160.0, 96.0));
                if (!storms.isEmpty()) {
                    storms.get(0).forceTentacleSlam();
                    world.playSound(null, cx, cy, cz, SoundEvents.ELDER_GUARDIAN_CURSE,
                            SoundSource.HOSTILE, 1.4f, 0.7f);
                }
            } catch (Throwable ignored) { }
        }
    }

    private McsmStormFx() {}
}
