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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.someguyssoftware.protectit.tileentity.ClaimLecternTileEntity;

import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.BookModel;
import net.minecraft.client.renderer.tileentity.EnchantmentTableTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.vector.Vector3f;

/**
 * 
 * @author Mark Gottschling on Nov 18, 2021
 *
 */
public class ClaimLecternTileEntityRenderer extends TileEntityRenderer<ClaimLecternTileEntity> {
	private final BookModel bookModel = new BookModel();

	public ClaimLecternTileEntityRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(ClaimLecternTileEntity tileEntity, float partialTicks, MatrixStack matrixStack,
			IRenderTypeBuffer renderTypeBuffer, int combinedLight, int combinedOverlay) {
		BlockState blockstate = tileEntity.getBlockState();
		if (blockstate.getValue(LecternBlock.HAS_BOOK)) {
			matrixStack.pushPose();
			matrixStack.translate(0.5D, 1.0625D, 0.5D);
			float f = blockstate.getValue(LecternBlock.FACING).getClockWise().toYRot();
			matrixStack.mulPose(Vector3f.YP.rotationDegrees(-f));
			matrixStack.mulPose(Vector3f.ZP.rotationDegrees(67.5F));
			matrixStack.translate(0.0D, -0.125D, 0.0D);
			this.bookModel.setupAnim(0.0F, 0.1F, 0.9F, 1.2F);
			IVertexBuilder ivertexbuilder = EnchantmentTableTileEntityRenderer.BOOK_LOCATION.buffer(renderTypeBuffer, RenderType::entitySolid);
			this.bookModel.render(matrixStack, ivertexbuilder, combinedLight, combinedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
			matrixStack.popPose();
		}
	}

}
