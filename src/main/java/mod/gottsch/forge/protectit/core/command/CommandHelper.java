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
package mod.gottsch.forge.protectit.core.command;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.item.ModItems;
import mod.gottsch.forge.protectit.core.persistence.ProtectItSavedData;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.property.PropertyUtil;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import mod.gottsch.forge.protectit.core.zone.Zone;
import mod.gottsch.forge.protectit.core.zone.ZoneUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 
 * @author Mark Gottschling Feb 18, 2023
 *
 */
public class CommandHelper {
	public static final String PROPERTY = "property";
	public static final String BLOCK = "block";
	public static final String ADD = "add";
	public static final String CLEAR = "clear";
	public static final String LIST = "list";
	public static final String RENAME = "rename";
	public static final String WHITELIST = "whitelist";
	public static final String REMOVE ="remove";

	public static final String POS = "pos";
	public static final String POS2 = "pos2";
	public static final String TARGET = "target";
	public static final String TARGETS = "targets";
	public static final String UUID = "uuid";
	public static final String PROPERTY_NAME = "property_name";
	public static final String PERMISSIONS = "permissions";
	
	public static final String ZONE_NAME = "zone_name";
	
	public static final String GIVE = "give";
	public static final String GIVE_ITEM = "giveItem";

	///// SUGGESTIONS /////
	static final SuggestionProvider<CommandSourceStack> SUGGEST_PLAYER_NAMES = (source, builder) -> {
		List<String> names = source.getSource().getLevel().getServer().getPlayerList().getPlayers().stream().map(p -> p.getName().getString()).toList();
		return SharedSuggestionProvider.suggest(names, builder);
	};

//	static final SuggestionProvider<CommandSourceStack> SUGGEST_UUID = (source, builder) -> {
//		return SharedSuggestionProvider.suggest(ProtectionRegistries.block().findPropertiesBy(p -> !p.getOwner().getUuid().isEmpty()).stream()
//				.map(i -> String.format("%s [%s]", 
//						(i.getOwner().getName() == null) ? "" : i.getOwner().getName(),
//								(i.getOwner().getUuid() == null) ? "" : i.getOwner().getUuid())), builder);
//	};

	static final SuggestionProvider<CommandSourceStack> SUGGEST_GIVABLE_ITEMS = (source, builder) -> {
		List<String> items = Arrays.asList(
				"Property Lever", 
				"Remove Claim Stake"
				);
		return SharedSuggestionProvider.suggest(items, builder);
	};

	static final SuggestionProvider<CommandSourceStack> SUGGEST_PROPERTY_NAMES = (source, builder) -> {
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(source.getSource().getPlayerOrException().getUUID());
//		List<Property> properties = ProtectionRegistries.block().getProtections(source.getSource().getPlayerOrException().getStringUUID());
		List<String> names = properties.stream().map(p -> p.getNameByOwner()).collect(Collectors.toList());
		return SharedSuggestionProvider.suggest(names, builder);
	};

	static final SuggestionProvider<CommandSourceStack> SUGGEST_TARGET_PROPERTY_NAMES = (source, builder) -> {
		ServerPlayer player = source.getSource().getPlayerOrException();
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(source.getSource().getPlayerOrException().getUUID());
		List<String> names = properties.stream().map(p -> p.getName(player.getUUID())).collect(Collectors.toList());
		return SharedSuggestionProvider.suggest(names, builder);
	};
	
	static final SuggestionProvider<CommandSourceStack> SUGGEST_DEED_PROPERTY_NAMES = (source, builder) -> {
		ServerPlayer player = source.getSource().getPlayerOrException();
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(source.getSource().getPlayerOrException().getUUID());
		List<String> names = properties.stream().filter(p -> p.isDomain()).map(p -> p.getName(player.getUUID())).collect(Collectors.toList());
		return SharedSuggestionProvider.suggest(names, builder);
	};
	
	static final SuggestionProvider<CommandSourceStack> SUGGEST_FIEF_NAMES_BY_OWNER = (source, builder) -> {
		ServerPlayer player = source.getSource().getPlayerOrException();
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(source.getSource().getPlayerOrException().getUUID());
		List<String> names = properties.stream().filter(p -> !p.isDomain()).map(p -> p.getName(player.getUUID())).collect(Collectors.toList());
		return SharedSuggestionProvider.suggest(names, builder);
	};
	
	static final SuggestionProvider<CommandSourceStack> SUGGEST_FIEF_NAMES_BY_LANDLORD = (source, builder) -> {
		List<Property> properties = ProtectionRegistries.property().getPropertiesByLord(source.getSource().getPlayerOrException().getUUID());
		List<String> names = properties.stream().filter(p -> !p.isDomain()).map(p -> p.getNameByLord()).collect(Collectors.toList());
		return SharedSuggestionProvider.suggest(names, builder);
	};
	
