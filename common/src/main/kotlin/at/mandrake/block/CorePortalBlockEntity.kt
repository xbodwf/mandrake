package at.mandrake.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class CorePortalBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(
    ModBlockEntityTypes.CORE_PORTAL ?: error("CorePortalBlockEntity type not registered"),
    pos, state
)

object ModBlockEntityTypes {
    @JvmField
    var CORE_PORTAL: BlockEntityType<CorePortalBlockEntity>? = null
}
