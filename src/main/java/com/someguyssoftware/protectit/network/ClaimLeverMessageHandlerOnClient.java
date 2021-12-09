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

import java.util.Optional;
import java.util.function.Supplier;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.registry.IBlockProtectionRegistry;
import com.someguyssoftware.protectit.registry.PlayerData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.tileentity.ClaimLeverTileEntity;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

/**
 * 
 * @author Mark Gottschling on Dec 9, 2021
 *
 */
public class ClaimLeverMessageHandlerOnClient {

	public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
		return ProtectItNetworking.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
	}

	public static void onMessageReceived(final ClaimLeverMessageToClient message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
		ctx.setPacketHandled(true);

		if (sideReceived != LogicalSide.CLIENT) {
			ProtectIt.LOGGER.warn("ClaimLeverMessageToClient received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}
		if (!message.isValid()) {
			ProtectIt.LOGGER.warn("ClaimLeverMessageToClient was invalid -> {}", message.toString());
			return;
		}

		// we know for sure that this handler is only used on the client side, so it is ok to assume
		//  that the ctx handler is a client, and that Minecraft exists.
		// Packets received on the server side must be handled differently!  See MessageHandlerOnServer

		Optional<ClientWorld> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
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
	private static void processMessage(ClientWorld worldClient, ClaimLeverMessageToClient message) {
		ProtectIt.LOGGER.debug("received claim lever message -> {}", message);

		try {			
			if (message.getCoords() == ClaimLeverMessageToClient.EMPTY_COORDS ||
					message.getClaimCoords() == ClaimLeverMessageToClient.EMPTY_COORDS) {
				ProtectIt.LOGGER.debug("coords/claimCoords are missing -> {}", message);
				return;
			}
			TileEntity tileEntity = worldClient.getBlockEntity(message.getCoords().toPos());
			ProtectIt.LOGGER.debug("tileEntity -> {}", tileEntity.getClass().getSimpleName());
			if (tileEntity instanceof ClaimLeverTileEntity) {
				((ClaimLeverTileEntity)tileEntity).setClaimCoords(message.getClaimCoords());
				ProtectIt.LOGGER.debug("set the TE claim coords -> {}", message.getClaimCoords());
			}
		}			
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unexpected error ->", e);
		}
	}
}
