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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.tileentity.ClaimLeverTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

/**
 * 
 * @author Mark Gottschling on Nov 8, 2021
 *
 */
public class ClaimLeverTileEntityRenderer extends TileEntityRenderer<ClaimLeverTileEntity> {

	public ClaimLeverTileEntityRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(ClaimLeverTileEntity tileEntity, float partialTicks, MatrixStack matrixStack,
			IRenderTypeBuffer renderTypeBuffer, int combinedLight, int combinedOverlay) {
		
		BlockPos pos = tileEntity.getBlockPos();
		BlockState state =  tileEntity.getLevel().getBlockState(pos);
		Claim claim = tileEntity.getClaim();
		
		if (!state.getValue(LeverBlock.POWERED) || claim == null) {
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
		ICoords size = claim.getBox().getMaxCoords().delta(claim.getBox().getMinCoords());
		renderClaim(tileEntity, matrixStack, builder, size, 0, green, 0, 1.0f);		
	}

	public void renderClaim(ClaimLeverTileEntity tileEntity, MatrixStack matrixStack, IVertexBuilder builder,
			ICoords claimSize, float red, float green, float blue, float alpha) {

//		int delta =  tileEntity.getBlockPos().getY() - tileEntity.getClaim().getBox().getMinCoords().getY();
		ICoords delta = new Coords(tileEntity.getBlockPos()).delta(tileEntity.getClaim().getBox().getMinCoords());
		
		// push the current transformation matrix + normals matrix
		matrixStack.pushPose(); 
		
		// translate on the y-axis by the delta of  the TE pos and the box min pos
		updateTranslation(matrixStack, delta.negate());
		
		// render
		WorldRenderer.renderLineBox(matrixStack, builder, 
				0, 0, 0,
				claimSize.getX(), claimSize.getY(), claimSize.getZ(),
				red, green,blue, 1.0f, red, green, blue);
		
		matrixStack.popPose();
	}
	
	/**
	 * 
	 * @param matrixStack
	 * @param offset
	 */
	public void updateTranslation(MatrixStack matrixStack, ICoords offset) {
		matrixStack.translate(offset.getX(), offset.getY(), offset.getZ());
	}
	
	/**
	 * 
	 * @param matrixStack
	 * @param yOffset
	 */
	public void updateTranslation(MatrixStack matrixStack, int yOffset) {
		final Vector3d TRANSLATION_OFFSET = new Vector3d(0.0, yOffset, 0.0);
		matrixStack.translate(TRANSLATION_OFFSET.x, TRANSLATION_OFFSET.y, TRANSLATION_OFFSET.z);
	}
}
