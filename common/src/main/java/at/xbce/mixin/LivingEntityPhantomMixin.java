package at.xbce.mixin;

import at.xbce.entity.PhantomExtra;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 幻翼在 LivingEntity 层面的钩子：装死（取消死亡）、真实死亡赃物结算、
 * 装死期间无敌但受击立即苏醒。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPhantomMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void xbce$phantomFakeDeath(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof PhantomExtra extra && extra.xbce$deathHook(source)) {
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void xbce$phantomRealDeathLoot(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof PhantomExtra extra) {
            extra.xbce$onRealDeath();
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void xbce$phantomWakeOnHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof PhantomExtra extra)) return;
        if (extra.xbce$isPlayingDead()) {
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
            extra.xbce$wakeFromFakeDeath(attacker);
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
