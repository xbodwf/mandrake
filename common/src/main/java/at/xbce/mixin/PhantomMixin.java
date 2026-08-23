package at.xbce.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import at.xbce.entity.PhantomBoostControl;
import at.xbce.entity.PhantomExtra;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 幻翼整活合集（直接增强原版幻翼）：
 *  1. 烟花加速（空战战术）：玩家开鞘翅逃跑 → 高概率点火紧咬；俯冲攻击 → 概率点火加速；
 *     自身残血 → 点火反向脱离。翅膀上绑着可见的烟花火箭，死亡时掉落。
 *  2. 装死骗膜：被玩家打死时有概率装死坠落，掉出“幻翼膜”诱饵（无法拾取）；
 *     玩家靠近即尖啸复活、诱饵凭空消失；无人问津则连尸带膜一并蒸发。
 *  3. 俯冲偷盔：掠过玩家时叼走头盔挂在身下（下界合金太重抢不动），杀死后归还。
 *  4. 抓取升空：咬住玩家拽上天再松手，摔落伤害自理；打中幻翼可提前脱身。
 *  5. 空运轰炸：一生一次，抓起附近苦力怕飞到目标头顶空投。
 */
@Mixin(Phantom.class)
public abstract class PhantomMixin implements PhantomExtra {

    @Unique private static final float XBCE_FAKE_DEATH_CHANCE = 0.25F;
    @Unique private static final float XBCE_STEAL_CHANCE = 0.20F;
    @Unique private static final float XBCE_GRAB_CHANCE_END = 0.32F;
    @Unique private static final long XBCE_GRAB_PLAYER_COOLDOWN = 1200L;
    @Unique private static final int XBCE_GRAB_MAX_TICKS = 80;
    @Unique private static final double XBCE_GRAB_MAX_ASCENT = 11.0D;
    @Unique private static final double XBCE_WAKE_RADIUS_SQR = 12.25D; // 3.5^2
    @Unique private static final int XBCE_PLAY_DEAD_TIMEOUT = 500;
    @Unique private static final int XBCE_BOOST_COOLDOWN = 25;
    /** 烟花加速期间的速度上限（格/tick），约等于鞘翅 + 火箭连发的巡航速度。 */
    @Unique private static final float XBCE_BOOST_SPEED = 2.4F;
    /** 追击模式速度：略高于玩家烟花连发，保证能咬上。 */
    @Unique private static final float XBCE_PURSUE_SPEED = 2.6F;
    // 追击断锁参数
    @Unique private static final int XBCE_PURSUIT_LOST_SIGHT = 30;    // 丢视线 1.5s 断锁
    @Unique private static final int XBCE_PURSUIT_GROUND = 20;        // 猎物落地 1s 断锁
    @Unique private static final int XBCE_PURSUIT_EXHAUST = 200;      // 累计追击 10s 未命中 → 疲劳
    @Unique private static final long XBCE_PURSUIT_BAN = 600L;        // 疲劳后 30s 不再追击
    @Unique private static final double XBCE_PURSUIT_BREAK_DIST_SQR = 96.0D * 96.0D;

    /** 背上挂载的烟花数量 0~2（同步给客户端渲染层）。 */
    @Unique private static final EntityDataAccessor<Integer> XBCE_FIREWORK_COUNT =
        SynchedEntityData.defineId(Phantom.class, EntityDataSerializers.INT);

    /** 玩家维度级别的抓取冷却（跨幻翼共享，防止围殴式连环抓）。 */
    @Unique private static final Map<UUID, Long> XBCE_GRAB_COOLDOWNS = new HashMap<>();

    @Unique private boolean xbce$fakingDeath = false;
    @Unique private boolean xbce$usedFakeDeath = false;
    @Unique private int xbce$baitLandedTicks = 0;
    @Unique private UUID xbce$baitId = null;

    @Unique private boolean xbce$stoleHelmet = false;
    @Unique private ItemStack xbce$pendingHelmetDrop = null;

