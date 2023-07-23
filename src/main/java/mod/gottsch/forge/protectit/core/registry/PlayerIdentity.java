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
package mod.gottsch.forge.protectit.core.registry;

import java.util.Objects;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import mod.gottsch.forge.protectit.core.util.UuidUtil;
import net.minecraft.nbt.CompoundTag;

/**
 * 
 * @author Mark Gottschling on Oct 9, 2021
 *
 */
public class PlayerIdentity {
	private static final String UUID_KEY = "uuid";
	private static final String NAME_KEY = "name";
	
	public static final PlayerIdentity EMPTY = new PlayerIdentity(UuidUtil.EMPTY_UUID, "");
	
	private UUID uuid;
	private String name;
	
	/**
	 * 
	 */
	public PlayerIdentity() {
		this(UuidUtil.EMPTY_UUID);
	}
	
	public PlayerIdentity(UUID uuid) {
		setUuid(uuid);
		setName("");
	}
	
	public PlayerIdentity(UUID uuid, String name) {
		this(uuid);
		setName(name);
	}
	
	/**
	 * Convenience wrapper.
	 * @param profile
	 */
	public PlayerIdentity(GameProfile profile) {
		this(profile.getId(), profile.getName());
	}
	
	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		save(nbt);
		return nbt;
	}
	
	public void save(CompoundTag tag) {
		tag.putUUID(UUID_KEY, getUuid());
		tag.putString(NAME_KEY, (getName() == null) ? "" : getName());
	}
	
	public PlayerIdentity load(CompoundTag tag) {
		if (tag.contains(UUID_KEY)) {
			setUuid(tag.getUUID(UUID_KEY));
		}
		if (tag.contains(NAME_KEY)) {
			setName(tag.getString(NAME_KEY));
		}
		return this;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, uuid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PlayerIdentity other = (PlayerIdentity) obj;
		return Objects.equals(uuid, other.uuid);
	}

	@Override
	public String toString() {
		return "PlayerData [uuid=" + uuid + ", name=" + name + "]";
	}
}
