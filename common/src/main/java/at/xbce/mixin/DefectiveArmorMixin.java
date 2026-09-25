package at.xbce.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 残次品：贴到护甲上的膜照样不护机——护甲吸收的伤害按身上最残的那件
 * 缩水 10%/20%/30%（I/II/III）。
 */
@Mixin(LivingEntity.class)
public class DefectiveArmorMixin {

    @Unique private static final ResourceKey<Enchantment> XBCE_DEFECTIVE =
        ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("xbce", "defective"));

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true)
    private void xbce$defectiveArmorReduction(DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        int level = xbce$maxDefectiveArmorLevel(self);
        if (level <= 0) return;

        float afterArmor = cir.getReturnValueF();
        float absorbed = damage - afterArmor;
        if (absorbed <= 0.0F) return;
        // 护甲吸收的部分缩水：I 10% / II 20% / III 30%
        float newAbsorbed = absorbed * (1.0F - level * 0.10F);
        cir.setReturnValue(damage - newAbsorbed);
    }

    /** 取四件护甲（仅限 ArmorItem）中残次品的最高等级。 */
    @Unique
    private static int xbce$maxDefectiveArmorLevel(LivingEntity entity) {
        int max = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem)) continue;
            for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) {
                if (holder.unwrapKey().map(key -> key.equals(XBCE_DEFECTIVE)).orElse(false)) {
                    max = Math.max(max, stack.getEnchantments().getLevel(holder));
                    break;
                }
            }
        }
        return max;
    }
}
