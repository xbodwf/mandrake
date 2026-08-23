package at.xbce.mixin;

import at.xbce.entity.PhantomExtra;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 幻翼在 Mob 层面的钩子：扫掠命中（偷盔甲 / 抓取）与装死期间免于远距离消失。
 * 以及蠹虫骑士的协同撕咬：宿主命中时头顶蠹虫补刀。
 */
@Mixin(Mob.class)
public abstract class MobSweepMixin {

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void xbce$phantomSweepTricks(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Phantom && (Object) this instanceof PhantomExtra extra
            && extra.xbce$sweepAttackHook(target)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    @Inject(method = "doHurtTarget", at = @At("TAIL"))
    private void xbce$jockeyRiderBite(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;

        // 蠹虫骑士：宿主打中谁，头顶的蠹虫就跟着咬一口（伤害归属蠹虫，击杀信息也是它）
        if ((Object) this instanceof Zombie zombie && !(target instanceof Silverfish)
            && zombie.level() instanceof ServerLevel
            && zombie.getFirstPassenger() instanceof Silverfish rider) {
            rider.doHurtTarget(target);
        }

        // 幻翼追击：命中猎物重置疲劳
        if ((Object) this instanceof Phantom && (Object) this instanceof PhantomExtra extra) {
            extra.xbce$onSweepLanded(target);
        }
    }

    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    private void xbce$phantomStayWhilePlayingDead(double distance, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Phantom && (Object) this instanceof PhantomExtra extra
            && extra.xbce$isPlayingDead()) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
