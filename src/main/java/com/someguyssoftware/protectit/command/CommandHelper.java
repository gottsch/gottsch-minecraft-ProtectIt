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
package com.someguyssoftware.protectit.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.UUID;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ProtectItBlocks;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.property.Property;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.util.LangUtil;

import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 
 * @author Mark Gottschling Feb 18, 2023
 *
 */
public class CommandHelper {
	public static final String BLOCK = "block";
	public static final String ADD = "add";
	public static final String CLEAR = "clear";
	public static final String LIST = "list";
	public static final String WHITELIST = "whitelist";
	public static final String REMOVE ="remove";
	
	public static final String POS = "pos";
	public static final String POS2 = "pos2";
	public static final String TARGET = "target";
	public static final String TARGETS = "targets";
	public static final String UUID = "uuid";
	
	public static final String GIVE = "give";
	public static final String GIVE_ITEM = "giveItem";
	
	///// SUGGESTIONS /////
	static final SuggestionProvider<CommandSourceStack> SUGGEST_UUID = (source, builder) -> {
		return SharedSuggestionProvider.suggest(ProtectionRegistries.block().findByClaim(p -> !p.getOwner().getUuid().isEmpty()).stream()
				.map(i -> String.format("%s [%s]", 
						(i.getOwner().getName() == null) ? "" : i.getOwner().getName(),
								(i.getOwner().getUuid() == null) ? "" : i.getOwner().getUuid())), builder);
	};
	
	static final SuggestionProvider<CommandSourceStack> GIVABLE_ITEMS = (source, builder) -> {
		List<String> items = Arrays.asList(
				"Property Lever", 
				"Remove Claim Stake"
				);
		return SharedSuggestionProvider.suggest(items, builder);
	};
	
	/**
	 * 
	 * @param source
	 * @return
	 */
	static int unavailable(CommandSourceStack source) {
		source.sendSuccess(new TranslatableComponent("message.protectit.option_unavailable"), false);
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
			source.sendFailure(new TranslatableComponent(LangUtil.message("unable_locate_player")));
			return 1;
		}
		List<Property> claims = ProtectionRegistries.block().getProtections(player.getStringUUID());
		// TODO steam and find claim with oldName
		List<Property> namedClaims = claims.stream().filter(claim -> claim.getName().equalsIgnoreCase(oldName)).collect(Collectors.toList());
		if (namedClaims.isEmpty()) {
			source.sendFailure(new TranslatableComponent(LangUtil.message("property.name.unknown"))
					.append(new TranslatableComponent(oldName.toUpperCase()).withStyle(ChatFormatting.AQUA)));
			return 1;
		}
		namedClaims.get(0).setName(newName.toUpperCase());
		
		source.sendSuccess(new TranslatableComponent(LangUtil.message("property.rename.success"))
				.append(new TranslatableComponent(newName.toUpperCase()).withStyle(ChatFormatting.AQUA)), false);

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
	public static int list(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
			return list(source, player);
		}
		catch(CommandSyntaxException 	e) {
			source.sendFailure(new TranslatableComponent(LangUtil.message("unable_locate_player")));
		}
		return 1;
	}
	
	/**
	 * 
	 * @param source
	 * @param player
	 * @return
	 */
	public static int list(CommandSourceStack source, ServerPlayer player) {
		List<Component> messages = new ArrayList<>();
		messages.add(new TextComponent(""));
		messages.add(new TranslatableComponent(LangUtil.message("property.list"), player.getName().getString()).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
		messages.add(new TextComponent(""));
		
		List<Component> components = formatList(messages, ProtectionRegistries.block().getProtections(player.getStringUUID()));
		components.forEach(component -> {
			source.sendSuccess(component, false);
		});
		return 1;
	}

	/**
	 * 
	 * @param messages
	 * @param list
	 * @return
	 */
	static List<Component> formatList(List<Component> messages, List<Property> list) {
		
		if (list.isEmpty()) {
			messages.add(new TranslatableComponent(LangUtil.message("property.list.empty")).withStyle(ChatFormatting.AQUA));
		}
		else {			
			list.forEach(claim -> {
				messages.add(new TranslatableComponent(claim.getName().toUpperCase() + ": ").withStyle(ChatFormatting.AQUA)
						.append(new TranslatableComponent(String.format("(%s) to (%s)", 
								formatCoords(claim.getBox().getMinCoords()), 
								formatCoords(claim.getBox().getMaxCoords()))).withStyle(ChatFormatting.GREEN)
						)
						.append(new TranslatableComponent(", size: (" + formatCoords(claim.getBox().getSize()) + ")").withStyle(ChatFormatting.WHITE))
				);

//				[STYLE].withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockpos.getX() + " " + s1 + " " + blockpos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))
			});
		}
		return messages;
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
			case "claim access lectern":
//				givableItem = Item.byBlock(ProtectItBlocks.CLAIM_LECTERN.get());
				break;
			case "claim vizualizer lever":
				givableItem = Item.byBlock(ProtectItBlocks.PROPERTY_LEVER.get());
				break;
			case "claim access manifest":
//				givableItem = ProtectItItems.CLAIM_BOOK.get();
				break;
			case "remove claim stake":
				givableItem = Item.byBlock(ProtectItBlocks.REMOVE_CLAIM.get());
				break;
			}
			if (givableItem == null) {
				source.sendSuccess(new TranslatableComponent(LangUtil.message("non_givable_item")), false);
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
	
	private static String formatCoords(ICoords coords) {
		return String.format("%s, %s, %s", coords.getX(), coords.getY(), coords.getZ());
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
		List<Property> claims = ProtectionRegistries.block().getProtections(owner.toString());
		// get the named property
		List<Property> namedClaims = claims.stream().filter(claim -> claim.getUuid().equals(property)).collect(Collectors.toList());
		if (namedClaims.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(namedClaims.get(0));
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
