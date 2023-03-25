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
package mod.gottsch.forge.protectit.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.item.Deed;
import mod.gottsch.forge.protectit.core.item.ModItems;
import mod.gottsch.forge.protectit.core.network.ModNetworking;
import mod.gottsch.forge.protectit.core.network.PermissionChangeS2CPush;
import mod.gottsch.forge.protectit.core.network.AddFiefS2CPush2;
import mod.gottsch.forge.protectit.core.network.WhitelistAddS2CPush;
import mod.gottsch.forge.protectit.core.network.WhitelistClearS2CPush;
import mod.gottsch.forge.protectit.core.network.WhitelistRemoveS2CPush;
import mod.gottsch.forge.protectit.core.property.Permission;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.property.PropertyUtil;
import mod.gottsch.forge.protectit.core.registry.PlayerIdentity;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.registry.TransactionRegistry;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import mod.gottsch.forge.protectit.core.util.UuidUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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


	private static final SuggestionProvider<CommandSourceStack> WHITELIST_NAMES = (source, builder) -> {
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(source.getSource().getPlayerOrException().getUUID());
		List<String> names = properties.stream().flatMap(x -> x.getWhitelist().stream().map(y -> y.getName() )).collect(Collectors.toList());
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
				.requires(source -> {
					return source.hasPermission(0);
				})
				///// BLOCK TOP-LEVEL OPTION /////
				.then(Commands.literal(CommandHelper.PROPERTY)							
						///// LIST OPTION /////
						.then(Commands.literal(CommandHelper.LIST)
								.executes(source -> {
									return CommandHelper.listProperties(source.getSource());
								})
								)
						///// FIEF /////
						.then(Commands.literal("fief")
								.then(Commands.literal("add")
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_PROPERTY_NAMES)
												.then(Commands.argument(CommandHelper.POS, BlockPosArgument.blockPos())
														.then(Commands.argument(CommandHelper.POS2, BlockPosArgument.blockPos())
																.executes(source -> {
																	return createFief(source.getSource(),
																			StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME),
																			BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS),
																			BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS2),
																			new PlayerIdentity());
																})
																.then(Commands.argument("owner", EntityArgument.player())
																		.executes(source -> {
																			return createFief(source.getSource(),
																					StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME),
																					BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS),
																					BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS2),
																					EntityArgument.getPlayer(source, "owner"));
																		})
																		)))										
												)
										)
								///// FIEF REMOVE /////
								.then(Commands.literal("remove")
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_FIEF_NAMES)
												.executes(source -> {
													return annexFief(source.getSource(), StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME));
												})
												) )
								)


						///// GENERATE LEASE
						.then(Commands.literal("lease")
								.then(Commands.literal("generate")
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												// TODO property names must provide all owned properties AND vacant landlord properties
												.suggests(CommandHelper.SUGGEST_ALL_NESTED_PROPERTY_NAMES)
												.executes(source -> {
													return generateLease(source.getSource(),
															StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME));
												})
												)
										)
								.then(Commands.literal("transfer")

										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_ALL_NESTED_PROPERTY_NAMES)
												.then(Commands.argument(CommandHelper.TARGET, EntityArgument.player())
														.executes(source -> {
															return transferLease(source.getSource(),
																	StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME),
																	EntityArgument.getPlayer(source, CommandHelper.TARGET));
														})																
														)
												)

										)
								)
						///// GENERATE DEED
						.then(Commands.literal("deed")
								.then(Commands.literal("generate")
										// TODO don't need TARGET
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_TARGET_PROPERTY_NAMES)
												.executes(source -> {
													return generateDeed(source.getSource(),
															StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME));
												})
												)
										)
								// DEED TRANSFER
								.then(Commands.literal("transfer")
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_TARGET_PROPERTY_NAMES)
												.then(Commands.argument(CommandHelper.TARGET, EntityArgument.player())
														.executes(source -> {
															return transferDeed(source.getSource(),
																	StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME),
																	EntityArgument.getPlayer(source, CommandHelper.TARGET));
														})																
														)
												)
										)

								)
						///// PERMISSION
						.then(Commands.literal("permission")
								///// PERMISSION CHANGE
								.then(Commands.literal("change")
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_PROPERTY_NAMES)
												.then(Commands.argument("permission", StringArgumentType.string())
														.suggests(PERMISSIONS)
														.then(Commands.literal("on")
																.executes(source -> {
																	return propertyChangePermission(source.getSource(), 
																			StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME), 
																			StringArgumentType.getString(source, "permission"), true);
																})
																)
														.then(Commands.literal("off")
																.executes(source -> {
																	return propertyChangePermission(source.getSource(), 
																			StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME), 
																			StringArgumentType.getString(source, "permission"), false);
																})
																)
														)
												)
										)
								///// PERMISSION LIST
								.then(Commands.literal("list")
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_PROPERTY_NAMES)
												.executes(source -> {
													return propertyListPermissions(source.getSource(), StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME));
												})
												)
										)
								)

						///// RENAME OPTION
						.then(Commands.literal(CommandHelper.RENAME)
								.then(Commands.argument(CURRENT_NAME, StringArgumentType.string())
										.suggests(CommandHelper.SUGGEST_PLAYER_PROPERTY_NAMES) //SUGGEST_PROPERTY_NAMES)
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
										.suggests(CommandHelper.SUGGEST_GIVABLE_ITEMS)
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
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_PROPERTY_NAMES)
												.then(Commands.argument(CommandHelper.TARGET,EntityArgument.player())
														.executes(source -> {
															return whitelistAddPlayer(source.getSource(), 
																	StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME), EntityArgument.getPlayer(source, CommandHelper.TARGET));
														})
														)
												)
										)
								///// WHITELIST REMOVE /////
								.then(Commands.literal(CommandHelper.REMOVE)
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_PROPERTY_NAMES)
												.then(Commands.argument(CommandHelper.TARGET, StringArgumentType.string())
														.suggests(WHITELIST_NAMES)
														.executes(source -> {
															return whitelistRemovePlayer(source.getSource(), 
																	StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME), StringArgumentType.getString(source, CommandHelper.TARGET));
														})
														)
												)
										)
								///// WHITELIST LIST /////
								.then(Commands.literal(CommandHelper.LIST)
										.requires(source -> {
											return source.hasPermission(0);
										})
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_PROPERTY_NAMES)
												.executes(source -> {
													return whitelistListForProperty(source.getSource(), StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME));
												})
												)
										)
								///// WHTIELIST CLEAR /////
								.then(Commands.literal(CommandHelper.CLEAR)
										.then(Commands.argument(CommandHelper.PROPERTY_NAME, StringArgumentType.string())
												.suggests(CommandHelper.SUGGEST_PROPERTY_NAMES)
												.executes(source -> {
													return whitelistClear(source.getSource(), StringArgumentType.getString(source, CommandHelper.PROPERTY_NAME));
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

	private static int annexFief(CommandSourceStack source, String propertyName) {
		try {
			// get the owner
			ServerPlayer owner = source.getPlayerOrException();

			Optional<Property> property = CommandHelper.getPropertyByLord(owner.getUUID(), propertyName);
			if (property.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			} else if (property.get().getParent() == null || property.get().getParent().equals(UuidUtil.EMPTY_UUID)) {
				source.sendFailure(Component.translatable(LangUtil.message("property.no_parent"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
			
			Optional<Property> parent = ProtectionRegistries.property().getPropertyByUuid(property.get().getParent());
			if (parent.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.fief.no_parent"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
						
			// remove child from registry
			ProtectionRegistries.property().removeProperty(property.get());
			
			// remove child from parent
			parent.get().getChildren().remove(property.get().getUuid());
		} catch(Exception e) {
			ProtectIt.LOGGER.error("Unable to execute annexFief() command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}
		
		source.sendSuccess(Component.translatable(LangUtil.message("property.fief.annex_success"))
				.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);
		
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param propertyName
	 * @param player
	 * @return
	 */
	private static int transferLease(CommandSourceStack source, String propertyName, ServerPlayer player) {
		UUID propertyUuid = null;
		try {
			ServerPlayer landlord = source.getPlayerOrException();
			// get owned properties
			//			List<Property> properties = ProtectionRegistries.block().getProtections(player.getStringUUID());
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(player.getUUID());
			// get the entire hierarchy of properties
			properties = PropertyUtil.getPropertyHierarchy(properties);
			// filter by landowner and owner == null and propertyname
			Optional<Property> property = properties.stream().filter(p -> p.getOwner().equals(PlayerIdentity.EMPTY) && p.getLord().getUuid().equals(landlord.getUUID())
					&& p.getName().equalsIgnoreCase(propertyName)).findFirst();

			//			Optional<Property> property = CommandHelper.getPropertyByName(owner.getUUID(), propertyName);
			if (property.isPresent()) {
				propertyUuid = property.get().getUuid();
			}
			else {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown")).withStyle(ChatFormatting.RED)
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}

			// transfer ownership
			ProtectionRegistries.property().updateOwner(property.get(), new PlayerIdentity(player.getUUID(), player.getName().getString()));

		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute transferDeed() command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}
		return 1;
	}

	/**
	 * NOTE all these generate license/lease/deed generate the items the same way. create a method for them
	 * @param source
	 * @param propertyName
	 * @return
	 */
	private static int generateLease(CommandSourceStack source, String propertyName) {
		ServerPlayer player = source.getPlayer();		
		try {
			//			List<Property> properties = ProtectionRegistries.block().getProtections(player.getStringUUID());
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(player.getUUID());
			// TODO probably can make a method like CommandHelper.getPropertyByName
			properties = PropertyUtil.getPropertyHierarchy(properties);
			Optional<Property> property = properties.stream()
					.filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName) || (p.getOwner().equals(PlayerIdentity.EMPTY) &&
							p.getNameByLord().equalsIgnoreCase(propertyName)))
					.findFirst();
			if (!property.isPresent()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown")).withStyle(ChatFormatting.RED)
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}

			// create item stack
			ItemStack itemStack = new ItemStack(ModItems.FIEF_DEED.get());

			// set tag properties of stack
			CompoundTag tag = itemStack.getOrCreateTag();
			tag.putUUID(Deed.OWNER_ID_KEY, player.getUUID());
			tag.putString(Deed.OWNER_NAME_KEY, player.getName().getString());
			tag.putUUID(Deed.PROPERTY_ID_KEY, property.get().getUuid());
			tag.putString(Deed.PROPERTY_NAME_KEY, propertyName);
			CompoundTag boxTag = new CompoundTag();
			property.get().getBox().save(boxTag);
			tag.put("propertyBox", boxTag);

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
			ProtectIt.LOGGER.error("Unable to execute generateLease() command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param owner
	 * @param string
	 * @param player
	 * @return
	 */
	private static int transferDeed(CommandSourceStack source, String propertyName, ServerPlayer player) {

		UUID propertyUuid = null;
		try {
			ServerPlayer owner = source.getPlayerOrException();
			Optional<Property> property = CommandHelper.getPropertyByName(owner.getUUID(), propertyName);
			if (property.isPresent()) {
				propertyUuid = property.get().getUuid();
			}
			else {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown")).withStyle(ChatFormatting.RED)
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}

			// transfer ownership
			ProtectionRegistries.property().updateOwner(property.get(), new PlayerIdentity(player.getUUID(), player.getName().getString()));

		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute transferDeed() command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}
		return 1;
	}

	/**
	 * NOTE propertyName is requried for a Deed.
	 * @param source
	 * @param player
	 * @param propertyName
	 * @return
	 */
	private static int generateDeed(CommandSourceStack source, String propertyName) {
		try {
			ServerPlayer player = source.getPlayerOrException();

			Optional<Property> property = CommandHelper.getPropertyByName(player.getUUID(), propertyName);
			if (!property.isPresent()) {
				// TODO error
				return 1;
			}

			// test against the TransactionRegistry
			if (TransactionRegistry.getDeedsCount(property.get().getUuid()) > 0) {
				source.sendFailure(Component.translatable(LangUtil.message("property.deed.exceeded_limit"))
						.withStyle(ChatFormatting.RED));
				return 1;
			}

			// create item stack
			ItemStack itemStack = new ItemStack(ModItems.PROPERTY_DEED.get());

			// set tag properties of stack
			CompoundTag tag = itemStack.getOrCreateTag();
			tag.putUUID(Deed.OWNER_ID_KEY, player.getUUID());
			tag.putString(Deed.OWNER_NAME_KEY, player.getName().getString());
			tag.putUUID(Deed.PROPERTY_ID_KEY, property.get().getUuid());
			tag.putString(Deed.PROPERTY_NAME_KEY, propertyName);
			CompoundTag boxTag = new CompoundTag();
			property.get().getBox().save(boxTag);
			tag.put("propertyBox", boxTag);

			// give to calling player
			ServerPlayer giver = source.getPlayerOrException();
			if (!giver.getInventory().add(itemStack)) {
				ItemEntity itemEntity = giver.drop(itemStack, false);
				if (itemEntity != null) {
					itemEntity.setNoPickUpDelay();
					itemEntity.setOwner(giver.getUUID());
				}
			}

			// increment transaction registry
			TransactionRegistry.sellDeed(property.get().getUuid());

		} catch (Exception e) {
			ProtectIt.LOGGER.error("Unable to execute generateDeed() command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param propertyName
	 * @param pos
	 * @param pos2
	 * @param targetOwner
	 * @return
	 */
	private static int createFief(CommandSourceStack source, String propertyName, BlockPos pos, BlockPos pos2, ServerPlayer targetOwner) {
		return createFief(source, propertyName, pos, pos2, new PlayerIdentity(targetOwner.getUUID(), targetOwner.getName().getString()));
	}

	/**
	 * 
	 * @param source
	 * @param propertyName
	 * @param pos
	 * @param pos2
	 * @param targetOwner
	 * @return
	 */
	private static int createFief(CommandSourceStack source, String propertyName, BlockPos pos, BlockPos pos2, PlayerIdentity targetOwner) {
		ProtectIt.LOGGER.debug("executing createFief command...");

		try {
			// get the owner
			ServerPlayer owner = source.getPlayerOrException();

			// get property by name
			Optional<Property> parent = CommandHelper.getPropertyByName(owner.getUUID(), propertyName);
			if (parent.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}

			ICoords coords1 = new Coords(pos);
			ICoords coords2 = new Coords(pos2);

			// check that pos2 > pos1
			Optional<Tuple<ICoords, ICoords>> validCoords = CommandHelper.validateCoords(coords1, coords2);
			if (!validCoords.isPresent()) {
				source.sendSuccess(Component.translatable(LangUtil.message("invalid_coords_format"))
						.withStyle(ChatFormatting.RED), true);
				return 1;
			}

			Property target = parent.get();

			// is the property a fiefdom
			if (!target.isFiefdom()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.fiefdom.not_granted"))
						.withStyle(ChatFormatting.RED));
				return 1;
			}

			// within bounds of target property
			if (isOutsideBoundary(coords1, target.getBox()) || isOutsideBoundary(coords2, target.getBox())) {
				source.sendFailure(Component.translatable(LangUtil.message("property.fief.outside_property_boundary"))
						.withStyle(ChatFormatting.RED));
				return 1;
			}

			// create a box from the valid coords
			Box box = new Box(validCoords.get().getA(), validCoords.get().getB());

			// doesn't over any sibling properties
			for (Optional<Property> c : target.getChildren().stream().map(uuid -> ProtectionRegistries.property().getPropertyByUuid(uuid)).toList()) { //target.getChildren()) {
				if (c.isPresent() && c.get().intersects(box)) {
					source.sendFailure(Component.translatable(LangUtil.message("property.fief.intersects_property"))
							.withStyle(ChatFormatting.RED));
					return 1;
				}
			}

			// create a name for the new subdivision property
			String name1 = target.getNameByOwner() + "." + (target.getChildren().size() + 1);
			String name2;
			ProtectIt.LOGGER.debug("landlord name -> {}", name1);
			if (target.getNameByOwner().equalsIgnoreCase(targetOwner.getName())) {
				name2 = name1;
				ProtectIt.LOGGER.debug("property name -> {}", name1);
			} else {
				name2 = (StringUtils.isEmpty(targetOwner.getName()) ? target.getNameByOwner() : targetOwner.getName())
						+ "." + (ProtectionRegistries.property().getPropertiesByOwner(targetOwner.getUuid()).size() + 1);
				ProtectIt.LOGGER.debug("property name -> {}", name1);
			}

			// create the new property
			Property property = new Property(
					box.getMinCoords(),
					box,
					targetOwner,
					name2);
			property.setParent(target.getUuid());
			property.setLord(target.getOwner());
			property.setNameByLord(name1);
			property.setCreateTime(source.getLevel().getGameTime());

			Optional<Property> finalProperty = ProtectionRegistries.property().addFief(target, property);
			if (finalProperty.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
						.withStyle(ChatFormatting.RED));
				return 1;
			}

			// save world data
			CommandHelper.saveData(source.getLevel());

			source.sendSuccess(Component.translatable(LangUtil.message("property.fief.success"))
					.append(Component.translatable(propertyName).withStyle(ChatFormatting.AQUA)), false);

			// send message to subdivide protection on all clients
			if(source.getLevel().getServer().isDedicatedServer()) {
				AddFiefS2CPush2 message = new AddFiefS2CPush2(
						target.getUuid(),
						property.getLord().getUuid(),
						owner.getUUID(),
						property.getUuid(),
						property.getBox()
						);
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}
		} catch(Exception e) {
			ProtectIt.LOGGER.error("Unable to execute subdivide command:", e);
			source.sendFailure(Component.translatable(LangUtil.message("unexcepted_error"))
					.withStyle(ChatFormatting.RED));
		}

		return 1;
	}


	private static boolean isOutsideBoundary(ICoords coords1, Box box) {
		return coords1.getX() < box.getMinCoords().getX() || coords1.getZ() < box.getMinCoords().getZ() || coords1.getY() < box.getMinCoords().getY() ||
				coords1.getX() > box.getMaxCoords().getX() || coords1.getZ() > box.getMaxCoords().getZ() || coords1.getY() > box.getMaxCoords().getY();
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
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner.getUUID());
			// get the named property
			List<Property> namedProperties = properties.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
			if (namedProperties.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
			Property property = namedProperties.get(0);

			List<Component> messages = new ArrayList<>();
			messages.add(Component.literal(""));
			messages.add(Component.translatable(LangUtil.message("property.permission.list"))
					.withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE)
					.append(propertyName).withStyle(ChatFormatting.AQUA));
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
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner.getUUID());
			// get the named property
			List<Property> namedProperties = properties.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
			if (namedProperties.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
			Property property = namedProperties.get(0);

			// update permission on property
			property.setPermission(Permission.valueOf(permissionName).value, value);
			CommandHelper.saveData(source.getLevel());

			//send update to client
			if(source.getLevel().getServer().isDedicatedServer()) {
				PermissionChangeS2CPush message = new PermissionChangeS2CPush(
						owner.getUUID(),
						property.getUuid(),
						Permission.valueOf(permissionName).value,
						value
						);
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}

			source.sendSuccess(Component.translatable(LangUtil.message("property.permission.change_success"))
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
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner.getUUID());
			// get the named property
			List<Property> namedProperties = properties.stream().filter(prop -> prop.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
			if (namedProperties.isEmpty()) {
				source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
						.append(Component.translatable(propertyName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
				return 1;
			}
			Property property = namedProperties.get(0);
			// update property whitelist with player
			if (property.getWhitelist().stream().noneMatch(data -> data.getName().equalsIgnoreCase(player.getDisplayName().getString()))) {
				property.getWhitelist().add(new PlayerIdentity(player.getUUID(), player.getDisplayName().getString()));
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
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
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
			List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner.getUUID());
			// get the named property
			List<Property> names = properties.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
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
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
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

			//			Property property = namedProperties.get(0);
			property.get().getWhitelist().clear();
			CommandHelper.saveData(source.getLevel());

			//send update to client
			if(source.getLevel().getServer().isDedicatedServer()) {
				WhitelistClearS2CPush message = new WhitelistClearS2CPush(
						owner.getUUID(),
						property.get().getUuid()
						);
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
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
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(player.getUUID());
		List<Property> namedProperties = properties.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
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
