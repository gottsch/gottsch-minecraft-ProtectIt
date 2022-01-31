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
package com.someguyssoftware.protectit.claim;

import java.util.ArrayList;
import java.util.List;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.registry.PlayerData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;


/**
 * 
 * @author Mark Gottschling on Nov 4, 2021
 *
 */
public class Claim {
	public static Claim EMPTY = new Claim(WorldInfo.EMPTY_COORDS, Box.EMPTY);

	public static final String NO_NAME = "";
	public static final String NAME_KEY = "name";
	public static final String OWNER_KEY = "owner";
	public static final String COORDS_KEY = "coords";
	public static final String BOX_KEY = "box";
	public static final String WHITELIST_KEY = "whitelist";
	
	private String name;
	private PlayerData owner;
	private List<PlayerData> whitelist;
	private ICoords coords;
	private Box box;
	
	// TODO probably a good candidate for a Builder
	// TODO add equals, hashCode()

	/**
	 * Empty constructor
	 */
	public Claim() {
		this(WorldInfo.EMPTY_COORDS, Box.EMPTY);
	}

	/**
	 * 
	 * @param coords
	 * @param box
	 */
	public Claim(ICoords coords, Box box) {
		setCoords(coords);
		setBox(box);
		setOwner(new PlayerData());
		setName(NO_NAME);
	}

	public Claim(ICoords coords, Box box, PlayerData data) {
		this(coords, box);
		setOwner(data);
	}
	
	public Claim(ICoords coords, Box box, PlayerData data, String name) {
		this(coords, box, data);
		setName(name);
	}
	
	/**
	 * 
	 * @param nbt
	 */
	public void save(CompoundTag nbt) {
		ProtectIt.LOGGER.debug("saving claim -> {}", this);

		CompoundTag ownerNbt = new CompoundTag();
		getOwner().save(ownerNbt);
		nbt.put(OWNER_KEY, ownerNbt);

		CompoundTag coordsNbt = new CompoundTag();
		getCoords().save(coordsNbt);
		nbt.put(COORDS_KEY, coordsNbt);

		CompoundTag boxNbt = new CompoundTag();
		getBox().save(boxNbt);
		nbt.put(BOX_KEY, boxNbt);

		nbt.putString(NAME_KEY, getName());

		ListTag list = new ListTag();
		getWhitelist().forEach(data -> {
			CompoundTag playerNbt = new CompoundTag();
			data.save(playerNbt);
			list.add(playerNbt);
		});
		nbt.put(WHITELIST_KEY, list);
	}

	/**
	 * 
	 * @param nbt
	 * @return
	 */
	public Claim load(CompoundTag nbt) {
//		ProtectIt.LOGGER.debug("loading claim...");

		if (nbt.contains(OWNER_KEY)) {
			getOwner().load(nbt.getCompound(OWNER_KEY));
		}
		if (nbt.contains(COORDS_KEY)) {
			setCoords(WorldInfo.EMPTY_COORDS.load(nbt.getCompound(COORDS_KEY)));
		}
		if (nbt.contains(BOX_KEY)) {
			setBox(Box.load(nbt.getCompound(BOX_KEY)));
		}
		if (nbt.contains(NAME_KEY)) {
			setName(nbt.getString(NAME_KEY));
		}
		if (nbt.contains(WHITELIST_KEY)) {
			ListTag list = nbt.getList(WHITELIST_KEY, 10);
			list.forEach(element -> {
				PlayerData playerData = new PlayerData("");
				playerData.load((CompoundTag)element);
				getWhitelist().add(playerData);
			});
		}
		return this;
	}

	public PlayerData getOwner() {
		return owner;
	}

	public void setOwner(PlayerData owner) {
		this.owner = owner;
	}

	public List<PlayerData> getWhitelist() {
		if (whitelist == null) {
			whitelist = new ArrayList<>();
		}
		return whitelist;
	}

	public void setWhitelist(List<PlayerData> whitelist) {
		this.whitelist = whitelist;
	}

	public ICoords getCoords() {
		return coords;
	}

	public void setCoords(ICoords coords) {
		this.coords = coords;
	}

	public Box getBox() {
		return box;
	}

	public void setBox(Box box) {
		this.box = box;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Claim [name=" + name + ", owner=" + owner + ", whitelist=" + whitelist + ", coords=" + coords + ", box="
				+ box + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((box == null) ? 0 : box.hashCode());
		result = prime * result + ((coords == null) ? 0 : coords.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((whitelist == null) ? 0 : whitelist.hashCode());
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
		Claim other = (Claim) obj;
		if (box == null) {
			if (other.box != null)
				return false;
		} else if (!box.equals(other.box))
			return false;
		if (coords == null) {
			if (other.coords != null)
				return false;
		} else if (!coords.equals(other.coords))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (whitelist == null) {
			if (other.whitelist != null)
				return false;
		} else if (!whitelist.equals(other.whitelist))
			return false;
		return true;
	}
}
