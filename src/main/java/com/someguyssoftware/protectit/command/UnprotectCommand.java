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
package com.someguyssoftware.protectit.command;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;


/**
 * TODO need to add uuid in suggestions as opposed to Entity. so would need an additional literal argument (-entity | -uuid)
 * @author Mark Gottschling on Sep 15, 2021
 *
 */
@Deprecated
public class UnprotectCommand {
	private static final String POS = "pos";
	private static final String POS2 = "pos2";
	private static final String UUID = "uuid";
	private static final String TARGETS = "targets";

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_UUID = (source, builder) -> {
		// get all uuids from registry
//		return ISuggestionProvider.suggest(ProtectionRegistries.block().find(p -> !p.getData().getOwner().getUuid().isEmpty()).stream()
//				.map(i -> String.format("%s[%s]", 
//						(i.getData().getOwner().getName() == null) ? "" : i.getData().getOwner().getName(),
//								(i.getData().getOwner().getUuid() == null) ? "" : i.getData().getOwner().getUuid())), builder);
		return null;
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher
		.register(Commands.literal("unprotect")
				.requires(source -> {
					return source.hasPermission(4);
				})
				.then(Commands.literal(UUID)
						.then(Commands.argument(UUID, StringArgumentType.string())
								.suggests(SUGGEST_UUID)
								.executes(source -> {
									return unprotect(source.getSource(), StringArgumentType.getString(source, UUID));							
								})
								)
						)
				.then(Commands.literal("pos")
						.then(Commands.argument(POS, BlockPosArgument.blockPos())
								.executes(source -> {
									return unprotect(source.getSource(), BlockPosArgument.getLoadedBlockPos(source, POS));
								})
								.then(Commands.argument("pos2", BlockPosArgument.blockPos())
										.executes(source -> {
											return unprotect(source.getSource(), BlockPosArgument.getLoadedBlockPos(source, POS), BlockPosArgument.getLoadedBlockPos(source, "pos2"));
										})
										.then(Commands.literal(UUID)
												.then(Commands.argument(UUID, StringArgumentType.string())
														.suggests(SUGGEST_UUID)
														.executes(source -> {
															return unprotect(source.getSource(), BlockPosArgument.getLoadedBlockPos(source, POS), BlockPosArgument.getLoadedBlockPos(source, POS2), StringArgumentType.getString(source, UUID));							
														})
														)
												)
										.then(Commands.literal("entity")
												.then(Commands.argument(TARGETS, EntityArgument.entities())
														.executes(source -> {
															return unprotect(source.getSource(), BlockPosArgument.getLoadedBlockPos(source, POS), BlockPosArgument.getLoadedBlockPos(source, POS2), EntityArgument.getEntities(source, TARGETS));							
														})
														)													
												)
										)
								)
						)

				);
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @return
	 */
	private static int unprotect(CommandSourceStack source, BlockPos pos) {
		return unprotect(source, pos, pos);
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @param pos2
	 * @return
	 */
	private static int unprotect(CommandSourceStack source, BlockPos pos, BlockPos pos2) {
		// first, check that pos2 > pos1
		Optional<Tuple<ICoords, ICoords>> validCoords = CommandUtils.validateCoords(new Coords(pos), new Coords(pos2));
		if (!validCoords.isPresent()) {
			source.sendSuccess(new TranslatableComponent("message.protectit.invalid_coords_format"), true);
			return 1;
		}

		ProtectionRegistries.block().removeProtection(validCoords.get().getA(), validCoords.get().getB());
		// save world data
		ServerLevel world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
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
	private static int unprotect(CommandSourceStack source, BlockPos pos, BlockPos pos2, String uuid) {
		// first, check that pos2 > pos1
		Optional<Tuple<ICoords, ICoords>> validCoords = CommandUtils.validateCoords(new Coords(pos), new Coords(pos2));
		if (!validCoords.isPresent()) {
			source.sendSuccess(new TranslatableComponent("message.protectit.invalid_coords_format"), true);
			return 1;
		}

		// parse out the uuid
		if (uuid.contains("[")) {
			Pattern p = Pattern.compile("\\[(.*?)\\]");
			Matcher m = p.matcher(uuid);
			m.find();
			// get first occurence
			uuid = m.group(1);
		}
		ProtectionRegistries.block().removeProtection(validCoords.get().getA(), validCoords.get().getB(), uuid);
		return 1;
	}

	/**
	 * Unprotect all protections where player == uuid 
	 * @param source
	 * @param uuid
	 * @return
	 */
	private static int unprotect(CommandSourceStack source, String uuid) {		
		ProtectionRegistries.block().removeProtection(uuid);
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @param pos2
	 * @return
	 */
	private static int unprotect(CommandSourceStack source, BlockPos pos, BlockPos pos2, Collection<? extends Entity> entities) {

		// first, check that pos2 > pos1
		Optional<Tuple<ICoords, ICoords>> validCoords = CommandUtils.validateCoords(new Coords(pos), new Coords(pos2));
		if (!validCoords.isPresent()) {
			source.sendSuccess(new TranslatableComponent("message.protectit.invalid_coords_format"), true);
			return 1;
		}

		String uuid = "";
		Entity entity = entities.iterator().next();
		ProtectIt.LOGGER.debug("entity -> {}", entity);

		if (entity instanceof Player) {
			ProtectIt.LOGGER.debug("player entity -> {}", ((Player)entity).getDisplayName());
			Player player = (Player)entity;
			uuid = player.getStringUUID();				
		}
		ProtectionRegistries.block().removeProtection(validCoords.get().getA(), validCoords.get().getB(), uuid);


		//		ProtectionRegistries.getRegistry().removeProtection(validCoords.get().getA(), validCoords.get().getB(), uuid);
		// save world data
		ServerLevel world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
		}
		return 1;
	}
}
