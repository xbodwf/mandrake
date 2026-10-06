package at.xbce.entity

import at.xbce.CyanFishStare
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.goal.PanicGoal
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal
import net.minecraft.world.entity.animal.AbstractFish
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

/**
 * 天青色瞪眼鱼：水里自然生成的鱼有 10% 概率是它（见 CyanFishSpawnMixin）。
 *  - 不躲避玩家，反而会死死盯着附近的玩家（头部始终朝向玩家）；
 *  - 与玩家四目相对时触发凝视：玩家定身 3 秒、鱼同步静止，见 [CyanFishStare]。
 */
class CyanStaringFishEntity(type: EntityType<out CyanStaringFishEntity>, level: Level) : AbstractFish(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = AbstractFish.createAttributes()
    }

    override fun registerGoals() {
        // 与普通鱼不同：不注册 AvoidEntityGoal——它就是要盯着你
        goalSelector.addGoal(0, PanicGoal(this, 1.25))
        goalSelector.addGoal(4, RandomSwimmingGoal(this, 1.0, 10))
    }

    override fun aiStep() {
        super.aiStep()
        if (level().isClientSide) return
        if (isNoAi) return // 凝视定身中，由管理器接管
        if (tickCount % 5 != 0) return

        val nearest = level().getNearestPlayer(this, 20.0) as? ServerPlayer ?: return
        if (nearest.isSpectator || nearest.isCreative) return
        lookAt(nearest, 360.0f, 360.0f) // 死死盯住
        if (CyanFishStare.eyeContact(this, nearest)) {
            CyanFishStare.tryEngage(this, nearest)
        }
    }

    override fun getFlopSound(): SoundEvent = SoundEvents.COD_FLOP

    override fun getBucketItemStack(): ItemStack = ItemStack(Items.COD_BUCKET)

    override fun getAmbientSound(): SoundEvent? = SoundEvents.COD_AMBIENT
}
