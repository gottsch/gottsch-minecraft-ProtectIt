package mod.gottsch.forge.protectit.core.item;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.PersonalParcel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 18, 2024
 *
 */
public class PersonalDeed extends Deed {

    public PersonalDeed(Properties properties) {
        super(properties);
    }

    public Parcel createParcel(BlockPos pos, ItemStack stack) {
        Parcel parcel = new PersonalParcel();

        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(PARCEL_ID)) {
            parcel.setId(tag.getUUID(PARCEL_ID));
        }
        if (tag.contains(DEED_ID)) {
            parcel.setDeedId(tag.getUUID(DEED_ID));
        }
        if (tag.contains(OWNER_ID)) {
            parcel.setOwnerId(tag.getUUID(OWNER_ID));
        }

        Box size = Box.EMPTY;
        if (tag.contains(SIZE)) {
            CompoundTag sizeTag = tag.getCompound(SIZE);
            size = Box.load(sizeTag);
        } else {
            size = DEFAULT_SIZE;
        }
        parcel.setSize(DEFAULT_SIZE);

        parcel.setCoords(new Coords(pos));
        parcel.setName(parcel.randomName());

        return parcel;
    }

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

    // TODO any unique data for appendHoverText

    // NOTE rethink how deed id, parcels, foundation stones and issuing deeds works
    // stones are only temporary - used to claim a parcel.
}
