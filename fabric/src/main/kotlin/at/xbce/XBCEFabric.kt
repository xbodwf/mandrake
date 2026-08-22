package at.xbce

import at.xbce.entity.FakeIronGolemEntity
import at.xbce.entity.FakeSnowGolemEntity
import at.xbce.entity.FakeVillagerEntity
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

class XBCEFabric : ModInitializer {
    override fun onInitialize() {
        XBCE.registerEntities()
        XBCE.registerItems()

        FabricDefaultAttributeRegistry.register(XBCE.FAKE_SNOW_GOLEM, FakeSnowGolemEntity.createAttributes().build())
        FabricDefaultAttributeRegistry.register(XBCE.FAKE_VILLAGER, FakeVillagerEntity.createAttributes().build())
        FabricDefaultAttributeRegistry.register(XBCE.FAKE_IRON_GOLEM, FakeIronGolemEntity.createAttributes().build())

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register { entries ->
            entries.accept(XBCE.ENDER_EYE_SMITHING_TEMPLATE)
        }
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register { entries ->
            entries.accept(XBCE.REVEAL_SPYGLASS)
        }

        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register { entity, source ->
            at.xbce.XBCEAdvancements.onDeath(entity, source)
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
