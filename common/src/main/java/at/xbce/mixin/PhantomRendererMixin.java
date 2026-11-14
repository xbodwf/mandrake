package at.xbce.mixin;

import at.xbce.client.render.PhantomFireworkLayer;
import at.xbce.client.render.PhantomHelmetLayer;
import at.xbce.client.render.PhantomSpearLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.world.entity.monster.Phantom;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extra phantom render layers: stolen helmet, firework payload and carried spear.
 */
@Mixin(PhantomRenderer.class)
public abstract class PhantomRendererMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void xbce$addHelmetLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        PhantomRenderer self = (PhantomRenderer) (Object) this;
        ((LivingEntityLayersAccessor<Phantom, PhantomModel<Phantom>>) this).xbce$getLayers()
            .add(new PhantomHelmetLayer(
                self,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR))
            ));
        ((LivingEntityLayersAccessor<Phantom, PhantomModel<Phantom>>) this).xbce$getLayers()
            .add(new PhantomFireworkLayer(self));
        ((LivingEntityLayersAccessor<Phantom, PhantomModel<Phantom>>) this).xbce$getLayers()
            .add(new PhantomSpearLayer(self));
    }
}
