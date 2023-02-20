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

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Property;
import com.someguyssoftware.protectit.item.ClaimBook;
import com.someguyssoftware.protectit.network.ClaimBookMessageToServer;
import com.someguyssoftware.protectit.network.ProtectItNetworking;
import com.someguyssoftware.protectit.registry.PlayerData;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


/**
 * @author Mark Gottschling on Nov 16, 2021
 *
 */
// REMOVE until a sutiable GUI is added.
@Deprecated
public class EditClaimBookScreen extends Screen {
	private static final String PLAYER_DATA_TAG = "playerData";

	// context properties
	private final Player owner;
	private final ItemStack book;
	private final InteractionHand hand;

	// buttons
	private Button doneButton;

	// transient book state properties
	private final List<String> pages = Lists.newArrayList();
	//	private boolean isModified;
	private int frameTick;
	private int currentPage;
	private long lastClickTime;
	private int lastIndex = -1;

	// persistent book state properties
	private final List<PlayerData> playerDataCache = Lists.newArrayList();
	private Property claim;

	private final TextFieldHelper pageEdit = new TextFieldHelper(this::getCurrentPageText, this::setCurrentPageText, this::getClipboard, this::setClipboard, (p_238774_1_) -> {
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
	public EditClaimBookScreen(Player player, ItemStack itemStack, InteractionHand hand) {
		super(NarratorChatListener.NO_TITLE);
		this.owner = player;
		this.book = itemStack;
		this.hand = hand;

		// load the data from the item
		getPlayerDataCache().addAll(ClaimBook.loadPlayerData(itemStack));
		// update the cache
		if (!getPlayerDataCache().isEmpty()) {
			this.pages.add(getPlayerDataCache().stream().map(data -> data.getName()).collect(Collectors.joining("\n")));
		}

		// load the claim
		setClaim(ClaimBook.loadClaim(itemStack));

		if (this.pages.isEmpty()) {
			this.pages.add("");
		}
	}

	protected void init() {
		this.clearDisplayCache();
		this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

		this.doneButton = this.addRenderableWidget(new Button(this.width / 2 + 2, 196, 98, 20, CommonComponents.GUI_DONE, (p_214204_1_) -> {
			ProtectIt.LOGGER.debug("clicked done.");
			this.minecraft.setScreen((Screen)null);
			this.saveChanges();
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
			TextFieldHelper.setClipboardContents(this.minecraft, text);
		}
	}

	/**
	 * 
	 * @return
	 */
	private String getClipboard() {
		return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
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
	private void saveChanges() {
		// always perform the name checks
		this.eraseEmptyTrailingPages();

		// save the player data
		List<PlayerData> workingData = Lists.newArrayList();
		this.pages.forEach(entry -> {
			List<String> playerNames = Arrays.asList(entry.split("\n"));
			playerNames.forEach(playerName -> {
				// check if playerName already exists in playerData
				boolean playerDataAdded = false;
				for (PlayerData playerData : getPlayerDataCache()) {
					if (playerData.getName().equalsIgnoreCase(playerName)) {
						playerDataAdded = workingData.add(playerData);
						break;
					}
				}
				// player name was not found in list
				if (!playerDataAdded) {
					PlayerData playerData = new PlayerData("", playerName);
					workingData.add(playerData);
				}
			});
		});

		// clear the player data cache
		getPlayerDataCache().clear();
		// copy all from working to cache
		getPlayerDataCache().addAll(workingData);
		ProtectIt.LOGGER.debug("playerDataCache.size -> {}", getPlayerDataCache().size());

		// save the data cache to the item
		ListTag playerDataList = new ListTag();
		getPlayerDataCache().forEach(playerData -> {
			CompoundTag nbt = new CompoundTag();
			ProtectIt.LOGGER.debug("saving/adding player data -> {}", playerData);
			playerData.save(nbt);
			ProtectIt.LOGGER.info("result nbt uuid -> {}", nbt.getString("uuid"));
			ProtectIt.LOGGER.info("result nbt name -> {}", nbt.getString("name"));
			playerDataList.add(nbt);
			ProtectIt.LOGGER.debug("playerDataList.size -> {}", playerDataList.size());
		});
		this.book.addTagElement(PLAYER_DATA_TAG, playerDataList);

		int slot = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().selected : 40;
		ClaimBookMessageToServer messageToServer = new ClaimBookMessageToServer(this.book, slot);
		ProtectItNetworking.channel.sendToServer(messageToServer);

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
	 * @param key
	 * @param p_214230_2_
	 * @param p_214230_3_
	 * @return
	 */
	private boolean bookKeyPressed(int key, int p_214230_2_, int p_214230_3_) {
		if (Screen.isSelectAll(key)) {
			this.pageEdit.selectAll();
			return true;
		} else if (Screen.isCopy(key)) {
			this.pageEdit.copy();
			return true;
		} else if (Screen.isPaste(key)) {
			this.pageEdit.paste();
			return true;
		} else if (Screen.isCut(key)) {
			this.pageEdit.cut();
			return true;
		} else {
			switch(key) {
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
			//			this.isModified = true;
			this.clearDisplayCache();
		}
	}

	/**
	 * 
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void render(PoseStack matrixStack, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
		this.renderBackground(matrixStack);
		this.setFocused((GuiEventListener)null);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, BookViewScreen.BOOK_LOCATION);		int startX = (this.width - 192) / 2;
		this.blit(matrixStack, startX, 2, 0, 0, 192, 192);
		// add title
		TranslatableComponent title = new TranslatableComponent("label.protectit.claim_book.title", ChatFormatting.GOLD);
		int titleWidth = this.font.width(title);
		this.font.draw(matrixStack, title, (float)((this.width/2) - (titleWidth/2)), 18.0F, 0);

		EditClaimBookScreen.BookPage page = this.getDisplayCache();
		for(EditClaimBookScreen.BookLine line : page.lines) {
			//			String name = line.contents;
			//			Optional<String> uuid = getPlayerDataCache().stream().filter(data -> data.getName().equals(name)).map(data -> data.getUuid()).findAny();

			//			IFormattableTextComponent nameText = new StringTextComponent(name);
			//			if (!uuid.isPresent() || (uuid.isPresent() && uuid.get().isEmpty())) {
			//				nameText = nameText.withStyle(TextFormatting.RED);
			//			}
			this.font.draw(matrixStack, line.asComponent, (float)line.x, (float)line.y, -16777216);
		}

		this.renderHighlight(page.selection);
		this.renderCursor(matrixStack, page.cursor, page.cursorAtEnd);

		super.render(matrixStack, p_230430_2_, p_230430_3_, p_230430_4_);
	}

	/**
	 * 
	 * @param rectangle
	 */
	   private void renderHighlight(Rect2i[] p_98139_) {
		      Tesselator tesselator = Tesselator.getInstance();
		      BufferBuilder bufferbuilder = tesselator.getBuilder();
		      RenderSystem.setShader(GameRenderer::getPositionShader);
		      RenderSystem.setShaderColor(0.0F, 0.0F, 255.0F, 255.0F);
		      RenderSystem.disableTexture();
		      RenderSystem.enableColorLogicOp();
		      RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
		      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

		      for(Rect2i rect2i : p_98139_) {
		         int i = rect2i.getX();
		         int j = rect2i.getY();
		         int k = i + rect2i.getWidth();
		         int l = j + rect2i.getHeight();
		         bufferbuilder.vertex((double)i, (double)l, 0.0D).endVertex();
		         bufferbuilder.vertex((double)k, (double)l, 0.0D).endVertex();
		         bufferbuilder.vertex((double)k, (double)j, 0.0D).endVertex();
		         bufferbuilder.vertex((double)i, (double)j, 0.0D).endVertex();
		      }

		      tesselator.end();
		      RenderSystem.disableColorLogicOp();
		      RenderSystem.enableTexture();
		   }

	private void renderCursor(PoseStack matrixStack, EditClaimBookScreen.Point p_238756_2_, boolean p_238756_3_) {
		if (this.frameTick / 6 % 2 == 0) {
			p_238756_2_ = this.convertLocalToScreen(p_238756_2_);
			if (!p_238756_3_) {
				GuiComponent.fill(matrixStack, p_238756_2_.x, p_238756_2_.y - 1, p_238756_2_.x + 1, p_238756_2_.y + 9, -16777216);
			} else {
				this.font.draw(matrixStack, "_", (float)p_238756_2_.x, (float)p_238756_2_.y, 0);
			}
		}
	}

	private EditClaimBookScreen.BookPage getDisplayCache() {
		if (this.displayCache == null) {
			this.displayCache = this.rebuildDisplayCache();
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
			StringSplitter stringSplitter = this.font.getSplitter();
			stringSplitter.splitLines(s, 114, Style.EMPTY, true, (defaultStyle, p_238762_7_, p_238762_8_) -> {
				int k3 = mutableint.getAndIncrement();
				String s2 = s.substring(p_238762_7_, p_238762_8_);
				mutableboolean.setValue(s2.endsWith("\n"));
				String s3 = StringUtils.stripEnd(s2, " \n");
				int l3 = k3 * 9;
				EditClaimBookScreen.Point EditClaimBookScreen$point1 = this.convertLocalToScreen(new EditClaimBookScreen.Point(0, l3));
				intlist.add(p_238762_7_);
				list.add(new EditClaimBookScreen.BookLine(defaultStyle, s3, EditClaimBookScreen$point1.x, EditClaimBookScreen$point1.y));
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

			List<Rect2i> list1 = Lists.newArrayList();
			if (i != j) {
				int l2 = Math.min(i, j);
				int i1 = Math.max(i, j);
				int j1 = findLineFromPos(aint, l2);
				int k1 = findLineFromPos(aint, i1);
				if (j1 == k1) {
					int l1 = j1 * 9;
					int i2 = aint[j1];
					list1.add(this.createPartialLineSelection(s, stringSplitter, l2, i1, l1, i2));
				} else {
					int i3 = j1 + 1 > aint.length ? s.length() : aint[j1 + 1];
					list1.add(this.createPartialLineSelection(s, stringSplitter, l2, i3, j1 * 9, aint[j1]));

					for(int j3 = j1 + 1; j3 < k1; ++j3) {
						int j2 = j3 * 9;
						String s1 = s.substring(aint[j3], aint[j3 + 1]);
						int k2 = (int)stringSplitter.stringWidth(s1);
						list1.add(this.createSelection(new EditClaimBookScreen.Point(0, j2), new EditClaimBookScreen.Point(k2, j2 + 9)));
					}

					list1.add(this.createPartialLineSelection(s, stringSplitter, aint[k1], i1, k1 * 9, aint[k1]));
				}
			}
			return new EditClaimBookScreen.BookPage(s, EditClaimBookScreen$point, flag, aint, list.toArray(new EditClaimBookScreen.BookLine[0]), list1.toArray(new Rect2i[0]));
		}
	}

	private Point convertLocalToScreen(Point point) {
		return new Point(point.x + (this.width - 192) / 2 + 36, point.y + 32);
	}

	private Point convertScreenToLocal(Point point) {
		return new Point(point.x - (this.width - 192) / 2 - 36, point.y - 32);
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
		this.pageEdit.setSelectionRange(StringSplitter.getWordPosition(s, -1, position, false), StringSplitter.getWordPosition(s, 1, position, false));
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

	private Rect2i createPartialLineSelection(String p_238761_1_, StringSplitter p_238761_2_, int p_238761_3_, int p_238761_4_, int p_238761_5_, int p_238761_6_) {
		String s = p_238761_1_.substring(p_238761_6_, p_238761_3_);
		String s1 = p_238761_1_.substring(p_238761_6_, p_238761_4_);
		Point editbookscreen$point = new Point((int)p_238761_2_.stringWidth(s), p_238761_5_);
		Point editbookscreen$point1 = new Point((int)p_238761_2_.stringWidth(s1), p_238761_5_ + 9);
		return this.createSelection(editbookscreen$point, editbookscreen$point1);
	}

	private Rect2i createSelection(Point point1, Point point2) {
		Point screenPoint1 = this.convertLocalToScreen(point1);
		Point screenPoint2 = this.convertLocalToScreen(point2);
		int i = Math.min(screenPoint1.x, screenPoint2.x);
		int j = Math.max(screenPoint1.x, screenPoint2.x);
		int k = Math.min(screenPoint1.y, screenPoint2.y);
		int l = Math.max(screenPoint1.y, screenPoint2.y);
		return new Rect2i(i, k, j - i, l - k);
	}

	public void tick() {
		super.tick();
		++this.frameTick;
	}

	protected ItemStack getBook() {
		return book;
	}

	protected InteractionHand getHand() {
		return hand;
	}

	@Deprecated
	protected List<String> getPages() {
		return pages;
	}

	private static int findLineFromPos(int[] p_238768_0_, int p_238768_1_) {
		int i = Arrays.binarySearch(p_238768_0_, p_238768_1_);
		return i < 0 ? -(i + 2) : i;
	}

	static class BookLine {
		private final Style style;
		private final String contents;
		private final Component asComponent;
		private final int x;
		private final int y;

		public BookLine(Style p_i232289_1_, String p_i232289_2_, int p_i232289_3_, int p_i232289_4_) {
			this.style = p_i232289_1_;
			this.contents = p_i232289_2_;
			this.x = p_i232289_3_;
			this.y = p_i232289_4_;
			this.asComponent = (new TextComponent(p_i232289_2_)).setStyle(p_i232289_1_);
		}
	}

	/**
	 *
	 */
	static class BookPage {
		private static final EditClaimBookScreen.BookPage EMPTY = new EditClaimBookScreen.BookPage("", new EditClaimBookScreen.Point(0, 0), true, new int[]{0}, new EditClaimBookScreen.BookLine[]{new EditClaimBookScreen.BookLine(Style.EMPTY, "", 0, 0)}, new Rect2i[0]);
		private final String fullText;
		private final EditClaimBookScreen.Point cursor;
		private final boolean cursorAtEnd;
		private final int[] lineStarts;
		private final EditClaimBookScreen.BookLine[] lines;
		private final Rect2i[] selection;

		public BookPage(String text, EditClaimBookScreen.Point point, boolean cursorAtEnd, int[] lineStarts, 
				EditClaimBookScreen.BookLine[] lines, Rect2i[] selection) {
			this.fullText = text;
			this.cursor = point;
			this.cursorAtEnd = cursorAtEnd;
			this.lineStarts = lineStarts;
			this.lines = lines;
			this.selection = selection;
		}

		public int getIndexAtPosition(Font p_238789_1_, EditClaimBookScreen.Point p_238789_2_) {
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

	/**
	 *
	 */
	static class Point {
		public final int x;
		public final int y;

		Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	protected List<PlayerData> getPlayerDataCache() {
		return playerDataCache;
	}

	protected Property getClaim() {
		return claim;
	}

	protected void setClaim(Property claim) {
		this.claim = claim;
	}
}
