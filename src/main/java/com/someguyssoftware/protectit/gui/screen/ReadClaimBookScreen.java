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
package com.someguyssoftware.protectit.gui.screen;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 
 * @author Mark Gottschling on Nov 18, 2021
 *
 */
@OnlyIn(Dist.CLIENT)
public class ReadClaimBookScreen extends Screen {
	public static final ResourceLocation BOOK_LOCATION = new ResourceLocation("textures/gui/book.png");

	// NOTES
	// MC send a List<String> representing pages, in each in line there are LF
	// delimiters for each line on the page.

	private List<String> playerNames = Lists.newArrayList();

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
		this.addButton(new Button(this.width / 2 - 100, 196, 200, 20, DialogTexts.GUI_DONE, (p_214161_1_) -> {
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

	public void render(MatrixStack matrixStack, int xPos, int yPos, float p_230430_4_) {
		this.renderBackground(matrixStack);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bind(BOOK_LOCATION);
		int i = (this.width - 192) / 2;

		this.blit(matrixStack, i, 2, 0, 0, 192, 192);

		// TODO use this code to write title centered "Claim Whitelist" or "Claim
		// Members"
//				      int i1 = this.font.width(this.pageMsg);
		// this.font.draw(matrixStack, this.pageMsg, (float)(i - i1 + 192 - 44), 18.0F,
		// 0);
//		int k = Math.min(128 / 9, this.cachedPageComponents.size());

		// TODO highlight any names without UUIDs
		for (int index = 0; index < getPlayerNames().size(); index++) {
			this.font.draw(matrixStack, getPlayerNames().get(index), (float) (i + 36), (float) (32 + index * 9), 0);
		}
		super.render(matrixStack, xPos, yPos, p_230430_4_);
	}

	protected List<String> getPlayerNames() {
		return playerNames;
	}

	protected void setPlayerNames(List<String> playerNames) {
		this.playerNames = playerNames;
	}
}
