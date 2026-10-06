package at.xbce

import at.xbce.entity.CyanStaringFishEntity
import at.xbce.entity.FakeSnowGolemEntity
import at.xbce.entity.FakeVillagerEntity
import at.xbce.item.RevealSpyglassItem
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.item.Item
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.item.SmithingTemplateItem
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object XBCE {
    const val MOD_ID = "xbce"

    lateinit var FAKE_SNOW_GOLEM: EntityType<FakeSnowGolemEntity>
    lateinit var FAKE_VILLAGER: EntityType<FakeVillagerEntity>
    lateinit var CYAN_STARING_FISH: EntityType<CyanStaringFishEntity>

    lateinit var ENDER_EYE_SMITHING_TEMPLATE: Item
    lateinit var REVEAL_SPYGLASS: Item
    lateinit var FAKE_SNOW_GOLEM_SPAWN_EGG: Item
    lateinit var FAKE_VILLAGER_SPAWN_EGG: Item
    lateinit var CYAN_STARING_FISH_SPAWN_EGG: Item

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

        CYAN_STARING_FISH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("cyan_staring_fish"),
            EntityType.Builder.of({ type, level -> CyanStaringFishEntity(type, level) }, MobCategory.WATER_AMBIENT)
                .sized(0.5f, 0.3f)
                .clientTrackingRange(8)
                .build("cyan_staring_fish")
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

        // 颜色填白色：纹理本身已上色（26.2 风格），避免被原版刷怪蛋 tint 二次染色
        FAKE_SNOW_GOLEM_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM, id("fake_snow_golem_spawn_egg"),
            SpawnEggItem(FAKE_SNOW_GOLEM, 0xFFFFFF, 0xFFFFFF, Item.Properties())
        )

        FAKE_VILLAGER_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM, id("fake_villager_spawn_egg"),
            SpawnEggItem(FAKE_VILLAGER, 0xFFFFFF, 0xFFFFFF, Item.Properties())
        )

        CYAN_STARING_FISH_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM, id("cyan_staring_fish_spawn_egg"),
            SpawnEggItem(CYAN_STARING_FISH, 0x7FE9FF, 0x1E8FB5, Item.Properties())
        )
    }
}
