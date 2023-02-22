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
package com.someguyssoftware.protectit.network;

import java.util.UUID;

import com.someguyssoftware.protectit.ProtectIt;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.network.FriendlyByteBuf;


/**
 * 
 * @author Mark Gottschling on Dec 9, 2021
 *
 */
public class PropertyLeverMessageToClient implements ICoordsHandler {

	private boolean valid;
	public ICoords coords;
	public ICoords propertyCoords;
	public UUID propertyUuid;

	/**
	 * 
	 * @param builder
	 */
	public PropertyLeverMessageToClient(ICoords coords, ICoords propertyCoords, UUID propertyUuid) {
		setCoords(coords);
		setPropertyCoords(propertyCoords);
		setPropertyUuid(propertyUuid);
		setValid(true);
	}

	/**
	 * 
	 */
	public PropertyLeverMessageToClient() {
		setValid(false);
	}

	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		if (!isValid()) {
			return;
		}

		if (getCoords() == null) {
			writeCoords(Coords.EMPTY, buf);
		}
		else {
			writeCoords(getCoords(), buf);
		}
		
		if (getPropertyCoords() == null) {
			writeCoords(Coords.EMPTY, buf);
		}
		else {
			writeCoords(getPropertyCoords(), buf);
		}
		
		if (getPropertyUuid() == null) {
			buf.writeUtf("NULL");
		} else {
			buf.writeUtf(getPropertyUuid().toString());
		}

	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static PropertyLeverMessageToClient decode(FriendlyByteBuf buf) {
		PropertyLeverMessageToClient message;
		
		try {
			ICoords coords = ICoordsHandler.readCoords(buf);
			ICoords propertyCoords = ICoordsHandler.readCoords(buf);
			String uuidStr = buf.readUtf();
			UUID uuid = null;
//			new UUID(0L, 0L);
			if (!uuidStr.equalsIgnoreCase("NULL")) {
				uuid = UUID.fromString(uuidStr);
			}
			message = new PropertyLeverMessageToClient(coords, propertyCoords, uuid);
			message.setValid(true);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred attempting to read message: ", e);
			message = new PropertyLeverMessageToClient();
		}
		return message;
	}

	public ICoords getCoords() {
		return coords;
	}

	public void setCoords(ICoords coords) {
		this.coords = coords;
	}

	@Override
	public String toString() {
		return "PropertyLeverMessageToClient [valid=" + valid + ", coords=" + coords + ", propertyCoords="
				+ propertyCoords + ", propertyUuid=" + propertyUuid + "]";
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public ICoords getPropertyCoords() {
		return propertyCoords;
	}

	public void setPropertyCoords(ICoords claimCoords) {
		this.propertyCoords = claimCoords;
	}

	public UUID getPropertyUuid() {
		return propertyUuid;
	}

	public void setPropertyUuid(UUID propertyUuid) {
		this.propertyUuid = propertyUuid;
	}
}
