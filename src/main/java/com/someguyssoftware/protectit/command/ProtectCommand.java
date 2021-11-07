/*
 * This file is part of  Treasure2.
 * Copyright (c) 2021, Mark Gottschling (gottsch)
 * 
 * All rights reserved.
 *
 * Treasure2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Treasure2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Treasure2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package com.someguyssoftware.protectit.command;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.network.ProtectItNetworking;
import com.someguyssoftware.protectit.network.RegistryMutatorMessageToClient;
import com.someguyssoftware.protectit.network.RegistryWhitelistMutatorMessageToClient;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.registry.PlayerData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.registry.BlockProtectionRegistry;
import com.someguyssoftware.protectit.registry.bst.Interval;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Sep 15, 2021
 *
 */
public class ProtectCommand {

	private static final String POS = "pos";
	private static final String POS2 = "pos2";
	private static final String TARGET = "target";
	private static final String TARGETS = "targets";
	private static final String UUID = "uuid";

	///// SUGGESTIONS /////
	private static final SuggestionProvider<CommandSource> SUGGEST_UUID = (source, builder) -> {
		// get all uuids from registry
//		return ISuggestionProvider.suggest(ProtectionRegistries.block().find(p -> !p.getData().getOwner().getUuid().isEmpty()).stream()
//				.map(i -> String.format("%s [%s]", 
//						(i.getData().getOwner().getName() == null) ? "" : i.getData().getOwner().getName(),
//								(i.getData().getOwner().getUuid() == null) ? "" : i.getData().getOwner().getUuid())), builder);
		
		return ISuggestionProvider.suggest(ProtectionRegistries.block().findByClaim(p -> !p.getOwner().getUuid().isEmpty()).stream()
				.map(i -> String.format("%s [%s]", 
						(i.getOwner().getName() == null) ? "" : i.getOwner().getName(),
								(i.getOwner().getUuid() == null) ? "" : i.getOwner().getUuid())), builder);
	};

