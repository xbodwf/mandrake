package at.mandrake.block

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import org.joml.Matrix4f

class CorePortalRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<CorePortalBlockEntity> {

    override fun render(
        entity: CorePortalBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val matrix = poseStack.last().pose()
        val consumer = bufferSource.getBuffer(RenderType.endPortal())
        val down = 0.375f
        val up = 0.75f

        face(consumer, matrix, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
        face(consumer, matrix, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f)
        face(consumer, matrix, 1f, 1f, 1f, 0f, 0f, 1f, 1f, 0f)
        face(consumer, matrix, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f)
        face(consumer, matrix, 0f, 1f, down, down, 0f, 0f, 1f, 1f)
        face(consumer, matrix, 0f, 1f, up, up, 1f, 1f, 0f, 0f)
    }

    private fun face(
        consumer: VertexConsumer, matrix: Matrix4f,
        minX: Float, maxX: Float, minY: Float, maxY: Float,
        z1: Float, z2: Float, z3: Float, z4: Float
    ) {
        consumer.addVertex(matrix, minX, minY, z1)
        consumer.addVertex(matrix, maxX, minY, z2)
        consumer.addVertex(matrix, maxX, maxY, z3)
        consumer.addVertex(matrix, minX, maxY, z4)
    }
}
