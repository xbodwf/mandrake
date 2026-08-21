package at.xbce.client

import at.xbce.XBCE
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.renderer.entity.IronGolemRenderer
import net.minecraft.client.renderer.entity.SnowGolemRenderer
import net.minecraft.client.renderer.entity.VillagerRenderer

class XBCEFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        EntityRendererRegistry.register(XBCE.FAKE_SNOW_GOLEM) { ctx ->
            SnowGolemRenderer(ctx)
        }

        EntityRendererRegistry.register(XBCE.FAKE_VILLAGER) { ctx ->
            VillagerRenderer(ctx)
        }

        EntityRendererRegistry.register(XBCE.FAKE_IRON_GOLEM) { ctx ->
            IronGolemRenderer(ctx)
        }
    }
}
