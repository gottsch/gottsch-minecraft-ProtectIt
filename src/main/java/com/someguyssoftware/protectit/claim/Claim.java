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
import com.someguyssoftware.protectit.registry.PlayerData;

/**
 * 
 * @author Mark Gottschling on Nov 4, 2021
 *
 */
public class Claim {
	public static Claim EMPTY = new Claim(Coords.EMPTY, Box.EMPTY);

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
		this(Coords.EMPTY, box.EMPTY);
	}

	/**
	 * 
	 * @param coords
	 * @param box
	 */
	public Claim(ICoords coords, Box box) {
		setCoords(coords);
		setBox(box);
		setOwner(PlayerData.EMPTY);
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
	public void save(CompoundNBT nbt) {
		ProtectIt.LOGGER.debug("saving claim -> {}", this);

		CompoundNBT ownerNbt = CompoundNBT();
		getOwner().save(dataNbt);
		nbt.put(OWNER_KEY, dataNbt);

		CompoundNBT coordsNbt = new CompoundNBT();
		getCoords().save(coordsNbt);
		nbt.put(COORDS_KEY, coordsNbt);

		CompoundNBT boxNbt = new CompoundNBT();
		getBox().save(boxNbt);
		nbt.put(BOX_KEY, boxNbt);

		nbt.putString(NAME_KEY, getName());

		ListNBT list = new ListNBT();
		getWhitelist().forEach(data -> {
			CompoundNBT playerNbt = new CompoundNBT();
			data.save(playerNbt);
			list.add(playerNbt);
		})
		nbt.putList(WHITELIST_KEY, list);
	}

	/**
	 * 
	 * @param nbt
	 * @return
	 */
	public Claim load(CompoundNBT nbt) {
		ProtectIt.LOGGER.debug("loading claim -> {}", this);

		if (nbt.contains(OWNER_KEY)) {
			getOwner().load(nbt.get(OWNER_KEY));
		}
		if (nbt.contains(COORDS_KEY)) {
			getCoords().load(nbt.get(COORDS_KEY));
		}
		if (nbt.contains(BOX_KEY)) {
			getbox().load(nbt.get(BOX_KEY));
		}
		if (nbt.contains(NAME_KEY)) {
			setName(nbt.getString(NAME_KEY));
		}
		if (nbt.contains(WHITELIST_KEY)) {
			ListNBT list = nbt.getList(WHITELIST_KEY, 10);
			list.forEach(element -> {
				PlayerData playerData = new PlayerData("");
				playerData.load(element);
				getWhitelist().add(playerData);
			})
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

	protected String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Claim [name=" + name + ", owner=" + owner + ", whitelist=" + whitelist + ", coords=" + coords + ", box="
				+ box + "]";
	}
}
