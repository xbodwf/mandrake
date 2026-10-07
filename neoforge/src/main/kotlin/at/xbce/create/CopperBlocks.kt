package at.xbce.create

import at.xbce.XBCE
import com.mojang.logging.LogUtils
import com.simibubi.create.content.equipment.wrench.IWrenchable
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
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.InteractionResult
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

    override fun variant(stage: CopperStage, waxed: Boolean): Block = XBCECopperBlocks.variant(family, stage, finish, waxed)

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

    override fun variant(stage: CopperStage, waxed: Boolean): Block = XBCECopperBlocks.variant(family, stage, finish, waxed)

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

/** 铜机壳：非动力学装饰方块，天然氧化 + 涂蜡/刮蜡；扳手行为与 Create 机壳一致。 */
class CopperCasingBlock(
    override val stage: CopperStage,
    override val waxed: Boolean,
    properties: BlockBehaviour.Properties
) : Block(properties), CopperOxidizableBlock, IWrenchable {

    override val family: CopperFamily = CopperFamily.CASING
    override val finish: CopperFinish = CopperFinish.PLAIN

    override fun variant(stage: CopperStage, waxed: Boolean): Block = XBCECopperBlocks.variant(family, stage, finish, waxed)

    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hitResult: BlockHitResult
    ): ItemInteractionResult =
        CopperOxidation.useItemOn(this, stack, state, level, pos, player)
            ?: super.useItemOn(stack, state, level, pos, player, hand, hitResult)

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) =
        CopperOxidation.randomTick(this, state, level, pos, random)

    // 同 Create 的 CasingBlock：普通右键扳手无效，潜行右键拆卸走 IWrenchable 默认实现
    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult = InteractionResult.FAIL
}

/**
 * 注册 3 工艺 × 3 几何 × 4 氧化 × 2 涂蜡 = 72 个铜动力学方块，
 * 外加 7 个铜机壳（非动力学：涂蜡纯机壳 + 斑驳/锈蚀/氧化 × 2 涂蜡），共 79 个。
 * 第 8 个"纯铜机壳"直接用官方的 create:copper_casing，它的氧化/打蜡由 CopperCasingMixin 接管。
 * 仅在 Create 存在时由 XBCENeoForge 调用 init。
 */
object XBCECopperBlocks {

    private val LOGGER = LogUtils.getLogger()

