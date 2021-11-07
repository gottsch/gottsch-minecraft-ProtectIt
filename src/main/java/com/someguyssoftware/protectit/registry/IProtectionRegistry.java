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

import java.util.List;
import java.util.function.Predicate;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.ICoords;

import net.minecraft.nbt.CompoundNBT;

/**
 * 
 * @author Mark Gottschling on Nov 6, 2021
 *
 */
public interface IProtectionRegistry {
	/**
	 * 
	 * @param coords
	 */
	public void addProtection(ICoords coords);
	
	/**
	 * add protection for the region specified by coords1 -> coords2
	 * @param coords1
	 * @param coords2
	 */
	public void addProtection(ICoords coords1, ICoords coords2);
	
	/**
	 * removes any protection intervals that coords intersects with
	 */
	public void removeProtection(ICoords coords);
	public void removeProtection(ICoords coords1, ICoords coords2);
	
	/**
	 * determines if the point at coords is a protected region
	 * @param coords
	 * @return
	 */
	public boolean isProtected(ICoords coords);
	public boolean isProtected(ICoords coords1, ICoords coords2);
	
	/**
	 * get a list of all protections that intersect with the coords area
	 * @param coords
	 * @return
	 */
	public List<Box> getProtections(ICoords coords);
	public List<Box> getProtections(ICoords coords1, ICoords coords2);
	// TODO replace Interval with Box
	public List<Box> getProtections(ICoords coords1, ICoords coords2, boolean findFast);

	/**
	 * clear all protections
	 */
	public void clear();
	
	/**
	 * TODO replace Interval with Box
	 * get all protections in a List<>
	 * @return
	 */
	public List<Box> list();
	public List<Box> find(Predicate<Box> predicate);
	
	/**
	 * get all protections in a read-friendly format
	 * @return
	 */
	public List<String> toStringList();
	
	/**
	 * load from an NBT
	 * @param nbt
	 */
	public void load(CompoundNBT nbt);	
	
	/**
	 * save to a NBT
	 * @param nbt
	 * @return
	 */
	public CompoundNBT save(CompoundNBT nbt);
}
