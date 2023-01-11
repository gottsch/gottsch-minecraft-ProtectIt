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
package com.someguyssoftware.protectit.network;

import java.util.Optional;
import java.util.function.Supplier;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.registry.IBlockProtectionRegistry;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

/**
 * 
 * @author Mark Gottschling on Dec 8, 2021
 *
 */
public class RegistryWhitelistMutatorMessageHandlerOnClient {
	
	public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
		return ProtectItNetworking.PROTOCOL_VERSION.equals(protocolVersion);
	}

	public static void onMessageReceived(final RegistryWhitelistMutatorMessageToClient message, Supplier<NetworkEvent.Context> ctxSupplier) {
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
	private static void processMessage(Level worldClient, RegistryWhitelistMutatorMessageToClient message) {
		ProtectIt.LOGGER.info("received registry mutator message -> {}", message);
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

			if (message.getAction().equals(RegistryWhitelistMutatorMessageToClient.WHITELIST_REPLACE_ACTION)) {
				if (!message.getClaims().isEmpty()) {
					message.getClaims().forEach(c -> {
						// update the claim - NOTE this should update the CLAIMS_BY_COORDS and CLAIMS_BY_OWNER as
						// they both reference the same object.
						Claim claim = ProtectionRegistries.block().getClaimByCoords(c.getBox().getMinCoords());
						if (claim != null) {
							claim.getWhitelist().clear();
							claim.getWhitelist().addAll(c.getWhitelist());
							ProtectIt.LOGGER.debug("updated claim on client -> {}", claim);
						}
						else {
							ProtectIt.LOGGER.debug("could not find claim for coords -> {}", c.getBox().getMinCoords());
						}
					});
				}
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unexpected error ->", e);
		}
	}
	
	
}
