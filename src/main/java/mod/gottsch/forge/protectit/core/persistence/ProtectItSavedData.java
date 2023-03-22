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

import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.registry.TransactionRegistry;
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
public class ProtectItSavedData extends SavedData {

	private static final String PROTECT_IT = ProtectIt.MODID;
	private static final String PROTECTION_REGISTRY = "protectionRegistry";
	private static final String PVP_REGISTRY = "pvpRegistry";
	private static final String TRANSACTION_REGISTRY = "transactionRegistry";
	
	/**
	 * 
	 * @return
	 */
	public static ProtectItSavedData create() {
		return new ProtectItSavedData();
	}

	public static ProtectItSavedData load(CompoundTag tag) {
		ProtectIt.LOGGER.debug("world data loading...");
		if (tag.contains(PROTECTION_REGISTRY)) {
			ProtectionRegistries.property().load(tag.getCompound(PROTECTION_REGISTRY));
		}
		if (tag.contains(PVP_REGISTRY)) {
			ProtectionRegistries.pvp().load(tag.getCompound(PVP_REGISTRY));
		}
		if (tag.contains(TRANSACTION_REGISTRY)) {
			TransactionRegistry.load(tag.getCompound(TRANSACTION_REGISTRY));
		}
		return create();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ProtectIt.LOGGER.debug("world data saving...");
		tag.put(PROTECTION_REGISTRY, ProtectionRegistries.property().save(new CompoundTag()));
		tag.put(PVP_REGISTRY, ProtectionRegistries.pvp().save(new CompoundTag()));
		tag.put(TRANSACTION_REGISTRY, TransactionRegistry.save(new CompoundTag()));
		return tag;
	}
	
	/**
	 * @param world
	 * @return
	 */
	public static ProtectItSavedData get(Level world) {
		DimensionDataStorage storage = ((ServerLevel)world).getDataStorage();
		ProtectItSavedData data = (ProtectItSavedData) storage.computeIfAbsent(
				ProtectItSavedData::load, ProtectItSavedData::create, PROTECT_IT);
		return data;
	}
}
