package at.xbce.client.render

import at.xbce.XBCE
import at.xbce.entity.CyanStaringFishEntity
import net.minecraft.client.model.CodModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

/** 天青色瞪眼鱼渲染器：鳕鱼模型 + 天青色重着色贴图。 */
class CyanStaringFishRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<CyanStaringFishEntity, CodModel<CyanStaringFishEntity>>(
        context,
        CodModel(context.bakeLayer(ModelLayers.COD)),
        0.3f
    ) {

    private val texture: ResourceLocation = XBCE.id("textures/entity/cyan_staring_fish.png")

    override fun getTextureLocation(entity: CyanStaringFishEntity): ResourceLocation = texture
}
