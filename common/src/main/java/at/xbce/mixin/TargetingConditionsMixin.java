package at.xbce.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 全局潜行系统——察觉半径修正：
 *  - 潜行移动：×0.35
 *  - 疾跑：×1.15
 *  - 光照 < 4（黑暗）：×0.7
 *  反击永远有效（怪物已被目标攻击/锁定时不受缩减影响）。
 */
@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {

    @Shadow
    private double range;

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void xbce$stealthDetection(LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (range <= 0) return;
        if (!(attacker instanceof Monster)) return;
        if (!(target instanceof Player player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        // 反击豁免：它刚被这个玩家打过，或者已经盯上他了
        if (attacker.getLastHurtByMob() == target) return;
        if (attacker instanceof Mob mob && mob.getTarget() == target) return;

        double factor = player.isCrouching() ? 0.35 : (player.isSprinting() ? 1.15 : 1.0);
        if (player.level().getMaxLocalRawBrightness(player.blockPosition()) < 4) {
            factor *= 0.7;
        }
        factor = Math.max(factor, 0.25);

        double limit = range * factor;
        if (attacker.distanceToSqr(target) > limit * limit) {
            cir.setReturnValue(false);
        }
    }
}
