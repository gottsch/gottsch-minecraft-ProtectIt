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
package com.someguyssoftware.protectit.eventhandler;

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 
 * @author Mark Gottschling on Dec 7, 2021
 *
 */
public class ClientOnly {

	/**
	 *
	 */
	@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	
	/**
	 * 
	 * @author Mark Gottschling on Dec 7, 2021
	 *
	 */
	public static class RegistrationHandler {

		@SubscribeEvent
		public static void onTextureStitchEvent(TextureStitchEvent.Pre event) {
			TextureAtlas atlas = event.getAtlas();
			if (!atlas.location().equals(TextureAtlas.LOCATION_BLOCKS)) return;
			event.addSprite(new ResourceLocation(ProtectIt.MODID, "entity/claim_lectern_book"));
		}
	}
}
