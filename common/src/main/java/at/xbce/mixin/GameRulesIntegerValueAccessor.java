package at.xbce.mixin;

import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRules.IntegerValue.class)
public interface GameRulesIntegerValueAccessor {

    @Invoker("create")
    static GameRules.Type<GameRules.IntegerValue> xbce$create(int defaultValue) {
        throw new AssertionError();
    }
}
