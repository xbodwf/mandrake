package at.xbce.entity

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.PanicGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.animal.SnowGolem
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3

class FakeSnowGolemEntity(type: EntityType<out FakeSnowGolemEntity>, level: Level) : SnowGolem(type, level) {

    private var snowLayerHealth = MAX_SNOW_LAYERS

    companion object {
        const val MAX_SNOW_LAYERS = 4

        fun createAttributes(): AttributeSupplier.Builder = SnowGolem.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, PanicGoal(this, 0.4))
        goalSelector.addGoal(2, WaterAvoidingRandomStrollGoal(this, 0.4))
        goalSelector.addGoal(3, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(4, RandomLookAroundGoal(this))

        targetSelector.addGoal(1, NearestAttackableTargetGoal(this, Creeper::class.java, 10, true, false, null))
    }

    override fun aiStep() {
        super.aiStep()

        if (!level().isClientSide) {
            if (random.nextInt(20) == 0) {
                val pos = blockPosition()
                if (level().isEmptyBlock(pos)) {
                    level().setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState())
                }
            }
        }

        if (target != null && isBeingWatchedByPlayer()) {
            target = null
            navigation.stop()
        }
    }

    private fun isBeingWatchedByPlayer(): Boolean {
        for (player in level().players()) {
            if (player.isSpectator) continue
            if (distanceTo(player) > 16.0) continue

            val lookVec: Vec3 = player.getViewVector(1.0f)
            val toEntity: Vec3 = position().subtract(player.eyePosition).normalize()
            val dot: Double = lookVec.x * toEntity.x + lookVec.y * toEntity.y + lookVec.z * toEntity.z

            if (dot > 0.82 && player.hasLineOfSight(this)) {
                return true
            }
        }
        return false
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (level().isClientSide) return false

        if (source.`is`(DamageTypes.DROWN) || source.`is`(DamageTypes.INDIRECT_MAGIC)) {
            if (snowLayerHealth > 0) {
                snowLayerHealth--
                playSound(SoundEvents.SNOW_BREAK, 1.0f, 1.0f)
                return false
            }
        }

        val result = super.hurt(source, amount)
        if (result && snowLayerHealth > 0) {
            snowLayerHealth--
            if (level() is ServerLevel) {
                (level() as ServerLevel).sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    x, y + 0.5, z, 5, 0.3, 0.3, 0.3, 0.02
                )
            }
        }
        return result
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        nbt.putInt("SnowLayerHealth", snowLayerHealth)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        snowLayerHealth = nbt.getInt("SnowLayerHealth").coerceIn(0, MAX_SNOW_LAYERS)
    }
}
