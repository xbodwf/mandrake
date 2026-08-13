package at.mandrake.worldgen

import at.mandrake.Mandrake
import at.mandrake.registry.ModDimensions
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Server-authoritative, platform-neutral environmental pressure for Earth Core. */
object CoreHazardHandler {
    private data class Exposure(var spores: Int = 0, var heat: Int = 0)

    private val exposure = ConcurrentHashMap<UUID, Exposure>()

    fun playerTick(player: ServerPlayer) {
        if (player.tickCount % 20 != 0) return

        if (player.level().dimension() != ModDimensions.EARTH_CORE_KEY || player.isCreative || player.isSpectator) {
            exposure.remove(player.uuid)
            return
        }

        val state = exposure.computeIfAbsent(player.uuid) { Exposure() }
        val biome = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null)?.location()
        val inFungalForest = biome == Mandrake.id("fungal_forest")
        val inHeatZone = biome == Mandrake.id("magma_plains") || biome == Mandrake.id("basalt_groves")

        state.spores = approach(state.spores, if (inFungalForest) 100 else 0, if (inFungalForest) 4 else 7)
        val protectedFromHeat = player.hasEffect(MobEffects.FIRE_RESISTANCE)
        state.heat = approach(state.heat, if (inHeatZone && !protectedFromHeat) 100 else 0, if (inHeatZone) 5 else 8)

        when {
            state.spores >= 80 -> {
                player.displayClientMessage(Component.translatable("hazard.mandrake.spores.critical"), true)
                player.addEffect(MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, true))
                player.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false, true))
            }
            state.spores >= 45 -> player.displayClientMessage(Component.translatable("hazard.mandrake.spores.warning"), true)
            state.heat >= 80 -> {
                player.displayClientMessage(Component.translatable("hazard.mandrake.heat.critical"), true)
                player.hurt(player.damageSources().onFire(), 2.0f)
            }
            state.heat >= 45 -> player.displayClientMessage(Component.translatable("hazard.mandrake.heat.warning"), true)
        }
    }

    private fun approach(value: Int, target: Int, step: Int): Int = when {
        value < target -> (value + step).coerceAtMost(target)
        value > target -> (value - step).coerceAtLeast(target)
        else -> value
    }
}