	// REVIEW
	// NEEDS to get ALL the properties that you are owner or lord of.
	static final SuggestionProvider<CommandSourceStack> SUGGEST_PLAYER_PROPERTY_NAMES = (source, builder) -> {
		ServerPlayer player = source.getSource().getPlayerOrException();
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(player.getUUID());
		ProtectionRegistries.property().getPropertiesByLord(player.getUUID()).stream().forEach(p -> {
			if (!properties.contains(p)) {
				properties.add(p);
			}
		});
		List<String> names = properties.stream().map(p -> p.getName(player.getUUID())).collect(Collectors.toList());
		return SharedSuggestionProvider.suggest(names, builder);
	};

	//	static final SuggestionProvider<CommandSourceStack> SUGGEST_NESTED_PROPERTY_NAMES = (source, builder) -> {
	//		List<Property> properties = ProtectionRegistries.block().getProtections(source.getSource().getPlayerOrException().getStringUUID());
	//		List<String> names = new ArrayList<>();
	//		properties.forEach(p -> {
	//			buildName(p, "", names);
	//		});
	//		//		List<String> names = properties.stream().map(p -> p.getName().toUpperCase()).collect(Collectors.toList());
	//		return SharedSuggestionProvider.suggest(names, builder);
	//	};

	@Deprecated
	static final SuggestionProvider<CommandSourceStack> SUGGEST_ALL_NESTED_PROPERTY_NAMES = (source, builder) -> {
		ServerPlayer player = source.getSource().getPlayerOrException();
//		List<Property> properties = ProtectionRegistries.block().getProtections(player.getStringUUID());
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(source.getSource().getPlayerOrException().getUUID());
		List<String> names = PropertyUtil.getPropertyHierarchyNames(properties, player);
		return SharedSuggestionProvider.suggest(names, builder);
	};
	
	static final SuggestionProvider<CommandSourceStack> SUGGEST_ZONE_NAMES = (source, builder) -> {
		List<Zone> zones = ProtectionRegistries.pvp().getAll();
		List<String> names = zones.stream().map(p -> p.getName()).collect(Collectors.toList());
		return SharedSuggestionProvider.suggest(names, builder);
	};

	/**
	 * 
	 * @param source
	 * @return
	 */
	static int unavailable(CommandSourceStack source) {
		source.sendSuccess(Component.translatable(LangUtil.message("option_unavailable")), false);
		return 1;
	}



	/**
	 * 
	 * @param source
	 * @param oldName
	 * @param newName
	 * @return
	 */
	public static int rename(CommandSourceStack source, String oldName, String newName) {
		ServerPlayer player = null;
		try {
			player = source.getPlayerOrException();
		}
		catch(CommandSyntaxException 	e) {
			source.sendFailure(Component.translatable(LangUtil.message("unable_locate_player")));
			return 1;
		}
		List<Property> owners = ProtectionRegistries.property().getPropertiesByOwner(player.getUUID());
		ProtectIt.LOGGER.debug("owner props -> {}", owners);
		List<Property> namedProperties = owners.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(oldName)).toList();
		if (namedProperties.isEmpty()) {
			List<Property> lords = ProtectionRegistries.property().getPropertiesByLord(player.getUUID());
			ProtectIt.LOGGER.debug("lord props -> {}", lords);
			namedProperties = lords.stream().filter(p -> p.getNameByLord().equalsIgnoreCase(oldName)).toList();
		}

		//		List<Property> claims = ProtectionRegistries.block().getProtections(player.getStringUUID());
		//		List<Property> namedClaims = claims.stream().filter(claim -> claim.getNameByOwner().equalsIgnoreCase(oldName)).collect(Collectors.toList());
		if (namedProperties.isEmpty()) {
			source.sendFailure(Component.translatable(LangUtil.message("property.name.unknown"))
					.append(Component.translatable(oldName.toUpperCase()).withStyle(ChatFormatting.RED)));
			return 1;
		}
		
		ProtectIt.LOGGER.debug("named claim -> {}", namedProperties.get(0));
		// checked against existing names
		boolean alreadyExists = false;
		for (Property p : namedProperties) {
			if (p.getNameByOwner().equalsIgnoreCase(newName) || p.getNameByLord().equalsIgnoreCase(newName)) {
				alreadyExists = true;
			}
		}
		if (alreadyExists) {
			source.sendFailure(Component.translatable(LangUtil.message("property.name.conflict"))
					.append(Component.translatable(oldName.toUpperCase()).withStyle(ChatFormatting.RED)));
			return 1;
		}
		
		namedProperties.get(0).setName(player.getUUID(), newName.toUpperCase());

		source.sendSuccess(Component.translatable(LangUtil.message("property.rename.success"))
				.append(Component.translatable(newName.toUpperCase()).withStyle(ChatFormatting.AQUA)), false);

		saveData(source.getLevel());
		// NOTE it is not necessary to send message to client as rename() method is called from server
		// and server registry is used to lookup rename() etc.	

