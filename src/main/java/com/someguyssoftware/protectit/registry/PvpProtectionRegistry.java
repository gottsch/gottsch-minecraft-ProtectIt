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
import com.someguyssoftware.protectit.registry.bst.Interval;

import net.minecraft.nbt.CompoundNBT;

/**
 * 
 * @author Mark Gottschling on Nov 6, 2021
 *
 */
public class PvpProtectionRegistry implements IPvpProtectionRegistry {

	@Override
	public void addProtection(ICoords coords) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addProtection(ICoords coords1, ICoords coords2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void load(CompoundNBT nbt) {
		// TODO Auto-generated method stub

	}

	@Override
	public CompoundNBT save(CompoundNBT nbt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeProtection(ICoords coords) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeProtection(ICoords coords1, ICoords coords2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isProtected(ICoords coords) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProtected(ICoords coords1, ICoords coords2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Box> getProtections(ICoords coords) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Box> getProtections(ICoords coords1, ICoords coords2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Box> getProtections(ICoords coords1, ICoords coords2, boolean findFast) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Box> list() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Box> find(Predicate<Box> predicate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> toStringList() {
		// TODO Auto-generated method stub
		return null;
	}

}
