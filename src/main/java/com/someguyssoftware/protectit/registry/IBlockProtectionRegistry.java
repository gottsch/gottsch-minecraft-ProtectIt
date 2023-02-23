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
package com.someguyssoftware.protectit.registry;

import java.util.List;
import java.util.function.Predicate;

import com.someguyssoftware.protectit.core.property.Property;

import mod.gottsch.forge.gottschcore.spatial.ICoords;

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
	public List<Property> findByClaim(Predicate<Property> predicate);
	public Property getClaimByCoords(ICoords coords);

}
