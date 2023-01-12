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

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;


/**
 * 
 * @author Mark Gottschling on Oct 13, 2021
 *
 */
public class ProtectItNetworking {
	public static final String PROTOCOL_VERSION = "1.0";
	public static final int REGISTRY_MUTATOR_MESSAGE_ID = 14;
	public static final int REGISTRY_LOAD_MESSAGE_ID = 15;
	public static final int REGISTRY_LOAD_MESSAGE_TO_SERVER_ID = 16;
	public static final int REGISTRY_WHITELIST_MUTATOR_MESSAGE_ID = 17;
	public static final int CLAIM_BOOK_MESSAGE_ID = 18;
	public static final int CLAIM_LEVER_MESSAGE_ID = 19;

	public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(ProtectIt.MODID, "protectit_channel");

	public static SimpleChannel channel;    // used to transmit your network messages

	/**
	 * 
	 * @param event
	 */
	public static void register() {
		// register the channel
		channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_NAME)
				.networkProtocolVersion(() -> PROTOCOL_VERSION)
				.clientAcceptedVersions(PROTOCOL_VERSION::equals)
				.serverAcceptedVersions(PROTOCOL_VERSION::equals)
				.simpleChannel();

		// register the messages		
		channel.registerMessage(REGISTRY_MUTATOR_MESSAGE_ID, RegistryMutatorMessageToClient.class,
				RegistryMutatorMessageToClient::encode, RegistryMutatorMessageToClient::decode,
				RegistryMutatorMessageHandlerOnClient::onMessageReceived,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));

		channel.registerMessage(REGISTRY_WHITELIST_MUTATOR_MESSAGE_ID, RegistryWhitelistMutatorMessageToClient.class,
				RegistryWhitelistMutatorMessageToClient::encode, RegistryWhitelistMutatorMessageToClient::decode,
				RegistryWhitelistMutatorMessageHandlerOnClient::onMessageReceived,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));

		channel.registerMessage(REGISTRY_LOAD_MESSAGE_ID, RegistryLoadMessageToClient.class,
				RegistryLoadMessageToClient::encode, RegistryLoadMessageToClient::decode,
				RegistryLoadMessageHandlerOnClient::onMessageReceived,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));

		channel.registerMessage(REGISTRY_LOAD_MESSAGE_TO_SERVER_ID, RegistryLoadMessageToServer.class,
				RegistryLoadMessageToServer::encode, RegistryLoadMessageToServer::decode,
				RegistryLoadMessageHandlerOnServer::onMessageReceived,
				Optional.of(NetworkDirection.PLAY_TO_SERVER));

		channel.registerMessage(CLAIM_BOOK_MESSAGE_ID, ClaimBookMessageToServer.class,
				ClaimBookMessageToServer::encode, ClaimBookMessageToServer::decode,
				ClaimBookMessageHandlerOnServer::onMessageReceived,
				Optional.of(NetworkDirection.PLAY_TO_SERVER));

		channel.registerMessage(CLAIM_LEVER_MESSAGE_ID, ClaimLeverMessageToClient.class,
				ClaimLeverMessageToClient::encode, ClaimLeverMessageToClient::decode,
				ClaimLeverMessageHandlerOnClient::onMessageReceived,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}

}
