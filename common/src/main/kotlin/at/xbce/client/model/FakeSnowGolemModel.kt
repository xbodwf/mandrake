package at.xbce.client.model

import at.xbce.entity.FakeSnowGolemEntity
import net.minecraft.client.model.HierarchicalModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.util.Mth

/**
 * 假雪傀儡模型：苦力怕头 + 雪傀儡身体。
 * 戴南瓜时隐藏苦力怕头（另由南瓜头渲染层盖住），剪掉南瓜后露出苦力怕头。
 */
class FakeSnowGolemModel(private val root: ModelPart) : HierarchicalModel<FakeSnowGolemEntity>() {

    private val creeperHead: ModelPart = root.getChild("creeper_head")
    private val upperBody: ModelPart = root.getChild("upper_body")
    private val leftArm: ModelPart = root.getChild("left_arm")
    private val rightArm: ModelPart = root.getChild("right_arm")

    companion object {
        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val root = mesh.root
            val deformation = CubeDeformation(-0.5f)

            root.addOrReplaceChild(
                "creeper_head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f),
                PartPose.offset(0.0f, 4.0f, 0.0f)
            )
            root.addOrReplaceChild(
                "upper_body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-5.0f, -10.0f, -5.0f, 10.0f, 10.0f, 10.0f, deformation),
                PartPose.offset(0.0f, 13.0f, 0.0f)
            )
            root.addOrReplaceChild(
                "lower_body",
                CubeListBuilder.create().texOffs(0, 36).addBox(-6.0f, -12.0f, -6.0f, 12.0f, 12.0f, 12.0f, deformation),
                PartPose.offset(0.0f, 24.0f, 0.0f)
            )
            val arm = CubeListBuilder.create().texOffs(32, 0)
                .addBox(-1.0f, 0.0f, -1.0f, 12.0f, 2.0f, 2.0f, deformation)
            root.addOrReplaceChild(
                "left_arm", arm,
                PartPose.offsetAndRotation(5.0f, 6.0f, 1.0f, 0.0f, 0.0f, 1.0f)
            )
            root.addOrReplaceChild(
                "right_arm", arm,
                PartPose.offsetAndRotation(-5.0f, 6.0f, -1.0f, 0.0f, Math.PI.toFloat(), -1.0f)
            )

            return LayerDefinition.create(mesh, 64, 64)
        }
    }

    override fun root(): ModelPart = root

    fun getHead(): ModelPart = creeperHead

    override fun setupAnim(
        entity: FakeSnowGolemEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        // 戴南瓜时隐藏苦力怕头，防止穿模露馅
        creeperHead.visible = !entity.hasPumpkin()
        creeperHead.yRot = netHeadYaw * (Math.PI.toFloat() / 180f)
        creeperHead.xRot = headPitch * (Math.PI.toFloat() / 180f)

        upperBody.yRot = netHeadYaw * (Math.PI.toFloat() / 180f) * 0.25f
        val sin = Mth.sin(upperBody.yRot)
        val cos = Mth.cos(upperBody.yRot)

        leftArm.yRot = upperBody.yRot
        rightArm.yRot = upperBody.yRot + Mth.PI
        leftArm.x = cos * 5.0f
        leftArm.z = -sin * 5.0f
        rightArm.x = -cos * 5.0f
        rightArm.z = sin * 5.0f
    }
}
