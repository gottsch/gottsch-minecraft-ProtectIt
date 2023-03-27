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
package mod.gottsch.forge.protectit.core.zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 
 * @author Mark Gottschling Mar 21, 2023
 *
 */
public class ZoneUtil {

	/**
	 * 
	 * @param zones
	 * @return
	 */
	public static Optional<Zone> getLeastSignificant(List<Zone> zones) {
		/*
		 * NOTE this list of zones may or may not contain any nested zones, or only some (ie middle).
		 */
		if (zones.size() == 1) {
			return Optional.of(zones.get(0));
		}
		
		// return the smallest sized zone
		Zone selected = zones.get(0);
		for (Zone zone : zones) {
			if (sizeOf(zone) < sizeOf(selected)) {
				selected = zone;
			}
		}
		return Optional.ofNullable(selected);
	}
	
	/**
	 * 
	 * @param zone
	 * @return
	 */
	public static int sizeOf(Zone zone) {
		ICoords size = zone.getBox().getSize().add(1, 1, 1);
		return size.getX() * size.getY() * size.getZ();
	}

	public static List<Component> listZones() {
		List<Component> messages = new ArrayList<>();
		messages.add(Component.literal(""));
		messages.add(Component.translatable(LangUtil.message("zone.list")).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
		messages.add(Component.literal(""));

		// get top-level properties only by player
		List<Zone> zones = ProtectionRegistries.pvp().getAll();
		List<Component> components = formatList(messages, zones);
		return components;
	}
	
	static List<Component> formatList(List<Component> messages, List<Zone> list) {

		if (list.isEmpty()) {
			messages.add(Component.translatable(LangUtil.message("property.list.empty")).withStyle(ChatFormatting.AQUA));
		}
		else {			
			list.forEach(zone -> {
				messages.add(Component.translatable(zone.getName().toUpperCase() + ": ").withStyle(ChatFormatting.AQUA)
						.append(Component.translatable(String.format("(%s) to (%s)", 
								formatCoords(zone.getBox().getMinCoords()), 
								formatCoords(zone.getBox().getMaxCoords()))).withStyle(ChatFormatting.GREEN)
								)
						.append(Component.translatable(", size: (" + formatCoords(zone.getBox().getSize().add(1, 1, 1)) + ")").withStyle(ChatFormatting.WHITE))
						);

				//				[STYLE].withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockpos.getX() + " " + s1 + " " + blockpos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))
			});
		}
		return messages;
	}

	public static String formatCoords(ICoords coords) {
		return String.format("%s, %s, %s", coords.getX(), coords.getY(), coords.getZ());
	}
}
