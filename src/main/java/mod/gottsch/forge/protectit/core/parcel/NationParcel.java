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

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.item.CitizenDeed;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.ObjectUtils;

import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 16, 2024
 *
 */
public class NationParcel extends AbstractParcel {
    // TODO resolve _ID and _KEY constants - only need 1 set.
    public static final String NATION_ID = "nation_id";
    private static final String NATION_KEY = "nation";

    private UUID nationId;
    // TODO add getter/setter etc
    private String nationName;

    public NationParcel() {
        setType(ParcelType.NATION);
    }

    @Override
    public int getBufferSize() {
        return Config.GENERAL.nationParcelBufferRadius.get();
    }

    @Override
    public boolean hasAccess(UUID entityId, ItemStack stack) {
        if (hasAccess(entityId)) return true;

        if (stack.getItem() instanceof CitizenDeed) {
            CitizenParcel parcel = new CitizenParcel(stack);
            if (parcel.getNationId().equals(getNationId())) {
                // TODO add check against nation blacklist
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean validateData(Parcel parcel) {
        if (( parcel.getId() == null || parcel.getId().equals(getId()))
                && parcel.getDeedId().equals(getDeedId())
        ) {
            return parcel.getOwnerId() == null || parcel.getOwnerId().equals(getOwnerId());
        }
        return false;
    }

    @Override
    public boolean validateData(FoundationStoneBlockEntity blockEntity) {
        if ((blockEntity.getParcelId() == null || getId().equals(blockEntity.getParcelId()))
                && getDeedId().equals(blockEntity.getDeedId())
        ) {
            return blockEntity.getOwnerId() == null || blockEntity.getOwnerId().equals(getOwnerId());
        }
        return false;

    }

    @Override
    public void populateBlockEntity(FoundationStoneBlockEntity entity) {
        super.populateBlockEntity(entity);
        entity.setNationId(getNationId());
        entity.setParcelType(ParcelType.NATION.name());
        // NOTE nope, don't need to do this - maybe
        // override coords to be at place of block entity
//        entity.setCoords(new Coords(entity.getBlockPos()));
    }

    @Override
    public void save(CompoundTag tag) {
        super.save(tag);
        tag.putString(TYPE, "nation");
        if (ObjectUtils.isNotEmpty(getNationId())) {
            tag.putUUID(NATION_KEY, getNationId());
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

    @Override
    public String toString() {
        return "NationParcel{" +
                "nationId=" + nationId +
                "} " + super.toString();
    }
}
