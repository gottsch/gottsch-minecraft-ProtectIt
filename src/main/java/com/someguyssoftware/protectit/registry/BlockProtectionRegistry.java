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
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.property.Property;
import com.someguyssoftware.protectit.registry.bst.IdentifierData;
import com.someguyssoftware.protectit.registry.bst.Interval;
import com.someguyssoftware.protectit.registry.bst.OwnershipData;
import com.someguyssoftware.protectit.registry.bst.ProtectedIntervalTree;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * 
 * @author Mark Gottschling on Oct 5, 2021
 *
 */
public class BlockProtectionRegistry implements IBlockProtectionRegistry {

	private static final String CLAIMS_KEY = "claims";

	private final Map<String, List<Property>> PROPERTIES_BY_OWNER = new HashMap<>(); 
	private final Map<ICoords, Property> PROPERTIES_BY_COORDS = new HashMap<>();

	private final Map<UUID, ProtectedIntervalTree> NESTED_PROPERTIES = new HashMap<>();
	
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
		Property claim = new Property(coords1, new Box(coords1, coords2), data);
		addProtection(claim);
	}

	/**
	 * 
	 */
	@Override
	public void addProtection(Property claim) {
		ProtectIt.LOGGER.debug("adding claim protection -> {}", claim);
		
		// add claims by owner
		List<Property> claims = null;
		// TODO make a method
		if (!PROPERTIES_BY_OWNER.containsKey(claim.getOwner().getUuid())) {
			// create new list entry
			PROPERTIES_BY_OWNER.put(claim.getOwner().getUuid(), new ArrayList<>());
		}
		claims = PROPERTIES_BY_OWNER.get(claim.getOwner().getUuid());
		claims.add(claim);
		
		// add claims by coords
		PROPERTIES_BY_COORDS.put(claim.getBox().getMinCoords(), claim);
		
		// add to BST
		tree.insert(new Interval(claim.getBox().getMinCoords(), claim.getBox().getMaxCoords(), new OwnershipData(claim.getOwner().getUuid(), claim.getOwner().getName())));
		ProtectIt.LOGGER.debug("size of tree -> {}", getProtections(claim.getBox().getMinCoords(), claim.getBox().getMaxCoords()).size());
	}

	/**
	 * 
	 * @param coords1
	 * @param coords2
	 * @param owner
	 * @param data
	 * @return
	 */
	public List<Interval> addWhitelist(ICoords coords1, ICoords coords2, PlayerData owner, PlayerData data) {
		List<Interval> whitelisted = new ArrayList<>();
		ProtectIt.LOGGER.debug("adding whitelist -> {} to {}", coords1.toShortString(), coords2.toShortString());
		List<Interval> list = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords2), false);
		list.forEach(i -> {
			if (i.getData() != null && i.getData().getOwner().getUuid().equalsIgnoreCase(owner.getUuid())) {
				// TODO check that whitelist doesn't already contain player
				i.getData().getWhitelist().add(new IdentifierData(data.getUuid(), data.getName()));
				whitelisted.add(i);
			}
		});
		ProtectIt.LOGGER.debug("size of tree -> {}", getProtections(coords1, coords2).size());
		return whitelisted;
	}

	@Override
	public void removeProtection(ICoords coords) {
		removeProtection(coords, coords);
	}

	@Override
	public void removeProtection(ICoords coords1, ICoords coords2) {
//		ProtectIt.LOGGER.debug("in remove protection for c1 -> {}, c2 -> {}", coords1, coords2);
		List<Box> protections = getProtections(coords1, coords2);
//		ProtectIt.LOGGER.debug("found protections -> {}", protections);
		removeClaims(protections);
		protections.forEach(p -> {
			tree.delete(new Interval(p.getMinCoords(), p.getMaxCoords()));
		});
		
	}

	/**
	 * 
	 * @param protections
	 */
	private void removeClaims(final List<Box> protections) {
		if (!protections.isEmpty()) {
			protections.forEach(p -> {
				Property property = PROPERTIES_BY_COORDS.remove(p.getMinCoords());
//				ProtectIt.LOGGER.debug("claim was removed from BY_COORDS -> {}", claim);
//				ProtectIt.LOGGER.debug("claims after removal from BY_COORDS -> {}", CLAIMS_BY_COORDS);
				PROPERTIES_BY_OWNER.values().forEach(l -> {
					l.removeIf(c -> c.getBox().getMinCoords().equals(p.getMinCoords()));
				});
//				ProtectIt.LOGGER.debug("claims_by_owner -> {}", CLAIMS_BY_OWNER);

			});
		}
	}
	
	@Override
	public void removeProtection(ICoords coords1, ICoords coords2, String uuid) {
//		ProtectIt.LOGGER.debug("in remove protection for c1 -> {}, c2 -> {}, uuid -> {}", coords1, coords2, uuid);
		List<Box> protections = getProtections(coords1, coords2);
//		ProtectIt.LOGGER.debug("found protections -> {}", protections);
		removeClaims(protections, uuid);
		protections.forEach(p -> {
			List<Interval> intervals = tree.delete(new Interval(p.getMinCoords(), p.getMaxCoords()), uuid);
//			ProtectIt.LOGGER.debug("removed from tree -> {}", intervals);
		});
	}

	private void removeClaims(final List<Box> protections, final String uuid) {
		if (!protections.isEmpty()) {
			protections.forEach(p -> {
//				ProtectIt.LOGGER.debug("claims -> {}", CLAIMS_BY_COORDS);
				Property claim = getClaimByCoords(p.getMinCoords());
//				ProtectIt.LOGGER.debug("claim -> {}", claim);
				if (claim != null && claim.getOwner().getUuid().equalsIgnoreCase(uuid)) {
					PROPERTIES_BY_COORDS.remove(p.getMinCoords());
//					ProtectIt.LOGGER.debug("claim was removed from BY_COORDS -> {}", claim);
				}
				List<Property> claims = PROPERTIES_BY_OWNER.get(uuid);
				if (claims != null && !claims.isEmpty()) {
					claims.removeIf(c -> c.getBox().getMinCoords().equals(p.getMinCoords()));
				}
			});
		}
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
		PROPERTIES_BY_OWNER.remove(uuid);		
		PROPERTIES_BY_COORDS.entrySet().removeIf(entry -> entry.getValue().getOwner().getUuid().equalsIgnoreCase(uuid));
	}

	/**
	 */
	@Override
	public List<Property> getAll() {
		List<Property> claims = new ArrayList<>();
		claims.addAll(PROPERTIES_BY_COORDS.values());
		return claims;
	}
	
	/**
	 * Get all the claims owned by player.
	 * @return A list of claims or an empty list if a claim isn't found.
	 */
	@Override
	public List<Property> getProtections(String uuid) {
		List<Property> claims = PROPERTIES_BY_OWNER.get(uuid);
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
		return getProtections(coords1, coords2, false);
	}

	@Override
	public List<Box> getProtections(ICoords coords1, ICoords coords2, boolean findFast) {
		return getProtections(coords1, coords2, findFast, true);
	}
	
	@Override
	public List<Box> getProtections(ICoords coords1, ICoords coords2, boolean findFast, boolean includeBorder) {
		List<Interval> protections = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords2), findFast, includeBorder);
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
		return isProtected(coords1, coords2, true);
	}

	/**
	 * 
	 */
	@Override
	public boolean isProtected(ICoords coords1, ICoords coords2, boolean includeBorders) {
		List<Box> protections = getProtections(coords1, coords2, true, includeBorders);
		if (protections.isEmpty()) {
			return false;
		}		 
		return true;
	}

	@Override
	public boolean isProtectedAgainst(ICoords coords, String uuid, int permission) {
		return isProtectedAgainst(coords, coords, uuid, permission);
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
	public boolean isProtectedAgainst(ICoords coords1, ICoords coords2, String uuid, int permission) {
		List<Interval> protections = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords2), true);
		if (protections.isEmpty()) {
//			ProtectIt.LOGGER.debug("empty protections - not protected");
			return false;
		}
		else {

			// interrogate each interval to determine if the uuid is the owner
			for (Interval p : protections) {
//				ProtectIt.LOGGER.debug("isProtectedAgainst -> {}, protection -> {}", uuid, p);
				// short circuit if owner or no owner
				if (p.getData().getOwner().getUuid().equalsIgnoreCase(uuid)) {
					break;
				}
				// legacy check when admin-owned/server-owner property was given no owner
				if (StringUtils.isBlank(p.getData().getOwner().getUuid())) {
					return true;
				}
				
				// get the claim
				Property property = PROPERTIES_BY_COORDS.get(p.getCoords1());
				ProtectIt.LOGGER.debug("isProtectedAgainst.claimsByCoords -> {}, claim -> {}", p.getCoords1(), property);

				// short circuit on permission
				if (property.hasPermission(permission)) {
					break;
				}
				
				// cycle through whitelist
//				boolean isWhitelist = false;
				if (!property.getWhitelist().isEmpty()) {
					ProtectIt.LOGGER.debug("isProtectedAgainst whitelist is not null");
//					for(IdentifierData id : p.getData().getWhitelist()) {
					for (PlayerData id : property.getWhitelist()) {
						ProtectIt.LOGGER.debug("isProtectedAgainst compare whitelist id -> {} to uuid -> {}", id.getUuid(), uuid);
						if (id.getUuid().equalsIgnoreCase(uuid)) {
//							isWhitelist = true;
//							break;
							// TODO this is bad. this will only check the first property
							return false;
						}
					}
				}
				// TODO this is wrong as well, will only allow one property
				// to be examined. the coords could be a wide region and not
				// just one block.
				return true;
//				if (!isWhitelist) {
//					ProtectIt.LOGGER.debug("protected against me!");
//					return true;
//				}
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

	// TODO change to return Claim.EMPTY is not found or Optional<Claim> (Optional is better)
	 @Override
	public Property getClaimByCoords(ICoords coords) {
		return PROPERTIES_BY_COORDS.get(coords);
	}
	
	@Override
	public List<Property> findByClaim(Predicate<Property> predicate) {
		List<Property> claims = new ArrayList<>();
		PROPERTIES_BY_COORDS.values().forEach(claim -> {
			if (predicate.test(claim)) {
				claims.add(claim);
			}
		});
		return claims;
	}
	
	@Override 
	public List<Box> find(Predicate<Box> predicate) {
		List<Box> boxes = new ArrayList<>();
		PROPERTIES_BY_COORDS.values().forEach(claim -> {
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
	public synchronized CompoundTag save(CompoundTag nbt) {
		ProtectIt.LOGGER.debug("saving registry...");

		ListTag list = new ListTag();
		PROPERTIES_BY_COORDS.forEach((coords, claim) -> {
			ProtectIt.LOGGER.debug("registry saving claim -> {}", claim);
			CompoundTag claimNbt = new CompoundTag();
			claim.save(claimNbt);
			list.add(claimNbt);
		});
		nbt.put(CLAIMS_KEY, list);

		return nbt;
	}

	/**
	 * 
	 */
	public synchronized void load(CompoundTag nbt) {
		ProtectIt.LOGGER.debug("loading registry...");
		clear();

//		tree.load(nbt);

		if (nbt.contains(CLAIMS_KEY)) {
			ListTag list = nbt.getList(CLAIMS_KEY, 10);
			list.forEach(element -> {

				Property property = new Property().load((CompoundTag)element);
				ProtectIt.LOGGER.debug("loaded claim -> {}", property);
				PROPERTIES_BY_COORDS.put(property.getCoords(), property);
//				ProtectIt.LOGGER.debug("coords mapped claim -> {}", CLAIMS_BY_COORDS.get(claim.getCoords()));
				
				if (!PROPERTIES_BY_OWNER.containsKey(property.getOwner().getUuid())) {
					// create new list entry
					PROPERTIES_BY_OWNER.put(property.getOwner().getUuid(), new ArrayList<>());
				}
				PROPERTIES_BY_OWNER.get(property.getOwner().getUuid()).add(property);
				ProtectIt.LOGGER.debug("claim BEFORE inserting into tree -> {}", PROPERTIES_BY_COORDS.get(property.getCoords()));
				tree.insert(new Interval(property.getBox().getMinCoords(), property.getBox().getMaxCoords(), new OwnershipData(property.getOwner().getUuid(), property.getOwner().getName())));
				ProtectIt.LOGGER.debug("claim AFTER inserting into tree -> {}", PROPERTIES_BY_COORDS.get(property.getCoords()));
				ProtectIt.LOGGER.debug("running loaded properties_by_coords -> {}", PROPERTIES_BY_COORDS);
			});
//			ProtectIt.LOGGER.debug("0.all loaded claims_by_coords -> {}", CLAIMS_BY_COORDS);
		}
		// print the loaded claim again
		ProtectIt.LOGGER.debug("1. all loaded properties_by_coords -> {}", PROPERTIES_BY_COORDS);
		ProtectIt.LOGGER.debug("all loaded in tree -> {}", tree.toStringList(tree.getRoot()));
	}

	/**
	 * 
	 */
	public void clear() {
		PROPERTIES_BY_OWNER.clear();
		PROPERTIES_BY_COORDS.clear();
		tree.clear();
	}
}
