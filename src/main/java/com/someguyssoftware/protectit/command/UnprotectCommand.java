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

import com.mojang.brigadier.CommandDispatcher;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.registry.ProtectionRegistry;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

/**
 * 
 * @author Mark Gottschling on Sep 15, 2021
 *
 */
public class UnprotectCommand {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher
		.register(Commands.literal("unprotect")
				.requires(source -> {
					return source.hasPermission(2);
				})
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(source -> {
							return unprotect(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, "pos"));
						})
						.then(Commands.argument("pos2", BlockPosArgument.blockPos())
								.executes(source -> {
									return protect(source.getSource(), BlockPosArgument.getOrLoadBlockPos(source, "pos"), BlockPosArgument.getOrLoadBlockPos(source, "pos2"));
								})
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
	private static int unprotect(CommandSource source, BlockPos pos) {
		ProtectionRegistry.removeProtection(pos);
		// save world data
		ServerWorld world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
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
	private static int protect(CommandSource source, BlockPos pos, BlockPos pos2) {
		ProtectionRegistry.removeProtection(pos, pos2);
		// save world data
		ServerWorld world = source.getLevel();
		ProtectItSavedData savedData = ProtectItSavedData.get(world);
		if (savedData != null) {
			savedData.setDirty();
		}
		return 1;
	}
}
