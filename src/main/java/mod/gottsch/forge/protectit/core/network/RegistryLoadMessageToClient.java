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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.item.Deed;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.PlayerIdentity;
import mod.gottsch.forge.protectit.core.util.UuidUtil;
import net.minecraft.network.FriendlyByteBuf;

/**
 *
 * @author Mark Gottschling on Oct 14, 2021
 *
 */
public class RegistryLoadMessageToClient {
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

		properties.forEach(property -> {
			writeProperty(buf, property);
		});
		ProtectIt.LOGGER.debug("encoded property -> {}", this);
	}

	/**
	 * 
	 * @param buf
	 * @param property
	 */
	private void writeProperty(FriendlyByteBuf buf, Property property) {
		
		buf.writeUUID(Optional.ofNullable(property.getUuid()).orElse(UuidUtil.EMPTY_UUID));
		// protect against empty owner
		buf.writeUUID(Optional.ofNullable(property.getOwner()).map(PlayerIdentity::getUuid).orElse(UuidUtil.EMPTY_UUID));
		buf.writeUtf(Optional.ofNullable(property.getOwner()).map(PlayerIdentity::getName).orElse(""));
		// protect against empty landlord
		buf.writeUUID(Optional.ofNullable(property.getLord()).map(PlayerIdentity::getUuid).orElse(UuidUtil.EMPTY_UUID));
		buf.writeUtf(Optional.ofNullable(property.getLord()).map(PlayerIdentity::getName).orElse(""));

//		if (property.getCoords() == null) {
//				writeCoords(EMPTY_COORDS, buf);
//			}
//		else {
//			writeCoords(property.getCoords(), buf);
//		}

		if (property.getBox() == null) {
			writeCoords(Coords.EMPTY, buf);
			writeCoords(Coords.EMPTY, buf);
		}
		else {
			writeCoords(property.getBox().getMinCoords(), buf);
			writeCoords(property.getBox().getMaxCoords(), buf);
		}

		buf.writeUtf(StringUtils.defaultString(property.getNameByOwner(), ""));

		if (property.getWhitelist().isEmpty()) {
			ProtectIt.LOGGER.debug("property has no whitelist-> {}", property.getNameByOwner());
			buf.writeInt(0);
		}
		else {
			buf.writeInt(property.getWhitelist().size());
			property.getWhitelist().forEach(player -> {
				ProtectIt.LOGGER.debug("writing whitelist playerdata -> {}", player);
				buf.writeUUID(Optional.ofNullable(player).map(PlayerIdentity::getUuid).orElse(UuidUtil.EMPTY_UUID));
				buf.writeUtf(Optional.ofNullable(player).map(PlayerIdentity::getName).orElse(""));
			});
		}

		// permissions
		buf.writeByte(property.getPermissions());
		
		// fiefdom - don't need it
		
		// parent
		if (property.getParent() == null) {
			buf.writeUUID(Deed.EMPTY_UUID);
		}
		else {
			buf.writeUUID(property.getParent());
		}
		
		// children
		if (property.getChildren().isEmpty()) {
			ProtectIt.LOGGER.debug("property has no children -> {}", property.getNameByOwner());
			buf.writeInt(0);
		}
		else {
			buf.writeInt(property.getChildren().size());
			property.getChildren().forEach(uuid -> {
				ProtectIt.LOGGER.debug("writing child property -> {}", uuid);
//				writeProperty(buf, p);
				buf.writeUUID(uuid);
			});
		}
		
		// create time - don't need it
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static RegistryLoadMessageToClient decode(FriendlyByteBuf buf) {
		RegistryLoadMessageToClient message;

		List<Property> properties = new ArrayList<>();

		try {
			String type = buf.readUtf();
			int size = buf.readInt();

			for (int index = 0; index < size; index++) {
				Property property = readProperty(buf);
				properties.add(property);
			}

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
		List<PlayerIdentity> whitelist = new ArrayList<>();
		
		UUID uuid = buf.readUUID();
		UUID ownerUuid = buf.readUUID();
		String ownerName = buf.readUtf();
		UUID landlordUuid = buf.readUUID();
		String landlordName = buf.readUtf();
		
//		ICoords coords = readCoords(buf);
		ICoords coords1 = readCoords(buf);
		ICoords coords2 = readCoords(buf);
		
		String name = buf.readUtf();
		
		int size = buf.readInt();
		for (int index = 0; index < size; index++) {
			UUID playerUuid = buf.readUUID();
			String playerName = buf.readUtf();
			whitelist.add(new PlayerIdentity(playerUuid, playerName));
		}
		
		byte permissions = buf.readByte();
		UUID parent = buf.readUUID();
		
		Property property = new Property(uuid, name, coords1, new Box(coords1, coords2), new PlayerIdentity(ownerUuid, ownerName));
		property.setLord(new PlayerIdentity(landlordUuid, landlordName));
		property.setWhitelist(whitelist);
		property.setPermissions(permissions);
		property.setParent(parent);
		
		int childrenSize = buf.readInt();
		for (int index = 0; index < childrenSize; index++) {
//			Property child = readProperty(buf);
			property.getChildren().add(buf.readUUID());
		}
		
		ProtectIt.LOGGER.debug("decoded property -> {}", property);
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

	protected void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	@Override
	public String toString() {
		return "RegistryLoadMessageToClient [valid=" + valid + ", type=" + type + ", size=" + size + ", properties="
				+ properties + "]";
	}
}
