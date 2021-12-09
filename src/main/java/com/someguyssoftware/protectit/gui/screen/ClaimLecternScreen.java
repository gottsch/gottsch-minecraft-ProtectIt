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
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.inventory.ClaimLecternContainer;
import com.someguyssoftware.protectit.item.ClaimBook;
import com.someguyssoftware.protectit.item.ProtectItItems;
import com.someguyssoftware.protectit.registry.PlayerData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * 
 * @author Mark Gottschling on Nov 19, 2021
 *
 */
public class ClaimLecternScreen extends ReadClaimBookScreen implements IHasContainer<ClaimLecternContainer> {

	private final IContainerListener listener = new IContainerListener() {
		public void refreshContainer(Container container, NonNullList<ItemStack> itemStack) {
			ClaimLecternScreen.this.bookChanged();
		}

		public void slotChanged(Container container, int slot, ItemStack itemStack) {
			ClaimLecternScreen.this.bookChanged();
		}

		public void setContainerData(Container container, int p_71112_2_, int p_71112_3_) {
		}
	};

	private final ClaimLecternContainer menu;
	private final PlayerEntity player;

	/**
	 * 
	 * @param container
	 * @param playerInventory
	 * @param text
	 */
	public ClaimLecternScreen(ClaimLecternContainer container, PlayerInventory playerInventory, ITextComponent text) {
		this.menu = container;
		this.player = playerInventory.player;
	}

	@Override
	protected void init() {
		super.init();
		this.menu.addSlotListener(this.listener);
	}

	@Override
	protected void createMenuControls() {
		if (this.minecraft.player.mayBuild()) {
			this.addButton(new Button(this.width / 2 - 100, 196, 98, 20, DialogTexts.GUI_DONE, (p_214181_1_) -> {
				this.minecraft.setScreen((Screen) null);
			}));
			this.addButton(new Button(this.width / 2 + 2, 196, 98, 20,
					new TranslationTextComponent("lectern.take_book"), (p_214178_1_) -> {
						this.sendButtonClick(3);
					}));

		} else {
			super.createMenuControls();
		}
	}

	private void sendButtonClick(int p_214179_1_) {
		this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, p_214179_1_);
	}

	@Override
	public ClaimLecternContainer getMenu() {
		return this.menu;
	}

	@Override
	public void onClose() {
		this.minecraft.player.closeContainer();
		super.onClose();
	}

	@Override
	public void removed() {
		super.removed();
		this.menu.removeSlotListener(this.listener);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/**
	 * 
	 */
	private void bookChanged() {
		ProtectIt.LOGGER.debug("bookChanged!");
		ItemStack bookStack = this.menu.getBook();
		ProtectIt.LOGGER.debug("book item? -> {}", bookStack);
		try {
			if (bookStack.getItem() == ProtectItItems.CLAIM_BOOK) {
				// TODO replace with ClaimBook.loadPlayerData()
				CompoundNBT bookNbt = bookStack.getTag();
				ListNBT list = bookNbt.getList("playerData", 10).copy();
				List<PlayerData> playerDataList = Lists.newArrayList();
				list.forEach(element -> {
					PlayerData playerData = new PlayerData().load((CompoundNBT) element);
					ProtectIt.LOGGER.debug("updating player names with name -> {}", playerData.getName());
					playerDataList.add(playerData);
				});
				this.setPlayerDataCache(playerDataList);
				this.setClaim(ClaimBook.loadClaim(bookStack));
			}
		} catch (Exception e) {
			ProtectIt.LOGGER.error("An error occurred reading the Claim Book data", e);
		}
	}
}
