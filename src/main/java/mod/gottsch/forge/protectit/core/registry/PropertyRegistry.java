/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.property.PropertyUtil;
import mod.gottsch.forge.protectit.core.registry.bst.Interval;
import mod.gottsch.forge.protectit.core.registry.bst.ProtectedIntervalTree;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * 
 * @author Mark Gottschling Mar 19, 2023
 *
 */
public class PropertyRegistry implements IBlockProtectionRegistry {
	private static final String PROPERTIES_KEY = "properties";
	
	// NOTE all properties should added to all maps
	
	/*
	 * a multimap of properties by property coords.
	 * this is the main backing map for the data held in the BST.
	 * the min. coords of the property is used as the key.
	 */
	private final Multimap<ICoords, Property> PROPERTY_BY_COORDS = ArrayListMultimap.create();

	/*
	 * a map of property lists by lord (uuid)
	 */
	private final Multimap<UUID, Property> PROPERTY_BY_LORD = ArrayListMultimap.create();

	/*
	 * a map of property lists by owner (uuid)
	 */
	private final Multimap<UUID,Property> PROPERTY_BY_OWNER = ArrayListMultimap.create();

	/*
	 * a map of properties by property uuid.
	 */
	private final Map<UUID, Property> PROPERTY_BY_UUID = Maps.newHashMap();

	/*
	 * an interval binary search tree (interval-bst) for fast lookups for property access/mutation actions.
	 * this is the used when only the location (coords/blockPos) is known.
	 */
	private final ProtectedIntervalTree tree = new ProtectedIntervalTree();

	/**
	 * 
	 */
	@Override
	public void addProperty(Property property) {
		ProtectIt.LOGGER.debug("adding property protection -> {}", property);

		// add properties by owner
//		List<Property> properties = null;
//		if (!PROPERTY_BY_OWNER.containsKey(property.getOwner().getUuid())) {
//			// create new list entry
//			PROPERTY_BY_OWNER.put(property.getOwner().getUuid(), new ArrayList<>());
//		}
//		properties = PROPERTY_BY_OWNER.get(property.getOwner().getUuid());
//		properties.add(property);
		PROPERTY_BY_OWNER.put(property.getOwner().getUuid(), property);
		PROPERTY_BY_LORD.put(property.getLord().getUuid(), property);
		PROPERTY_BY_COORDS.put(property.getBox().getMinCoords(), property);
		PROPERTY_BY_UUID.put(property.getUuid(), property);

		// add to BST
		tree.insert(new Interval(property.getBox().getMinCoords(), property.getBox().getMaxCoords()));
		ProtectIt.LOGGER.debug("size of tree -> {}", getProtections(property.getBox().getMinCoords(), property.getBox().getMaxCoords()).size());
	}
	
	/**
	 * NOTE getProtections only return the Box objects from the BST
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
	 * Updates to the returned list does not update the underlying registry data.
	 */
	@Override
	public List<Property> getAll() {
		List<Property> properties = new ArrayList<>();
		properties.addAll(PROPERTY_BY_COORDS.values());
		return properties;
	}
	
	@Override
	public void removeProperty(ICoords coords) {
		removeProperties(coords, coords);
	}

	public void removeProperty(Property property) {
		// need to remove all children recursively of this property as well
		List<Property> properties = PropertyUtil.getPropertyHierarchy(Arrays.asList(property));
		
		// remove top-level properties from BST
		properties.forEach(p -> {
			tree.delete(new Interval(p.getBox().getMinCoords(), p.getBox().getMaxCoords()));
		});
		// remove properties from the different maps
		removeProperties(properties);
	}
	
	@Override
	public void removeProperties(ICoords coords1, ICoords coords2) {
		List<Box> protections = getProtections(coords1, coords2);
		List<Property> properties = protections.stream().flatMap(p -> PROPERTY_BY_COORDS.get(p.getMinCoords()).stream()).toList();
		
		// need to remove all children recursively of this property as well
		properties = PropertyUtil.getPropertyHierarchy(properties);
		
		// remove top-level properties from BST
		properties.forEach(p -> {
			tree.delete(new Interval(p.getBox().getMinCoords(), p.getBox().getMaxCoords()));
		});
		// remove properties from the different maps
		removeProperties(properties);
	}
	
