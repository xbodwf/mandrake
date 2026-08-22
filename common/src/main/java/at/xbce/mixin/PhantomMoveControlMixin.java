package at.xbce.mixin;

import at.xbce.entity.PhantomBoostControl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 暴露幻翼移控的内部 speed 字段（0.1 缓升至 1.8 的巡航速度），
 * 烟花加速期间由 PhantomMixin 临时改写。
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomMoveControl")
public abstract class PhantomMoveControlMixin implements PhantomBoostControl {

    @Shadow
    private float speed;

    @Override
    public void xbce$setBoostSpeed(float value) {
        this.speed = value;
    }
}
