package at.xbce.entity

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

/**
 * 伪装成铁傀儡的监守者。
 *  - 0.5% 概率替代自然生成的铁傀儡。
 *  - 外壳即本体血量（100 点“壳血量”）：像铁傀儡一样正常受伤、被击退、反击。
 *  - 壳越损坏越会露出监守者特征（客户端渲染）。
 *  - 累计承受满 100 点伤害（壳血量归零）时秒切为监守者，
 *    该监守者血量为 400（因为 100 点已被铁傀儡壳吸收）。
 *  - 可用铁锭 / 蜜脾上蜡修复壳。
 */
class FakeIronGolemEntity(type: EntityType<out FakeIronGolemEntity>, level: Level) : IronGolem(type, level) {

    private var isRevealed = false
    private var repairProgress = 0

    companion object {
        const val MAX_SHELL_HEALTH = 100
        // 监守者原版血量 500；扣除已被外壳吸收的 100 点 → 揭露时为 400。
        const val WARDEN_REVEAL_HEALTH = 400.0f

        fun createAttributes(): AttributeSupplier.Builder = IronGolem.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_SHELL_HEALTH.toDouble())
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ATTACK_DAMAGE, 30.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
    }

    /**
     * 壳损坏程度 0.0(完好) ~ 1.0(即将揭露)，供客户端渲染监守者特征。
     * 直接基于本体血量计算——LivingEntity 的血量会自动同步到客户端。
     */
    fun getShellDamageFraction(): Float =
        (1.0f - health / maxHealth).coerceIn(0.0f, 1.0f)

    override fun registerGoals() {
        // 直接复用铁傀儡的完整 AI：巡逻、近战攻击(MeleeAttackGoal)、被攻击反击(HurtByTargetGoal)。
        // 之前自定义 goal 漏掉了 MeleeAttackGoal，导致它锁定目标却永远不出手、也不像铁傀儡那样活动。
        super.registerGoals()

        // 伪装期间额外靠近村民以融入村庄（不影响原版行为）。
        targetSelector.addGoal(
            4,
            net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal(this, Villager::class.java, 10, true, false) { !isRevealed }
        )
    }

    override fun tick() {
        super.tick()
        if (!level().isClientSide && !isRevealed) {
            if (random.nextInt(100) == 0) {
                val nearbyVillager = level().getEntitiesOfClass(
                    Villager::class.java,
                    AABB.ofSize(position(), 16.0, 8.0, 16.0)
                ).firstOrNull()
                if (nearbyVillager != null) {
                    navigation.moveTo(nearbyVillager, 0.4)
                }
            }
        }
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (isRevealed) {
            return super.hurt(source, amount)
        }

        // 外壳即本体血量：正常受伤、被击退、触发反击仇恨。
        val result = super.hurt(source, amount)

        if (!level().isClientSide && result) {
            val level = level()
            if (level is ServerLevel) {
                level.sendParticles(ParticleTypes.CRIT, x, y + 1.0, z, 3, 0.2, 0.2, 0.2, 0.01)
            }
            playSound(SoundEvents.ANVIL_LAND, 0.5f, 0.7f + (random.nextFloat() * 0.3f))
        }

        return result
    }

    /**
     * 壳血量（本体血量）耗尽时，不真正死亡，而是揭露为监守者。
     */
    override fun die(source: DamageSource) {
        if (!isRevealed && !level().isClientSide) {
            val attacker = source.entity as? Player
                ?: level().getNearestPlayer(this, 32.0)
            reveal(attacker)
            return
        }
        super.die(source)
    }

    private fun reveal(triggeredBy: Player?) {
        isRevealed = true

        val level = level()
        if (level !is ServerLevel) return

        val warden = EntityType.WARDEN.create(level) ?: return
        warden.moveTo(x, y, z, yRot, xRot)
        warden.yHeadRot = yHeadRot
        customName?.let {
            warden.customName = it
            warden.isCustomNameVisible = isCustomNameVisible
        }

        level.addFreshEntity(warden)

        // 铁傀儡壳已吸收 100 点伤害，因此揭露出的监守者只剩 400 血。
        warden.getAttribute(Attributes.MAX_HEALTH)?.baseValue = WARDEN_REVEAL_HEALTH.toDouble()
        warden.health = WARDEN_REVEAL_HEALTH

        // 关键：给予足够高的愤怒值（> ANGRY 阈值 80）并锁定攻击目标，
        // 否则监守者会遁地或对声响无动于衷（"打了个寂寞"）。
        // 必须先 increaseAngerAt（写入 AngerManagement），再 setAttackTarget，
        // 这样监守者的 Brain 下一 tick 不会因缺少愤怒目标而清除 ATTACK_TARGET。
        if (triggeredBy != null) {
            warden.increaseAngerAt(triggeredBy, 150, true)
            warden.setAttackTarget(triggeredBy)
        }

        level.playSound(null, x, y, z, SoundEvents.WARDEN_EMERGE, soundSource, 1.0f, 1.0f)
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, y + 1.0, z, 20, 0.5, 0.8, 0.5, 0.05)

        discard()
    }

    override fun mobInteract(player: Player, hand: net.minecraft.world.InteractionHand): net.minecraft.world.InteractionResult {
        if (!level().isClientSide && !isRevealed) {
            val stack = player.getItemInHand(hand)

            // 铁锭修复外壳，或蜜脾上蜡修复（原版蜜脾对傀儡无用途，这里赋予新用途）
            if (stack.`is`(Items.IRON_INGOT) || stack.`is`(Items.HONEYCOMB)) {
                if (health < maxHealth) {
                    repairProgress++
                    if (!player.abilities.instabuild) {
                        stack.shrink(1)
                    }
                    val sound = if (stack.`is`(Items.HONEYCOMB)) SoundEvents.HONEYCOMB_WAX_ON else SoundEvents.ANVIL_USE
                    playSound(sound, 1.0f, 1.0f)

                    if (repairProgress >= 6) {
                        repairShell()
                        repairProgress = 0
                    }

                    return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide)
                }
            }
        }

        return super.mobInteract(player, hand)
    }

    private fun repairShell() {
        health = maxHealth
        playSound(SoundEvents.ANVIL_LAND, 1.0f, 1.0f)
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        nbt.putBoolean("IsRevealed", isRevealed)
        nbt.putInt("RepairProgress", repairProgress)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        isRevealed = nbt.getBoolean("IsRevealed")
        repairProgress = nbt.getInt("RepairProgress")
    }
}
