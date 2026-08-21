package at.xbce.client

import at.xbce.XBCE
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent

object XBCENeoForgeClient {

    fun register() {
    }

    @SubscribeEvent
    fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(XBCE.FAKE_SNOW_GOLEM) { ctx ->
            net.minecraft.client.renderer.entity.SnowGolemRenderer(ctx)
        }

        event.registerEntityRenderer(XBCE.FAKE_VILLAGER) { ctx ->
            net.minecraft.client.renderer.entity.VillagerRenderer(ctx)
        }

        event.registerEntityRenderer(XBCE.FAKE_IRON_GOLEM) { ctx ->
            net.minecraft.client.renderer.entity.IronGolemRenderer(ctx)
        }
    }
}
