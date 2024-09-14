package mod.gottsch.forge.protectit.core.item;

import mod.gottsch.forge.gottschcore.block.BlockContext;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.block.ProtectItBlocks;
import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.persistence.PersistedData;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import mod.gottsch.forge.protectit.core.util.ModUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Mar 18, 2024
 *
 */
public abstract class Deed extends Item {
    // TODO these really belong in parcel (or duplicate them)
    public static final String PARCEL_ID = "parcel_id";
    public static final String DEED_ID = "deed_id";
    public static final String OWNER_ID = "owner_id";
    public static final String PARCEL_TYPE = "parcel_type";
    public static final String SIZE = "size";

    public static final Box DEFAULT_SIZE = new Box(new Coords(0, -15, 0), new Coords(16, 16, 16));

    public Deed(Item.@NotNull Properties properties) {

        super(properties.stacksTo(1));
    }

    // TODO should just use a parcel constructor(deed) instead of this
    public abstract Parcel createParcel();

    /**
     *
     * @param deedStack
     * @param coords
     * @param player
     * @return
     */
    // TODO update to take ICoords instead of BlockPos
    public Parcel createParcel(ItemStack deedStack, ICoords coords, Player player) {
        // TODO could've determined parcel type by tag variable DEED_TYPE
        Parcel parcel = createParcel();

        CompoundTag tag = deedStack.getOrCreateTag();
        if (tag.contains(PARCEL_ID)) {
            parcel.setId(tag.getUUID(PARCEL_ID));
        }
        if (tag.contains(DEED_ID)) {
            parcel.setDeedId(tag.getUUID(DEED_ID));
        }
        if (tag.contains(OWNER_ID)) {
            parcel.setOwnerId(tag.getUUID(OWNER_ID));
        } else {
            parcel.setOwnerId(player.getUUID());
        }

        parcel.setSize(getSize(tag));
        parcel.setCoords(coords);
        parcel.setName(parcel.randomName());

        return parcel;
    }

