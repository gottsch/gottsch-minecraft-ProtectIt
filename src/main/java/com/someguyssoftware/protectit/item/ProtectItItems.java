/*
 * This file is part of  Protect It.
 * Copyright (c) 2021, Mark Gottschling (gottsch)
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

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ProtectItItems {
//	public static Item SMALL_STAKE;
	
	/**
	 * The actual event handler that registers the custom items.
	 *
	 * @param event The event this event handler handles
	 */
	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {

		/*
		 *  initialize items
		 */

		// ITEMS
//		SMALL_STAKE = new Item(new Item.Properties()).setRegistryName(new ResourceLocation(ProtectIt.MODID, "small_stake"));
		
		/*
		 * register items (make sure you have set the registry name).
		 */
//		event.getRegistry().registerAll(SMALL_STAKE);
	}
}
