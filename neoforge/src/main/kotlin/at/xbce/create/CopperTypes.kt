package at.xbce.create

import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties

/** 铜工艺家族：氧化阶段（命名与行为照抄原版铜）。 */
enum class CopperStage(val word: String) {
    COPPER(""),
    EXPOSED("exposed"),
    WEATHERED("weathered"),
    OXIDIZED("oxidized");

    /** 下一氧化阶段；最后一级为 null。 */
    val next: CopperStage?
        get() = entries.getOrNull(ordinal + 1)

    /** 上一氧化阶段（斧头"除锈"往回退一级用）；最年轻的铜为 null。 */
    val previous: CopperStage?
        get() = entries.getOrNull(ordinal - 1)
}

/** 原版铜中"纯纹理工艺"（同形状换皮）才可映射到传动杆/齿轮上：光面 / 切制 / 雕纹。 */
enum class CopperFinish(val seg: String, val before: Boolean) {
    PLAIN("", false),
    CUT("cut", true),
    CHISELED("chiseled", true)
}

enum class CopperFamily(val suffix: String) {
    SHAFT("shaft"),
    COGWHEEL("cogwheel"),
    LARGE_COGWHEEL("large_cogwheel"),
    CASING("casing")
}

/** 生成与资源命名一致的基础名：copper / exposed_cut_copper / copper_grate ... */
fun copperBaseName(stage: CopperStage, finish: CopperFinish): String {
    val parts = ArrayList<String>(4)
    if (stage.word.isNotEmpty()) parts.add(stage.word)
    if (finish.before && finish.seg.isNotEmpty()) parts.add(finish.seg)
    parts.add("copper")
    if (!finish.before && finish.seg.isNotEmpty()) parts.add(finish.seg)
    return parts.joinToString("_")
}

interface CopperOxidizableBlock {
    val family: CopperFamily
    val stage: CopperStage
    val finish: CopperFinish
    val waxed: Boolean

    fun variant(stage: CopperStage, waxed: Boolean): Block
}

object CopperOxidation {

    private const val OXIDIZE_CHANCE = 0.05688889f

    /** 蜜脾打蜡 / 斧头刮蜡除锈；null 表示不处理（交还原版逻辑）。 */
    fun useItemOn(
        block: CopperOxidizableBlock,
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player
    ): ItemInteractionResult? {
        if (stack.`is`(Items.HONEYCOMB)) {
            if (block.waxed) return null
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, copyState(state, block.variant(block.stage, true)))
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_WAX_ON, pos, 0)
                awardWaxOn(player)
            }
            if (!player.abilities.instabuild) stack.shrink(1)
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }
        if (stack.item is AxeItem) {
            // 与原版一致：斧头只做两件事——刮蜡、除锈（往回退一个氧化阶段）。
            // 未涂蜡、又已经是最年轻的铜（没有上一级）时什么都不做，交还原版逻辑。
            val target = when {
                block.waxed -> block.variant(block.stage, false)
                block.stage.previous != null -> block.variant(block.stage.previous!!, false)
                else -> null
            } ?: return null
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, copyState(state, target))
                level.levelEvent(
                    if (block.waxed) LevelEvent.PARTICLES_WAX_OFF else LevelEvent.PARTICLES_SCRAPE,
                    pos, 0
                )
                if (block.waxed) awardWaxOff(player)
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)
            }
            level.playSound(
                player, pos,
                if (block.waxed) SoundEvents.AXE_WAX_OFF else SoundEvents.AXE_SCRAPE,
                SoundSource.BLOCKS, 1.0f, 1.0f
            )
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }
        return null
    }

    /** 自然氧化：未涂蜡时按原版铜块的概率推进一个阶段。 */
    fun randomTick(
        block: CopperOxidizableBlock,
        state: BlockState,
        level: ServerLevel,
        pos: BlockPos,
        random: RandomSource
    ) {
        if (block.waxed) return
        val next = block.stage.next ?: return
        if (random.nextFloat() < OXIDIZE_CHANCE) {
            level.setBlockAndUpdate(pos, copyState(state, block.variant(next, false)))
        }
    }

    // 以下两个方法由 CopperCasingMixin 调用：把官方 create:copper_casing 直接接入我们的氧化链，
    // 这样模组就不必再注册一个与官方重复的"纯铜机壳"方块。

    /** 官方机壳的自然氧化：按原版铜块的概率推进到 xbce:exposed_copper_casing。 */
    fun tickOfficialCasing(level: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (random.nextFloat() < OXIDIZE_CHANCE) {
            level.setBlockAndUpdate(
                pos,
                XBCECopperBlocks.variant(CopperFamily.CASING, CopperStage.EXPOSED, CopperFinish.PLAIN, false).defaultBlockState()
            )
        }
    }

    /** 官方机壳打蜡 → xbce:waxed_copper_casing（并授予原版"涂蜡 (Wax On)"成就）。 */
    fun waxOfficialCasing(level: Level, pos: BlockPos, player: Player, stack: ItemStack): ItemInteractionResult {
        if (!level.isClientSide) {
            level.setBlockAndUpdate(
                pos,
                XBCECopperBlocks.variant(CopperFamily.CASING, CopperStage.COPPER, CopperFinish.PLAIN, true).defaultBlockState()
            )
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_WAX_ON, pos, 0)
            if (!player.abilities.instabuild) stack.shrink(1)
            awardWaxOn(player)
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide)
    }

    private fun copyState(from: BlockState, toBlock: Block): BlockState {
        var s = toBlock.defaultBlockState()
        if (from.hasProperty(BlockStateProperties.AXIS) && s.hasProperty(BlockStateProperties.AXIS)) {
            s = s.setValue(BlockStateProperties.AXIS, from.getValue(BlockStateProperties.AXIS))
        }
        if (from.hasProperty(BlockStateProperties.WATERLOGGED) && s.hasProperty(BlockStateProperties.WATERLOGGED)) {
            s = s.setValue(BlockStateProperties.WATERLOGGED, from.getValue(BlockStateProperties.WATERLOGGED))
        }
        return s
    }

    // 原版官方成就：涂蜡 "Wax On" / 刮蜡 "Wax Off"。其条件写死了原版铜方块列表，
    // 收不到我们方块的 trigger，所以在服务端直接按 criteria 名授予。
    private val WAX_ON_ADVANCEMENT = ResourceLocation.withDefaultNamespace("husbandry/wax_on")
    private val WAX_OFF_ADVANCEMENT = ResourceLocation.withDefaultNamespace("husbandry/wax_off")

    /** 涂蜡成功后授予原版成就"涂蜡 (Wax On)"（给本家族方块与官方铜机壳转换共用）。 */
    fun awardWaxOn(player: Player) = awardCopperAdvancement(player, WAX_ON_ADVANCEMENT, "wax_on")

    /** 刮蜡成功后授予原版成就"刮蜡 (Wax Off)"。 */
    fun awardWaxOff(player: Player) = awardCopperAdvancement(player, WAX_OFF_ADVANCEMENT, "wax_off")

    private fun awardCopperAdvancement(player: Player, id: ResourceLocation, criterion: String) {
        val serverPlayer = player as? ServerPlayer ?: return
        val advancement = serverPlayer.server?.advancements?.get(id) ?: return
        serverPlayer.advancements.award(advancement, criterion)
    }
}
