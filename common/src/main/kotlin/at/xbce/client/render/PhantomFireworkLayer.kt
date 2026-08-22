package at.xbce.client.render

import at.xbce.entity.PhantomExtra
import com.mojang.blaze3d.vertex.PoseStack
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
 * 幻翼挂载烟花层：携带烟花的幻翼，两侧翅膀上各绑一支烟花火箭，
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
        if (!extra.xbceHasFireworks()) return

        val itemRenderer = Minecraft.getInstance().itemRenderer
        val stack = ItemStack(Items.FIREWORK_ROCKET)
        val body = this.parentModel.root().getChild("body")

        // 左右翼根各一支（跟随翅膀摆动）
        for ((childName, offsetX, seed) in listOf(
            Triple("left_wing_base", 3.0f, 1),
            Triple("right_wing_base", -3.0f, 2)
        )) {
            poseStack.pushPose()
            body.getChild(childName).translateAndRotate(poseStack)
            // 竖立在翼面上方：翼面局部 y[0,2]、z[0,9]，取中段靠上位置
            poseStack.translate(offsetX, 0.6f, 4.5f)
            poseStack.scale(1.25f, 1.25f, 1.25f)
            itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
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
