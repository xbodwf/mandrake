package at.xbce.client

import at.xbce.XBCE
import at.xbce.client.model.FakeSnowGolemModel
import at.xbce.client.render.FakeIronGolemRenderer
import at.xbce.client.render.FakeSnowGolemModelLayers
import at.xbce.client.render.FakeSnowGolemRenderer
import at.xbce.client.render.FakeVillagerRenderer
import at.xbce.client.render.TrueFormRenderer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent

object XBCENeoForgeClient {

    fun register() {
    }

    @SubscribeEvent
    fun onRegisterLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(FakeSnowGolemModelLayers.FAKE_SNOW_GOLEM) {
            FakeSnowGolemModel.createBodyLayer()
        }
    }

    @SubscribeEvent
    fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(XBCE.FAKE_SNOW_GOLEM) { ctx ->
            TrueFormRenderer(ctx, FakeSnowGolemRenderer(ctx), net.minecraft.client.renderer.entity.CreeperRenderer(ctx))
        }

        event.registerEntityRenderer(XBCE.FAKE_VILLAGER) { ctx ->
            FakeVillagerRenderer(ctx)
        }

        event.registerEntityRenderer(XBCE.FAKE_IRON_GOLEM) { ctx ->
            FakeIronGolemRenderer(ctx)
        }

        event.registerEntityRenderer(XBCE.CARAVAN_TRADER) { ctx ->
            at.xbce.client.render.CaravanTraderRenderer(ctx)
        }
    }
}
