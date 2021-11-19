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

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.fonts.TextInputUtil;
import net.minecraft.client.gui.screen.ReadBookScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.text.CharacterManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author Mark Gottschling on Nov 16, 2021
 *
 */
@OnlyIn(Dist.CLIENT)
public class EditClaimBookScreen extends Screen {
	private static final String PAGES_TAG = "pages";

	// context properties
	private final PlayerEntity owner;
	private final ItemStack book;
	private final Hand hand;

	// buttons
	private Button doneButton;

	// book state properties
	private final List<String> pages = Lists.newArrayList();
	private boolean isModified;
	private int frameTick;
	private int currentPage;
	private long lastClickTime;
	private int lastIndex = -1;
	private ITextComponent pageMsg = StringTextComponent.EMPTY;

	private final TextInputUtil pageEdit = new TextInputUtil(this::getCurrentPageText, this::setCurrentPageText, this::getClipboard, this::setClipboard, (p_238774_1_) -> {
		return p_238774_1_.length() < 1024 && this.font.wordWrapHeight(p_238774_1_, 114) <= 128;
	});

	@Nullable
	private EditClaimBookScreen.BookPage displayCache = EditClaimBookScreen.BookPage.EMPTY;


	/**
	 * 
	 * @param player
	 * @param itemStack
	 * @param hand
	 */
	public EditClaimBookScreen(PlayerEntity player, ItemStack itemStack, Hand hand) {
		super(NarratorChatListener.NO_TITLE);
		this.owner = player;
		this.book = itemStack;
		this.hand = hand;

		// load the text from the item
		CompoundNBT nbt = itemStack.getTag();
		if (nbt != null) {
			ListNBT listNbt = nbt.getList(PAGES_TAG, 8).copy();

			// there is only 1 page to whitelist book
			//			for(int i = 0; i < listNbt.size(); ++i) {
			this.pages.add(listNbt.getString(0));
			//			}
		}

		if (this.pages.isEmpty()) {
			this.pages.add("");
		}
	}

	protected void init() {
		this.clearDisplayCache();
		this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

		this.doneButton = this.addButton(new Button(this.width / 2 + 2, 196, 98, 20, DialogTexts.GUI_DONE, (p_214204_1_) -> {
			this.minecraft.setScreen((Screen)null);
			this.saveChanges(false);
		}));
	}

	private void clearDisplayCache() {
		this.displayCache = null;
	}

	/**
	 * 
	 * @param text
	 */
	private void setClipboard(String text) {
		if (this.minecraft != null) {
			TextInputUtil.setClipboardContents(this.minecraft, text);
		}
	}

	/**
	 * 
	 * @return
	 */
	private String getClipboard() {
		return this.minecraft != null ? TextInputUtil.getClipboardContents(this.minecraft) : "";
	}

	private void eraseEmptyTrailingPages() {
		ListIterator<String> listiterator = this.pages.listIterator(this.pages.size());

		while(listiterator.hasPrevious() && listiterator.previous().isEmpty()) {
			listiterator.remove();
		}
	}

	/**
	 * 
	 * @param finalize
	 */
	private void saveChanges(boolean finalize) {
		if (this.isModified) {
			this.eraseEmptyTrailingPages();
			ListNBT listnbt = new ListNBT();
			this.pages.stream().map(StringNBT::valueOf).forEach(listnbt::add);
			if (!this.pages.isEmpty()) {
				this.book.addTagElement(PAGES_TAG, listnbt);
			}

			int i = this.hand == Hand.MAIN_HAND ? this.owner.inventory.selected : 40;
			// TODO replace with custom message to server
			//		         this.minecraft.getConnection().send(new CEditBookPacket(this.book, finalize, i));
		}
	}

	public void removed() {
		this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
	}

