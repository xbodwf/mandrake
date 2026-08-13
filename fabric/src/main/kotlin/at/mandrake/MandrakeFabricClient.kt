package at.mandrake

import at.mandrake.block.CorePortalRenderer
import at.mandrake.block.ModBlockEntityTypes
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry

class MandrakeFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        BlockEntityRendererRegistry.register(
            ModBlockEntityTypes.CORE_PORTAL!!, ::CorePortalRenderer
        )
    }
}
