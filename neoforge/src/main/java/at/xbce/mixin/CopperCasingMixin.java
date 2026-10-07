package at.xbce.mixin;

import at.xbce.create.CopperOxidation;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 直接接管 Create 官方铜机壳（create:copper_casing），让它成为我们氧化链里的"纯机壳"一环：
 *  - 自然氧化：随机刻按原版铜的概率推进到 xbce:exposed_copper_casing；
 *  - 蜜脾打蜡：转换为 xbce:waxed_copper_casing，并授予原版"涂蜡 (Wax On)"成就。
 * 因此模组不再需要自己注册一个与官方完全重复的纯铜机壳方块；
 * 其余变体（涂蜡纯机壳 + 斑驳/锈蚀/氧化 × 涂蜡）仍由 xbce 正常注册。
 * 仅 Create 存在时生效：mixin 配置只挂在 neoforge 模块，目标类缺失时 Mixin 只会记一条警告。
 */
@Mixin(CasingBlock.class)
public abstract class CopperCasingMixin extends Block {

    protected CopperCasingMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * 为什么无条件返回 true：BlockState 会在方块注册期间（BlockBehaviour.BlockStateBase#initCache）
     * 就把随机刻开关缓存下来，那一刻 AllBlocks.COPPER_CASING 还没赋值，无法按 ID 做身份判断。
     * 所以这里统一放行，真正的身份判断放在 randomTick 里（随机刻命中时才比较一次，代价可忽略）。
     */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (xbce$isOfficialCopperCasing()) {
            CopperOxidation.INSTANCE.tickOfficialCasing(level, pos, random);
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (xbce$isOfficialCopperCasing() && stack.is(Items.HONEYCOMB)) {
            return CopperOxidation.INSTANCE.waxOfficialCasing(level, pos, player, stack);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Unique
    private boolean xbce$isOfficialCopperCasing() {
        return (Object) this == AllBlocks.COPPER_CASING.get();
    }
}
