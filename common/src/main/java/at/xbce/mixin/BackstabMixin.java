package at.xbce.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 背刺：潜行状态下从目标身后近战命中 → 伤害 ×1.5。
 * 身后判定：目标视线与“指向攻击者”的方向夹角超过约 120°（点积 < -0.5）。
 */
@Mixin(LivingEntity.class)
public class BackstabMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float xbce$backstab(float amount, DamageSource source) {
        if (!(source.getEntity() instanceof Player player)) return amount;
        if (!player.isShiftKeyDown()) return amount;
        // 仅近战直击（直接来源=玩家本人）
        if (source.getDirectEntity() != source.getEntity()) return amount;

        LivingEntity self = (LivingEntity) (Object) this;
        Vec3 look = self.getViewVector(1.0F);
        Vec3 toAttacker = new Vec3(player.getX() - self.getX(), 0.0, player.getZ() - self.getZ());
        double len = toAttacker.length();
        if (len < 0.01) return amount;

        double dot = look.x * (toAttacker.x / len) + look.z * (toAttacker.z / len);
        if (dot < -0.5) {
            return amount * 1.5F;
        }
        return amount;
    }
}
