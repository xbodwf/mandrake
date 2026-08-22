package at.xbce.mixin;

import at.xbce.entity.PhantomExtra;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 幻翼在 Mob 层面的钩子：扫掠命中（偷盔甲 / 抓取）与装死期间免于远距离消失。
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

    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    private void xbce$phantomStayWhilePlayingDead(double distance, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Phantom && (Object) this instanceof PhantomExtra extra
            && extra.xbce$isPlayingDead()) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
