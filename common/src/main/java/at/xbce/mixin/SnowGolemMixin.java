package at.xbce.mixin;

import at.xbce.entity.FakeSnowGolemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 真雪傀儡对假雪傀儡的识别逻辑：
 *  - 戴着南瓜（伪装中）的假雪傀儡不会被认出，也不会挨打；
 *  - 南瓜被剪掉（露出苦力怕头）的假雪傀儡会像普通苦力怕一样被攻击。
 */
@Mixin(SnowGolem.class)
public abstract class SnowGolemMixin {

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        SnowGolem self = (SnowGolem) (Object) this;
        if (self.getTarget() instanceof FakeSnowGolemEntity fake && fake.hasPumpkin()) {
            self.setTarget(null);
        }
    }

    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void onPerformRangedAttack(LivingEntity target, float power, CallbackInfo ci) {
        if (target instanceof FakeSnowGolemEntity fake && fake.hasPumpkin()) {
            ci.cancel();
        }
    }
}
