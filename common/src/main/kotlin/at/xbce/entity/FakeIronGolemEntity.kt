package at.xbce.entity

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

class FakeIronGolemEntity(type: EntityType<out FakeIronGolemEntity>, level: Level) : IronGolem(type, level) {

    private var shellHealth = MAX_SHELL_HEALTH
    private var isRevealed = false
    private var repairProgress = 0

    companion object {
        const val MAX_SHELL_HEALTH = 100

        fun createAttributes(): AttributeSupplier.Builder = IronGolem.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 500.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ATTACK_DAMAGE, 30.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, MoveTowardsTargetGoal(this, 0.5, 32.0f))
        goalSelector.addGoal(2, WaterAvoidingRandomStrollGoal(this, 0.4))
        goalSelector.addGoal(3, LookAtPlayerGoal(this, Player::class.java, 12.0f))
        goalSelector.addGoal(4, RandomLookAroundGoal(this))

        targetSelector.addGoal(1, net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this).setAlertOthers())
        targetSelector.addGoal(2, net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal(this, Player::class.java, 10, true, false) { isRevealed })
        targetSelector.addGoal(3, net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal(this, Villager::class.java, 10, true, false) { !isRevealed })
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

        if (!level().isClientSide && source.entity is Player) {
            val actualDamage = amount.coerceAtLeast(0f)
            shellHealth -= actualDamage.toInt()

            if (level() is ServerLevel) {
                (level() as ServerLevel).sendParticles(
                    ParticleTypes.CRIT,
                    x, y + 1.0, z, 3, 0.2, 0.2, 0.2, 0.01
                )
            }

            if (shellHealth <= 0) {
                shellHealth = 0
                reveal(source.entity as Player)
            } else {
                playSound(SoundEvents.ANVIL_LAND, 0.5f, 0.7f + (random.nextFloat() * 0.3f))
            }
        }

        return false
    }

    private fun reveal(triggeredBy: Player) {
        isRevealed = true

        val warden = EntityType.WARDEN.create(level()) ?: return
        warden.moveTo(position())
        warden.yRot = yRot
        warden.xRot = xRot
        warden.setTarget(triggeredBy)
        customName?.let { warden.customName = it }

        level().addFreshEntity(warden)
        discard()
    }

    override fun mobInteract(player: Player, hand: net.minecraft.world.InteractionHand): net.minecraft.world.InteractionResult {
        if (!level().isClientSide && !isRevealed) {
            val stack = player.getItemInHand(hand)

            if (stack.`is`(Items.IRON_INGOT) || stack.`is`(Items.LEATHER)) {
                if (repairProgress < 6) {
                    repairProgress++
                    if (!player.abilities.instabuild) {
                        stack.shrink(1)
                    }
                    playSound(SoundEvents.ANVIL_USE, 1.0f, 1.0f)

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
        shellHealth = MAX_SHELL_HEALTH
        playSound(SoundEvents.ANVIL_LAND, 1.0f, 1.0f)
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        nbt.putInt("ShellHealth", shellHealth)
        nbt.putBoolean("IsRevealed", isRevealed)
        nbt.putInt("RepairProgress", repairProgress)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        shellHealth = nbt.getInt("ShellHealth").coerceIn(0, MAX_SHELL_HEALTH)
        isRevealed = nbt.getBoolean("IsRevealed")
        repairProgress = nbt.getInt("RepairProgress")
    }
}
