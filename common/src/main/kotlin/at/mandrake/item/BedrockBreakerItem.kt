package at.mandrake.item

import at.mandrake.worldgen.CoreDimensionHandler
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks

class BedrockBreakerItem(properties: Properties) : Item(properties) {

    private val crackStages = HashMap<BlockPos, Int>()

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos

        if (!level.getBlockState(pos).`is`(Blocks.BEDROCK)) return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS

        val serverLevel = level as ServerLevel
        val stage = (crackStages[pos] ?: 0) + 1

        if (stage < MAX_STAGES) {
            crackStages[pos] = stage
            serverLevel.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.8f, 0.5f + stage * 0.2f)
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                20 + stage * 10, 0.3, 0.3, 0.3, 0.1)
            return InteractionResult.SUCCESS
        }

        crackStages.remove(pos)
        serverLevel.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f)
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
            pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
            100, 0.5, 0.5, 0.5, 1.0)

        CoreDimensionHandler.createPortal(serverLevel, pos)
        context.itemInHand.shrink(1)

        return InteractionResult.SUCCESS
    }

    companion object {
        private const val MAX_STAGES = 3
    }
}
