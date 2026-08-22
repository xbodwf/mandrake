package at.xbce.mixin;

import java.util.List;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露渲染层的内部列表，供外部渲染器 Mixin 追加自定义层
 * （addLayer 是 protected，且 @Shadow 对声明在超类的方法会报找不到目标）。
 */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityLayersAccessor<T extends LivingEntity, M extends EntityModel<T>> {

    @Accessor("layers")
    List<RenderLayer<T, M>> xbce$getLayers();
}
