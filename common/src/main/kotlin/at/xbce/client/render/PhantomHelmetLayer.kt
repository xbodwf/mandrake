package at.xbce.client.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.tags.ItemTags
import net.minecraft.util.FastColor.ARGB32
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor

/**
 * 幻翼专属头盔层（原版 HumanoidArmorLayer 的泛型要求父模型是人形，幻翼挂不上）。
 * 手动对齐幻翼模型的 body > head 部件，把偷来的头盔渲染上去。
 */
class PhantomHelmetLayer(
    parent: RenderLayerParent<Phantom, PhantomModel<Phantom>>,
    private val helmetModel: HumanoidModel<Phantom>
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
        val stack: ItemStack = phantom.getItemBySlot(EquipmentSlot.HEAD)
        val item = stack.item
        if (item !is ArmorItem || item.equipmentSlot != EquipmentSlot.HEAD) return

        // 对齐幻翼头部部件（含俯仰姿态），再缩放盖住小脑袋
        val head = this.parentModel.root().getChild("body").getChild("head")
        poseStack.pushPose()
        head.translateAndRotate(poseStack)
        poseStack.translate(0.0f, 0.9f, -2.4f)
        poseStack.scale(0.55f, 0.55f, 0.55f)

        helmetModel.setAllVisible(false)
        helmetModel.head.visible = true
        helmetModel.hat.visible = true

        val color = if (stack.`is`(ItemTags.DYEABLE)) ARGB32.opaque(DyedItemColor.getOrDefault(stack, -6265536)) else -1
        for (layer in item.material.value().layers()) {
            val vertexConsumer = bufferSource.getBuffer(RenderType.armorCutoutNoCull(layer.texture(false)))
            helmetModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, color)
        }
        if (stack.hasFoil()) {
            helmetModel.renderToBuffer(
                poseStack,
                bufferSource.getBuffer(RenderType.armorEntityGlint()),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }

        poseStack.popPose()
    }
}
