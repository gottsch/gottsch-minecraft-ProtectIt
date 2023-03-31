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
import java.util.function.Supplier;

import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.NetworkUtil;
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
 * @author Mark Gottschling Mar 22, 2023
 *
 */
public class PvpRemoveZoneS2CPush extends PvpZoneIdMessage {

	public PvpRemoveZoneS2CPush(UUID uuid, ICoords coords) {
		super(uuid, coords);
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(getUuid());
		NetworkUtil.writeCoords(getCoords(), buf);
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static PvpRemoveZoneS2CPush decode(FriendlyByteBuf buf) {
		UUID uuid;
		ICoords coords;
		try {
			uuid = buf.readUUID();
			coords = NetworkUtil.readCoords(buf);
			PvpRemoveZoneS2CPush message = new PvpRemoveZoneS2CPush(uuid, coords);
			return message;
			
		} catch(Exception e) {
			ProtectIt.LOGGER.error("an error occurred attempting to read message: ", e);
			return null;
		}
	}
	
	/**
	 * 
	 * @param message
	 * @param ctxSupplier
	 */
	public static void handle(final PvpZoneIdMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		
		if (ctx.getDirection().getReceptionSide() != LogicalSide.CLIENT) {
			ProtectIt.LOGGER.warn("RemoveZoneS2CPush received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}

		Optional<Level> client = LogicalSidedProvider.CLIENTWORLD.get(ctx.getDirection().getReceptionSide());
		if (!client.isPresent()) {
			ProtectIt.LOGGER.warn("RemoveZoneS2CPush context could not provide a ClientWorld.");
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
	private static Object processMessage(ClientLevel clientLevel, PvpZoneIdMessage message) {

		try {
			// load registry from interval list
			ProtectionRegistries.pvp().removeZone(message.getUuid(), message.getCoords());
		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to remove zone on client: ", e);
		}
		return null;
	}
}