package at.xbce.client.render

import at.xbce.entity.PhantomExtra
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * 幻翼挂载烟花层：加速期间背上绑着烟花火箭（链式连发最多 2 支，左右翼对称），
 * 让玩家在它加速前就能看到"这家伙带了货"。
 */
class PhantomFireworkLayer(
    parent: RenderLayerParent<Phantom, PhantomModel<Phantom>>
) : RenderLayer<Phantom, PhantomModel<Phantom>>(parent) {

    override fun render(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        phantom: Phantom,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        val extra = phantom as? PhantomExtra ?: return
        val count = extra.xbceFireworkCount()
        if (count <= 0) return

        val itemRenderer = Minecraft.getInstance().itemRenderer
        val stack = ItemStack(Items.FIREWORK_ROCKET)
        val body = this.parentModel.root().getChild("body")

        // 第 1 支挂左翼根，第 2 支挂右翼根（引线镜像对称）
        val wings = listOf(
            Triple("left_wing_base", 3.0f, 1),
            Triple("right_wing_base", -3.0f, 2)
        )
        for ((index, wing) in wings.withIndex()) {
            if (index >= count) break
            val (childName, offsetX, seed) = wing

            poseStack.pushPose()
            body.getChild(childName).translateAndRotate(poseStack)
            // 位置：翼面中段（单位：格，像素÷16）
            poseStack.translate(offsetX / 16f, -1.4f / 16f, 4.5f / 16f)
            // 平放沿机身前后、头朝前
            poseStack.mulPose(Axis.XP.rotationDegrees(-90f))
            // 右翼镜像 180°，让两支引线朝向对称
            if (index == 1) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            }
            poseStack.scale(1.25f, 1.25f, 1.25f)
            itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                phantom.level(),
                phantom.id + seed
            )
            poseStack.popPose()
        }
    }
}
