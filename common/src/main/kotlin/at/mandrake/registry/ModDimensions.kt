package at.mandrake.registry

import at.mandrake.Mandrake
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.dimension.DimensionType

object ModDimensions {
    val EARTH_CORE_KEY = ResourceKey.create(Registries.DIMENSION, Mandrake.id("earth_core"))
    val EARTH_CORE_TYPE_KEY = ResourceKey.create(Registries.DIMENSION_TYPE, Mandrake.id("earth_core"))
}
