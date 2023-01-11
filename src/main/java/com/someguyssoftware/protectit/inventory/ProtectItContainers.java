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
package com.someguyssoftware.protectit.inventory;

import com.someguyssoftware.protectit.setup.Registration;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * @author Mark Gottschling on Nov 18, 2021
 *
 */
public class ProtectItContainers {
	
	public static final RegistryObject<MenuType<ClaimLecternMenu>> CLAIM_LECTERN_CONTAINER_TYPE;

	static {
		CLAIM_LECTERN_CONTAINER_TYPE = Registration.CONTAINERS.register("standard_chest_container",
	            () -> IForgeMenuType.create((windowId, inventory, data) ->  new ClaimLecternMenu(windowId)));			
	}
	
	public static void register() {
		Registration.registerContainers();
	}
	
//	@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD)	
//	public static class RegistrationHandler {		
//		
//		@SubscribeEvent
//		public static void registerContainers(final RegistryEvent.Register<ContainerType<?>> event) {
//			CLAIM_LECTERN_CONTAINER_TYPE = IForgeContainerType.create(ClaimLecternContainer::create);
//			CLAIM_LECTERN_CONTAINER_TYPE.setRegistryName("claim_lectern_container");
//			event.getRegistry().register(CLAIM_LECTERN_CONTAINER_TYPE);
//			
//		}
//	}
}
