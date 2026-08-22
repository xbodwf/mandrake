package at.xbce

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.warden.Warden
import net.minecraft.world.entity.player.Player

/**
 * 进度奖励辅助：代码触发的进度（原版触发器无法覆盖的场景）。
 */
object XBCEAdvancements {

    private fun award(player: Player, path: String, criterion: String) {
        val serverPlayer = player as? ServerPlayer ?: return
        val holder = serverPlayer.server.advancements.get(XBCE.id(path)) ?: return
        serverPlayer.advancements.award(holder, criterion)
    }

    /** 剪穿假雪傀儡 → 火眼金睛 */
    @JvmStatic
    fun onShearFakeSnowGolem(player: Player) {
        award(player, "shear_fake", "sheared")
    }

    /** 用雪块把苦力怕变成假雪傀儡 → 以假乱真 */
    @JvmStatic
    fun onFakeSnowGolemConverted(player: Player) {
        award(player, "fake_maker", "converted")
    }

    /** 被监守者击杀 → 打了个寂寞 */
    @JvmStatic
    fun onDeath(entity: LivingEntity, source: DamageSource) {
        if (entity is ServerPlayer && source.entity is Warden) {
            award(entity, "warden_kill", "killed_by_warden")
        }
    }
}
