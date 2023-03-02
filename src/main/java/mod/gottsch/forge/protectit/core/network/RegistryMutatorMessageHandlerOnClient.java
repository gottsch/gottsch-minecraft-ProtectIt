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

import java.util.Optional;
import java.util.function.Supplier;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.IBlockProtectionRegistry;
import mod.gottsch.forge.protectit.core.registry.PlayerData;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;



/**
 * 
 * @author Mark Gottschling on Oct 13, 2021
 *
 */
public class RegistryMutatorMessageHandlerOnClient {
	
	public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
		return ProtectItNetworking.PROTOCOL_VERSION.equals(protocolVersion);
	}

	public static void onMessageReceived(final RegistryMutatorMessageToClient message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
		ctx.setPacketHandled(true);

		if (sideReceived != LogicalSide.CLIENT) {
			ProtectIt.LOGGER.warn("RegistryMutatorMessageToClient received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}
		if (!message.isValid()) {
			ProtectIt.LOGGER.warn("RegistryMutatorMessageToClient was invalid -> {}", message.toString());
			return;
		}

		// we know for sure that this handler is only used on the client side, so it is ok to assume
		//  that the ctx handler is a client, and that Minecraft exists.
		// Packets received on the server side must be handled differently!  See MessageHandlerOnServer

		Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
		if (!clientWorld.isPresent()) {
			ProtectIt.LOGGER.warn("RegistryMutatorMessageToClient context could not provide a ClientWorld.");
			return;
		}

		// This code creates a new task which will be executed by the client during the next tick
		//  In this case, the task is to call messageHandlerOnClient.processMessage(worldclient, message)
		ctx.enqueueWork(() -> processMessage(clientWorld.get(), message));
	}

	/**
	 * 
	 * @param worldClient
	 * @param message
	 */
	private static void processMessage(Level worldClient, RegistryMutatorMessageToClient message) {
		ProtectIt.LOGGER.debug("received registry mutator message -> {}", message);
		try {
			IBlockProtectionRegistry registry = null;
			switch(message.getType()) {
			default:
			case RegistryMutatorMessageToClient.BLOCK_TYPE:
				registry = ProtectionRegistries.block();
				break;
			case RegistryMutatorMessageToClient.PVP_TYPE:
				// TODO
				break;
			}
			
			if (message.getAction().equalsIgnoreCase(RegistryMutatorMessageToClient.ADD_ACTION)) {
				PlayerData data = new PlayerData(message.getUuid(), message.getPlayerName());
				Property claim = new Property(message.getCoords(), new Box(message.getCoords1(), message.getCoords2()), data, message.getName());
				registry.addProtection(claim);
			}
			else if (message.getAction().equalsIgnoreCase(RegistryMutatorMessageToClient.REMOVE_ACTION)) {
				if (!message.getCoords1().equals(RegistryMutatorMessageToClient.EMPTY_COORDS)) {
					ProtectIt.LOGGER.debug("has coords");
					// use methods that take coords
					if (message.getUuid().equals(RegistryMutatorMessageToClient.NULL_UUID)) {
						ProtectIt.LOGGER.debug("doesn't have uuid");
						registry.removeProtection(message.getCoords1(), message.getCoords2());
					}
					else {
						ProtectIt.LOGGER.debug("has uuid");
						registry.removeProtection(message.getCoords1(), message.getCoords2(), message.getUuid());
					}
				}
				else {
					if (!message.getUuid().equals(RegistryMutatorMessageToClient.NULL_UUID)) {
						ProtectIt.LOGGER.debug("doesn't have coord, but has uuid");
						registry.removeProtection(message.getUuid());
					}
				}
			}
			else if (message.getAction().equals(RegistryMutatorMessageToClient.CLEAR_ACTION)) {
				registry.removeProtection(message.getUuid());
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unexpected error ->", e);
		}
	}
	
	
}
