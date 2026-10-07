package at.xbce.create.client

import at.xbce.XBCE
import at.xbce.create.CopperFamily
import at.xbce.create.CopperFinish
import at.xbce.create.CopperOxidizableBlock
import at.xbce.create.CopperStage
import at.xbce.create.XBCECopperBlocks
import at.xbce.create.copperBaseName
import com.simibubi.create.AllSpriteShifts
import com.simibubi.create.CreateClient
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour
import com.simibubi.create.content.kinetics.base.RotatingInstance
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel
import com.simibubi.create.foundation.block.connected.AllCTTypes
import com.simibubi.create.foundation.block.connected.CTModel
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry
import com.simibubi.create.foundation.block.connected.CTSpriteShifter
import com.simibubi.create.foundation.render.AllInstanceTypes
import dev.engine_room.flywheel.api.instance.Instance
import dev.engine_room.flywheel.api.model.Model
import dev.engine_room.flywheel.api.visual.BlockEntityVisual
import dev.engine_room.flywheel.api.visualization.VisualizationContext
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry
import dev.engine_room.flywheel.lib.model.Models
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import java.util.function.Consumer

/**
 * 铜动力学方块的客户端渲染：
 *  - Flywheel 视觉按（工艺 + 氧化度）选择对应贴图的部分模型；
 *  - 方块模型套上 Create 的 BracketedKineticBlockModel（正常渲染时只画支架，避免与视觉重复）；
 *  - BER 用 Create 的支架渲染器（Flywheel 关闭时兜底）。
 * 仅在 Create 存在时由 XBCENeoForgeClient / FMLClientSetupEvent 调用。
 */
object XBCECopperClient {

    private val partials = HashMap<String, PartialModel>()

    private fun partial(path: String): PartialModel =
        partials.getOrPut(path) { PartialModel.of(XBCE.id("block/$path")) }

    private fun model(stage: CopperStage, finish: CopperFinish, suffix: String): Model =
        Models.partial(partial("${copperBaseName(stage, finish)}_$suffix"))

    /**
     * 每个氧化度一套自己的 CT（连接纹理）shift。Create 的 CTModel 只在
     * "模型用的 sprite == shift.getOriginal()" 时才重写 UV，所以：
     *  - 纯铜（用官方贴图 create:block/copper_casing）继续复用官方 COPPER_CASING；
     *  - 其余氧化度必须指向自己那套贴图，connected 图由 build/gen_casing_connected.py
     *    对 create:block/copper_casing_connected 做与本体同样的 HSV 迁移生成（128x128 表）。
     */
    private val casingShifts: Map<CopperStage, CTSpriteShiftEntry> = mapOf(
        CopperStage.COPPER to AllSpriteShifts.COPPER_CASING,
        CopperStage.EXPOSED to casingShift("exposed"),
        CopperStage.WEATHERED to casingShift("weathered"),
        CopperStage.OXIDIZED to casingShift("oxidized"),
    )

