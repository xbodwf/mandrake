package at.xbce.mixin;

import at.xbce.XBCE;
import at.xbce.entity.FakeSnowGolemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 对苦力怕使用雪块 → 转换为没有南瓜头的假雪傀儡（可再用雕刻南瓜装扮伪装）。
 */
@Mixin(Creeper.class)
public abstract class CreeperConvertMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Creeper self = (Creeper) (Object) this;
        // 假雪傀儡自身继承 Creeper，需排除
        if (self instanceof FakeSnowGolemEntity) return;

        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.SNOW_BLOCK)) return;

        if (self.level().isClientSide()) {
            cir.setReturnValue(InteractionResult.sidedSuccess(true));
            return;
        }

        ServerLevel level = (ServerLevel) self.level();
        FakeSnowGolemEntity fake = new FakeSnowGolemEntity(XBCE.FAKE_SNOW_GOLEM, level);
        fake.moveTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot());
        fake.setYHeadRot(self.getYHeadRot());
        fake.markPlayerCreated();
        fake.setHealth(fake.getMaxHealth());
        fake.setTarget(self.getTarget());
        if (self.hasCustomName()) {
            fake.setCustomName(self.getCustomName());
            fake.setCustomNameVisible(self.isCustomNameVisible());
        }

        level.addFreshEntity(fake);
        self.discard();
        at.xbce.XBCEAdvancements.onFakeSnowGolemConverted(player);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, fake.getX(), fake.getY(), fake.getZ(),
            SoundEvents.SNOW_PLACE, fake.getSoundSource(), 1.0f, 1.0f);

        cir.setReturnValue(InteractionResult.sidedSuccess(false));
    }
}
