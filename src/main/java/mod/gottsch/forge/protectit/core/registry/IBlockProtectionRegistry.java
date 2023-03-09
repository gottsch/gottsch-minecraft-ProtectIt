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

/**
 * Protection Registry more specific to land claim ownership / block protections ie can't alter blocks in area unless you are the owner.
 * @author Mark Gottschling on Oct 5, 2021
 *
 */
public interface IBlockProtectionRegistry extends IProtectionRegistry {

	public void addProtection(ICoords coords, PlayerData data);
	public void addProtection(ICoords coords1, ICoords coords2, PlayerData data);

	public void removeProtection(ICoords coords1, ICoords coords2, String uuid);
	public void removeProtection(String uuid);

	boolean isProtectedAgainst(ICoords coords, String uuid, int permission);
	boolean isProtectedAgainst(ICoords coords1, ICoords coords2, String uuid, int permission);
	
	// TODO this replaces old PlayerData methods
	public void addProtection(Property property);
	public List<Property> getAll();
	public List<Property> getProtections(String uuid);
	public List<Property> findPropertiesBy(Predicate<Property> predicate);
	public Property getPropertyByCoords(ICoords coords);
	
//	public Optional<Property> addRoom(Property hotel, Box box, PlayerData playerData);
//	Optional<Property> addHotel(Property parentProperty, Box box, PlayerData owner);

	boolean setSubdivisible(Property property, boolean isHotel);
	boolean setSubdivisible(ICoords coords, boolean isHotel);
	
	
//	public List<Property> getHotels(Property property);
	public Optional<Property> addSubdivision(Property target, Property property);
	Optional<Property> getPropertyByUuid(UUID uuid);
	List<Property> getPropertiesByOwner(UUID owner);
	public void updateOwner(Property property, PlayerData owner);
	

}
