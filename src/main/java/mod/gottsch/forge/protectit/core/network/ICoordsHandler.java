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
package mod.gottsch.forge.protectit.core.network;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 
 * @author Mark Gottschling on Dec 9, 2021
 *
 */
@Deprecated
public interface ICoordsHandler {

	default public void writeCoords(ICoords coords, FriendlyByteBuf buf) {
		if (coords != null) {
			buf.writeInt(coords.getX());
			buf.writeInt(coords.getY());
			buf.writeInt(coords.getZ());
		}
	}

	public static ICoords readCoords(FriendlyByteBuf buf) {
		ICoords coords = new Coords(buf.readInt(), buf.readInt(), buf.readInt());
		return coords;
	}
	
}
