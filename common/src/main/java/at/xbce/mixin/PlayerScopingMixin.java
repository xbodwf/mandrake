package at.xbce.mixin;

import at.xbce.item.RevealSpyglassItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让原版 isScoping() 认可显形望远镜：
 * 举镜时获得与原版望远镜一致的缩放/遮罩行为。
 */
@Mixin(Player.class)
public abstract class PlayerScopingMixin {

    @Inject(method = "isScoping", at = @At("HEAD"), cancellable = true)
    private void xbce$isScoping(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isUsingItem()) {
            ItemStack useItem = self.getUseItem();
            if (useItem.getItem() instanceof RevealSpyglassItem) {
                cir.setReturnValue(true);
            }
        }
    }
}
