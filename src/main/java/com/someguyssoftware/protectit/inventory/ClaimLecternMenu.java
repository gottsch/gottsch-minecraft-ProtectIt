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
package com.someguyssoftware.protectit.inventory;

import com.someguyssoftware.protectit.claim.Property;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 
 * @author Mark Gottschling on Nov 18, 2021
 *
 */
public class ClaimLecternMenu extends AbstractContainerMenu {
	private final Container lectern;
	private final ContainerData lecternData;

	private Property claim;
	
	/**
	 * 
	 * @param windowID
	 * @param playerInventory
	 * @param extraData
	 * @return
	 */
	public static ClaimLecternMenu create(int windowID, Inventory playerInventory, FriendlyByteBuf extraData) {
		return new ClaimLecternMenu(windowID);
	}

	/**
	 * Client-side constructor
	 * 
	 * @param windowId
	 * @param containerType
	 * @param playerInventory
	 * @param slotCount
	 */
	public ClaimLecternMenu(int windowID) {
		this(windowID, new SimpleContainer(1), new SimpleContainerData(1));
	}

	/**
	 * 
	 * @param windowID
	 * @param inventory
	 * @param data
	 */
	public ClaimLecternMenu(int windowID, Container inventory, ContainerData data) {
		super(ProtectItContainers.CLAIM_LECTERN_CONTAINER_TYPE.get(), windowID);
		checkContainerSize(inventory, 1);
		checkContainerDataCount(data, 1);
		this.lectern = inventory;
		this.lecternData = data;
		this.addSlot(new Slot(inventory, 0, 0, 0) {
			public void setChanged() {
				super.setChanged();
				ClaimLecternMenu.this.slotsChanged(this.container);
			}
		});
		this.addDataSlots(data);
	}

	public boolean clickMenuButton(Player player, int p_75140_2_) {
		if (p_75140_2_ >= 100) {
			int k = p_75140_2_ - 100;
			this.setData(0, k);
			return true;
		} else {
			switch (p_75140_2_) {
			case 1:
				int j = this.lecternData.get(0);
				this.setData(0, j - 1);
				return true;
			case 2:
				int i = this.lecternData.get(0);
				this.setData(0, i + 1);
				return true;
			case 3:
				if (!player.mayBuild()) {
					return false;
				}

				// take only if the owner of the claim
				if (getClaim() != null && !player.getStringUUID().equalsIgnoreCase(getClaim().getOwner().getUuid())) {
					player.sendMessage(new TranslatableComponent("message.protectit.block_region_not_owner"), player.getUUID());
					return false;
				}
				
				ItemStack itemstack = this.lectern.removeItemNoUpdate(0);
				this.lectern.setChanged();
				if (!player.getInventory().add(itemstack)) {
					player.drop(itemstack, false);
				}

				return true;
			default:
				return false;
			}
		}
	}

	public void setData(int p_75137_1_, int p_75137_2_) {
		super.setData(p_75137_1_, p_75137_2_);
		this.broadcastChanges();
	}

	@Override
	public boolean stillValid(Player player) {
		return this.lectern.stillValid(player);
	}

	public ItemStack getBook() {
		return this.lectern.getItem(0);
	}

	@Deprecated
	public int getPage() {
		return this.lecternData.get(0);
	}

	public Property getClaim() {
		return claim;
	}

	public void setClaim(Property claim) {
		this.claim = claim;
	}
}
