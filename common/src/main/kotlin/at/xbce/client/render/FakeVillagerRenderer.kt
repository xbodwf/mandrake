package at.xbce.client.render

import at.xbce.entity.FakeVillagerEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.VillagerModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.VillagerRenderer
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.FastColor
import net.minecraft.world.entity.npc.Villager

/**
 * 假村民渲染器：平时与普通村民一致；
 * 举着显形望远镜观察时，叠加敌意红雾，暗示“这不是村民”。
 * （卫道士模型对实体类型强转不安全，无法直接换用原版卫道士渲染器。）
 */
class FakeVillagerRenderer(context: EntityRendererProvider.Context) : VillagerRenderer(context) {

    init {
        @Suppress("UNCHECKED_CAST")
        val layer = HostileTintLayer(
            this as RenderLayerParent<FakeVillagerEntity, VillagerModel<FakeVillagerEntity>>
        )
        @Suppress("UNCHECKED_CAST")
        addLayer(layer as RenderLayer<Villager, VillagerModel<Villager>>)
    }

    private class HostileTintLayer(
        parent: RenderLayerParent<FakeVillagerEntity, VillagerModel<FakeVillagerEntity>>
    ) : RenderLayer<FakeVillagerEntity, VillagerModel<FakeVillagerEntity>>(parent) {

        override fun render(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            packedLight: Int,
            entity: FakeVillagerEntity,
            limbSwing: Float,
            limbSwingAmount: Float,
            partialTicks: Float,
            ageInTicks: Float,
            netHeadYaw: Float,
            headPitch: Float
        ) {
            if (!RevealLensClient.shouldShowTrueForm(entity)) return
            if (entity.isInvisible) return

            val consumer = buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)))
            parentModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, TINT)
        }

        companion object {
            // 敌意暗红
            private val TINT: Int = FastColor.ARGB32.color(150, 190, 30, 30)
        }
    }
}
