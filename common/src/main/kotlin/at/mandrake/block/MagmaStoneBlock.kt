package at.mandrake.block

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

class MagmaStoneBlock : Block(BlockBehaviour.Properties.of()
    .strength(3.0f, 9.0f)
    .lightLevel { 4 }
    .sound(SoundType.STONE)
    .requiresCorrectToolForDrops())
