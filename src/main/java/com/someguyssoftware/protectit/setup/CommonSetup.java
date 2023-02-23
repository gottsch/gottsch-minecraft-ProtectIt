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
package com.someguyssoftware.protectit.setup;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.config.Config;
import com.someguyssoftware.protectit.item.ProtectItItems;
import com.someguyssoftware.protectit.network.ProtectItNetworking;

import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.CreativeModeTabEvent.BuildContents;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 
 * @author Mark Gottschling on Nov 3, 2021
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetup {
	/**
	 * 
	 * @param event
	 */
	public static void common(final FMLCommonSetupEvent event) {
		// add mod specific logging
		Config.instance.addRollingFileAppender(ProtectIt.MODID);
		ProtectItNetworking.register();
	}
	
	@SubscribeEvent
	public static void registemItemsToTab(BuildContents event) {
		if (event.getTab() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(ProtectItItems.SMALL_CLAIM.get(), TabVisibility.PARENT_AND_SEARCH_TABS);
			event.accept(ProtectItItems.MEDIUM_CLAIM.get(), TabVisibility.PARENT_AND_SEARCH_TABS);
			event.accept(ProtectItItems.LARGE_CLAIM.get(), TabVisibility.PARENT_AND_SEARCH_TABS);
			event.accept(ProtectItItems.REMOVE_CLAIM.get(), TabVisibility.PARENT_AND_SEARCH_TABS);
			event.accept(ProtectItItems.PROPERTY_LEVER.get(), TabVisibility.PARENT_AND_SEARCH_TABS);
		}
	}
}
