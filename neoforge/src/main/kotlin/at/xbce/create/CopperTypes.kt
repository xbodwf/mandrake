package at.xbce.create

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
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
    LARGE_COGWHEEL("large_cogwheel")
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
            }
            if (!player.abilities.instabuild) stack.shrink(1)
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }
        if (stack.item is AxeItem) {
            val target = when {
                block.waxed -> block.variant(block.stage, false)
                block.stage.next != null -> block.variant(block.stage.next!!, false)
                else -> null
            } ?: return null
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, copyState(state, target))
                level.levelEvent(
                    if (block.waxed) LevelEvent.PARTICLES_WAX_OFF else LevelEvent.PARTICLES_SCRAPE,
                    pos, 0
                )
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
}
