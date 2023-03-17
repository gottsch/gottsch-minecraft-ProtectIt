/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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

import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.ClaimBlock;
import mod.gottsch.forge.protectit.core.block.entity.ClaimBlockEntity;
import mod.gottsch.forge.protectit.core.block.entity.CustomClaimBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 
 * @author Mark Gottschling Mar 7, 2023
 *
 */
public class CustomClaimBlockEntityRenderer  implements BlockEntityRenderer<CustomClaimBlockEntity>, IPropertyRenderer {

	public CustomClaimBlockEntityRenderer(BlockEntityRendererProvider.Context context) {

	}
	
	/**
	 * NOTE most of this method is the same as ClaimBlockEntityRenderer.... abstract most of it out.
	 */
	@Override
	public void render(CustomClaimBlockEntity blockEntity, float partialTicks, PoseStack matrixStack,
			MultiBufferSource renderTypeBuffer, int combinedLight, int combinedOverlay) {

		BlockPos pos = blockEntity.getBlockPos();
		Block block = blockEntity.getLevel().getBlockState(pos).getBlock();

		if (!(block instanceof ClaimBlock)) {
			ProtectIt.LOGGER.debug("not the right block -> {}", block);
			return; // should never happen
		}

		// test if the player owns the tile entity
		if (StringUtils.isBlank(blockEntity.getOwnerUuid()) ||
				!Minecraft.getInstance().player.getStringUUID().equalsIgnoreCase(blockEntity.getOwnerUuid())) {
			return;
		}

		// color of the bound (White)
		Color c = Color.WHITE;
		// split up in red, green and blue and transform it to 0.0 - 1.0
		float red = c.getRed() / 255.0f;
		float green = c.getGreen() / 255.0f;
		float blue = c.getBlue() / 255.0f;

		VertexConsumer builder = renderTypeBuffer.getBuffer(RenderType.lines());

		// render the claim
		boolean hasOverlaps = false;
		if (!((ClaimBlockEntity)blockEntity).getOverlaps().isEmpty()) {
			hasOverlaps = true;
		}

		// render claim outlines
		renderProperty(blockEntity, matrixStack, builder, blockEntity.getClaimSize(), hasOverlaps ? red : 0, green, hasOverlaps ? blue : 0, 1.0f);

		// render all overlaps
		((ClaimBlockEntity)blockEntity).getOverlaps().forEach(b -> {
			renderOverlap(blockEntity, matrixStack, builder, b, red, 0, 0, 1.0f);
		});

		// render higlights
		if (hasOverlaps) {
			renderHighlight(blockEntity, partialTicks, matrixStack, renderTypeBuffer, blockEntity.getClaimSize(), 
					new Color(255, 255, 255, 100), combinedLight, combinedOverlay);
		}
		else {
			renderHighlight(blockEntity, partialTicks, matrixStack, renderTypeBuffer, blockEntity.getClaimSize(), 
					combinedLight, combinedOverlay);
		}

		((ClaimBlockEntity)blockEntity).getOverlaps().forEach(b -> {
			renderOverlapHighlight(blockEntity, partialTicks, matrixStack, renderTypeBuffer, b, 
					new Color(255, 0, 0, 100), combinedLight, combinedOverlay);
		});
	}


	@Override
	public void updatePropertyTranslation(BlockEntity blockEntity, PoseStack matrixStack) {
		BlockPos pos = blockEntity.getBlockPos();
		Block block = blockEntity.getLevel().getBlockState(pos).getBlock();
		final Box box = ((ClaimBlock)block).getBox(blockEntity.getLevel(), pos);
		ICoords delta = new Coords(0, 0, 0).withY(pos.getY() - box.getMinCoords().getY());		
		matrixStack.translate(delta.getX(), -delta.getY(), delta.getZ());
	}
}
