package at.mandrake.block

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

class CrystalBlock : Block(BlockBehaviour.Properties.of()
    .strength(1.5f, 6.0f)
    .lightLevel { 8 }
    .sound(SoundType.AMETHYST)
    .requiresCorrectToolForDrops())
