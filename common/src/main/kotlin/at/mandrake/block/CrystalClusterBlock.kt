package at.mandrake.block

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

class CrystalClusterBlock : Block(BlockBehaviour.Properties.of()
    .strength(0.5f, 1.0f)
    .lightLevel { 6 }
    .sound(SoundType.AMETHYST)
    .noOcclusion()
    .noCollission()
    .requiresCorrectToolForDrops())
