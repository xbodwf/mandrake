package at.xbce.create.client

import at.xbce.XBCE
import at.xbce.create.CopperFamily
import at.xbce.create.CopperFinish
import at.xbce.create.CopperStage
import at.xbce.create.XBCECopperBlocks
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.ponder.api.registration.PonderPlugin
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper
import net.createmod.ponder.api.scene.PonderStoryBoard
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.foundation.PonderIndex
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties

/**
 * xbce 铜方块的思索（Ponder）场景。
 *
 * 场景结构文件在 `assets/xbce/ponder/<path>.nbt`，由 build/gen_ponder_schematics.py 生成：
 * - shaft/relay、cog/small、cog/large 复制自 Create 官方场景，方块换成普通铜；
 * - variants/ 目录下是各家族的"变种介绍"（氧化 → 涂蜡 → 刮蜡）。
 *
 * 注册时机：在 FMLClientSetupEvent 里 addPlugin；Ponder 要到 FMLLoadCompleteEvent
 * （PonderClient.modLoadCompleted）才 PonderIndex.registerAll()，所以肯定来得及。
 * 语言键：`xbce.ponder.<title 路径>.header` / `.text_N`（title() 的第一个参数是路径）。
 */
object XBCECopperPonder {

    private val COPPER_TAG: ResourceLocation = XBCE.id("copper")

    fun register() {
        PonderIndex.addPlugin(object : PonderPlugin {
            override fun getModId(): String = XBCE.MOD_ID

            override fun registerScenes(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
                val shafts = familyItems(CopperFamily.SHAFT)
                val cogs = familyItems(CopperFamily.COGWHEEL)
                val largeCogs = familyItems(CopperFamily.LARGE_COGWHEEL)
                val casings = casingItems()

                helper.forComponents(*shafts)
                    .addStoryBoard("shaft/relay", PonderStoryBoard { b, u -> shaftRelay(b, u) }, COPPER_TAG)
                    .addStoryBoard("variants/shaft", PonderStoryBoard { b, u -> shaftVariants(b, u) }, COPPER_TAG)

                helper.forComponents(*cogs)
                    .addStoryBoard("cog/small", PonderStoryBoard { b, u -> cogRelay(b, u) }, COPPER_TAG)
                    .addStoryBoard("variants/cogwheel", PonderStoryBoard { b, u -> cogVariants(b, u) }, COPPER_TAG)

                helper.forComponents(*largeCogs)
                    .addStoryBoard("cog/large", PonderStoryBoard { b, u -> largeCogRelay(b, u) }, COPPER_TAG)
                    .addStoryBoard("variants/large_cogwheel", PonderStoryBoard { b, u -> largeCogVariants(b, u) }, COPPER_TAG)

                // 机壳家族（含官方 create:copper_casing，它的氧化/打蜡由我们的 mixin 接管）
                helper.forComponents(*casings)
                    .addStoryBoard("variants/casing", PonderStoryBoard { b, u -> casingVariants(b, u) }, COPPER_TAG)
            }

            override fun registerTags(helper: PonderTagRegistrationHelper<ResourceLocation>) {
                helper.registerTag(COPPER_TAG)
                    .addToIndex()
                    .item(XBCECopperBlocks.variant(CopperFamily.SHAFT, CopperStage.COPPER, CopperFinish.PLAIN, false))
                    .title("铜质方块")
                    .description("铜质系列：传动杆、齿轮与机壳的氧化与涂蜡")
                    .register()
            }
        })
    }

    // ------------------------------------------------------------ 场景注册辅助

    private fun rl(block: Block): ResourceLocation = BuiltInRegistries.BLOCK.getKey(block)

    /** 某家族全部变体的物品 id（4 氧化度 × 3 工艺 × 2 涂蜡 = 24）。 */
    private fun familyItems(family: CopperFamily): Array<ResourceLocation> = buildList {
        for (stage in CopperStage.entries) {
            for (finish in CopperFinish.entries) {
                for (waxed in listOf(false, true)) {
                    add(rl(XBCECopperBlocks.variant(family, stage, finish, waxed)))
                }
            }
        }
    }.toTypedArray()

