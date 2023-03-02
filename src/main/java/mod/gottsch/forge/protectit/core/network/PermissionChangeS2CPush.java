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

import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.command.CommandHelper;
import mod.gottsch.forge.protectit.core.property.Property;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

/**
 * 
 * @author Mark Gottschling Feb 20, 2023
 *
 */
public class PermissionChangeS2CPush {
	private UUID owner;
	private UUID property;
	private int permission;
	private boolean value;
	
	protected PermissionChangeS2CPush() {}
	
	public PermissionChangeS2CPush(UUID owner, UUID property, 
			int permission, boolean value) {
	
		this.owner = owner;
		this.property = property;
		this.permission = permission;
		this.value = value;
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(owner);
		buf.writeUUID(property);
		buf.writeInt(permission);
		buf.writeBoolean(value);
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static PermissionChangeS2CPush decode(FriendlyByteBuf buf) {
		PermissionChangeS2CPush message = new PermissionChangeS2CPush();
		
		try {
			message.owner = buf.readUUID();
			message.property = buf.readUUID();
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
	public static void handle(final PermissionChangeS2CPush message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		
		if (ctx.getDirection().getReceptionSide() != LogicalSide.CLIENT) {
			ProtectIt.LOGGER.warn("WhitelistAddS2CPush received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}

		Optional<Level> client = LogicalSidedProvider.CLIENTWORLD.get(ctx.getDirection().getReceptionSide());
		if (!client.isPresent()) {
			ProtectIt.LOGGER.warn("WhitelistAddS2CPush context could not provide a ClientWorld.");
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
	static void processMessage(ClientLevel level, PermissionChangeS2CPush message) {

		try {
			// get the property by uuid
			Optional<Property> property = CommandHelper.getProperty(message.owner, message.property);
			if (property.isPresent()) {
				property.get().setPermission(message.permission, message.value);
			}
		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to update whitelist on client: ", e);
		}
	}
}