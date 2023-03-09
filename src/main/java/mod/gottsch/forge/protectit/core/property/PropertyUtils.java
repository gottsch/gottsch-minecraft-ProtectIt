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

import mod.gottsch.forge.protectit.core.registry.PlayerData;
import net.minecraft.world.entity.player.Player;

/**
 * 
 * @author Mark Gottschling Mar 6, 2023
 *
 */
public class PropertyUtils {

	/**
	 * 
	 * @param properties
	 * @param player
	 * @return
	 */
	public static List<String> getPropertyHierarchyNames(List<Property> properties, Player player) {
		List<String> names = new ArrayList<>();
		List<Property> result = new ArrayList<>();
//		properties.forEach(parent -> {
//			if (!parent.getChildren().isEmpty()) {
//				parent.getChildren().forEach(child -> {
//					if (!child.getChildren().isEmpty()) {
//						result.addAll(child.getChildren());
//					}
//				});
//				result.addAll(parent.getChildren());
//			}
//			result.add(parent);
//		});
		result = getPropertyHierarchy(properties);
//		result.forEach(p -> {
//			names.add(p.getNames().get(player.getUUID()));
//		});
		PlayerData playerData = new PlayerData(player.getStringUUID(), player.getName().getString());
		names = result.stream().filter(p -> p.getOwner().equals(playerData) || 
				(p.getOwner().equals(PlayerData.EMPTY) && p.getLandlord().equals(playerData))).map(p -> p.getName(player.getUUID())).toList();
		
		return names;
	}

	/**
	 * TODO fix like above
	 * @param properties
	 * @return
	 */
	public static List<Property> getPropertyHierarchy(List<Property> properties) {
//		List<Property> names = new ArrayList<>();
//		properties.stream()
//		.filter(p1 -> !p1.getChildren().isEmpty())
//		.flatMap(p1 -> p1.getChildren().stream()
//				.filter(p2 -> !p2.getChildren().isEmpty())
//				.flatMap(p2 -> p2.getChildren().stream())).toList();
//		return names;
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
