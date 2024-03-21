package mod.gottsch.forge.protectit.core.item;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.parcel.ParcelFactory;
import mod.gottsch.forge.protectit.core.parcel.ParcelType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.ObjectUtils;

import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 19, 2024
 *
 */
public class DeedFactory {

    private DeedFactory() {}

    public static ItemStack createDeed(Class clazz, Box size) {
        if (ProtectItItems.PERSONAL_DEED.get().getClass().equals(clazz)) {
            return createPersonalDeed(size);
        }
        // TODO add other types
        else {
            return createPersonalDeed(size);
        }
    }

    public static ItemStack createPersonalDeed(Box size) {
        ItemStack deed = createItemStack(ParcelType.PERSONAL);
        CompoundTag tag = deed.getOrCreateTag();
        // add the ids
        tag.putUUID(Deed.PARCEL_ID, UUID.randomUUID());
        tag.putUUID(Deed.DEED_ID, UUID.randomUUID());
        // add the type
        tag.putString(Deed.DEED_TYPE, ParcelType.PERSONAL.name());
        // add the size
        CompoundTag sizeTag = new CompoundTag();
        size.save(sizeTag);
        tag.put(Deed.SIZE, sizeTag);

        return deed;
    }

    public static ItemStack createPersonalDeed(UUID ownerId, Box size) {
        ItemStack deed = createPersonalDeed(size);
        CompoundTag tag = deed.getTag();
        if (ObjectUtils.isNotEmpty(ownerId) && tag != null) {
            tag.putUUID(Deed.OWNER_ID, ownerId);
        }
        return deed;
    }

    private static ItemStack createItemStack(ParcelType type) {
        return switch(type) {
            case PERSONAL -> new ItemStack(ProtectItItems.PERSONAL_DEED.get());
            case NATION -> null;
            case CITIZEN -> null;
        };
    }
}
