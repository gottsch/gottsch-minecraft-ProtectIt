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

import mod.gottsch.forge.protectit.ProtectIt;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;


/**
 * 
 * @author Mark Gottschling on Oct 13, 2021
 *
 */
public class ModNetworking {
	public static final String PROTOCOL_VERSION = "1.0";
	public static final int REGISTRY_MUTATOR_MESSAGE_ID = 14;
	public static final int REGISTRY_LOAD_MESSAGE_ID = 15;
	public static final int REGISTRY_LOAD_MESSAGE_TO_SERVER_ID = 16;
	public static final int REGISTRY_WHITELIST_MUTATOR_MESSAGE_ID = 17;

	public static final int PROPERTY_LEVER_MESSAGE_ID = 19;
	
	public static final int WHITELIST_ADD_ID = 20;
	public static final int WHITELIST_REMOVE_ID = 21;
	public static final int WHITELIST_CLEAR_ID = 22;
	public static final int PERMISSION_CHANGE_ID = 23;
	public static final int FIEF_ADD_ID = 24;
	
	public static final int PVP_REGISTRY_LOAD_ID = 25;
	public static final int ZONE_ADD_ID = 26;
	public static final int ZONE_REMOVE_ID = 27;
	public static final int ZONE_LIST_ID = 28;
	public static final int ZONE_CLEAR_ID = 29;
	public static final int PVP_PERMISSION_CHANGE_ID = 30;
	
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

		channel.registerMessage(REGISTRY_LOAD_MESSAGE_ID, RegistryLoadMessageToClient.class,
				RegistryLoadMessageToClient::encode, RegistryLoadMessageToClient::decode,
				RegistryLoadMessageHandlerOnClient::onMessageReceived,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));

//		channel.registerMessage(REGISTRY_LOAD_MESSAGE_TO_SERVER_ID, RegistryLoadMessageToServer.class,
//				RegistryLoadMessageToServer::encode, RegistryLoadMessageToServer::decode,
//				RegistryLoadMessageHandlerOnServer::onMessageReceived,
//				Optional.of(NetworkDirection.PLAY_TO_SERVER));

		channel.registerMessage(PROPERTY_LEVER_MESSAGE_ID, PropertyLeverS2C.class,
				PropertyLeverS2C::encode, PropertyLeverS2C::decode,
				PropertyLeverS2C::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(WHITELIST_ADD_ID, WhitelistAddS2CPush.class,
				WhitelistAddS2CPush::encode, WhitelistAddS2CPush::decode,
				WhitelistAddS2CPush::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(WHITELIST_REMOVE_ID, WhitelistRemoveS2CPush.class,
				WhitelistRemoveS2CPush::encode, WhitelistRemoveS2CPush::decode,
				WhitelistRemoveS2CPush::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(WHITELIST_CLEAR_ID, WhitelistClearS2CPush.class,
				WhitelistClearS2CPush::encode, WhitelistClearS2CPush::decode,
				WhitelistClearS2CPush::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(PERMISSION_CHANGE_ID, PermissionChangeS2CPush.class,
				PermissionChangeS2CPush::encode, PermissionChangeS2CPush::decode,
				PermissionChangeS2CPush::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(FIEF_ADD_ID, AddFiefS2CPush2.class,
				AddFiefS2CPush2::encode, AddFiefS2CPush2::decode,
				AddFiefS2CPush2::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(PVP_REGISTRY_LOAD_ID, PvpRegistryLoadS2CPush.class,
				PvpRegistryLoadS2CPush::encode, PvpRegistryLoadS2CPush::decode,
				PvpRegistryLoadS2CPush::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(ZONE_ADD_ID, PvpAddZoneS2CPush.class,
				PvpAddZoneS2CPush::encode, PvpAddZoneS2CPush::decode,
				PvpAddZoneS2CPush::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(ZONE_REMOVE_ID, PvpRemoveZoneS2CPush.class,
				PvpRemoveZoneS2CPush::encode, PvpRemoveZoneS2CPush::decode,
				PvpRemoveZoneS2CPush::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(ZONE_CLEAR_ID, PvpClearS2CPush.class,
				PvpClearS2CPush::encode, PvpClearS2CPush::decode,
				PvpClearS2CPush::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		
		channel.registerMessage(PVP_PERMISSION_CHANGE_ID, PvpPermissionChangeS2CPush.class,
				PvpPermissionChangeS2CPush::encode, PvpPermissionChangeS2CPush::decode,
				PvpPermissionChangeS2CPush::handle,
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}

}
