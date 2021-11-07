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
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ClaimBlock;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.tileentity.AbstractClaimTileEntity;
import com.someguyssoftware.protectit.tileentity.IClaimTileEntity;
import com.someguyssoftware.protectit.tileentity.ClaimTileEntity;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ClaimTileEntityRenderer extends TileEntityRenderer<AbstractClaimTileEntity> implements IClaimTileEntityRenderer {

	/**
	 * 
	 * @param dispatcher
	 */
	public ClaimTileEntityRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(AbstractClaimTileEntity tileEntity, float partialTicks, MatrixStack matrixStack,
			IRenderTypeBuffer renderTypeBuffer, int combinedLight, int combinedOverlay) {

		BlockPos pos = tileEntity.getBlockPos();
		Block block = tileEntity.getLevel().getBlockState(pos).getBlock();

		if (!(block instanceof ClaimBlock)) {
			ProtectIt.LOGGER.info("not the right block -> {}", block);
			return; // should never happen
		}
				
		// test if the player owns the tile entity
//		ProtectIt.LOGGER.info("owner uuid @ {} -> {}", pos.toShortString(), tileEntity.getOwnerUuid());
//		ProtectIt.LOGGER.info("minecraft player -> {}", Minecraft.getInstance().player.getStringUUID());
		if (StringUtils.isBlank(tileEntity.getOwnerUuid()) ||
				!tileEntity.getOwnerUuid().equalsIgnoreCase(Minecraft.getInstance().player.getStringUUID())) {
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
		renderClaim(tileEntity, matrixStack, builder, ((ClaimBlock)block).getClaimSize(), hasOverlaps ? red : 0, green, hasOverlaps ? blue : 0, 1.0f);

		// render all overlaps
		((ClaimTileEntity)tileEntity).getOverlaps().forEach(b -> {
			renderOverlap(tileEntity, matrixStack, builder, b, red, 0, 0, 1.0f);
		});
	}

	/**
	 * 
	 * @param tileEntity
	 * @param matrixStack
	 * @param builder
	 * @param claimSize
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 */
	@Override
	public void renderClaim(AbstractClaimTileEntity tileEntity, MatrixStack matrixStack, IVertexBuilder builder,
			ICoords claimSize, float red, float green, float blue, float alpha) {
		
		BlockPos pos = tileEntity.getBlockPos();
		Block block = tileEntity.getLevel().getBlockState(pos).getBlock();
		final Box box = ((ClaimBlock)block).getBox(pos	);
		int delta = pos.getY() - box.getMinCoords().getY();
		
		// push the current transformation matrix + normals matrix
		matrixStack.pushPose(); 

		// translate on the y-axis by the delta of  the TE pos and the box min pos
		updateTranslation(matrixStack, -delta);
		
		// render
		WorldRenderer.renderLineBox(matrixStack, builder, 
				0, 0, 0,
				claimSize.getX(), claimSize.getY(), claimSize.getZ(),
				red, green,blue, 1.0f, red, green, blue);
		
		matrixStack.popPose();
	}

	/**
	 * 
	 * @param tileEntity
	 * @param matrixStack
	 * @param builder
	 * @param overlapBox
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 */
	@Override
	public void renderOverlap(AbstractClaimTileEntity tileEntity, MatrixStack matrixStack, IVertexBuilder builder,
			Box overlapBox, float red, float green, float blue, float alpha) {
		// calculate render pos -> delta of b & pos
		ICoords offsetCoords = overlapBox.getMinCoords().delta(tileEntity.getBlockPos());
		// calculate size of b
		ICoords size = overlapBox.getSize();

		matrixStack.pushPose(); 
		updateTranslation(matrixStack, offsetCoords);
		WorldRenderer.renderLineBox(matrixStack, builder, 0, 0, 0,
				size.getX(),
				size.getY(),
				size.getZ(),
				red, 0, 0, 1.0f, red, 0, 0);
		matrixStack.popPose();
	}
	
	/**
	 * 
	 * @param matrixStack
	 * @param offset
	 */
	@Override
	public void updateTranslation(MatrixStack matrixStack, ICoords offset) {
		matrixStack.translate(offset.getX(), offset.getY(), offset.getZ());
	}

	/**
	 * 
	 * @param matrixStack
	 * @param yOffset
	 */
	@Override
	public void updateTranslation(MatrixStack matrixStack, int yOffset) {
		final Vector3d TRANSLATION_OFFSET = new Vector3d(0.0, yOffset, 0.0);
		matrixStack.translate(TRANSLATION_OFFSET.x, TRANSLATION_OFFSET.y, TRANSLATION_OFFSET.z);
	}

	//	public void updateTranslation(MatrixStack matrixStack, ICoords offset) {
	//		// The model is defined centred on [0,0,0], so if we drew it at the current render origin, its centre would be
	//		// at the corner of the block, sunk halfway into the ground and overlapping into the adjacent blocks.
	//		// We want it to hover above the centre of the hopper base, so we need to translate up and across to the desired position
	//		
	//		// TODO use same formula as in ClaimBlock - or call ClaimBlock.getBox
	//		final Vector3d TRANSLATION_OFFSET = new Vector3d(0.0, -(offset.getY() / 2), 0.0);
	//		matrixStack.translate(TRANSLATION_OFFSET.x, TRANSLATION_OFFSET.y, TRANSLATION_OFFSET.z);
	//	}
}
