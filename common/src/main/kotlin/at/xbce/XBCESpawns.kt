package at.xbce

import at.xbce.entity.CaravanTraderEntity
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.village.poi.PoiManager
import net.minecraft.world.entity.ai.village.poi.PoiTypes
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import kotlin.math.cos
import kotlin.math.sin

/**
 * 商队生成器：按游戏规则 xbceCaravanInterval 指定的间隔生成商队
 * （商队商人 + 两头拴绳的行商羊驼）。间隔设为 0 可完全禁用。
 */
object XBCESpawns {

    private val lastSpawnTick = HashMap<ResourceKey<Level>, Long>()

    fun tick(level: ServerLevel) {
        val interval = level.gameRules.getInt(XBCEGameRules.CARAVAN_INTERVAL).toLong()
        if (interval <= 0) return                 // 0 = 禁用商队
        if (level.players().isEmpty()) return

        val key = level.dimension()
        val last = lastSpawnTick[key]
        if (last != null && level.gameTime - last < interval) return
        // 到期后每 15 秒重试一次（村庄不在附近等条件不满足时）
        if (level.gameTime % 300L != 0L) return

        // 同维度最多 1 支商队
        var count = 0
        for (entity in level.allEntities) {
            if (entity is CaravanTraderEntity && entity.isAlive) count++
        }
        if (count >= 1) return

        val player = level.players()[level.random.nextInt(level.players().size)]
        val pois = level.poiManager.getInSquare(
            { h -> h.`is`(PoiTypes.MEETING) },
            player.blockPosition(), 96, PoiManager.Occupancy.ANY
        ).toList()
        if (pois.isEmpty()) return
        val startPoi = pois[level.random.nextInt(pois.size)].pos

        val angle = level.random.nextDouble() * 2.0 * Math.PI
        val dist = 24.0 + level.random.nextDouble() * 16.0
        val bx = (startPoi.x + 0.5 + cos(angle) * dist).toInt()
        val bz = (startPoi.z + 0.5 + sin(angle) * dist).toInt()
        val by = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz)
        val spawnPos = BlockPos(bx, by, bz)
        if (!Mob.checkMobSpawnRules(EntityType.WANDERING_TRADER, level, MobSpawnType.NATURAL, spawnPos, level.random)) return

        val trader = CaravanTraderEntity(XBCE.CARAVAN_TRADER, level)
        trader.moveTo(bx + 0.5, by.toDouble(), bz + 0.5, level.random.nextFloat() * 360.0f, 0.0f)
        trader.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, null as SpawnGroupData?)
        level.addFreshEntity(trader)

        // 行商羊驼随行（拴绳）
        repeat(2) {
            val llama = EntityType.TRADER_LLAMA.create(level) ?: return@repeat
            llama.moveTo(
                bx + 1.0 + it, by.toDouble(), bz - 1.0 + it,
                level.random.nextFloat() * 360.0f, 0.0f
            )
            level.addFreshEntity(llama)
            llama.setLeashedTo(trader, true)
        }

        lastSpawnTick[key] = level.gameTime
    }
}
