package at.xbce.mixin;

import at.xbce.XBCE;
import at.xbce.entity.FakeSnowGolemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 自然生成的雪傀儡有 20% 概率被替换为伪装的假雪傀儡（真身为苦力怕）。
 */
@Mixin(Mob.class)
public abstract class SnowGolemSpawnMixin {

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void onFinalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        MobSpawnType reason,
        SpawnGroupData spawnData,
        CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (!((Object) this instanceof SnowGolem)) return;
        // 排除假雪傀儡自身（其继承 Creeper 而非 SnowGolem，一般不会命中，保险起见）
        if ((Object) this instanceof FakeSnowGolemEntity) return;
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return;
        if (level.getLevel().isClientSide()) return;
        if (level.getRandom().nextFloat() > 0.20f) return;

        ServerLevel serverLevel = level.getLevel();
        SnowGolem self = (SnowGolem) (Object) this;

        FakeSnowGolemEntity fake = new FakeSnowGolemEntity(XBCE.FAKE_SNOW_GOLEM, serverLevel);
        fake.moveTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot());
        fake.finalizeSpawn(level, difficulty, reason, null);

        serverLevel.addFreshEntity(fake);
        self.discard();
    }
}
