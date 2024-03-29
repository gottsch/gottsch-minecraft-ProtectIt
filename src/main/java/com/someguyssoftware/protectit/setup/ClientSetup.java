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
import com.someguyssoftware.protectit.client.render.blockentity.ClaimBlockEntityRenderer;
import com.someguyssoftware.protectit.client.render.blockentity.PropertyLeverBlockEntityRenderer;
import com.someguyssoftware.protectit.client.render.blockentity.RemoveClaimBlockEntityRenderer;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.SMALL_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.MEDIUM_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.LARGE_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.REMOVE_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.PROPERTY_LEVER.get(), RenderType.cutoutMipped());
		});
	}

	/**
	 * register renderers
	 * @param event
	 */
	@SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ProtectItBlockEntities.CLAIM_TYPE.get(), ClaimBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(ProtectItBlockEntities.REMOVE_CLAIM_TYPE.get(), RemoveClaimBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(ProtectItBlockEntities.PROPERTY_LEVER_TYPE.get(), PropertyLeverBlockEntityRenderer::new);
	}
}
