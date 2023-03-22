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
package mod.gottsch.forge.protectit.core.zone;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import mod.gottsch.forge.protectit.core.property.IPermission;

/**
 * 
 * @author Mark Gottschling Mar 21, 2023
 *
 */
public enum ZonePermission implements IPermission {
	MOB_PVP_PERMISSION(0),
	MOB_SPAWN_PERMISSION(1),
	PLAYER_PVP_PERMISSION(2);

	private static final Map<Integer, IPermission> values = new HashMap<Integer, IPermission>();
	public int value;

	// setup reverse lookup
	static {
		for (ZonePermission type : EnumSet.allOf(ZonePermission.class)) {
			values.put(type.value, type);
		}
	}

	ZonePermission(int value) {
		this.value = value;
	}

	public static ZonePermission getByValue(Integer value) {
		return (ZonePermission) values.get(value);
	}

	/**
	 * 
	 * @return
	 */
	public static List<String> getNames() {
		List<String> names = EnumSet.allOf(ZonePermission.class).stream().map(x -> x.name()).collect(Collectors.toList());
		return names;
	}
}