package mod.gottsch.forge.protectit.core.item;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 *
 * @author Mark Gottschling on Mar 18, 2024
 *
 */
public class FoundationStoneItem extends BlockItem {

    public FoundationStoneItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        // if not in a parcel then don't place
        if (!context.getLevel().isClientSide()) {
                // get the parcel for this position
                List<Box> list = ParcelRegistry.findBoxes(new Coords(context.getClickedPos()), new Coords(context.getClickedPos()), false, true);
                ProtectIt.LOGGER.debug("found parcels -> {}", list);
                if (list.isEmpty()) {
                    return false;
                }
                // TODO theoretically could check if player has access to parcel here as well
                // thus saving an additional lookup during placeBlockEvent and the unnecessary
                // screen update during client-server mismatch.
        } else {
            return false;
        }

        // if parcel is not owned
        return super.placeBlock(context, state);
    }
}
