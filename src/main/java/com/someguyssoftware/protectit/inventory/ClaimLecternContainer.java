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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 
 * @author Mark Gottschling on Nov 18, 2021
 *
 */
public class ClaimLecternContainer extends Container {
	private final IInventory lectern;
	private final IIntArray lecternData;

	public static ClaimLecternContainer create(int windowID, PlayerInventory playerInventory, PacketBuffer extraData) {
		return new ClaimLecternContainer(windowID);
	}

	/**
	 * Client-side constructor
	 * 
	 * @param windowId
	 * @param containerType
	 * @param playerInventory
	 * @param slotCount
	 */
	private ClaimLecternContainer(int windowID) {
		this(windowID, new Inventory(1), new IntArray(1));
	}

	/**
	 * 
	 * @param windowID
	 * @param inventory
	 * @param data
	 */
	public ClaimLecternContainer(int windowID, IInventory inventory, IIntArray data) {
		super(ProtectItContainers.CLAIM_LECTERN_CONTAINER_TYPE, windowID);
		checkContainerSize(inventory, 1);
		checkContainerDataCount(data, 1);
		this.lectern = inventory;
		this.lecternData = data;
		this.addSlot(new Slot(inventory, 0, 0, 0) {
			public void setChanged() {
				super.setChanged();
				ClaimLecternContainer.this.slotsChanged(this.container);
			}
		});
		this.addDataSlots(data);
	}

	public boolean clickMenuButton(PlayerEntity player, int p_75140_2_) {
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

				ItemStack itemstack = this.lectern.removeItemNoUpdate(0);
				this.lectern.setChanged();
				if (!player.inventory.add(itemstack)) {
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
	public boolean stillValid(PlayerEntity player) {
		return this.lectern.stillValid(player);
	}

	@OnlyIn(Dist.CLIENT)
	public ItemStack getBook() {
		return this.lectern.getItem(0);
	}

	@Deprecated
	@OnlyIn(Dist.CLIENT)
	public int getPage() {
		return this.lecternData.get(0);
	}
}
