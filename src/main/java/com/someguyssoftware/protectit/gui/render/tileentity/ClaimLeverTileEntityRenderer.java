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
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ProtectItBlocks;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.registry.BlockProtectionRegistry;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.tileentity.ClaimLeverTileEntity;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * 
 * @author Mark Gottschling on Nov 8, 2021
 *
 */
public class ClaimLeverTileEntityRenderer extends TileEntityRenderer<ClaimLeverTileEntity> implements IClaimRenderer {

	public ClaimLeverTileEntityRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(ClaimLeverTileEntity tileEntity, float partialTicks, MatrixStack matrixStack,
			IRenderTypeBuffer renderTypeBuffer, int combinedLight, int combinedOverlay) {
		
		if (tileEntity == null) {
			return;
		}
		
		BlockPos pos = tileEntity.getBlockPos();
		BlockState state =  tileEntity.getLevel().getBlockState(pos);
		Claim claim = ProtectionRegistries.block().getClaimByCoords(tileEntity.getClaimCoords());

		if (!state.is(ProtectItBlocks.CLAIM_LEVER) || !state.getValue(LeverBlock.POWERED) || claim == null) {
			return;
		}
		
		// only render for the owner and whitelist
//		ProtectIt.LOGGER.debug("claim -> {}", claim);
		if (!StringUtils.isBlank(claim.getOwner().getUuid())) {
//			ProtectIt.LOGGER.debug("player -> {}", Minecraft.getInstance().player.getStringUUID());
//			ProtectIt.LOGGER.debug("whitelist -> {}", claim.getWhitelist());
		}
		if (StringUtils.isBlank(claim.getOwner().getUuid()) ||
				(!Minecraft.getInstance().player.getStringUUID().equalsIgnoreCase(claim.getOwner().getUuid()) &&
				claim.getWhitelist().stream().noneMatch(p -> p.getUuid().equals(Minecraft.getInstance().player.getStringUUID())))) {	
//			ProtectIt.LOGGER.debug("not owner nor whitelist -> {}, {}", Minecraft.getInstance().player.getDisplayName().getString(), Minecraft.getInstance().player.getStringUUID());
			return;
		}

		// color of the bound (White)
		Color c = Color.WHITE;
		// split up in red, green and blue and transform it to 0.0 - 1.0
		float green = c.getGreen() / 255.0f;

		IVertexBuilder builder = renderTypeBuffer.getBuffer(RenderType.lines());

		// TODO merge the Color strategies between the render methods
		
		// render the claim
		ICoords size = claim.getBox().getMaxCoords().delta(claim.getBox().getMinCoords());
		renderClaim(tileEntity, matrixStack, builder, size, 0, green, 0, 1.0f);	
		renderHighlight(tileEntity, partialTicks, matrixStack, renderTypeBuffer, size, combinedLight, combinedOverlay);
	}


	@Override
	public void updateClaimTranslation(TileEntity tileEntity, MatrixStack matrixStack) {
		ICoords delta = new Coords(tileEntity.getBlockPos()).delta(((ClaimLeverTileEntity)tileEntity).getClaimCoords()).negate();
		matrixStack.translate(delta.getX(), delta.getY(), delta.getZ());		
	}
	
	@Override
	public void updateHighlightTranslation(TileEntity tileEntity, MatrixStack matrixStack) {
		Claim claim = ProtectionRegistries.block().getClaimByCoords(((ClaimLeverTileEntity)tileEntity).getClaimCoords());

		ICoords leverCoords = new Coords(tileEntity.getBlockPos());
		ICoords highlightFloor = new Coords(leverCoords);
		while (highlightFloor.getY() > claim.getBox().getMinCoords().getY()) {
			if (tileEntity.getLevel().getBlockState(highlightFloor.down(1).toPos()).getMaterial().isSolid()) {
				break;
			}
			highlightFloor = highlightFloor.down(1);
		}
		highlightFloor = leverCoords.delta(highlightFloor).negate();
		
		ICoords delta = 
				new Coords(tileEntity.getBlockPos()).delta(((ClaimLeverTileEntity)tileEntity).getClaimCoords())
				.negate()
				.withY(highlightFloor.getY());

		matrixStack.translate(delta.getX(), delta.getY(), delta.getZ());
	}
}
