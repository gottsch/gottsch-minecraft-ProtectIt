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
package com.someguyssoftware.protectit.registry.bst;

import java.util.ArrayList;
import java.util.List;

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * 
 * @author Mark Gottschling on Nov 4, 2021
 *
 */
public class OwnershipData {
	private IdentifierData owner;
	@Deprecated
	private List<IdentifierData> whitelist;
		
	public OwnershipData() {
		setOwner(new IdentifierData(""));
	}
	
	public OwnershipData(String uuid, String name) {
		setOwner(new IdentifierData(uuid, name));
	}
	
	public void save(CompoundTag nbt) {
		CompoundTag ownerNbt = new CompoundTag();
		ProtectIt.LOGGER.info("saving owner -> {}, {}", getOwner().getName(), getOwner().getUuid());
		getOwner().save(ownerNbt);
		nbt.put("owner", ownerNbt);
		
		ListTag list = new ListTag();
		getWhitelist().forEach(data -> {
			CompoundTag player = new CompoundTag();
			data.save(player);
			list.add(player);
		});
		nbt.put("whitelist", list);
	}
	
	public OwnershipData load(CompoundTag nbt) {		
		if (nbt.contains("owner")) {
			ProtectIt.LOGGER.info("loading owner");
			getOwner().load(nbt.getCompound("owner"));
			ProtectIt.LOGGER.info("loaded owner -> {}, {}", getOwner().getName(), getOwner().getUuid());
		}
		if (nbt.contains("whitelist")) {
			ListTag list = nbt.getList("whitelist", 10);
			list.forEach(compound -> {
				IdentifierData player = new IdentifierData("");
				player.load((CompoundTag)compound);
				getWhitelist().add(player);
			});
		}
		return this;
	}
	
	public IdentifierData getOwner() {
		return owner;
	}
	public void setOwner(IdentifierData owner) {
		this.owner = owner;
	}
	public List<IdentifierData> getWhitelist() {
		if (whitelist == null) {
			this.whitelist = new ArrayList<>();
		}
		return whitelist;
	}
	public void setWhitelist(List<IdentifierData> whitelist) {
		this.whitelist = whitelist;
	}

	@Override
	public String toString() {
		return "OwnershipData [owner=" + owner + ", whitelist=" + whitelist + "]";
	}
}
