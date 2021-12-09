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

import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.item.ProtectItItems;
import com.someguyssoftware.protectit.registry.PlayerData;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

/**
 * 
 * @author Mark Gottschling on Nov 20, 2021
 *
 */
public class ClaimBookMessageHandlerOnServer {
	private static final String PLAYER_DATA_TAG = "playerData";

	public static void onMessageReceived(final ClaimBookMessageToServer message,
			Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
		ctx.setPacketHandled(true);

		if (sideReceived != LogicalSide.SERVER) {
			ProtectIt.LOGGER.warn("ClaimBookMessageToServer received on wrong side -> {}",
					ctx.getDirection().getReceptionSide());
			return;
		}
		if (!message.isValid()) {
			ProtectIt.LOGGER.warn("ClaimBookMessageToServer was invalid -> {}", message.toString());
			return;
		}

		// we know for sure that this handler is only used on the server side, so it is
		// ok to assume that the ctx handler is a serverhandler, and that ServerPlayerEntity exists.
		// Packets received on the client side must be handled differently! See MessageHandlerOnClient

		final ServerPlayerEntity sendingPlayer = ctx.getSender();
		if (sendingPlayer == null) {
			ProtectIt.LOGGER.warn("EntityPlayerMP was null when ClaimBookMessageToServer was received");
		}

		// This code creates a new task which will be executed by the server during the next tick,
		// In this case, the task is to call messageHandlerOnServer.processMessage(message, sendingPlayer)
		ctx.enqueueWork(() -> processMessage(message, sendingPlayer));
	}

	/**
	 * 
	 * @param worldServer
	 * @param message
	 */
	private static void processMessage(ClaimBookMessageToServer message, ServerPlayerEntity sendingPlayer) {
		ProtectIt.LOGGER.debug("received registry load message -> {}", message);
		try {
			if (sendingPlayer != null) {
				if (message.getBook().getItem() == ProtectItItems.CLAIM_BOOK) {
					CompoundNBT nbt = message.getBook().getTag();

					List<PlayerData> playerDataList = Lists.newArrayList();
					ListNBT sourceListNbt = nbt.getList(PLAYER_DATA_TAG, 10);
					ProtectIt.LOGGER.debug("sourceListNbt.size -> {}", sourceListNbt.size());
					List<ServerPlayerEntity> players = sendingPlayer.getLevel().players();
					sourceListNbt.forEach(element -> {
						PlayerData data = new PlayerData().load((CompoundNBT) element);
						ProtectIt.LOGGER.debug("loaded data -> {}", data);
						// check for player on server if missing uuid and add it
						if (data.getUuid().isEmpty()) {
							players.forEach(player -> {
								ProtectIt.LOGGER.debug("checking data -> {} against player -> {}", data.getName(), player.getDisplayName());
								if (player.getDisplayName().getString().equals(data.getName())) {									
									data.setUuid(player.getStringUUID());
								}
							});
						}
						ProtectIt.LOGGER.debug("adding data to playerDataList -> {}", data);
						playerDataList.add(data);
					});

					int slot = message.getSlot();
					if (PlayerInventory.isHotbarSlot(slot) || slot == 40) {
						ItemStack itemStack = sendingPlayer.inventory.getItem(slot);
						if (itemStack.getItem() == ProtectItItems.CLAIM_BOOK) {
							ListNBT destListNbt = new ListNBT();
							playerDataList.forEach(data -> {
								CompoundNBT dataNbt = data.save();
								destListNbt.add(dataNbt);
							});
							itemStack.addTagElement("playerData", destListNbt);
						}
					}
				}
			}
		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unexpected error ->", e);
		}
	}
}
