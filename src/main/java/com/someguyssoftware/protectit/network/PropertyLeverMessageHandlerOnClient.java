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

import mod.gottsch.forge.gottschcore.spatial.Coords;
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

		if (!message.isValid()) {
			ProtectIt.LOGGER.warn("ClaimLeverMessageToClient was invalid -> {}", message.toString());
			return;
		}

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
			if (message.getCoords() == Coords.EMPTY || message.getPropertyCoords() == Coords.EMPTY) {
				ProtectIt.LOGGER.debug("coords/propertyCoords are missing -> {}", message);
				return;
			}
			BlockEntity blockEntity = worldClient.getBlockEntity(message.getCoords().toPos());
			ProtectIt.LOGGER.debug("blockEntity -> {}", blockEntity.getClass().getSimpleName());
			if (blockEntity instanceof PropertyLeverBlockEntity) {
				((PropertyLeverBlockEntity)blockEntity).setPropertyUuid(message.getPropertyUuid());
				((PropertyLeverBlockEntity)blockEntity).setPropertyCoords(message.getPropertyCoords());
				ProtectIt.LOGGER.debug("set the BE claim coords -> {}", message.getPropertyCoords());
			}
		}			
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unexpected error ->", e);
		}
	}
}
