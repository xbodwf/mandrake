package at.xbce.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 由 PhantomMixin 实现并合并进原版 Phantom，
 * 供 MobMixin / LivingEntityPhantomMixin 等其他目标类的 Mixin 调用。
 */
public interface PhantomExtra {

    /** 俯冲命中玩家时调用（来自 Mob#doHurtTarget 头部）。返回 true 表示吞掉本次伤害（偷盔甲/抓取）。 */
    boolean xbce$sweepAttackHook(Entity target);

    /** 致命伤结算时调用（来自 LivingEntity#die 头部）。返回 true 表示取消死亡，进入装死。 */
    boolean xbce$deathHook(DamageSource source);

    /** 真正死亡结算完毕后调用（来自 LivingEntity#die 尾部）：归还赃物、掉落烟花。 */
    void xbce$onRealDeath();

    /** 装死期间被攻击时调用：立即苏醒并锁定攻击者。 */
    void xbce$wakeFromFakeDeath(LivingEntity attacker);

    /** 当前是否处于装死状态。 */
    boolean xbce$isPlayingDead();

    /** 是否携带着烟花（客户端渲染层据此在翅膀上画出烟花火箭）。注意：会被 Kotlin 侧调用，方法名不能含 $。 */
    boolean xbceHasFireworks();
}
