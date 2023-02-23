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
package com.someguyssoftware.protectit.core.property;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;

/**
 * 
 * @author Mark Gottschling on Oct 30, 2021
 *
 */
public class PropertySizes {

	public static final ICoords SMALL_CLAIM_SIZE = new Coords(8, 16, 8);
	public static final ICoords MEDIUM_CLAIM_SIZE = new Coords(16, 32, 16);
	public static final ICoords LARGE_CLAIM_SIZE = new Coords(32, 64, 32);

}
