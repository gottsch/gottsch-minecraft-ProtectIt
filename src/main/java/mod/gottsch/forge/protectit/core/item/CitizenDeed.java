package mod.gottsch.forge.protectit.core.item;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.parcel.CitizenParcel;
import mod.gottsch.forge.protectit.core.parcel.NationParcel;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.PersonalParcel;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Apr 3, 2024
 *
 */
public class CitizenDeed extends Deed {

    public CitizenDeed(Properties properties) {
        super(properties);
    }

    @Override
    public Parcel createParcel() {
        return new CitizenParcel();
    }

    @Override
    public Parcel createParcel(ItemStack deedStack, ICoords coords, Player player) {
        CitizenParcel parcel = (CitizenParcel) super.createParcel(deedStack, coords, player);

        CompoundTag tag = deedStack.getOrCreateTag();

        // add nation id
        if (tag.contains(NationDeed.NATION_ID)) {
            parcel.setNationId(tag.getUUID(NationDeed.NATION_ID));
        }

        return parcel;
    }

    @Override
    protected boolean canPlaceBlock(Level level, ICoords coords, Parcel parcel) {
        // test if a parcel already exists for the deed id
        boolean canPlace = false;
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);

        /*
         * inside a parcel.
         */
        if (registryParcel.isPresent()) {
            if (parcel.validateData(registryParcel.get())) {
                canPlace = true;
            }
        }
        return canPlace;
    }

    @Override
    protected boolean validateWorldPlacement(Level level, BlockPos pos, Box size, Player player) {
        boolean result = super.validateWorldPlacement(level, pos, size, player);
        if (!result) {
            return false;
        }

        // TODO ensure the citizen parcel is wholly within a nation parcel

        return true;
    }

    // TODO any unique data for appendHoverText

}
