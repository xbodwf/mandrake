package at.xbce.client.render

import at.xbce.client.model.FakeSnowGolemModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.block.Blocks
import at.xbce.entity.FakeSnowGolemEntity

/**
 * 在假雪傀儡头顶渲染雕刻南瓜方块模型（仅在戴南瓜时）。
 * 剪掉南瓜后此层不渲染，露出下方的苦力怕头网格。
 */
class FakeSnowGolemHeadLayer(
    parent: RenderLayerParent<FakeSnowGolemEntity, FakeSnowGolemModel>,
    private val blockRenderer: BlockRenderDispatcher
) : RenderLayer<FakeSnowGolemEntity, FakeSnowGolemModel>(parent) {

    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: FakeSnowGolemEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTicks: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        if (!entity.hasPumpkin()) return
        if (entity.isInvisible) return

        poseStack.pushPose()
        parentModel.getHead().translateAndRotate(poseStack)
        poseStack.translate(0.0f, -0.34375f, 0.0f)
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f))
        poseStack.scale(0.625f, -0.625f, -0.625f)
        poseStack.translate(-0.5f, -0.5f, -0.5f)
        blockRenderer.renderSingleBlock(
            Blocks.CARVED_PUMPKIN.defaultBlockState(),
            poseStack,
            buffer,
            packedLight,
            OverlayTexture.NO_OVERLAY
        )
        poseStack.popPose()
    }
}
