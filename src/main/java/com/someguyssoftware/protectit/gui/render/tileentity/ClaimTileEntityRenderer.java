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
package com.someguyssoftware.protectit.gui.render.tileentity;

import java.awt.Color;

import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ClaimBlock;
import com.someguyssoftware.protectit.tileentity.ClaimTileEntity;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ClaimTileEntityRenderer extends TileEntityRenderer<ClaimTileEntity> implements IClaimRenderer {

	/**
	 * 
	 * @param dispatcher
	 */
	public ClaimTileEntityRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(ClaimTileEntity tileEntity, float partialTicks, MatrixStack matrixStack,
			IRenderTypeBuffer renderTypeBuffer, int combinedLight, int combinedOverlay) {

		BlockPos pos = tileEntity.getBlockPos();
		Block block = tileEntity.getLevel().getBlockState(pos).getBlock();

		if (!(block instanceof ClaimBlock)) {
			ProtectIt.LOGGER.info("not the right block -> {}", block);
			return; // should never happen
		}

		// test if the player owns the tile entity
		if (StringUtils.isBlank(tileEntity.getOwnerUuid()) ||
				!Minecraft.getInstance().player.getStringUUID().equalsIgnoreCase(tileEntity.getOwnerUuid())) {
			return;
		}

		// color of the bound (White)
		Color c = Color.WHITE;
		// split up in red, green and blue and transform it to 0.0 - 1.0
		float red = c.getRed() / 255.0f;
		float green = c.getGreen() / 255.0f;
		float blue = c.getBlue() / 255.0f;

		IVertexBuilder builder = renderTypeBuffer.getBuffer(RenderType.lines());

		// render the claim
		boolean hasOverlaps = false;
		if (!((ClaimTileEntity)tileEntity).getOverlaps().isEmpty()) {
			hasOverlaps = true;
		}

		// render claim outlines
		renderClaim(tileEntity, matrixStack, builder, ((ClaimBlock)block).getClaimSize(), hasOverlaps ? red : 0, green, hasOverlaps ? blue : 0, 1.0f);

		// render all overlaps
		((ClaimTileEntity)tileEntity).getOverlaps().forEach(b -> {
			renderOverlap(tileEntity, matrixStack, builder, b, red, 0, 0, 1.0f);
		});

		// render higlights
		if (hasOverlaps) {
			renderHighlight(tileEntity, partialTicks, matrixStack, renderTypeBuffer, ((ClaimBlock)block).getClaimSize(), 
					new Color(255, 255, 255, 100), combinedLight, combinedOverlay);
		}
		else {
			renderHighlight(tileEntity, partialTicks, matrixStack, renderTypeBuffer, ((ClaimBlock)block).getClaimSize(), 
					combinedLight, combinedOverlay);
		}

		((ClaimTileEntity)tileEntity).getOverlaps().forEach(b -> {
			renderOverlapHighlight(tileEntity, partialTicks, matrixStack, renderTypeBuffer, b, 
					new Color(255, 0, 0, 100), combinedLight, combinedOverlay);
		});
	}

	@Override
	public void updateClaimTranslation(TileEntity tileEntity, MatrixStack matrixStack) {
		BlockPos pos = tileEntity.getBlockPos();
		Block block = tileEntity.getLevel().getBlockState(pos).getBlock();
		final Box box = ((ClaimBlock)block).getBox(pos	);
		ICoords delta = new Coords(0, 0, 0).withY(pos.getY() - box.getMinCoords().getY());		
		matrixStack.translate(delta.getX(), -delta.getY(), delta.getZ());
	}
}
