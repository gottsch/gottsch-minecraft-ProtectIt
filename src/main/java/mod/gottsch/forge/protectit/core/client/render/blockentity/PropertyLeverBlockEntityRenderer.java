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
package mod.gottsch.forge.protectit.core.client.render.blockentity;

import java.awt.Color;

import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.block.ModBlocks;
import mod.gottsch.forge.protectit.core.block.entity.PropertyLeverBlockEntity;
import mod.gottsch.forge.protectit.core.item.Deed;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


/**
 * 
 * @author Mark Gottschling on Nov 8, 2021
 *
 */
public class PropertyLeverBlockEntityRenderer implements BlockEntityRenderer<PropertyLeverBlockEntity>, IPropertyRenderer {

	public PropertyLeverBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
//		super(context);
	}

	@Override
	public void render(PropertyLeverBlockEntity blockEntity, float partialTicks, PoseStack matrixStack,
			MultiBufferSource renderTypeBuffer, int combinedLight, int combinedOverlay) {
		
		if (blockEntity == null) {
			return;
		}
		if (blockEntity.getPropertyUuid() == null || blockEntity.getPropertyUuid().equals(Deed.EMPTY_UUID)) {
			return;
		}
		
		BlockPos pos = blockEntity.getBlockPos();
		BlockState state =  blockEntity.getLevel().getBlockState(pos);
		Property property = ProtectionRegistries.block().getPropertyByCoords(blockEntity.getPropertyCoords());

		if (property == null) {
			return;
		}
		
		if (!state.is(ModBlocks.PROPERTY_LEVER.get()) || !state.getValue(LeverBlock.POWERED) || property == null) {
			return;
		}
		
		if (StringUtils.isBlank(property.getOwner().getUuid()) ||
				(!Minecraft.getInstance().player.getStringUUID().equalsIgnoreCase(property.getOwner().getUuid()) &&
				property.getWhitelist().stream().noneMatch(p -> p.getUuid().equals(Minecraft.getInstance().player.getStringUUID())))) {	
//			ProtectIt.LOGGER.debug("not owner nor whitelist -> {}, {}", Minecraft.getInstance().player.getDisplayName().getString(), Minecraft.getInstance().player.getStringUUID());
			return;
		}

		// color of the bound (White)
		Color c = Color.WHITE;
		// split up in red, green and blue and transform it to 0.0 - 1.0
		float green = c.getGreen() / 255.0f;

		VertexConsumer builder = renderTypeBuffer.getBuffer(RenderType.lines());

		// TODO merge the Color strategies between the render methods
		
		// render the claim
		ICoords size = property.getBox().getMaxCoords().delta(property.getBox().getMinCoords());
		renderProperty(blockEntity, matrixStack, builder, size, 0, green, 0, 1.0f);	
		renderHighlight(blockEntity, partialTicks, matrixStack, renderTypeBuffer, size, combinedLight, combinedOverlay);
	}


	@Override
	public void updatePropertyTranslation(BlockEntity tileEntity, PoseStack matrixStack) {
		ICoords delta = new Coords(tileEntity.getBlockPos()).delta(((PropertyLeverBlockEntity)tileEntity).getPropertyCoords()).negate();
		matrixStack.translate(delta.getX(), delta.getY(), delta.getZ());		
	}
	
	@Override
	public void updateHighlightTranslation(BlockEntity tileEntity, PoseStack matrixStack) {
		Property claim = ProtectionRegistries.block().getPropertyByCoords(((PropertyLeverBlockEntity)tileEntity).getPropertyCoords());

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
				new Coords(tileEntity.getBlockPos()).delta(((PropertyLeverBlockEntity)tileEntity).getPropertyCoords())
				.negate()
				.withY(highlightFloor.getY());

		matrixStack.translate(delta.getX(), delta.getY(), delta.getZ());
	}
}
