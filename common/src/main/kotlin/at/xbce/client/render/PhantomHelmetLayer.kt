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
 * 幻翼赃物层：偷来的头盔不戴着，而是像爪下攥着战利品一样
 * 挂在身体正下方（对齐 body 部件，随俯仰摆动）。
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

        // 挂在身体正下方：对齐 body 部件（随俯仰摆动），再往下偏移
        // 注意：PoseStack.translate 单位是"格"，模型像素要除以 16
        val body = this.parentModel.root().getChild("body")
        poseStack.pushPose()
        body.translateAndRotate(poseStack)
        // Model space uses +Y for down: positive Y hangs the helmet below the body.
        poseStack.translate(0.0f, 0.30f, -0.20f)
        poseStack.scale(0.75f, 0.75f, 0.75f)

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
