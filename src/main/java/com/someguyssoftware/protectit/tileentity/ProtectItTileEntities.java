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
package com.someguyssoftware.protectit.tileentity;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ProtectItBlocks;

import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ProtectItTileEntities {
	public static TileEntityType<ClaimTileEntity> CLAIM_TILE_ENTITY_TYPE;
	
	@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD)
	public static class RegistrationHandler {

		@SubscribeEvent
		public static void onTileEntityTypeRegistration(final RegistryEvent.Register<TileEntityType<?>> event) {
			CLAIM_TILE_ENTITY_TYPE = TileEntityType.Builder
					.of(ClaimTileEntity::new, ProtectItBlocks.SMALL_CLAIM, ProtectItBlocks.MEDIUM_CLAIM, ProtectItBlocks.LARGE_CLAIM)
					.build(null);
			CLAIM_TILE_ENTITY_TYPE.setRegistryName("claim_te");
			event.getRegistry().register(CLAIM_TILE_ENTITY_TYPE);
		}
		
	}
}
