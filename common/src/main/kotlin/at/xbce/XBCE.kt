package at.xbce

import at.xbce.entity.CaravanTraderEntity
import at.xbce.entity.FakeIronGolemEntity
import at.xbce.entity.FakeSnowGolemEntity
import at.xbce.entity.FakeVillagerEntity
import at.xbce.item.RevealSpyglassItem
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.item.Item
import net.minecraft.world.item.SmithingTemplateItem
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object XBCE {
    const val MOD_ID = "xbce"

    lateinit var FAKE_SNOW_GOLEM: EntityType<FakeSnowGolemEntity>
    lateinit var FAKE_VILLAGER: EntityType<FakeVillagerEntity>
    lateinit var FAKE_IRON_GOLEM: EntityType<FakeIronGolemEntity>
    lateinit var CARAVAN_TRADER: EntityType<CaravanTraderEntity>

    lateinit var ENDER_EYE_SMITHING_TEMPLATE: Item
    lateinit var REVEAL_SPYGLASS: Item

    fun id(path: String) = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

    fun registerEntities() {
        FAKE_SNOW_GOLEM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("fake_snow_golem"),
            EntityType.Builder.of({ type, level -> FakeSnowGolemEntity(type, level) }, MobCategory.MONSTER)
                .sized(0.7f, 1.9f)
                .clientTrackingRange(8)
                .build("fake_snow_golem")
        )

        FAKE_VILLAGER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("fake_villager"),
            EntityType.Builder.of({ type, level -> FakeVillagerEntity(type, level) }, MobCategory.CREATURE)
                .sized(0.6f, 1.95f)
                .clientTrackingRange(8)
                .build("fake_villager")
        )

        FAKE_IRON_GOLEM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("fake_iron_golem"),
            EntityType.Builder.of({ type, level -> FakeIronGolemEntity(type, level) }, MobCategory.CREATURE)
                .sized(1.4f, 2.7f)
                .clientTrackingRange(10)
                .build("fake_iron_golem")
        )

        CARAVAN_TRADER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("caravan_trader"),
            EntityType.Builder.of({ type, level -> CaravanTraderEntity(type, level) }, MobCategory.CREATURE)
                .sized(0.6f, 1.95f)
                .clientTrackingRange(8)
                .build("caravan_trader")
        )
    }

    fun registerItems() {
        ENDER_EYE_SMITHING_TEMPLATE = Registry.register(
            BuiltInRegistries.ITEM, id("ender_eye_smithing_template"),
            SmithingTemplateItem(
                Component.translatable("item.xbce.ender_eye_smithing_template.applies_to"),
                Component.translatable("item.xbce.ender_eye_smithing_template.ingredients"),
                Component.translatable("item.xbce.ender_eye_smithing_template.upgrade_description").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.xbce.ender_eye_smithing_template.base_slot_description"),
                Component.translatable("item.xbce.ender_eye_smithing_template.additions_slot_description"),
                listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_emerald")),
                listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_redstone_dust"))
            )
        )

        REVEAL_SPYGLASS = Registry.register(
            BuiltInRegistries.ITEM, id("reveal_spyglass"),
            RevealSpyglassItem(Item.Properties().stacksTo(1))
        )
    }
}
