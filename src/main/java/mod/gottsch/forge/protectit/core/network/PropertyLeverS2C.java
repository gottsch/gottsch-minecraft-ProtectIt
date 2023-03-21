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
import java.util.UUID;
import java.util.function.Supplier;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.PropertyLeverBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
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
public class PropertyLeverS2C implements ICoordsHandler {

	private boolean valid;
	public ICoords coords;
	public ICoords propertyCoords;
	public UUID propertyUuid;

	/**
	 * 
	 * @param builder
	 */
	public PropertyLeverS2C(ICoords coords, ICoords propertyCoords, UUID propertyUuid) {
		setCoords(coords);
		setPropertyCoords(propertyCoords);
		setPropertyUuid(propertyUuid);
		setValid(true);
	}

	/**
	 * 
	 */
	public PropertyLeverS2C() {
		setValid(false);
	}

	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		if (!isValid()) {
			return;
		}

		if (getCoords() == null) {
			writeCoords(Coords.EMPTY, buf);
		}
		else {
			writeCoords(getCoords(), buf);
		}
		
		if (getPropertyCoords() == null) {
			writeCoords(Coords.EMPTY, buf);
		}
		else {
			writeCoords(getPropertyCoords(), buf);
		}
		
		if (getPropertyUuid() == null) {
			buf.writeUtf("NULL");
		} else {
			buf.writeUtf(getPropertyUuid().toString());
		}

	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static PropertyLeverS2C decode(FriendlyByteBuf buf) {
		PropertyLeverS2C message;
		
		try {
			ICoords coords = ICoordsHandler.readCoords(buf);
			ICoords propertyCoords = ICoordsHandler.readCoords(buf);
			String uuidStr = buf.readUtf();
			UUID uuid = null;
//			new UUID(0L, 0L);
			if (!uuidStr.equalsIgnoreCase("NULL")) {
				uuid = UUID.fromString(uuidStr);
			}
			message = new PropertyLeverS2C(coords, propertyCoords, uuid);
			message.setValid(true);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred attempting to read message: ", e);
			message = new PropertyLeverS2C();
		}
		return message;
	}

	public static void handle(final PropertyLeverS2C message, Supplier<NetworkEvent.Context> ctxSupplier) {
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

	private static void processMessage(Context ctx, PropertyLeverS2C message) {
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
	private static void processMessage(Level worldClient, PropertyLeverS2C message) {
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
	
	public ICoords getCoords() {
		return coords;
	}

	public void setCoords(ICoords coords) {
		this.coords = coords;
	}

	@Override
	public String toString() {
		return "PropertyLeverMessageToClient [valid=" + valid + ", coords=" + coords + ", propertyCoords="
				+ propertyCoords + ", propertyUuid=" + propertyUuid + "]";
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public ICoords getPropertyCoords() {
		return propertyCoords;
	}

	public void setPropertyCoords(ICoords claimCoords) {
		this.propertyCoords = claimCoords;
	}

	public UUID getPropertyUuid() {
		return propertyUuid;
	}

	public void setPropertyUuid(UUID propertyUuid) {
		this.propertyUuid = propertyUuid;
	}
}