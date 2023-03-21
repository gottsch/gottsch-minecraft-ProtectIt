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
package mod.gottsch.forge.protectit.core.setup;

import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.ModBlocks;
import mod.gottsch.forge.protectit.core.block.entity.ModBlockEntities;
import mod.gottsch.forge.protectit.core.client.render.blockentity.ClaimBlockEntityRenderer;
import mod.gottsch.forge.protectit.core.client.render.blockentity.CustomClaimBlockEntityRenderer;
import mod.gottsch.forge.protectit.core.client.render.blockentity.PropertyLeverBlockEntityRenderer;
import mod.gottsch.forge.protectit.core.client.render.blockentity.RemoveClaimBlockEntityRenderer;
import mod.gottsch.forge.protectit.core.client.render.blockentity.UnclaimedStakeBlockEntityRenderer;
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
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SMALL_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MEDIUM_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LARGE_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CUSTOM_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.REMOVE_CLAIM.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PROPERTY_LEVER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNCLAIMED_STAKE.get(), RenderType.cutoutMipped());
		});
	}

	/**
	 * register renderers
	 * @param event
	 */
	@SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
		try {
		event.registerBlockEntityRenderer(ModBlockEntities.CLAIM_TYPE.get(), ClaimBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.CUSTOM_CLAIM_TYPE.get(), CustomClaimBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.REMOVE_CLAIM_TYPE.get(), RemoveClaimBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.PROPERTY_LEVER_TYPE.get(), PropertyLeverBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.UNCLAIMED_TYPE.get(), UnclaimedStakeBlockEntityRenderer::new);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("error", e);
		}
	}
}