	/**
	 * 
	 * @param protections
	 */
	private void removeProperties(final List<Property> properties) {

		properties.forEach(p -> {
			try {
				PROPERTY_BY_COORDS.removeAll(p.getBox().getMinCoords());
				PROPERTY_BY_LORD.get(p.getLord().getUuid()).remove(p);
				PROPERTY_BY_OWNER.get(p.getOwner().getUuid()).remove(p);
				PROPERTY_BY_UUID.remove(p.getUuid());
			} catch(Exception e) {
				ProtectIt.LOGGER.error("error removing property -> ", e);
			}
		});
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

	// TODO this has to be redone
	/**
	 * is protected against player
	 * @param coords1
	 * @param coords2
	 * @param uuid
	 * @return 
	 * @return
	 */
	@Override
	public boolean isProtectedAgainst(ICoords coords1, UUID uuid, int permission) {
		// check for top-level protections
		List<Interval> protections = tree.getOverlapping(tree.getRoot(), new Interval(coords1, coords1), true, false);
		if (protections.isEmpty()) {
			return false;
		}
		else {
			// get first as there should only be one since using findFast option in search
			Interval protection = protections.get(0);
			ProtectIt.LOGGER.debug("isProtectedAgainst interval.coords -> {}, interval -> {}", coords1, protection);
			
			// get the properties by coord
			List<Property> properties = (List<Property>) PROPERTY_BY_COORDS.get(protection.getCoords1());
			ProtectIt.LOGGER.debug("isProtectedAgainst properties -> {}", properties);
//			if (!properties.getChildren().isEmpty()) {
//				Box box = new Box(coords1);
//				List<Property> properties = PropertyUtil.getPropertyHierarchy(Arrays.asList(properties));
//				Optional<Property> target = properties.stream().filter(p -> p.intersects(box)).findFirst();
//				if (target.isPresent()) {
//					properties = target.get();
//				}
//			}
			Property property;
			Optional<Property> target = PropertyUtil.getLeastSignificant(properties);
			if (target.isPresent()) {
				property = target.get();
			} else {
				return true;
			}
			ProtectIt.LOGGER.debug("isProtectedAgainst property -> {}", properties);
			ProtectIt.LOGGER.debug("isProtectedAgainst uuid -> {}", uuid);
			
			// short circuit if owner or no owner
			if (property.getOwner().getUuid().equals(uuid) ||
					(property.getOwner().equals(PlayerIdentity.EMPTY) && property.getLord().getUuid().equals(uuid)) ) {
				//				break;
				return false;
			}

			// get the property
			//				Property property = PROPERTY_BY_COORDS.get(p.getCoords1());
			// TODO being the owner of any property should short-circuit
//			ProtectIt.LOGGER.debug("isProtectedAgainst.propertiesByCoords -> {}, property -> {}", property.getCoords(), property);

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
				for (PlayerIdentity id : property.getWhitelist()) {
					ProtectIt.LOGGER.debug("isProtectedAgainst compare whitelist id -> {} to uuid -> {}", id.getUuid(), uuid);
					if (id.getUuid().equals(uuid)) {
						//							isWhitelist = true;
						//							break;
						// TODO this is bad. this will only check the first property
						return false;
					}
				}
			}
			return true;
		}
	}
	