    private fun casingShift(name: String): CTSpriteShiftEntry = CTSpriteShifter.getCT(
        AllCTTypes.OMNIDIRECTIONAL,
        XBCE.id("block/${name}_copper_casing"),
        XBCE.id("block/${name}_copper_casing_connected")
    )

    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        val type = XBCECopperBlocks.beType()
        event.registerBlockEntityRenderer(type) { ctx -> BracketedKineticBlockEntityRenderer(ctx) }
    }

    fun onClientSetup() {
        // 关键：必须在模型烘焙前注册全部 PartialModel（否则懒加载会拿到 missing model → 紫黑）
        for (stage in CopperStage.entries) {
            for (finish in CopperFinish.entries) {
                val base = copperBaseName(stage, finish)
                partial("${base}_shaft")
                partial("${base}_cogwheel")
                partial("${base}_large_cogwheel_shaftless")
                partial("${base}_cogwheel_shaft")
            }
        }

        val type = XBCECopperBlocks.beType()

        VisualizerRegistry.setVisualizer(
            type,
            SimpleBlockEntityVisualizer.builder(type)
                .factory { context, be, partialTick -> createVisual(context, be, partialTick) }
                .apply()
        )

        // 支架模型包装（同 Create 原生 kinetic 方块）
        for (holder in XBCECopperBlocks.blockHolders()) {
            val block = holder.get()
            CreateClient.MODEL_SWAPPER.customBlockModels.register(BuiltInRegistries.BLOCK.getKey(block)) { baked ->
                BracketedKineticBlockModel(baked)
            }
        }

        // 铜机壳：注册到 Create 的机壳连接纹理（同氧化度的机壳相邻时接缝消失，视觉上连成整体）
        for (holder in XBCECopperBlocks.casingHolders()) {
            val block = holder.get()
            val stage = (block as? CopperOxidizableBlock)?.stage ?: CopperStage.COPPER
            val shift = casingShifts.getValue(stage)
            // CasingConnectivity$Entry.getCasing() 之间是引用比较（if_acmpeq），所以只有共用同一个
            // shift 对象的方块才会互相连接：同氧化度的涂蜡/未涂蜡共用一套贴图 ⇒ 共用 shift ⇒ 相连；
            // 不同氧化度贴图不同 ⇒ 各自一套 shift ⇒ 不会连错色。
            CreateClient.CASING_CONNECTIVITY.makeCasing(block, shift)
            CreateClient.MODEL_SWAPPER.customBlockModels.register(BuiltInRegistries.BLOCK.getKey(block)) { baked ->
                CTModel(baked, EncasedCTBehaviour(shift))
            }
        }
    }

    private fun createVisual(
        context: VisualizationContext,
        be: BracketedKineticBlockEntity,
        partialTick: Float
    ): BlockEntityVisual<BracketedKineticBlockEntity> {
        val oxidizable = be.blockState.block as? CopperOxidizableBlock
        val stage = oxidizable?.stage ?: CopperStage.COPPER
        val finish = oxidizable?.finish ?: CopperFinish.PLAIN
        return when (oxidizable?.family) {
            CopperFamily.LARGE_COGWHEEL -> CopperLargeCogVisual(
                context, be, partialTick,
                model(stage, finish, "large_cogwheel_shaftless"),
                model(stage, finish, "cogwheel_shaft")
            )
            CopperFamily.COGWHEEL -> SingleAxisRotatingVisual(context, be, partialTick, model(stage, finish, "cogwheel"))
            else -> SingleAxisRotatingVisual(context, be, partialTick, model(stage, finish, "shaft"))
        }
    }

    /** 大齿轮 = 无轴大轮 + 独立旋转的中轴（照抄 Create 的 LargeCogVisual 结构）。 */
    private class CopperLargeCogVisual(
        context: VisualizationContext,
        be: BracketedKineticBlockEntity,
        partialTick: Float,
        model: Model,
        shaftModel: Model
    ) : SingleAxisRotatingVisual<BracketedKineticBlockEntity>(context, be, partialTick, model) {

        private val additionalShaft: RotatingInstance = instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, shaftModel)
            .createInstance()
            .rotateToFace(rotationAxis())
            .setup(be)
            .setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos))
            .setPosition(visualPosition)
            .also { it.setChanged() }

        override fun update(partialTick: Float) {
            super.update(partialTick)
            additionalShaft
                .setup(blockEntity)
                .setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos))
                .setChanged()
        }

        // 关键：中轴是独立实例，必须一起补光，否则它的 light 停在 0 → 渲染成黑色
        override fun updateLight(partialTick: Float) {
            super.updateLight(partialTick)
            relight(additionalShaft)
        }

        override fun collectCrumblingInstances(consumer: Consumer<Instance?>) {
            super.collectCrumblingInstances(consumer)
            consumer.accept(additionalShaft)
        }

        override fun _delete() {
            super._delete()
            additionalShaft.delete()
        }
    }
}
