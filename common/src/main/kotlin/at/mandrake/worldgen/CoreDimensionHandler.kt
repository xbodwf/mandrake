package at.mandrake.worldgen

import at.mandrake.Mandrake
import at.mandrake.block.CorePortalBlock
import at.mandrake.registry.ModDimensions
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks

object CoreDimensionHandler {

    fun createPortal(overworld: ServerLevel, bedrockPos: BlockPos) {
        val poolMin = bedrockPos
        val poolMax = bedrockPos.offset(1, 0, 1)

        for (dx in 0..1) for (dz in 0..1) {
            overworld.setBlock(
                bedrockPos.offset(dx, 0, dz),
                Mandrake.CORE_PORTAL_BLOCK.defaultBlockState(), 3
            )
        }

        val coreLevel = overworld.server?.getLevel(ModDimensions.EARTH_CORE_KEY) ?: return
        val cx = bedrockPos.x shr 4; val cz = bedrockPos.z shr 4
        coreLevel.getChunk(cx, cz)

        // Find an exposed floor in the main cavern instead of the first solid block.
        // The old top-down scan placed portals inside terrain and ignored most of the 640-block height.
        var coreY = 64
        for (y in 400 downTo -48) {
            val floor = BlockPos(bedrockPos.x, y, bedrockPos.z)
            val hasFloor = !coreLevel.getBlockState(floor.below()).isAir
            val hasHeadroom = (0..2).all { coreLevel.getBlockState(floor.above(it)).isAir }
            if (hasFloor && hasHeadroom) {
                coreY = y
                break
            }
        }
        val corePos = BlockPos(bedrockPos.x, coreY, bedrockPos.z)
        for (dx in 0..1) for (dz in 0..1) {
            coreLevel.setBlock(
                corePos.offset(dx, 0, dz),
                Mandrake.CORE_PORTAL_BLOCK.defaultBlockState(), 3
            )
        }
        for (dx in 0..1) for (dz in 0..1) {
            coreLevel.setBlock(
                corePos.offset(dx, -1, dz),
                Blocks.OBSIDIAN.defaultBlockState(), 3
            )
        }
        for (dx in 0..1) for (dz in 0..1) for (dy in 1..2) {
            coreLevel.setBlock(
                corePos.offset(dx, dy, dz),
                Blocks.AIR.defaultBlockState(), 3
            )
        }

        for (dx in -1..2) for (dz in -1..2) {
            if (dx in 0..1 && dz in 0..1) continue
            val framePos = corePos.offset(dx, 0, dz)
            coreLevel.setBlock(framePos, Blocks.SCULK.defaultBlockState(), 3)
            val isCorner = (dx == -1 && dz == -1) || (dx == -1 && dz == 2) ||
                           (dx == 2 && dz == -1) || (dx == 2 && dz == 2)
            if (!isCorner) {
                coreLevel.setBlock(framePos.above(), Blocks.SOUL_TORCH.defaultBlockState(), 3)
                coreLevel.setBlock(framePos.above(2), Blocks.AIR.defaultBlockState(), 3)
            }
        }

        PortalCache.getOrCreate(overworld).store(poolMin, poolMax, corePos, corePos.offset(1, 0, 1))
    }

    fun handlePortalBreak(level: ServerLevel, brokenPos: BlockPos) {
        val overworld = level.server!!.overworld()
        val cache = PortalCache.getOrCreate(overworld)
        val entry = cache.findEntry(brokenPos) ?: return
        val (overMin, overMax, coreMin, coreMax) = entry

        for (x in overMin.x..overMax.x) for (z in overMin.z..overMax.z) {
            CorePortalBlock.playerBroken.add(BlockPos(x, overMin.y, z))
        }
        val coreLevel = level.server!!.getLevel(ModDimensions.EARTH_CORE_KEY)
        if (coreLevel != null) {
            for (x in coreMin.x..coreMax.x) for (z in coreMin.z..coreMax.z) {
                CorePortalBlock.playerBroken.add(BlockPos(x, coreMin.y, z))
            }
        }

        CorePortalBlock.removing = true

        for (x in overMin.x..overMax.x) for (z in overMin.z..overMax.z) {
            val pos = BlockPos(x, overMin.y, z)
            if (overworld.getBlockState(pos).`is`(Mandrake.CORE_PORTAL_BLOCK)) {
                overworld.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
            }
        }

        if (coreLevel != null) {
            for (x in coreMin.x..coreMax.x) for (z in coreMin.z..coreMax.z) {
                val pos = BlockPos(x, coreMin.y, z)
                if (coreLevel.getBlockState(pos).`is`(Mandrake.CORE_PORTAL_BLOCK)) {
                    coreLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }

        CorePortalBlock.removing = false
        cache.removeEntry(entry)
    }
}
