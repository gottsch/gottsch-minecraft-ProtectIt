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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.registry.bst.Interval;
import com.someguyssoftware.protectit.registry.bst.ProtectedIntervalTree;

import net.minecraft.nbt.CompoundNBT;

/**
 * 
 * @author Mark Gottschling on Oct 5, 2021
 *
 */
public class ProtectionRegistry2 implements IBlockProtectionRegistry {
	private final ProtectedIntervalTree tree = new ProtectedIntervalTree();

	@Override
	public void addProtection(ICoords coords) {
		addProtection(coords, coords);
	}

	@Override
	public void addProtection(ICoords coords1, ICoords coords2) {
		tree.insert(new Interval(coords1, coords2));
	}

	@Override
	public void addProtection(ICoords coords, PlayerData data) {
		addProtection(coords, coords, data);
	}
	
	@Override
	public void addProtection(ICoords coords1, ICoords coords2, PlayerData data) {
		ProtectIt.LOGGER.info("adding protection -> {} to {}", coords1.toShortString(), coords2.toShortString());
		tree.insert(new Interval(coords1, coords2, new Interval.Data(data.getUuid(), data.getName())));
		ProtectIt.LOGGER.info("size of tree -> {}", getProtections(coords1, coords2).size());
	}

	@Override
	public void removeProtection(ICoords coords) {
		tree.delete(new Interval(coords, coords));
	}

	@Override
	public void removeProtection(ICoords coords1, ICoords coords2) {
		tree.delete(new Interval(coords1, coords2));
	}

	@Override
	public void removeProtection(ICoords coords1, ICoords coords2, String uuid) {
		tree.delete(new Interval(coords1, coords2), uuid);
	}

	@Override
	public void removeProtection(String uuid) {
		// walk the entire tree until it returns null
		Interval interval = null;
		do {
			interval = tree.delete(tree.getRoot(), p -> p.getData().getUuid().equalsIgnoreCase(uuid));
			ProtectIt.LOGGER.debug("remove by uuid interval -> {}", interval);
		} while (interval != null);
	}

	/**
	 * A list is returned, but only one element should be returned
	 * (else something went wrong)
	 */
	@Override
	public List<Box> getProtections(ICoords coords) {
		return getProtections(coords, coords);
	}

	@Override
	public List<Box> getProtections(ICoords coords1, ICoords coords2) {
		List<Interval> protections = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords2), false);
		List<Box> boxes = new ArrayList<>();
		protections.forEach(p -> {
			boxes.add(p.toBox());
		});
		return boxes;
	}

	@Override
	public List<Interval> getProtections(ICoords coords1, ICoords coords2, boolean findFast) {
		List<Interval> protections = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords2), findFast);
		return protections;
	}

	/**
	 * For a single block
	 * @param coords
	 * @return
	 */
	@Override
	public boolean isProtected(ICoords coords) {
		return isProtected(coords, coords);
	}

	/**
	 * 
	 * @param coords1
	 * @param coords2
	 * @return
	 */
	@Override
	public boolean isProtected(ICoords coords1, ICoords coords2) {
		List<Interval> protections = getProtections(coords1, coords2, true);
		if (protections.isEmpty()) {
			return false;
		}		 
		return true;
	}

	@Override
	public boolean isProtectedAgainst(ICoords coords, String uuid) {
		return isProtectedAgainst(coords, coords, uuid);
	}

	/**
	 * is protected against player uuid
	 * @param coords1
	 * @param coords2
	 * @param uuid
	 * @return 
	 * @return
	 */
	@Override
	public boolean isProtectedAgainst(ICoords coords1, ICoords coords2, String uuid) {
		List<Interval> protections = getProtections(coords1, coords2, true);
		if (protections.isEmpty()) {
//			ProtectIt.LOGGER.debug("empty protections - not protected");
			return false;
		}
		else {
			// interrogate each interval to determine if the uuid is the owner
			for (Interval p : protections) {
//				ProtectIt.LOGGER.debug("protection -> {}", p);
				if (p.getData().getUuid() == null || !p.getData().getUuid().equals(uuid)) {
//					ProtectIt.LOGGER.debug("protected against me!");
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public List<Interval> list() {
		List<Interval> protections = new ArrayList<>();
		tree.list(tree.getRoot(), protections);
		return protections;
	}

	@Override
	public List<Interval> find(Predicate<Interval> predicate) {
		List<Interval> protections = new ArrayList<>();
		tree.find(tree.getRoot(), predicate, protections);
		return protections;
	}
	
	/**
	 * Walk the tree and output user-friendly string display of protection areas
	 */
	@Override
	public List<String> toStringList() {
		List<String> protections = tree.toStringList(tree.getRoot());
		return protections;
	}
	
	/**
	 * 
	 * @param nbt
	 * @return
	 */
	public CompoundNBT save(CompoundNBT nbt) {
		ProtectIt.LOGGER.info("saving tree...");
		tree.save(nbt);
		return nbt;
	}

	/**
	 * 
	 */
	public void load(CompoundNBT nbt) {
		ProtectIt.LOGGER.info("loading tree...");
		clear();
		tree.load(nbt);
	}

	/**
	 * 
	 */
	public void clear() {
		tree.clear();
	}
}
