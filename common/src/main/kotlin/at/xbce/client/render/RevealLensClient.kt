package at.xbce.client.render

import at.xbce.item.RevealSpyglassItem
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * 客户端照妖镜状态：本地玩家是否正举着显形望远镜观察。
 * 举镜时，视野内伪装生物的渲染会切换为揭示状态。
 */
object RevealLensClient {

    const val REVEAL_RANGE_SQR = 64.0 * 64.0

    @JvmStatic
    fun isScopingReveal(): Boolean {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        return player.isUsingItem && player.useItem.item is RevealSpyglassItem
    }

    @JvmStatic
    fun shouldShowTrueForm(entity: Entity): Boolean {
        if (!isScopingReveal()) return false
        val player: Player = Minecraft.getInstance().player ?: return false
        if (entity.isSpectator) return false
        return entity.distanceToSqr(player) <= REVEAL_RANGE_SQR
    }
}