		return 1;
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	public static int listProperties(CommandSourceStack source) {
		try {
			ServerPlayer 	player = source.getPlayerOrException();		
			return listProperties(source, player);
		}
		catch(CommandSyntaxException 	e) {
			source.sendFailure(Component.translatable(LangUtil.message("unable_locate_player")));
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @param player
	 * @return
	 */
	public static int listProperties(CommandSourceStack source, ServerPlayer player) {
		List<Component> components = PropertyUtil.listProperties(player);
		components.forEach(component -> {
			source.sendSuccess(component, false);
		});
		return 1;
	}

	public static int listProperties(CommandSourceStack source, String player) {
		// get the player
		try {
			GameProfileCache cache = source.getLevel().getServer().getProfileCache();
			Optional<GameProfile> profile = cache.get(player.toLowerCase());
			if (profile.isPresent()) {
				ProtectIt.LOGGER.debug("profile -> {}", profile.get().getId());
				List<Component> components = PropertyUtil.listProperties(profile.get());
				components.forEach(component -> {
					source.sendSuccess(component, false);
				});
			} else {
				source.sendFailure(Component.translatable(LangUtil.message("unable_locate_player")));
			}
		} catch(Exception e) {
			source.sendFailure(Component.translatable(LangUtil.message("unable_locate_player")));
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	public static int listZones(CommandSourceStack source) {
			List<Component> components = ZoneUtil.listZones();
			components.forEach(component -> {
				source.sendSuccess(component, false);
			});
		return 1;
	}
	
	/**
	 * 
	 * @param source
	 * @param registryName
	 * @return
	 */
	// TODO update to take in a Player param
	static int give(CommandSourceStack source, String name) {
		try {
			Item givableItem = null;
			switch (name.toLowerCase()) {
			case "property lever":
				givableItem = ModItems.PROPERTY_LEVER.get();
				break;
			case "remove claim stake":
				givableItem = ModItems.REMOVE_CLAIM.get();
				break;
			}
			if (givableItem == null) {
				source.sendSuccess(Component.translatable(LangUtil.message("non_givable_item")), false);
				return 1;
			}
			source.getPlayerOrException().getInventory().add(new ItemStack(givableItem));
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("error on give -> ", e);
		}
		return 1;
	}

	/**
	 * 
	 * @param source
	 */
	static void saveData(ServerLevel level) {
		ProtectItSavedData savedData = ProtectItSavedData.get(level);
		if (savedData != null) {
			savedData.setDirty();
		}	
	}

	/**
	 * 
	 * @param uuid
	 * @return
	 */
	static String parseNameUuid(String uuid) {
		String output = "";
		// parse out the uuid
		if (uuid.contains("[")) {
			// find between square brackets
			Pattern p = Pattern.compile("\\[([^\"]*)\\]");
			Matcher m = p.matcher(uuid);
			// get first occurence
			if (m.find()) {
				output = m.group(1);
			}
		}
		return output;
	}

	/**
	 * 
	 * @param owner
	 * @param property
	 * @return
	 */
	public static Optional<Property> getProperty(UUID owner, UUID property) {
		// get the owner's properties
//		List<Property> properties = ProtectionRegistries.block().getProtections(owner.toString());
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner);
		// get the named property
		List<Property> namedProperties = properties.stream().filter(p -> p.getUuid().equals(property)).collect(Collectors.toList());
		if (namedProperties.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(namedProperties.get(0));
	}

	/**
	 * Executed against top-level properties
	 * @param owner
	 * @param propertyName
	 * @return
	 */
	public static Optional<Property> getPropertyByName(UUID owner, String propertyName) {
		// get the owner's properties
//		List<Property> properties = ProtectionRegistries.block().getProtections(owner.toString());
		List<Property> properties = ProtectionRegistries.property().getPropertiesByOwner(owner);
		// get the named property
		List<Property> namedProperties = properties.stream().filter(p -> p.getNameByOwner().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
		if (namedProperties.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(namedProperties.get(0));
	}

	public static Optional<Property> getPropertyByLord(UUID lord, String propertyName) {
		// get the lord's properties
		List<Property> properties = ProtectionRegistries.property().getPropertiesByLord(lord);
		// get the named property
		List<Property> namedProperties = properties.stream().filter(p -> p.getNameByLord().equalsIgnoreCase(propertyName)).collect(Collectors.toList());
		if (namedProperties.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(namedProperties.get(0));
	}
	
	/**
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static Optional<Tuple<ICoords, ICoords>> validateCoords(ICoords c1, ICoords c2) {
		Optional<Tuple<ICoords, ICoords>> coords = Optional.of (new Tuple<ICoords, ICoords>(c1, c2));
		if (!isDownField(c1, c2)) {
			// attempt to flip coords and test again
			if (isDownField(c2, c1)) {
				coords = Optional.of(new Tuple<ICoords, ICoords>(c2, c1));
			}
			else {
				coords = Optional.empty();
			}
		}
		return coords;
	}

	/**
	 * TODO When updating to allow Y values, update this method to include Y check
	 * @param from
	 * @param to
	 * @return
	 */
	public static boolean isDownField(ICoords from, ICoords to) {
		if (to.getX() >= from.getX() && to.getZ() >= from.getZ()) {
			return true;
		}
		return false;
	}
}
