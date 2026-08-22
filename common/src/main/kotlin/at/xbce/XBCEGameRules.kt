package at.xbce

import at.xbce.mixin.GameRulesAccessor
import at.xbce.mixin.GameRulesIntegerValueAccessor
import net.minecraft.world.level.GameRules

/**
 * 自定义游戏规则。
 *  - xbceCaravanInterval：商队生成间隔（tick），0 = 禁用商队。默认 12000（10 分钟）。
 */
object XBCEGameRules {

    const val DEFAULT_CARAVAN_INTERVAL = 12000

    lateinit var CARAVAN_INTERVAL: GameRules.Key<GameRules.IntegerValue>
        private set

    private var registered = false

    /** 必须在世界加载前调用（两个 loader 的初始化入口都会调用）。 */
    fun register() {
        if (registered) return
        registered = true
        CARAVAN_INTERVAL = GameRulesAccessor.`xbce$register`(
            "xbceCaravanInterval",
            GameRules.Category.SPAWNING,
            GameRulesIntegerValueAccessor.`xbce$create`(DEFAULT_CARAVAN_INTERVAL)
        )
    }
}
