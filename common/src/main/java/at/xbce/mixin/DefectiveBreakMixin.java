package at.xbce.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 残次品（I~III）：残次品膜不护机——每点耐久消耗按等级
 * 有 10%/20%/30% 概率额外 +1。
 * （附魔书不通过碎裂获得，与原版两种诅咒一样走自然刷新渠道。）
 */
@Mixin(ItemStack.class)
public class DefectiveBreakMixin {

    @Unique private static final RandomSource XBCE_RANDOM = RandomSource.create();
    @Unique private static final ResourceKey<Enchantment> XBCE_DEFECTIVE =
        ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("xbce", "defective"));

    /** 按等级 10%/20%/30% 概率本次耐久额外 -1。 */
    @ModifyVariable(
        method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private int xbce$defectiveExtraWear(int amount) {
        if (amount <= 0) return amount;
        int defectiveLevel = xbce$defectiveLevel((ItemStack) (Object) this);
        if (defectiveLevel <= 0) return amount;
        if (XBCE_RANDOM.nextInt(100) >= defectiveLevel * 10) return amount;
        return amount + 1;
    }

    /** 返回物品上的残次品等级（0 = 没有）。 */
    @Unique
    private static int xbce$defectiveLevel(ItemStack stack) {
        for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) {
            if (holder.unwrapKey().map(key -> key.equals(XBCE_DEFECTIVE)).orElse(false)) {
                return stack.getEnchantments().getLevel(holder);
            }
        }
        return 0;
    }
}
