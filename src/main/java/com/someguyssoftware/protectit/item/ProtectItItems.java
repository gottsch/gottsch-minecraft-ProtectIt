/*
 * This file is part of  Protect It.
 * Copyright (c) 2021 Mark Gottschling (gottsch)
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
package com.someguyssoftware.protectit.item;

import java.util.function.Supplier;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ProtectItBlocks;
import com.someguyssoftware.protectit.setup.Registration;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ProtectItItems {
	
	// item properties
	public static final Supplier<Item.Properties> ITEM_PROPS_SUPPLIER = () -> new Item.Properties().tab(CreativeModeTab.TAB_MISC);

	// items
	public static RegistryObject<Item> SMALL_CLAIM = fromClaimBlock(ProtectItBlocks.SMALL_CLAIM, ITEM_PROPS_SUPPLIER);
	public static RegistryObject<Item> MEDIUM_CLAIM = fromClaimBlock(ProtectItBlocks.MEDIUM_CLAIM, ITEM_PROPS_SUPPLIER);
	public static RegistryObject<Item> LARGE_CLAIM = fromClaimBlock(ProtectItBlocks.LARGE_CLAIM, ITEM_PROPS_SUPPLIER);
	public static RegistryObject<Item> REMOVE_CLAIM = Registration.ITEMS.register("remove_claim", () -> new RemoveClaimBlockItem(ProtectItBlocks.REMOVE_CLAIM.get(), ITEM_PROPS_SUPPLIER.get()));

	public static RegistryObject<Item> PROPERTY_LEVER = Registration.ITEMS.register("property_lever", () -> new PropertyLeverBlockItem(ProtectItBlocks.PROPERTY_LEVER.get(), ITEM_PROPS_SUPPLIER.get()));

	/**
	 * 
	 */
	public static void register() {
		// cycle through all block and create items
		Registration.registerItems();
	}
	
	/**
	 * The actual event handler that registers the custom items.
	 *
	 * @param event The event this event handler handles
	 */
//	@SubscribeEvent
//	public static void registerItems(RegistryEvent.Register<Item> event) {
//
//		/*
//		 *  initialize items
//		 */
//		
//		// ITEMS
//		CLAIM_BOOK = new ClaimBook(ProtectIt.MODID, "claim_book", new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_MISC));
//		// TODO may remove from tabs
//		
//		/*
//		 * register items (make sure you have set the registry name).
//		 */
//		event.getRegistry().register(CLAIM_BOOK);
//	}
	
	// convenience method: take a RegistryObject<Block> and make a corresponding RegistryObject<Item> from it
	public static <B extends Block> RegistryObject<Item> fromClaimBlock(RegistryObject<B> block, Supplier<Item.Properties> itemProperties) {
		return Registration.ITEMS.register(block.getId().getPath(), () -> new ClaimBlockItem(block.get(), itemProperties.get()));
	}
}
