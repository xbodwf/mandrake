package at.mandrake

import net.minecraft.ChatFormatting
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.SmithingTemplateItem

object Mandrake {
    const val MOD_ID = "mandrake"

    lateinit var ENDER_EYE_SMITHING_TEMPLATE: Item

    fun fabricRegisterAll() {
        ENDER_EYE_SMITHING_TEMPLATE = Registry.register(
            BuiltInRegistries.ITEM, id("ender_eye_smithing_template"),
            SmithingTemplateItem(
                Component.translatable("item.mandrake.ender_eye_smithing_template.applies_to"),
                Component.translatable("item.mandrake.ender_eye_smithing_template.ingredients"),
                Component.translatable("item.mandrake.ender_eye_smithing_template.upgrade_description").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.mandrake.ender_eye_smithing_template.base_slot_description"),
                Component.translatable("item.mandrake.ender_eye_smithing_template.additions_slot_description"),
                listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_emerald")),
                listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_redstone_dust"))
            )
        )
    }

    fun id(path: String) = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
