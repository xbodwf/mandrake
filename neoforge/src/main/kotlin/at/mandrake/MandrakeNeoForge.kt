package at.mandrake

import at.mandrake.block.*
import at.mandrake.item.BedrockBreakerItem
import at.mandrake.worldgen.CoreDimensionHandler
import at.mandrake.worldgen.CoreHazardHandler
import at.mandrake.worldgen.CorePortalActivation
import at.mandrake.worldgen.density.CoreCeilingFunction
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.SmithingTemplateItem
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.LootTableLoadEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.fml.loading.FMLEnvironment
import java.util.function.Supplier

@Mod(Mandrake.MOD_ID)
class MandrakeNeoForge(modBus: IEventBus) {

    private val items = DeferredRegister.createItems(Mandrake.MOD_ID)
    private val blocks = DeferredRegister.create(Registries.BLOCK, Mandrake.MOD_ID)
    private val blockEntityTypes = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mandrake.MOD_ID)

    private val enderEyeTemplate: DeferredItem<SmithingTemplateItem> = items.register("ender_eye_smithing_template",
        Supplier { SmithingTemplateItem(
            Component.translatable("item.mandrake.ender_eye_smithing_template.applies_to"),
            Component.translatable("item.mandrake.ender_eye_smithing_template.ingredients"),
            Component.translatable("item.mandrake.ender_eye_smithing_template.upgrade_description").withStyle(ChatFormatting.GRAY),
            Component.translatable("item.mandrake.ender_eye_smithing_template.base_slot_description"),
            Component.translatable("item.mandrake.ender_eye_smithing_template.additions_slot_description"),
            listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_emerald")),
            listOf(ResourceLocation.withDefaultNamespace("item/empty_slot_redstone_dust"))
        )}
    )

    private val bedrockBreaker: DeferredItem<BedrockBreakerItem> = items.register("bedrock_breaker",
        Supplier { BedrockBreakerItem(Item.Properties().stacksTo(1)) }
    )

    private val corePortalBlock: DeferredHolder<Block, CorePortalBlock> = blocks.register("core_portal",
        Supplier { CorePortalBlock() })
    private val crystalBlock: DeferredHolder<Block, CrystalBlock> = blocks.register("crystal_block",
        Supplier { CrystalBlock() })
    private val crystalCluster: DeferredHolder<Block, CrystalClusterBlock> = blocks.register("crystal_cluster",
        Supplier { CrystalClusterBlock() })
    private val magmaStone: DeferredHolder<Block, MagmaStoneBlock> = blocks.register("magma_stone",
        Supplier { MagmaStoneBlock() })
    private val glowingMushroomCap: DeferredHolder<Block, GlowingMushroomCapBlock> = blocks.register("glowing_mushroom_cap",
        Supplier { GlowingMushroomCapBlock() })
    private val glowingMushroomStem: DeferredHolder<Block, GlowingMushroomStemBlock> = blocks.register("glowing_mushroom_stem",
        Supplier { GlowingMushroomStemBlock() })
    private val geyserVent: DeferredHolder<Block, GeyserVentBlock> = blocks.register("geyser_vent",
        Supplier { GeyserVentBlock() })

    private val corePortalBlockEntityType = blockEntityTypes.register("core_portal",
        Supplier { BlockEntityType.Builder.of(::CorePortalBlockEntity, corePortalBlock.get()).build(null) }
    )

    init {
        items.register(modBus)
        blocks.register(modBus)
        blockEntityTypes.register(modBus)

        val p = Item.Properties()
        items.register("core_portal", Supplier { BlockItem(corePortalBlock.get(), p) })
        items.register("crystal_block", Supplier { BlockItem(crystalBlock.get(), p) })
        items.register("crystal_cluster", Supplier { BlockItem(crystalCluster.get(), p) })
        items.register("magma_stone", Supplier { BlockItem(magmaStone.get(), p) })
        items.register("glowing_mushroom_cap", Supplier { BlockItem(glowingMushroomCap.get(), p) })
        items.register("glowing_mushroom_stem", Supplier { BlockItem(glowingMushroomStem.get(), p) })
        items.register("geyser_vent", Supplier { BlockItem(geyserVent.get(), p) })

        modBus.addListener { event: RegisterEvent ->
            if (event.registryKey == Registries.ITEM) {
                Mandrake.ENDER_EYE_SMITHING_TEMPLATE = enderEyeTemplate.get()
                Mandrake.BEDROCK_BREAKER = bedrockBreaker.get()
            } else if (event.registryKey == Registries.BLOCK) {
                Mandrake.CORE_PORTAL_BLOCK = corePortalBlock.get()
                Mandrake.CRYSTAL_BLOCK = crystalBlock.get()
                Mandrake.CRYSTAL_CLUSTER = crystalCluster.get()
                Mandrake.MAGMA_STONE = magmaStone.get()
                Mandrake.GLOWING_MUSHROOM_CAP = glowingMushroomCap.get()
                Mandrake.GLOWING_MUSHROOM_STEM = glowingMushroomStem.get()
                Mandrake.GEYSER_VENT = geyserVent.get()
            } else if (event.registryKey == Registries.DENSITY_FUNCTION_TYPE) {
                event.register(Registries.DENSITY_FUNCTION_TYPE) { helper ->
                    helper.register(Mandrake.id("core_ceiling"), CoreCeilingFunction.CODEC.codec())
                }
            } else if (event.registryKey == Registries.BLOCK_ENTITY_TYPE) {
                @Suppress("UNCHECKED_CAST")
                val type = corePortalBlockEntityType.get() as BlockEntityType<CorePortalBlockEntity>
                ModBlockEntityTypes.CORE_PORTAL = type
            }
        }

        modBus.addListener(this::onCreativeTabContents)
        NeoForge.EVENT_BUS.addListener(this::onLootTableLoad)
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak)
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick)
        NeoForge.EVENT_BUS.addListener(this::onLevelPostTick)
    }

    private fun onCreativeTabContents(event: BuildCreativeModeTabContentsEvent) {
        when (event.tabKey) {
            CreativeModeTabs.INGREDIENTS -> event.accept(enderEyeTemplate.get())
            CreativeModeTabs.TOOLS_AND_UTILITIES -> event.accept(bedrockBreaker.get())
            CreativeModeTabs.BUILDING_BLOCKS -> {
                event.accept(crystalBlock.get())
                event.accept(crystalCluster.get())
                event.accept(magmaStone.get())
                event.accept(glowingMushroomCap.get())
                event.accept(glowingMushroomStem.get())
                event.accept(geyserVent.get())
            }
        }
    }

    private fun onLootTableLoad(event: LootTableLoadEvent) {
        if (event.name == ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous")) {
            val pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Mandrake.ENDER_EYE_SMITHING_TEMPLATE))
                .`when`(LootItemRandomChanceCondition.randomChance(0.1f))
            event.table.addPool(pool.build())
        } else if (event.name == ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward")) {
            val pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Mandrake.ENDER_EYE_SMITHING_TEMPLATE))
                .`when`(LootItemRandomChanceCondition.randomChance(0.025f))
            event.table.addPool(pool.build())
        }
    }

    private fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val level = event.level
        if (level is ServerLevel) {
            if (event.state.`is`(Mandrake.CORE_PORTAL_BLOCK)) {
                CoreDimensionHandler.handlePortalBreak(level, event.pos)
            } else if (event.state.`is`(Blocks.SCULK)) {
                CoreDimensionHandler.handlePortalBreak(level, event.pos)
            }
        }
    }

    private fun onPlayerTick(event: PlayerTickEvent.Post) {
        if (event.entity is ServerPlayer) {
            val player = event.entity as ServerPlayer
            CorePortalActivation.playerTick(player)
            CoreHazardHandler.playerTick(player)
        }
    }

    private fun onLevelPostTick(event: LevelTickEvent.Post) {
        if (event.level is ServerLevel) {
            CorePortalBlock.onServerPostTick(event.level as ServerLevel)
        }
    }
}
