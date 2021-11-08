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
package com.someguyssoftware.protectit.registry;

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.nbt.CompoundNBT;

/**
 * 
 * @author Mark Gottschling on Oct 9, 2021
 *
 */
public class PlayerData {
	public static final PlayerData EMPTY = new PlayerData("", "");
	
	private String uuid;
	private String name;
	
	public PlayerData(String uuid) {
		setUuid(uuid);
		setName("");
	}
	
	public PlayerData(String uuid, String name) {
		this(uuid);
		setName(name);
	}
	
	public void save(CompoundNBT nbt) {
		nbt.putString("uuid", getUuid());
		nbt.putString("name", (getName() == null) ? "" : getName());
		ProtectIt.LOGGER.info("saved uuid -> {}", nbt.getString("uuid"));
		ProtectIt.LOGGER.info("saved name -> {}", nbt.getString("name"));
	}
	
	public PlayerData load(CompoundNBT nbt) {
		if (nbt.contains("uuid")) {
			ProtectIt.LOGGER.info("loading uuid -> {}", nbt.getString("uuid"));
			setUuid(nbt.getString("uuid"));
		}
		if (nbt.contains("name")) {
			ProtectIt.LOGGER.info("loading name -> {}", nbt.getString("name"));
			setName(nbt.getString("name"));
		}
		return this;
	}
	
	public String getUuid() {
		return uuid;
	}
	protected void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "PlayerData [uuid=" + uuid + ", name=" + name + "]";
	}
	
}
