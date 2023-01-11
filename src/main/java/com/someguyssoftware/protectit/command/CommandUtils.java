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
package com.someguyssoftware.protectit.command;

import java.util.Optional;

import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.util.Tuple;

/**
 * 
 * @author Mark Gottschling on Oct 7, 2021
 *
 */
public class CommandUtils {
	
	public static Optional<Tuple<ICoords, ICoords>> validateCoords(ICoords c1, ICoords c2) {
		Optional<Tuple<ICoords, ICoords>> coords = Optional.of (new Tuple<ICoords, ICoords>(c1, c2));
		if (!isDownField(c1, c2)) {
			// attempt to flip coords and test again
			if (isDownField(c2, c1)) {
				coords = Optional.of(new Tuple<ICoords, ICoords>(c2, c1));
			}
			else {
				coords = Optional.empty();
			}
		}
		return coords;
	}

	/**
	 * TODO When updating to allow Y values, update this method to include Y check
	 * @param from
	 * @param to
	 * @return
	 */
	public static boolean isDownField(ICoords from, ICoords to) {
		if (to.getX() >= from.getX() && to.getZ() >= from.getZ()) {
			return true;
		}
		return false;
	}
}