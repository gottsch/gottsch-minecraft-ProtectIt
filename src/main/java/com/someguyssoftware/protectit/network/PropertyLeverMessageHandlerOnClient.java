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
import com.someguyssoftware.protectit.block.entity.PropertyLeverBlockEntity;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkEvent.Context;


/**
 * 
 * @author Mark Gottschling on Dec 9, 2021
 *
 */
public class PropertyLeverMessageHandlerOnClient {

	public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
		return ProtectItNetworking.PROTOCOL_VERSION.equals(protocolVersion);
	}

	public static void onMessageReceived(final PropertyLeverMessageToClient message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
//		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
//
//		if (sideReceived != LogicalSide.CLIENT) {
//			ProtectIt.LOGGER.warn("ClaimLeverMessageToClient received on wrong side -> {}", ctx.getDirection().getReceptionSide());
//			return;
//		}
		if (!message.isValid()) {
			ProtectIt.LOGGER.warn("ClaimLeverMessageToClient was invalid -> {}", message.toString());
			return;
		}

		// we know for sure that this handler is only used on the client side, so it is ok to assume
		//  that the ctx handler is a client, and that Minecraft exists.
		// Packets received on the server side must be handled differently!  See MessageHandlerOnServer

//		Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
//		if (!clientWorld.isPresent()) {
//			ProtectIt.LOGGER.warn("RegistryMutatorMessageToClient context could not provide a ClientWorld.");
//			return;
//		}

		// This code creates a new task which will be executed by the client during the next tick
		//  In this case, the task is to call messageHandlerOnClient.processMessage(worldclient, message)
		ctx.enqueueWork(() -> 
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> processMessage(ctx, message))
				);
		ctx.setPacketHandled(true);
	}

	private static void processMessage(Context ctx, PropertyLeverMessageToClient message) {
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
		Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
		ProtectIt.LOGGER.debug("world -> {}", clientWorld.get());
		if (sideReceived != LogicalSide.CLIENT) {
			ProtectIt.LOGGER.warn("VaultCountMessageToClient received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}		
		Level level = clientWorld.get();
		processMessage(level, message);
	}

	/**
	 * 
	 * @param worldClient
	 * @param message
	 */
	private static void processMessage(Level worldClient, PropertyLeverMessageToClient message) {
		ProtectIt.LOGGER.debug("received claim lever message -> {}", message);

		try {			
			if (message.getCoords() == PropertyLeverMessageToClient.EMPTY_COORDS ||
					message.getClaimCoords() == PropertyLeverMessageToClient.EMPTY_COORDS) {
				ProtectIt.LOGGER.debug("coords/claimCoords are missing -> {}", message);
				return;
			}
			BlockEntity tileEntity = worldClient.getBlockEntity(message.getCoords().toPos());
			ProtectIt.LOGGER.debug("tileEntity -> {}", tileEntity.getClass().getSimpleName());
			if (tileEntity instanceof PropertyLeverBlockEntity) {
				((PropertyLeverBlockEntity)tileEntity).setClaimCoords(message.getClaimCoords());
				ProtectIt.LOGGER.debug("set the TE claim coords -> {}", message.getClaimCoords());
			}
		}			
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unexpected error ->", e);
		}
	}
}
