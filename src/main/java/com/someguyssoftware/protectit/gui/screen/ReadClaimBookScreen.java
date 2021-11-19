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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.ReadBookScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
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

	public static final IBookInfo EMPTY_ACCESS = new IBookInfo() {

		public int getPageCount() {
			return 0;
		}

		public ITextProperties getPageRaw(int p_230456_1_) {
			return ITextProperties.EMPTY;
		}
	};

	private IBookInfo bookAccess;
//	private final boolean playTurnSound;
	private int currentPage;
	private int cachedPage = -1;
	private List<IReorderingProcessor> cachedPageComponents = Collections.emptyList();

	/**
	 * 
	 */
	public ReadClaimBookScreen() {
		this(EMPTY_ACCESS/*, false*/);
	}

	/**
	 * 
	 * @param bookInfo
	 * @param playSound
	 */
	private ReadClaimBookScreen(IBookInfo bookInfo/*, boolean playSound*/) {
		super(NarratorChatListener.NO_TITLE);
		this.bookAccess = bookInfo;
//		this.playTurnSound = playSound;
	}

	/**
	 * 
	 */
	protected void init() {
		this.createMenuControls();
		//		      this.createPageControlButtons();
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
		int j = 2;
		this.blit(matrixStack, i, 2, 0, 0, 192, 192);
		if (this.cachedPage != this.currentPage) {
			ITextProperties textProperties = this.bookAccess.getPage(this.currentPage);
			this.cachedPageComponents = this.font.split(textProperties, 114);
			//		         this.pageMsg = new TranslationTextComponent("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
		}

		this.cachedPage = this.currentPage;
		//		      int i1 = this.font.width(this.pageMsg);
		//		      this.font.draw(matrixStack, this.pageMsg, (float)(i - i1 + 192 - 44), 18.0F, 0);
		int k = Math.min(128 / 9, this.cachedPageComponents.size());

		for(int l = 0; l < k; ++l) {
			IReorderingProcessor reorderingProcessor = this.cachedPageComponents.get(l);
			this.font.draw(matrixStack, reorderingProcessor, (float)(i + 36), (float)(32 + l * 9), 0);
		}

		Style style = this.getClickedComponentStyleAt((double)xPos, (double)yPos);
		if (style != null) {
			this.renderComponentHoverEffect(matrixStack, style, xPos, yPos);
		}

		super.render(matrixStack, xPos, yPos, p_230430_4_);
	}

	@Nullable
	public Style getClickedComponentStyleAt(double xPos, double yPos) {
		if (this.cachedPageComponents.isEmpty()) {
			return null;
		} else {
			int i = MathHelper.floor(xPos - (double)((this.width - 192) / 2) - 36.0D);
			int j = MathHelper.floor(yPos - 2.0D - 30.0D);
			if (i >= 0 && j >= 0) {
				int k = Math.min(128 / 9, this.cachedPageComponents.size());
				if (i <= 114 && j < 9 * k + k) {
					int l = j / 9;
					if (l >= 0 && l < this.cachedPageComponents.size()) {
						IReorderingProcessor ireorderingprocessor = this.cachedPageComponents.get(l);
						return this.minecraft.font.getSplitter().componentStyleAtWidth(ireorderingprocessor, i);
					} else {
						return null;
					}
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
	}

	/**
	 * 
	 * @param nbt
	 * @return
	 */
	public static List<String> convertPages(CompoundNBT nbt) {
		ListNBT list = nbt.getList("pages", 8).copy();
		Builder<String> builder = ImmutableList.builder();

		for(int i = 0; i < list.size(); ++i) {
			builder.add(list.getString(i));
		}
		return builder.build();
	}

	//	   protected void createPageControlButtons() {
	//		      int i = (this.width - 192) / 2;
	//		      int j = 2;
	//		      this.forwardButton = this.addButton(new ChangePageButton(i + 116, 159, true, (p_214159_1_) -> {
	//		         this.pageForward();
	//		      }, this.playTurnSound));
	//		      this.backButton = this.addButton(new ChangePageButton(i + 43, 159, false, (p_214158_1_) -> {
	//		         this.pageBack();
	//		      }, this.playTurnSound));
	//		      this.updateButtonVisibility();
	//		   }

	/**
	 * 
	 * @param bookAccess
	 */
	public void setBookAccess(IBookInfo bookAccess) {
		this.bookAccess = bookAccess;
		this.currentPage = 0;
		this.cachedPage = -1;
	}

	/**
	 * 
	 * @param page
	 * @return
	 */
	public boolean setPage(int page) {
		// this will always be 0. there is only 1 page
		int i = 0; // MathHelper.clamp(page, 0, this.bookAccess.getPageCount() - 1);
		if (i != this.currentPage) {
			this.currentPage = i;
			this.cachedPage = -1;
			return true;
		} else {
			return false;
		}
	}

	private int getNumPages() {
		return 0;
	}

	/**
	 * 
	 * @author Mark Gottschling on Nov 19, 2021
	 *
	 */
	@OnlyIn(Dist.CLIENT)
	public interface IBookInfo {
		int getPageCount();

		ITextProperties getPageRaw(int page);

		default ITextProperties getPage(int page) {
			return page >= 0 && page < this.getPageCount() ? this.getPageRaw(page) : ITextProperties.EMPTY;
		}

		static IBookInfo fromItem(ItemStack itemStack) {
			return (IBookInfo) new UnwrittenBookInfo(itemStack);
		}
	}

	/**
	 * 
	 * @author Mark Gottschling on Nov 19, 2021
	 *
	 */
	@OnlyIn(Dist.CLIENT)
	public static class UnwrittenBookInfo implements ReadBookScreen.IBookInfo {
		private final List<String> pages;

		public UnwrittenBookInfo(ItemStack p_i50617_1_) {
			this.pages = readPages(p_i50617_1_);
		}

		private static List<String> readPages(ItemStack p_216919_0_) {
			CompoundNBT compoundnbt = p_216919_0_.getTag();
			return (List<String>) (compoundnbt != null ? convertPages(compoundnbt) : ImmutableList.of());
		}

		public int getPageCount() {
			return this.pages.size();
		}

		public ITextProperties getPageRaw(int p_230456_1_) {
			return ITextProperties.of(this.pages.get(p_230456_1_));
		}
	}
}