    /** 机壳家族 8 个物品 id（纯铜归官方 create:copper_casing，涂蜡纯铜 + 6 个氧化变种归我们）。 */
    private fun casingItems(): Array<ResourceLocation> = buildList {
        for (stage in CopperStage.entries) {
            for (waxed in listOf(false, true)) {
                add(rl(XBCECopperBlocks.variant(CopperFamily.CASING, stage, CopperFinish.PLAIN, waxed)))
            }
        }
    }.toTypedArray()

    /**
     * 设置方块的 axis。
     * Create 的 RotatedPillarKineticBlock.AXIS 就是 BlockStateProperties.AXIS（字节码已确认：
     * clinit 里 `getstatic BlockStateProperties.AXIS; putstatic AXIS`），我们的铜方块继承同一状态定义，
     * 所以直接用这个单例即可。
     */
    private fun withAxis(state: BlockState, axis: Direction.Axis): BlockState =
        state.setValue(BlockStateProperties.AXIS, axis)

    private fun plainStage(stage: CopperStage, family: CopperFamily, waxed: Boolean): BlockState =
        XBCECopperBlocks.variant(family, stage, CopperFinish.PLAIN, waxed).defaultBlockState()

    // ------------------------------------------------------------ 机械原理场景（照抄 Create，方块换铜质）

    /** 复制自 Create 的 shaft/relay（title "shaft"）。 */
    private fun shaftRelay(sb: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sb)
        scene.title("copper_shaft", "Relaying rotational force using Copper Shafts")
        scene.configureBasePlate(0, 0, 5)
        scene.world().showSection(util.select().layer(0), Direction.UP)

