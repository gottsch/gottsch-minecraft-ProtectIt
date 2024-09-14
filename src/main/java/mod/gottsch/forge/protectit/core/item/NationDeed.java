package mod.gottsch.forge.protectit.core.item;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.parcel.NationParcel;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.PersonalParcel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 27, 2024
 *
 */
public class NationDeed extends Deed {
    public static final String NATION_ID = "nation_id";

    public NationDeed(Properties properties) {
        super(properties);
    }

    @Override
    public Parcel createParcel() {
        return new NationParcel();
    }

    @Override
    public Parcel createParcel(ItemStack deedStack, ICoords coords, Player player) {
        NationParcel parcel = (NationParcel)super.createParcel(deedStack, coords, player);

        CompoundTag tag = deedStack.getOrCreateTag();

        // add nation id
        if (tag.contains(NATION_ID)) {
            parcel.setNationId(tag.getUUID(NATION_ID));
        }

        // override coords
        parcel.setCoords(parcel.getCoords().withY(0));

        return parcel;
    }

    @Deprecated
    public boolean validateParcel(FoundationStoneBlockEntity blockEntity, Parcel parcel) {

        if (parcel.getId().equals(blockEntity.getParcelId())
            && parcel.getDeedId().equals(blockEntity.getDeedId())
//            && parcel.getOwnerId().equals(blockEntity.getOwnerId())
        ) {
            // TODO check if owner has a value, if so, check against blockEntity
                return true;
        }
        return false;
    }

    @Deprecated
    public boolean validateParcel(Parcel parcel, Parcel parcel2) {

        if (parcel2.getId().equals(parcel.getId())
                && parcel2.getDeedId().equals(parcel.getDeedId())
//            && parcel.getOwnerId().equals(blockEntity.getOwnerId())
        ) {
            // TODO check if owner has a value, if so, check against blockEntity
            return true;
        }
        return false;
    }

//    @Override
//    public Box getSize(CompoundTag tag) {
//        Box size = Box.EMPTY;
//        if (tag.contains(SIZE)) {
//            CompoundTag sizeTag = tag.getCompound(SIZE);
//            size = Box.load(sizeTag);
//
//            // create new Box with max y values
//            ICoords minCoords = size.getMinCoords().withY(Math.min(-64, ))
//        } else {
//            size = DEFAULT_SIZE;
//        }
//        return size;
//    }

    // TODO any unique data for appendHoverText

}
