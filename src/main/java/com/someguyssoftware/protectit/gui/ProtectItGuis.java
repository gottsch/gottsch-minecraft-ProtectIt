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
package com.someguyssoftware.protectit.gui;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ProtectItBlocks;
import com.someguyssoftware.protectit.gui.render.tileentity.ClaimLecternTileEntityRenderer;
import com.someguyssoftware.protectit.gui.render.tileentity.ClaimLeverTileEntityRenderer;
import com.someguyssoftware.protectit.gui.render.tileentity.ClaimTileEntityRenderer;
import com.someguyssoftware.protectit.gui.render.tileentity.RemoveClaimTileEntityRenderer;
import com.someguyssoftware.protectit.gui.screen.ClaimLecternScreen;
import com.someguyssoftware.protectit.inventory.ProtectItContainers;
import com.someguyssoftware.protectit.tileentity.ProtectItTileEntities;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 
 * @author Mark Gottschling on Oct 17, 2021
 *
 */
public class ProtectItGuis {
	@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
	public static class RegistrationHandler {
		// register the factory that is used on the client to generate Screen corresponding to our Container
		@SubscribeEvent
		public static void onClientSetupEvent(FMLClientSetupEvent event) {
			// tell the renderer that the base is rendered using CUTOUT_MIPPED (to match the Block Hopper)
			RenderTypeLookup.setRenderLayer(ProtectItBlocks.SMALL_CLAIM, RenderType.cutoutMipped());
			RenderTypeLookup.setRenderLayer(ProtectItBlocks.MEDIUM_CLAIM, RenderType.cutoutMipped());
			RenderTypeLookup.setRenderLayer(ProtectItBlocks.LARGE_CLAIM, RenderType.cutoutMipped());
			RenderTypeLookup.setRenderLayer(ProtectItBlocks.REMOVE_CLAIM, RenderType.cutoutMipped());
			RenderTypeLookup.setRenderLayer(ProtectItBlocks.CLAIM_LEVER, RenderType.cutoutMipped());
			RenderTypeLookup.setRenderLayer(ProtectItBlocks.CLAIM_LECTERN, RenderType.cutoutMipped());
			
			// register the custom screens
			ScreenManager.register(ProtectItContainers.CLAIM_LECTERN_CONTAINER_TYPE, ClaimLecternScreen::new);
			
			// register the custom renderer for our tile entity
			ClientRegistry.bindTileEntityRenderer(ProtectItTileEntities.CLAIM_TILE_ENTITY_TYPE, ClaimTileEntityRenderer::new);
			ClientRegistry.bindTileEntityRenderer(ProtectItTileEntities.REMOVE_CLAIM_TILE_ENTITY_TYPE, RemoveClaimTileEntityRenderer::new);
			ClientRegistry.bindTileEntityRenderer(ProtectItTileEntities.CLAIM_LEVER_TILE_ENTITY_TYPE, ClaimLeverTileEntityRenderer::new);
			ClientRegistry.bindTileEntityRenderer(ProtectItTileEntities.CLAIM_LECTERN_TILE_ENTITY_TYPE, ClaimLecternTileEntityRenderer::new);

		}
		
	}
}
