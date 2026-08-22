package at.xbce.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 投掷物落地声东击西：雪球/鸡蛋等砸地的声响会吸引
 * 附近 16 格内没有锁定目标的怪物过去查看。
 */
@Mixin(Projectile.class)
public abstract class ProjectileNoiseMixin {

    @Inject(method = "onHit", at = @At("TAIL"))
    private void xbce$noise(HitResult result, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;
        if (!(self instanceof ThrowableItemProjectile)) return;

        Vec3 loc = result.getLocation();
        for (Monster monster : level.getEntitiesOfClass(Monster.class, AABB.ofSize(loc, 32.0, 16.0, 32.0))) {
            if (monster.getTarget() != null) continue;
            monster.getNavigation().moveTo(loc.x, loc.y, loc.z, 1.1);
        }
    }
}
