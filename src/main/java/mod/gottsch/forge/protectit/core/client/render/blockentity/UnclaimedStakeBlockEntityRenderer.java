/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.protectit.core.client.render.blockentity;

import java.awt.Color;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.block.entity.PropertyLeverBlockEntity;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 
 * @author Mark Gottschling Mar 1, 2023
 *
 */
public class UnclaimedStakeBlockEntityRenderer extends PropertyLeverBlockEntityRenderer {
	
	/**
	 * 
	 * @param dispatcher
	 */
	public UnclaimedStakeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(PropertyLeverBlockEntity blockEntity, float partialTicks, PoseStack matrixStack,
			MultiBufferSource renderTypeBuffer, int combinedLight, int combinedOverlay) {
		
//		BlockPos pos = blockEntity.getBlockPos();	
//		Block block = blockEntity.getLevel().getBlockState(pos).getBlock();
		Optional<Property> property = ProtectionRegistries.block().getPropertyByUuid(blockEntity.getPropertyUuid());
		
		if (property.isEmpty()) {
			return;
		}
		
		VertexConsumer builder = renderTypeBuffer.getBuffer(RenderType.lines());

		// render the claim
		ICoords size = property.get().getBox().getMaxCoords().delta(property.get().getBox().getMinCoords());
		renderProperty(blockEntity, matrixStack, builder, size, 0, 0, 0, 1.0f);	
		renderHighlight(blockEntity, partialTicks, matrixStack, renderTypeBuffer, size, combinedLight, combinedOverlay);
	}
	
	@Override
	public Color getHighlightColor(BlockEntity tileEntity) {
		return new Color(0, 0, 255, 100);
	}
	
	@Override
	public void updateHighlightTranslation(BlockEntity blockEntity, PoseStack matrixStack) {
		// TODO use template method and extract, this calc to a method
		Optional<Property> property = ProtectionRegistries.block().getPropertyByUuid(((PropertyLeverBlockEntity)blockEntity).getPropertyUuid());
		if (property.isEmpty()) {
			return;
		}
		ICoords coords = new Coords(blockEntity.getBlockPos());
		ICoords highlightFloor = new Coords(coords);
		while (highlightFloor.getY() > property.get().getBox().getMinCoords().getY()) {
			if (blockEntity.getLevel().getBlockState(highlightFloor.down(1).toPos()).getMaterial().isSolid()) {
				break;
			}
			highlightFloor = highlightFloor.down(1);
		}
		highlightFloor = coords.delta(highlightFloor).negate();
		
		ICoords delta = 
				new Coords(blockEntity.getBlockPos()).delta(((PropertyLeverBlockEntity)blockEntity).getPropertyCoords())
				.negate()
				.withY(highlightFloor.getY());

		matrixStack.translate(delta.getX(), delta.getY(), delta.getZ());
	}
}
