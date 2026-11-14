package at.xbce.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Soft compatibility for spear items: either a future vanilla spear or a mod
 * that backports the vanilla spear ids (for example Backported Spears).
 * Any item with the id minecraft:*_spear is treated as a spear.
 */
public final class SpearCompat {

    private static List<Item> cachedSpears;

    private SpearCompat() {
    }

    public static List<Item> spears() {
        List<Item> cached = cachedSpears;
        if (cached != null) return cached;

        List<Item> found = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if ("minecraft".equals(id.getNamespace()) && id.getPath().endsWith("_spear")) {
                found.add(item);
            }
        }
        cachedSpears = found;
        return found;
    }

    public static boolean hasSpears() {
        return !spears().isEmpty();
    }

    public static boolean isSpear(ItemStack stack) {
        return !stack.isEmpty() && spears().contains(stack.getItem());
    }

    public static Item randomSpear(RandomSource random) {
        List<Item> all = spears();
        return all.isEmpty() ? null : all.get(random.nextInt(all.size()));
    }

    /** Extra ATTACK_DAMAGE granted by the spear, with a fallback for items without modifiers. */
    public static float bonusDamage(ItemStack stack) {
        float[] bonus = {0.0f};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)
                && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                bonus[0] += (float) modifier.amount();
            }
        });
        return Math.max(2.0f, bonus[0]);
    }
}