        val centre = util.grid().at(0, 1, 2)
        val centreSel = util.select().position(centre)
        scene.world().showSection(centreSel, Direction.UP)
        scene.world().setKineticSpeed(centreSel, 0f)
        scene.idle(5)
        scene.world().showSection(util.select().position(5, 1, 2), Direction.DOWN)
        scene.idle(10)
        for (i in 4 downTo 1) {
            if (i == 2) scene.rotateCameraY(70f)
            scene.idle(5)
            scene.world().showSection(util.select().position(i, 1, 2), Direction.DOWN)
        }
        scene.world().setKineticSpeed(centreSel, 64f)
        scene.effects().indicateSuccess(centre)
        scene.idle(10)
        scene.overlay().showText(1000)
            .placeNearTarget()
            .text("铜传动杆会沿直线传递旋转力，和普通传动杆一样。")
            .pointAt(util.vector().of(3.0, 1.5, 2.5))
        scene.idle(20)
        scene.markAsFinished()
    }

    /** 复制自 Create 的 cog/small（title "cogwheel"）。 */
    private fun cogRelay(sb: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sb)
        scene.title("copper_cogwheel", "Relaying rotational force using Copper Cogwheels")
        scene.configureBasePlate(0, 0, 5)

        val motor = util.select().position(util.grid().at(4, 1, 1))
        scene.world().showSection(util.select().layer(0), Direction.UP)
        scene.world().showSection(motor, Direction.UP)
        scene.world().setKineticSpeed(motor, 0f)
        scene.idle(5)
        scene.world().showSection(util.select().fromTo(1, 1, 3, 1, 1, 5), Direction.DOWN)
        scene.idle(10)
        for (i in 1..4) {
            scene.idle(5)
            if (i == 2) scene.world().showSection(util.select().position(0, 1, 2), Direction.DOWN)
            scene.world().showSection(util.select().position(i, 1, 2), Direction.DOWN)
        }
        scene.world().setKineticSpeed(motor, 64f)
        scene.effects().indicateSuccess(util.grid().at(4, 1, 1))
        scene.idle(10)
        scene.overlay().showText(60)
            .text("相邻的铜齿轮会互相啮合，传递旋转力。")
            .pointAt(util.vector().blockSurface(util.grid().at(0, 1, 2), Direction.EAST))
        scene.idle(60)
        scene.world().showSection(util.select().fromTo(1, 1, 1, 2, 1, 1), Direction.SOUTH)
        scene.idle(10)
        scene.effects().rotationDirectionIndicator(util.grid().at(1, 1, 1))
        scene.effects().rotationDirectionIndicator(util.grid().at(2, 1, 1))
        scene.idle(20)
        scene.overlay().showText(100)
            .text("这样连接的相邻传动杆会朝相反方向旋转。")
            .placeNearTarget()
            .attachKeyFrame()
            .pointAt(util.vector().blockSurface(util.grid().at(1, 1, 2), Direction.NORTH))
        scene.idle(70)
        scene.markAsFinished()
    }

    /** 复制自 Create 的 cog/large（title "large_cogwheel"）。 */
    private fun largeCogRelay(sb: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sb)
        scene.title("copper_large_cogwheel", "Relaying rotational force using Copper Large Cogwheels")
        scene.configureBasePlate(1, 1, 5)
        scene.world().setBlock(
            util.grid().at(4, 2, 3),
            withAxis(plainStage(CopperStage.COPPER, CopperFamily.LARGE_COGWHEEL, false), Direction.Axis.X),
            false
        )
        scene.showBasePlate()
        scene.idle(5)
        scene.world().showSection(util.select().layer(1), Direction.DOWN)
        scene.idle(5)
        scene.world().showSection(util.select().position(3, 2, 4), Direction.NORTH)
        for (i in 3 downTo 1) {
            scene.idle(5)
            if (i == 3) scene.world().showSection(util.select().position(3, 2, 5), Direction.DOWN)
            scene.world().showSection(util.select().position(3, 2, i), Direction.DOWN)
        }
        scene.overlay().showText(70)
            .text("铜大齿轮可以直角相连，把旋转力传到另一个方向。")
            .placeNearTarget()
            .pointAt(util.vector().centerOf(3, 1, 4))
        scene.idle(70)
        scene.world().hideSection(util.select().fromTo(3, 2, 1, 3, 2, 5), Direction.SOUTH)
        scene.idle(15)
        scene.world().modifyBlock(util.grid().at(3, 2, 3), { s -> withAxis(s, Direction.Axis.X) }, false)
        scene.world().setKineticSpeed(util.select().fromTo(1, 2, 3, 5, 2, 3), 16f)
        scene.world().showSection(util.select().position(4, 2, 3), Direction.WEST)
        for (i in 3 downTo 1) {
            scene.idle(5)
            if (i == 3) scene.world().showSection(util.select().position(5, 2, 3), Direction.DOWN)
            scene.world().showSection(util.select().position(i, 2, 3), Direction.DOWN)
        }
        scene.idle(5)
        scene.overlay().showText(90)
            .text("它会帮助把转速传递到另一个旋转轴。")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(1, 2, 3), Direction.WEST))
        scene.effects().rotationSpeedIndicator(util.grid().at(3, 1, 3))
        scene.effects().rotationSpeedIndicator(util.grid().at(4, 2, 3))
        scene.idle(60)
        scene.markAsFinished()
    }

    // ------------------------------------------------------------ 变种介绍场景（每个家族各演自己的方块）

    /**
     * 变种场景的公共节奏：地板 → 逐个亮出四档氧化 → 亮出涂蜡样例 →
     * 整排转动（证明氧化不改传动）→ 把中间那块涂蜡再刮蜡。
     * 行布局（schematic 的 variants 目录）：y=1, z=2, x=0..5。
     */
    private fun variantScene(
        sb: SceneBuilder,
        util: SceneBuildingUtil,
        title: String,
        header: String,
        family: CopperFamily,
        kinetic: Boolean,
        text1: String,
        text2: String,
        text3: String
    ) {
        val scene = CreateSceneBuilder(sb)
        scene.title(title, header)
        scene.configureBasePlate(0, 0, 5)
        scene.world().showSection(util.select().layer(0), Direction.UP)
        scene.idle(5)

        // 四档氧化逐个亮出（x=0 新 → x=3 氧化）
        scene.world().showSection(util.select().position(0, 1, 2), Direction.DOWN)
        scene.idle(10)
        scene.overlay().showText(100)
            .placeNearTarget()
            .text(text1)
            .pointAt(util.vector().centerOf(1, 1, 2))
        scene.idle(15)
        scene.world().showSection(util.select().position(1, 1, 2), Direction.DOWN)
        scene.idle(15)
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN)
        scene.idle(15)
        scene.world().showSection(util.select().position(3, 1, 2), Direction.DOWN)
        scene.idle(45)

        // 涂蜡样例（x=4 涂蜡纯铜，x=5 涂蜡氧化）
        scene.world().showSection(util.select().position(4, 1, 2), Direction.DOWN)
        scene.idle(15)
        scene.world().showSection(util.select().position(5, 1, 2), Direction.DOWN)
        scene.idle(15)

        if (kinetic) {
            // 整排转动：氧化/涂蜡不改变传动能力
            scene.world().setKineticSpeed(util.select().fromTo(0, 1, 2, 5, 1, 2), 64f)
            scene.effects().indicateSuccess(util.grid().at(3, 1, 2))
            scene.idle(10)
            scene.overlay().showText(90)
                .placeNearTarget()
                .text(text2)
                .pointAt(util.vector().centerOf(2, 1, 2))
            scene.idle(70)
        } else {
            scene.overlay().showText(90)
                .placeNearTarget()
                .text(text2)
                .pointAt(util.vector().centerOf(2, 1, 2))
            scene.idle(80)
        }

        // 涂蜡：锈蚀档 → 涂蜡的锈蚀档
        scene.world().modifyBlock(
            util.grid().at(2, 1, 2),
            { _ -> plainStage(CopperStage.WEATHERED, family, true) },
            false
        )
        scene.effects().indicateSuccess(util.grid().at(2, 1, 2))
        if (kinetic) scene.world().setKineticSpeed(util.select().fromTo(0, 1, 2, 5, 1, 2), 64f)
        scene.idle(20)
        scene.overlay().showText(140)
            .placeNearTarget()
            .text(text3)
            .pointAt(util.vector().centerOf(2, 1, 2))
        scene.idle(60)

        // 刮蜡：涂蜡的锈蚀档 → 锈蚀档（之后又会继续氧化）
        scene.world().modifyBlock(
            util.grid().at(2, 1, 2),
            { _ -> plainStage(CopperStage.WEATHERED, family, false) },
            false
        )
        scene.effects().indicateSuccess(util.grid().at(2, 1, 2))
        if (kinetic) scene.world().setKineticSpeed(util.select().fromTo(0, 1, 2, 5, 1, 2), 64f)
        scene.idle(50)
        scene.markAsFinished()
    }

    private fun shaftVariants(sb: SceneBuilder, util: SceneBuildingUtil) = variantScene(
        sb, util, "copper_shaft_variants", "Copper Shaft Variants", CopperFamily.SHAFT, kinetic = true,
        text1 = "铜方块会缓慢氧化：崭新 → 斑驳 → 锈蚀 → 氧化，和原版铜方块一样。",
        text2 = "氧化与涂蜡只改变外观，传动能力完全不变。",
        text3 = "右键蜜脾涂蜡可锁定当前状态；用斧头刮掉蜡层后，它又会继续氧化。"
    )

    private fun cogVariants(sb: SceneBuilder, util: SceneBuildingUtil) = variantScene(
        sb, util, "copper_cogwheel_variants", "Copper Cogwheel Variants", CopperFamily.COGWHEEL, kinetic = true,
        text1 = "铜齿轮会缓慢氧化：崭新 → 斑驳 → 锈蚀 → 氧化，和原版铜方块一样。",
        text2 = "氧化与涂蜡只改变外观，啮合与传动完全不变。",
        text3 = "右键蜜脾涂蜡可锁定当前状态；用斧头刮掉蜡层后，它又会继续氧化。"
    )

    private fun largeCogVariants(sb: SceneBuilder, util: SceneBuildingUtil) = variantScene(
        sb, util, "copper_large_cogwheel_variants", "Copper Large Cogwheel Variants", CopperFamily.LARGE_COGWHEEL, kinetic = true,
        text1 = "铜大齿轮会缓慢氧化：崭新 → 斑驳 → 锈蚀 → 氧化，和原版铜方块一样。",
        text2 = "氧化与涂蜡只改变外观，啮合与传动完全不变。",
        text3 = "右键蜜脾涂蜡可锁定当前状态；用斧头刮掉蜡层后，它又会继续氧化。"
    )

    private fun casingVariants(sb: SceneBuilder, util: SceneBuildingUtil) = variantScene(
        sb, util, "copper_casing_variants", "Copper Casing Variants", CopperFamily.CASING, kinetic = false,
        text1 = "铜机壳会缓慢氧化：崭新 → 斑驳 → 锈蚀 → 氧化，和其它铜方块一样。",
        text2 = "所有铜机壳的氧化进度都与原版铜方块一致。",
        text3 = "右键蜜脾涂蜡可锁定当前状态；用斧头刮掉蜡层后，它又会继续氧化。"
    )
}