    private val blocksReg = DeferredRegister.createBlocks(XBCE.MOD_ID)
    private val itemsReg = DeferredRegister.createItems(XBCE.MOD_ID)
    private val beReg = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, XBCE.MOD_ID)

    /** (家族, 氧化阶段, 工艺, 涂蜡) 才能唯一确定一个方块：工艺必须进键，否则切制/雕纹会互相覆盖。 */
    private data class VariantKey(
        val family: CopperFamily,
        val stage: CopperStage,
        val finish: CopperFinish,
        val waxed: Boolean
    )

    private val byKey = HashMap<VariantKey, Supplier<out Block>>()
    private val allBlockHolders = ArrayList<Supplier<out Block>>()
    private val casingHolders = ArrayList<Supplier<out Block>>()
    private val tabItems = ArrayList<Supplier<out Item>>()

    private lateinit var beTypeInstance: BlockEntityType<BracketedKineticBlockEntity>

    lateinit var BE_TYPE: DeferredHolder<BlockEntityType<*>, BlockEntityType<BracketedKineticBlockEntity>>
        private set

    fun beType(): BlockEntityType<BracketedKineticBlockEntity> = BE_TYPE.get()

    fun blockHolders(): List<Supplier<out Block>> = allBlockHolders

    /** 铜机壳方块（无 BE，不进动力学类型）——供客户端注册连接纹理用。 */
    fun casingHolders(): List<Supplier<out Block>> = casingHolders

    fun variant(family: CopperFamily, stage: CopperStage, finish: CopperFinish, waxed: Boolean): Block =
        byKey.getValue(VariantKey(family, stage, finish, waxed)).get()

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

                    byKey[VariantKey(CopperFamily.SHAFT, stage, finish, waxed)] = shaft
                    byKey[VariantKey(CopperFamily.COGWHEEL, stage, finish, waxed)] = cog
                    byKey[VariantKey(CopperFamily.LARGE_COGWHEEL, stage, finish, waxed)] = large
                    allBlockHolders.add(shaft); allBlockHolders.add(cog); allBlockHolders.add(large)
                    tabItems.add(shaftItem); tabItems.add(cogItem); tabItems.add(largeItem)

                    // 铜机壳只有一种工艺（光面），非动力学（无 BE）。
                    // 初始的"纯铜机壳"直接用官方的 create:copper_casing，不再注册一个重复方块：
                    // 它的自然氧化/打蜡由 at.xbce.mixin.CopperCasingMixin 接管。
                    // 其余 7 个变体（涂蜡纯机壳 + 斑驳/锈蚀/氧化 × 涂蜡）照常注册。
                    if (finish == CopperFinish.PLAIN) {
                        if (stage == CopperStage.COPPER && !waxed) {
                            byKey[VariantKey(CopperFamily.CASING, CopperStage.COPPER, CopperFinish.PLAIN, false)] =
                                Supplier { com.simibubi.create.AllBlocks.COPPER_CASING.get() }
                        } else {
                            val casing = blocksReg.registerBlock("${prefix}${base}_casing") {
                                CopperCasingBlock(stage, waxed, casingProps(stage))
                            }
                            val casingItem = itemsReg.register("${prefix}${base}_casing", Supplier { BlockItem(casing.get(), Item.Properties()) })
                            byKey[VariantKey(CopperFamily.CASING, stage, CopperFinish.PLAIN, waxed)] = casing
                            casingHolders.add(casing)
                            tabItems.add(casingItem)
                        }
                    }
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

        // 诊断：确认 Create 专用的 CopperCasingMixin 是否真的合并进了 CasingBlock。
        // （打蜡另有 data/xbce/data_maps/block/waxables.json 兜底；但自然氧化只能靠 mixin。）
        // 注意：这里只能按类名反射。mod 构造期注册表项还没绑定，碰 AllBlocks.COPPER_CASING.get() 会
        // 抛 "Trying to access unbound value: create:copper_casing"，不能用来做启动期检测。
        try {
            val casingClass = Class.forName("com.simibubi.create.content.decoration.encasing.CasingBlock")
            val mixinApplied = casingClass.declaredMethods.any { it.name == "xbce\$isOfficialCopperCasing" }
            if (mixinApplied) {
                LOGGER.info("[xbce] CopperCasingMixin 已生效：create:copper_casing 会自然氧化，且支持蜜脾打蜡/斧头刮蜡")
            } else {
                LOGGER.warn("[xbce] CopperCasingMixin 未生效：create:copper_casing 不会自然氧化（检查 xbce.create.mixins.json 是否被加载）")
            }
        } catch (e: Throwable) {
            LOGGER.warn("[xbce] 无法检测 CopperCasingMixin 状态：{}", e.toString())
        }
    }

    // 注意：ofFullCopy 会连 isRandomlyTicking 一起抄（ANDESITE=false），必须显式 randomTicks，
    // 否则 randomTick 永远不会触发、方块无法自然氧化。
    private fun shaftProps(): BlockBehaviour.Properties =
        BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).mapColor(MapColor.METAL).forceSolidOff().randomTicks()

    private fun cogProps(): BlockBehaviour.Properties =
        BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).sound(SoundType.WOOD).mapColor(MapColor.DIRT).randomTicks()

    // 对齐原版铜块属性（Blocks.COPPER_BLOCK）：mapColor 随氧化阶段变化，需要正确工具才掉落数
    private fun casingProps(stage: CopperStage): BlockBehaviour.Properties {
        val color = when (stage) {
            CopperStage.COPPER -> MapColor.COLOR_ORANGE
            CopperStage.EXPOSED -> MapColor.TERRACOTTA_LIGHT_GRAY
            CopperStage.WEATHERED -> MapColor.WARPED_STEM
            CopperStage.OXIDIZED -> MapColor.WARPED_NYLIUM
        }
        return BlockBehaviour.Properties.of()
            .mapColor(color)
            .requiresCorrectToolForDrops()
            .strength(3.0f, 6.0f)
            .sound(SoundType.COPPER)
            .randomTicks()
    }
}
