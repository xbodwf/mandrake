package at.mandrake.block

import at.mandrake.registry.ModDimensions
import at.mandrake.worldgen.CoreDimensionHandler
import at.mandrake.worldgen.PortalCache
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HalfTransparentBlock
import net.minecraft.world.level.block.Portal
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.portal.DimensionTransition
import net.minecraft.world.phys.Vec3
import java.util.*

class CorePortalBlock : HalfTransparentBlock(
    BlockBehaviour.Properties.of()
        .strength(0.5f)
        .noLootTable()
        .noCollission()
        .lightLevel { 11 }
), Portal, EntityBlock {

    override fun entityInside(state: BlockState, level: Level, pos: BlockPos, entity: Entity) {
        if (level !is ServerLevel || entity.isPassenger) return
        if (entity.isOnPortalCooldown) return
        if (debounced.contains(entity.uuid)) return

        val goingToCore = level.dimension() == Level.OVERWORLD
        val target = if (goingToCore)
            level.server?.getLevel(ModDimensions.EARTH_CORE_KEY)
        else
            level.server?.getLevel(Level.OVERWORLD) ?: return

        if (target == null) return

        val cache = PortalCache.getOrCreate(level.server!!.overworld())
        val entry = cache.findEntry(pos)

        val tpPos = if (entry != null) {
            val (overMin, overMax, coreMin, coreMax) = entry
            val inOverworld = level.dimension() == Level.OVERWORLD
            val targetOrigin = if (inOverworld) {
                BlockPos((coreMin.x + coreMax.x) / 2, coreMin.y, (coreMin.z + coreMax.z) / 2)
            } else {
                BlockPos((overMin.x + overMax.x) / 2, overMin.y, (overMin.z + overMax.z) / 2)
            }
            findSafeExit(target, targetOrigin) ?: Vec3.atCenterOf(target.sharedSpawnPos)
        } else {
            Vec3.atCenterOf(target.sharedSpawnPos)
        }

        entity.setPortalCooldown()
        debounced.add(entity.uuid)
        entity.changeDimension(DimensionTransition(
            target, tpPos,
            entity.deltaMovement, entity.yRot, entity.xRot,
            false, DimensionTransition.DO_NOTHING
        ))
    }

    override fun getPortalTransitionTime(level: ServerLevel, entity: Entity) = 0

    override fun getPortalDestination(level: ServerLevel, entity: Entity, pos: BlockPos) = null

    override fun getLocalTransition() = Portal.Transition.CONFUSION

    override fun canBeReplaced(state: BlockState, fluid: Fluid) = false

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = CorePortalBlockEntity(pos, state)

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        super.onRemove(state, level, pos, newState, movedByPiston)
        if (!level.isClientSide && newState.isAir && playerBroken.remove(pos)) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3)
        }
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        if (level is ServerLevel && !removing && level.getBlockState(pos.below()).isAir) {
            CorePortalBlock.removing = true
            CoreDimensionHandler.handlePortalBreak(level, pos)
            CorePortalBlock.removing = false
        }
    }

    companion object {
        @JvmField var removing = false
        val playerBroken = HashSet<BlockPos>()

        private val debounced = HashSet<UUID>()

        fun onServerPostTick(level: ServerLevel) {
            val it = debounced.iterator()
            while (it.hasNext()) {
                val uuid = it.next()
                val entity = level.getEntity(uuid)
                if (entity == null) {
                    it.remove()
                    continue
                }
                val pos = entity.blockPosition()
                if (level.getBlockState(pos).block !is CorePortalBlock &&
                    level.getBlockState(pos.above()).block !is CorePortalBlock
                ) {
                    it.remove()
                }
            }
        }

        private fun findSafeExit(level: ServerLevel, center: BlockPos): Vec3? {
            val offsets = listOf(
                2 to 0, -2 to 0, 0 to 2, 0 to -2,
                3 to 0, -3 to 0, 0 to 3, 0 to -3
            )
            for ((dx, dz) in offsets) {
                val x = center.x + dx
                val z = center.z + dz
                for (dy in 0..3) {
                    val foot = BlockPos(x, center.y + dy, z)
                    val head = BlockPos(x, center.y + dy + 1, z)
                    val below = BlockPos(x, center.y + dy - 1, z)
                    if (level.getBlockState(foot).isAir &&
                        level.getBlockState(head).isAir &&
                        level.getBlockState(below).isCollisionShapeFullBlock(level, below)
                    ) {
                        return Vec3(x + 0.5, center.y + dy + 0.0, z + 0.5)
                    }
                }
            }
            return null
        }
    }
}
