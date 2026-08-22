package at.xbce.entity

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

/**
 * 伪装成雪傀儡的苦力怕。
 *  - 自然生成时戴上雕刻南瓜，看起来和普通雪傀儡一样。
 *  - 剪掉南瓜头会露出苦力怕头，此时会被真雪傀儡认出并攻击。
 *  - 在非寒冷群系壳会逐渐融化，融化完毕后变成普通苦力怕。
 *  - 受到攻击时雪壳会逐层脱落。
 *  - 仍然保留苦力怕的膨胀 / 爆炸行为。
 */
class FakeSnowGolemEntity(type: EntityType<out FakeSnowGolemEntity>, level: Level) : Creeper(type, level) {

    private var snowLayerHealth = MAX_SNOW_LAYERS
    private var meltCooldown = MELT_COOLDOWN
    private var playerCreated = false
    private var firstTickDone = false

    companion object {
        const val MAX_SNOW_LAYERS = 4
        private const val MELT_COOLDOWN = 20

        private val DATA_HAS_PUMPKIN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(FakeSnowGolemEntity::class.java, EntityDataSerializers.BOOLEAN)

        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_HAS_PUMPKIN, false)
    }

    fun hasPumpkin(): Boolean = entityData.get(DATA_HAS_PUMPKIN)

    fun setPumpkin(pumpkin: Boolean) {
        entityData.set(DATA_HAS_PUMPKIN, pumpkin)
    }

    /** 由玩家用雪块转化而来时调用：初始不戴南瓜头（露出苦力怕头）。 */
    fun markPlayerCreated() {
        playerCreated = true
    }

    /** 像雪傀儡一样怕水。 */
    override fun isSensitiveToWater(): Boolean = true

    override fun aiStep() {
        super.aiStep()

        val level = level()
        if (level !is ServerLevel) return

        // 首次 tick：自然生成的戴上南瓜伪装成普通雪傀儡
        if (!firstTickDone) {
            firstTickDone = true
            if (!playerCreated) {
                setPumpkin(true)
            }
        }

        // 铺设积雪路径（与雪傀儡一致）
        if (level.gameRules.getBoolean(GameRules.RULE_MOBGRIEFING)) {
            val snow: BlockState = Blocks.SNOW.defaultBlockState()
            for (i in 0 until 4) {
                val xx = Mth.floor(x + ((i % 2 * 2 - 1) * 0.25f).toDouble())
                val yy = Mth.floor(y)
                val zz = Mth.floor(z + ((i / 2 % 2 * 2 - 1) * 0.25f).toDouble())
                val snowPos = BlockPos(xx, yy, zz)
                if (level.getBlockState(snowPos).isAir && snow.canSurvive(level, snowPos)) {
                    level.setBlockAndUpdate(snowPos, snow)
                }
            }
        }

        // 非寒冷群系：每秒融化 1 层雪
        if (--meltCooldown <= 0) {
            meltCooldown = MELT_COOLDOWN
            val pos = blockPosition()
            if (!level.getBiome(pos).value().coldEnoughToSnow(pos)) {
                reduceSnowLayer(level, 1)
            }
        }

        // 戴南瓜时鬼鬼祟祟：追击玩家时若被注视则停下（尚未膨胀时）
        if (hasPumpkin() && target != null && swellDir <= 0 && isBeingWatchedByPlayer()) {
            target = null
            navigation.stop()
        }
    }

    private fun isBeingWatchedByPlayer(): Boolean {
        for (player in level().players()) {
            if (player.isSpectator) continue
            if (distanceTo(player) > 16.0) continue

            val look: Vec3 = player.getViewVector(1.0f)
            val toEntity: Vec3 = Vec3(x - player.x, eyeY - player.eyeY, z - player.z).normalize()
            val dot = look.dot(toEntity)
            if (dot > 0.82 && player.hasLineOfSight(this)) {
                return true
            }
        }
        return false
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val level = level()
        // 水伤（雨/水/喷溅水瓶）：只削减雪层，不扣本体血量
        if (source.`is`(DamageTypes.DROWN) || source.`is`(DamageTypes.INDIRECT_MAGIC)) {
            if (level is ServerLevel && snowLayerHealth > 0) {
                reduceSnowLayer(level, Mth.ceil(amount))
            }
            return false
        }

        val result = super.hurt(source, amount)
        if (result && level is ServerLevel && snowLayerHealth > 0) {
            reduceSnowLayer(level, Mth.ceil(amount))
        }
        return result
    }

    private fun reduceSnowLayer(level: ServerLevel, amount: Int) {
        if (snowLayerHealth <= 0) return
        snowLayerHealth -= amount
        if (snowLayerHealth <= 0) {
            snowLayerHealth = 0
            revertToCreeper(level)
        } else {
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 1.0, z, 5, 0.3, 0.5, 0.3, 0.02)
            playSound(SoundEvents.SNOW_BREAK, 1.0f, 1.0f)
        }
    }

    private fun revertToCreeper(level: ServerLevel) {
        val creeper = EntityType.CREEPER.create(level) ?: return
        creeper.moveTo(x, y, z, yRot, xRot)
        creeper.yHeadRot = yHeadRot
        creeper.target = target
        customName?.let {
            creeper.customName = it
            creeper.isCustomNameVisible = isCustomNameVisible
        }

        // 掉落南瓜
        if (hasPumpkin()) {
            spawnAtLocation(ItemStack(Items.CARVED_PUMPKIN))
        }

        level.sendParticles(ParticleTypes.SPLASH, x, y + 0.5, z, 15, 0.5, 0.3, 0.5, 0.05)
        level.playSound(null, x, y, z, SoundEvents.SNOW_GOLEM_DEATH, soundSource, 0.5f, 1.5f)

        level.addFreshEntity(creeper)
        discard()
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)

        // 雕刻南瓜盖头
        if (stack.`is`(Items.CARVED_PUMPKIN) && !hasPumpkin()) {
            if (!level().isClientSide) {
                setPumpkin(true)
                if (!player.abilities.instabuild) stack.shrink(1)
                playSound(SoundEvents.SNOW_PLACE, 1.0f, 1.0f)
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }

        // 剪刀摘南瓜，露出苦力怕头
        if (stack.`is`(Items.SHEARS) && hasPumpkin()) {
            if (!level().isClientSide) {
                setPumpkin(false)
                spawnAtLocation(ItemStack(Items.CARVED_PUMPKIN))
                playSound(SoundEvents.SNOW_GOLEM_SHEAR, 1.0f, 1.0f)
                at.xbce.XBCEAdvancements.onShearFakeSnowGolem(player)
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }

        return super.mobInteract(player, hand)
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        nbt.putBoolean("Pumpkin", hasPumpkin())
        nbt.putInt("SnowLayerHealth", snowLayerHealth)
        nbt.putInt("MeltCooldown", meltCooldown)
        nbt.putBoolean("PlayerCreated", playerCreated)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        setPumpkin(nbt.getBoolean("Pumpkin"))
        snowLayerHealth = if (nbt.contains("SnowLayerHealth")) {
            nbt.getInt("SnowLayerHealth").coerceIn(0, MAX_SNOW_LAYERS)
        } else {
            MAX_SNOW_LAYERS
        }
        meltCooldown = if (nbt.contains("MeltCooldown")) nbt.getInt("MeltCooldown") else MELT_COOLDOWN
        playerCreated = nbt.getBoolean("PlayerCreated")
        firstTickDone = true
    }
}