	// TODO redo
	/**
	 * 
	 */
	public void updateOwner(Property property, PlayerIdentity newOwner) {
		List<Property>	properties = new ArrayList<>();
		properties.addAll(getAllPropertiesByUuid(property.getChildren()));
		properties.addAll(properties.stream().flatMap(p -> getAllPropertiesByUuid(p.getChildren()).stream()).toList());
		properties.add(property);

		// update all properties in hierarchy
		properties.forEach(p -> {
			// remove old owner
			if (PROPERTY_BY_OWNER.containsKey(property.getOwner().getUuid())) {
				PROPERTY_BY_OWNER.remove(property.getOwner().getUuid(), p);
			}

			// update owner
			if (p.getOwner().equals(property.getOwner())) {
				p.setOwner(newOwner);
			}
			// update lord
			if (p.getLord() != null && p.getLord().equals(property.getOwner())) {
				// remove from lord list
				PROPERTY_BY_LORD.remove(p.getLord().getUuid(), p);
				p.setLord(newOwner);
				PROPERTY_BY_LORD.put(newOwner.getUuid(), p);
			}
			// update whitelist
			if (p.getWhitelist() != null) {
				p.getWhitelist().clear();
			}

			// update property
			//			property.setOwner(newOwner);
//			if (!PROPERTY_BY_OWNER.containsKey(newOwner.getUuid())) {
//				PROPERTY_BY_OWNER.put(newOwner.getUuid(), new ArrayList<>());
//			}
//			PROPERTY_BY_OWNER.get(newOwner.getUuid()).add(p);
			PROPERTY_BY_OWNER.put(newOwner.getUuid(), p);

			// NOTE only the owner is being updated. the property should already be present in the PROPERTY_BY_UUID
			
//			UUID ownerUuid = UUID.fromString(newOwner.getUuid());
//			if (!PROPERTY_BY_LORD.containsKey(ownerUuid)) {
//				PROPERTY_BY_LORD.put(ownerUuid, new ArrayList<>());
//			}
//			PROPERTY_BY_LORD.get(ownerUuid).add(p);
		});
	}
	
	
	@Override
	public boolean setFiefdom(Property property, boolean fiefdom) {
		property.setFiefdom(fiefdom);
		return true;
	}

	@Override
	public boolean setFiefdom(ICoords coords, boolean fiefdom) {
		//  get all properties by coords
		List<Property> properties = (List<Property>) PROPERTY_BY_COORDS.get(coords);
		if (properties == null || properties.isEmpty()) {
			return false;
		}
		Optional<Property> property = PropertyUtil.getLeastSignificant(properties);
		if (property.isEmpty()) {
			return false;
		}

		return setFiefdom(property.get(), fiefdom);
	}
	
	/**
	 * 
	 * @param parent
	 * @param property
	 * @return
	 */
	@Override
	public Optional<Property> addFief(Property parent, Property property) {
		if (parent == null | !parent.isFiefdom() || property == null) {
			ProtectIt.LOGGER.debug("fief not added due to validation.");
			return Optional.empty();
		}

		/*
		 *  update indexes/maps
		 */
		// update the owner
//		List<Property> properties;
//		if (!PROPERTY_BY_OWNER.containsKey(property.getOwner().getUuid())) {
//			// create new list entry
//			PROPERTY_BY_OWNER.put(property.getOwner().getUuid(), new ArrayList<>());
//		}
//		properties = PROPERTY_BY_OWNER.get(property.getOwner().getUuid());
//		properties.add(property);
		

		// update the landlord
//		UUID landlordUuid = UUID.fromString(property.getLord().getUuid());
//		if (!PROPERTY_BY_LANDLORD.containsKey(landlordUuid)) {
//			// create new list entry
//			PROPERTY_BY_LANDLORD.put(landlordUuid, new ArrayList<>());
//		}
//		properties = PROPERTY_BY_LANDLORD.get(landlordUuid);
//		properties.add(property);
//
//		// update the uuid
//		PROPERTY_BY_UUID.put(property.getUuid(), property);

		addProperty(property);
		
		// add to parent's children list
		if (!parent.getChildren().contains(property.getUuid())) {
			parent.getChildren().add(property.getUuid());
		}

		return Optional.of(property);
	}
	
