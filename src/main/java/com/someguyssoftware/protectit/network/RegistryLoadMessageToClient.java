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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.registry.PlayerData;

import net.minecraft.network.PacketBuffer;

/**
 *
 * @author Mark Gottschling on Oct 14, 2021
 *
 */
public class RegistryLoadMessageToClient {
	public static final String NULL_UUID = "NULL";
	public static final String NULL_NAME = "NULL";
	public static final ICoords EMPTY_COORDS = new Coords(0, -255, 0);
	
	private boolean valid;
	private String type;
	private int size;
//	@Deprecated
//	private List<Interval> intervals;
	private List<Claim> claims;
	
	public RegistryLoadMessageToClient() {
		valid = false;
	}
	
//	public RegistryLoadMessageToClient(String type, List<Interval> intervals) {
//		this.valid = true;
//		this.type = type;
//		this.size = intervals.size();
//		this.intervals = intervals;
//	}

	public RegistryLoadMessageToClient(String type, List<Claim> claims) {
		this.valid = true;
		this.type = type;
		this.size = claims.size();
		this.claims = claims;
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(PacketBuffer buf) {
		if (!isValid()) {
			return;
		}
		// buf.writeUtf(StringUtils.defaultString(type, ""));
		// buf.writeInt(intervals.size());
		
		// intervals.forEach(interval -> {
		// 	if (interval.getCoords1() == null) {
		// 		writeCoords(EMPTY_COORDS, buf);
		// 	}
		// 	else {
		// 		writeCoords(interval.getCoords1(), buf);
		// 	}
		// 	if (interval.getCoords2() == null) {
		// 		writeCoords(EMPTY_COORDS, buf);
		// 	}
		// 	else {
		// 		writeCoords(interval.getCoords2(), buf);
		// 	}
		// 	buf.writeUtf(StringUtils.defaultString(interval.getData().getOwner().getUuid(), NULL_UUID));
		// 	buf.writeUtf(StringUtils.defaultString(interval.getData().getOwner().getName(), ""));

		// });

		claims.forEach(claim -> {
			writeClaim(buf, claim);
		});
	}

	private void writeClaim(PacketBuffer buf, Claim claim) {
		buf.writeUtf(StringUtils.defaultString(claim.getOwner().getUuid(), NULL_UUID));
		buf.writeUtf(StringUtils.defaultString(claim.getOwner().getName(), ""));

		if (claim.getCoords() == null) {
				writeCoords(EMPTY_COORDS, buf);
			}
		else {
			writeCoords(claim.getCoords(), buf);
		}

		if (claim.getBox() == null) {
			writeCoords(EMPTY_COORDS, buf);
			writeCoords(EMPTY_COORDS, buf);
		}
		else {
			writeCoords(claim.getBox().getMinCoords(), buf);
			writeCoords(claim.getBox().getMaxCoords(), buf);
		}

		buf.writeUtf(StringUtils.defaultString(claim.getName(), ""));

		if (claim.getWhitelist().isEmpty()) {
			buf.writeInt(0);
			// buf.writeUtf(NULL_UUID));
			// buf.writeUtf(NULL_NAME);
		}
		else {
			claim.getWhitelist().forEach(player -> {
				buf.writeUtf(StringUtils.defaultString(player.getUuid(), NULL_UUID));
				buf.writeUtf(StringUtils.defaultString(player.getName(), ""));
			});
		}
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static RegistryLoadMessageToClient decode(PacketBuffer buf) {
		RegistryLoadMessageToClient message;
		
		// List<Interval> intervals = new ArrayList<>();
		List<Claim> claims = new ArrayList<>();

		try {
			String type = buf.readUtf();
			int size = buf.readInt();
			// for (int index = 0; index < size; index++) {
			// 	ICoords coords1 = readCoords(buf);
			// 	ICoords coords2 = readCoords(buf);
			// 	String uuid = buf.readUtf();
			// 	String playerName = buf.readUtf();
			// 	intervals.add(new Interval(coords1, coords2, new OwnershipData(uuid, playerName)));
			// }

			for (int index = 0; index < size; index++) {
				Claim claim = readClaim(buf);
				claims.add(claim);
			}
			
			// message = new RegistryLoadMessageToClient(type, intervals);
			message = new RegistryLoadMessageToClient(type, claims);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred attempting to read message: ", e);
			message = new RegistryLoadMessageToClient();
		}
		return message;
	}

	/**
	 *
	 */
	public static Claim readClaim(PacketBuffer buf) {
		List<PlayerData> whitelist = new ArrayList<>();

		String ownerUuid = buf.readUtf();
		String ownerName = buf.readUtf();
		ICoords coords = readCoords(buf);
		ICoords coords1 = readCoords(buf);
		ICoords coords2 = readCoords(buf);
		String name = buf.readUtf();
		int size = buf.readInt();
		for (int index = 0; index < size; index++) {
			String playerUuid = buf.readUtf();
			String playerName = buf.readUtf();
			whitelist.add(new PlayerData(playerName, playerUuid));
		}
		Claim claim = new Claim(coords, new Box(coords1, coords2), new PlayerData(ownerUuid, ownerName), name);
		claim.setWhitelist(whitelist);
		return claim;
	}
	
	// shared with RegistryMutatorMessageToClient
	private void writeCoords(ICoords coords, PacketBuffer buf) {
		if (coords != null) {
			buf.writeInt(coords.getX());
			buf.writeInt(coords.getY());
			buf.writeInt(coords.getZ());
		}
	}
	
	private static ICoords readCoords(PacketBuffer buf) {
		ICoords coords = new Coords(buf.readInt(), buf.readInt(), buf.readInt());
		return coords;
	}
	
	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

//	public List<Interval> getIntervals() {
//		return intervals;
//	}
//
//	public void setIntervals(List<Interval> intervals) {
//		this.intervals = intervals;
//	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	protected List<Claim> getClaims() {
		return claims;
	}

	protected void setClaims(List<Claim> claims) {
		this.claims = claims;
	}
}
