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
package mod.gottsch.forge.protectit.core.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.authlib.GameProfile;

import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.registry.PlayerIdentity;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * 
 * @author Mark Gottschling Mar 6, 2023
 *
 */
public class PropertyUtil {

	/**
	 * 
	 * @param source
	 * @param player
	 * @return
	 */
	public static List<Component> listProperties(Player player) {
		return listProperties(player.getGameProfile());
	}

	public static List<Component> listProperties(GameProfile player) {
		List<Component> messages = new ArrayList<>();
		messages.add(Component.literal(""));
		messages.add(Component.translatable(LangUtil.message("property.list")).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE)
				.append(Component.translatable(player.getName()).withStyle(ChatFormatting.AQUA)));
		messages.add(Component.literal(""));

		// get top-level properties only by player
		List<Property> owners = ProtectionRegistries.property().getPropertiesByOwner(player.getId());
//				.stream().filter(p -> p.getLord() == null || p.getOwner().getUuid().equals(player.getId())).toList();
//		ProtectIt.LOGGER.debug("owner props -> {}", owners);
		List<Property> lords = ProtectionRegistries.property().getPropertiesByLord(player.getId())
				.stream().filter(	p -> !p.getLord().equals(p.getOwner()) && !owners.contains(p)).toList();
//		ProtectIt.LOGGER.debug("lord props -> {}", lords);
		List<Component> components = formatList(messages, owners, lords);
		return components;
	}

	/**
	 * 
	 * @param messages
	 * @param owners
	 * @param lords
	 * @return
	 */
	private static List<Component> formatList(List<Component> messages, List<Property> owners, List<Property> lords) {

		if (owners.isEmpty() && lords.isEmpty()) {
			messages.add(Component.translatable(LangUtil.message("property.list.empty")).withStyle(ChatFormatting.AQUA));
		}
		else {
			owners.forEach(p -> {
				messages.add(
						Component.translatable(p.getNameByOwner().toUpperCase() + ": ").withStyle(ChatFormatting.AQUA)
						.append(Component.translatable(String.format("(%s) to (%s)", 
								formatCoords(p.getBox().getMinCoords()), 
								formatCoords(p.getBox().getMaxCoords()))).withStyle(ChatFormatting.GREEN)
								)
						.append(Component.translatable(", size: (" + formatCoords(p.getBox().getSize()) + ")").withStyle(ChatFormatting.WHITE))
						);
			});
			lords.forEach(p -> {
				messages.add(
						Component.translatable(p.getNameByLord().toUpperCase() + ": ").withStyle(ChatFormatting.LIGHT_PURPLE)
						.append(Component.translatable(String.format("(%s) to (%s)", 
								formatCoords(p.getBox().getMinCoords()), 
								formatCoords(p.getBox().getMaxCoords()))).withStyle(ChatFormatting.GREEN)
								)
						.append(Component.translatable(", size: (" + formatCoords(p.getBox().getSize()) + ")").withStyle(ChatFormatting.WHITE))
						);
			});
		}
		return messages;
	}

	/**
	 * TODO should move to ChatHelper / ChatUtil
	 * @param messages
	 * @param list
	 * @return
	 */
	static List<Component> formatList(List<Component> messages, List<Property> list) {

		if (list.isEmpty()) {
			messages.add(Component.translatable(LangUtil.message("property.list.empty")).withStyle(ChatFormatting.AQUA));
		}
		else {			
			list.forEach(p -> {
				messages.add(Component.translatable(p.getNameByOwner().toUpperCase() + ": ").withStyle(ChatFormatting.AQUA)
						.append(Component.translatable(String.format("(%s) to (%s)", 
								formatCoords(p.getBox().getMinCoords()), 
								formatCoords(p.getBox().getMaxCoords()))).withStyle(ChatFormatting.GREEN)
								)
						.append(Component.translatable(", size: (" + formatCoords(p.getBox().getSize()) + ")").withStyle(ChatFormatting.WHITE))
						);

				//				[STYLE].withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockpos.getX() + " " + s1 + " " + blockpos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))
			});
		}
		return messages;
	}

	public static String formatCoords(ICoords coords) {
		return String.format("%s, %s, %s", coords.getX(), coords.getY(), coords.getZ());
	}
	/**
	 * 
	 * @param properties
	 * @param player
	 * @return
	 */
	public static List<String> getPropertyHierarchyNames(List<Property> properties, Player player) {
		List<String> names = new ArrayList<>();
		List<Property> result = new ArrayList<>();

		result = getPropertyHierarchy(properties);

		PlayerIdentity playerData = new PlayerIdentity(player.getUUID(), player.getName().getString());
		names = result.stream().filter(p -> p.getOwner().equals(playerData) || 
				(p.getOwner().equals(PlayerIdentity.EMPTY) && p.getLord().equals(playerData))).map(p -> p.getName(player.getUUID())).toList();

		return names;
	}

	/**
	 * Version that only takes in a single parent to avoid creating a list.
	 * @param property
	 * @return
	 */
	public static List<Property> getPropertyHierarchy(Property property) {
		List<Property> result = new ArrayList<>();

		result.addAll(ProtectionRegistries.property().getAllPropertiesByUuid(property.getChildren()));
		result.addAll(result.stream().flatMap(p -> ProtectionRegistries.property().getAllPropertiesByUuid(p.getChildren()).stream()).toList());

		result.add(property);
		return result;
	}

	/**
	 * @param properties
	 * @return
	 */
	public static List<Property> getPropertyHierarchy(List<Property> properties) {
		List<Property> result = new ArrayList<>();
		
		properties.forEach(parent -> {
			result.addAll(ProtectionRegistries.property().getAllPropertiesByUuid(parent.getChildren()));
			result.addAll(result.stream().flatMap(p -> ProtectionRegistries.property().getAllPropertiesByUuid(p.getChildren()).stream()).toList());

			result.add(parent);
		});

		return result;
	}

	/**
	 * It is assumed that the properties list is of the same hierarchy.
	 * @param properties
	 * @return
	 */
	public static Optional<Property> getLeastSignificant(List<Property> properties) {
		/*
		 * NOTE this list of properties may or may not contain any children properties, or only some (ie middle).
		 * Property.getChildren() only contains UUIDs of the children
		 */
		if (properties.size() == 1) {
			return Optional.of(properties.get(0));
		}
		
		Property selected = null;
		for (Property p : properties) {
			if (!p.isDomain()) {
				selected = p;
				// short-circuit if found the least significant
				if (!selected.hasChildren()) {
					return Optional.of(selected);
				}
			}
		}
		return Optional.ofNullable(selected);
	}
	
	public static Optional<Property> getMostSignificant(List<Property> properties) {
		Optional<Property> property = properties.stream().filter(p -> p.isDomain()).findFirst();
		return property;
	}
}
