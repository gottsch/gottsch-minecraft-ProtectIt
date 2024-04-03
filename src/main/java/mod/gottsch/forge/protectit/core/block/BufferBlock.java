package mod.gottsch.forge.protectit.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 *
 * @author Mark Gottschling on Mar 24, 2024
 *
 */
public class BufferBlock extends BorderBlock {

    private static final VoxelShape NORTH_SOUTH = Block.box(7D, 7D, 5D, 9D, 9D, 11D);
    private static final VoxelShape EAST_WEST = Block.box(5D, 7D, 7D, 11D, 9D, 9D);
    private static final VoxelShape UP_DOWN = Block.box(7D, 5D, 7D, 9D, 11D, 9D);

    private static final VoxelShape SHAPE = Shapes.or(NORTH_SOUTH, EAST_WEST, UP_DOWN);

    public BufferBlock(Properties properties) {
        super(properties);
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
