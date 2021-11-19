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
package com.someguyssoftware.protectit.tileentity;

import javax.annotation.Nullable;

import com.someguyssoftware.gottschcore.tileentity.AbstractModTileEntity;
import com.someguyssoftware.protectit.block.ClaimLectern;
import com.someguyssoftware.protectit.inventory.ClaimLecternContainer;
import com.someguyssoftware.protectit.item.ClaimBook;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IClearable;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * 
 * @author Mark Gottschling on Nov 16, 2021
 *
 */
public class ClaimLecternTileEntity extends AbstractModTileEntity implements IClearable, INamedContainerProvider {
	private static final String BOOK_TAG = "book";
	private static final String PAGE_TAG = "page";

	private ItemStack book = ItemStack.EMPTY;
	private int page;
	// private int pageCount;

	private final IIntArray dataAccess = new IIntArray() {
		public int get(int index) {
			return index == 0 ? ClaimLecternTileEntity.this.page : 0;
		}

		public void set(int index, int value) {
			if (index == 0) {
				ClaimLecternTileEntity.this.setPage(value);
			}
		}

		public int getCount() {
			return 1;
		}
	};

	/**
	 * 
	 */
	public ClaimLecternTileEntity() {
		super(ProtectItTileEntities.CLAIM_LECTERN_TILE_ENTITY_TYPE);
	}

	@Override
	public ITextComponent getDisplayName() {
		return new TranslationTextComponent("container.claim_lectern");
	}

	public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity player) {
		return new ClaimLecternContainer(id, this.bookAccess, this.dataAccess);
	}

	/**
	 * 
	 * @return
	 */
	public boolean hasBook() {
		Item item = this.book.getItem();
		return (item instanceof ClaimBook);
	}

	/**
	 * 
	 */
	private void onBookItemRemove() {
		this.page = 0;
		// this.pageCount = 0;
		ClaimLectern.resetBookState(this.getLevel(), this.getBlockPos(), this.getBlockState(), false);
	}

	@Override
	public boolean onlyOpCanSetNbt() {
		return true;
	}

	public void clearContent() {
		this.setBook(ItemStack.EMPTY);
	}

	/**
	 * 
	 */
	@Override
	public void load(BlockState state, CompoundNBT nbt) {
		super.load(state, nbt);
		if (nbt.contains(BOOK_TAG, 10)) {
			this.book = ItemStack.of(nbt.getCompound(BOOK_TAG));
		} else {
			this.book = ItemStack.EMPTY;
		}
		// this.pageCount = WrittenBookItem.getPageCount(this.book);
		this.page = nbt.getInt(PAGE_TAG);
	}

	/**
	 * 
	 */
	@Override
	public CompoundNBT save(CompoundNBT nbt) {
		super.save(nbt);
		if (!this.getBook().isEmpty()) {
			nbt.put(BOOK_TAG, this.getBook().save(new CompoundNBT()));
			nbt.putInt(PAGE_TAG, this.page);
		}
		return nbt;
	}

	public ItemStack getBook() {
		return this.book;
	}

	public void setBook(ItemStack itemStack) {
		this.setBook(itemStack, (PlayerEntity) null);
	}

	public void setBook(ItemStack itemStack, @Nullable PlayerEntity player) {
		this.book = itemStack;
		this.page = 0;
		// this.pageCount = WrittenBookItem.getPageCount(this.book);
		// this.setChanged();
	}

	private void setPage(int page) {
		// there is only one page - this is deprecated
		// int i = MathHelper.clamp(page, 0, this.pageCount - 1);
		// if (i != this.page) {
		// this.page = i;
		// this.setChanged();
		// LecternBlock.signalPageChange(this.getLevel(), this.getBlockPos(),
		// this.getBlockState());
		// }
	}

	public int getPage() {
		return this.page;
	}

	/**
	 * 
	 */
	private final IInventory bookAccess = new IInventory() {
		public int getContainerSize() {
			return 1;
		}

		public boolean isEmpty() {
			return ClaimLecternTileEntity.this.book.isEmpty();
		}

		public ItemStack getItem(int p_70301_1_) {
			return p_70301_1_ == 0 ? ClaimLecternTileEntity.this.book : ItemStack.EMPTY;
		}

		public ItemStack removeItem(int p_70298_1_, int p_70298_2_) {
			if (p_70298_1_ == 0) {
				ItemStack itemstack = ClaimLecternTileEntity.this.book.split(p_70298_2_);
				if (ClaimLecternTileEntity.this.book.isEmpty()) {
					ClaimLecternTileEntity.this.onBookItemRemove();
				}

				return itemstack;
			} else {
				return ItemStack.EMPTY;
			}
		}

		public ItemStack removeItemNoUpdate(int slot) {
			if (slot == 0) {
				ItemStack itemstack = ClaimLecternTileEntity.this.book;
				ClaimLecternTileEntity.this.book = ItemStack.EMPTY;
				ClaimLecternTileEntity.this.onBookItemRemove();
				return itemstack;
			} else {
				return ItemStack.EMPTY;
			}
		}

		public void setItem(int slot, ItemStack itemStack) {
		}

		public int getMaxStackSize() {
			return 1;
		}

		public void setChanged() {
			ClaimLecternTileEntity.this.setChanged();
		}

		public boolean stillValid(PlayerEntity player) {
			if (ClaimLecternTileEntity.this.level
					.getBlockEntity(ClaimLecternTileEntity.this.worldPosition) != ClaimLecternTileEntity.this) {
				return false;
			} else {
				return player.distanceToSqr((double) ClaimLecternTileEntity.this.worldPosition.getX() + 0.5D,
						(double) ClaimLecternTileEntity.this.worldPosition.getY() + 0.5D,
						(double) ClaimLecternTileEntity.this.worldPosition.getZ() + 0.5D) > 64.0D ? false
								: ClaimLecternTileEntity.this.hasBook();
			}
		}

		public boolean canPlaceItem(int p_94041_1_, ItemStack itemStack) {
			return false;
		}

		public void clearContent() {
		}
	};
}
