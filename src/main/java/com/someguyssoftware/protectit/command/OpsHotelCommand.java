package com.someguyssoftware.protectit.command;

import com.mojang.brigadier.CommandDispatcher;
import com.someguyssoftware.protectit.config.Config;

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
		
		// call Registry.registerHotel(property, pos1, pos2, renter)
		
		return 1;
	}


}
