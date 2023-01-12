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
import com.someguyssoftware.protectit.block.ProtectItBlocks;
import com.someguyssoftware.protectit.block.entity.ProtectItBlockEntities;
import com.someguyssoftware.protectit.client.render.tileentity.ClaimLecternTileEntityRenderer;
import com.someguyssoftware.protectit.client.render.tileentity.ClaimLeverTileEntityRenderer;
import com.someguyssoftware.protectit.client.render.tileentity.ClaimTileEntityRenderer;
import com.someguyssoftware.protectit.client.render.tileentity.RemoveClaimTileEntityRenderer;
import com.someguyssoftware.protectit.client.screen.ClaimLecternScreen;
import com.someguyssoftware.protectit.inventory.ProtectItContainers;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 
 * @author Mark Gottschling on Dec 7, 2021
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

	public static void init(final FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
        	// attach our container(s) to the screen(s)
            MenuScreens.register(ProtectItContainers.CLAIM_LECTERN_CONTAINER_TYPE.get(), ClaimLecternScreen::new);           
  
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.SMALL_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.MEDIUM_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.LARGE_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.REMOVE_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.CLAIM_LEVER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.CLAIM_LECTERN.get(), RenderType.cutoutMipped());
		});
	}

	@SubscribeEvent
	public static void onTextureStitchEvent(TextureStitchEvent.Pre event) {
		TextureAtlas map = event.getAtlas();
		if (!map.location().equals( TextureAtlas.LOCATION_BLOCKS)) {
			return;
		}
		event.addSprite(new ResourceLocation(ProtectIt.MODID, "entity/claim_lectern_book"));
	}

	/**
	 * register renderers
	 * @param event
	 */
	@SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ProtectItBlockEntities.CLAIM_TYPE.get(), ClaimTileEntityRenderer::new);
		event.registerBlockEntityRenderer(ProtectItBlockEntities.REMOVE_CLAIM_TYPE.get(), RemoveClaimTileEntityRenderer::new);
		event.registerBlockEntityRenderer(ProtectItBlockEntities.CLAIM_LEVER_TYPE.get(), ClaimLeverTileEntityRenderer::new);
		event.registerBlockEntityRenderer(ProtectItBlockEntities.CLAIM_LECTERN_TYPE.get(), ClaimLecternTileEntityRenderer::new);
	}
}
