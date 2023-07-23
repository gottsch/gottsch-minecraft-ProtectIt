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

import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.property.Property;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

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
	
	public synchronized static CompoundTag save(CompoundTag tag) {
		ProtectIt.LOGGER.debug("saving registry...");

		if (!DEEDS_COUNT.isEmpty()) {
			ListTag list = new ListTag();
			DEEDS_COUNT.forEach((uuid, count) -> {
				if (count > 0) {
					CompoundTag deedTag = new CompoundTag();
					deedTag.putUUID("uuid", uuid);
					deedTag.putInt("count", count);
					list.add(deedTag);
				}
			});
			tag.put("deeds", list);
		}
		
		if (!LEASES_COUNT.isEmpty()) {
			ListTag list = new ListTag();
			LEASES_COUNT.forEach((uuid, count) -> {
				if (count > 0) {
					CompoundTag leaseTag = new CompoundTag();
					leaseTag.putUUID("uuid", uuid);
					leaseTag.putInt("count", count);
					list.add(leaseTag);
				}
			});
			tag.put("leases", list);
		}
		return tag;
	}
	
	public synchronized static void load(CompoundTag tag) {
		ProtectIt.LOGGER.debug("loading registry...");
		clear();

		if (tag.contains("deeds")) {
			ListTag list = tag.getList("deeds", CompoundTag.TAG_COMPOUND);
			list.forEach(element -> {
				CompoundTag deedTag = (CompoundTag)element;
				if (deedTag.contains("uuid") && deedTag.contains("count")) {
					DEEDS_COUNT.put(deedTag.getUUID("uuid"), deedTag.getInt("count"));
				}
			});
		}
		
		if (tag.contains("leases")) {
			ListTag list = tag.getList("leases", CompoundTag.TAG_COMPOUND);
			list.forEach(element -> {
				CompoundTag leaseTag = (CompoundTag)element;
				if (leaseTag.contains("uuid") && leaseTag.contains("count")) {
					LEASES_COUNT.put(leaseTag.getUUID("uuid"), leaseTag.getInt("count"));
				}
			});
		}
	}
	
	public static void dump() {
		Path path = Paths.get("config", ProtectIt.MODID, "dumps").toAbsolutePath();
		if (Files.notExists(path)) { 
			try {
				Files.createDirectories(path);
			} catch (Exception e) {
				ProtectIt.LOGGER.error("unable to create dump file: ", e);
			}
		}

		GsonBuilder gsonBuilder = new GsonBuilder();		
		try {
			Type listType = new TypeToken<Map<UUID, Integer>>() {}.getType();
			Gson gson = gsonBuilder.setPrettyPrinting().create();

			String json = gson.toJson(DEEDS_COUNT, listType);
			FileWriter fw = new FileWriter(path.resolve("domain-transaction-registry.json").toAbsolutePath().toString());
			fw.write(json);
			fw.close();

			json = gson.toJson(LEASES_COUNT, listType);
			fw = new FileWriter(path.resolve("fief-transaction-registry.json").toAbsolutePath().toString());
			fw.write(json);
			fw.close();
			
		} catch (Exception e) {
			ProtectIt.LOGGER.error("error writing protection registry to file -> ", e);
		}
	}
}
