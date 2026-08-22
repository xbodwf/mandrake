package at.xbce.client.render

import at.xbce.XBCE
import at.xbce.client.model.FakeSnowGolemModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import at.xbce.entity.FakeSnowGolemEntity

class FakeSnowGolemRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<FakeSnowGolemEntity, FakeSnowGolemModel>(
        context,
        FakeSnowGolemModel(context.bakeLayer(FakeSnowGolemModelLayers.FAKE_SNOW_GOLEM)),
        0.5f
    ) {

    private val texture: ResourceLocation = XBCE.id("textures/entity/fake_snow_golem.png")

    init {
        addLayer(FakeSnowGolemHeadLayer(this, context.blockRenderDispatcher))
    }

    override fun getTextureLocation(entity: FakeSnowGolemEntity): ResourceLocation = texture

    override fun scale(entity: FakeSnowGolemEntity, poseStack: PoseStack, partialTick: Float) {
        // 膨胀动画（同原版苦力怕）
        var g = entity.getSwelling(partialTick)
        val wobble = 1.0f + Mth.sin(g * 100.0f) * g * 0.01f
        g = Mth.clamp(g, 0.0f, 1.0f)
        g *= g
        g *= g
        val s = (1.0f + g * 0.4f) * wobble
        val hs = (1.0f + g * 0.1f) / wobble
        poseStack.scale(s, hs, s)
    }

    override fun getWhiteOverlayProgress(entity: FakeSnowGolemEntity, partialTick: Float): Float {
        val step = entity.getSwelling(partialTick)
        if ((step * 10.0f).toInt() % 2 == 0) {
            return 0.0f
        }
        return Mth.clamp(step, 0.5f, 1.0f)
    }
}
