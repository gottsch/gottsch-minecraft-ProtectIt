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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.UuidUtil;
import mod.gottsch.forge.protectit.core.zone.Zone;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

/**
 * 
 * @author Mark Gottschling Mar 21, 2023
 *
 */
public class PvpRegistryLoadS2CPush {
	private List<Zone> zones;

	protected PvpRegistryLoadS2CPush() { }

	public PvpRegistryLoadS2CPush(List<Zone> zones) {
		setZones(zones);
	}

	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		
		 buf.writeInt(zones.size());
		zones.forEach(zone -> {
			writeZone(buf, zone);
		});
	}

	private void writeZone(FriendlyByteBuf buf, Zone zone) {
		buf.writeUUID(Optional.ofNullable(zone.getUuid()).orElse(UuidUtil.EMPTY_UUID));
		buf.writeUtf(StringUtils.defaultIfBlank(zone.getName(), ""));
		if (zone.getBox() == null) {
			writeCoords(Coords.EMPTY, buf);
			writeCoords(Coords.EMPTY, buf);
		}
		else {
			writeCoords(zone.getBox().getMinCoords(), buf);
			writeCoords(zone.getBox().getMaxCoords(), buf);
		}
		buf.writeByte(zone.getPermissions());
		buf.writeLong(zone.getCreateTime());
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static PvpRegistryLoadS2CPush decode(FriendlyByteBuf buf) {
		List<Zone> zones = new ArrayList<>();
		try {
			int size = buf.readInt();
			for (int index = 0; index < size; index++) {
				Zone property = readZone(buf);
				zones.add(property);
			}

			PvpRegistryLoadS2CPush message = new PvpRegistryLoadS2CPush(zones);
			return message;
			
		} catch(Exception e) {
			ProtectIt.LOGGER.error("an error occurred attempting to read message: ", e);
			return null;
		}
	}

	private static Zone readZone(FriendlyByteBuf buf) {
		UUID uuid = buf.readUUID();
		String name = buf.readUtf();
		ICoords coords1 = readCoords(buf);
		ICoords coords2 = readCoords(buf);
		byte permissions = buf.readByte();
		long createTime = buf.readLong();
		
		Zone zone = new Zone(uuid, name, new Box(coords1, coords2));
		zone.setPermissions(permissions);
		zone.setCreateTime(createTime);
		
		return zone;
	}

	/**
	 * 
	 * @param message
	 * @param ctxSupplier
	 */
	public static void handle(final PvpRegistryLoadS2CPush message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		
		if (ctx.getDirection().getReceptionSide() != LogicalSide.CLIENT) {
			ProtectIt.LOGGER.warn("PvpRegistryLoadS2CPush received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}

		Optional<Level> client = LogicalSidedProvider.CLIENTWORLD.get(ctx.getDirection().getReceptionSide());
		if (!client.isPresent()) {
			ProtectIt.LOGGER.warn("PvpRegistryLoadS2CPush context could not provide a ClientWorld.");
			return;
		}
		
		ctx.enqueueWork(() -> 
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> processMessage((ClientLevel) client.get(), message))
				);
		ctx.setPacketHandled(true);
	}
	
	/**
	 * 
	 * @param clientLevel
	 * @param message
	 * @return
	 */
	private static Object processMessage(ClientLevel clientLevel, PvpRegistryLoadS2CPush message) {

		try {
			// load registry from interval list
			for(Zone zone : message.getZones()) {
				ProtectIt.LOGGER.debug("adding zone to registry -> {}", zone);
				ProtectionRegistries.pvp().addZone(zone);
			}
		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to update whitelist on client: ", e);
		}
		return null;
	}

	public List<Zone> getZones() {
		return zones;
	}

	public void setZones(List<Zone> zones) {
		this.zones = zones;
	}
	
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
}
