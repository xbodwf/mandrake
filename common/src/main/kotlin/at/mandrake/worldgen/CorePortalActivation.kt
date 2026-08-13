package at.mandrake.worldgen

import at.mandrake.Mandrake
import at.mandrake.registry.ModDimensions
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.Vec3

object CorePortalActivation {
    private const val TICK_INTERVAL = 20
    private const val MAX_FRAME = 16
    private const val MAX_INTERIOR = 8
    private const val RANGE = 32.0

    fun playerTick(player: ServerPlayer) {
        val level = player.serverLevel()
        val dim = level.dimension()
        if (dim != Level.OVERWORLD && dim != ModDimensions.EARTH_CORE_KEY) return
        if (player.tickCount % TICK_INTERVAL != 0) return

        val items = level.getEntitiesOfClass(
            ItemEntity::class.java,
            player.boundingBox.inflate(RANGE)
        )

        for (item in items) {
            if (item.item.`is`(Items.NETHER_STAR) && item.isInWater && item.owner === player) {
                if (tryActivate(level, item)) return
            }
        }
    }

    private fun tryActivate(level: ServerLevel, star: ItemEntity): Boolean {
        val frame = findFrame(level, star.blockPosition()) ?: return false
        if (!validateSoulTorches(level, frame)) return false

        star.discard()

        val center = BlockPos(
            (frame.minX + frame.maxX) / 2,
            frame.y,
            (frame.minZ + frame.maxZ) / 2
        )

        val lightning = EntityType.LIGHTNING_BOLT.create(level)!!
        lightning.moveTo(Vec3.atBottomCenterOf(center))
        level.addFreshEntity(lightning)

        for (x in frame.minX + 1 until frame.maxX) {
            for (z in frame.minZ + 1 until frame.maxZ) {
                level.setBlock(
                    BlockPos(x, frame.y, z),
                    Mandrake.CORE_PORTAL_BLOCK.defaultBlockState(), 3
                )
            }
        }

        val goingToCore = level.dimension() == Level.OVERWORLD
        val pairedDimKey = if (goingToCore) ModDimensions.EARTH_CORE_KEY else Level.OVERWORLD
        val pairedLevel = level.server?.getLevel(pairedDimKey)
        if (pairedLevel != null) {
            val cx = center.x shr 4
            val cz = center.z shr 4
            pairedLevel.getChunk(cx, cz)

            var pairedY = 16
            for (y in 200 downTo 0) {
                if (!pairedLevel.getBlockState(BlockPos(center.x, y, center.z)).isAir) {
                    pairedY = y; break
                }
            }
            val pairedPos = BlockPos(center.x, pairedY, center.z)
            for (dx in 0..1) for (dz in 0..1) {
                pairedLevel.setBlock(
                    pairedPos.offset(dx, 0, dz),
                    Mandrake.CORE_PORTAL_BLOCK.defaultBlockState(), 3
                )
            }
            for (dx in 0..1) for (dz in 0..1) {
                pairedLevel.setBlock(
                    pairedPos.offset(dx, -1, dz),
                    Blocks.OBSIDIAN.defaultBlockState(), 3
                )
            }
            for (dx in 0..1) for (dz in 0..1) for (dy in 1..2) {
                pairedLevel.setBlock(
                    pairedPos.offset(dx, dy, dz),
                    Blocks.AIR.defaultBlockState(), 3
                )
            }

            for (dx in -1..2) for (dz in -1..2) {
                if (dx in 0..1 && dz in 0..1) continue
                val framePos = pairedPos.offset(dx, 0, dz)
                pairedLevel.setBlock(framePos, Blocks.SCULK.defaultBlockState(), 3)
                val isCorner = (dx == -1 && dz == -1) || (dx == -1 && dz == 2) ||
                               (dx == 2 && dz == -1) || (dx == 2 && dz == 2)
                if (!isCorner) {
                    pairedLevel.setBlock(framePos.above(), Blocks.SOUL_TORCH.defaultBlockState(), 3)
                    pairedLevel.setBlock(framePos.above(2), Blocks.AIR.defaultBlockState(), 3)
                }
            }

            val overworld = level.server!!.overworld()
            val cache = PortalCache.getOrCreate(overworld)
            val poolMin = BlockPos(frame.minX + 1, frame.y, frame.minZ + 1)
            val poolMax = BlockPos(frame.maxX - 1, frame.y, frame.maxZ - 1)
            val pairedMin = pairedPos
            val pairedMax = pairedPos.offset(1, 0, 1)
            if (goingToCore) {
                cache.store(poolMin, poolMax, pairedMin, pairedMax)
            } else {
                cache.store(pairedMin, pairedMax, poolMin, poolMax)
            }
        }

        return true
    }

