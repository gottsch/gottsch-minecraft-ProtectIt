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
package com.someguyssoftware.protectit.client.render.tileentity;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.someguyssoftware.protectit.block.entity.PropertyLeverBlockEntity;
import com.someguyssoftware.protectit.claim.Property;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 
 * @author Mark Gottschling on Dec 2, 2021
 *
 */
public class RemoveClaimTileEntityRenderer extends PropertyLeverTileEntityRenderer {
	
	/**
	 * 
	 * @param dispatcher
	 */
	public RemoveClaimTileEntityRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(PropertyLeverBlockEntity tileEntity, float partialTicks, PoseStack matrixStack,
			MultiBufferSource renderTypeBuffer, int combinedLight, int combinedOverlay) {

		BlockPos pos = tileEntity.getBlockPos();	
		Block block = tileEntity.getLevel().getBlockState(pos).getBlock();
		Property claim = ProtectionRegistries.block().getClaimByCoords(tileEntity.getClaimCoords());
		
		if (claim == null) {
//			ProtectIt.LOGGER.info("or claim is null", block);
			return;
		}

		VertexConsumer builder = renderTypeBuffer.getBuffer(RenderType.lines());

		// render the claim
		ICoords size = claim.getBox().getMaxCoords().delta(claim.getBox().getMinCoords());
		renderClaim(tileEntity, matrixStack, builder, size, 0, 0, 0, 1.0f);	
		renderHighlight(tileEntity, partialTicks, matrixStack, renderTypeBuffer, size, combinedLight, combinedOverlay);
	}
	
	@Override
	public Color getHighlightColor(BlockEntity tileEntity) {
		return new Color(0, 0, 0, 100);
	}
}