    @Unique private boolean xbce$fireworksUser = false;
    @Unique private int xbce$boostCooldown = 0;
    @Unique private int xbce$boostTicksLeft = 0;
    /** 0=进攻（移控直扑目标） 1=撤离（反向推力） 2=追击（直接朝鞘翅玩家 homing） */
    @Unique private int xbce$boostMode = 0;
    @Unique private UUID xbce$pursuitId = null;
    // 追击断锁状态
    @Unique private int xbce$lostSightTicks = 0;
    @Unique private int xbce$preyGroundTicks = 0;
    @Unique private int xbce$pursuitTicks = 0;
    @Unique private long xbce$pursuitBanUntil = 0L;
    @Unique private int xbce$trailTicks = 0;
    // 背上烟花挂载数（链式点火累计，最多 2）
    @Unique private int xbce$chainCount = 0;
    @Unique private long xbce$chainResetAt = 0L;

    @Unique private boolean xbce$grabbing = false;
    @Unique private UUID xbce$grabVictimId = null;
    @Unique private int xbce$grabTicksLeft = 0;
    @Unique private double xbce$grabAscent = 0.0D;

    @Unique private int xbce$bomberTimer = 1200;
    @Unique private boolean xbce$bombedOnce = false;

    private Mob xbce$mob() {
        return (Mob) (Object) this;
    }

    private Entity xbce$entity() {
        return (Entity) (Object) this;
    }

    // ------------------------------------------------------------------ tick

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void xbce$defineExtraData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(XBCE_FIREWORK_COUNT, 0);
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void xbce$enhancedTick(CallbackInfo ci) {
        Entity self = xbce$entity();
        Level level = self.level();
        if (level.isClientSide() || !self.isAlive()) return;
        if (!(level instanceof ServerLevel server)) return;

        if (xbce$fakingDeath) {
            xbce$tickPlayingDead(server);
            return;
        }

        xbce$tickGrab(server);
        xbce$tickFireworkBoost(server);
        xbce$tickBomber(server);
    }

    // ------------------------------------------------------------- play dead

    @Unique
    private void xbce$tickPlayingDead(ServerLevel level) {
        Mob self = xbce$mob();
        self.setRemainingFireTicks(0); // 装死期间不烧

        Entity selfEntity = self;
        if (!selfEntity.onGround()) {
            // 飞行生物没有重力，手动让它坠机
            Vec3 dm = self.getDeltaMovement();
            self.setDeltaMovement(dm.x * 0.85D, Math.min(dm.y - 0.02D, -0.08D), dm.z * 0.85D);
            return;
        }

        if (xbce$baitId == null) {
            ItemEntity bait = new ItemEntity(level, self.getX(), self.getY() + 0.4D, self.getZ(),
                new ItemStack(Items.PHANTOM_MEMBRANE));
            bait.setPickUpDelay(Short.MAX_VALUE); // 诱饵根本捡不起来
            level.addFreshEntity(bait);
            xbce$baitId = bait.getUUID();
            self.playSound(SoundEvents.PHANTOM_BITE, 0.6F, 0.6F);
            return;
        }

        xbce$baitLandedTicks++;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            if (player.distanceToSqr(self) < XBCE_WAKE_RADIUS_SQR) {
                xbce$wakeFromFakeDeath(player);
                return;
            }
        }