	/*
	 * protect [block|pvp] [add|remove|list|whitelist*|unwhitelist*] [uuid|pos] [pos2] [uuid|entity]
	 */
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher
		.register(Commands.literal("protect")
				.requires(source -> {
					return source.hasPermission(0); // everyone can use base command
				})

				///// BLOCK TOP-LEVEL OPTION /////
				.then(Commands.literal("block")
						///// ADD OPTION /////
						.then(Commands.literal("add")
								.requires(source -> {
									return source.hasPermission(4);
								})
								.then(Commands.argument(POS, BlockPosArgument.blockPos())
										.executes(source -> {
											return add(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS));
										})
										// TODO have just player instead of pos2
										.then(Commands.argument(POS2, BlockPosArgument.blockPos())
												.executes(source -> {
													return add(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS), BlockPosArgument.getOrLoadBlockPos(source, POS2));
												})
												.then(Commands.argument(TARGETS, EntityArgument.players())
														.executes(source -> {
															return add(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS), BlockPosArgument.getOrLoadBlockPos(source, POS2), EntityArgument.getPlayers(source, TARGETS));							
														})
														)
												)
										)
								)
						///// REMOVE OPTION /////
						.then(Commands.literal("remove")
								.requires(source -> {
									return source.hasPermission(4);
								})
								.then(Commands.literal(UUID)
										.then(Commands.argument(UUID, StringArgumentType.greedyString())
												.suggests(SUGGEST_UUID)
												.executes(source -> {
													return remove(source.getSource(), StringArgumentType.getString(source, UUID));							
												})
												)
										)
								.then(Commands.literal("entity")
										.then(Commands.argument(TARGETS, EntityArgument.entities())
												.executes(source -> {
													return remove(source.getSource(), EntityArgument.getEntities(source, TARGETS));							
												})
												)
										)
								.then(Commands.literal("pos")
										.then(Commands.argument(POS, BlockPosArgument.blockPos())
												.executes(source -> {
													return remove(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS));
												})
												.then(Commands.argument("pos2", BlockPosArgument.blockPos())
														.executes(source -> {
															return remove(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS), BlockPosArgument.getOrLoadBlockPos(source, "pos2"));
														})
														.then(Commands.literal(UUID)
																.then(Commands.argument(UUID, StringArgumentType.greedyString())
																		.suggests(SUGGEST_UUID)
																		.executes(source -> {
																			return remove(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS), BlockPosArgument.getOrLoadBlockPos(source, POS2), StringArgumentType.getString(source, UUID));							
																		})
																		)
																)
														.then(Commands.literal("entity")
																.then(Commands.argument(TARGETS, EntityArgument.entities())
																		.executes(source -> {
																			return remove(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS), BlockPosArgument.getOrLoadBlockPos(source, POS2), EntityArgument.getEntities(source, TARGETS));							
																		})
																		)													
																)
														)
												)
										)
								)
						///// LIST OPTION /////
						.then(Commands.literal("list")
								.requires(source -> {
									return source.hasPermission(0);
								})
								.executes(source -> {
									return list(source.getSource());
								})
								)
						///// CLEAR OPTION /////
						.then(Commands.literal("clear")
								.requires(source -> {
									return source.hasPermission(4);
								})
								.executes(source -> {
									return clear(source.getSource());
								})
								)
						///// WHITELIST OPTION /////
						.then(Commands.literal("whitelist")
								.requires(source -> {
									return source.hasPermission(0);
								})
								///// WHITELIST ADD /////
								.then(Commands.literal("add")
										.then(Commands.argument(POS, BlockPosArgument.blockPos())
												.executes(source -> {
													return addWhitelist(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS));
												})
												.then(Commands.argument(POS2, BlockPosArgument.blockPos())
														.executes(source -> {
															return addWhitelist(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS), BlockPosArgument.getOrLoadBlockPos(source, POS2));
														})
														.then(Commands.argument(TARGET, EntityArgument.player())
																.executes(source -> {
																	return addWhitelist(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, POS), BlockPosArgument.getOrLoadBlockPos(source, POS2), EntityArgument.getPlayer(source, TARGET));							
																})
																)
														)
												)
										)
								///// WHITELIST REMOVE /////
								.then(Commands.literal("remove")
										// TODO
										.executes(source -> {
											return unavailable(source.getSource());
										})
										)
								///// WHITELIST LIST /////
								.then(Commands.literal("list")
										.requires(source -> {
											return source.hasPermission(0);
										})
										// TODO - needs pos1, pos2, entity
										.executes(source -> {
											return unavailable(source.getSource());
										})
										)
								///// WHTIELIST CLEAR /////
								.then(Commands.literal("clear")
										.requires(source -> {
											return source.hasPermission(4);
										})
										// TODO
										.executes(source -> {
											return unavailable(source.getSource());
										})
										)								
								)							
						)
				///// PVP TOP-LEVEL OPTION /////
				.then(Commands.literal("pvp")
						// TODO
						.executes(source -> {
							return unavailable(source.getSource());
						})
						)

				);
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	private static int unavailable(CommandSource source) {
		source.sendSuccess(new TranslationTextComponent("message.protectit.option_unavailable"), true);
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @return
	 */
	private static int add(CommandSource source, BlockPos pos) {
		return add(source, pos, pos, null);
	}

	private static int add(CommandSource source, BlockPos pos, BlockPos pos2) {
		return add(source, pos, pos2, null);
	}
	/**
	 * 
	 * @param source
	 * @param pos
	 * @param pos2
	 * @return
	 */
	private static int add(CommandSource source, BlockPos pos, BlockPos pos2, @Nullable Collection<ServerPlayerEntity> players) {
		ProtectIt.LOGGER.debug("Executing protect command...");
		try {
			// first, check that pos2 > pos1
			Optional<Tuple<ICoords, ICoords>> validCoords = CommandUtils.validateCoords(new Coords(pos), new Coords(pos2));
			if (!validCoords.isPresent()) {
				source.sendSuccess(new TranslationTextComponent("message.protectit.invalid_coords_format"), true);
				return 1;
			}

			// second, check if any block in the area is already protected.
			if (ProtectionRegistries.block().isProtected(validCoords.get().getA(), validCoords.get().getB())) {
				// send message
				source.sendSuccess(new TranslationTextComponent("message.protectit.block_region_protected"), true);
				return 1;
			}

			// determine the player uuid
			String uuid = "";
			AtomicReference<String> name = new AtomicReference<>("");
			if (players != null) {
				ServerPlayerEntity player = players.iterator().next();
				ProtectIt.LOGGER.debug("player entity -> {}", player.getDisplayName().getString());
				uuid = player.getStringUUID();
				name.set(player.getName().getString());
			}

			// check if player already owns protections
			List<Claim> claims = ProtectionRegistries.block().getProtections(uuid);
			
			// TODO check if the max # of claims has been reached (via config value)
			
			// create a claim
			Claim claim = new Claim(
					validCoords.get().getA(), 
					new Box(validCoords.get().getA(), validCoords.get().getB()),
					new PlayerData(uuid, name.get()),
					String.valueOf(claims.size() + 1));
			
			// add protection on server
//			ProtectionRegistries.block().addProtection(validCoords.get().getA(), validCoords.get().getB(), new PlayerData(uuid, name.get()));
			ProtectionRegistries.block().addProtection(claim);
			
			// save world data
			ServerWorld world = source.getLevel();
			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			if (savedData != null) {
				savedData.setDirty();
			}

			// send message to add protection on all clients
			if(((ServerWorld)world).getServer().isDedicatedServer()) {
				RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
						RegistryMutatorMessageToClient.BLOCK_TYPE, 
						RegistryMutatorMessageToClient.ADD_ACTION, 
						uuid).with($ -> {
							$.coords1 = validCoords.get().getA();
							$.coords2 = validCoords.get().getB();
							$.playerName = name.get();
						}).build();
				ProtectItNetworking.simpleChannel.send(PacketDistributor.ALL.noArg(), message);
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unable to execute protect command:", e);
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @return
	 */
	private static int remove(CommandSource source, BlockPos pos) {
		return remove(source, pos, pos);
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @param pos2
	 * @return
	 */
	private static int remove(CommandSource source, BlockPos pos, BlockPos pos2) {
		// first, check that pos2 > pos1
		Optional<Tuple<ICoords, ICoords>> validCoords = CommandUtils.validateCoords(new Coords(pos), new Coords(pos2));
		if (!validCoords.isPresent()) {
			source.sendSuccess(new TranslationTextComponent("message.protectit.invalid_coords_format"), true);
			return 1;
		}

		ProtectionRegistries.block().removeProtection(validCoords.get().getA(), validCoords.get().getB());
		// save world data
		ServerWorld world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
		}
		if(((ServerWorld)world).getServer().isDedicatedServer()) {
			sendRemoveMessage(RegistryMutatorMessageToClient.BLOCK_TYPE, 
					validCoords.get().getA(), 
					validCoords.get().getB(), 
					RegistryMutatorMessageToClient.NULL_UUID);
		}
		return 1;
	}

	/**
	 * Unprotect all protections intersecting the interval pos -> pos2 && where player == uuid
	 * @param source
	 * @param pos
	 * @param pos2
	 * @param uuid
	 * @return
	 */
	private static int remove(CommandSource source, BlockPos pos, BlockPos pos2, String uuid) {
		try {
			// first, check that pos2 > pos1
			Optional<Tuple<ICoords, ICoords>> validCoords = CommandUtils.validateCoords(new Coords(pos), new Coords(pos2));
			if (!validCoords.isPresent()) {
				source.sendSuccess(new TranslationTextComponent("message.protectit.invalid_coords_format"), true);
				return 1;
			}

			uuid = parseNameUuid(uuid);

			ProtectionRegistries.block().removeProtection(validCoords.get().getA(), validCoords.get().getB(), uuid);
			// save world data
			ServerWorld world = source.getLevel();
			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			if (savedData != null) {
				savedData.setDirty();
			}
			if(((ServerWorld)world).getServer().isDedicatedServer()) {
				sendRemoveMessage(RegistryMutatorMessageToClient.BLOCK_TYPE, validCoords.get().getA(), validCoords.get().getB(), uuid);
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("error on remove uuid -> ", e);
		}
		return 1;
	}

	/**
	 * Unprotect all protections where player == uuid 
	 * @param source
	 * @param uuid
	 * @return
	 */
	private static int remove(CommandSource source, String uuid) {
		try {
			// parse out the uuid
			uuid = parseNameUuid(uuid);
			ProtectionRegistries.block().removeProtection(uuid);
			// save world data
			ServerWorld world = source.getLevel();
			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			if (savedData != null) {
				savedData.setDirty();
			}
			if(((ServerWorld)world).getServer().isDedicatedServer()) {
				sendRemoveMessage(RegistryMutatorMessageToClient.BLOCK_TYPE, null, null, uuid);
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("error on remove uuid -> ", e);
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param entities
	 * @return
	 */
	private static int remove(CommandSource source, Collection<? extends Entity> entities) {
		String uuid = "";
		Entity entity = entities.iterator().next();

		if (entity instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity)entity;
			uuid = player.getStringUUID();				
		}
		ProtectionRegistries.block().removeProtection(uuid);
		// save world data
		ServerWorld world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
		}
		if(((ServerWorld)world).getServer().isDedicatedServer()) {
			sendRemoveMessage(RegistryMutatorMessageToClient.BLOCK_TYPE, null, null, uuid);
		}

		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @param pos2
	 * @return
	 */
	private static int remove(CommandSource source, BlockPos pos, BlockPos pos2, Collection<? extends Entity> entities) {
		try {
			// first, check that pos2 > pos1
			Optional<Tuple<ICoords, ICoords>> validCoords = CommandUtils.validateCoords(new Coords(pos), new Coords(pos2));
			if (!validCoords.isPresent()) {
				source.sendSuccess(new TranslationTextComponent("message.protectit.invalid_coords_format"), true);
				return 1;
			}

			String uuid = "";
			Entity entity = entities.iterator().next();

			if (entity instanceof PlayerEntity) {
				PlayerEntity player = (PlayerEntity)entity;
				uuid = player.getStringUUID();				
			}
			ProtectionRegistries.block().removeProtection(validCoords.get().getA(), validCoords.get().getB(), uuid);

			// save world data
			ServerWorld world = source.getLevel();
			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			if (savedData != null) {
				savedData.setDirty();
			}
			if(((ServerWorld)world).getServer().isDedicatedServer()) {
				sendRemoveMessage(RegistryMutatorMessageToClient.BLOCK_TYPE, 
						validCoords.get().getA(), 
						validCoords.get().getB(), 
						uuid);
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("error on remove -> ", e);
		}
		return 1;
	}

	/**
	 * 
	 * @param uuid
	 * @return
	 */
	private static String parseNameUuid(String uuid) {
		String output = "";
		// parse out the uuid
		if (uuid.contains("[")) {
			// find between square brackets
			Pattern p = Pattern.compile("\\[([^\"]*)\\]");
			Matcher m = p.matcher(uuid);
			// get first occurence
			if (m.find()) {
				output = m.group(1);
			}
		}
		return output;
	}

	/**
	 * 
	 * @param coords1
	 * @param coords2
	 * @param uuid
	 */
	private static void sendRemoveMessage(String type, @Nullable ICoords coords1, @Nullable ICoords coords2, String uuid) {
		if (uuid == null) {
			uuid = RegistryMutatorMessageToClient.NULL_UUID;
		}

		// send message to add protection on all clients
		RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
				type,
				RegistryMutatorMessageToClient.REMOVE_ACTION, 
				uuid).with($ -> {
					$.coords1 = coords1;
					$.coords2 = coords2;
					$.playerName = "";
				}).build();
		ProtectItNetworking.simpleChannel.send(PacketDistributor.ALL.noArg(), message);
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	private static int list(CommandSource source) {
		List<String> list = ProtectionRegistries.block().toStringList();
		list.forEach(element -> {
			source.sendSuccess(new StringTextComponent(element), true);
		});
		if (list.isEmpty()) {
			source.sendSuccess(new TranslationTextComponent("message.protectit.empty_list"), true);
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	private static int clear(CommandSource source) {
		ProtectionRegistries.block().clear();

		ServerWorld world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
		}

		if(((ServerWorld)world).getServer().isDedicatedServer()) {
			// send message to add protection on all clients
			RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
					RegistryMutatorMessageToClient.BLOCK_TYPE, 
					RegistryMutatorMessageToClient.CLEAR_ACTION, 
					RegistryMutatorMessageToClient.NULL_UUID).build();
			ProtectItNetworking.simpleChannel.send(PacketDistributor.ALL.noArg(), message);
		}
		return 1;
	}
	
	
	/**
	 * 
	 * @param source
	 * @param pos
	 * @return
	 */
	private static int addWhitelist(CommandSource source, BlockPos pos) {
		return addWhitelist(source, pos, pos, null);
	}

	private static int addWhitelist(CommandSource source, BlockPos pos, BlockPos pos2) {
		return addWhitelist(source, pos, pos2, null);
	}
	/**
	 * 
	 * @param source
	 * @param pos
	 * @param pos2
	 * @return
	 */
	private static int addWhitelist(CommandSource source, BlockPos pos, BlockPos pos2, @Nullable ServerPlayerEntity player) {
		ProtectIt.LOGGER.debug("Executing protect command...");

		try {
			// first, check that pos2 > pos1
			Optional<Tuple<ICoords, ICoords>> validCoords = CommandUtils.validateCoords(new Coords(pos), new Coords(pos2));
			if (!validCoords.isPresent()) {
				source.sendSuccess(new TranslationTextComponent("message.protectit.invalid_coords_format"), true);
				return 1;
			}

			// second, check if the area is not protected.
			if (!ProtectionRegistries.block().isProtected(validCoords.get().getA(), validCoords.get().getB())) {
				// send message
				source.sendSuccess(new TranslationTextComponent("message.protectit.block_region_not_protected_or_owner"), true);
				return 1;
			}

			// determine the player uuid
			String uuid = "";
			AtomicReference<String> name = new AtomicReference<>("");
			if (player != null) {
				ProtectIt.LOGGER.debug("player entity -> {}", player.getDisplayName().getString());
				uuid = player.getStringUUID();
				name.set(player.getName().getString());
			}
			ServerPlayerEntity owner = source.getPlayerOrException();
			
			// TODO update
			// add protection on server
			List<Interval> whitelisted = ((BlockProtectionRegistry)ProtectionRegistries.block()).addWhitelist(validCoords.get().getA(), validCoords.get().getB(), new PlayerData(owner.getStringUUID(), owner.getName().getString()), new PlayerData(uuid, name.get()));
			if (whitelisted.isEmpty()) {
				return 1;
			}

			// save world data
			ServerWorld world = source.getLevel();
			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			if (savedData != null) {
				savedData.setDirty();
			}

			// send message to add protection on all clients
			if(((ServerWorld)world).getServer().isDedicatedServer()) {
				RegistryMutatorMessageToClient message = new RegistryWhitelistMutatorMessageToClient.Builder(
						RegistryMutatorMessageToClient.BLOCK_TYPE, 
						RegistryWhitelistMutatorMessageToClient.WHITELIST_ADD_ACTION,
						uuid).with($ -> {
							$.coords1 = validCoords.get().getA();
							$.coords2 = validCoords.get().getB();
							$.playerName = name.get();
						}).build();
				ProtectItNetworking.simpleChannel.send(PacketDistributor.ALL.noArg(), message);
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unable to execute protect command:", e);
		}
		
		// TODO check that you are the owner of said protection(s)
		// TODO add targetEntity's uuid & name to whitelist of protection(s)
		// TODO send update message
		// TEMP
		source.sendSuccess(new StringTextComponent("You attempted to whitelist someone"), true);
		return 1;
	}
}
