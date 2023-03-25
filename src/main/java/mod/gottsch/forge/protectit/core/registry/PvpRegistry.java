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

import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.registry.bst.Interval;
import mod.gottsch.forge.protectit.core.registry.bst.OwnershipData;
import mod.gottsch.forge.protectit.core.registry.bst.ProtectedIntervalTree;
import mod.gottsch.forge.protectit.core.zone.Zone;
import mod.gottsch.forge.protectit.core.zone.ZoneUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * 
 * @author Mark Gottschling on Nov 6, 2021
 *
 */
public class PvpRegistry implements IPvpRegistry {
	private static final String ZONES_KEY = "zones";
	
	/*
	 * a multimap of properties by property coords.
	 * this is the main backing map for the data held in the BST.
	 * the min. coords of the property is used as the key.
	 */
	private final Multimap<ICoords, Zone> BY_COORDS = ArrayListMultimap.create();

	
	/*
	 * an interval binary search tree (interval-bst) for fast lookups for property access/mutation actions.
	 * this is the used when only the location (coords/blockPos) is known.
	 */
	private final ProtectedIntervalTree tree = new ProtectedIntervalTree();

	public void addZone(Zone zone) {
		BY_COORDS.put(zone.getBox().getMinCoords(), zone);
		/*
		 *  NOTE use the UUID of the zone as the Ownership, so that
		 *  intervals can be distinguised from each other when their coords
		 *  are the same ie overlap, and can be treated independently
		 *  ie 'parent' zone can be removed without removing nested zone.
		 */
		tree.insert(new Interval(zone.getBox().getMinCoords(), zone.getBox().getMaxCoords(),
				new OwnershipData(zone.getUuid().toString(), "")));
		ProtectIt.LOGGER.debug("size of tree -> {}", getProtections(zone.getBox().getMinCoords(), zone.getBox().getMaxCoords()).size());
	}
	
	/**
	 * NOTE getProtections only return the Box objects from the BST
	 */
	public List<Box> getProtections(ICoords coords) {
		return getProtections(coords, coords);
	}

	public List<Box> getProtections(ICoords coords1, ICoords coords2) {
		return getProtections(coords1, coords2, false);
	}

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
	
	@Override
	public void removeZone(Zone zone) {
		// NOTE zones are independent of each other, so care must be taken
		// when the coords are the same.
		tree.delete(new Interval(zone.getBox().getMinCoords(), zone.getBox().getMaxCoords(),
				new OwnershipData(zone.getUuid().toString(), "")));

		// remove zone from map
		BY_COORDS.remove(zone.getBox().getMinCoords(), zone);
	}
	
	@Override
	public void removeZone(UUID uuid, ICoords coords) {
		tree.delete(new Interval(coords, coords.add(1, 1, 1),
				new OwnershipData(uuid.toString(), "")));
		
		BY_COORDS.get(coords).removeIf(z -> z.getUuid().equals(uuid));
	}
	
	@Override
	public void changePermission(UUID zoneUuid, Box box, int permission, boolean value) {
		Optional<Zone> zone = BY_COORDS.get(box.getMinCoords()).stream().filter(z -> z.getUuid().equals(zoneUuid)).findFirst();
		if (zone.isPresent()) {
			zone.get().setPermission(permission, value);
		}
	}
	
	/**
	 * For a single block
	 * @param coords
	 * @return
	 */
	public boolean isProtected(ICoords coords) {
		return isProtected(coords, coords);
	}

	/**
	 * 
	 * @param coords1
	 * @param coords2
	 * @return
	 */
	public boolean isProtected(ICoords coords1, ICoords coords2) {
		return isProtected(coords1, coords2, true);
	}

	/**
	 * 
	 */
	public boolean isProtected(ICoords coords1, ICoords coords2, boolean includeBorders) {
		List<Box> protections = getProtections(coords1, coords2, true, includeBorders);
		if (protections.isEmpty()) {
			return false;
		}		 
		return true;
	}
	
