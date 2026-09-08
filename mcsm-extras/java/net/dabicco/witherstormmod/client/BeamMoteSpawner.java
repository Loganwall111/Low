package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.ModParticles;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.client.particle.BeamMoteParticle;
import net.dabicco.witherstormmod.entity.WitherStormHeadEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Beam motes: fine translucent sparkles inside the tractor beam (MCSM ref).
 * Small size, soft alpha, synchronized upward climb. NOT chunky cubes.
 */
public final class BeamMoteSpawner {
    private static final float SPAWN_CHANCE = 0.78F;
    private static final int SPAWNS_PER_TICK = 8;
    /** Shared climb so sparkles rise as one flow. */
    private static final double SYNC_CLIMB = 0.011;

    private BeamMoteSpawner() {
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.isPaused() || !mc.level.tickRateManager().runsNormally()) {
            return;
        }
        RandomSource random = mc.level.getRandom();
        // weather-tinted mote colour lives on the particle via beam config
        float br = (float) Math.max(0.0, Math.min(1.0, DabyWSClientConfig.beamColorR));
        float bg = (float) Math.max(0.0, Math.min(1.0, DabyWSClientConfig.beamColorG));
        float bb = (float) Math.max(0.0, Math.min(1.0, DabyWSClientConfig.beamColorB));

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof WitherStormHeadEntity head) || !head.isBeamActive()) {
                continue;
            }
            float beamScale = head.beamScale();
            int radius = Math.max(Math.round(ClientConfigCache.cfg.beamGroundRadius * beamScale), 1);
            Vec3 ground = head.clientBeamEnd != null ? head.clientBeamEnd : head.getBeamEndExact();
            if (ground == null) {
                continue;
            }
            for (int n = 0; n < SPAWNS_PER_TICK; n++) {
                if (random.nextFloat() > SPAWN_CHANCE) {
                    continue;
                }
                double angle = random.nextDouble() * Math.PI * 2.0;
                // bias toward denser core of the cone
                double radialFrac = Math.pow(random.nextDouble(), 0.55);
                // spawn near ground so they all climb the full shaft together
                double axisT = 0.002 + random.nextDouble() * 0.04;
                BeamMoteParticle.pendingHead = head;
                BeamMoteParticle.pendingAngle = angle;
                BeamMoteParticle.pendingRadialFrac = radialFrac;
                BeamMoteParticle.pendingAxisT = axisT;
                BeamMoteParticle.pendingClimbPerTick = SYNC_CLIMB; // synchronized
                BeamMoteParticle.pendingBaseRadius = radius;
                BeamMoteParticle.pendingBeamScale = beamScale; // natural size, no inflate
                BeamMoteParticle.pendingR = br;
                BeamMoteParticle.pendingG = bg;
                BeamMoteParticle.pendingB = bb;
                double r = TractorBeamRenderer.baseHalfWidth(radius) * radialFrac;
                Vec3 pos = ground.add(Math.cos(angle) * r, 0.25, Math.sin(angle) * r);
                mc.level.addParticle(ModParticles.BEAM_MOTE, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
                BeamMoteParticle.pendingHead = null;
            }
        }
    }
}
