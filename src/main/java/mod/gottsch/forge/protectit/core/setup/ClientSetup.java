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

import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.ProtectItBlockEntities;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 
 * @author Mark Gottschling on Dec 7, 2021
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

//	public static void init(final FMLClientSetupEvent event) {
//		event.enqueueWork(() -> {
//            ItemBlockRenderTypes.setRenderLayer(ProtectItBlocks.FOUNDATION_STONE.get(), RenderType.cutoutMipped());
//		});
//	}

	/**
	 * register renderers
	 * @param event
	 */
	@SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
//		event.registerBlockEntityRenderer(ProtectItBlockEntities.FOUNDATION_STONE.get(), FoundationStoneBlockEntityRenderer::new);
	}
}