	/**
	 * is protected against a permission
	 * @param coords
	 * @param coords2
	 * @param uuid
	 * @return 
	 * @return
	 */
	@Override
	public boolean isProtectedAgainst(ICoords coords, int permission) {
		// TODO should includeBorders = true
		// check for top-level protections
		List<Interval> protections = tree.getOverlapping(tree.getRoot(), new Interval(coords, coords), true, false);
		if (protections.isEmpty()) {
			return false;
		}
		else {
			// NOTE get first as there should only be one since using findFast option in search
			Interval protection = protections.get(0);
			ProtectIt.LOGGER.debug("isProtectedAgainst interval.coords -> {}, interval -> {}", coords, protection);
			
			// get all the zones by coord
			List<Zone> zones = (List<Zone>) BY_COORDS.get(protection.getCoords1());
			ProtectIt.LOGGER.debug("isProtectedAgainst properties -> {}", zones);

			Zone zone;
			Optional<Zone> target = ZoneUtil.getLeastSignificant(zones);
			if (target.isPresent()) {
				zone = target.get();
			} else {
				// by default, all zones are protected for all permissions
				return true;
			}
			ProtectIt.LOGGER.debug("isProtectedAgainst zones -> {}", zones);

			// short circuit on permission
			if (zone.hasPermission(permission)) {
				return false;
			}
			return true;
		}
	}
	
	/**
	 * 
	 * @param tag
	 * @return
	 */
	public synchronized CompoundTag save(CompoundTag tag) {
		ProtectIt.LOGGER.debug("saving registry...");

		if (!BY_COORDS.isEmpty()) {
			ListTag list = new ListTag();
			BY_COORDS.forEach((coords, zone) -> {
				ProtectIt.LOGGER.debug("registry saving property -> {}", zone);
				CompoundTag propertyTag = new CompoundTag();
				zone.save(propertyTag);
				list.add(propertyTag);
			});
			tag.put(ZONES_KEY, list);

		}
		return tag;
	}
	
	/**
	 * 
	 * @param tag
	 */
	public synchronized void load(CompoundTag tag) {
		ProtectIt.LOGGER.debug("loading registry...");
		clear();

		if (tag.contains(ZONES_KEY)) {
			ListTag list = tag.getList(ZONES_KEY, 10);
			list.forEach(element -> {
				Zone property = new Zone().load((CompoundTag)element);
				ProtectIt.LOGGER.debug("loaded property -> {}", property);
				BY_COORDS.put(property.getBox().getMinCoords(), property);

				ProtectIt.LOGGER.debug("zone BEFORE inserting into tree -> {}", BY_COORDS.get(property.getBox().getMinCoords()));
				tree.insert(new Interval(property.getBox().getMinCoords(), property.getBox().getMaxCoords()));

				ProtectIt.LOGGER.debug("zone AFTER inserting into tree -> {}", BY_COORDS.get(property.getBox().getMinCoords()));
				ProtectIt.LOGGER.debug("running loaded propertys_by_coords -> {}", BY_COORDS);
			});
		}

		// print the loaded property again
		ProtectIt.LOGGER.debug("1. all loaded zones by_coords -> {}", BY_COORDS);
		ProtectIt.LOGGER.debug("all loaded in tree -> {}", tree.toStringList(tree.getRoot()));
	}
	
	@Override
	public List<Zone> getAll() {
		List<Zone> zones = new ArrayList<>();
		zones.addAll(BY_COORDS.values());
		return zones;
	}
	
	/**
	 * 
	 */
	public void clear() {
		BY_COORDS.clear();
		tree.clear();
	}
	
	public List<Zone> getZoneByCoords(ICoords coords) {
		return (List<Zone>) BY_COORDS.get(coords);
	}
	
	/**
	 * 
	 */
	public void dump() {
		Path path = Paths.get("config", ProtectIt.MODID, "dumps").toAbsolutePath();
		if (Files.notExists(path)) { 
			try {
				Files.createDirectories(path);
			} catch (Exception e) {
				ProtectIt.LOGGER.error("unable to create dump file: ", e);
			}
		}

		GsonBuilder gsonBuilder = new GsonBuilder();		
		try {
			Type listType = new TypeToken<List<Zone>>() {}.getType();
			Gson gson = gsonBuilder.setPrettyPrinting().create();
			
			// convert map to list
			List<Zone> list = new ArrayList<>();
			BY_COORDS.forEach((coords, zone) -> {
				list.add(zone);
			});

			String json = gson.toJson(list, listType);
			FileWriter fw = new FileWriter(path.resolve("zone-registry.json").toAbsolutePath().toString());
			fw.write(json);
			fw.close();

		} catch (Exception e) {
			ProtectIt.LOGGER.error("error writing protection registry to file -> ", e);
		}
	}

	@Override
	public int size() {
		return BY_COORDS.size();
	}


}
