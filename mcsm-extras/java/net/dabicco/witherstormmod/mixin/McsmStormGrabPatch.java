package net.dabicco.witherstormmod.mixin;

import net.mcsm.extras.McsmExtrasConfig;
import net.mcsm.extras.McsmStormFx;
import net.mcsm.extras.McsmFxDriver;
import net.mcsm.extras.McsmGate;
import net.mcsm.extras.McsmStormBeaconBlock;

import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCSM - tentacle grab + storm-rise ground fx.
 *
 * The grab machinery already exists in the mod (forceTentacleSlam spawns a
 * GrabTentacleEntity whose own hit handling calls registerGrabHit to pull,
 * shake, eat or throw players); it just never self-triggers outside the
 * command. This injects at the tail of the storm's tick: while a survival
 * player is within reach, the storm slams on its own on a cadence. The rise
 * fx fires during the spawn animation the storm plays as it ascends - spark
 * and dust rings tear off the ground around it ("the ground being torn apart").
 */
@Mixin(WitherStormEntity.class)
public abstract class McsmStormGrabPatch extends net.minecraft.world.entity.boss.wither.WitherBoss {

    private static final Map<UUID, Long> MCSM$GRAB_CD = new ConcurrentHashMap<>();
    private long mcsm$gt = -1L;

    private McsmStormGrabPatch() { super(null, null); }

    // MCSM -- 26.2 spelling of the particle call: options, not types.
    private static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(rgb, scale);
    }

    @Inject(method = {"die"}, at = @At("HEAD"))
    private void mcsm$death(DamageSource source, CallbackInfo ci) {
        WitherStormEntity self = (WitherStormEntity) (Object) this;
        Level level = self.level();
        // Fire before vanilla death cleanup/removal so the rings use the real storm position.
        McsmFxDriver.deathCinematic(self, level);
    }

    // MCSM -- /kill (and any command-based removal) never calls die():
    // it removes the entity outright, so the death sequence silently never
    // began. From the player's seat that is exactly "nothing happens when the
    // storm dies, no shockwave, no rings". remove() is on every removal path,
    // so this is the net that catches them all; the guard inside
    // deathCinematic() keeps an ordinary damage-death from firing twice.
    @Inject(method = {"remove"}, at = @At("HEAD"))
    private void mcsm$removed(net.minecraft.world.entity.Entity.RemovalReason reason,
                              CallbackInfo ci) {
        WitherStormEntity self = (WitherStormEntity) (Object) this;
        Level level = self.level();
        if (level == null || level.isClientSide()) return;
        try {
            if (reason == net.minecraft.world.entity.Entity.RemovalReason.KILLED
                    || self.isDeadOrDying()) {
                McsmFxDriver.deathCinematic(self, level);
            }
        } catch (Throwable ignored) {
            // a death visual must never break removal
        }
    }

    // MCSM -- the addSubGrowth TAIL hook that used to live here armed
    // the phase 4/7 rise shockwave a SECOND time for the same phase jump:
    // McsmFxDriver.tick() already watches for the transition, and both paths
    // called startBlast, so the player saw two identical
    // "rise shockwave armed" chat lines and the front restarted mid-expansion.
    // The tick watcher is the single owner of the transition now, with a
    // 5-second re-arm latch inside McsmFxDriver.startBlast as the safety net.

    @Inject(method = {"tick"}, at = @At("TAIL"))
    private void mcsm$extras(CallbackInfo ci) {
        McsmExtrasConfig.load();
        WitherStormEntity self = (WitherStormEntity) (Object) this;
        Level level = self.level();
        if (level == null || level.isClientSide()) return;
        // Server only: in single player both sides share one JVM, and writing
        // the client's synced copy of the world config would be overwritten by
        // the next sync packet (or silently never reach the server).
        McsmGate.openWorld(level);
        long gt = level.getGameTime();
        if (gt == this.mcsm$gt) return;
        this.mcsm$gt = gt;
        McsmFxDriver.tick(self, level, gt);

        // ---- rise fx: spawn animation = the storm leaving the ground ----
        if (McsmExtrasConfig.enableRiseFx && self.isPlayingSpawnAnimation() && gt % 3L == 0L
                && level instanceof ServerLevel sl) {
            double x = self.getX(), y = self.getY(), z = self.getZ();
            // AABB.getXsize() could not be verified against 26.2 — the public
            // minX/maxX fields are (BowelsFrame uses them), so compute it here.
            double r0 = (self.getBoundingBox().maxX - self.getBoundingBox().minX) * 0.5 + 5.0;
            long seed = gt * 31L;
            for (int i = 0; i < 14; i++) {
                double a = (i / 14.0 + ((seed >> 3 & 63) / 4096.0)) * Math.PI * 2.0;
                double rr = r0 + ((seed >> i & 7)) * 1.7;
                double px = x + Math.cos(a) * rr, pz = z + Math.sin(a) * rr;
                double py = Math.max(y - 26.0, self.getBoundingBox().minY) + 0.4;
                sl.sendParticles(dust(0xd8e6ff, 0.7f), px, py + 1.2, pz, 3, 0.5, 0.9, 0.5, 0.05);
                sl.sendParticles(dust(0x9aa0a6, 2.2f), px, py, pz, 4, 0.7, 0.5, 0.7, 0.028);
                sl.sendParticles(dust(0xb8b8b8, 1.8f), px, py + 0.6, pz, 2, 0.5, 0.7, 0.5, 0.06);
            }
        }

        // ---- tentacle grab: self-triggered slams on a cadence ----
        if (!McsmExtrasConfig.enableTentacleGrab || McsmExtrasConfig.grabIntervalSeconds <= 0.0) return;
        if (self.isPlayingSpawnAnimation()) return;
        if (gt - MCSM$GRAB_CD.getOrDefault(self.getUUID(), Long.MIN_VALUE)
                < (long) (McsmExtrasConfig.grabIntervalSeconds * 20.0)) return;
        if (!(level instanceof ServerLevel srv)) return;
        Player victim = null;
        double best = 46.0 * 46.0;
        for (net.minecraft.server.level.ServerPlayer sp
                : srv.getPlayers(q -> q.isAlive() && !q.isSpectator() && !q.getAbilities().instabuild)) {
            double d = sp.distanceToSqr(self);
            if (d < best) { best = d; victim = sp; }
        }
        if (victim == null) return;
        MCSM$GRAB_CD.put(self.getUUID(), gt);
        try {
            self.forceTentacleSlam();
        } catch (Throwable ignored) {
            // their slam keeps its own state; if a phase forbids it, skip quietly
        }
    }
}
