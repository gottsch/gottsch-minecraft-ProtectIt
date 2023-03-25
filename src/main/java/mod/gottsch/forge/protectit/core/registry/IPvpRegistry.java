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
import java.util.UUID;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.zone.Zone;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 *  * Protection Registry more specific to PVP play. The protected areas are safe-zones.
 * @author Mark Gottschling on Nov 6, 2021
 *
 */
public interface IPvpRegistry {

	boolean isProtected(ICoords a, ICoords b);

	void addZone(Zone zone);
	void removeZone(Zone zone);
	void removeZone(UUID uuid, ICoords coords);
	
	Tag save(CompoundTag compoundTag);
	void load(CompoundTag compound);

	List<Box> getProtections(ICoords coords, ICoords add, boolean findFast, boolean includeBorder);
	List<Zone> getZoneByCoords(ICoords minCoords);

	int size();
	void clear();
	List<Zone> getAll();

	void dump();

	void changePermission(UUID zone, Box box, int permission, boolean value);

	/**
	 * is protected against a permission
	 * @param coords
	 * @param coords2
	 * @param uuid
	 * @return 
	 * @return
	 */
	boolean isProtectedAgainst(ICoords coords, int permission);

	

	

}
