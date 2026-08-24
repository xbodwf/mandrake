package at.xbce.mixin;

import at.xbce.entity.JockeyExtra;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 蠹虫骑士：自然生成的小僵尸有概率被蠹虫寄生——
 * 蠹虫骑在宿主头顶，鞭策其进入"寄生狂热"（移速 +35%）。
 * 宿主死亡时蠹虫弹射出并锁定凶手（见 SilverfishJockeyLivingMixin）。
 */
@Mixin(Zombie.class)
public abstract class SilverfishJockeyMixin implements JockeyExtra {

    @Unique private static final float XBCE_JOCKEY_CHANCE = 0.10F;
    @Unique private static final double XBCE_FRENZY_SPEED = 0.35D;
    @Unique private static final ResourceLocation XBCE_FRENZY_ID =
        ResourceLocation.fromNamespaceAndPath("xbce", "silverfish_jockey_frenzy");

    @Unique private boolean xbce$reinforcementsCalled = false;

    // ---------------------------------------------------------------- spawn

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void xbce$trySilverfishJockey(
        ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
        SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        Zombie self = (Zombie) (Object) this;
        if (!self.isBaby()) return;
        if (self.isPassenger()) return; // 鸡骑士等已有坐骑的不再叠加
        if (level.getRandom().nextFloat() >= XBCE_JOCKEY_CHANCE) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Silverfish rider = EntityType.SILVERFISH.create(serverLevel);
        if (rider == null) return;
        rider.moveTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), 0.0F);
        rider.setPersistenceRequired();
        // 出生即有敌意：锁定 32 格内最近的生存玩家（蠹虫本是中立怪，不锁就不会主动咬人）
        Player prey = serverLevel.getNearestPlayer(self.getX(), self.getY(), self.getZ(), 32.0D,
            p -> p instanceof Player player && !player.isSpectator() && !player.isCreative());
        if (prey != null) {
            rider.setTarget(prey);
        }
        if (!rider.startRiding(self, true)) {
            rider.discard();
            return;
        }

        // 寄生狂热：鞭策宿主狂奔（永久修饰符，随实体 NBT 持久化）
        AttributeInstance speed = self.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(XBCE_FRENZY_ID) == null) {
            speed.addPermanentModifier(new AttributeModifier(
                XBCE_FRENZY_ID, XBCE_FRENZY_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // ------------------------------------------------------------------ nbt

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void xbce$saveJockey(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("XBceJkyR", xbce$reinforcementsCalled);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void xbce$loadJockey(CompoundTag tag, CallbackInfo ci) {
        xbce$reinforcementsCalled = tag.getBoolean("XBceJkyR");
    }

    @Override
    @Unique
    public boolean xbce$hasCalledReinforcements() {
        return xbce$reinforcementsCalled;
    }

    @Override
    @Unique
    public void xbce$markReinforcementsCalled() {
        xbce$reinforcementsCalled = true;
    }
}
