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
package com.someguyssoftware.protectit.client.render.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.entity.ClaimLecternBlockEntity;

import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;


/**
 * 
 * @author Mark Gottschling on Nov 18, 2021
 *
 */
public class ClaimLecternTileEntityRenderer implements BlockEntityRenderer<ClaimLecternBlockEntity> {
	private final BookModel bookModel;
	public static final Material BOOK_LOCATION = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation(ProtectIt.MODID, "entity/claim_lectern_book"));

//	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ProtectIt.MODID, "claim_lecturn_book"), "main");
//	public static final ResourceLocation CAULDRON_CHEST_RENDERER_ATLAS_TEXTURE = new ResourceLocation(Treasure.MODID, "entity/chest/cauldron_chest");

	public ClaimLecternTileEntityRenderer(BlockEntityRendererProvider.Context context) {
//		super(dispatcher);
		// TODO use the custom location
		this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
	}

	@Override
	public void render(ClaimLecternBlockEntity blockEntity, float partialTicks, PoseStack matrixStack,
			MultiBufferSource renderTypeBuffer, int combinedLight, int combinedOverlay) {
		BlockState blockstate = blockEntity.getBlockState();
		if (blockstate.getValue(LecternBlock.HAS_BOOK)) {
			matrixStack.pushPose();
			matrixStack.translate(0.5D, 1.0625D, 0.5D);
			float f = blockstate.getValue(LecternBlock.FACING).getClockWise().toYRot();
			matrixStack.mulPose(Vector3f.YP.rotationDegrees(-f));
			matrixStack.mulPose(Vector3f.ZP.rotationDegrees(67.5F));
			matrixStack.translate(0.0D, -0.125D, 0.0D);
			this.bookModel.setupAnim(0.0F, 0.1F, 0.9F, 1.2F);
			VertexConsumer vertexConsumer = BOOK_LOCATION.buffer(renderTypeBuffer, RenderType::entitySolid);
			this.bookModel.render(matrixStack, vertexConsumer, combinedLight, combinedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
			matrixStack.popPose();
		}
	}

}
