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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.core.property.Property;
import com.someguyssoftware.protectit.registry.PlayerData;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.network.FriendlyByteBuf;

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
	private List<Property> properties;
	
	public RegistryLoadMessageToClient() {
		valid = false;
	}

	public RegistryLoadMessageToClient(String type, List<Property> properties) {
		this.valid = true;
		this.type = type;
		this.size = properties.size();
		this.properties = properties;
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		if (!isValid()) {
			return;
		}
		 buf.writeUtf(StringUtils.defaultString(type, ""));
		 buf.writeInt(properties.size());

		properties.forEach(claim -> {
			writeProperty(buf, claim);
		});
	}

	/**
	 * 
	 * @param buf
	 * @param property
	 */
	private void writeProperty(FriendlyByteBuf buf, Property property) {
		buf.writeUtf(StringUtils.defaultString(property.getUuid().toString(), NULL_UUID));
		buf.writeUtf(StringUtils.defaultString(property.getOwner().getUuid(), NULL_UUID));
		buf.writeUtf(StringUtils.defaultString(property.getOwner().getName(), ""));

		if (property.getCoords() == null) {
				writeCoords(EMPTY_COORDS, buf);
			}
		else {
			writeCoords(property.getCoords(), buf);
		}

		if (property.getBox() == null) {
			writeCoords(EMPTY_COORDS, buf);
			writeCoords(EMPTY_COORDS, buf);
		}
		else {
			writeCoords(property.getBox().getMinCoords(), buf);
			writeCoords(property.getBox().getMaxCoords(), buf);
		}

		buf.writeUtf(StringUtils.defaultString(property.getName(), ""));

		if (property.getWhitelist().isEmpty()) {
			ProtectIt.LOGGER.debug("claim has no whitelist-> {}", property);
			buf.writeInt(0);
		}
		else {
			buf.writeInt(property.getWhitelist().size());
			property.getWhitelist().forEach(player -> {
				ProtectIt.LOGGER.debug("writing whitelist playerdata -> {}", player);
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
	public static RegistryLoadMessageToClient decode(FriendlyByteBuf buf) {
		RegistryLoadMessageToClient message;
		
		// List<Interval> intervals = new ArrayList<>();
		List<Property> properties = new ArrayList<>();

		try {
			String type = buf.readUtf();
			int size = buf.readInt();

			for (int index = 0; index < size; index++) {
				Property property = readProperty(buf);
				properties.add(property);
			}
			
			// message = new RegistryLoadMessageToClient(type, intervals);
			message = new RegistryLoadMessageToClient(type, properties);
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
	public static Property readProperty(FriendlyByteBuf buf) {
		List<PlayerData> whitelist = new ArrayList<>();

		String uuid = buf.readUtf();
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
			whitelist.add(new PlayerData(playerUuid, playerName));
		}
		Property property = new Property(coords, new Box(coords1, coords2), new PlayerData(ownerUuid, ownerName), name, UUID.fromString(uuid));
		property.setWhitelist(whitelist);
		ProtectIt.LOGGER.debug("decoded claim -> {}", property);
		return property;
	}
	
	// shared with RegistryMutatorMessageToClient
	private void writeCoords(ICoords coords, FriendlyByteBuf buf) {
		if (coords != null) {
			buf.writeInt(coords.getX());
			buf.writeInt(coords.getY());
			buf.writeInt(coords.getZ());
		}
	}
	
	private static ICoords readCoords(FriendlyByteBuf buf) {
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	protected List<Property> getProperties() {
		return properties;
	}

	protected void setProperties(List<Property> claims) {
		this.properties = claims;
	}

	@Override
	public String toString() {
		return "RegistryLoadMessageToClient [valid=" + valid + ", type=" + type + ", size=" + size + ", claims="
				+ properties + "]";
	}
}
