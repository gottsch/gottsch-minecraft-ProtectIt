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

import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.registry.bst.Interval;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * TODO move to GottschCore
 *  GottschCore replacement for AxisAlignedBB.
 * @author Mark Gottschling on Oct 30, 2021
 *
 */
@Deprecated
public class Box {
	// TODO determine what invalid y is (important when moving to mc1.17)
	public static final Box EMPTY = new Box(new Coords(0, -255, 0), new Coords(0, -255, 0));
	private static final String MIN_COORDS = "min";
	private static final String MAX_COORDS = "max";
	private ICoords minCoords;
	private ICoords maxCoords;
	
	/**
	 * 
	 */	
	public Box(final ICoords c1, final ICoords c2) {
		int minX = Math.min(c1.getX(), c2.getX());
		int minY = Math.min(c1.getY(), c2.getY());
		int minZ = Math.min(c1.getZ(), c2.getZ());
		int maxX = Math.max(c1.getX(), c2.getX());
		int maxY = Math.max(c1.getY(), c2.getY());
		int maxZ = Math.max(c1.getZ(), c2.getZ());
		
		setMinCoords(new Coords(minX, minY, minZ));
		setMaxCoords(new Coords(maxX, maxY, maxZ));
	}

	/**
	 * 
	 * @param c
	 */
	public Box(final ICoords c) {
		setMinCoords(new Coords(c));
		setMaxCoords(c.add(1, 1, 1));
	}
	
	/*
	 * Constructor from AxisAlignedBB.
	 */
	public Box(final AxisAlignedBB aabb) {
		setMinCoords(new Coords((int)aabb.minX, (int)aabb.minY, (int)aabb.minZ));
		setMaxCoords(new Coords((int)aabb.maxX, (int)aabb.maxY, (int)aabb.maxZ));
	}
	
	/**
	 * 
	 * @return
	 */
	public ICoords getSize() {
		return getMaxCoords().delta(getMinCoords());
	}
	
	/**
	 * 
	 * @param nbt
	 */
	public void save(CompoundNBT nbt) {
		ProtectIt.LOGGER.debug("saving box -> {}", this);

		CompoundNBT coordsNbt1 = new CompoundNBT();
		CompoundNBT coordsNbt2 = new CompoundNBT();

		coordsNbt1 = saveCoords(getMinCoords());
		coordsNbt2 = saveCoords(getMaxCoords());

		nbt.put(MIN_COORDS, coordsNbt1);
		nbt.put(MAX_COORDS, coordsNbt2);
	}
	
	/**
	 * 
	 * @param nbt
	 * @return
	 */
	public static Box load(CompoundNBT nbt) {
		Box box;
		ICoords c1;
		ICoords c2;
		if (nbt.contains(MIN_COORDS)) {
			c1 = loadCoords(nbt, MIN_COORDS);
		}
		else {
			return Box.EMPTY;
		}
		
		if (nbt.contains(MAX_COORDS)) {
			c2 = loadCoords(nbt, MAX_COORDS);
		}
		else {
			return Box.EMPTY;
		}		
		box = new Box(c1, c2);
		
		ProtectIt.LOGGER.debug("loaded box -> {}", box);
		return box;
	}
	
	/**
	 * TODO move into Coords in GottschCore
	 * @param coords
	 * @return
	 */
	private static CompoundNBT saveCoords(ICoords coords) {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putInt("x", coords.getX());
		nbt.putInt("y", coords.getY());
		nbt.putInt("z", coords.getZ());
		return nbt;
	}

	/**
	 * TODO move into Coords in GottschCore
	 * @param nbt
	 * @param name
	 * @return
	 */
	private static ICoords loadCoords(CompoundNBT nbt, String name) {
		CompoundNBT coords = nbt.getCompound(name);
		int x = coords.getInt("x");
		int y = coords.getInt("y");
		int z = coords.getInt("z");
		return new Coords(x, y, z);
	}
	
	/**
	 * @return the minCoords
	 */
	public ICoords getMinCoords() {
		return minCoords;
	}

	/**
	 * @param minCoords the minCoords to set
	 */
	public void setMinCoords(ICoords minCoords) {
		this.minCoords = minCoords;
	}

	/**
	 * @return the maxCoords
	 */
	public ICoords getMaxCoords() {
		return maxCoords;
	}

	/**
	 * @param maxCoords the maxCoords to set
	 */
	public void setMaxCoords(ICoords maxCoords) {
		this.maxCoords = maxCoords;
	}

	@Override
	public String toString() {
		return "Box [minCoords=" + minCoords.toShortString() + ", maxCoords=" + maxCoords.toShortString() + "]";
	}
}
