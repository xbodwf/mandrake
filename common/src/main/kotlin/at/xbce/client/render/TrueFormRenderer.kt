package at.xbce.client.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.Entity

/**
 * 伪装生物渲染器包装：平时转发给“伪装外观”渲染器，
 * 本地玩家举着显形望远镜观察时，切换为“本体外观”渲染器。
 */
@Suppress("UNCHECKED_CAST")
class TrueFormRenderer<T : Entity>(
    context: EntityRendererProvider.Context,
    disguise: Any,
    trueForm: Any
) : EntityRenderer<T>(context) {

    private val disguiseRenderer = disguise as EntityRenderer<T>
    private val trueFormRenderer = trueForm as EntityRenderer<T>

    private fun pick(entity: T): EntityRenderer<T> =
        if (RevealLensClient.shouldShowTrueForm(entity)) trueFormRenderer else disguiseRenderer

    override fun getTextureLocation(entity: T) = pick(entity).getTextureLocation(entity)

    override fun shouldRender(
        entity: T,
        frustum: Frustum,
        camX: Double,
        camY: Double,
        camZ: Double
    ): Boolean = pick(entity).shouldRender(entity, frustum, camX, camY, camZ)

    override fun render(
        entity: T,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        pick(entity).render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight)
    }
}
