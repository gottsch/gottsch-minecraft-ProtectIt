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
package com.someguyssoftware.protectit.network;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.command.CommandHelper;
import com.someguyssoftware.protectit.property.Property;
import com.someguyssoftware.protectit.registry.PlayerData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.util.LangUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

/**
 * 
 * @author Mark Gottschling Feb 20, 2023
 *
 */
public class WhitelistAddS2CPush {
	private UUID owner;
	private UUID property;
	private String playerName;
	private UUID playerUuid;
	
	protected WhitelistAddS2CPush() {}
	
	public WhitelistAddS2CPush(UUID owner, UUID property, 
			String playerName, UUID playerUuid) {
	
		this.owner = owner;
		this.property = property;
		this.playerName = playerName;
		this.playerUuid = playerUuid;
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(owner);
		buf.writeUUID(property);
		buf.writeUtf(playerName);
		buf.writeUUID(playerUuid);
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static WhitelistAddS2CPush decode(FriendlyByteBuf buf) {
		WhitelistAddS2CPush message = new WhitelistAddS2CPush();
		
		try {
			message.owner = buf.readUUID();
			message.property = buf.readUUID();
			message.playerName = buf.readUtf();
			message.playerUuid = buf.readUUID();
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
	public static void handle(final WhitelistAddS2CPush message, Supplier<NetworkEvent.Context> ctxSupplier) {
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
	static void processMessage(ClientLevel level, WhitelistAddS2CPush message) {

		try {
			// get the property by uuid
			Optional<Property> claim = CommandHelper.getProperty(message.owner, message.property);
			if (claim.isPresent()) {
				claim.get().getWhitelist().add(new PlayerData(message.playerUuid.toString(), message.playerName));
			}
		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to update whitelist on client: ", e);
		}
	}
}
