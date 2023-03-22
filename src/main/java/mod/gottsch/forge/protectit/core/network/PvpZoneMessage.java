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
package mod.gottsch.forge.protectit.core.network;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.util.NetworkUtil;
import mod.gottsch.forge.protectit.core.util.UuidUtil;
import mod.gottsch.forge.protectit.core.zone.Zone;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 
 * @author Mark Gottschling Mar 21, 2023
 *
 */
public abstract class PvpZoneMessage {
	private Zone zone;
	
	public PvpZoneMessage(Zone zone) {
		setZone(zone);
	}
	
	/**
	 * 
	 * @param buf
	 * @param zone
	 */
	public void writeZone(FriendlyByteBuf buf, Zone zone) {
		buf.writeUUID(Optional.ofNullable(zone.getUuid()).orElse(UuidUtil.EMPTY_UUID));
		buf.writeUtf(StringUtils.defaultIfBlank(zone.getName(), ""));
		if (zone.getBox() == null) {
			NetworkUtil.writeCoords(Coords.EMPTY, buf);
			NetworkUtil.writeCoords(Coords.EMPTY, buf);
		}
		else {
			NetworkUtil.writeCoords(zone.getBox().getMinCoords(), buf);
			NetworkUtil.writeCoords(zone.getBox().getMaxCoords(), buf);
		}
		buf.writeByte(zone.getPermissions());
		buf.writeLong(zone.getCreateTime());
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static Zone readZone(FriendlyByteBuf buf) {
		UUID uuid = buf.readUUID();
		String name = buf.readUtf();
		ICoords coords1 = NetworkUtil.readCoords(buf);
		ICoords coords2 = NetworkUtil.readCoords(buf);
		byte permissions = buf.readByte();
		long createTime = buf.readLong();
		
		Zone zone = new Zone(uuid, name, new Box(coords1, coords2));
		zone.setPermissions(permissions);
		zone.setCreateTime(createTime);
		
		return zone;
	}

	public Zone getZone() {
		return zone;
	}

	public void setZone(Zone zone) {
		this.zone = zone;
	}
}
