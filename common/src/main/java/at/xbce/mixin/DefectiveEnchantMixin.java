package at.xbce.mixin;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 残次品：加权选取路径（附魔台同款算法，战利品 enchant_with_levels 等）
 * 有 10% 概率把残次品从候选池剔除 —— 相对出现率约为原版诅咒的 90%。
 */
@Mixin(EnchantmentHelper.class)
public class DefectiveEnchantMixin {

    @Unique private static final RandomSource XBCE_RANDOM = RandomSource.create();
    @Unique private static final ResourceKey<Enchantment> XBCE_DEFECTIVE =
        ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("xbce", "defective"));

    @Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"))
    private static void xbce$shaveDefectiveChance(
        int cost, ItemStack stack, Stream<Holder<Enchantment>> stream,
        CallbackInfoReturnable<List<EnchantmentInstance>> cir
    ) {
        List<EnchantmentInstance> results = cir.getReturnValue();
        if (results == null || results.size() < 2) return;
        if (XBCE_RANDOM.nextInt(10) != 0) return;
        results.removeIf(instance -> instance.enchantment.unwrapKey()
            .map(key -> key.equals(XBCE_DEFECTIVE)).orElse(false));
    }
}
