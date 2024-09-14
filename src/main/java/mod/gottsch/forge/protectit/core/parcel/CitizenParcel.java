/*
 * This file is part of  Protect It.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
 *
 * All rights reserved.
 *
 * Protect It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Protect It is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Protect It.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.protectit.core.parcel;

import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.item.CitizenDeed;
import mod.gottsch.forge.protectit.core.item.NationDeed;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.ObjectUtils;

import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 16, 2024
 *
 */
public class CitizenParcel extends AbstractParcel {
    // TODO resolve _ID and _KEY constants - only need 1 set.
    private static final String NATION_KEY = "nation";

    private UUID nationId;

    public CitizenParcel() {
        setType(ParcelType.CITIZEN);
    }

    public CitizenParcel(ItemStack deed) {
        CompoundTag tag = deed.getOrCreateTag();
        if (tag.contains(PARCEL_ID)) {
            setId(tag.getUUID(PARCEL_ID));
        }
        if (tag.contains(DEED_ID)) {
            setDeedId(tag.getUUID(DEED_ID));
        }
        if (tag.contains(OWNER_ID)) {
            setOwnerId(tag.getUUID(OWNER_ID));
        }
        if (tag.contains(NationParcel.NATION_ID)) {
            setNationId(tag.getUUID(NationParcel.NATION_ID));
        }
    }

    @Override
    public int getBufferSize() {
        return Config.GENERAL.parcelBufferRadius.get();
    }

    @Override
    public boolean hasAccess(UUID entityId, ItemStack stack) {
        if (hasAccess(entityId)) return true;

        if (stack.getItem() instanceof CitizenDeed) {
            CitizenParcel parcel = new CitizenParcel(stack);
            return parcel.getId().equals(getId()) &&
                    parcel.getDeedId().equals(getDeedId());
        }
        return false;
    }

    @Override
    public boolean validateData(Parcel parcel) {

        // if parcel is a nation and the nation ids are the same
        if ( ((parcel.getType() == ParcelType.NATION) && getNationId().equals(((NationParcel)parcel).getNationId()))
                // OR the parcels share the same id and deed id, and possibly the same owner id
                || ((parcel.getId() == null || parcel.getId().equals(getId()))
                && parcel.getDeedId().equals(getDeedId())
            && (parcel.getOwnerId() == null || parcel.getOwnerId().equals(getOwnerId())))

        ) {
            return true;
        }
        return false;
    }

    @Override
    public boolean validateData(FoundationStoneBlockEntity blockEntity) {
        if ((blockEntity.getParcelId() == null ||getId().equals(blockEntity.getParcelId()))
                && getDeedId().equals(blockEntity.getDeedId())
                && getNationId().equals(blockEntity.getNationId())
        ) {
            return blockEntity.getOwnerId() == null || blockEntity.getOwnerId().equals(getOwnerId());
        }
        return false;
    }

    @Override
    public void populateBlockEntity(FoundationStoneBlockEntity entity) {
        super.populateBlockEntity(entity);
        entity.setNationId(getNationId());
        entity.setParcelType(ParcelType.CITIZEN.name());
    }

    @Override
    public void save(CompoundTag tag) {
        super.save(tag);
        tag.putString(TYPE, getType().name());
        if (ObjectUtils.isNotEmpty(getNationId())) {
            tag.putUUID(NATION_KEY, getDeedId());
        }
    }

    @Override
    public Parcel load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NATION_KEY)) {
            setNationId(tag.getUUID(NATION_KEY));
        }
        return this;
    }

    public UUID getNationId() {
        return nationId;
    }

    public void setNationId(UUID nationId) {
        this.nationId = nationId;
    }
}
