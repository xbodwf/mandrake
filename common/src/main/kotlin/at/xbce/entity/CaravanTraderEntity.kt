package at.xbce.entity

import at.xbce.XBCE
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.PanicGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.village.poi.PoiManager
import net.minecraft.world.entity.ai.village.poi.PoiTypes
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.Vindicator
import net.minecraft.world.entity.npc.AbstractVillager
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.npc.VillagerTrades
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.AABB
import java.util.Optional
import kotlin.math.cos
import kotlin.math.sin

/**
 * 商队商人：在村庄间赶路的行脚商。
 *  - 路上可能遭遇“假村民”伏击队（伪装的卫道士）。
 *  - 玩家护航（清空伏击者且商人存活）→ 感恩：当场赠礼 + 解锁谢礼特供交易。
 */
class CaravanTraderEntity(type: EntityType<out CaravanTraderEntity>, level: Level) : AbstractVillager(type, level) {

    companion object {
        const val STATE_TRAVELING = 0
        const val STATE_AMBUSHED = 1
        const val STATE_GRATEFUL = 2

        fun createAttributes(): AttributeSupplier.Builder = Villager.createAttributes()
    }

    private var routeTarget: BlockPos? = null
    private var state = STATE_TRAVELING
    private var repathCooldown = 0
    private var resolveCooldown = 0

    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, PanicGoal(this, 0.5))
        goalSelector.addGoal(
            4,
            net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal(this)
        )
        goalSelector.addGoal(2, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(3, RandomLookAroundGoal(this))
    }

    /** 右键打开交易界面；首次交互时补货。 */
    override fun mobInteract(player: Player, hand: net.minecraft.world.InteractionHand): net.minecraft.world.InteractionResult {
        if (isBaby) return super.mobInteract(player, hand)

        if (!level().isClientSide) {
            if (hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
                player.awardStat(net.minecraft.stats.Stats.TALKED_TO_VILLAGER)
            }
            if (getOffers().isEmpty()) {
                updateTrades()
                if (state == STATE_GRATEFUL) {
                    appendBonusTrades()
                }
            }
            setTradingPlayer(player)
            openTradingScreen(player, displayName, 1)
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide)
    }

    override fun aiStep() {
        super.aiStep()
        if (level().isClientSide) return

        if (state != STATE_AMBUSHED) {
            if (--repathCooldown <= 0) {
                repathCooldown = 40
                val target = routeTarget
                if (target == null ||
                    distanceToSqr(target.x + 0.5, target.y.toDouble(), target.z + 0.5) < 16.0
                ) {
                    pickNewRoute()
                } else {
                    navigation.moveTo(target.x + 0.5, target.y.toDouble(), target.z + 0.5, 0.38)
                }
            }
            maybeTriggerAmbush()
        } else {
            if (--resolveCooldown <= 0) {
                resolveCooldown = 100
                checkAmbushResolved()
            }
        }
    }

    /** 选下一段路线：优先别的村庄集合点，否则随机远点。 */
    private fun pickNewRoute() {
        val level = level()
        if (level !is ServerLevel) return
        val current = blockPosition()

        val poi = level.poiManager.getInSquare({ h -> h.`is`(PoiTypes.MEETING) }, current, 256, PoiManager.Occupancy.ANY)
            .filter { it.pos.distSqr(current) > 48 * 48 }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.let { list -> list[level.random.nextInt(list.size)]?.pos }

        routeTarget = if (poi != null && level.random.nextInt(3) > 0) {
            poi
        } else {
            val angle = level.random.nextDouble() * 2.0 * Math.PI
            val dist = 200.0 + level.random.nextDouble() * 200.0
            BlockPos.containing(x + cos(angle) * dist, y, z + sin(angle) * dist)
        }
    }

    private fun maybeTriggerAmbush() {
        if (state != STATE_TRAVELING) return
        if (random.nextInt(2400) != 0) return
        // 只在有玩家能目击时触发（否则商队在视野外白白送死）
        val witness = level().players().firstOrNull { !it.isSpectator && distanceTo(it) < 48.0 } ?: return
        triggerAmbush()
    }

    private fun triggerAmbush() {
        state = STATE_AMBUSHED
        resolveCooldown = 100

        val level = level()
        if (level !is ServerLevel) return

        var spawned = 0
        for (i in 0 until 8) {
            if (spawned >= 4) break
            val angle = random.nextDouble() * 2.0 * Math.PI
            val dist = 8.0 + random.nextDouble() * 6.0
            val bx = Mth.floor(x + cos(angle) * dist)
            val bz = Mth.floor(z + sin(angle) * dist)
            val by = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz)
            val pos = BlockPos(bx, by, bz)
            if (!Mob.checkMobSpawnRules(EntityType.VINDICATOR, level, MobSpawnType.EVENT, pos, random)) continue

            if (random.nextBoolean()) {
                // 伪装的伏击者：靠近才现形
                val fake = FakeVillagerEntity(XBCE.FAKE_VILLAGER, level)
                fake.moveTo(bx + 0.5, by.toDouble(), bz + 0.5, random.nextFloat() * 360.0f, 0.0f)
                fake.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null)
                fake.primeAmbush()
                level.addFreshEntity(fake)
            } else {
                val vindicator = EntityType.VINDICATOR.create(level) ?: continue
                vindicator.moveTo(bx + 0.5, by.toDouble(), bz + 0.5, random.nextFloat() * 360.0f, 0.0f)
                vindicator.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null)
                vindicator.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.IRON_AXE))
                vindicator.target = this
                level.addFreshEntity(vindicator)
            }
            spawned++
        }
    }

    private fun checkAmbushResolved() {
        val level = level()
        if (level !is ServerLevel) return

        val remaining = level.getEntitiesOfClass(
            Monster::class.java,
            AABB.ofSize(position(), 64.0, 32.0, 64.0)
        ) { it is Vindicator || it is FakeVillagerEntity }
        if (remaining.isNotEmpty()) return

        state = STATE_GRATEFUL
        appendBonusTrades()

        // 护航奖励：在场生存玩家各得一份谢礼
        for (player in level.players()) {
            if (player.isSpectator || player.isCreative()) continue
            if (distanceTo(player) > 24.0) continue
            giveGift(player)
        }
        playSound(SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f)
    }

    private fun giveGift(player: ServerPlayer) {
        val gift = rollGift()
        level().addFreshEntity(ItemEntity(player.level(), player.x, player.y + 0.5, player.z, gift))
    }

    private fun rollGift(): ItemStack = when (random.nextInt(5)) {
        0 -> ItemStack(Items.DIAMOND, 1 + random.nextInt(2))
        1 -> ItemStack(Items.EMERALD_BLOCK)
        2 -> ItemStack(Items.GOLDEN_APPLE)
        3 -> ItemStack(Items.NAME_TAG)
        else -> {
            val pool = listOf(
                Enchantments.PROTECTION, Enchantments.SHARPNESS,
                Enchantments.EFFICIENCY, Enchantments.UNBREAKING
            )
            enchantedBook(pool[random.nextInt(pool.size)], 2 + random.nextInt(2))
        }
    }

    private fun enchantedBook(enchantment: net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>, level_: Int): ItemStack {
        val book = ItemStack(Items.ENCHANTED_BOOK)
        book.enchant(level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), level_)
        return book
    }

    /** 平时的货：复用流浪商人的交易池。 */
    override fun updateTrades() {
        val offers = MerchantOffers()
        for ((_, listings) in VillagerTrades.WANDERING_TRADER_TRADES) {
            repeat(minOf(2, listings.size)) {
                val offer = listings[random.nextInt(listings.size)].getOffer(this, random)
                if (offer != null) offers.add(offer)
            }
        }
        overrideOffers(offers)
    }

    /** 感恩特供：护航成功后追加的高性价比交易。 */
    private fun appendBonusTrades() {
        val merged = MerchantOffers()
        getOffers()?.let { merged.addAll(it) }
        merged.addAll(
            listOf(
                MerchantOffer(ItemCost(Items.EMERALD, 6), Optional.empty(), ItemStack(Items.ENDER_PEARL, 2), 0, 4, 5, 0.05f),
                MerchantOffer(ItemCost(Items.EMERALD, 12), Optional.empty(), ItemStack(Items.DIAMOND, 1), 0, 3, 8, 0.05f),
                MerchantOffer(ItemCost(Items.EMERALD, 20), Optional.empty(), enchantedBook(Enchantments.PROTECTION, 3), 0, 2, 10, 0.05f)
            )
        )
        overrideOffers(merged)
    }

    override fun rewardTradeXp(offer: MerchantOffer) {
        // 商队商人交易不给经验（与流浪商人一致）
    }

    override fun getBreedOffspring(level: net.minecraft.server.level.ServerLevel, other: net.minecraft.world.entity.AgeableMob): net.minecraft.world.entity.AgeableMob? {
        // 商队商人不参与繁殖
        return null
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        nbt.putInt("State", state)
        nbt.putLong("RouteTarget", routeTarget?.asLong() ?: 0L)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        state = nbt.getInt("State").coerceIn(STATE_TRAVELING, STATE_GRATEFUL)
        val route = nbt.getLong("RouteTarget")
        routeTarget = if (route == 0L) null else BlockPos.of(route)
    }
}
