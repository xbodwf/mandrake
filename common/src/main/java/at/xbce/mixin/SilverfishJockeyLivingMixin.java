package at.xbce.mixin;

import java.util.List;

import at.xbce.entity.JockeyExtra;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 蠹虫骑士的宿主钩子：
 *  - 受伤时呼叫一次增援蠹虫（附近数量有上限）；
 *  - 宿主死亡时，头顶的蠹虫弹射出并锁定凶手。
 */
@Mixin(LivingEntity.class)
public abstract class SilverfishJockeyLivingMixin {

    @Unique private static final float XBCE_REINFORCE_CHANCE = 0.30F;
    @Unique private static final int XBCE_REINFORCE_MAX_NEARBY = 8;

    // ----------------------------------------------------------------- hurt

    @Inject(method = "hurt", at = @At("TAIL"))
    private void xbce$jockeyCallReinforcements(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Zombie zombie)) return;
        if (!((Object) this instanceof JockeyExtra extra)) return;
        if (extra.xbce$hasCalledReinforcements()) return;
        if (zombie.level().isClientSide() || !zombie.isAlive()) return;
        if (!(zombie.level() instanceof ServerLevel level)) return;
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        if (!zombie.hasPassenger(p -> p instanceof Silverfish)) return;
        if (zombie.getRandom().nextFloat() >= XBCE_REINFORCE_CHANCE) return;

        List<Silverfish> nearby = level.getEntitiesOfClass(Silverfish.class,
            zombie.getBoundingBox().inflate(16.0D), e -> e.isAlive());
        if (nearby.size() >= XBCE_REINFORCE_MAX_NEARBY) {
            extra.xbce$markReinforcementsCalled();
            return;
        }

        extra.xbce$markReinforcementsCalled();
        int spawned = 0;
        for (int i = 0; i < 4 && spawned < 2; i++) {
            double ox = (zombie.getRandom().nextDouble() - 0.5D) * 6.0D;
            double oz = (zombie.getRandom().nextDouble() - 0.5D) * 6.0D;
            var pos = zombie.blockPosition().offset((int) ox, 0, (int) oz);
            if (!Mob.checkMobSpawnRules(EntityType.SILVERFISH, level, MobSpawnType.EVENT, pos, zombie.getRandom())) continue;

            Silverfish backup = EntityType.SILVERFISH.create(level);
            if (backup == null) continue;
            backup.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                zombie.getRandom().nextFloat() * 360.0F, 0.0F);
            backup.setTarget(attacker);
            level.addFreshEntity(backup);
            spawned++;
        }
        if (spawned > 0) {
            level.sendParticles(ParticleTypes.POOF,
                zombie.getX(), zombie.getY() + 1.0D, zombie.getZ(), 10, 0.4D, 0.5D, 0.4D, 0.02D);
            zombie.playSound(SoundEvents.SILVERFISH_AMBIENT, 1.5F, 0.7F);
        }
    }

    // ------------------------------------------------------------------ die

    @Inject(method = "die", at = @At("TAIL"))
    private void xbce$jockeyPopOut(DamageSource source, CallbackInfo ci) {
        if (!((Object) this instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide()) return;

        LivingEntity killer = source.getEntity() instanceof LivingEntity living ? living : null;
        for (Entity passenger : List.copyOf(zombie.getPassengers())) {
            if (passenger instanceof Silverfish rider) {
                if (killer != null && !killer.isSpectator()) {
                    rider.setTarget(killer);
                }
                rider.playSound(SoundEvents.SILVERFISH_AMBIENT, 1.0F, 1.2F);
            }
        }
    }
}