    private data class Frame(val minX: Int, val maxX: Int, val minZ: Int, val maxZ: Int, val y: Int)

    private fun findFrame(level: ServerLevel, pos: BlockPos): Frame? {
        val y = findWaterY(level, pos) ?: return null

        val maxX = scanEdge(level, pos.x, y, pos.z, 1, 0)
        val minX = scanEdge(level, pos.x, y, pos.z, -1, 0)
        val maxZ = scanEdge(level, pos.x, y, pos.z, 0, 1)
        val minZ = scanEdge(level, pos.x, y, pos.z, 0, -1)
        if (maxX == null || minX == null || maxZ == null || minZ == null) return null

        val w = maxX - minX - 1
        val d = maxZ - minZ - 1
        if (w < 2 || d < 2 || w > MAX_INTERIOR || d > MAX_INTERIOR) return null

        val f = Frame(minX, maxX, minZ, maxZ, y)

        for (x in minX..maxX) for (z in minZ..maxZ) {
            if (x == minX || x == maxX || z == minZ || z == maxZ) {
                if ((x == minX && z == minZ) || (x == minX && z == maxZ) ||
                    (x == maxX && z == minZ) || (x == maxX && z == maxZ)) continue
                if (!level.getBlockState(BlockPos(x, y, z)).`is`(Blocks.SCULK)) return null
            }
        }

        for (x in minX + 1 until maxX) for (z in minZ + 1 until maxZ) {
            val fs = level.getFluidState(BlockPos(x, y, z))
            if (!fs.`is`(Fluids.WATER) || !fs.isSource) return null
        }

        return f
    }

    private fun scanEdge(level: ServerLevel, sx: Int, y: Int, sz: Int, dx: Int, dz: Int): Int? {
        var x = sx; var z = sz
        for (i in 1..MAX_FRAME) {
            x += dx; z += dz
            val pos = BlockPos(x, y, z)
            if (level.getBlockState(pos).`is`(Blocks.SCULK)) return if (dx != 0) x else z
            val fs = level.getFluidState(pos)
            if (!fs.`is`(Fluids.WATER) || !fs.isSource) return null
        }
        return null
    }

    private fun findWaterY(level: ServerLevel, pos: BlockPos): Int? {
        for (dy in -1..1) {
            val fs = level.getFluidState(pos.offset(0, dy, 0))
            if (fs.`is`(Fluids.WATER) && fs.isSource) return pos.y + dy
        }
        return null
    }

    private fun validateSoulTorches(level: ServerLevel, frame: Frame): Boolean {
        for (x in frame.minX..frame.maxX) for (z in frame.minZ..frame.maxZ) {
            if (x == frame.minX || x == frame.maxX || z == frame.minZ || z == frame.maxZ) {
                if (x == frame.minX && z == frame.minZ) continue
                if (x == frame.minX && z == frame.maxZ) continue
                if (x == frame.maxX && z == frame.minZ) continue
                if (x == frame.maxX && z == frame.maxZ) continue
                if (!level.getBlockState(BlockPos(x, frame.y + 1, z)).`is`(Blocks.SOUL_TORCH)) return false
            }
        }
        return true
    }
}
