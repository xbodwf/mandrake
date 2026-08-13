package at.mandrake.block

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

class GlowingMushroomCapBlock : Block(BlockBehaviour.Properties.of()
    .strength(1.0f, 3.0f)
    .lightLevel { 12 }
    .sound(SoundType.WOOD))
