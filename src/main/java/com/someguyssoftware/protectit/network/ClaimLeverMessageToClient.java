/*
 * This file is part of  Protect It.
 * Copyright (c) 2021, Mark Gottschling (gottsch)
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

import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.network.PacketBuffer;

/**
 * 
 * @author Mark Gottschling on Dec 9, 2021
 *
 */
public class ClaimLeverMessageToClient implements ICoordsHandler {
	public static final ICoords EMPTY_COORDS = new Coords(0, -255, 0);

	private boolean valid;
	public ICoords coords;
	public ICoords claimCoords;

	/**
	 * 
	 * @param builder
	 */
	public ClaimLeverMessageToClient(ICoords coords, ICoords claimCoords) {
		setCoords(coords);
		setClaimCoords(claimCoords);
		setValid(true);
	}

	/**
	 * 
	 */
	public ClaimLeverMessageToClient() {
		setValid(false);
	}

	/**
	 * 
	 * @param buf
	 */
	public void encode(PacketBuffer buf) {
		if (!isValid()) {
			return;
		}

		if (getCoords() == null) {
			writeCoords(EMPTY_COORDS, buf);
		}
		else {
			writeCoords(getCoords(), buf);
		}
		
		if (getClaimCoords() == null) {
			writeCoords(EMPTY_COORDS, buf);
		}
		else {
			writeCoords(getClaimCoords(), buf);
		}

	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static ClaimLeverMessageToClient decode(PacketBuffer buf) {
		ClaimLeverMessageToClient message;
		
		try {
			ICoords coords = ICoordsHandler.readCoords(buf);
			ICoords claimCoords = ICoordsHandler.readCoords(buf);
			message = new ClaimLeverMessageToClient(coords, claimCoords);
			message.setValid(true);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred attempting to read message: ", e);
			message = new ClaimLeverMessageToClient();
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
		return "ClaimLeverMessageToClient [valid=" + valid + ", coords=" + coords + ", claimCoords=" + claimCoords
				+ "]";
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public ICoords getClaimCoords() {
		return claimCoords;
	}

	public void setClaimCoords(ICoords claimCoords) {
		this.claimCoords = claimCoords;
	}
}
