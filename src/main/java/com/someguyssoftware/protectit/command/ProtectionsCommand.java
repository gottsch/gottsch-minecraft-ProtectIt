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
package com.someguyssoftware.protectit.command;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.someguyssoftware.protectit.registry.ProtectionRegistry;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

/**
 * 
 * @author Mark Gottschling on Sep 16, 2021
 *
 */
public class ProtectionsCommand {

	/**
	 * 
	 * @param dispatcher
	 */
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher
		.register(Commands.literal("protections")
				.requires(source -> {
					return source.hasPermission(2);
				})
				.executes(source -> {
					return protections(source.getSource());
				})
				);
	}

	/**
	 * 
	 * @param source
	 * @param pos
	 * @return
	 */
	private static int protections(CommandSource source) {
		List<String> list = ProtectionRegistry.list();
		list.forEach(element -> {
	         source.sendSuccess(new StringTextComponent(element), true);
		});
		return 1;
	}
}