	/**
	 * 
	 * @param tag
	 * @return
	 */
	public synchronized CompoundTag save(CompoundTag tag) {
		ProtectIt.LOGGER.debug("saving registry...");

		if (!PROPERTY_BY_COORDS.isEmpty()) {
			// legacy
			ListTag list = new ListTag();
			PROPERTY_BY_COORDS.forEach((coords, property) -> {
				ProtectIt.LOGGER.debug("registry saving property -> {}", property);
				CompoundTag propertyTag = new CompoundTag();
				property.save(propertyTag);
				list.add(propertyTag);
			});
			tag.put(PROPERTIES_KEY, list);

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

		if (tag.contains(PROPERTIES_KEY)) {
			ListTag list = tag.getList(PROPERTIES_KEY, 10);
			list.forEach(element -> {
				//			for (CompoundTag compound : list.listIterator().
				Property property = new Property().load((CompoundTag)element);
				ProtectIt.LOGGER.debug("loaded property -> {}", property);
				PROPERTY_BY_COORDS.put(property.getBox().getMinCoords(), property);

//				if (!PROPERTY_BY_OWNER.containsKey(property.getOwner().getUuid())) {
//					// create new list entry
//					PROPERTY_BY_OWNER.put(property.getOwner().getUuid(), new ArrayList<>());
//				}
//				PROPERTY_BY_OWNER.get(property.getOwner().getUuid()).add(property);
				PROPERTY_BY_OWNER.put(property.getOwner().getUuid(), property);
				PROPERTY_BY_LORD.put(property.getLord().getUuid(), property);
				PROPERTY_BY_UUID.put(property.getUuid(), property);

				ProtectIt.LOGGER.debug("property BEFORE inserting into tree -> {}", PROPERTY_BY_COORDS.get(property.getBox().getMinCoords()));
				tree.insert(new Interval(property.getBox().getMinCoords(), property.getBox().getMaxCoords()));

				// TODO now walk the children and update BY_UUID, BY_OWNER, BY_LANDLORD
//				property.getChildren().forEach(child -> {
//					addSubdivision(property, child);
//				});

				ProtectIt.LOGGER.debug("property AFTER inserting into tree -> {}", PROPERTY_BY_COORDS.get(property.getBox().getMinCoords()));
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
		PROPERTY_BY_LORD.clear();
		PROPERTY_BY_UUID.clear();
		tree.clear();
	}
	
	@Override
	public List<Property> getPropertyByCoords(ICoords coords) {
		return (List<Property>) PROPERTY_BY_COORDS.get(coords);
	}

	@Override
	public Optional<Property> getPropertyByUuid(UUID uuid) {
		return Optional.ofNullable(PROPERTY_BY_UUID.get(uuid));
	}

	@Override
	public List<Property> getAllPropertiesByUuid(List<UUID> uuids) {
		List<Property> properties = new ArrayList<>();
		uuids.forEach(id -> {
			Optional<Property> p = getPropertyByUuid(id);
			if (p.isPresent()) {
				properties.add(p.get());
			}
		});
		return properties;
	}
	
	@Override
	public List<Property> getPropertiesByOwner(UUID owner) {
		if (PROPERTY_BY_OWNER.containsKey(owner)) {
			return (List<Property>) PROPERTY_BY_OWNER.get(owner);
		}
		return new ArrayList<>();
	}

	@Override
	public List<Property> getPropertiesByLord(UUID landlord) {
		if (PROPERTY_BY_LORD.containsKey(landlord)) {
			return (List<Property>) PROPERTY_BY_LORD.get(landlord);
		}
		return new ArrayList<>();
	}

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
			Type typeOfSrc = new TypeToken<ArrayListMultimap<ICoords, Property>>(){}.getType();
			Type listType = new TypeToken<List<Property>>() {}.getType();
			Gson gson = gsonBuilder.setPrettyPrinting().create();
			
			// convert map to list
			List<Property> list = new ArrayList<>();
			PROPERTY_BY_COORDS.forEach((coords, property) -> {
				list.add(property);
			});
//			String byCoords = gson.toJson(PROPERTY_BY_COORDS, typeOfSrc);
			String json = gson.toJson(list, listType);
			FileWriter fw = new FileWriter(path.resolve("protection-registries.json").toAbsolutePath().toString());
//			fw.write(byCoords);
			fw.write(json);
			fw.close();

		} catch (Exception e) {
			ProtectIt.LOGGER.error("error writing protection registry to file -> ", e);
		}
	}
}