package at.xbce.client.render

import at.xbce.entity.CaravanTraderEntity
import net.minecraft.client.model.VillagerModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

class CaravanTraderRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<CaravanTraderEntity, VillagerModel<CaravanTraderEntity>>(
        context,
        VillagerModel(context.bakeLayer(ModelLayers.WANDERING_TRADER)),
        0.5f
    ) {

    override fun getTextureLocation(entity: CaravanTraderEntity): ResourceLocation =
        ResourceLocation.withDefaultNamespace("textures/entity/wandering_trader.png")
}
