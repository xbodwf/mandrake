package at.mandrake

import at.mandrake.block.CorePortalBlockEntity
import at.mandrake.block.CorePortalRenderer
import at.mandrake.block.ModBlockEntityTypes
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.EntityRenderersEvent

@EventBusSubscriber(value = [Dist.CLIENT], bus = EventBusSubscriber.Bus.MOD)
object MandrakeNeoForgeClient {

    @SubscribeEvent
    @JvmStatic
    fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        @Suppress("UNCHECKED_CAST")
        val type = ModBlockEntityTypes.CORE_PORTAL as? net.minecraft.world.level.block.entity.BlockEntityType<CorePortalBlockEntity>
        if (type != null) {
            event.registerBlockEntityRenderer(type) { ctx -> CorePortalRenderer(ctx) }
        }
    }
}
