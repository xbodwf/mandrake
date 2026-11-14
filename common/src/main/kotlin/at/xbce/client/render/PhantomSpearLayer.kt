package at.xbce.client.render

import at.xbce.entity.SpearCompat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.item.ItemDisplayContext

/**
 * Renders a spear carried by a phantom: held below the body and pointing forward,
 * so players can spot the 4% spear variant before it dives.
 */
class PhantomSpearLayer(
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
        val stack = phantom.getItemBySlot(EquipmentSlot.MAINHAND)
        if (!SpearCompat.isSpear(stack)) return

        val body = this.parentModel.root().getChild("body")
        poseStack.pushPose()
        body.translateAndRotate(poseStack)
        // Model space uses +Y for down: positive Y hangs the spear below the body.
        poseStack.translate(0.0f, 0.32f, -0.34f)
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
        poseStack.scale(1.0f, 1.0f, 1.0f)
        Minecraft.getInstance().itemRenderer.renderStatic(
            stack,
            ItemDisplayContext.FIXED,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            phantom.level(),
            phantom.id
        )
        poseStack.popPose()
    }
}
