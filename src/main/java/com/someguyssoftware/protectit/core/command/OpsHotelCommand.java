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
package com.someguyssoftware.protectit.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.someguyssoftware.protectit.core.config.Config;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * 
 * @author Mark Gottschling Feb 22, 2023
 *
 */
public class OpsHotelCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("hotel").requires(source -> {
			return source.hasPermission(Config.GENERAL.opsPermissionLevel.get());
		})
				///// ADD
				.then(Commands.literal(CommandHelper.ADD)
						.then(Commands.argument(CommandHelper.POS2, BlockPosArgument.blockPos())
								.then(Commands.argument(CommandHelper.POS2, BlockPosArgument.blockPos())
										.then(Commands.argument(CommandHelper.TARGET, EntityArgument.player())
												.executes(source -> {
													return addHotel(source.getSource(),
															BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS),
															BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS2),
															EntityArgument.getPlayer(source, CommandHelper.TARGETS));
												})
												)
										)
								)
						) // end of ADD then

				///// SET
				.then(Commands.literal("set")

						) // end of SET

				///// REMOVE
				.then(Commands.literal(CommandHelper.REMOVE)

						) // end of REMOVE
				
				///// GIVE
				.then(Commands.literal(CommandHelper.GIVE)
						// TODO add deed
						// TODO add ownership
						) // end of GIVE
				); // end of register
	}

	/**
	 * 
	 * @param source
	 * @param loadedBlockPos
	 * @param loadedBlockPos2
	 * @param players
	 * @return
	 */
	private static int addHotel(CommandSourceStack source, BlockPos pos, BlockPos pos2,
			ServerPlayer renter) {

		// NOTE the 'renter' is the owner of the hotel
		// NOTE the 'owner' is the owner of the encapsulating property
		
		// TODO get the property for pos <--> pos2
		//		error if spans more than 1 property
		
		// TODO check if area is already hoteled unless totally inside a hotel property
		
		// TODO check if area has reached its max nesting
		
		// call Registry.addHotel(property, pos1, pos2, renter)
		
		return 1;
	}


}