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
package com.someguyssoftware.protectit.client.screen;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.someguyssoftware.protectit.claim.Property;
import com.someguyssoftware.protectit.registry.PlayerData;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;


/**
 * 
 * @author Mark Gottschling on Nov 18, 2021
 *
 */
//@OnlyIn(Dist.CLIENT)
public class ReadClaimBookScreen extends Screen {
	public static final ResourceLocation BOOK_LOCATION = new ResourceLocation("textures/gui/book.png");

	private List<PlayerData> playerDataCache = Lists.newArrayList();
	private Property claim;

	/**
	 * 
	 */
	public ReadClaimBookScreen() {
		super(NarratorChatListener.NO_TITLE);
	}

	/**
	 * 
	 */
	protected void init() {
		this.createMenuControls();
	}

	/**
	 * 
	 */
	protected void createMenuControls() {
		this.addRenderableWidget(new Button(this.width / 2 - 100, 196, 200, 20, CommonComponents.GUI_DONE, (p_214161_1_) -> {
			this.minecraft.setScreen((Screen) null);
		}));
	}

	public boolean keyPressed(int p_231046_1_, int p_231046_2_, int p_231046_3_) {
		if (super.keyPressed(p_231046_1_, p_231046_2_, p_231046_3_)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 */
	@SuppressWarnings("deprecation")
	public void render(PoseStack matrixStack, int xPos, int yPos, float p_230430_4_) {
		this.renderBackground(matrixStack);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
	    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	    RenderSystem.setShaderTexture(0, BOOK_LOCATION);
	    int i = (this.width - 192) / 2;
		this.blit(matrixStack, i, 2, 0, 0, 192, 192);
		// add title
		TranslatableComponent  title = new TranslatableComponent("label.protectit.claim_book.title", ChatFormatting.GOLD);
		int titleWidth = this.font.width(title);
		this.font.draw(matrixStack, title, (float)((this.width/2) - (titleWidth/2)), 18.0F, 0);

		for (int index = 0; index < getPlayerDataCache().size(); index++) {
			// highlight any names without UUIDs
			PlayerData playerData = getPlayerDataCache().get(index);
			MutableComponent nameText = new TextComponent(playerData.getName());
			if (StringUtils.isBlank(playerData.getUuid())) {
				nameText = nameText.withStyle(ChatFormatting.RED);
			}			
			this.font.draw(matrixStack, nameText, (float) (i + 36), (float) (32 + index * 9), 0);
		}
		super.render(matrixStack, xPos, yPos, p_230430_4_);
	}

	public List<PlayerData> getPlayerDataCache() {
		return playerDataCache;
	}

	public void setPlayerDataCache(List<PlayerData> playerDataCache) {
		this.playerDataCache = playerDataCache;
	}

	public Property getClaim() {
		return claim;
	}

	public void setClaim(Property claim) {
		this.claim = claim;
	}
}
