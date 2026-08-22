package at.xbce.client.render

import at.xbce.entity.FakeIronGolemEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.model.IronGolemModel
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.util.Mth

/**
 * 随着假铁傀儡外壳损坏，在铁傀儡模型上叠加逐渐加深的“监守者”深青色发光层，
 * 制造监守者特征从裂缝中渗出的效果。壳完好时完全不可见。
 *
 * 直接把监守者贴图贴到铁傀儡模型上 UV 不匹配会花屏，因此这里复用铁傀儡自身
 * 贴图并施加深青色半透明染色 + 发光，读起来像监守者的幽冥色泽。
 */
class FakeIronGolemWardenLayer(
    parent: RenderLayerParent<FakeIronGolemEntity, IronGolemModel<FakeIronGolemEntity>>
) : RenderLayer<FakeIronGolemEntity, IronGolemModel<FakeIronGolemEntity>>(parent) {

    companion object {
        private val GOLEM_TEXTURE: ResourceLocation =
            ResourceLocation.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png")
        // 监守者的幽冥深青色
        private const val TINT_R = 20
        private const val TINT_G = 120
        private const val TINT_B = 130
    }

    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: FakeIronGolemEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTicks: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        // 举着显形望远镜观察时，无论壳损坏程度如何都完全显现
        val damage = if (RevealLensClient.shouldShowTrueForm(entity)) {
            1.0f
        } else {
            entity.getShellDamageFraction()
        }
        if (damage <= 0.05f) return
        if (entity.isInvisible) return

        val alpha = Mth.clamp(damage, 0.0f, 1.0f)
        val color = FastColor.ARGB32.color((alpha * 200).toInt(), TINT_R, TINT_G, TINT_B)
        val consumer = buffer.getBuffer(RenderType.entityTranslucent(GOLEM_TEXTURE))
        parentModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color)
    }
}
