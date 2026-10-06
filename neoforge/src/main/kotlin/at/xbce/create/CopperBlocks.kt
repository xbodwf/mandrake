package at.xbce.create

import at.xbce.XBCE
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

/** 铜传动杆：真动力学（ShaftBlock），带氧化/涂蜡/刮除。 */
class CopperShaftBlock(
    override val family: CopperFamily,
    override val stage: CopperStage,
    override val finish: CopperFinish,
    override val waxed: Boolean,
    properties: BlockBehaviour.Properties
) : ShaftBlock(properties), CopperOxidizableBlock {

    override fun variant(stage: CopperStage, waxed: Boolean): Block = XBCECopperBlocks.variant(family, stage, waxed)

    override fun getBlockEntityType(): BlockEntityType<out KineticBlockEntity> = XBCECopperBlocks.beType()

    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hitResult: BlockHitResult
    ): ItemInteractionResult =
        CopperOxidation.useItemOn(this, stack, state, level, pos, player)
            ?: super.useItemOn(stack, state, level, pos, player, hand, hitResult)

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) =
        CopperOxidation.randomTick(this, state, level, pos, random)
}

/** 铜齿轮 / 铜大齿轮：真动力学（CogWheelBlock），同样带氧化/涂蜡。 */
class CopperCogwheelBlock(
    override val family: CopperFamily,
    override val stage: CopperStage,
    override val finish: CopperFinish,
    override val waxed: Boolean,
    large: Boolean,
    properties: BlockBehaviour.Properties
) : CogWheelBlock(large, properties), CopperOxidizableBlock {

    override fun variant(stage: CopperStage, waxed: Boolean): Block = XBCECopperBlocks.variant(family, stage, waxed)

    override fun getBlockEntityType(): BlockEntityType<out KineticBlockEntity> = XBCECopperBlocks.beType()

    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hitResult: BlockHitResult
    ): ItemInteractionResult =
        CopperOxidation.useItemOn(this, stack, state, level, pos, player)
            ?: super.useItemOn(stack, state, level, pos, player, hand, hitResult)

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) =
        CopperOxidation.randomTick(this, state, level, pos, random)
}

/**
 * 注册 3 工艺 × 3 几何 × 4 氧化 × 2 涂蜡 = 72 个铜动力学方块。
 * 仅在 Create 存在时由 XBCENeoForge 调用 init。
 */
object XBCECopperBlocks {

    private val blocksReg = DeferredRegister.createBlocks(XBCE.MOD_ID)
    private val itemsReg = DeferredRegister.createItems(XBCE.MOD_ID)
    private val beReg = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, XBCE.MOD_ID)

    private val byKey = HashMap<Triple<CopperFamily, CopperStage, Boolean>, Supplier<out Block>>()
    private val allBlockHolders = ArrayList<Supplier<out Block>>()
    private val tabItems = ArrayList<Supplier<out Item>>()

    private lateinit var beTypeInstance: BlockEntityType<BracketedKineticBlockEntity>

    lateinit var BE_TYPE: DeferredHolder<BlockEntityType<*>, BlockEntityType<BracketedKineticBlockEntity>>
        private set

    fun beType(): BlockEntityType<BracketedKineticBlockEntity> = BE_TYPE.get()

    fun blockHolders(): List<Supplier<out Block>> = allBlockHolders

    fun variant(family: CopperFamily, stage: CopperStage, waxed: Boolean): Block =
        byKey.getValue(Triple(family, stage, waxed)).get()

    fun init(modBus: IEventBus) {
        for (stage in CopperStage.entries) {
            for (finish in CopperFinish.entries) {
                val base = copperBaseName(stage, finish)
                for (waxed in listOf(false, true)) {
                    val prefix = if (waxed) "waxed_" else ""

                    val shaft = blocksReg.registerBlock("${prefix}${base}_shaft") {
                        CopperShaftBlock(CopperFamily.SHAFT, stage, finish, waxed, shaftProps())
                    }
                    val cog = blocksReg.registerBlock("${prefix}${base}_cogwheel") {
                        CopperCogwheelBlock(CopperFamily.COGWHEEL, stage, finish, waxed, false, cogProps())
                    }
                    val large = blocksReg.registerBlock("${prefix}${base}_large_cogwheel") {
                        CopperCogwheelBlock(CopperFamily.LARGE_COGWHEEL, stage, finish, waxed, true, cogProps())
                    }

                    val shaftItem = itemsReg.register("${prefix}${base}_shaft", Supplier { BlockItem(shaft.get(), Item.Properties()) })
                    val cogItem = itemsReg.register("${prefix}${base}_cogwheel", Supplier { CogwheelBlockItem(cog.get(), Item.Properties()) })
                    val largeItem = itemsReg.register("${prefix}${base}_large_cogwheel", Supplier { CogwheelBlockItem(large.get(), Item.Properties()) })

                    byKey[Triple(CopperFamily.SHAFT, stage, waxed)] = shaft
                    byKey[Triple(CopperFamily.COGWHEEL, stage, waxed)] = cog
                    byKey[Triple(CopperFamily.LARGE_COGWHEEL, stage, waxed)] = large
                    allBlockHolders.add(shaft); allBlockHolders.add(cog); allBlockHolders.add(large)
                    tabItems.add(shaftItem); tabItems.add(cogItem); tabItems.add(largeItem)
                }
            }
        }

        BE_TYPE = beReg.register("copper_kinetic", Supplier {
            val supplier = BlockEntityType.BlockEntitySupplier<BracketedKineticBlockEntity> { pos, state ->
                BracketedKineticBlockEntity(beTypeInstance, pos, state)
            }
            val type = BlockEntityType.Builder
                .of(supplier, *allBlockHolders.map { it.get() }.toTypedArray())
                .build(null)
            beTypeInstance = type
            type
        })

        blocksReg.register(modBus)
        itemsReg.register(modBus)
        beReg.register(modBus)

        // 加入自定义物品栏（注意：BuildCreativeModeTabContentsEvent 属于 mod 总线事件）
        modBus.addListener { event: BuildCreativeModeTabContentsEvent ->
            if (event.tabKey == ResourceKey.create(Registries.CREATIVE_MODE_TAB, XBCE.id("main"))) {
                for (item in tabItems) event.accept(item.get())
            }
        }
    }

    private fun shaftProps(): BlockBehaviour.Properties =
        BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).mapColor(MapColor.METAL).forceSolidOff()

    private fun cogProps(): BlockBehaviour.Properties =
        BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).sound(SoundType.WOOD).mapColor(MapColor.DIRT)
}
