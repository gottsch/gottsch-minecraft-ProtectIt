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
package com.someguyssoftware.protectit.persistence;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.registry.ProtectionRegistry;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;

/**
 * 
 * @author Mark Gottschling on Sep 15, 2021
 *
 */
public class ProtectItSavedData extends WorldSavedData {

	public static final String GEN_DATA_KEY = ProtectIt.MODID + ":generationData";
	private static final String PROTECT_IT = ProtectIt.MODID;
	private static final String PROTECTION_REGISTRY = "protectionRegistry";
	
	public ProtectItSavedData() {
		super(GEN_DATA_KEY);
	}
	
	public ProtectItSavedData(String key) {
		super(key);
	}

	@Override
	public void load(CompoundNBT nbt) {
		ProtectIt.LOGGER.info("world data loading...");
		CompoundNBT protectIt = nbt.getCompound(PROTECT_IT);
		if (protectIt.contains(PROTECTION_REGISTRY)) {
//			ProtectionRegistry.load(protectIt.getCompound(PROTECTION_REGISTRY));
			ProtectionRegistries.block().load(protectIt.getCompound(PROTECTION_REGISTRY));
		}
	}

	@Override
	public CompoundNBT save(CompoundNBT nbt) {
		ProtectIt.LOGGER.info("world data saving...");
		// create a treasure compound			
		CompoundNBT protectIt = new CompoundNBT();
		nbt.put(PROTECT_IT, protectIt);
//		protectIt.put(PROTECTION_REGISTRY, ProtectionRegistry.save(new CompoundNBT()));
		protectIt.put(PROTECTION_REGISTRY, ProtectionRegistries.block().save(new CompoundNBT()));
		// TODO save pvp registry
		return nbt;
	}
	
	/**
	 * @param world
	 * @return
	 */
	public static ProtectItSavedData get(IWorld world) {
		DimensionSavedDataManager storage = ((ServerWorld)world).getDataStorage();
		ProtectItSavedData data = (ProtectItSavedData) storage.computeIfAbsent(ProtectItSavedData::new, GEN_DATA_KEY);
		
		if (data == null) {
			data = new ProtectItSavedData();
			storage.set(data);
		}
		return data;
	}
}
