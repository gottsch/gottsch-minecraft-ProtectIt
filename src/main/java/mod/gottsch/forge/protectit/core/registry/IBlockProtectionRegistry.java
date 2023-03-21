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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.property.Property;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Protection Registry more specific to land claim ownership / block protections ie can't alter blocks in area unless you are the owner.
 * @author Mark Gottschling on Oct 5, 2021
 *
 */
public interface IBlockProtectionRegistry {
//
//	public void addProtection(ICoords coords, PlayerIdentity data);
//	public void addProtection(ICoords coords1, ICoords coords2, PlayerIdentity data);
//
//	public void removeProtection(ICoords coords1, ICoords coords2, String uuid);
//	public void removeProtection(String uuid);

//	boolean isProtectedAgainst(ICoords coords, String uuid, int permission);

	public void addProperty(Property property);
	
//	public List<Property> getAll();
//	public List<Property> getProtections(String uuid);
//	public List<Property> findPropertiesBy(Predicate<Property> predicate);
	public List<Property> getPropertyByCoords(ICoords coords);


	Optional<Property> getPropertyByUuid(UUID uuid);
	List<Property> getPropertiesByOwner(UUID owner);
	List<Property> getPropertiesByLord(UUID landlord);
	public void updateOwner(Property property, PlayerIdentity owner);
	public void dump();

	void removeProperty(ICoords coords);
	void removeProperties(ICoords coords1, ICoords coords2);
	public void removeProperty(Property property);

	boolean isProtectedAgainst(ICoords coords1, UUID uuid, int permission);

	boolean setFiefdom(Property property, boolean fiefdom);
	boolean setFiefdom(ICoords coords, boolean fiefdom);
	Optional<Property> addFief(Property parent, Property property);
	
	/**
	 * determines if the point at coords is a protected region
	 * @param coords
	 * @return
	 */
	public boolean isProtected(ICoords coords);
	public boolean isProtected(ICoords coords1, ICoords coords2);
	public boolean isProtected(ICoords coords1, ICoords coords2, boolean includeBorders);
	
	/**
	 * get a list of all protections that intersect with the coords area
	 * @param coords
	 * @return
	 */
	public List<Box> getProtections(ICoords coords);
	public List<Box> getProtections(ICoords coords1, ICoords coords2);
	public List<Box> getProtections(ICoords coords1, ICoords coords2, boolean findFast);
	public List<Box> getProtections(ICoords coords1, ICoords coords2, boolean findFast, boolean includeBorder);

	List<Property> getAllPropertiesByUuid(List<UUID> uuids);

	public void load(CompoundTag compound);
	public Tag save(CompoundTag compoundTag);

	public void clear();

	/**
	 * Updates to the returned list does not update the underlying registry data.
	 */
	List<Property> getAll();

	


}
