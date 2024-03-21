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

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 16, 2024
 *
 */
public interface Parcel {

    default String randomName() {
        return StringUtils.capitalize(RandomStringUtils.random(8, true, false));
    }

    void save(CompoundTag parcelTag);
    Parcel load(CompoundTag tag);

    ICoords getMinCoords();
    ICoords getMaxCoords();

    UUID getId();

    void setId(UUID id);

    UUID getOwnerId();

    void setOwnerId(UUID ownerId);

    UUID getDeedId();

    void setDeedId(UUID deedId);

    String getName();

    void setName(String name);

    ICoords getCoords();

    void setCoords(ICoords coords);

    Box getSize();

    void setSize(Box size);

    int getArea();
}
