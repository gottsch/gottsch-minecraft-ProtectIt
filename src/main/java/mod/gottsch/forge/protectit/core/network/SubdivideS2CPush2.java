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

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.command.CommandHelper;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.PlayerData;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
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
public class SubdivideS2CPush2 implements ICoordsHandler {
	private UUID target;
	private UUID landlord;
	private UUID owner;
	private UUID property;
	private Box box;
	
	protected SubdivideS2CPush2() {}
	
	public SubdivideS2CPush2(UUID target, UUID landlord, UUID owner, UUID property, Box box) {
		this.target = target;
		this.landlord = landlord;
		this.owner = owner;
		this.property = property;
		this.box = box;
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(target);
		buf.writeUUID(landlord);
		buf.writeUUID(owner);
		buf.writeUUID(property);
		writeCoords(box.getMinCoords(), buf);
		writeCoords(box.getMaxCoords(), buf);
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static SubdivideS2CPush2 decode(FriendlyByteBuf buf) {
		SubdivideS2CPush2 message = new SubdivideS2CPush2();
		
		try {
			message.target = buf.readUUID();
			message.landlord = buf.readUUID();
			message.owner = buf.readUUID();
			message.property = buf.readUUID();
			ICoords min = ICoordsHandler.readCoords(buf);
			ICoords max = ICoordsHandler.readCoords(buf);
			message.box = new Box(min, max);
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
	public static void handle(final SubdivideS2CPush2 message, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		
		if (ctx.getDirection().getReceptionSide() != LogicalSide.CLIENT) {
			ProtectIt.LOGGER.warn("WhitelistAddS2CPush received on wrong side -> {}", ctx.getDirection().getReceptionSide());
			return;
		}

		Optional<Level> client = LogicalSidedProvider.CLIENTWORLD.get(ctx.getDirection().getReceptionSide());
		if (!client.isPresent()) {
			ProtectIt.LOGGER.warn("SubdivideS2CPush context could not provide a ClientWorld.");
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
	static void processMessage(ClientLevel level, SubdivideS2CPush2 message) {

		try {
			// get the property by uuid
//			Optional<Property> property = CommandHelper.getProperty(message.owner, message.property);
			Optional<Property> target = ProtectionRegistries.block().getPropertyByUuid(message.target);
			if (target.isPresent()) {
				// create a box from the valid coords
				Box box = message.box;
				
				// build a property
				Property property = new Property(
						box.getMinCoords(),
						box,
						new PlayerData(target.get().getOwner().getUuid(), target.get().getOwner().getName()),
						"NAME_DOESNT_MATTER_ON_CLIENT");
				property.setLandlord(new PlayerData(message.owner.toString(), message.owner.toString()));
				property.setNameByLandlord("NAME_DOESNT_MATTER_ON_CLIENT");
				
				ProtectionRegistries.block().addSubdivision(target.get(), property);
			}
		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to update whitelist on client: ", e);
		}
	}
}
