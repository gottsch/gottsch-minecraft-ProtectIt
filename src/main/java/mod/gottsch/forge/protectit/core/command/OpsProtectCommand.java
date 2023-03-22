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
package mod.gottsch.forge.protectit.core.command;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.CustomClaimBlockEntity;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.item.ModItems;
import mod.gottsch.forge.protectit.core.network.PvpAddZoneS2CPush;
import mod.gottsch.forge.protectit.core.network.PvpClearS2CPush;
import mod.gottsch.forge.protectit.core.network.PvpRemoveZoneS2CPush;
import mod.gottsch.forge.protectit.core.network.ModNetworking;
import mod.gottsch.forge.protectit.core.network.RegistryMutatorMessageToClient;
import mod.gottsch.forge.protectit.core.persistence.ProtectItSavedData;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.PlayerIdentity;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import mod.gottsch.forge.protectit.core.zone.Zone;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling Feb 19, 2023
 *
 */
public class OpsProtectCommand {
	private static final String PROTECT = "protect-ops";

	// TODO add Whitelist OPS commands
	// it takes extra params like player

	/**
	 * 
	 * @param dispatcher
	 */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(PROTECT).requires(source -> {
			return source.hasPermission(Config.GENERAL.opsPermissionLevel.get()); // everyone can use base command
		}).then(Commands.literal(CommandHelper.PROPERTY)
				///// ADD OPTION /////
				.then(Commands.literal(CommandHelper.ADD)
						//						.requires(source -> {
						//					return source.hasPermission(Config.GENERAL.opsPermissionLevel.get());
						//				})
						.then(Commands.argument(CommandHelper.POS, BlockPosArgument.blockPos())
								.then(Commands.argument(CommandHelper.POS2, BlockPosArgument.blockPos()).then(
										Commands.argument(CommandHelper.TARGET, EntityArgument.player()).executes(source -> {
											return add(source.getSource(),
													BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS),
													BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS2),
													EntityArgument.getPlayer(source, CommandHelper.TARGET), false);
										}).then(Commands.literal("override_limit").executes(source -> {
											return add(source.getSource(),
													BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS),
													BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS2),
													EntityArgument.getPlayer(source, CommandHelper.TARGET), true);
										})
												)))))
				///// REMOVE OPTION /////
				.then(Commands.literal("remove")
						.requires(source -> {
							return source.hasPermission(Config.GENERAL.opsPermissionLevel.get());
						})
						//						.then(Commands.literal(CommandHelper.UUID)
						//								.then(Commands.argument(CommandHelper.UUID, StringArgumentType.greedyString())
						//										.suggests(CommandHelper.SUGGEST_UUID)
						//										// TODO pick a specific property from here
						//										.executes(source -> {
						//											return remove(source.getSource(), StringArgumentType.getString(source, CommandHelper.UUID));							
						//										})
						//										)
						//								)
						.then(Commands.literal("player")
								.then(Commands.argument(CommandHelper.TARGET, EntityArgument.player())
										// TODO option to pick a specific property
										.executes(source -> {
											return remove(source.getSource(), EntityArgument.getPlayer(source, CommandHelper.TARGET));							
										})
										)
								)
						.then(Commands.literal("pos")
								.then(Commands.argument(CommandHelper.POS, BlockPosArgument.blockPos())
										.executes(source -> {
											return remove(source.getSource(), BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS));
										})
										.then(Commands.argument("pos2", BlockPosArgument.blockPos())
												.executes(source -> {
													return remove(source.getSource(), BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS), BlockPosArgument.getLoadedBlockPos(source, "pos2"));
												})
												//												.then(Commands.literal(CommandHelper.UUID)
												//														.then(Commands.argument(CommandHelper.UUID, StringArgumentType.greedyString())
												//																.suggests(CommandHelper.SUGGEST_UUID)
												//																.executes(source -> {
												//																	return remove(source.getSource(), BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS), BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS2), StringArgumentType.getString(source, CommandHelper.UUID));							
												//																})
												//																)
												//														)
												.then(Commands.literal("player")
														.then(Commands.argument(CommandHelper.TARGET, EntityArgument.player())
																.executes(source -> {
																	return remove(source.getSource(), BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS), BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS2), EntityArgument.getPlayer(source, CommandHelper.TARGET));							
																})
																)													
														)
												)
										)
								)
						)
				///// CUSTOM STAKE /////
				.then(Commands.literal("custom-stake")
						.then(Commands.argument("size", BlockPosArgument.blockPos())
								.executes(source -> {
									return customStake(source.getSource(), BlockPosArgument.getLoadedBlockPos(source, "size"));
								})
								)
						)
				///// FIEF /////
				.then(Commands.literal("fief")
						///// ADD /////
						.then(Commands.literal("add")
								.executes(source -> {
									return CommandHelper.unavailable(source.getSource());
								})								
								)
						///// REMOVE / ANNEX /////
						.then(Commands.literal("remove")
								.executes(source -> {
									return CommandHelper.unavailable(source.getSource());
								})
								)
						///// TOGGLE
						.then(Commands.literal("toggle")
								.then(Commands.argument(CommandHelper.TARGET, EntityArgument.player())
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_TARGET_PROPERTY_NAMES)
												.then(Commands.literal("enable")
														.executes(source -> {
															return enableSubdivisible(source.getSource(), 
																	EntityArgument.getPlayer(source, CommandHelper.TARGET),
																	StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME), true);
														})										
														)
												.then(Commands.literal("disable")
														.executes(source -> {
															return enableSubdivisible(source.getSource(), 
																	EntityArgument.getPlayer(source, CommandHelper.TARGET),
																	StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME), false);
														})											
														)
												)	
										)
								)
						// TODO rename to fiefdom-deed
						///// GENERATE LICENSE /////
						.then(Commands.literal("generate-license")
								///// BY PLAYER (ONLINE) /////
								.then(Commands.literal("player")
										.then(Commands.argument(CommandHelper.TARGET, EntityArgument.player())
												.executes(source -> {
													return generateSubdivideLicense(source.getSource(), 
															EntityArgument.getPlayer(source, CommandHelper.TARGET));
												})
												.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
														.suggests(CommandHelper.SUGGEST_ALL_NESTED_PROPERTY_NAMES)
														.executes(source -> {
															return generateSubdivideLicense(source.getSource(),
																	EntityArgument.getPlayer(source, CommandHelper.TARGET),
																	StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME));
														})
														)
												)
										)
								///// SEARCH (EXISTING LAND OWNERS) /////
								.then(Commands.literal("search"))
								.executes(source -> {
									return CommandHelper.unavailable(source.getSource());
								})
								)
						)

				///// LIST OPTION /////
				.then(Commands.literal(CommandHelper.LIST)
						.then(Commands.argument(CommandHelper.TARGET, StringArgumentType.string())
								.suggests(CommandHelper.SUGGEST_PLAYER_NAMES)
								.executes(source -> {
									return CommandHelper.listProperties(source.getSource(), StringArgumentType.getString(source, CommandHelper.TARGET));
								})
								)
						)

				///// RENAME /////
				.then(Commands.literal(CommandHelper.RENAME)
						.then(Commands.argument(CommandHelper.TARGET, StringArgumentType.string())
								.suggests(CommandHelper.SUGGEST_PLAYER_NAMES)
								.executes(source -> {
									// TODO 
									return CommandHelper.unavailable(source.getSource());
								})						
								)
						)
				///// CLEAR OPTION /////
				.then(Commands.literal("clear")
						.requires(source -> {
							return source.hasPermission(Config.GENERAL.opsPermissionLevel.get());
						})
						// TODO rename player - clear is clear all
						.then(Commands.argument(CommandHelper.TARGET, EntityArgument.player())
								.executes(source -> {
									return clear(source.getSource(), EntityArgument.getPlayer(source, CommandHelper.TARGET));
								})
								)
						)
				///// GIVE OPTION /////
				.then(Commands.literal(CommandHelper.GIVE)
						.requires(source -> {
							return source.hasPermission(Config.GENERAL.giveCommandLevel.get());
						})
						.then(Commands.argument(CommandHelper.GIVE_ITEM, StringArgumentType.greedyString())
								.suggests(CommandHelper.SUGGEST_GIVABLE_ITEMS)
								.executes(source -> {
									return CommandHelper.give(source.getSource(), StringArgumentType.getString(source, CommandHelper.GIVE_ITEM));
								})
								)
						)
				.then(Commands.literal("dump")
						.executes(source -> {
							return dump();
						})
						)
				)
				///// PVP TOP-LEVEL OPTION /////
				.then(Commands.literal("pvp")
						///// ADD
						.then(Commands.literal(CommandHelper.ADD)
								.then(Commands.argument(CommandHelper.POS, BlockPosArgument.blockPos())
										.then(Commands.argument(CommandHelper.POS2, BlockPosArgument.blockPos())
												.executes(source -> {
													return addZone(source.getSource(),
															BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS),
															BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS2));
												})))
								)
						///// REMOVE
						.then(Commands.literal(CommandHelper.REMOVE)
								.then(Commands.argument(CommandHelper.ZONE_NAME, StringArgumentType.string())
										.suggests(CommandHelper.SUGGEST_ZONE_NAMES)
										.executes(source -> {
											return removeZone(source.getSource(), StringArgumentType.getString(source, CommandHelper.ZONE_NAME));
										})									
										)
								// TODO

								)
						///// CLEAR
						.then(Commands.literal(CommandHelper.CLEAR)
								.executes(source -> {
									return clearPvp(source.getSource());
								})
								)
						///// LIST

						///// RENAME
						.then(Commands.literal(CommandHelper.RENAME)
								.executes(source -> {
									return CommandHelper.unavailable(source.getSource());
								})
								)
						//// DUMP
						.then(Commands.literal("dump") 
								.executes(source -> {
									return dumpPvp();
								})
								)
						));
	}

	private static int removeZone(CommandSourceStack source, String zoneName) {
		// TODO find zone
		Optional<Zone> zone = ProtectionRegistries.pvp().getAll().stream().filter(z -> z.getName().equalsIgnoreCase(zoneName)).findFirst();
		if (zone.isPresent()) {
			ProtectionRegistries.pvp().removeZone(zone.get());
			// save world data
			CommandHelper.saveData(source.getLevel());

			// send message to add protection on all clients
			if(source.getLevel().getServer().isDedicatedServer()) {
				PvpRemoveZoneS2CPush message = new PvpRemoveZoneS2CPush(zone.get().getUuid(), zone.get().getBox().getMinCoords());
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}

			// TODO success message
			source.sendSuccess(Component.translatable(LangUtil.message("zone.successfully_removed"))
					.append(Component.translatable(zoneName).withStyle(ChatFormatting.AQUA)), false);
		}
		else {
			// failed message
			source.sendFailure(Component.translatable(LangUtil.message("zone.unknown_name")).withStyle(ChatFormatting.RED)
					.append(Component.translatable(zoneName).withStyle(ChatFormatting.AQUA))	);
		}
		return 1;
	}

	private static int clearPvp(CommandSourceStack source) {
		ProtectionRegistries.pvp().clear();

		CommandHelper.saveData(source.getLevel());

		ServerLevel world = source.getLevel();
		if(((ServerLevel)world).getServer().isDedicatedServer()) {
			// send message to add protection on all clients
			PvpClearS2CPush message = new PvpClearS2CPush();
			ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param loadedBlockPos
	 * @param loadedBlockPos2
	 * @return
	 */
	private static int addZone(CommandSourceStack source, BlockPos pos, BlockPos pos2) {
		ProtectIt.LOGGER.debug("Executing pvp add command...");
		try {
			// first, check that pos2 > pos1
			Optional<Tuple<ICoords, ICoords>> validCoords = CommandHelper.validateCoords(new Coords(pos), new Coords(pos2));
			if (!validCoords.isPresent()) {
				source.sendSuccess(Component.translatable(LangUtil.message("invalid_coords_format"))
						.withStyle(ChatFormatting.RED), true);
				return 1;
			}

			// second, check if any block in the area is already protected.
			if (ProtectionRegistries.pvp().isProtected(validCoords.get().getA(), validCoords.get().getB())) {
				// send message
				source.sendSuccess(Component.translatable(LangUtil.message("block_region.protected"))
						.withStyle(ChatFormatting.RED), true);
				return 1;
			}

			Zone zone = new Zone(
					UUID.randomUUID(),
					String.valueOf(ProtectionRegistries.pvp().size() + 1),
					new Box(validCoords.get().getA(), validCoords.get().getB()));
			zone.setCreateTime(source.getLevel().getGameTime());

			// add protection on server
			ProtectionRegistries.pvp().addZone(zone);

			// save world data
			CommandHelper.saveData(source.getLevel());

			// send message to add protection on all clients
			if(source.getLevel().getServer().isDedicatedServer()) {
				PvpAddZoneS2CPush message = new PvpAddZoneS2CPush(zone);
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unable to execute pvd add command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param size
	 * @return
	 */
	private static int customStake(CommandSourceStack source, BlockPos size) {
		Item claim = ModItems.CUSTOM_CLAIM.get();
		ItemStack stake = new ItemStack(claim);
		ICoords propertySize = new Coords(size);
		CompoundTag tag = stake.getOrCreateTag();
		tag.put(CustomClaimBlockEntity.CLAIM_SIZE_KEY, propertySize.save(new CompoundTag()));

		ServerPlayer player = source.getPlayer();
		if (!player.getInventory().add(stake)) {
			ItemEntity itemEntity = player.drop(stake, false);
			if (itemEntity != null) {
				itemEntity.setNoPickUpDelay();
				itemEntity.setOwner(player.getUUID());
			}
		}
		return 1;
	}

	/**
	 * 
	 * @return
	 */
	private static int dump() {
		try {
			ProtectionRegistries.property().dump();
		} catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		return 1;
	}
	
	private static int dumpPvp() {
		try {
			ProtectionRegistries.pvp().dump();
		} catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param player
	 * @return
	 */
	private static int generateSubdivideLicense(CommandSourceStack source, ServerPlayer player) {
		return generateSubdivideLicense(source, player, null);
	}

	private static int generateSubdivideLicense(CommandSourceStack source, ServerPlayer player, String propertyName) {
		try {
			Optional<Property> property = Optional.empty();
			UUID propertyUuid = null;
			// get the property by name
			if (StringUtils.isEmpty(propertyName)) {
				propertyUuid = new UUID(0L, 0L);
				propertyName = "";
			} else {
				property = CommandHelper.getPropertyByName(player.getUUID(), propertyName);
				if (property.isPresent()) {
					propertyUuid = property.get().getUuid();
				}
				else {
					propertyUuid = new UUID(0L, 0L);
				}
			}

			// create item stack
			ItemStack itemStack = new ItemStack(ModItems.FIEFDOM_GRANT.get());

			// set tag properties of stack
			CompoundTag tag = itemStack.getOrCreateTag();
			tag.putUUID("owner", player.getUUID());
			tag.putString("ownerName", player.getName().getString());
			tag.putUUID("property", propertyUuid);
			tag.putString("propertyName", propertyName);
			if (property.isPresent()) {
				CompoundTag boxTag = new CompoundTag();
				property.get().getBox().save(boxTag);
				tag.put("propertyBox", boxTag);
			}

			// give to ops	
			ServerPlayer giver = source.getPlayerOrException();
			if (!giver.getInventory().add(itemStack)) {
				ItemEntity itemEntity = giver.drop(itemStack, false);
				if (itemEntity != null) {
					itemEntity.setNoPickUpDelay();
					itemEntity.setOwner(giver.getUUID());
				}
			}

		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute generateSubdivideLicense() command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}
		return 1;
	}

	private static int enableSubdivisible(CommandSourceStack source, ServerPlayer owner, String propertyName, boolean value) {
		try {
			Optional<Property> property = CommandHelper.getPropertyByName(owner.getUUID(), propertyName);
			if (property.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
			property.get().setFiefdom(value);
			CommandHelper.saveData(source.getLevel());

			source.sendSuccess(Component.translatable(LangUtil.message("property.subdivide.enable.success"))
					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);

		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute enableSubdivisible command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}

		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @return
	 */
	private static int remove(CommandSourceStack source, BlockPos pos) {
		return remove(source, pos, pos);
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @param pos2
	 * @return
	 */
	private static int remove(CommandSourceStack source, BlockPos pos, BlockPos pos2) {
		// first, check that pos2 > pos1
		Optional<Tuple<ICoords, ICoords>> validCoords = CommandHelper.validateCoords(new Coords(pos), new Coords(pos2));
		if (!validCoords.isPresent()) {
			source.sendSuccess(Component.translatable("message.protectit.invalid_coords_format"), true);
			return 1;
		}

		ProtectionRegistries.property().removeProperties(validCoords.get().getA(), validCoords.get().getB());
		// save world data
		ServerLevel world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
		}
		if(((ServerLevel)world).getServer().isDedicatedServer()) {
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
	private static int remove(CommandSourceStack source, BlockPos pos, BlockPos pos2, String uuid) {
		try {
			// first, check that pos2 > pos1
			Optional<Tuple<ICoords, ICoords>> validCoords = CommandHelper.validateCoords(new Coords(pos), new Coords(pos2));
			if (!validCoords.isPresent()) {
				source.sendSuccess(Component.translatable("message.protectit.invalid_coords_format"), true);
				return 1;
			}

			uuid = CommandHelper.parseNameUuid(uuid);

			//			ProtectionRegistries.block().removeProtection(validCoords.get().getA(), validCoords.get().getB(), uuid);
			Box box = new Box(validCoords.get().getA(), validCoords.get().getB());
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(UUID.fromString(uuid))
					.stream().filter(p -> p.intersects(box)).toList();
			for (Property p : properties) {
				ProtectionRegistries.property().removeProperty(p);
			}

			// save world data
			ServerLevel world = source.getLevel();
			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			if (savedData != null) {
				savedData.setDirty();
			}
			if(((ServerLevel)world).getServer().isDedicatedServer()) {
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
	@Deprecated
	private static int remove(CommandSourceStack source, String uuid) {
		try {
			// parse out the uuid
			uuid = CommandHelper.parseNameUuid(uuid);
			//			ProtectionRegistries.block().removeProtection(uuid);
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(UUID.fromString(uuid));
			for (Property p : properties) {
				ProtectionRegistries.property().removeProperty(p);
			}

			// save world data
			ServerLevel world = source.getLevel();
			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			if (savedData != null) {
				savedData.setDirty();
			}
			if(((ServerLevel)world).getServer().isDedicatedServer()) {
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
	private static int remove(CommandSourceStack source, ServerPlayer player) {
		//		ProtectionRegistries.block().removeProtection(player.getStringUUID());
		ProtectIt.LOGGER.debug("doesn't have coord, but has uuid");
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(player.getUUID());
		for (Property p : properties) {
			ProtectionRegistries.property().removeProperty(p);
		}
		// save world data
		ServerLevel world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
		}
		if(((ServerLevel)world).getServer().isDedicatedServer()) {
			sendRemoveMessage(RegistryMutatorMessageToClient.BLOCK_TYPE, null, null, player.getStringUUID());
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
	private static int remove(CommandSourceStack source, BlockPos pos, BlockPos pos2, ServerPlayer player) {
		try {
			// first, check that pos2 > pos1
			Optional<Tuple<ICoords, ICoords>> validCoords = CommandHelper.validateCoords(new Coords(pos), new Coords(pos2));
			if (!validCoords.isPresent()) {
				source.sendSuccess(Component.translatable(LangUtil.message("invalid_coords_format")), true);
				return 1;
			}

			//			ProtectionRegistries.block().removeProtection(validCoords.get().getA(), validCoords.get().getB(), uuid);
			Box box = new Box(validCoords.get().getA(), validCoords.get().getB());
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(player.getUUID())
					.stream().filter(p -> p.intersects(box)).toList();
			for (Property p : properties) {
				ProtectionRegistries.property().removeProperty(p);
			}

			// save world data
			ServerLevel world = source.getLevel();
			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			if (savedData != null) {
				savedData.setDirty();
			}
			if(((ServerLevel)world).getServer().isDedicatedServer()) {
				sendRemoveMessage(RegistryMutatorMessageToClient.BLOCK_TYPE, 
						validCoords.get().getA(), 
						validCoords.get().getB(), 
						player.getUUID().toString());
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("error on remove -> ", e);
		}
		return 1;
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
		ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @param pos2
	 * @param players
	 * @return
	 */
	private static int add(CommandSourceStack source, BlockPos pos, BlockPos pos2, ServerPlayer player, boolean overrideLimit) {
		ProtectIt.LOGGER.debug("Executing add command...");
		try {
			// first, check that pos2 > pos1
			Optional<Tuple<ICoords, ICoords>> validCoords = CommandHelper.validateCoords(new Coords(pos), new Coords(pos2));
			if (!validCoords.isPresent()) {
				source.sendSuccess(Component.translatable(LangUtil.message("invalid_coords_format"))
						.withStyle(ChatFormatting.RED), true);
				return 1;
			}

			// second, check if any block in the area is already protected.
			if (ProtectionRegistries.property().isProtected(validCoords.get().getA(), validCoords.get().getB())) {
				// send message
				source.sendSuccess(Component.translatable(LangUtil.message("block_region.protected"))
						.withStyle(ChatFormatting.RED), true);
				return 1;
			}

			ProtectIt.LOGGER.debug("player entity -> {}", player.getDisplayName().getString());

			AtomicReference<String> name = new AtomicReference<>(player.getName().getString());			

			// check if player already owns protections
			//			List<Property> properties = ProtectionRegistries.block().getProtections(uuid);
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(player.getUUID());

			// check if the max # of properties has been reached (via config value)
			if (properties.size() >= Config.GENERAL.propertiesPerPlayer.get() && !overrideLimit) {
				source.sendFailure(Component.translatable(LangUtil.message("player_properties_max_limit"))
						.append(Component.translatable(String.valueOf(properties.size())).withStyle(ChatFormatting.AQUA)));
				return 1;
			}

			Property property = new Property(
					validCoords.get().getA(), 
					new Box(validCoords.get().getA(), validCoords.get().getB()),
					new PlayerIdentity(player.getUUID(), name.get()),
					String.valueOf(properties.size() + 1));
			property.setCreateTime(player.level.getGameTime());

			// add protection on server
			ProtectionRegistries.property().addProperty(property);

			// save world data
			CommandHelper.saveData(source.getLevel());

			// send message to add protection on all clients
			if(source.getLevel().getServer().isDedicatedServer()) {
				RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
						RegistryMutatorMessageToClient.BLOCK_TYPE, 
						RegistryMutatorMessageToClient.ADD_ACTION, 
						player.getUUID().toString()).with($ -> {
							$.coords1 = validCoords.get().getA();
							$.coords2 = validCoords.get().getB();
							$.playerName = name.get();
						}).build();
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("Unable to execute protect command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}

		return 1;
	}

	/**
	 * TODO refactor removing player
	 * @param source
	 * @return
	 */
	private static int clear(CommandSourceStack source, ServerPlayer player) {
		// remove all properties from player
		ProtectionRegistries.property().clear();

		ServerLevel world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
		}

		if(((ServerLevel)world).getServer().isDedicatedServer()) {
			// send message to add protection on all clients
			RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
					RegistryMutatorMessageToClient.BLOCK_TYPE, 
					RegistryMutatorMessageToClient.CLEAR_ACTION, 
					player.getStringUUID()).build();
			ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
		}
		return 1;
	}
}
