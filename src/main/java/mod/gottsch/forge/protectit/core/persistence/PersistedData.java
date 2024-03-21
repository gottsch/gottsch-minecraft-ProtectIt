/*
 * This file is part of  Protect It.
 * Copyright (c) 2021, Mark Gottschling (gottsch)
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
package mod.gottsch.forge.protectit.core.persistence;

import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * 
 * @author Mark Gottschling on Sep 15, 2021
 *
 */
public class PersistedData extends SavedData {

	private static final String PROTECT_IT = ProtectIt.MODID;
	private static final String PARCEL_REGISTRY = "parcel_registry";
	
	/**
	 * 
	 * @return
	 */
	public static PersistedData create() {
		return new PersistedData();
	}

	public static PersistedData load(CompoundTag tag) {
		ProtectIt.LOGGER.debug("world data loading...");
		if (tag.contains(PARCEL_REGISTRY)) {
//			ProtectionRegistries.block().load(tag.getCompound(REGISTRY));
			ParcelRegistry.load(tag.getCompound(PARCEL_REGISTRY));
		}
		return create();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ProtectIt.LOGGER.debug("world data saving...");
//		tag.put(PARCEL_REGISTRY, ProtectionRegistries.block().save(new CompoundTag()));
		tag.put(PARCEL_REGISTRY, ParcelRegistry.save(new CompoundTag()));
		return tag;
	}
	
	/**
	 * @param world
	 * @return
	 */
	public static PersistedData get(Level world) {
		DimensionDataStorage storage = ((ServerLevel)world).getDataStorage();
		PersistedData data = (PersistedData) storage.computeIfAbsent(
				PersistedData::load, PersistedData::create, PROTECT_IT);
		return data;
	}
}
