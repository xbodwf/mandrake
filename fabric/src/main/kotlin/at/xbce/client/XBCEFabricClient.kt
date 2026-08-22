package at.xbce.client

import at.xbce.XBCE
import at.xbce.client.render.FakeIronGolemRenderer
import at.xbce.client.render.FakeSnowGolemModelLayers
import at.xbce.client.render.FakeSnowGolemRenderer
import at.xbce.client.render.FakeVillagerRenderer
import at.xbce.client.render.TrueFormRenderer
import at.xbce.client.model.FakeSnowGolemModel
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.renderer.entity.CreeperRenderer

class XBCEFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(FakeSnowGolemModelLayers.FAKE_SNOW_GOLEM) {
            FakeSnowGolemModel.createBodyLayer()
        }

        EntityRendererRegistry.register(XBCE.FAKE_SNOW_GOLEM) { ctx ->
            // 举显形望远镜时直接呈现苦力怕本体
            TrueFormRenderer(ctx, FakeSnowGolemRenderer(ctx), CreeperRenderer(ctx))
        }

        EntityRendererRegistry.register(XBCE.FAKE_VILLAGER) { ctx ->
            FakeVillagerRenderer(ctx)
        }

        EntityRendererRegistry.register(XBCE.FAKE_IRON_GOLEM) { ctx ->
            FakeIronGolemRenderer(ctx)
        }
    }
}
