package at.xbce.client.render

import at.xbce.entity.FakeIronGolemEntity
import net.minecraft.client.model.IronGolemModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.animal.IronGolem

/**
 * 假铁傀儡渲染器：外观与铁傀儡一致（含受损裂纹），另叠加随外壳损坏渐显的监守者特征层。
 */
class FakeIronGolemRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<FakeIronGolemEntity, IronGolemModel<FakeIronGolemEntity>>(
        context,
        IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)),
        0.7f
    ) {

    private val texture: ResourceLocation =
        ResourceLocation.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png")

    init {
        // 原版裂纹层按 血量/最大血量 计算破损等级，与外壳血量机制天然契合。
        // 两处泛型均经擦除桥接（实体确为 IronGolem 子类，运行时安全）：
        //  1. 父渲染器桥接为 Layer 要求的 RenderLayerParent<IronGolem, ...>
        //  2. 构造出的 Layer 桥接回本渲染器要求的 RenderLayer<FakeIronGolemEntity, ...>
        @Suppress("UNCHECKED_CAST")
        val shellCracks = IronGolemCrackinessLayer(this as RenderLayerParent<IronGolem, IronGolemModel<IronGolem>>)
            as RenderLayer<FakeIronGolemEntity, IronGolemModel<FakeIronGolemEntity>>
        addLayer(shellCracks)
        addLayer(FakeIronGolemWardenLayer(this))
    }

    override fun getTextureLocation(entity: FakeIronGolemEntity): ResourceLocation = texture
}

