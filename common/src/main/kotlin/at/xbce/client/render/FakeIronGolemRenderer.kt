package at.xbce.client.render

import at.xbce.entity.FakeIronGolemEntity
import net.minecraft.client.model.IronGolemModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

/**
 * 假铁傀儡渲染器：外观与铁傀儡一致，另叠加随外壳损坏渐显的监守者特征层。
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
        addLayer(FakeIronGolemWardenLayer(this))
    }

    override fun getTextureLocation(entity: FakeIronGolemEntity): ResourceLocation = texture
}
