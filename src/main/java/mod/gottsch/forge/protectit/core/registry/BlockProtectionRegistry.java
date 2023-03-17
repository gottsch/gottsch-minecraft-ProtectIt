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
package mod.gottsch.forge.protectit.core.registry;

import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.bst.IdentifierData;
import mod.gottsch.forge.protectit.core.registry.bst.Interval;
import mod.gottsch.forge.protectit.core.registry.bst.OwnershipData;
import mod.gottsch.forge.protectit.core.registry.bst.ProtectedIntervalTree;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * 
 * @author Mark Gottschling on Oct 5, 2021
 *
 */
public class BlockProtectionRegistry implements IBlockProtectionRegistry {

	// legacy key/value
	private static final String CLAIMS_KEY = "claims";
	private static final String PROPERTIES_KEY = "properties";
//	private static final String UUID_PROPERTIES_KEY = "propertiesByUuid";

	/*
	 * a map of properties by property coords.
	 * this is the main backing map for the data held in the BST.
	 * this and the BST are meant for the top-level properties.
	 * the min. coords of the property is used as the key.
	 */
	private final Map<ICoords, Property> PROPERTY_BY_COORDS = new HashMap<>();
	/*
	 * TODO change String key to UUID key.
	 * a map of property lists by owner (uuid)
	 */
	private final Map<String, List<Property>> PROPERTY_BY_OWNER = new HashMap<>(); 
	/*
	 * a map of properties by property uuid.
	 * all properties whether owner or landlord should be added here.
	 * ie. the system of record for ALL properties
	 */
	private final Map<UUID, Property> PROPERTY_BY_UUID = new HashMap<>();

	/*
	 * a map of property lists by landlord (uuid)
	 */
	private final Map<UUID, List<Property>> PROPERTY_BY_LANDLORD = new HashMap<>();


	/*
	 * an interval binary search tree (interval-bst) for fast lookups for property access/mutation actions.
	 * this is the used when only the location (coords/blockPos) is known.
	 */
	private final ProtectedIntervalTree tree = new ProtectedIntervalTree();

	// NOTE the interval-bst's are not saved/loaded to nbt data, but rather are re-built on load from the data in the maps.

	@Deprecated
	@Override
	public void addProtection(ICoords coords) {
		addProtection(coords, coords);
	}

