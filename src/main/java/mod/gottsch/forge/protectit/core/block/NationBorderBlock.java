package mod.gottsch.forge.protectit.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Mark Gottschling on Mar 23, 2024
 *
 */
public class NationBorderBlock extends BorderBlock {

    public NationBorderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(@NotNull BlockState state, ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getGameTime() % 5 == 0) {
            tick(state, level, pos, random);
        }
    }
}
