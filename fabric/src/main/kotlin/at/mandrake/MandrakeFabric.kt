package at.mandrake

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

class MandrakeFabric : ModInitializer {
    override fun onInitialize() {
        Mandrake.fabricRegisterAll()

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register { entries ->
            entries.accept(Mandrake.ENDER_EYE_SMITHING_TEMPLATE)
        }

        LootTableEvents.MODIFY.register { key, tableBuilder, source, _ ->
            if (source.isBuiltin && key.location() == ResourceLocation.withDefaultNamespace("chests/ancient_city")) {
                val pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Mandrake.ENDER_EYE_SMITHING_TEMPLATE))
                    .`when`(LootItemRandomChanceCondition.randomChance(0.25f))
                tableBuilder.withPool(pool)
            } else if (source.isBuiltin && key.location() == ResourceLocation.withDefaultNamespace("chests/ancient_city_ice_box")) {
                val pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Mandrake.ENDER_EYE_SMITHING_TEMPLATE))
                    .`when`(LootItemRandomChanceCondition.randomChance(0.25f))
                tableBuilder.withPool(pool)
            }
        }
    }
}
