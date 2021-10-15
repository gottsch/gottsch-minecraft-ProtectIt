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

import java.util.UUID;
import java.util.function.Supplier;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Oct 14, 2021
 *
 */
public class RegistryLoadMessageHandlerOnServer {
	
	public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
		return ProtectItNetworking.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
	}

	public static void onMessageReceived(final RegistryLoadMessageToServer message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
		ctx.setPacketHandled(true);

		if (sideReceived != LogicalSide.SERVER) {
			ProtectIt.LOGGER.warn("PoisonMistMessageToServer received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}
		if (!message.isValid()) {
			ProtectIt.LOGGER.warn("PoisonMessageToServer was invalid -> {}", message.toString());
			return;
		}

		// we know for sure that this handler is only used on the server side, so it is ok to assume
		//  that the ctx handler is a serverhandler, and that ServerPlayerEntity exists
		// Packets received on the client side must be handled differently!  See MessageHandlerOnClient

		final ServerPlayerEntity sendingPlayer = ctx.getSender();
		if (sendingPlayer == null) {
			ProtectIt.LOGGER.warn("EntityPlayerMP was null when PoisonMistMessageToServer was received");
		}

		// This code creates a new task which will be executed by the server during the next tick,
		//  In this case, the task is to call messageHandlerOnServer.processMessage(message, sendingPlayer)
		ctx.enqueueWork(() -> processMessage(message, sendingPlayer));
	}

	/**
	 * 
	 * @param worldServer
	 * @param message
	 */
	private static void processMessage(RegistryLoadMessageToServer message, ServerPlayerEntity sendingPlayer) {
		ProtectIt.LOGGER.debug("received registry load message -> {}", message);
		try {
			MinecraftServer minecraftServer = sendingPlayer.server;
			ServerPlayerEntity player = minecraftServer.getPlayerList().getPlayer(UUID.fromString(message.getUuid()));
			if (sendingPlayer != null) {
				// send message to update client's registry with the loaded data
				RegistryLoadMessageToClient clientMessage = new RegistryLoadMessageToClient(RegistryMutatorMessageToClient.BLOCK_TYPE, ProtectionRegistries.getRegistry().list());
				ProtectItNetworking.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)player), clientMessage);
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unexpected error ->", e);
		}
	}	
}
