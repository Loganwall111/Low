package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.entity.WitherStormHeadEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.dabicco.witherstormmod.config.WitherStormConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 4+ beam ground crumbs: denser block-break particles matching the
 * actual floor blocks (user: not glacier cards — pulled dirt/stone/grass).
 * Complements spawnCrumbParticles with a second burst each tick.
 */
@Mixin(WitherStormHeadEntity.class)
public abstract class McsmBeamCrumbBoostPatch {

    @Inject(method = "spawnCrumbParticles", at = @At("TAIL"), require = 0)
    private void mcsm$moreCrumbs(ServerLevel server, BlockPos end, int groundRadius, CallbackInfo ci) {
        try {
            WitherStormHeadEntity self = (WitherStormHeadEntity) (Object) this;
            // denser ring of matching floor particles rising into the beam
            int bursts = 10;
            for (int i = 0; i < bursts; i++) {
                double ang = server.getRandom().nextDouble() * Math.PI * 2.0;
                double rr = groundRadius * Math.sqrt(server.getRandom().nextDouble());
                int bx = end.getX() + (int) Math.round(Math.cos(ang) * rr);
                int bz = end.getZ() + (int) Math.round(Math.sin(ang) * rr);
                BlockPos top = server.getHeightmapPos(
                        WitherStormConfigs.get(server).groundHeightmap(),
                        new BlockPos(bx, 0, bz)).below();
                if (Math.abs(top.getY() - end.getY()) > 5) {
                    top = end;
                }
                BlockState state = server.getBlockState(top);
                if (state.isAir()) {
                    continue;
                }
                // block dust + a few rising chips
                server.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        top.getX() + server.getRandom().nextDouble(),
                        top.getY() + 1.05 + server.getRandom().nextDouble() * 0.4,
                        top.getZ() + server.getRandom().nextDouble(),
                        4,
                        0.35, 0.35, 0.35,
                        0.12);
            }
        } catch (Throwable ignored) {
        }
    }
}
