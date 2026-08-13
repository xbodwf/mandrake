package at.mandrake

import at.mandrake.block.*
import at.mandrake.item.BedrockBreakerItem
import net.minecraft.ChatFormatting
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.SmithingTemplateItem
import net.minecraft.world.level.block.Block

object Mandrake {
    const val MOD_ID = "mandrake"

    lateinit var ENDER_EYE_SMITHING_TEMPLATE: Item
    lateinit var BEDROCK_BREAKER: Item
    lateinit var CORE_PORTAL_BLOCK: Block
    lateinit var CRYSTAL_BLOCK: Block
    lateinit var CRYSTAL_CLUSTER: Block
    lateinit var MAGMA_STONE: Block
    lateinit var GLOWING_MUSHROOM_CAP: Block
    lateinit var GLOWING_MUSHROOM_STEM: Block
    lateinit var GEYSER_VENT: Block

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

        BEDROCK_BREAKER = Registry.register(
            BuiltInRegistries.ITEM, id("bedrock_breaker"),
            BedrockBreakerItem(Item.Properties().stacksTo(1))
        )

        CORE_PORTAL_BLOCK = registerBlock("core_portal", CorePortalBlock())
        CRYSTAL_BLOCK = registerBlock("crystal_block", CrystalBlock())
        CRYSTAL_CLUSTER = registerBlock("crystal_cluster", CrystalClusterBlock())
        MAGMA_STONE = registerBlock("magma_stone", MagmaStoneBlock())
        GLOWING_MUSHROOM_CAP = registerBlock("glowing_mushroom_cap", GlowingMushroomCapBlock())
        GLOWING_MUSHROOM_STEM = registerBlock("glowing_mushroom_stem", GlowingMushroomStemBlock())
        GEYSER_VENT = registerBlock("geyser_vent", GeyserVentBlock())
    }

    private fun registerBlock(name: String, block: Block): Block {
        Registry.register(BuiltInRegistries.BLOCK, id(name), block)
        Registry.register(BuiltInRegistries.ITEM, id(name), BlockItem(block, Item.Properties()))
        return block
    }

    fun id(path: String) = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
