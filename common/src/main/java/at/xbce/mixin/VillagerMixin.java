package at.xbce.mixin;

import at.xbce.XBCE;
import at.xbce.entity.FakeVillagerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerMixin {

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void onFinalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        MobSpawnType reason,
        SpawnGroupData spawnData,
        CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        ServerLevel serverLevel = level.getLevel();
        if (serverLevel.isClientSide) return;
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return;
        if (serverLevel.random.nextFloat() > 0.10f) return;

        Villager self = (Villager) (Object) this;
        var villagerData = self.getVillagerData();

        FakeVillagerEntity fake = new FakeVillagerEntity(XBCE.FAKE_VILLAGER, serverLevel);
        fake.moveTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot());
        fake.setVillagerData(villagerData);
        fake.finalizeSpawn(level, difficulty, reason, null);

        serverLevel.addFreshEntity(fake);
        self.discard();
    }
}
