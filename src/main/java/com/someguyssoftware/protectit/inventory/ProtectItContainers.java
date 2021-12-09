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
package com.someguyssoftware.protectit.inventory;

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 
 * @author Mark Gottschling on Nov 18, 2021
 *
 */
public class ProtectItContainers {
	public static ContainerType<ClaimLecternContainer> CLAIM_LECTERN_CONTAINER_TYPE;

	@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD)	
	public static class RegistrationHandler {		
		
		@SubscribeEvent
		public static void registerContainers(final RegistryEvent.Register<ContainerType<?>> event) {
			CLAIM_LECTERN_CONTAINER_TYPE = IForgeContainerType.create(ClaimLecternContainer::create);
			CLAIM_LECTERN_CONTAINER_TYPE.setRegistryName("claim_lectern_container");
			event.getRegistry().register(CLAIM_LECTERN_CONTAINER_TYPE);
			
		}
	}
}