    protected boolean validateWorldPlacement(Level level, BlockPos pos, Box size, Player player) {
        if (level.isOutsideBuildHeight(pos.offset(size.getMaxCoords().toPos()))) {
            player.sendSystemMessage((Component.translatable(LangUtil.chat("parcel.outside_world_boundaries"))
                    .withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.ITALIC})));
            return false;
        }
        return true;
    }

    protected boolean validateParcelThreshold(Player player) {
        // gather the number of parcels the player has
        List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
        if (parcels.size() >= Config.GENERAL.parcelsPerPlayer.get() && !player.hasPermissions(Config.GENERAL.opsPermissionLevel.get())) {
            // TODO colorize
            // TODO create a class ChatHelper that has premade color formatters
            player.sendSystemMessage(Component.translatable(LangUtil.chat("parcel.max_reached")));
            return false;
        }
        return true;
    }

    /**
     *
     * @param context
     * @return
     */
    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {

        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        /*
         * check if the player has reached there max parcels already
         */
        boolean isParcelThresholdValid = validateParcelThreshold(context.getPlayer());
        if (!isParcelThresholdValid) {
            return InteractionResult.FAIL;
        }

        // wrapped BlockPos
        ICoords targetCoords = new Coords(context.getClickedPos());

        // create a parcel object from the deed itemStack and context info
        Parcel parcel = createParcel(context.getItemInHand(), targetCoords, context.getPlayer());

        // validate that the parcel's owner id == player id
        if (!parcel.isOwner(context.getPlayer().getUUID())) {
            return InteractionResult.FAIL;
        }

        // if using the deed on a foundation stone
        if (context.getLevel().getBlockState(context.getClickedPos()).is(ProtectItBlocks.FOUNDATION_STONE.get())) {
            /*
             * accept location as parcel
             */

            // validate
            BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
            if (blockEntity instanceof FoundationStoneBlockEntity foundationStoneBlockEntity) {

//            Optional<Parcel> parcelOptional = ParcelUtil.findLeastSignificant(ParcelRegistry.find(new Coords(context.getClickedPos())));
                Box parcelBox = foundationStoneBlockEntity.getBox(targetCoords);
                Box inflatedBox = ModUtil.inflate(parcelBox, parcel.getBufferSize());

                // find overlaps of parcel with buffered registry parcels
                List<Parcel> overlaps = ParcelRegistry.findBuffer(parcelBox);
                if (!overlaps.isEmpty()) {
                    for (Parcel overlapParcel : overlaps) {
                        // if parcel in hand equals parcel in world then fail
                        // NOTE this should be moot as the deed shouldn't exist at this point anymore (survival)
                        if (parcel.getId().equals(overlapParcel.getId())) {
                            return InteractionResult.FAIL;
                        }

                        // if parcel in hand has same owner as parcel in world, ignore buffers, but check border overlaps
                        if (parcel.getOwnerId().equals(overlapParcel.getOwnerId())) {
                            List<Parcel> ownedOverlaps = ParcelRegistry.find(parcelBox);
                            if (!ownedOverlaps.isEmpty()) {
                                return InteractionResult.FAIL;
                            }
                        }
                    }
                }


                // TEMP for now use findBoxes() - may upgrade to find()
                // TODO this will definitely need to be changed with Nation and Citizen parcels- should be moved into validateParcel
//                if (!ParcelRegistry.findBoxes(inflatedBox).isEmpty()) {
//                    return InteractionResult.FAIL;
//                }

                // TODO need a method that checks if placement is valid
                // ie Parcel.isPlacementValid(parcel) which checks all overlaps if any are person or citizen, or if nation, then check
                // 1) does this parcel belong to the nation
                // 2) is totally within bounds of nation
                // validate the parcel data itself
                boolean isValid = parcel.validateData(foundationStoneBlockEntity);

                if (isValid) {
                    // check if there is an existing parcel and update it else add it
                    Optional<Parcel> registryParcel = ParcelRegistry.findByParcelId(parcel.getId());
                    if (registryParcel.isPresent()) {
                        registryParcel.get().setOwnerId(parcel.getOwnerId());
                    } else {
                        // add to the registry
                        ParcelRegistry.add(parcel);
                    }

                    PersistedData savedData = PersistedData.get(context.getLevel());
                    // mark data as dirty
                    if (savedData != null) {
                        savedData.setDirty();
                    }

                    // if creative force removal of item
                    if (context.getPlayer().isCreative()) {
                        context.getPlayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    } else {
                        // consume item
                        context.getItemInHand().shrink(1);
                    }

                    // remove the border
                    ((FoundationStoneBlockEntity) blockEntity).removeParcelBorder();
                    // remove the foundation stone
                    blockEntity.getLevel().setBlock(context.getClickedPos(), Blocks.AIR.defaultBlockState(), 3);

                    return InteractionResult.CONSUME;
                }
            }

        } else {
            /*
             * place foundation stone
             */
            boolean canPlace = canPlaceBlock(context.getLevel(), targetCoords, parcel);

            boolean result = canPlace && this.placeBlock(new BlockPlaceContext(context), ProtectItBlocks.FOUNDATION_STONE.get().defaultBlockState());
            return result ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        return super.useOn(context);
    }

    protected boolean canPlaceBlock(Level level, ICoords coords, Parcel parcel) {
        boolean canPlace = false;

        /*
         * check if parcel is within another existing parcel
         */
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);

        /*
         * not inside another parcel.
         */
        if (registryParcel.isEmpty()) {
            /*
             * check if a parcel exists.
             * if a parcel does not exist then the deed is used to place a foundation stone.
             * NOTE parcels can exist for non-consumed deeds if it is a transfer/sale.
             */
            registryParcel = ParcelRegistry.findByParcelId(parcel.getId());
            if (registryParcel.isEmpty()) {
                canPlace = true;
            }
        } else {
            /*
             * inside a parcel - is it the parcel associated with this deed ?
             * if a parcel does exist, then this deed may be associated with it. ie transfer/sale,
             * and therefor can only be placed within the same parcel it is associated with.
             */
            if (parcel.validateData(registryParcel.get())) {
                canPlace = true;
            }
        }
        return canPlace;
    }

    /**
     *
     * @param context
     * @param state
     * @return
     */
    protected boolean placeBlock(@NotNull BlockPlaceContext context, BlockState state) {
        if (context.getLevel().isClientSide()) {
            return true;
        }

        // get the target position
        BlockPos targetPos = context.getClickedPos();
        BlockContext blockContext = new BlockContext(context.getLevel(), targetPos);
        if (blockContext.isAir() || blockContext.isReplaceable()) {
            CompoundTag tag = context.getItemInHand().getOrCreateTag();

            // get the size
            Box size = getSize(tag);

            if (!validateWorldPlacement(context.getLevel(), targetPos, size, context.getPlayer())) {
                return false;
            }

            // get the old stone coords if any
            ICoords oldFoundationStoneCoords = getPreviousCoords(context.getLevel(), tag);

            /*
             * add the foundation stone to the world
             */
            boolean result = context.getLevel().setBlock(targetPos, state, 26);
            if (result) {
                // if successful handle post placement
                handleBlockPlaced(context.getLevel(), targetPos, context.getPlayer(), context.getItemInHand(), oldFoundationStoneCoords);
            }
            return true;
        }
        return false;
    }

    /**
     *
     * @param level
     * @param pos
     * @param player
     * @param deed
     * @param previousCoords
     */
    protected void handleBlockPlaced(Level level, BlockPos pos, Player player, ItemStack deed, ICoords previousCoords) {
        // get the block entity
        FoundationStoneBlockEntity blockEntity = (FoundationStoneBlockEntity) level.getBlockEntity(pos);
        if (blockEntity != null) {

            // update data from deed.
            populateFoundationStone(blockEntity, deed, pos, player);

            //check if there is a stored position of foundation stone.
            if (previousCoords != Coords.EMPTY) {
                removePreviousLocation(level, deed, previousCoords);
            }

            // store position of new foundation stone
            storeCurrentLocation(level, deed, new Coords(pos));

            /*
             * NOTE foundation stone is non-craftable nor in the crafting tab
             * so need to initiate the borders manually.
             */
            // place border blocks
            blockEntity.placeParcelBorder();
        }
    }

    protected ICoords getPreviousCoords(Level level, CompoundTag tag) {
        // get the previous coords from tag if they exist
        ICoords coords = Coords.EMPTY;
        if (tag.contains("pos")) {
            CompoundTag posTag = tag.getCompound("pos");
            coords = Coords.EMPTY.load(posTag);
        }

        /*
         * check if deed has old info. ie foundation stone was destroyed by player
         * instead of destroy by using the deed somewhere else.
         */
        // get the old block entity if exists
        BlockEntity oldBlockEntity = level.getBlockEntity(coords.toPos());
        if (!(oldBlockEntity instanceof FoundationStoneBlockEntity)){
            // clean deed as a stone doesn't exist at pos
            tag.remove("pos");
            // reset pos to empty ie there isn't an old foundation stone position.
            coords = Coords.EMPTY;
        }

        return coords;
    }

    protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
        CompoundTag tag = deed.getOrCreateTag();
        Box size = getSize(tag);

        blockEntity.setParcelId(tag.contains(PARCEL_ID) ? tag.getUUID(PARCEL_ID) : null);
        blockEntity.setDeedId(tag.contains(DEED_ID) ? tag.getUUID(DEED_ID) : null);
        blockEntity.setOwnerId(tag.contains(OWNER_ID) ? tag.getUUID(OWNER_ID) : player.getUUID());
        blockEntity.setParcelType(tag.contains(PARCEL_TYPE) ? tag.getString(PARCEL_TYPE) : null);
        blockEntity.setCoords(new Coords(pos));
        blockEntity.setSize(size);
    }

    protected void removePreviousLocation(Level level, ItemStack deed, ICoords previousCoords) {
        if (level.getBlockState(previousCoords.toPos()).is(ProtectItBlocks.FOUNDATION_STONE.get())) {
            /*
             * destroy old foundationStone
             */
            level.destroyBlock(previousCoords.toPos(), false);
            // remove old pos
            deed.getOrCreateTag().remove("pos");
        }
    }

    protected void storeCurrentLocation(Level level, ItemStack deed, ICoords coords) {
        CompoundTag posTag = new CompoundTag();
        deed.getOrCreateTag().put("pos", coords.save(posTag));
    }

    public Box getSize(CompoundTag tag) {
        Box size = Box.EMPTY;
        if (tag.contains(SIZE)) {
            CompoundTag sizeTag = tag.getCompound(SIZE);
            size = Box.load(sizeTag);
        } else {
            size = DEFAULT_SIZE;
        }
        return size;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(Component.translatable(LangUtil.tooltip("parcel.howto")).withStyle(ChatFormatting.GREEN));
    }
}
