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
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.protectit.tileentity.AbstractClaimTileEntity;

/**
 * 
 * @author Mark Gottschling on Nov 3, 2021
 *
 */
public interface IClaimTileEntityRenderer {

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
	void renderClaim(AbstractClaimTileEntity tileEntity, MatrixStack matrixStack, IVertexBuilder builder,
			ICoords claimSize, float red, float green, float blue, float alpha);

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
	void renderOverlap(AbstractClaimTileEntity tileEntity, MatrixStack matrixStack, IVertexBuilder builder,
			Box overlapBox, float red, float green, float blue, float alpha);

	/**
	 * 
	 * @param matrixStack
	 * @param offset
	 */
	void updateTranslation(MatrixStack matrixStack, ICoords offset);

	/**
	 * 
	 * @param matrixStack
	 * @param yOffset
	 */
	void updateTranslation(MatrixStack matrixStack, int yOffset);

}