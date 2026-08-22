package at.xbce.mixin;

import at.xbce.XBCE;
import at.xbce.entity.FakeIronGolemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 用铁锭 + 皮革给监守者“上伪装” → 转换为伪装成铁傀儡的假铁傀儡。
 * 主手持铁锭右键监守者，背包中需另有皮革；各消耗 1 个。
 *
 * 注入点为 Mob#mobInteract（监守者本身未重写此方法），内部判断目标是否为监守者。
 */
@Mixin(Mob.class)
public abstract class WardenConvertMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!((Object) this instanceof Warden)) return;
        Warden self = (Warden) (Object) this;

        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.IRON_INGOT)) return;
        // 背包中必须还有皮革
        if (!player.getAbilities().instabuild && !player.getInventory().contains(new ItemStack(Items.LEATHER))) {
            return;
        }

        if (self.level().isClientSide()) {
            cir.setReturnValue(InteractionResult.sidedSuccess(true));
            return;
        }

        ServerLevel level = (ServerLevel) self.level();
        FakeIronGolemEntity fake = new FakeIronGolemEntity(XBCE.FAKE_IRON_GOLEM, level);
        fake.moveTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot());
        fake.setYHeadRot(self.getYHeadRot());
        fake.setHealth(fake.getMaxHealth());
        if (self.hasCustomName()) {
            fake.setCustomName(self.getCustomName());
            fake.setCustomNameVisible(self.isCustomNameVisible());
        }

        level.addFreshEntity(fake);
        self.discard();

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            player.getInventory().removeItem(new ItemStack(Items.LEATHER));
        }
        level.playSound(null, fake.getX(), fake.getY(), fake.getZ(),
            SoundEvents.ANVIL_USE, fake.getSoundSource(), 1.0f, 1.0f);

        cir.setReturnValue(InteractionResult.sidedSuccess(false));
    }
}
