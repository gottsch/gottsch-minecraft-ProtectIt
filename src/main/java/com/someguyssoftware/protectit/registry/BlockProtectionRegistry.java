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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.registry.bst.IdentifierData;
import com.someguyssoftware.protectit.registry.bst.Interval;
import com.someguyssoftware.protectit.registry.bst.OwnershipData;
import com.someguyssoftware.protectit.registry.bst.ProtectedIntervalTree;

import net.minecraft.nbt.CompoundNBT;

/**
 * 
 * @author Mark Gottschling on Oct 5, 2021
 *
 */
public class BlockProtectionRegistry implements IBlockProtectionRegistry {

	private static final CLAIMS_KEY = "claims";

	private final Map<String, List<Claim>> CLAIMS_BY_OWNER = new HashMap<>(); 
	private final Map<ICoords, Claim> CLAIMS_BY_COORDS = new HashMap<>();

	/**
	 * Interval Binary Search Tree for fast lookups for block access/mutation actions
	 */
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
		
	}

	/**
	 * 
	 */
	@Override
	public void addProtection(Claim claim) {
		ProtectIt.LOGGER.info("adding protection -> {}", claim.getBox());
		
		// add claims by owner
		List<Claim> claims = null;
		if (!CLAIMS_BY_OWNER.containsKey(claim.getOwner().getUuid())) {
			// create new list entry
			claims = CLAIMS_BY_OWNER.put(claim.getOwner().getUuid(), new ArrayList<>());
		}
		else {
			claims = CLAIMS_BY_OWNER.get(claim.getOwner().getUuid());
		}
		claims.add(claim);
		
		// add claims by coords
		CLAIMS_BY_COORDS.put(claim.getCoords(), claim);
		
		// add to BST
		tree.insert(new Interval(claim.getBox().getMinCoords(), claim.getBox().getMaxCoords(), new OwnershipData(claim.getOwner().getUuid(), claim.getOwner().getName())));
		ProtectIt.LOGGER.info("size of tree -> {}", getProtections(claim.getBox().getMinCoords(), claim.getBox().getMaxCoords()).size());
	}

	/**
	 * TODO not part of IBlockProtectionRegistry... need to abstract it out.
	 * @param coords1
	 * @param coords2
	 * @param owner
	 * @param data
	 * @return
	 */
	public List<Interval> addWhitelist(ICoords coords1, ICoords coords2, PlayerData owner, PlayerData data) {
		List<Interval> whitelisted = new ArrayList<>();
		ProtectIt.LOGGER.info("adding whitelist -> {} to {}", coords1.toShortString(), coords2.toShortString());
		List<Interval> list = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords2), false);
		list.forEach(i -> {
			if (i.getData() != null && i.getData().getOwner().getUuid().equalsIgnoreCase(owner.getUuid())) {
				// TODO check that whitelist doesn't already contain player
				i.getData().getWhitelist().add(new IdentifierData(data.getUuid(), data.getName()));
				whitelisted.add(i);
			}
		});
		ProtectIt.LOGGER.info("size of tree -> {}", getProtections(coords1, coords2).size());
		return whitelisted;
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
			interval = tree.delete(tree.getRoot(), p -> p.getData().getOwner().getUuid().equalsIgnoreCase(uuid));
			ProtectIt.LOGGER.debug("remove by uuid interval -> {}", interval);
		} while (interval != null);
		// TODO wouldn't have to do do-while if used tree.delete2()
		
		//delete from CLAIMs registries
		CLAIMS_BY_OWNER.remove(uuid);		
		CLAIMS_BY_COORDS.entrySet().removeIf(entry -> entry.getValue().getOwner().getUuid().equalsIgnoreCase(uuid));
	}

	/**
	 */
	@Override
	public List<Charm> getAll() {
		List<Claim> claims = new ArrayList<>();
		claims.addAll(CLAIMS_BY_COORDS.values());
	}
	
	/**
	 * Get all the claims owned by player.
	 * @return A list of claims or an empty list if a claim isn't found.
	 */
	@Override
	public List<Claim> getProtections(String uuid) {
		List<Claim> claims = CLAIMS_BY_OWNER.get(uuid);
		if (claims == null) {
			claims = new ArrayList<>();
		}
		return claims;
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
	public List<Box> getProtections(ICoords coords1, ICoords coords2, boolean findFast) {
		List<Interval> protections = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords2), findFast);
		List<Box> boxes = new ArrayList<>();
		protections.forEach(p -> {
			boxes.add(p.toBox());
		});
		return boxes;
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
		List<Box> protections = getProtections(coords1, coords2, true);
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
		List<Interval> protections = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords2), true);
		if (protections.isEmpty()) {
//			ProtectIt.LOGGER.debug("empty protections - not protected");
			return false;
		}
		else {
			// interrogate each interval to determine if the uuid is the owner
			for (Interval p : protections) {
//				ProtectIt.LOGGER.debug("protection -> {}", p);
				// TODO cycle through whitelist
				boolean isWhitelist = false;
				if (!p.getData().getWhitelist().isEmpty()) {
					for(IdentifierData id : p.getData().getWhitelist()) {
						if (id.getUuid().equals(uuid)) {
							isWhitelist = true;
							break;
						}
					}
				}
				if (p.getData().getOwner().getUuid() == null || (!p.getData().getOwner().getUuid().equals(uuid) && !isWhitelist)) {
//					ProtectIt.LOGGER.debug("protected against me!");
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public List<Box> list() {
		// TODO walk CLAIMS_BY_COORDS instead
		List<Interval> protections = new ArrayList<>();
		tree.list(tree.getRoot(), protections);
		List<Box> boxes = new ArrayList<>();
		protections.forEach(p -> {
			boxes.add(p.toBox());
		});
		return boxes;
	}

	@Override
	public List<Claim> findByClaim(Predicate<Claim> predicate) {
//		List<Interval> protections = new ArrayList<>();
//		tree.find(tree.getRoot(), predicate, protections);
//		List<Box> boxes = new ArrayList<>();
//		protections.forEach(p -> {
//			boxes.add(p.toBox());
//		});
//		return boxes;
		List<Claim> claims = new ArrayList<>();
		CLAIMS_BY_COORDS.values().forEach(claim -> {
			if (predicate.test(claim)) {
				claims.add(claim);
			}
		});
		return claims;
	}
	
	@Override 
	public List<Box> find(Predicate<Box> predicate) {
		List<Box> boxes = new ArrayList<>();
		CLAIMS_BY_COORDS.values().forEach(claim -> {
			if (predicate.test(claim.getBox())) {
				boxes.add(claim.getBox());
			}
		});
		return boxes;
		// see https://monospacedmonologues.com/2016/01/casting-lambdas-in-java/
		// to see if casting / generic lambdas would work here
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
		// TODO save CLAIMS_BY_COORDS instead
		//ProtectIt.LOGGER.info("saving tree...");
		tree.save(nbt);

		ListNBT list = new ListNBT();
		CLAIMS_BY_COORDS.forEach(claim -> {
			CompoundNBT claimNbt = new CompoundNBT();
			claim.save(claimNbt);
			list.add(claimNbt);
		});
		nbt.put(CLAIMS_KEY, list);

		return nbt;
	}

	/**
	 * 
	 */
	public void load(CompoundNBT nbt) {
		ProtectIt.LOGGER.info("loading tree...");
		clear();

		//tree.load(nbt);

		if (nbt.contains(CLAIMS_KEY)) {
			ListNBT list = nbt.getList(CLAIMS_KEY, 10);
			list.forEach(element -> {
				Claim claim = new Claim().load(element);
				CLAIMS_BY_COORDS.put(claim.getCoords(), claim);
				CLAIMS_BY_OWNER.put(claim.getOwner().getUuid(), claim);
				tree.insert(new Interval(claim.getBox().getMinCoords()), claim.getBox().getMaxCoords());
			})
		}
	}

	/**
	 * 
	 */
	public void clear() {
		CLAIMS_BY_OWNER.clear();
		CLAIMS_BY_COORDS.clear();
		tree.clear();
	}
}
