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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 *
 * @author Mark Gottschling on Mar 16, 2024
 *
 */
public class PersonalParcel extends AbstractParcel {

    public PersonalParcel() {
        setType(ParcelType.PERSONAL);
    }


    @Override
    public boolean validateData(Parcel parcel) {
        if (parcel.getId().equals(getId())
                && parcel.getDeedId().equals(getDeedId())
//            && parcel.getOwnerId().equals(blockEntity.getOwnerId())
        ) {
            return parcel.getOwnerId() == null || parcel.getOwnerId().equals(getOwnerId());
        }
        return false;
    }

    @Override
    public boolean validateData(FoundationStoneBlockEntity blockEntity) {

        if (getId().equals(blockEntity.getParcelId())
                && getDeedId().equals(blockEntity.getDeedId())
        ) {
            return blockEntity.getOwnerId() == null || blockEntity.getOwnerId().equals(getOwnerId());
        }
        return false;
    }

    @Override
    public void save(CompoundTag tag) {
        super.save(tag);
        tag.putString(TYPE, "personal");
    }

    @Override
    public Parcel load(CompoundTag tag) {
        super.load(tag);
        return this;
    }

    @Override
    public int getBufferSize() {
        return Config.GENERAL.parcelBufferRadius.get();
    }

    @Override
    public String toString() {
        return "PersonalParcel{} " + super.toString();
    }
}
