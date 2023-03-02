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

import java.util.UUID;
import java.util.function.Supplier;

import mod.gottsch.forge.protectit.ProtectIt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

/**
 * 
 * @author Mark Gottschling on Oct 14, 2021
 *
 */
@Deprecated
public class RegistryLoadMessageHandlerOnServer {
	
//	public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
//		return ProtectItNetworking.PROTOCOL_VERSION.equals(protocolVersion);
//	}

	public static void onMessageReceived(final RegistryLoadMessageToServer message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
		ctx.setPacketHandled(true);

		if (sideReceived != LogicalSide.SERVER) {
			ProtectIt.LOGGER.warn("RegistryLoadMessageToServer received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}
		if (!message.isValid()) {
			ProtectIt.LOGGER.warn("RegistryLoadMessageToServer was invalid -> {}", message.toString());
			return;
		}

		// we know for sure that this handler is only used on the server side, so it is ok to assume
		//  that the ctx handler is a serverhandler, and that ServerPlayerEntity exists
		// Packets received on the client side must be handled differently!  See MessageHandlerOnClient

		final ServerPlayer sendingPlayer = ctx.getSender();
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
	private static void processMessage(RegistryLoadMessageToServer message, ServerPlayer sendingPlayer) {
		ProtectIt.LOGGER.debug("received registry load message -> {}", message);
		try {
			MinecraftServer minecraftServer = sendingPlayer.server;
			ServerPlayer player = minecraftServer.getPlayerList().getPlayer(UUID.fromString(message.getUuid()));
			if (sendingPlayer != null) {
				// send message to update client's registry with the loaded data
//				TEMP
//				RegistryLoadMessageToClient clientMessage = new RegistryLoadMessageToClient(RegistryMutatorMessageToClient.BLOCK_TYPE, ProtectionRegistries.block().list());
//				ProtectItNetworking.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)player), clientMessage);
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unexpected error ->", e);
		}
	}	
}
