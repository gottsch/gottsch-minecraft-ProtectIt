/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.protectit.core.registry;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

/**
 * NOTE  Deeds have a count limit of 1.
 * Leases do not have a limit.
 * @author Mark Gottschling Mar 6, 2023
 *
 */
public class TransactionRegistry {
	private static final Map<UUID, Integer> DEEDS_COUNT = Maps.newHashMap();
	private static final Map<UUID, Integer> LEASES_COUNT = Maps.newHashMap();
	
	/**
	 * 
	 */
	private TransactionRegistry() {}
	
	public static Integer getDeedsCount(UUID uuid) {
		if (DEEDS_COUNT.containsKey(uuid)) {
			return DEEDS_COUNT.get(uuid);
		}
		return 0;
	}
	
	public static Integer getLeasesCount(UUID uuid) {
		if (LEASES_COUNT.containsKey(uuid)) {
			return LEASES_COUNT.get(uuid);
		}
		return 0;
	}
	
	public static boolean sellDeed(UUID uuid) {
		if (getDeedsCount(uuid) == 0) {
			increment(DEEDS_COUNT, uuid);
			return true;
		}
		return false;
	}
	
	public static boolean buyDeed(UUID uuid) {
		if (DEEDS_COUNT.containsKey(uuid)) {
			decrement(DEEDS_COUNT, uuid);
			return true;
		}
		return false;
	}
	
	public static boolean sellLease(UUID uuid) {
		if (getLeasesCount(uuid) == 0) {
			increment(LEASES_COUNT, uuid);
			return true;
		}
		return false;
	}
	
	public static boolean buyLease(UUID uuid) {
		if (LEASES_COUNT.containsKey(uuid)) {
			decrement(LEASES_COUNT, uuid);
			return true;
		}
		return false;
	}
	
	private static void increment(Map<UUID, Integer> registry, UUID uuid) {
		if (!registry.containsKey(uuid)) {
			registry.put(uuid, Integer.valueOf(1));
		} else {
			registry.put(uuid, Integer.valueOf(registry.get(uuid) + 1));
		}
	}
	
	private static void decrement(Map<UUID, Integer> registry, UUID uuid) {
		if (registry.containsKey(uuid)) {
			int value = Math.max(registry.get(uuid)-1, 0);
			if (value == 0) {
				registry.remove(uuid);
			} else {
				registry.put(uuid, value);
			}
		}
	}
	
	public static void resetDeed(UUID uuid) {
		if (DEEDS_COUNT.containsKey(uuid)) {
			DEEDS_COUNT.remove(uuid);
		}
	}
	
	public static void setDeed(UUID uuid) {
		DEEDS_COUNT.put(uuid, 1);
	}
	
	public static void resetLease(UUID uuid) {
		if (LEASES_COUNT.containsKey(uuid)) {
			LEASES_COUNT.remove(uuid);
		}
	}
	
	public static void setLease(UUID uuid, Integer value) {
		LEASES_COUNT.put(uuid, value);
	}
	
	public static void clear() {
		DEEDS_COUNT.clear();
		LEASES_COUNT.clear();
	}
}
