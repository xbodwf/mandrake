package at.mandrake.block

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

class GeyserVentBlock : Block(BlockBehaviour.Properties.of()
    .strength(2.0f, 6.0f)
    .lightLevel { 8 }
    .sound(SoundType.STONE)
    .requiresCorrectToolForDrops()
) {
    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                pos.x + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                pos.y + 1.0,
                pos.z + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                0.0, 0.1 + random.nextDouble() * 0.1, 0.0)
        }
    }
}
