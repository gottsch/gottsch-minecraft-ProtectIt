package mod.gottsch.forge.protectit.core.block;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.ParcelUtil;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Mar 18, 2024
 *
 */
public class FoundationStoneBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    public FoundationStoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        FoundationStoneBlockEntity blockEntity = null;
        try {
            blockEntity = new FoundationStoneBlockEntity(pos, state);
        }
        catch(Exception e) {
            ProtectIt.LOGGER.error(e);
        }
//        ProtectIt.LOGGER.debug("createNewTileEntity | blockEntity -> {}}", blockEntity);
        return blockEntity;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    // run the ticker
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, blockState, t) -> {
                if (t instanceof FoundationStoneBlockEntity entity) { // test and cast
                    entity.tickServer();
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * this get called only when the block is placed by the player.
     * this can only happen within parcels that the player has access too.
     * therefor there cannot be any overlaps with other parcels,
     * other than nation parcels.
     * @param worldIn
     * @param pos
     * @param state
     * @param placer
     * @param stack
     */
    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockEntity blockEntity = worldIn.getBlockEntity(pos);
        ProtectIt.LOGGER.debug("setPlacedBy called.");

        if (blockEntity instanceof FoundationStoneBlockEntity foundationStoneEntity) {

            foundationStoneEntity.updateParcelBorder();

//            foundationStoneEntity.setOwnerId(placer.getUUID());
//            // save any intersects with this parcel
//            List<Box> overlaps = ParcelRegistry.findBoxes(foundationStoneEntity.getSize().getMinCoords(), foundationStoneEntity.getSize().getMaxCoords(), false, false);
//            // TODO getSize() is used wrong here. it is relative. need getBox()
//            ProtectIt.LOGGER.debug("num of overlaps @ {} <--> {} -> {}", foundationStoneEntity.getSize().getMinCoords().toShortString(), foundationStoneEntity.getSize().getMaxCoords().toShortString(), overlaps.size());
//            if (!overlaps.isEmpty()) {
//                foundationStoneEntity.getOverlaps().addAll(overlaps);
//            }
        }
    }

    //    @Override
//    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
//        if (!level.isClientSide()) {
//            FoundationStoneBlockEntity blockEntity = (FoundationStoneBlockEntity) level.getBlockEntity(pos);
//            if (blockEntity != null) {
//                blockEntity.removeParcelBorder();
//            }
//        }
//        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
//    }

    /**
     * Called whenever the block is remove - either destroyed by player or level.destroyBlock()
     * @param state
     * @param level
     * @param pos
     * @param state2
     * @param b
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState state2, boolean b) {
        if (!level.isClientSide()) {
            FoundationStoneBlockEntity blockEntity = (FoundationStoneBlockEntity) level.getBlockEntity(pos);
            if (blockEntity != null) {
                /*
                 * NOTE the stone and border will be remove, but if the stone is caused by
                 * onDestroyedByPlayer(), then the owning Deed will still contain the
                 * pos of this block. Checks must be added to the Deed to ensure that the old
                 * stone exists before attempting to remove it. This is because the Deed
                 * adds a new stone and then attempts to remove the old stone. But if the
                 * stones are in the same location, weird things happen.
                 */
                blockEntity.removeParcelBorder();
            }
        }
        super.onRemove(state, level, pos, state2, b);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockState = this.defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
        return blockState;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }
}
