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
package mod.gottsch.forge.protectit.core.zone;

import java.util.List;
import java.util.Optional;

import mod.gottsch.forge.gottschcore.spatial.ICoords;

/**
 * 
 * @author Mark Gottschling Mar 21, 2023
 *
 */
public class ZoneUtil {

	public static Optional<Zone> getLeastSignificant(List<Zone> zones) {
		/*
		 * NOTE this list of zones may or may not contain any nested zones, or only some (ie middle).
		 */
		if (zones.size() == 1) {
			return Optional.of(zones.get(0));
		}
		
		// return the smallest sized zone
		Zone selected = zones.get(0);
		for (Zone zone : zones) {
			if (sizeOf(zone) < sizeOf(selected)) {
				selected = zone;
			}
		}
		return Optional.ofNullable(selected);
	}
	
	public static int sizeOf(Zone zone) {
		ICoords size = zone.getBox().getSize();
		return size.getX() * size.getY() * size.getZ();
	}
}
