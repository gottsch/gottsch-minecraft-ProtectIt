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

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Mar 18, 2024
 *
 */
public class ParcelFactory {

    public static Optional<Parcel> create(CompoundTag tag) {
        if (tag.contains("type")) {
            String type = tag.getString("type");
            if (type.equalsIgnoreCase("personal")) {
                return Optional.of(new PersonalParcel());
            }
            else if (type.equalsIgnoreCase("citizen")) {
                return Optional.of(new CitizenParcel());
            }
            else if (type.equalsIgnoreCase("nation")) {
                return Optional.of(new NationParcel());
            }
        }
        return Optional.empty();
    }
}
