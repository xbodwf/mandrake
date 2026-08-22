package at.xbce.mixin;

import at.xbce.XBCE;
import at.xbce.entity.FakeIronGolemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.entity.Mob.class)
public abstract class IronGolemSpawnMixin {

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void onFinalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        MobSpawnType reason,
        SpawnGroupData spawnData,
        CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (!((Object) this instanceof IronGolem)) return;
        // 排除假铁傀儡自身，避免 finalizeSpawn 递归触发
        if ((Object) this instanceof FakeIronGolemEntity) return;
        if (level.isClientSide()) return;
        if (level.getRandom().nextFloat() > 0.005f) return;

        ServerLevel serverLevel = level.getLevel();
        IronGolem self = (IronGolem) (Object) this;
        FakeIronGolemEntity fake = new FakeIronGolemEntity(XBCE.FAKE_IRON_GOLEM, serverLevel);
        fake.moveTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot());
        fake.finalizeSpawn(level, difficulty, reason, null);

        serverLevel.addFreshEntity(fake);
        self.discard();
    }
}
