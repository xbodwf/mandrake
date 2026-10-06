package at.xbce

import at.xbce.entity.CyanStaringFishEntity
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * 天青色瞪眼鱼的凝视管理器：
 *  - 玩家与鱼四目相对 → 玩家被定身 3 秒（不受重力、原地悬浮），鱼同步静止；
 *  - 3 秒结束后有 0.5 秒挣脱窗口：移开视线即可脱身（并短暂免疫），
 *    不移开则被继续定住。
 * 由两个加载器在服务端 tick 中调用 [tick]。
 */
object CyanFishStare {

    private const val STUN_TICKS = 60        // 3 秒
    private const val GRACE_TICKS = 10       // 0.5 秒挣脱窗口
    private const val BREAK_COOLDOWN = 40    // 挣脱后 2 秒内不再被锁
    private const val MAX_DISTANCE = 24.0
    private const val VIEW_DOT = 0.99        // “正好对上”的视线锥
    private const val FISH_DOT = 0.85        // 鱼也要大致朝向你
    private const val PHASE_STUN = 0
    private const val PHASE_GRACE = 1

    private class Stare(
        val fishId: UUID,
        val dimension: ResourceKey<Level>,
        var lockX: Double,
        var lockY: Double,
        var lockZ: Double,
        var ticksLeft: Int = STUN_TICKS,
        var phase: Int = PHASE_STUN
    )

    private val ACTIVE = HashMap<UUID, Stare>()
    private val BREAK_COOLDOWN_UNTIL = HashMap<UUID, Long>()

    /** 眼睛是否对上（玩家看向鱼 + 鱼看向玩家 + 无遮挡）。 */
    fun eyeContact(fish: CyanStaringFishEntity, player: Player): Boolean {
        val playerEyes = player.getEyePosition(1.0f)
        val fishEyes = fish.getEyePosition(1.0f)
        val toFish = fishEyes.subtract(playerEyes)
        val distSqr = toFish.lengthSqr()
        if (distSqr < 0.25 || distSqr > MAX_DISTANCE * MAX_DISTANCE) return false
        val dirToFish = toFish.normalize()
        if (player.getViewVector(1.0f).dot(dirToFish) < VIEW_DOT) return false
        if (fish.getViewVector(1.0f).dot(dirToFish.scale(-1.0)) < FISH_DOT) return false
        return player.hasLineOfSight(fish)
    }

    /** 鱼每 5 tick 调用；满足条件则开始凝视。 */
    fun tryEngage(fish: CyanStaringFishEntity, player: ServerPlayer): Boolean {
        if (ACTIVE.containsKey(player.uuid)) return false
        if (player.isPassenger) return false
        val level = fish.level() as? ServerLevel ?: return false
        val until = BREAK_COOLDOWN_UNTIL[player.uuid]
        if (until != null && level.gameTime < until) return false
        if (!eyeContact(fish, player)) return false

        ACTIVE[player.uuid] = Stare(fish.uuid, level.dimension(), player.x, player.y, player.z)
        BREAK_COOLDOWN_UNTIL.remove(player.uuid)
        fish.isNoAi = true
        fish.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 0.6f, 1.6f)
        return true
    }

    /** 服务端每 tick 调用。 */
    fun tick(server: MinecraftServer) {
        if (ACTIVE.isEmpty()) return
        val iterator = ACTIVE.entries.iterator()
        while (iterator.hasNext()) {
            val (playerId, stare) = iterator.next()
            val player = server.playerList.getPlayer(playerId)
            val level = server.getLevel(stare.dimension)
            val fish = level?.getEntity(stare.fishId) as? CyanStaringFishEntity

            if (player == null || !player.isAlive || player.isSpectator || player.isCreative
                || level == null || player.level() !== level
                || fish == null || !fish.isAlive
            ) {
                iterator.remove()
                fish?.isNoAi = false
                continue
            }

            if (stare.phase == PHASE_STUN) {
                // 定身：清速度 + 钉在锁定点（悬浮，不受重力影响）
                player.deltaMovement = Vec3.ZERO
                player.fallDistance = 0.0f
                player.connection.teleport(stare.lockX, stare.lockY, stare.lockZ, player.yRot, player.xRot)

                fish.isNoAi = true
                fish.deltaMovement = Vec3.ZERO

                stareGazeParticles(level, player, fish)

                if (--stare.ticksLeft <= 0) {
                    stare.phase = PHASE_GRACE
                    stare.ticksLeft = GRACE_TICKS
                    fish.isNoAi = false // 给鱼一点“松口”的错觉，视线断了就真断
                }
            } else {
                // 挣脱窗口：0.5 秒内移开视线即彻底脱身
                if (!playerStillLooking(player, fish)) {
                    BREAK_COOLDOWN_UNTIL[playerId] = level.gameTime + BREAK_COOLDOWN
                    iterator.remove()
                    fish.isNoAi = false
                } else if (--stare.ticksLeft <= 0) {
                    // 不移开视线 → 继续定住
                    stare.phase = PHASE_STUN
                    stare.ticksLeft = STUN_TICKS
                    stare.lockX = player.x
                    stare.lockY = player.y
                    stare.lockZ = player.z
                    fish.isNoAi = true
                }
            }
        }
    }

    private fun playerStillLooking(player: ServerPlayer, fish: CyanStaringFishEntity): Boolean {
        val toFish = fish.getEyePosition(1.0f).subtract(player.getEyePosition(1.0f))
        val distSqr = toFish.lengthSqr()
        if (distSqr > MAX_DISTANCE * MAX_DISTANCE) return false
        if (player.getViewVector(1.0f).dot(toFish.normalize()) < VIEW_DOT) return false
        return player.hasLineOfSight(fish)
    }

    /** 在双方视线之间绘制电火花，示意“锁死”。 */
    private fun stareGazeParticles(level: ServerLevel, player: ServerPlayer, fish: CyanStaringFishEntity) {
        val a = player.getEyePosition(1.0f)
        val b = fish.getEyePosition(1.0f)
        for (t in doubleArrayOf(0.35, 0.65)) {
            val p = a.lerp(b, t)
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }
}
