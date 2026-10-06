package at.xbce.mixin;

import at.xbce.XBCE;
import at.xbce.entity.CyanStaringFishEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.ServerLevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 水里自然生成的鱼有 10% 概率被替换为天青色瞪眼鱼。
 * 只作用于自然生成/区块生成（鱼桶、刷怪蛋不受影响）。
 */
@Mixin(Mob.class)
public abstract class CyanFishSpawnMixin {

    @Unique private static final float XBCE_CYAN_FISH_CHANCE = 0.10F;

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void xbce$cyanFishConversion(
        ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
        SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        Mob self = (Mob) (Object) this;
        if (!(self instanceof AbstractFish)) return;
        if (self instanceof CyanStaringFishEntity) return; // 防止递归替换
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) return;
        if (level.getRandom().nextFloat() >= XBCE_CYAN_FISH_CHANCE) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        CyanStaringFishEntity fish = XBCE.CYAN_STARING_FISH.create(serverLevel);
        if (fish == null) return;
        fish.moveTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot());
        fish.finalizeSpawn(level, difficulty, spawnType, null);
        serverLevel.addFreshEntity(fish);
        self.discard();
    }
}