        if (xbce$baitLandedTicks > XBCE_PLAY_DEAD_TIMEOUT) {
            xbce$discardBait(level);
            self.discard(); // 没人来捡？连尸带膜一起消失
        }
    }

    @Override
    @Unique
    public boolean xbce$deathHook(DamageSource source) {
        Mob self = xbce$mob();
        if (xbce$fakingDeath || xbce$usedFakeDeath) return false;
        if (!(source.getEntity() instanceof ServerPlayer killer)) return false;
        if (killer.isCreative() || killer.isSpectator()) return false;

        if (self.getRandom().nextFloat() >= XBCE_FAKE_DEATH_CHANCE) {
            xbce$usedFakeDeath = true; // 这次没装成，之后就是真死
            xbce$stashStolenHelmet();
            return false;
        }

        xbce$fakingDeath = true;
        xbce$usedFakeDeath = true;
        xbce$baitLandedTicks = 0;
        xbce$baitId = null;
        xbce$endGrab(false);

        for (Entity passenger : List.copyOf(self.getPassengers())) {
            passenger.stopRiding();
        }

        self.setNoAi(true);
        self.setHealth(1.0F);
        self.playSound(SoundEvents.PHANTOM_DEATH, 1.0F, 1.0F);
        ((ServerLevel) self.level()).sendParticles(ParticleTypes.POOF,
            self.getX(), self.getY() + 0.8D, self.getZ(), 12, 0.4D, 0.5D, 0.4D, 0.02D);
        return true;
    }

    @Override
    @Unique
    public boolean xbce$isPlayingDead() {
        return xbce$fakingDeath;
    }

    @Override
    @Unique
    public boolean xbceHasFireworks() {
        return this.xbceFireworkCount() > 0;
    }

    @Override
    @Unique
    public int xbceFireworkCount() {
        return xbce$entity().getEntityData().get(XBCE_FIREWORK_COUNT);
    }

    @Override
    @Unique
    public void xbce$onSweepLanded(Entity target) {
        // 追击中咬到猎物 → 疲劳清零，允许继续锁下去
        if (xbce$boostMode == 2 && xbce$pursuitId != null
            && target.getUUID().equals(xbce$pursuitId)) {
            xbce$pursuitTicks = 0;
        }
    }

    @Override
    @Unique
    public void xbce$wakeFromFakeDeath(LivingEntity attacker) {
        Mob self = xbce$mob();
        xbce$fakingDeath = false;
        self.setNoAi(false);
        self.setHealth(Math.max(4.0F, self.getMaxHealth() * 0.5F));
        self.setDeltaMovement(0.0D, 0.6D, 0.0D);
        if (attacker != null && !attacker.isSpectator()) {
            self.setTarget(attacker);
        }
        self.playSound(SoundEvents.WARDEN_ROAR, 2.0F, 1.3F);
        Level level = self.level();
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SCULK_SOUL,
                self.getX(), self.getY() + 1.0D, self.getZ(), 16, 0.4D, 0.5D, 0.4D, 0.05D);
            xbce$discardBait(server);
        }
    }

    @Unique
    private void xbce$discardBait(ServerLevel level) {
        if (xbce$baitId == null) return;
        Entity bait = level.getEntity(xbce$baitId);
        if (bait != null && bait.isAlive()) {
            level.sendParticles(ParticleTypes.POOF,
                bait.getX(), bait.getY() + 0.2D, bait.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.01D);
            bait.discard();
        }
        xbce$baitId = null;
    }

    // ---------------------------------------------------------- sweep tricks

    @Override
    @Unique
    public boolean xbce$sweepAttackHook(Entity target) {
        if (!(target instanceof ServerPlayer player)) return false;
        if (player.isCreative() || player.isSpectator()) return false;
        Mob self = xbce$mob();
        RandomSource random = self.getRandom();
        float roll = random.nextFloat();

        // 偷盔甲（下界合金太重抢不走）
        if (!xbce$stoleHelmet && roll < XBCE_STEAL_CHANCE) {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty() && !helmet.is(Items.NETHERITE_HELMET)) {
                player.getInventory().armor.set(EquipmentSlot.HEAD.getIndex(), ItemStack.EMPTY);
                player.containerMenu.broadcastChanges();
                self.setItemSlot(EquipmentSlot.HEAD, helmet);
                xbce$stoleHelmet = true;
                self.playSound(SoundEvents.PHANTOM_BITE, 1.5F, 0.7F);
                self.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 0.8F);
                ((ServerLevel) self.level()).sendParticles(ParticleTypes.CRIT,
                    self.getX(), self.getY() + 0.5D, self.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.1D);
                return true;
            }
            roll += XBCE_STEAL_CHANCE; // 没盔可偷 → 顺延判定抓取
        }

        // 抓取升空
        if (!xbce$grabbing && roll < XBCE_GRAB_CHANCE_END) {
            long now = self.level().getGameTime();
            Long last = XBCE_GRAB_COOLDOWNS.get(player.getUUID());
            if (last == null || now - last >= XBCE_GRAB_PLAYER_COOLDOWN) {
                XBCE_GRAB_COOLDOWNS.put(player.getUUID(), now);
                xbce$grabbing = true;
                xbce$grabVictimId = player.getUUID();
                xbce$grabTicksLeft = XBCE_GRAB_MAX_TICKS;
                xbce$grabAscent = 0.0D;
                self.playSound(SoundEvents.PHANTOM_BITE, 1.5F, 1.4F);
                return true; // 本次不造成伤害
            }
        }

        return false;
    }

    // ------------------------------------------------------------------ grab

    @Unique
    private void xbce$tickGrab(ServerLevel level) {
        if (!xbce$grabbing) return;
        if (xbce$grabVictimId == null) {
            xbce$endGrab(false);
            return;
        }
        Mob self = xbce$mob();
        if (!(level.getPlayerByUUID(xbce$grabVictimId) instanceof ServerPlayer victim)) {
            xbce$endGrab(false);
            return;
        }
        boolean invalid = !victim.isAlive()
            || victim.isCreative() || victim.isSpectator()
            || self.hurtTime > 0
            || self.distanceToSqr(victim) > 48.0D * 48.0D;
        if (invalid || xbce$grabTicksLeft <= 0 || xbce$grabAscent >= XBCE_GRAB_MAX_ASCENT) {
            xbce$endGrab(true);
            return;
        }

        Vec3 vm = victim.getDeltaMovement();
        double up = Math.min(vm.y + 0.10D, 0.42D);
        victim.setDeltaMovement(vm.x * 0.75D, up, vm.z * 0.75D);
        victim.hurtMarked = true;
        victim.fallDistance = 0.0F; // 松手前不计摔落 → 从最高点开始结算
        xbce$grabAscent += Math.max(0.0D, up);
        xbce$grabTicksLeft--;

        if (victim.tickCount % 5 == 0) {
            level.sendParticles(ParticleTypes.CRIT,
                victim.getX(), victim.getY() + 1.2D, victim.getZ(), 3, 0.2D, 0.3D, 0.2D, 0.02D);
        }
    }

    @Unique
    private void xbce$endGrab(boolean sound) {
        if (!xbce$grabbing) return;
        xbce$grabbing = false;
        xbce$grabVictimId = null;
        if (sound) {
            xbce$mob().playSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 1.3F);
        }
    }

    // -------------------------------------------------------------- fireworks

    @Unique
    private void xbce$tickFireworkBoost(ServerLevel level) {
        Mob self = xbce$mob();

        // 燃尽且过了展示期 → 背上烟花卸下
        if (xbce$boostTicksLeft <= 0 && xbce$chainCount > 0 && level.getGameTime() >= xbce$chainResetAt) {
            xbce$chainCount = 0;
            self.getEntityData().set(XBCE_FIREWORK_COUNT, 0);
        }

        // 持续加速中
        if (xbce$boostTicksLeft > 0) {
            switch (xbce$boostMode) {
                case 2 -> {
                    // 追击：按自持的 UUID 锁定猎物（不依赖原版索敌），每 tick 朝其 homing
                    LivingEntity prey = null;
                    if (xbce$pursuitId != null) {
                        net.minecraft.world.entity.player.Player locked = level.getPlayerByUUID(xbce$pursuitId);
                        if (locked != null && locked.isAlive() && !locked.isSpectator() && !locked.isCreative()) prey = locked;
                    }
                    if (prey == null) {
                        xbce$boostTicksLeft = 1; // 猎物消失，下 tick 燃尽
                    } else {
                        // 断锁判定：丢视线 / 猎物落地 / 距离超限 / 疲劳上限
                        xbce$lostSightTicks = self.hasLineOfSight(prey) ? 0 : xbce$lostSightTicks + 1;
                        xbce$preyGroundTicks = prey.isFallFlying() ? 0 : xbce$preyGroundTicks + 1;
                        xbce$pursuitTicks++;
                        boolean breakOff = xbce$lostSightTicks >= XBCE_PURSUIT_LOST_SIGHT
                            || xbce$preyGroundTicks >= XBCE_PURSUIT_GROUND
                            || xbce$pursuitTicks >= XBCE_PURSUIT_EXHAUST
                            || self.distanceToSqr(prey) > XBCE_PURSUIT_BREAK_DIST_SQR;
                        if (breakOff) {
                            xbce$boostTicksLeft = 1;
                            xbce$pursuitBanUntil = level.getGameTime() + XBCE_PURSUIT_BAN;
                        } else {
                            Vec3 to = prey.position().add(0.0D, 1.0D, 0.0D).subtract(self.position());
                            if (to.lengthSqr() > 1.0D) {
                                to = to.normalize();
                                self.setDeltaMovement(self.getDeltaMovement().lerp(to.scale(XBCE_PURSUE_SPEED), 0.5D));
                            }
                        }
                    }
                }
                case 1 -> {
                    // 撤离：反向推力甩开目标
                    LivingEntity target = self.getTarget();
                    double ax = target != null ? self.getX() - target.getX() : 1.0D;
                    double az = target != null ? self.getZ() - target.getZ() : 0.0D;
                    Vec3 away = new Vec3(ax, 0.0D, az);
                    if (away.lengthSqr() < 1.0E-4D) away = new Vec3(0.0D, 0.0D, 1.0D);
                    away = away.normalize().add(0.0D, 0.35D, 0.0D).normalize();
                    self.setDeltaMovement(self.getDeltaMovement().lerp(away.scale(XBCE_BOOST_SPEED), 0.35D));
                }
                default -> {
                    // 进攻：顶住移控速度上限，直扑 moveTargetPoint（俯冲阶段=玩家位置）
                    if (self.getMoveControl() instanceof PhantomBoostControl ctrl) {
                        ctrl.xbce$setBoostSpeed(XBCE_BOOST_SPEED);
                    }
                }
            }
            xbce$wingParticles(level, self, 2, 0.03D);
            xbce$boostTicksLeft--;
            if (xbce$boostTicksLeft <= 0) {
                if (xbce$boostMode == 0 && self.getMoveControl() instanceof PhantomBoostControl ctrl) {
                    ctrl.xbce$setBoostSpeed(0.2F); // 燃尽回落，靠自然缓升恢复巡航
                }
                if (xbce$boostMode == 2) {
                    xbce$pursuitId = null;
                }
                xbce$boostMode = 0;
                xbce$chainResetAt = level.getGameTime() + 30L; // 30t 内再点火算连发，否则卸下烟花
                self.playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 1.5F, 1.0F);
            }
            return;
        }

        if (xbce$trailTicks > 0) {
            xbce$trailTicks--;
            xbce$wingParticles(level, self, 2, 0.02D);
        }

        if (xbce$boostCooldown > 0) {
            xbce$boostCooldown--;
            return;
        }

        // 追击扫描：96 格内开着鞘翅且看得见的生存玩家（疲劳禁令期间不追）
        net.minecraft.world.entity.player.Player fleeing = null;
        double best = 96.0D * 96.0D;
        if (level.getGameTime() >= xbce$pursuitBanUntil) {
            for (net.minecraft.world.entity.player.Player player : level.players()) {
                if (player.isSpectator() || player.isCreative()) continue;
                if (!player.isFallFlying()) continue;
                double distSqr = self.distanceToSqr(player);
                if (distSqr < 64.0D || distSqr >= best) continue;
                if (!self.hasLineOfSight(player)) continue; // 看不见的不锁
                best = distSqr;
                fleeing = player;
            }
        }

        LivingEntity target = self.getTarget();
        boolean wounded = self.getHealth() <= self.getMaxHealth() * 0.3F;
        double dh = target != null ? self.getY() - target.getY() : 0.0D;
        double dx = target != null ? target.getX() - self.getX() : 0.0D;
        double dz = target != null ? target.getZ() - self.getZ() : 0.0D;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        Vec3 dm = self.getDeltaMovement();
        boolean diving = target != null && dm.y < -0.05D && dh > 1.5D && dh < 24.0D && horiz < 16.0D;

        if (wounded && self.getRandom().nextInt(15) == 0) {
            xbce$igniteBoost(level, 10 + self.getRandom().nextInt(20), 1);
        } else if (fleeing != null) {
            // 该用就用：咬住鞘翅玩家不撒口（短冷却，链式点火）
            self.setTarget(fleeing);
            xbce$pursuitId = fleeing.getUUID();
            xbce$igniteBoost(level, 30 + self.getRandom().nextInt(20), 2);
        } else if (diving && self.getRandom().nextInt(20) == 0) {
            xbce$igniteBoost(level, 10 * (1 + self.getRandom().nextInt(3)) + self.getRandom().nextInt(6) + self.getRandom().nextInt(6), 0);
        }
    }

    /** 点燃烟花：mode 0=进攻 1=撤离 2=追击。 */
    @Unique
    private void xbce$igniteBoost(ServerLevel level, int durationTicks, int mode) {
        Mob self = xbce$mob();
        xbce$boostTicksLeft = durationTicks;
        xbce$boostMode = mode;
        if (mode == 2) {
            xbce$lostSightTicks = 0;
            xbce$preyGroundTicks = 0;
            xbce$pursuitTicks = 0;
        }
        if (mode != 1) {
            // 进攻/追击：初始一脚油门朝当前运动方向踹出去
            Vec3 dm = self.getDeltaMovement();
            Vec3 kick = dm.add(dm.x * 0.4D, 0.15D, dm.z * 0.4D);
            if (kick.lengthSqr() > XBCE_BOOST_SPEED * XBCE_BOOST_SPEED) {
                kick = kick.normalize().scale(XBCE_BOOST_SPEED);
            }
            self.setDeltaMovement(kick);
            if (mode == 0 && self.getMoveControl() instanceof PhantomBoostControl ctrl) {
                ctrl.xbce$setBoostSpeed(XBCE_BOOST_SPEED);
            }
        }
        // 背上挂载：链式点火累计，最多同时 2 支
        xbce$chainCount = Math.min(xbce$chainCount + 1, 2);
        self.getEntityData().set(XBCE_FIREWORK_COUNT, xbce$chainCount);
        xbce$fireworksUser = true;
        xbce$boostCooldown = (mode == 2) ? 10 : XBCE_BOOST_COOLDOWN; // 追击链式点火
        self.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.0F, mode == 1 ? 0.8F : 1.0F);
        xbce$wingParticles(level, self, 8, 0.12D);
    }

    /** 双翼喷口粒子：以机身为轴，左右各一个发射点。 */
    @Unique
    private void xbce$wingParticles(ServerLevel level, Mob self, int count, double speed) {
        float yaw = self.getYRot() * ((float) Math.PI / 180F);
        double rx = Math.cos(yaw) * 0.55D; // 垂直于朝向的横向分量
        double rz = Math.sin(yaw) * 0.55D;
        for (int side = 0; side < 2; side++) {
            double s = side == 0 ? 1.0D : -1.0D;
            level.sendParticles(ParticleTypes.FIREWORK,
                self.getX() + rx * s, self.getY() + 0.2D, self.getZ() + rz * s,
                count, 0.15D, 0.15D, 0.15D, speed);
        }
    }

    // ----------------------------------------------------------------- bomber

    @Unique
    private void xbce$tickBomber(ServerLevel level) {
        Mob self = xbce$mob();
        Entity passenger = self.getFirstPassenger();

        if (passenger instanceof Creeper creeper) {
            creeper.setTarget(null); // 不许在半空炸我

            LivingEntity target = self.getTarget();
            if (target == null || !target.isAlive()) {
                creeper.stopRiding();
                return;
            }

            double dh = self.getY() - target.getY();
            double dx = target.getX() - self.getX();
            double dz = target.getZ() - self.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);

            if (dh > 6.0D && horiz < 4.5D) {
                creeper.stopRiding();
                creeper.fallDistance = 0.0F;
                creeper.setTarget(target); // 落地就开炸
                self.playSound(SoundEvents.PHANTOM_AMBIENT, 2.0F, 0.7F);
                level.sendParticles(ParticleTypes.POOF,
                    creeper.getX(), creeper.getY(), creeper.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.01D);
            } else if (dh < 6.0D) {
                // 飞太低了，拉起来
                Vec3 dm = self.getDeltaMovement();
                self.setDeltaMovement(dm.x, Math.max(dm.y + 0.05D, 0.06D), dm.z);
            }
            return;
        }

        if (xbce$bombedOnce) return;
        if (xbce$bomberTimer > 0) {
            xbce$bomberTimer--;
            return;
        }

        List<Creeper> creepers = level.getEntitiesOfClass(Creeper.class,
            self.getBoundingBox().inflate(24.0D),
            c -> c.isAlive() && !c.isPassenger());
        if (creepers.isEmpty()) {
            xbce$bomberTimer = 600;
            return;
        }

        Creeper creeper = creepers.get(self.getRandom().nextInt(creepers.size()));
        creeper.fallDistance = 0.0F;
        if (creeper.startRiding(self, true)) {
            xbce$bombedOnce = true;
            creeper.setTarget(null);
            self.playSound(SoundEvents.PHANTOM_AMBIENT, 2.0F, 0.7F);
        } else {
            xbce$bomberTimer = 200;
        }
    }

    // ------------------------------------------------------------ real death

    @Unique
    private void xbce$stashStolenHelmet() {
        Mob self = xbce$mob();
        if (!xbce$stoleHelmet) return;
        ItemStack helmet = self.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            xbce$pendingHelmetDrop = helmet;
            self.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
    }

    @Override
    @Unique
    public void xbce$onRealDeath() {
        Mob self = xbce$mob();
        xbce$endGrab(false);
        xbce$stashStolenHelmet();
        if (xbce$pendingHelmetDrop != null) {
            self.spawnAtLocation(xbce$pendingHelmetDrop);
            xbce$pendingHelmetDrop = null;
        }
        if (xbce$fireworksUser) {
            self.spawnAtLocation(new ItemStack(Items.FIREWORK_ROCKET, 1 + self.getRandom().nextInt(2)));
        }
        if (xbce$fakingDeath) { // 极端情况兜底
            xbce$fakingDeath = false;
            Level level = self.level();
            if (level instanceof ServerLevel server) {
                xbce$discardBait(server);
            }
        }
    }

    // ------------------------------------------------------------------- nbt

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void xbce$saveState(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("XBceFaking", xbce$fakingDeath);
        tag.putBoolean("XBceNoFake", xbce$usedFakeDeath);
        tag.putBoolean("XBceStole", xbce$stoleHelmet);
        tag.putBoolean("XBceBoom", xbce$fireworksUser);
        tag.putBoolean("XBceBomber", xbce$bombedOnce);
        if (xbce$baitId != null) {
            tag.putLong("XBceBaitM", xbce$baitId.getMostSignificantBits());
            tag.putLong("XBceBaitL", xbce$baitId.getLeastSignificantBits());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void xbce$loadState(CompoundTag tag, CallbackInfo ci) {
        xbce$fakingDeath = tag.getBoolean("XBceFaking");
        xbce$usedFakeDeath = tag.getBoolean("XBceNoFake");
        xbce$stoleHelmet = tag.getBoolean("XBceStole");
        xbce$fireworksUser = tag.getBoolean("XBceBoom");
        xbce$bombedOnce = tag.getBoolean("XBceBomber");
        if (tag.contains("XBceBaitM")) {
            xbce$baitId = new UUID(tag.getLong("XBceBaitM"), tag.getLong("XBceBaitL"));
        } else {
            xbce$baitId = null;
        }
        if (xbce$fakingDeath) {
            xbce$mob().setNoAi(true);
        }
        // 重载后背上烟花清空，下次点火重新挂载
        xbce$chainCount = 0;
        xbce$mob().getEntityData().set(XBCE_FIREWORK_COUNT, 0);
    }
}
