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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.block.entity.AbstractClaimBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;


/**
 * 
 * @author Mark Gottschling on Nov 8, 2021
 *
 */
public interface IPropertyRenderer {
	public static final ResourceLocation TEXTURE = new ResourceLocation("protectit:textures/entity/highlight.png");

	/**
	 * 
	 * @param matrixStack
	 * @param offset
	 */
	default public void updateTranslation(PoseStack matrixStack, ICoords offset) {
		matrixStack.translate(offset.getX(), offset.getY(), offset.getZ());
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
	default public void renderProperty(BlockEntity tileEntity, PoseStack matrixStack, VertexConsumer builder,
			ICoords claimSize, float red, float green, float blue, float alpha) {

		// push the current transformation matrix + normals matrix
		matrixStack.pushPose(); 

		updatePropertyTranslation(tileEntity, matrixStack);
		// render
		LevelRenderer.renderLineBox(matrixStack, builder, 
				0, 0, 0,
				claimSize.getX(), claimSize.getY(), claimSize.getZ(),
				red, green,blue, 1.0f, red, green, blue);
		
		matrixStack.popPose();
	}
	
	/**
	 * 
	 * @param tileEntity
	 * @param matrixStack
	 */
	default public void updatePropertyTranslation(BlockEntity blockEntity, PoseStack matrixStack) {
		// do nothing
	}
	
	/**
	 * Draw the highlight as a 2D plane, using quads.
	 * derived from The Grey Ghost's MinecraftByExample:
	 * https://github.com/TheGreyGhost/MinecraftByExample/blob/1-16-3-final/src/main/java/minecraftbyexample/mbe21_tileentityrenderer/RenderQuads.java
	 * @param tileEntity
	 * @param partialTicks
	 * @param matrixStack
	 * @param renderBuffers
	 * @param combinedLight
	 * @param combinedOverlay
	 */
	default public void renderHighlight(BlockEntity tileEntity, float partialTicks, PoseStack matrixStack, MultiBufferSource renderBuffers, ICoords size,
			Color color, int combinedLight, int combinedOverlay) {

		// push the current transformation matrix + normals matrix
		matrixStack.pushPose();
		
		updateHighlightTranslation(tileEntity, matrixStack);
		drawQuads(matrixStack, renderBuffers, size, color, combinedLight);
		
		// restore the original transformation matrix + normals matrix
		matrixStack.popPose();
	}

	default public void renderHighlight(BlockEntity blockEntity, float partialTicks, PoseStack matrixStack, MultiBufferSource renderBuffers, ICoords size,
			int combinedLight, int combinedOverlay) {
		renderHighlight(blockEntity, partialTicks, matrixStack, renderBuffers, size, getHighlightColor(blockEntity), combinedLight, combinedOverlay);
	}
	
	/**
	 * 
	 * @param blockEntity
	 * @return
	 */
	default public Color getHighlightColor(BlockEntity blockEntity) {
		return new Color(0, 255, 0, 100);
	}
	
	/**
	 * 
	 * @param blockEntity
	 * @param matrixStack
	 */
	default public void updateHighlightTranslation(BlockEntity blockEntity, PoseStack matrixStack) {
		// do nothing
	}

	/**
	 * 
	 * @param blockEntity
	 * @param matrixStack
	 * @param builder
	 * @param overlapBox
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 */
	default public void renderOverlap(AbstractClaimBlockEntity blockEntity, PoseStack matrixStack, VertexConsumer builder,
			Box overlapBox, float red, float green, float blue, float alpha) {
		// calculate render pos -> delta of b & pos
		ICoords offsetCoords = overlapBox.getMinCoords().delta(blockEntity.getBlockPos());
		// calculate size of b
		ICoords size = overlapBox.getSize();

		matrixStack.pushPose(); 
		updateTranslation(matrixStack, offsetCoords);
		LevelRenderer.renderLineBox(matrixStack, builder, 0, 0, 0,
				size.getX(),
				size.getY(),
				size.getZ(),
				red, 0, 0, 1.0f, red, 0, 0);
		matrixStack.popPose();
	}
	
	/**
	 * 
	 * @param blockEntity
	 * @param partialTicks
	 * @param matrixStack
	 * @param renderBuffers
	 * @param overlapBox
	 * @param color
	 * @param combinedLight
	 * @param combinedOverlay
	 */
	default public void renderOverlapHighlight(BlockEntity blockEntity, float partialTicks, PoseStack matrixStack, MultiBufferSource renderBuffers, 
			Box overlapBox, Color color, int combinedLight, int combinedOverlay) {

		// calculate render pos -> delta of b & pos
		ICoords offsetCoords = overlapBox.getMinCoords().delta(blockEntity.getBlockPos());
		// calculate size of b
		ICoords size = overlapBox.getSize();
		
		// push the current transformation matrix + normals matrix
		matrixStack.pushPose();
		
		updateTranslation(matrixStack, offsetCoords.withY(0));
		drawQuads(matrixStack, renderBuffers, size, color, combinedLight);
		
		// restore the original transformation matrix + normals matrix
		matrixStack.popPose();
	}
	
	/**
	 * Draw a cube from [0,0,0] to [x, y, z], same texture on all sides, using a supplied texture
	 */
	default public void drawQuads(PoseStack matrixStack, MultiBufferSource renderBuffer,
			ICoords size, Color color, int combinedLight) {

		// other typical RenderTypes used by TER are:
		// getEntityCutout, getBeaconBeam (which has translucency),
		VertexConsumer vertexBuilderBlockQuads = renderBuffer.getBuffer(RenderType.beaconBeam(TEXTURE, true));

		// retrieves the current transformation matrix
		Matrix4f matrixPos = matrixStack.last().pose();
		// retrieves the current transformation matrix for the normal vector
		Matrix3f matrixNormal = matrixStack.last().normal();

		// we use the whole texture
		Vec2 bottomLeftUV = new Vec2(0.0F, 1.0F);
		float uvWidth = 1.0F;
		float uvHeight = 1.0F;

		// all faces have the same height and width
		final float WIDTH = size.getX();
		final float HEIGHT = size.getZ();

		final Vec3 UP_FACE_ORIGIN = new Vec3(0, 0.005, 0);
		final Vec3 DOWN_FACE_ORIGIN = new Vec3(0, -0.005, 0);

		addFace(Direction.UP, matrixPos, matrixNormal, vertexBuilderBlockQuads,
				color, UP_FACE_ORIGIN, WIDTH, HEIGHT, bottomLeftUV, uvWidth, uvHeight, combinedLight);
		addFace(Direction.DOWN, matrixPos, matrixNormal, vertexBuilderBlockQuads,
				color, DOWN_FACE_ORIGIN, WIDTH, HEIGHT, bottomLeftUV, uvWidth, uvHeight, combinedLight);
	}

	/**
	 * Setup:
	 * Minecraft run positive from N -> S, W -> E.
	 * Therefor a default position from a player's POV is facing south (looking at block north face).
	 * Bottom = smallest x, z and Top is largest x, z, NW corner = (0, 0) = "bottom right", SE corner = (1, 1)  = "top left" (again from player POV).
	 * If you (human) are looking at a 2D graph, the player would be standing at (0, 0) facing down, where (0, 0) is the top-left position.
	 * Minecraft setup differently that a standard cartisean plane where (+1, +1) is in Quadrant IV instead of Quadrant I.
	 * @param whichFace
	 * @param matrixPos
	 * @param matrixNormal
	 * @param renderBuffer
	 * @param color
	 * @param originPos
	 * @param width
	 * @param height
	 * @param bottomLeftUV
	 * @param texUwidth
	 * @param texVheight
	 * @param lightmapValue
	 */
	default public void addFace(Direction whichFace, Matrix4f matrixPos, Matrix3f matrixNormal, VertexConsumer renderBuffer,
			Color color, Vec3 originPos, float width, float height, Vec2 bottomLeftUV, float texUwidth, float texVheight, int lightmapValue) {

		Vector3f xOffset, zOffset;
		xOffset = new Vector3f(1, 0, 0);
		zOffset = new Vector3f(0, 0, 1);

		xOffset.mul(width);  // multiply by width
		zOffset.mul(height);  // multiply by height

		Vector3f bottomLeftPos = new Vector3f(originPos);
		Vector3f topLeftPos = new Vector3f(originPos);
		Vector3f bottomRightPos = new Vector3f(originPos);
		Vector3f topRightPos = new Vector3f(originPos);

		switch (whichFace) {
		default:
		case UP: {
			bottomLeftPos.add(xOffset);
			topRightPos.add(zOffset);
			topLeftPos.add(xOffset);
			topLeftPos.add(zOffset);
			break;
		}
		case DOWN: { 
			topLeftPos.add(xOffset);
			bottomLeftPos.add(xOffset);
			bottomLeftPos.add(zOffset);	    	    	  
			bottomRightPos.add(zOffset);
			break;
		}
		}

		// texture coordinates are "upside down" relative to the face
		// eg bottom left = [U min, V max]
		Vec2 bottomLeftUVpos = new Vec2(bottomLeftUV.x, bottomLeftUV.y);
		Vec2 bottomRightUVpos = new Vec2(bottomLeftUV.x + texUwidth, bottomLeftUV.y);
		Vec2 topLeftUVpos = new Vec2(bottomLeftUV.x + texUwidth, bottomLeftUV.y + texVheight);
		Vec2 topRightUVpos = new Vec2(bottomLeftUV.x, bottomLeftUV.y + texVheight);

		Vector3f normalVector = whichFace.step();  // gives us the normal to the face

		addQuad(matrixPos, matrixNormal, renderBuffer,
				bottomLeftPos, bottomRightPos, topRightPos, topLeftPos,
				bottomLeftUVpos, bottomRightUVpos, topLeftUVpos, topRightUVpos,
				normalVector, color, lightmapValue);
	}

	/**
	 * Add a quad.
	 * The vertices are added in anti-clockwise order from the VIEWER's  point of view, i.e.
	 * bottom left; bottom right, top right, top left
	 * If you add the vertices in clockwise order, the quad will face in the opposite direction; i.e. the viewer will be
	 *   looking at the back face, which is usually culled (not visible)
	 * See
	 * http://greyminecraftcoder.blogspot.com/2014/12/the-tessellator-and-worldrenderer-18.html
	 * http://greyminecraftcoder.blogspot.com/2014/12/block-models-texturing-quads-faces.html
	 */
	default public void addQuad(Matrix4f matrixPos, Matrix3f matrixNormal, VertexConsumer renderBuffer,
			Vector3f blpos, Vector3f brpos, Vector3f trpos, Vector3f tlpos,
			Vec2 blUVpos, Vec2 brUVpos, Vec2 trUVpos, Vec2 tlUVpos,
			Vector3f normalVector, Color color, int lightmapValue) {
		addQuadVertex(matrixPos, matrixNormal, renderBuffer, blpos, blUVpos, normalVector, color, lightmapValue);
		addQuadVertex(matrixPos, matrixNormal, renderBuffer, brpos, brUVpos, normalVector, color, lightmapValue);
		addQuadVertex(matrixPos, matrixNormal, renderBuffer, trpos, trUVpos, normalVector, color, lightmapValue);
		addQuadVertex(matrixPos, matrixNormal, renderBuffer, tlpos, tlUVpos, normalVector, color, lightmapValue);
	}

	// suitable for vertexbuilders using the DefaultVertexFormats.ENTITY format
	static void addQuadVertex(Matrix4f matrixPos, Matrix3f matrixNormal, VertexConsumer renderBuffer,
			Vector3f pos, Vec2 texUV,
			Vector3f normalVector, Color color, int lightmapValue) {
		renderBuffer.vertex(matrixPos, pos.x(), pos.y(), pos.z()) // position coordinate
		.color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())        // color
		.uv(texUV.x, texUV.y)                     // texel coordinate
		.overlayCoords(OverlayTexture.NO_OVERLAY)  // only relevant for rendering Entities (Living)
		.uv2(lightmapValue)         			    // lightmap with full brightness
		.normal(matrixNormal, normalVector.x(), normalVector.y(), normalVector.z())
		.endVertex();
	}
}
