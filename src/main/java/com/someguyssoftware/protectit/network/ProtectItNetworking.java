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

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT;
import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_SERVER;

import java.util.Optional;

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * 
 * @author Mark Gottschling on Oct 13, 2021
 *
 */
public class ProtectItNetworking {
	public static final String MESSAGE_PROTOCOL_VERSION = "1.0";
	public static final int REGISTRY_MUTATOR_MESSAGE_ID = 14;
	public static final int REGISTRY_LOAD_MESSAGE_ID = 15;
	public static final int REGISTRY_LOAD_MESSAGE_TO_SERVER_ID = 16;
	public static final int REGISTRY_WHITELIST_MUTATOR_MESSAGE_ID = 17;
	public static final int CLAIM_BOOK_MESSAGE_ID = 18;
	
	public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(ProtectIt.MODID, "protectit_channel");

	public static SimpleChannel simpleChannel;    // used to transmit your network messages

	/**
	 * 
	 * @param event
	 */
	public static void common(final FMLCommonSetupEvent event) {
		// register the channel
		simpleChannel = NetworkRegistry.newSimpleChannel(CHANNEL_NAME, () -> MESSAGE_PROTOCOL_VERSION,
	            ProtectItNetworking::isThisProtocolAcceptedByClient,
	            ProtectItNetworking::isThisProtocolAcceptedByServer);
		
		// register the messages
		
		simpleChannel.registerMessage(REGISTRY_MUTATOR_MESSAGE_ID, RegistryMutatorMessageToClient.class,
				RegistryMutatorMessageToClient::encode, RegistryMutatorMessageToClient::decode,
				RegistryMutatorMessageHandlerOnClient::onMessageReceived,
		            Optional.of(PLAY_TO_CLIENT));
		
		simpleChannel.registerMessage(REGISTRY_WHITELIST_MUTATOR_MESSAGE_ID, RegistryWhitelistMutatorMessageToClient.class,
				RegistryWhitelistMutatorMessageToClient::encode, RegistryWhitelistMutatorMessageToClient::decode,
				RegistryWhitelistMutatorMessageHandlerOnClient::onMessageReceived,
		            Optional.of(PLAY_TO_CLIENT));
		
		simpleChannel.registerMessage(REGISTRY_LOAD_MESSAGE_ID, RegistryLoadMessageToClient.class,
				RegistryLoadMessageToClient::encode, RegistryLoadMessageToClient::decode,
				RegistryLoadMessageHandlerOnClient::onMessageReceived,
		            Optional.of(PLAY_TO_CLIENT));
		
		simpleChannel.registerMessage(REGISTRY_LOAD_MESSAGE_TO_SERVER_ID, RegistryLoadMessageToServer.class,
				RegistryLoadMessageToServer::encode, RegistryLoadMessageToServer::decode,
				RegistryLoadMessageHandlerOnServer::onMessageReceived,
	            Optional.of(PLAY_TO_SERVER));
		
		simpleChannel.registerMessage(CLAIM_BOOK_MESSAGE_ID, ClaimBookMessageToServer.class,
				ClaimBookMessageToServer::encode, ClaimBookMessageToServer::decode,
				ClaimBookMessageHandlerOnServer::onMessageReceived,
	            Optional.of(PLAY_TO_SERVER));
	}
	
	/**
	 * 
	 * @param protocolVersion
	 * @return
	 */
	public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
		return MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
	}
	
	/**
	 * 
	 * @param protocolVersion
	 * @return
	 */
	public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
		return MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
	}
}
