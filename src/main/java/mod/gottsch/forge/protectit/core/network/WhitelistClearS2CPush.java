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
import mod.gottsch.forge.protectit.core.registry.PlayerData;
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
 * @author Mark Gottschling Feb 20, 2023
 *
 */
public class WhitelistClearS2CPush {
	private UUID owner;
	private UUID property;
	
	protected WhitelistClearS2CPush() {}
	
	public WhitelistClearS2CPush(UUID owner, UUID property) {
	
		this.owner = owner;
		this.property = property;
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(owner);
		buf.writeUUID(property);
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static WhitelistClearS2CPush decode(FriendlyByteBuf buf) {
		WhitelistClearS2CPush message = new WhitelistClearS2CPush();
		
		try {
			message.owner = buf.readUUID();
			message.property = buf.readUUID();
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
	public static void handle(final WhitelistClearS2CPush message, Supplier<NetworkEvent.Context> ctxSupplier) {
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
		
		ctx.enqueueWork(() -> 
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> processMessage((ClientLevel) client.get(), message))
				);
		ctx.setPacketHandled(true);
	}

	/**
	 * 
	 * @param message
	 * @param sendingPlayer
	 */
	static void processMessage(ClientLevel level, WhitelistClearS2CPush message) {

		try {
			// get the property by uuid
			Optional<Property> claim = CommandHelper.getProperty(message.owner, message.property);
			if (claim.isPresent()) {
				claim.get().getWhitelist().clear();
			}
		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to update whitelist on client: ", e);
		}
	}
}
