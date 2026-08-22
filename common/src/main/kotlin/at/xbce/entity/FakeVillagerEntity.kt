package at.xbce.entity

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.PanicGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level

class FakeVillagerEntity(type: EntityType<out FakeVillagerEntity>, level: Level) : Villager(type, level) {

    private var isRevealed = false
    private var ambushPrimed = false

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Villager.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
    }

    /** 伏击模式：被预埋后，玩家靠近时立刻现形。 */
    fun primeAmbush() {
        ambushPrimed = true
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, PanicGoal(this, 0.4))
        goalSelector.addGoal(2, WaterAvoidingRandomStrollGoal(this, 0.4))
        goalSelector.addGoal(3, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(4, RandomLookAroundGoal(this))
    }

    override fun aiStep() {
        super.aiStep()
        if (!level().isClientSide && ambushPrimed && !isRevealed) {
            val near = level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity::class.java,
                net.minecraft.world.phys.AABB.ofSize(position(), 16.0, 8.0, 16.0)
            ) { it is Player }
                .minByOrNull { it.distanceToSqr(this) }
            if (near != null) {
                reveal(near)
            }
        }
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        if (!level().isClientSide && !isRevealed) {
            reveal(player)
            return InteractionResult.sidedSuccess(false)
        }
        return super.mobInteract(player, hand)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (!level().isClientSide && !isRevealed && source.entity is Player) {
            reveal(source.entity as Player)
            return false
        }
        return super.hurt(source, amount)
    }

    private fun reveal(triggeredBy: net.minecraft.world.entity.LivingEntity) {
        isRevealed = true

        val vindicator = EntityType.VINDICATOR.create(level()) ?: return
        vindicator.moveTo(position())
        vindicator.yRot = yRot
        vindicator.xRot = xRot
        vindicator.target = triggeredBy
        customName?.let { vindicator.customName = it }

        val axe = ItemStack(Items.IRON_AXE)
        val sharpLevel = random.nextInt(3) + 1
        val enchantment = level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS)
        axe.enchant(enchantment, sharpLevel)
        vindicator.setItemInHand(InteractionHand.MAIN_HAND, axe)

        level().addFreshEntity(vindicator)
        discard()
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        nbt.putBoolean("IsRevealed", isRevealed)
        nbt.putBoolean("AmbushPrimed", ambushPrimed)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        isRevealed = nbt.getBoolean("IsRevealed")
        ambushPrimed = nbt.getBoolean("AmbushPrimed")
    }
}
