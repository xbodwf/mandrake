package at.xbce.mixin;

import at.xbce.client.render.PhantomFireworkLayer;
import at.xbce.client.render.PhantomHelmetLayer;
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
 * 给幻翼渲染器挂自定义头盔层：
 * 偷来的头盔会真实地戴在幻翼头上（含材质、染色、附魔光泽），杀掉它即可取回。
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
    }
}
