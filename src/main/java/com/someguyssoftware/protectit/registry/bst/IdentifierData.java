/*
 * This file is part of  Protect It.
 * Copyright (c) 2021 Mark Gottschling (gottsch)
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

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.nbt.CompoundTag;


/**
 * 
 * @author Mark Gottschling on Nov 4, 2021
 *
 */
public class IdentifierData {
	private String uuid;
	private String name;
	
	public IdentifierData(String uuid) {
		this.uuid = (uuid == null) ? "" : uuid;
	}
	
	public IdentifierData(String uuid, String name) {
		this(uuid);
		this.name = name;
	}

	public void save(CompoundTag tag) {
		tag.putString("uuid", getUuid());
		tag.putString("name", (getName() == null) ? "" : getName());
		ProtectIt.LOGGER.info("saved uuid -> {}", tag.getString("uuid"));
		ProtectIt.LOGGER.info("saved name -> {}", tag.getString("name"));
	}
	
	public IdentifierData load(CompoundTag tag) {
		if (tag.contains("uuid")) {
			ProtectIt.LOGGER.info("loading uuid -> {}", tag.getString("uuid"));
			setUuid(tag.getString("uuid"));
		}
		if (tag.contains("name")) {
			ProtectIt.LOGGER.info("loading name -> {}", tag.getString("name"));
			setName(tag.getString("name"));
		}
		return this;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
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
		return "IdentifierData [uuid=" + uuid + ", name=" + name + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdentifierData other = (IdentifierData) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
}
