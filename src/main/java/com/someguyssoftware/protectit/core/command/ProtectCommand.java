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
package com.someguyssoftware.protectit.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.core.config.Config;
import com.someguyssoftware.protectit.core.property.Permission;
import com.someguyssoftware.protectit.core.property.Property;
import com.someguyssoftware.protectit.network.PermissionChangeS2CPush;
import com.someguyssoftware.protectit.network.ProtectItNetworking;
import com.someguyssoftware.protectit.network.WhitelistAddS2CPush;
import com.someguyssoftware.protectit.network.WhitelistClearS2CPush;
import com.someguyssoftware.protectit.network.WhitelistRemoveS2CPush;
import com.someguyssoftware.protectit.registry.PlayerData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.util.LangUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Sep 15, 2021
 *
 */
public class ProtectCommand {
	private static final String PROTECT = "protect";
	private static final String RENAME = "rename";
	private static final String CURRENT_NAME = "current_name";
	private static final String NEW_NAME = "new_name";
	private static final String PROPERTY_NAME = "property_name";
	
	private static final SuggestionProvider<CommandSourceStack> WHITELIST_NAMES = (source, builder) -> {
		List<Property> properties = ProtectionRegistries.block().getProtections(source.getSource().getPlayerOrException().getStringUUID());
		List<String> names = properties.stream().flatMap(x -> x.getWhitelist().stream().map(y -> y.getName())).collect(Collectors.toList());
		return SharedSuggestionProvider.suggest(names, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> PERMISSIONS = (source, builder) -> {
		return SharedSuggestionProvider.suggest(Permission.getNames(), builder);
	};

	
	/*
	 * protect [block | pvp] [give | list | rename | whitelist [add | remove | clear | list]]
	 */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher
		.register(Commands.literal(PROTECT)

				///// BLOCK TOP-LEVEL OPTION /////
				.then(Commands.literal(CommandHelper.BLOCK)							
						///// LIST OPTION /////
						.then(Commands.literal(CommandHelper.LIST)
								.executes(source -> {
									return CommandHelper.list(source.getSource());
								})
								)
						// TODO add option for page #
						// TODO add option for player - OPS only						///// PERMISSION
						.then(Commands.literal("permission")
								///// PERMISSION CHANGE
								.then(Commands.literal("change")
										.then(Commands.argument(PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.PROPERTY_NAMES)
												.then(Commands.argument("permission", StringArgumentType.string())
														.suggests(PERMISSIONS)
														.then(Commands.literal("on")
																.executes(source -> {
																	return propertyChangePermission(source.getSource(), 
																			StringArgumentType.getString(source, PROPERTY_NAME), 
																			StringArgumentType.getString(source, "permission"), true);
																})
																)
														.then(Commands.literal("off")
																.executes(source -> {
																	return propertyChangePermission(source.getSource(), 
																			StringArgumentType.getString(source, PROPERTY_NAME), 
																			StringArgumentType.getString(source, "permission"), false);
																})
																)
														)
												)
										)
								///// PERMISSION LIST
								.then(Commands.literal("list")
										.then(Commands.argument(PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.PROPERTY_NAMES)
												.executes(source -> {
													return propertyListPermissions(source.getSource(), StringArgumentType.getString(source, PROPERTY_NAME));
												})
												)
										)
								)
						
						///// RENAME OPTION
						.then(Commands.literal(RENAME)
								.then(Commands.argument(CURRENT_NAME, StringArgumentType.string())
										.suggests(CommandHelper.PROPERTY_NAMES)
										.then(Commands.argument(NEW_NAME, StringArgumentType.string())
												.executes(source -> {
													return CommandHelper.rename(source.getSource(), 
															StringArgumentType.getString(source, CURRENT_NAME),
															StringArgumentType.getString(source, NEW_NAME));
												})
												)
										)
								)
						///// GIVE OPTION /////
						.then(Commands.literal(CommandHelper.GIVE)
								.requires(source -> {
									return source.hasPermission(Config.GENERAL.giveCommandLevel.get());
								})
								.then(Commands.argument(CommandHelper.GIVE_ITEM, StringArgumentType.greedyString())
										.suggests(CommandHelper.GIVABLE_ITEMS)
										.executes(source -> {
											return CommandHelper.give(source.getSource(), StringArgumentType.getString(source, CommandHelper.GIVE_ITEM));
										})
										) // end of ITEM
								// TODO add ownership
								)
						///// WHITELIST OPTION /////
						.then(Commands.literal(CommandHelper.WHITELIST)
								.requires(source -> {
									return source.hasPermission(0);
								})
								///// WHITELIST ADD /////
								.then(Commands.literal(CommandHelper.ADD)
										.then(Commands.argument(PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.PROPERTY_NAMES)
												.then(Commands.argument(CommandHelper.TARGET,EntityArgument.player())
														.executes(source -> {
															return whitelistAddPlayer(source.getSource(), 
																	StringArgumentType.getString(source, PROPERTY_NAME), EntityArgument.getPlayer(source, CommandHelper.TARGET));
														})
														)
												)
										)
								///// WHITELIST REMOVE /////
								.then(Commands.literal(CommandHelper.REMOVE)
										.then(Commands.argument(PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.PROPERTY_NAMES)
												.then(Commands.argument(CommandHelper.TARGET, StringArgumentType.string())
														.suggests(WHITELIST_NAMES)
														.executes(source -> {
															return whitelistRemovePlayer(source.getSource(), 
																	StringArgumentType.getString(source, PROPERTY_NAME), StringArgumentType.getString(source, CommandHelper.TARGET));
														})
														)
												)
										)
								///// WHITELIST LIST /////
								.then(Commands.literal(CommandHelper.LIST)
										.requires(source -> {
											return source.hasPermission(0);
										})
										.then(Commands.argument(PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.PROPERTY_NAMES)
												.executes(source -> {
													return whitelistListForProperty(source.getSource(), StringArgumentType.getString(source, PROPERTY_NAME));
												})
												)
										)
								///// WHTIELIST CLEAR /////
								.then(Commands.literal(CommandHelper.CLEAR)
										.then(Commands.argument(PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.PROPERTY_NAMES)
												.executes(source -> {
													return whitelistClear(source.getSource(), StringArgumentType.getString(source, PROPERTY_NAME));
												})
												)
										)								
								)							
						)
				///// PVP TOP-LEVEL OPTION /////
				.then(Commands.literal("pvp")
						// TODO
						.executes(source -> {
							return CommandHelper.unavailable(source.getSource());
						})
						)
				);
	}

	/**
	 * 
	 * @param source
	 * @param propertyName
	 * @return
	 */
	private static int propertyListPermissions(CommandSourceStack source, String propertyName) {
		try {
			// get the owner
			ServerPlayer owner = source.getPlayerOrException();
			// get the owner's properties
			List<Property> properties = ProtectionRegistries.block().getProtections(owner.getStringUUID());
			// get the named property
			List<Property> namedProperties = properties.stream().filter(claim -> claim.getName().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
			if (namedProperties.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
			Property property = namedProperties.get(0);

			List<Component> messages = new ArrayList<>();
			messages.add(Component.literal(""));
			messages.add(Component.translatable(LangUtil.message("property.permission.list"), propertyName.toUpperCase()).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
			messages.add(Component.literal(""));

			for (int i = 0; i < Permission.values().length; i++) {
				property.hasPermission(i);
				Permission permission = Permission.getByValue(i);
				MutableComponent component = Component.translatable(permission.name()).withStyle(ChatFormatting.AQUA)
						.append(Component.literal(" = "));
				if (property.hasPermission(i)) {
					component.append(Component.translatable(LangUtil.message("permission.state.on")).withStyle(ChatFormatting.GREEN));
				}
				else {
					component.append(Component.translatable(LangUtil.message("permission.state.off")).withStyle(ChatFormatting.RED));
				}
				messages.add(component);
			}

			messages.forEach(component -> {
				source.sendSuccess(component, false);
			});
		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute whitelistAddPlayer command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}
		// TODO print out all the permissions that are on.
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param propertyName
	 * @param permissionName
	 * @param value
	 * @return
	 */
	private static int propertyChangePermission(CommandSourceStack source, String propertyName, String permissionName, boolean value) {
		try {
			// get the owner
			ServerPlayer owner = source.getPlayerOrException();
			// get the owner's properties
			List<Property> properties = ProtectionRegistries.block().getProtections(owner.getStringUUID());
			// get the named property
			List<Property> namedProperties = properties.stream().filter(claim -> claim.getName().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
			if (namedProperties.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
			Property property = namedProperties.get(0);

			// update permission on property
			property.setPermission(Permission.valueOf(permissionName).value, value);

			//send update to client
			if(source.getLevel().getServer().isDedicatedServer()) {
				PermissionChangeS2CPush message = new PermissionChangeS2CPush(
						owner.getUUID(),
						property.getUuid(),
						Permission.valueOf(permissionName).value,
						value
						);
				ProtectItNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}

			source.sendSuccess(Component.translatable(LangUtil.message("permission.change.success"))
					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);

		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute whitelistAddPlayer command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}

		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param propertyName
	 * @param player
	 * @return
	 */
	public static int whitelistAddPlayer(CommandSourceStack source, String propertyName, @Nullable ServerPlayer player) {
		ProtectIt.LOGGER.debug("executing whitelist.add() command...");

		try {
			// get the owner
			ServerPlayer owner = source.getPlayerOrException();
			// TODO replace with CommmandHelper call
			// get the owner's properties
			List<Property> properties = ProtectionRegistries.block().getProtections(owner.getStringUUID());
			// get the named property
			List<Property> namedProperties = properties.stream().filter(prop -> prop.getName().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
			if (namedProperties.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
			Property property = namedProperties.get(0);
			// update property whitelist with player
			if (property.getWhitelist().stream().noneMatch(data -> data.getName().equalsIgnoreCase(player.getDisplayName().getString()))) {
				property.getWhitelist().add(new PlayerData(player.getStringUUID(), player.getDisplayName().getString()));
				CommandHelper.saveData(source.getLevel());
			}
			//send update to client
			if(source.getLevel().getServer().isDedicatedServer()) {
				WhitelistAddS2CPush message = new WhitelistAddS2CPush(
						owner.getUUID(),
						property.getUuid(),
						player.getName().getString(),
						player.getUUID()
						);
				ProtectItNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}

			source.sendSuccess(Component.translatable(LangUtil.message("whitelist.add.success"))
					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);

		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute whitelistAddPlayer command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}

		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param propertyName
	 * @param player
	 * @return
	 */
	public static int whitelistRemovePlayer(CommandSourceStack source, String propertyName, @Nullable String playerName) {
		ProtectIt.LOGGER.debug("Executing whitelistRemovePlayer() command...");
		
		try {
			// get the owner
			ServerPlayer owner = source.getPlayerOrException();
			// TODO replace with CommandHelper call
			// get the owner's properties
			List<Property> properties = ProtectionRegistries.block().getProtections(owner.getStringUUID());
			// get the named property
			List<Property> names = properties.stream().filter(p -> p.getName().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
			if (names.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
			Property property = names.get(0);
			// update property whitelist with player
			boolean result = property.getWhitelist().removeIf(p -> p.getName().equalsIgnoreCase(playerName));
			CommandHelper.saveData(source.getLevel());

			// send update to client
			if(result && source.getLevel().getServer().isDedicatedServer()) {
				WhitelistRemoveS2CPush message = new WhitelistRemoveS2CPush(
						owner.getUUID(),
						property.getUuid(),
						playerName.toUpperCase(),
						null
						);
				ProtectItNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}
			
			source.sendSuccess(Component.translatable(LangUtil.message("whitelist.remove.success"))
					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);

		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute whitelistRemovePlayer command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}


		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param propertyName
	 * @param player
	 * @return
	 */
	public static int whitelistClear(CommandSourceStack source, String propertyName) {
		ProtectIt.LOGGER.debug("Executing whitelistClear() command...");

		try {
			// get the owner
			ServerPlayer owner = source.getPlayerOrException();
//			// get the owner's properties
//			List<Property> properties = ProtectionRegistries.block().getProtections(owner.getStringUUID());
//			// get the named property
//			List<Property> namedProperties = properties.stream().filter(p -> p.getName().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
			Optional<Property> property = CommandHelper.getPropertyByName(owner.getUUID(), propertyName);
			if (property.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
//			Property claim = namedProperties.get(0);
			property.get().getWhitelist().clear();
			CommandHelper.saveData(source.getLevel());

			//send update to client
			if(source.getLevel().getServer().isDedicatedServer()) {
				WhitelistClearS2CPush message = new WhitelistClearS2CPush(
						owner.getUUID(),
						property.get().getUuid()
						);
				ProtectItNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}

			source.sendSuccess(Component.translatable(LangUtil.message("whitelist.clear.success"))
					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);

		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute whitelistClear command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}

		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param propertyName
	 * @return
	 */
	public static int whitelistListForProperty(CommandSourceStack source, String propertyName) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		}
		catch(CommandSyntaxException 	e) {
			source.sendFailure(Component.translatable(LangUtil.message("unable_locate_player")));
			return 1;
		}

		// TODO replace with CommadnHelper call
		List<Property> properties = ProtectionRegistries.block().getProtections(player.getStringUUID());
		List<Property> namedProperties = properties.stream().filter(p -> p.getName().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
		if (namedProperties.isEmpty()) {
			source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
					.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
			return 1;
		}
		source.sendSuccess(Component.translatable(LangUtil.NEWLINE), false);
		source.sendSuccess(Component.translatable(LangUtil.message("whitelist.property.list")).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
				.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);
		source.sendSuccess(Component.translatable(LangUtil.NEWLINE), false);

		namedProperties.get(0).getWhitelist().forEach(data -> {
			source.sendSuccess(Component.translatable(data.getName()).withStyle(ChatFormatting.GREEN), false);
		});

		return 1;
	}
}
