package at.xbce.item

import net.minecraft.world.item.SpyglassItem

/**
 * 显形望远镜（照妖镜）。
 * 继承原版望远镜获得举镜/缩放行为（配合 PlayerScopingMixin 使原版 isScoping 认可本物品），
 * 举镜时客户端渲染层会揭示伪装生物的本体。
 */
class RevealSpyglassItem(properties: Properties) : SpyglassItem(properties)
