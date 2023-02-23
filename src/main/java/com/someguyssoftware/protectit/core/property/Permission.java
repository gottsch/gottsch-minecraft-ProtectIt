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
package com.someguyssoftware.protectit.core.property;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 
 * @author Mark Gottschling Feb 21, 2023
 *
 */
public enum Permission implements IPermission {
	BLOCK_BREAK_PERMISSION(0),
	BLOCK_PLACE_PERMISSION(1),
	MULTIBLOCK_PLACE_PERMISSION(2),
	TOOL_PERMISSION(3),
	INTERACT_PERMISSION(4),
	INVENTORY_INTERACT_PERMISSION(5), // TODO test and check for event
	DOOR_INTERACT_PERMISSION(6);

	private static final Map<Integer, IPermission> values = new HashMap<Integer, IPermission>();
	public int value;

	// setup reverse lookup
	static {
		for (Permission type : EnumSet.allOf(Permission.class)) {
			values.put(type.value, type);
		}
	}

	Permission(int value) {
		this.value = value;
	}

	public static Permission getByValue(Integer value) {
		return (Permission) values.get(value);
	}

	/**
	 * 
	 * @return
	 */
	public static List<String> getNames() {
		List<String> names = EnumSet.allOf(Permission.class).stream().map(x -> x.name()).collect(Collectors.toList());
		return names;
	}
}