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

import com.mojang.authlib.GameProfile;

import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.registry.PlayerData;
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
public class PropertyUtils {

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

		List<Property> owners = new ArrayList<>();
		List<Property> landlords = new ArrayList<>();

		// get top-level properties only by player
		List<Property> properties = ProtectionRegistries.block().getPropertiesByOwner(player.getId())
				.stream().filter(p -> p.getLandlord() == null || p.getLandlord().equals(PlayerData.EMPTY)).toList();
		// get the entire property hierarchy and organize into the 2 lists
		getPropertyHierarchy(properties).forEach(p -> {
			if (p.getOwner().getUuid().equals(player.getId().toString())) {
				owners.add(p);
			}
			else {
				landlords.add(p);
			}
		});

		//		List<Component> components = formatList(messages, ProtectionRegistries.block().getProtections(player.getStringUUID()));
		//		List<Component> components = formatList(messages, ProtectionRegistries.block().getPropertiesByOwner(player.getUUID()));
		List<Component> components = formatList(messages, owners, landlords);
		//		components.forEach(component -> {
		//			source.sendSuccess(component, false); // TODO just generate a list and return to caller
		//		});
		return components;
	}

	/**
	 * 
	 * @param messages
	 * @param owners
	 * @param landlords
	 * @return
	 */
	private static List<Component> formatList(List<Component> messages, List<Property> owners, List<Property> landlords) {

		if (owners.isEmpty() && landlords.isEmpty()) {
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
			landlords.forEach(p -> {
				messages.add(
						Component.translatable(p.getNameByOwner().toUpperCase() + ": ").withStyle(ChatFormatting.LIGHT_PURPLE)
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

		PlayerData playerData = new PlayerData(player.getStringUUID(), player.getName().getString());
		names = result.stream().filter(p -> p.getOwner().equals(playerData) || 
				(p.getOwner().equals(PlayerData.EMPTY) && p.getLandlord().equals(playerData))).map(p -> p.getName(player.getUUID())).toList();

		return names;
	}

	/**
	 * Version that only takes in a single parent to avoid creating a list.
	 * @param property
	 * @return
	 */
	public static List<Property> getPropertyHierarchy(Property property) {
		List<Property> result = new ArrayList<>();

		property.getChildren().forEach(child -> {
			if (!child.getChildren().isEmpty()) {
				result.addAll(child.getChildren());
			}
		});
		result.addAll(property.getChildren());

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
			if (!parent.getChildren().isEmpty()) {
				parent.getChildren().forEach(child -> {
					if (!child.getChildren().isEmpty()) {
						result.addAll(child.getChildren());
					}
				});
				result.addAll(parent.getChildren());
			}
			result.add(parent);
		});

		return result;
	}
}
