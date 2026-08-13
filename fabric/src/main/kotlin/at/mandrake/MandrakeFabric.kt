package at.mandrake

import at.mandrake.block.CorePortalBlock
import at.mandrake.block.CorePortalBlockEntity
import at.mandrake.block.ModBlockEntityTypes
import at.mandrake.worldgen.CoreDimensionHandler
import at.mandrake.worldgen.CoreHazardHandler
import at.mandrake.worldgen.CorePortalActivation
import at.mandrake.worldgen.density.CoreCeilingFunction
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

class MandrakeFabric : ModInitializer {
    override fun onInitialize() {
        Mandrake.fabricRegisterAll()
        net.minecraft.core.Registry.register(
            BuiltInRegistries.DENSITY_FUNCTION_TYPE, Mandrake.id("core_ceiling"), CoreCeilingFunction.CODEC.codec()
        )
        ModBlockEntityTypes.CORE_PORTAL = net.minecraft.core.Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Mandrake.id("core_portal"),
            net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                ::CorePortalBlockEntity, Mandrake.CORE_PORTAL_BLOCK
            ).build(null)
        )

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register { entries ->
            entries.accept(Mandrake.ENDER_EYE_SMITHING_TEMPLATE)
        }
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register { entries ->
            entries.accept(Mandrake.BEDROCK_BREAKER)
        }
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register { entries ->
            entries.accept(Mandrake.CRYSTAL_BLOCK)
            entries.accept(Mandrake.CRYSTAL_CLUSTER)
            entries.accept(Mandrake.MAGMA_STONE)
            entries.accept(Mandrake.GLOWING_MUSHROOM_CAP)
            entries.accept(Mandrake.GLOWING_MUSHROOM_STEM)
            entries.accept(Mandrake.GEYSER_VENT)
        }

        LootTableEvents.MODIFY.register { key, tableBuilder, source, _ ->
            if (source.isBuiltin && key.location() == ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous")) {
                val pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Mandrake.ENDER_EYE_SMITHING_TEMPLATE))
                    .`when`(LootItemRandomChanceCondition.randomChance(0.1f))
                tableBuilder.withPool(pool)
            } else if (source.isBuiltin && key.location() == ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward")) {
                val pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Mandrake.ENDER_EYE_SMITHING_TEMPLATE))
                    .`when`(LootItemRandomChanceCondition.randomChance(0.025f))
                tableBuilder.withPool(pool)
            }
        }

        PlayerBlockBreakEvents.BEFORE.register { world, _, pos, state, _ ->
            if (world is ServerLevel) {
                if (state.`is`(Mandrake.CORE_PORTAL_BLOCK)) {
                    CoreDimensionHandler.handlePortalBreak(world, pos)
                } else if (state.`is`(Blocks.SCULK)) {
                    CoreDimensionHandler.handlePortalBreak(world, pos)
                }
            }
            true
        }

        ServerTickEvents.START_WORLD_TICK.register { world ->
            if (world is ServerLevel) {
                for (player in world.players()) {
                    CorePortalActivation.playerTick(player)
                    CoreHazardHandler.playerTick(player)
                }
                CorePortalBlock.onServerPostTick(world)
            }
        }
    }
}
