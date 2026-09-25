package at.xbce.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 残次品：均匀随机路径（enchant_randomly 战利品函数）同样做 10% 剔除。
 * 注意 run 里的 list 来自 Stream.toList()（不可变），必须复制后再删。
 */
@Mixin(EnchantRandomlyFunction.class)
public class DefectiveLootMixin {

    @Unique private static final RandomSource XBCE_RANDOM = RandomSource.create();
    @Unique private static final ResourceKey<Enchantment> XBCE_DEFECTIVE =
        ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("xbce", "defective"));

    @ModifyVariable(method = "run", at = @At("STORE"), ordinal = 0)
    private List<Holder<Enchantment>> xbce$shaveDefective(List<Holder<Enchantment>> candidates) {
        if (candidates.size() < 2 || XBCE_RANDOM.nextInt(10) != 0) return candidates;
        List<Holder<Enchantment>> copy = new ArrayList<>(candidates);
        copy.removeIf(holder -> holder.unwrapKey()
            .map(key -> key.equals(XBCE_DEFECTIVE)).orElse(false));
        return copy;
    }
}
