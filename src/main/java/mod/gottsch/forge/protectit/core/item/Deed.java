package mod.gottsch.forge.protectit.core.item;

import mod.gottsch.forge.gottschcore.block.BlockContext;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.block.ProtectItBlocks;
import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.ParcelFactory;
import mod.gottsch.forge.protectit.core.parcel.ParcelUtil;
import mod.gottsch.forge.protectit.core.parcel.PersonalParcel;
import mod.gottsch.forge.protectit.core.persistence.PersistedData;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 18, 2024
 *
 */
public abstract class Deed extends Item {
    public static final String PARCEL_ID = "parcel_id";
    public static final String DEED_ID = "deed_id";
    public static final String OWNER_ID = "owner_id";
    public static final String DEED_TYPE = "deed_type";
    public static final String SIZE = "size";

    public static final Box DEFAULT_SIZE = new Box(new Coords(0, -15, 0), new Coords(16, 16, 16));

    public Deed(Item.@NotNull Properties properties) {

        super(properties.stacksTo(1));
    }

    protected boolean placeBlock(@NotNull BlockPlaceContext context, BlockState state) {
        if (context.getLevel().isClientSide()) {
            return true;
        }

        // get the target position
        BlockPos targetPos = context.getClickedPos();
        BlockContext blockContext = new BlockContext(context.getLevel(), targetPos);
        if (blockContext.isAir() || blockContext.isReplaceable()) {
            // TODO make method getSize(ItemStack)
            CompoundTag tag = context.getItemInHand().getOrCreateTag();
            // get the size
            Box size = Box.EMPTY;
            if (tag.contains(SIZE)) {
                CompoundTag sizeTag = tag.getCompound(SIZE);
                size = Box.load(sizeTag);
            } else {
                size = DEFAULT_SIZE;
            }

            // check if pos + size is within world boundaries
            if (context.getLevel().isOutsideBuildHeight(targetPos.offset(size.getMaxCoords().toPos()))) {
                context.getPlayer().sendSystemMessage((Component.translatable(LangUtil.chat("parcel.outside_world_boundaries"))
                        .withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.ITALIC})));
                return false;
            }

            ICoords oldFouncationStonePos = Coords.EMPTY;
            if (tag.contains("pos")) {
                CompoundTag posTag = tag.getCompound("pos");
                oldFouncationStonePos = Coords.EMPTY.load(posTag);
            }

            /*
             * check if deed has old info. ie foundation stone was destroyed by player
             * instead of destroy by using the deed somewhere else.
             */
            // get the old block entity if exists
            BlockEntity oldBlockEntity = context.getLevel().getBlockEntity(oldFouncationStonePos.toPos());
            if (oldBlockEntity == null || !(oldBlockEntity instanceof FoundationStoneBlockEntity)){
                // clean deed
                tag.remove("pos");
                // reset pos to empty ie there isn't an old foundation stone position.
                oldFouncationStonePos = Coords.EMPTY;
            }

            /*
             * add the foundation stone to the world
             */
            boolean result = context.getLevel().setBlock(targetPos, state, 26);
            if (result) {
                // get the block entity
                FoundationStoneBlockEntity blockEntity = (FoundationStoneBlockEntity) context.getLevel().getBlockEntity(targetPos);
                if (blockEntity != null) {

                    // update data from deed.
                    blockEntity.setParcelId(tag.contains(PARCEL_ID) ? tag.getUUID(PARCEL_ID) : null);
                    blockEntity.setDeedId(tag.contains(DEED_ID) ? tag.getUUID(DEED_ID) : null);
                    blockEntity.setOwnerId(tag.contains(OWNER_ID) ? tag.getUUID(OWNER_ID) : null);
                    blockEntity.setSize(size);

                    //check if there is a stored position of foundation stone.
                    if (oldFouncationStonePos != Coords.EMPTY) {
                        if (context.getLevel().getBlockState(oldFouncationStonePos.toPos()).is(ProtectItBlocks.FOUNDATION_STONE.get())) {
                            /*
                             * destroy old foundationStone
                             */
                            context.getLevel().destroyBlock(oldFouncationStonePos.toPos(), false);
                            // remove old pos
                            tag.remove("pos");
                        }
                    }

                    // store position of new foundation stone
                    ICoords foundationStoneCoords = new Coords(targetPos);
                    CompoundTag posTag = new CompoundTag();
                    tag.put("pos", foundationStoneCoords.save(posTag));

                    /*
                     * NOTE foundation stone is non-craftable nor in the crafting tab
                     * so need to initiate the borders manually.
                     */
                    // place border blocks
                    blockEntity.placeParcelBorder();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {

        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        /*
         * check if the player has reached there max parcels already
         */
        // gather the number of parcels the player has
        List<Parcel> parcels = ParcelRegistry.findByOwner(context.getPlayer().getUUID());

        if (parcels.size() >= Config.GENERAL.propertiesPerPlayer.get()) {
            // TODO colorize
            // TODO create a class ChatHelper that has premade color formatters
            context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("parcel.max_reached")));
            return InteractionResult.FAIL;
        }

        // wrapped BlockPos
        ICoords clickedCoords = new Coords(context.getClickedPos());

        // create a parcel object from the deed itemstack and context info
        Parcel parcel = createParcel(context.getItemInHand(), context.getClickedPos(), context.getPlayer());

        // check if parcel has a owner id and if its the player - if not, exit as they can't use it
        if (parcel.getOwnerId() != null && !parcel.getOwnerId().equals(context.getPlayer().getUUID())) {
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
                Box parcelBox = foundationStoneBlockEntity.getBox(clickedCoords);
                // TEMP for now use findBoxes() - may upgrade to find()
                if (!ParcelRegistry.findBoxes(parcelBox).isEmpty()) {
                    return InteractionResult.FAIL;
                }

                // validate the parcel data itself
                boolean isValid = validateParcel(foundationStoneBlockEntity, parcel);

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

                    // consume item
                    context.getItemInHand().shrink(1);

                    // remove the foundation stone
                    blockEntity.getLevel().setBlock(context.getClickedPos(), Blocks.AIR.defaultBlockState(), 3);

                    return InteractionResult.CONSUME;
                }
            }

        } else {
            /*
             * place foundation stone
             */

            // test if a parcel already exists for the parcel id
            boolean canPlace = false;
            Optional<Parcel> registryParcel = ParcelUtil.findLeastSignificant(ParcelRegistry.find(clickedCoords));
            /*
             * not inside any parcel.
             */
            if (registryParcel.isEmpty()) {
                /*
                 * check if a parcel exists.
                 * if a parcel does not exist then the deed is used to place a foundation stone
                 * in this position for the first time.
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
                // TODO change this to checkAccess()
                if (validateParcel(registryParcel.get(), parcel)) {
                    canPlace = true;
                }
            }

            boolean result = canPlace && this.placeBlock(new BlockPlaceContext(context), ProtectItBlocks.FOUNDATION_STONE.get().defaultBlockState());
            return result ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        return super.useOn(context);
    }

    public abstract boolean validateParcel(FoundationStoneBlockEntity blockEntity, Parcel parcel);
    public abstract boolean validateParcel(Parcel parcel, Parcel parcel2);
    public abstract Parcel createParcel(ItemStack stack, BlockPos pos, Player player);

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(Component.translatable(LangUtil.tooltip("parcel.howto")).withStyle(ChatFormatting.GREEN));
    }
}