	@Deprecated
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
		Property property = new Property(coords1, new Box(coords1, coords2), data);
		addProtection(property);
	}

	/**
	 * 
	 */
	@Override
	public void addProtection(Property property) {
		ProtectIt.LOGGER.debug("adding property protection -> {}", property);

		// TODO ensure the player data is valid


		// add properties by owner
		List<Property> properties = null;
		if (!PROPERTY_BY_OWNER.containsKey(property.getOwner().getUuid())) {
			// create new list entry
			PROPERTY_BY_OWNER.put(property.getOwner().getUuid(), new ArrayList<>());
		}
		properties = PROPERTY_BY_OWNER.get(property.getOwner().getUuid());
		properties.add(property);

		// add properties by coords
		PROPERTY_BY_COORDS.put(property.getBox().getMinCoords(), property);
		PROPERTY_BY_UUID.put(property.getUuid(), property);

		// add to BST
		tree.insert(new Interval(property.getBox().getMinCoords(), property.getBox().getMaxCoords(), new OwnershipData(property.getOwner().getUuid(), property.getOwner().getName())));
		ProtectIt.LOGGER.debug("size of tree -> {}", getProtections(property.getBox().getMinCoords(), property.getBox().getMaxCoords()).size());
	}

	/**
	 * 
	 */
	public void updateOwner(Property property, PlayerData newOwner) {
		List<Property>	properties = new ArrayList<>();
		properties.addAll(property.getChildren());
		properties.addAll(properties.stream().flatMap(p -> p.getChildren().stream()).toList());
		properties.add(property);
		
		// update all properties in hierarchy
		properties.forEach(p -> {
			// remove old owner
			if (PROPERTY_BY_OWNER.containsKey(p.getOwner().getUuid())) {
				PROPERTY_BY_OWNER.get(property.getOwner().getUuid()).remove(p);
			}

			// update owner
			if (p.getOwner().equals(property.getOwner())) {
				p.setOwner(newOwner);
			}
			// update landlord
			if (p.getLandlord() != null && p.getLandlord().equals(property.getOwner())) {
				p.setLandlord(newOwner);
			}
			// update whitelist
			if (p.getWhitelist() != null) {
				p.getWhitelist().clear();
			}
			
			// update property
//			property.setOwner(newOwner);
			if (!PROPERTY_BY_OWNER.containsKey(newOwner.getUuid())) {
				PROPERTY_BY_OWNER.put(newOwner.getUuid(), new ArrayList<>());
			}
			PROPERTY_BY_OWNER.get(newOwner.getUuid()).add(p);
			
			UUID ownerUuid = UUID.fromString(newOwner.getUuid());
			if (!PROPERTY_BY_LANDLORD.containsKey(ownerUuid)) {
				PROPERTY_BY_LANDLORD.put(ownerUuid, new ArrayList<>());
			}
			PROPERTY_BY_LANDLORD.get(ownerUuid).add(p);
		});
	}
	
	/**
	 * 
	 * @param property
	 * @param divisible
	 * @return
	 */

	@Override
	public boolean setSubdivisible(Property property, boolean subdivisible) {
		return setSubdivisible(property.getCoords(), subdivisible);
	}

	@Override
	public boolean setSubdivisible(ICoords coords, boolean subdivisible) {
		Property property = PROPERTY_BY_COORDS.get(coords);
		if (property == null) {
			return false;
		}

		property.setSubdivisible(subdivisible);
		return true;
	}

	/**
	 * 
	 * @param parent
	 * @param property
	 * @return
	 */
	public Optional<Property> addSubdivision(Property parent, Property property) {
		if (parent == null | !parent.isSubdivisible() || property == null) {
			return Optional.empty();
		}

		/*
		 *  update indexes/maps
		 */
		// update the owner
		List<Property> properties;
		if (!PROPERTY_BY_OWNER.containsKey(property.getOwner().getUuid())) {
			// create new list entry
			PROPERTY_BY_OWNER.put(property.getOwner().getUuid(), new ArrayList<>());
		}
		properties = PROPERTY_BY_OWNER.get(property.getOwner().getUuid());
		properties.add(property);

		// update the landlord
		UUID landlordUuid = UUID.fromString(property.getLandlord().getUuid());
		if (!PROPERTY_BY_LANDLORD.containsKey(landlordUuid)) {
			// create new list entry
			PROPERTY_BY_LANDLORD.put(landlordUuid, new ArrayList<>());
		}
		properties = PROPERTY_BY_LANDLORD.get(landlordUuid);
		properties.add(property);

		// update the uuid
		PROPERTY_BY_UUID.put(property.getUuid(), property);

		// add to parent's children list
		if (!parent.getChildren().contains(property)) {
			parent.getChildren().add(property);
		}

		return Optional.of(property);
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

	///////////////////// NEW remove() methods //////////////////
	//	@Override
	//	public void removePropertyById(UUID propertyId) {
	//		//		ProtectIt.LOGGER.debug("in remove protection for c1 -> {}, c2 -> {}, uuid -> {}", coords1, coords2, uuid);
	//		List<Box> protections = getProtections(coords1, coords2);
	//		//		ProtectIt.LOGGER.debug("found protections -> {}", protections);
	//		removeProperties(protections, uuid);
	//		protections.forEach(p -> {
	//			List<Interval> intervals = tree.delete(new Interval(p.getMinCoords(), p.getMaxCoords()), uuid);
	//			//			ProtectIt.LOGGER.debug("removed from tree -> {}", intervals);
	//		});
	//	}
	////////////////////////////////////

	@Override
	public void removeProtection(ICoords coords) {
		removeProtection(coords, coords);
	}

	@Override
	public void removeProtection(ICoords coords1, ICoords coords2) {
		List<Box> protections = getProtections(coords1, coords2);
		List<Property> properties = protections.stream().map(b -> PROPERTY_BY_COORDS.get(b.getMinCoords())).collect(Collectors.toList());

		// remove top-level properties from BST
		protections.forEach(p -> {
			tree.delete(new Interval(p.getMinCoords(), p.getMaxCoords()));
		});

		// add child properties
		properties.addAll(properties.stream().flatMap(p -> p.getChildren().stream()).toList());
		// remove properties from the different maps
		removeProperties(properties);
	}

	@Override
	public void removeProtection(ICoords coords1, ICoords coords2, String uuid) {
		List<Box> protections = getProtections(coords1, coords2);
		List<Property> properties = protections.stream().map(b -> PROPERTY_BY_COORDS.get(b.getMinCoords())).collect(Collectors.toList())
				.stream().filter(p -> p.getOwner().getUuid().equalsIgnoreCase(uuid)).collect(Collectors.toList());

		// remove top-level properties
		protections.forEach(p -> {
			List<Interval> intervals = tree.delete(new Interval(p.getMinCoords(), p.getMaxCoords()), uuid);
		});

		// add all child properties
		properties.addAll(properties.stream().flatMap(p -> p.getChildren().stream()).toList());
		removeProperties(properties);
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

		// get all props by owner, then delete from coords, uuid
		List<Property> properties = PROPERTY_BY_OWNER.get(uuid);
		// this list will contain non-top level properites. filter them out
		// NOTE could contain a null entry after this action
		properties = properties.stream().map(p -> PROPERTY_BY_COORDS.get(p.getCoords())).collect(Collectors.toList());
		// ensure to filter nulls out
		properties.addAll(properties.stream().filter(p -> p != null).flatMap(p -> p.getChildren().stream()).toList());
		// remove all the properties
		removeProperties(properties);
	}

	/**
	 * 
	 * @param protections
	 */
	private void removeProperties(final List<Property> properties) {

		properties.forEach(p -> {
			try {
				if (p.getLandlord() == null || p.getLandlord().equals(Property.EMPTY)) {
					PROPERTY_BY_COORDS.remove(p.getCoords());
				} else {
					PROPERTY_BY_LANDLORD.get(UUID.fromString(p.getLandlord().getUuid())).remove(p);
				}
				if (p.getOwner() != null && PROPERTY_BY_OWNER.containsKey(p.getOwner().getUuid())) {
					PROPERTY_BY_OWNER.get(p.getOwner().getUuid()).remove(p);
				}
				PROPERTY_BY_UUID.remove(p.getUuid());
			} catch(Exception e) {
				ProtectIt.LOGGER.error("error removing property -> ", e);
			}
		});
	}

	/**
	 * Updates to the returned list does not update the underlying registry data.
	 */
	@Override
	public List<Property> getAll() {
		List<Property> properties = new ArrayList<>();
		properties.addAll(PROPERTY_BY_COORDS.values());
		return properties;
	}

	/**
	 * Get all the properties owned by player.
	 * @return A list of properties or an empty list if a property isn't found.
	 */
	@Override
	public List<Property> getProtections(String uuid) {
		List<Property> properties = PROPERTY_BY_OWNER.get(uuid);
		if (properties == null) {
			properties = new ArrayList<>();
		}
		return properties;
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

	/**
	 * is protected against player uuid
	 * @param coords1
	 * @param coords2
	 * @param uuid
	 * @return 
	 * @return
	 */
	@Override
	public boolean isProtectedAgainst(ICoords coords1, String uuid, int permission) {
		// check for top-level protections
		List<Interval> protections = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords1), true);
		if (protections.isEmpty()) {
			return false;
		}
		else {

			// interrogate each interval to determine if the uuid is the owner
			Interval protection = protections.get(0);
			
			// /// TODO turn into a method
			Box box = new Box(coords1);
			int count = 1;
			int THRESHOLD = 3;
			// get the property
			Property property = PROPERTY_BY_COORDS.get(protection.getCoords1());
			while (!property.getChildren().isEmpty() && count < THRESHOLD) {
				for (Property p : property.getChildren()) {
					if (p.intersects(box)) {
						property = p;
						break;
					}
				}
				count++;
			}
			// /// END of turn into method
			
//			for (Interval p : protections) {
				//				ProtectIt.LOGGER.debug("isProtectedAgainst -> {}, protection -> {}", uuid, p);
				// short circuit if owner or no owner
			if (property.getOwner().getUuid().equalsIgnoreCase(uuid) ||
					(property.getOwner().equals(PlayerData.EMPTY) && property.getLandlord().getUuid().equalsIgnoreCase(uuid)) ) {
//				break;
				return false;
			}
			if (StringUtils.isBlank(property.getOwner().getUuid())) {
				return true;
			}
//				if (p.getData().getOwner().getUuid().equalsIgnoreCase(uuid)) {
//					break;
//				}
//				if (StringUtils.isBlank(p.getData().getOwner().getUuid())) {
//					return true;
//				}

				// get the property
//				Property property = PROPERTY_BY_COORDS.get(p.getCoords1());
				// TODO need to examine the property hierarchy to determime the exact property the player is in
				// TODO being the owner of any property should short-circuit
				ProtectIt.LOGGER.debug("isProtectedAgainst.propertiesByCoords -> {}, property -> {}", property.getCoords(), property);

				// short circuit on permission
				if (property.hasPermission(permission)) {
//					break;
					return false;
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
//		}
//		return false;
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
	public Property getPropertyByCoords(ICoords coords) {
		return PROPERTY_BY_COORDS.get(coords);
	}

	@Override
	public Optional<Property> getPropertyByUuid(UUID uuid) {
		return Optional.ofNullable(PROPERTY_BY_UUID.get(uuid));
	}

	@Override
	public List<Property> getPropertiesByOwner(UUID owner) {
		if (PROPERTY_BY_OWNER.containsKey(owner.toString())) {
			return PROPERTY_BY_OWNER.get(owner.toString());
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<Property> getPropertiesByLandlord(UUID landlord) {
		if (PROPERTY_BY_LANDLORD.containsKey(landlord)) {
			return PROPERTY_BY_LANDLORD.get(landlord);
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<Property> findPropertiesBy(Predicate<Property> predicate) {
		List<Property> properties = new ArrayList<>();
		PROPERTY_BY_COORDS.values().forEach(p -> {
			if (predicate.test(p)) {
				properties.add(p);
			}
		});
		return properties;
	}

	@Override 
	public List<Box> find(Predicate<Box> predicate) {
		List<Box> boxes = new ArrayList<>();
		PROPERTY_BY_COORDS.values().forEach(p -> {
			if (predicate.test(p.getBox())) {
				boxes.add(p.getBox());
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
	 * Only PROPERTY_BY_COORDS map is saved. These are the top-level properties and their
	 * children will be saved with them in the hierarchy.
	 * @param nbt
	 * @return
	 */
	public synchronized CompoundTag save(CompoundTag nbt) {
		ProtectIt.LOGGER.debug("saving registry...");

		if (!PROPERTY_BY_COORDS.isEmpty()) {
			// legacy
			ListTag list = new ListTag();
			PROPERTY_BY_COORDS.forEach((coords, property) -> {
				ProtectIt.LOGGER.debug("registry saving property -> {}", property);
				CompoundTag propertyNbt = new CompoundTag();
				property.save(propertyNbt);
				list.add(propertyNbt);
			});
			nbt.put(PROPERTIES_KEY, list);
			
			// save by uuid
//			ListTag uuidList = new ListTag();
//			PROPERTY_BY_UUID.forEach((uuid, property) -> {
//				ProtectIt.LOGGER.debug("registry saving uuid properties -> {}", property);
//				CompoundTag propertyNbt = new CompoundTag();
//				property.save(propertyNbt);
//				list.add(propertyNbt);				
//			});
//			nbt.put(UUID_PROPERTIES_KEY, uuidList);
		}
		return nbt;
	}

	/**
	 * Properties are loaded from the top-level. All other properties will be
	 * loaded and registered based on their hierarchy.
	 */
	public synchronized void load(CompoundTag tag) {
		ProtectIt.LOGGER.debug("loading registry...");
		clear();

		//		tree.load(nbt);
		ListTag list = null;

		if (tag.contains(PROPERTIES_KEY)) {
			list = tag.getList(PROPERTIES_KEY, 10);
		}
		// legacy check
		else if (tag.contains(CLAIMS_KEY)) {
			list = tag.getList(CLAIMS_KEY, 10);
			tag.remove(CLAIMS_KEY);
		}

		if (list != null) {
			list.forEach(element -> {
				//			for (CompoundTag compound : list.listIterator().
				Property property = new Property().load((CompoundTag)element);
				ProtectIt.LOGGER.debug("loaded property -> {}", property);
				PROPERTY_BY_COORDS.put(property.getCoords(), property);

				if (!PROPERTY_BY_OWNER.containsKey(property.getOwner().getUuid())) {
					// create new list entry
					PROPERTY_BY_OWNER.put(property.getOwner().getUuid(), new ArrayList<>());
				}
				PROPERTY_BY_OWNER.get(property.getOwner().getUuid()).add(property);
				PROPERTY_BY_UUID.put(property.getUuid(), property);

				ProtectIt.LOGGER.debug("property BEFORE inserting into tree -> {}", PROPERTY_BY_COORDS.get(property.getCoords()));
				tree.insert(new Interval(property.getBox().getMinCoords(), property.getBox().getMaxCoords(), new OwnershipData(property.getOwner().getUuid(), property.getOwner().getName())));

				// TODO now walk the children and update BY_UUID, BY_OWNER, BY_LANDLORD
				property.getChildren().forEach(child -> {
					addSubdivision(property, child);
				});

				ProtectIt.LOGGER.debug("property AFTER inserting into tree -> {}", PROPERTY_BY_COORDS.get(property.getCoords()));
				ProtectIt.LOGGER.debug("running loaded propertys_by_coords -> {}", PROPERTY_BY_COORDS);
			});
			//			ProtectIt.LOGGER.debug("0.all loaded properties_by_coords -> {}", CLAIMS_BY_COORDS);
		}
		
		// TODO load by landlord
		
		// print the loaded property again
		ProtectIt.LOGGER.debug("1. all loaded propertiess_by_coords -> {}", PROPERTY_BY_COORDS);
		ProtectIt.LOGGER.debug("all loaded in tree -> {}", tree.toStringList(tree.getRoot()));
	}

	/**
	 * 
	 */
	public void clear() {
		PROPERTY_BY_OWNER.clear();
		PROPERTY_BY_COORDS.clear();
		PROPERTY_BY_LANDLORD.clear();
		PROPERTY_BY_UUID.clear();
		tree.clear();
	}

	/**
	 * TODO, should dump every 5 minutes if the registry has changed
	 */
	@Override
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
			Type typeOfSrc = new TypeToken<Map<ICoords, Property>>(){}.getType();
			Type landlordMapType = new TypeToken<Map<UUID, Property>>() {}.getType();
			
			Gson gson = gsonBuilder.setPrettyPrinting().create();
			String byCoords = gson.toJson(PROPERTY_BY_COORDS, typeOfSrc);
			FileWriter fw = new FileWriter(path.resolve("protection-registries-by-coords.json").toAbsolutePath().toString());
			fw.write(byCoords);
			fw.close();
			
			// save by landlord
//			String byLandlord = gson.toJson(PROPERTY_BY_LANDLORD, landlordMapType);
//			fw = new FileWriter(path.resolve("protection-registries-by-landlord.json").toAbsolutePath().toString());
//			fw.write(byLandlord);
//			fw.close();
//			
//			// by uuid
//			String uuidJson = gson.toJson(PROPERTY_BY_UUID, landlordMapType);
//			fw = new FileWriter(path.resolve("protection-registries-by-uuid.json").toAbsolutePath().toString());
//			fw.write(uuidJson);
//			fw.close();
		} catch (Exception e) {
			ProtectIt.LOGGER.error("error writing protection registry to file -> ", e);
		}
	}
}