	/**
	 * 
	 */
	public boolean keyPressed(int p_231046_1_, int p_231046_2_, int p_231046_3_) {
		if (super.keyPressed(p_231046_1_, p_231046_2_, p_231046_3_)) {
			return true;
		} else {
			boolean flag = this.bookKeyPressed(p_231046_1_, p_231046_2_, p_231046_3_);
			if (flag) {
				this.clearDisplayCache();
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean charTyped(char p_231042_1_, int p_231042_2_) {
		if (super.charTyped(p_231042_1_, p_231042_2_)) {
			return true;
		} else if (SharedConstants.isAllowedChatCharacter(p_231042_1_)) {
			this.pageEdit.insertText(Character.toString(p_231042_1_));
			this.clearDisplayCache();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param p_214230_1_
	 * @param p_214230_2_
	 * @param p_214230_3_
	 * @return
	 */
	private boolean bookKeyPressed(int p_214230_1_, int p_214230_2_, int p_214230_3_) {
		if (Screen.isSelectAll(p_214230_1_)) {
			this.pageEdit.selectAll();
			return true;
		} else if (Screen.isCopy(p_214230_1_)) {
			this.pageEdit.copy();
			return true;
		} else if (Screen.isPaste(p_214230_1_)) {
			this.pageEdit.paste();
			return true;
		} else if (Screen.isCut(p_214230_1_)) {
			this.pageEdit.cut();
			return true;
		} else {
			switch(p_214230_1_) {
			case 257:
			case 335:
				this.pageEdit.insertText("\n");
				return true;
			case 259:
				this.pageEdit.removeCharsFromCursor(-1);
				return true;
			case 261:
				this.pageEdit.removeCharsFromCursor(1);
				return true;
			case 262:
				this.pageEdit.moveByChars(1, Screen.hasShiftDown());
				return true;
			case 263:
				this.pageEdit.moveByChars(-1, Screen.hasShiftDown());
				return true;
			case 264:
				this.keyDown();
				return true;
			case 265:
				this.keyUp();
				return true;
				//			case 266:
				//				this.backButton.onPress();
				//				return true;
				//			case 267:
				//				this.forwardButton.onPress();
				//				return true;
			case 268:
				this.keyHome();
				return true;
			case 269:
				this.keyEnd();
				return true;
			default:
				return false;
			}
		}
	}

	private void keyUp() {
		this.changeLine(-1);
	}

	private void keyDown() {
		this.changeLine(1);
	}

	private void changeLine(int p_238755_1_) {
		int i = this.pageEdit.getCursorPos();
		int j = this.getDisplayCache().changeLine(i, p_238755_1_);
		this.pageEdit.setCursorPos(j, Screen.hasShiftDown());
	}

	private void keyHome() {
		int i = this.pageEdit.getCursorPos();
		int j = this.getDisplayCache().findLineStart(i);
		this.pageEdit.setCursorPos(j, Screen.hasShiftDown());
	}

	private void keyEnd() {
		EditClaimBookScreen.BookPage editbookscreen$bookpage = this.getDisplayCache();
		int i = this.pageEdit.getCursorPos();
		int j = editbookscreen$bookpage.findLineEnd(i);
		this.pageEdit.setCursorPos(j, Screen.hasShiftDown());
	}

	private String getCurrentPageText() {
		return this.currentPage >= 0 && this.currentPage < this.pages.size() ? this.pages.get(this.currentPage) : "";
	}

	private void setCurrentPageText(String text) {
		if (this.currentPage >= 0 && this.currentPage < this.pages.size()) {
			this.pages.set(this.currentPage, text);
			this.isModified = true;
			this.clearDisplayCache();
		}
	}

	/**
	 * 
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void render(MatrixStack matrixStack, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
		this.renderBackground(matrixStack);
		this.setFocused((IGuiEventListener)null);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bind(ReadBookScreen.BOOK_LOCATION);
		int i = (this.width - 192) / 2;
		this.blit(matrixStack, i, 2, 0, 0, 192, 192);
		int j1 = this.font.width(this.pageMsg);
		this.font.draw(matrixStack, this.pageMsg, (float)(i - j1 + 192 - 44), 18.0F, 0);
		EditClaimBookScreen.BookPage editbookscreen$bookpage = this.getDisplayCache();

		for(EditClaimBookScreen.BookLine editbookscreen$bookline : editbookscreen$bookpage.lines) {
			this.font.draw(matrixStack, editbookscreen$bookline.asComponent, (float)editbookscreen$bookline.x, (float)editbookscreen$bookline.y, -16777216);
		}

		this.renderHighlight(editbookscreen$bookpage.selection);
		this.renderCursor(matrixStack, editbookscreen$bookpage.cursor, editbookscreen$bookpage.cursorAtEnd);

		super.render(matrixStack, p_230430_2_, p_230430_3_, p_230430_4_);
	}

	/**
	 * 
	 * @param rectangle
	 */
	private void renderHighlight(Rectangle2d[] rectangle) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuilder();
		RenderSystem.color4f(0.0F, 0.0F, 255.0F, 255.0F);
		RenderSystem.disableTexture();
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION);

		for(Rectangle2d rectangle2d : rectangle) {
			int i = rectangle2d.getX();
			int j = rectangle2d.getY();
			int k = i + rectangle2d.getWidth();
			int l = j + rectangle2d.getHeight();
			bufferbuilder.vertex((double)i, (double)l, 0.0D).endVertex();
			bufferbuilder.vertex((double)k, (double)l, 0.0D).endVertex();
			bufferbuilder.vertex((double)k, (double)j, 0.0D).endVertex();
			bufferbuilder.vertex((double)i, (double)j, 0.0D).endVertex();
		}

		tessellator.end();
		RenderSystem.disableColorLogicOp();
		RenderSystem.enableTexture();
	}

	private void renderCursor(MatrixStack matrixStack, EditClaimBookScreen.Point p_238756_2_, boolean p_238756_3_) {
		if (this.frameTick / 6 % 2 == 0) {
			p_238756_2_ = this.convertLocalToScreen(p_238756_2_);
			if (!p_238756_3_) {
				AbstractGui.fill(matrixStack, p_238756_2_.x, p_238756_2_.y - 1, p_238756_2_.x + 1, p_238756_2_.y + 9, -16777216);
			} else {
				this.font.draw(matrixStack, "_", (float)p_238756_2_.x, (float)p_238756_2_.y, 0);
			}
		}
	}

	private EditClaimBookScreen.BookPage getDisplayCache() {
		if (this.displayCache == null) {
			this.displayCache = this.rebuildDisplayCache();
			this.pageMsg = new TranslationTextComponent("book.pageIndicator", this.currentPage + 1, this.getNumPages());
		}

		return this.displayCache;
	}

	private EditClaimBookScreen.BookPage rebuildDisplayCache() {
		String s = this.getCurrentPageText();
		if (s.isEmpty()) {
			return EditClaimBookScreen.BookPage.EMPTY;
		} else {
			int i = this.pageEdit.getCursorPos();
			int j = this.pageEdit.getSelectionPos();
			IntList intlist = new IntArrayList();
			List<EditClaimBookScreen.BookLine> list = Lists.newArrayList();
			MutableInt mutableint = new MutableInt();
			MutableBoolean mutableboolean = new MutableBoolean();
			CharacterManager charactermanager = this.font.getSplitter();
			charactermanager.splitLines(s, 114, Style.EMPTY, true, (p_238762_6_, p_238762_7_, p_238762_8_) -> {
				int k3 = mutableint.getAndIncrement();
				String s2 = s.substring(p_238762_7_, p_238762_8_);
				mutableboolean.setValue(s2.endsWith("\n"));
				String s3 = StringUtils.stripEnd(s2, " \n");
				int l3 = k3 * 9;
				EditClaimBookScreen.Point EditClaimBookScreen$point1 = this.convertLocalToScreen(new EditClaimBookScreen.Point(0, l3));
				intlist.add(p_238762_7_);
				list.add(new EditClaimBookScreen.BookLine(p_238762_6_, s3, EditClaimBookScreen$point1.x, EditClaimBookScreen$point1.y));
			});
			int[] aint = intlist.toIntArray();
			boolean flag = i == s.length();
			EditClaimBookScreen.Point EditClaimBookScreen$point;
			if (flag && mutableboolean.isTrue()) {
				EditClaimBookScreen$point = new EditClaimBookScreen.Point(0, list.size() * 9);
			} else {
				int k = findLineFromPos(aint, i);
				int l = this.font.width(s.substring(aint[k], i));
				EditClaimBookScreen$point = new EditClaimBookScreen.Point(l, k * 9);
			}

			List<Rectangle2d> list1 = Lists.newArrayList();
			if (i != j) {
				int l2 = Math.min(i, j);
				int i1 = Math.max(i, j);
				int j1 = findLineFromPos(aint, l2);
				int k1 = findLineFromPos(aint, i1);
				if (j1 == k1) {
					int l1 = j1 * 9;
					int i2 = aint[j1];
					list1.add(this.createPartialLineSelection(s, charactermanager, l2, i1, l1, i2));
				} else {
					int i3 = j1 + 1 > aint.length ? s.length() : aint[j1 + 1];
					list1.add(this.createPartialLineSelection(s, charactermanager, l2, i3, j1 * 9, aint[j1]));

					for(int j3 = j1 + 1; j3 < k1; ++j3) {
						int j2 = j3 * 9;
						String s1 = s.substring(aint[j3], aint[j3 + 1]);
						int k2 = (int)charactermanager.stringWidth(s1);
						list1.add(this.createSelection(new EditClaimBookScreen.Point(0, j2), new EditClaimBookScreen.Point(k2, j2 + 9)));
					}

					list1.add(this.createPartialLineSelection(s, charactermanager, aint[k1], i1, k1 * 9, aint[k1]));
				}
			}

			return new EditClaimBookScreen.BookPage(s, EditClaimBookScreen$point, flag, aint, list.toArray(new EditClaimBookScreen.BookLine[0]), list1.toArray(new Rectangle2d[0]));
		}
	}

	private Point convertLocalToScreen(Point p_238767_1_) {
		return new Point(p_238767_1_.x + (this.width - 192) / 2 + 36, p_238767_1_.y + 32);
	}

	private Point convertScreenToLocal(Point p_238758_1_) {
		return new Point(p_238758_1_.x - (this.width - 192) / 2 - 36, p_238758_1_.y - 32);
	}

	public boolean mouseClicked(double p_231044_1_, double p_231044_3_, int p_231044_5_) {
		if (super.mouseClicked(p_231044_1_, p_231044_3_, p_231044_5_)) {
			return true;
		} else {
			if (p_231044_5_ == 0) {
				long i = Util.getMillis();
				EditClaimBookScreen.BookPage editbookscreen$bookpage = this.getDisplayCache();
				int j = editbookscreen$bookpage.getIndexAtPosition(this.font, this.convertScreenToLocal(new EditClaimBookScreen.Point((int)p_231044_1_, (int)p_231044_3_)));
				if (j >= 0) {
					if (j == this.lastIndex && i - this.lastClickTime < 250L) {
						if (!this.pageEdit.isSelecting()) {
							this.selectWord(j);
						} else {
							this.pageEdit.selectAll();
						}
					} else {
						this.pageEdit.setCursorPos(j, Screen.hasShiftDown());
					}

					this.clearDisplayCache();
				}

				this.lastIndex = j;
				this.lastClickTime = i;
			}

			return true;
		}
	}

	private void selectWord(int position) {
		String s = this.getCurrentPageText();
		this.pageEdit.setSelectionRange(CharacterManager.getWordPosition(s, -1, position, false), CharacterManager.getWordPosition(s, 1, position, false));
	}

	public boolean mouseDragged(double p_231045_1_, double p_231045_3_, int p_231045_5_, double p_231045_6_, double p_231045_8_) {
		if (super.mouseDragged(p_231045_1_, p_231045_3_, p_231045_5_, p_231045_6_, p_231045_8_)) {
			return true;
		} else {
			if (p_231045_5_ == 0) {
				BookPage bookPage = this.getDisplayCache();
				int i = bookPage.getIndexAtPosition(this.font, this.convertScreenToLocal(new Point((int)p_231045_1_, (int)p_231045_3_)));
				this.pageEdit.setCursorPos(i, true);
				this.clearDisplayCache();
			}

			return true;
		}
	}

	private Rectangle2d createPartialLineSelection(String p_238761_1_, CharacterManager p_238761_2_, int p_238761_3_, int p_238761_4_, int p_238761_5_, int p_238761_6_) {
		String s = p_238761_1_.substring(p_238761_6_, p_238761_3_);
		String s1 = p_238761_1_.substring(p_238761_6_, p_238761_4_);
		Point editbookscreen$point = new Point((int)p_238761_2_.stringWidth(s), p_238761_5_);
		Point editbookscreen$point1 = new Point((int)p_238761_2_.stringWidth(s1), p_238761_5_ + 9);
		return this.createSelection(editbookscreen$point, editbookscreen$point1);
	}

	private Rectangle2d createSelection(Point point1, Point point2) {
		Point screenPoint1 = this.convertLocalToScreen(point1);
		Point screenPoint2 = this.convertLocalToScreen(point2);
		int i = Math.min(screenPoint1.x, screenPoint2.x);
		int j = Math.max(screenPoint1.x, screenPoint2.x);
		int k = Math.min(screenPoint1.y, screenPoint2.y);
		int l = Math.max(screenPoint1.y, screenPoint2.y);
		return new Rectangle2d(i, k, j - i, l - k);
	}
	private int getNumPages() {
		return this.pages.size();
	}

	public void tick() {
		super.tick();
		++this.frameTick;
	}

	protected ItemStack getBook() {
		return book;
	}

	protected Hand getHand() {
		return hand;
	}


	protected List<String> getPages() {
		return pages;
	}

	private static int findLineFromPos(int[] p_238768_0_, int p_238768_1_) {
		int i = Arrays.binarySearch(p_238768_0_, p_238768_1_);
		return i < 0 ? -(i + 2) : i;
	}

	@OnlyIn(Dist.CLIENT)
	static class BookLine {
		private final Style style;
		private final String contents;
		private final ITextComponent asComponent;
		private final int x;
		private final int y;

		public BookLine(Style p_i232289_1_, String p_i232289_2_, int p_i232289_3_, int p_i232289_4_) {
			this.style = p_i232289_1_;
			this.contents = p_i232289_2_;
			this.x = p_i232289_3_;
			this.y = p_i232289_4_;
			this.asComponent = (new StringTextComponent(p_i232289_2_)).setStyle(p_i232289_1_);
		}
	}

	@OnlyIn(Dist.CLIENT)
	static class BookPage {
		private static final EditClaimBookScreen.BookPage EMPTY = new EditClaimBookScreen.BookPage("", new EditClaimBookScreen.Point(0, 0), true, new int[]{0}, new EditClaimBookScreen.BookLine[]{new EditClaimBookScreen.BookLine(Style.EMPTY, "", 0, 0)}, new Rectangle2d[0]);
		private final String fullText;
		private final EditClaimBookScreen.Point cursor;
		private final boolean cursorAtEnd;
		private final int[] lineStarts;
		private final EditClaimBookScreen.BookLine[] lines;
		private final Rectangle2d[] selection;

		public BookPage(String text, EditClaimBookScreen.Point point, boolean cursorAtEnd, int[] lineStarts, 
				EditClaimBookScreen.BookLine[] lines, Rectangle2d[] selection) {
			this.fullText = text;
			this.cursor = point;
			this.cursorAtEnd = cursorAtEnd;
			this.lineStarts = lineStarts;
			this.lines = lines;
			this.selection = selection;
		}

		public int getIndexAtPosition(FontRenderer p_238789_1_, EditClaimBookScreen.Point p_238789_2_) {
			int i = p_238789_2_.y / 9;
			if (i < 0) {
				return 0;
			} else if (i >= this.lines.length) {
				return this.fullText.length();
			} else {
				EditClaimBookScreen.BookLine EditClaimBookscreen$bookline = this.lines[i];
				return this.lineStarts[i] + p_238789_1_.getSplitter().plainIndexAtWidth(EditClaimBookscreen$bookline.contents, p_238789_2_.x, EditClaimBookscreen$bookline.style);
			}
		}

		public int changeLine(int p_238788_1_, int p_238788_2_) {
			int i = EditClaimBookScreen.findLineFromPos(this.lineStarts, p_238788_1_);
			int j = i + p_238788_2_;
			int k;
			if (0 <= j && j < this.lineStarts.length) {
				int l = p_238788_1_ - this.lineStarts[i];
				int i1 = this.lines[j].contents.length();
				k = this.lineStarts[j] + Math.min(l, i1);
			} else {
				k = p_238788_1_;
			}

			return k;
		}

		public int findLineStart(int p_238787_1_) {
			int i = EditClaimBookScreen.findLineFromPos(this.lineStarts, p_238787_1_);
			return this.lineStarts[i];
		}

		public int findLineEnd(int p_238791_1_) {
			int i = EditClaimBookScreen.findLineFromPos(this.lineStarts, p_238791_1_);
			return this.lineStarts[i] + this.lines[i].contents.length();
		}
	}

	@OnlyIn(Dist.CLIENT)
	static class Point {
		public final int x;
		public final int y;

		Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
}
