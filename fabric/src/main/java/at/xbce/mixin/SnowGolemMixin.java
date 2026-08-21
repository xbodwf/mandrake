package at.xbce.mixin;

import at.xbce.entity.FakeSnowGolemEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowGolem.class)
public abstract class SnowGolemMixin {

    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void onPerformRangedAttack(net.minecraft.world.entity.LivingEntity target, float power, CallbackInfo ci) {
        if (target instanceof FakeSnowGolemEntity) {
            ci.cancel();
        }
    }
}
