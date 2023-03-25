/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.command.CommandHelper;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.NetworkUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

/**
 * 
 * @author Mark Gottschling Mar 24, 2023
 *
 */
public class PvpPermissionChangeS2CPush {
	private UUID zone;
	private Box box;
	private int permission;
	private boolean value;
	
	protected PvpPermissionChangeS2CPush() {}
	
	public PvpPermissionChangeS2CPush(UUID zone, Box box, int permission, boolean value) {
		this.zone = zone;
		this.box = box;
		this.permission = permission;
		this.value = value;
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(zone);
		NetworkUtil.writeCoords(box.getMinCoords(), buf);
		NetworkUtil.writeCoords(box.getMaxCoords(), buf);
		buf.writeInt(permission);
		buf.writeBoolean(value);
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static PvpPermissionChangeS2CPush decode(FriendlyByteBuf buf) {
		PvpPermissionChangeS2CPush message = new PvpPermissionChangeS2CPush();
		
		try {
			message.zone = buf.readUUID();
			message.box = new Box(NetworkUtil.readCoords(buf), NetworkUtil.readCoords(buf));
			message.permission = buf.readInt();
			message.value = buf.readBoolean();
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("an error occurred attempting to read message: ", e);
			return message;
		}
		return message;
	}
	
	/**
	 * 
	 * @param message
	 * @param ctxSupplier
	 */
	public static void handle(final PvpPermissionChangeS2CPush message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		
		if (ctx.getDirection().getReceptionSide() != LogicalSide.CLIENT) {
			ProtectIt.LOGGER.warn("PvpPermissionChangeS2CPush received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}

		Optional<Level> client = LogicalSidedProvider.CLIENTWORLD.get(ctx.getDirection().getReceptionSide());
		if (!client.isPresent()) {
			ProtectIt.LOGGER.warn("PvpPermissionChangeS2CPush context could not provide a ClientWorld.");
			return;
		}
		
		// this creates a new task which will be executed by the server during the next tick
		ctx.enqueueWork(() -> processMessage((ClientLevel) client.get(), message));
		// mark as handled
		ctx.setPacketHandled(true);
	}

	/**
	 * 
	 * @param message
	 * @param sendingPlayer
	 */
	static void processMessage(ClientLevel level, PvpPermissionChangeS2CPush message) {

		try {
			ProtectionRegistries.pvp().changePermission(message.zone, message.box, message.permission, message.value);
		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to update zone permission on client: ", e);
		}
	}
}