package at.mandrake.worldgen.density

import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.KeyDispatchDataCodec
import net.minecraft.world.level.levelgen.DensityFunction

/** Smoothly closes the artificial vault. Positive density means solid terrain. */
class CoreCeilingFunction(
    private val fromY: Int = 410,
    private val toY: Int = 520
) : DensityFunction {

    init {
        require(toY > fromY) { "to_y must be greater than from_y" }
    }

    override fun compute(context: DensityFunction.FunctionContext): Double {
        val progress = ((context.blockY() - fromY).toDouble() / (toY - fromY)).coerceIn(0.0, 1.0)
        val smoothStep = progress * progress * (3.0 - 2.0 * progress)
        return -0.25 + smoothStep * 3.45
    }

    override fun fillArray(arr: DoubleArray, provider: DensityFunction.ContextProvider) =
        provider.fillAllDirectly(arr, this)

    override fun mapAll(visitor: DensityFunction.Visitor): DensityFunction = visitor.apply(this)

    override fun minValue() = -0.25

    override fun maxValue() = 3.2

    override fun codec(): KeyDispatchDataCodec<out DensityFunction> = CODEC

    companion object {
        val CODEC: KeyDispatchDataCodec<CoreCeilingFunction> = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("from_y", 410).forGetter { it.fromY },
                    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("to_y", 520).forGetter { it.toY }
                ).apply(instance, ::CoreCeilingFunction)
            }
        )
    }
}
