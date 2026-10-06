package at.xbce

import at.xbce.entity.CyanStaringFishEntity
import at.xbce.entity.FakeSnowGolemEntity
import at.xbce.entity.FakeVillagerEntity
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

class XBCEFabric : ModInitializer {
    override fun onInitialize() {
        XBCE.registerEntities()
        XBCE.registerItems()

        // 自定义物品栏（NeoForge 侧用其 builder()，Fabric 侧用 FabricItemGroup）
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            XBCE.id("main"),
            FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.xbce.main"))
                .icon { ItemStack(XBCE.REVEAL_SPYGLASS) }
                .displayItems { _, output ->
                    output.accept(XBCE.ENDER_EYE_SMITHING_TEMPLATE)
                    output.accept(XBCE.REVEAL_SPYGLASS)
                    output.accept(XBCE.FAKE_SNOW_GOLEM_SPAWN_EGG)
                    output.accept(XBCE.FAKE_VILLAGER_SPAWN_EGG)
                    output.accept(XBCE.CYAN_STARING_FISH_SPAWN_EGG)
                }
                .build()
        )

        FabricDefaultAttributeRegistry.register(XBCE.FAKE_SNOW_GOLEM, FakeSnowGolemEntity.createAttributes().build())
        FabricDefaultAttributeRegistry.register(XBCE.FAKE_VILLAGER, FakeVillagerEntity.createAttributes().build())
        FabricDefaultAttributeRegistry.register(XBCE.CYAN_STARING_FISH, CyanStaringFishEntity.createAttributes().build())

        // 天青色瞪眼鱼的凝视判定（服务端每 tick）
        ServerTickEvents.END_SERVER_TICK.register { server ->
            CyanFishStare.tick(server)
        }

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register { entries ->
            entries.accept(XBCE.ENDER_EYE_SMITHING_TEMPLATE)
        }
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register { entries ->
            entries.accept(XBCE.REVEAL_SPYGLASS)
        }
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register { entries ->
            entries.accept(XBCE.FAKE_SNOW_GOLEM_SPAWN_EGG)
            entries.accept(XBCE.FAKE_VILLAGER_SPAWN_EGG)
            entries.accept(XBCE.CYAN_STARING_FISH_SPAWN_EGG)
        }

        LootTableEvents.MODIFY.register { key, tableBuilder, source, _ ->
            if (source.isBuiltin && key.location() == ResourceLocation.withDefaultNamespace("chests/ancient_city")) {
                val pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(XBCE.ENDER_EYE_SMITHING_TEMPLATE))
                    .`when`(LootItemRandomChanceCondition.randomChance(0.08f))
                tableBuilder.withPool(pool)
            } else if (source.isBuiltin && key.location() == ResourceLocation.withDefaultNamespace("chests/ancient_city_ice_box")) {
                val pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(XBCE.ENDER_EYE_SMITHING_TEMPLATE))
                    .`when`(LootItemRandomChanceCondition.randomChance(0.08f))
                tableBuilder.withPool(pool)
            }
        }
    }
}
