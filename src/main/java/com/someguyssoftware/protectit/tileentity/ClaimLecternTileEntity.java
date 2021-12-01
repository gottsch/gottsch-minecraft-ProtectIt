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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.gottschcore.tileentity.AbstractModTileEntity;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ClaimLectern;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.inventory.ClaimLecternContainer;
import com.someguyssoftware.protectit.item.ClaimBook;
import com.someguyssoftware.protectit.item.ProtectItItems;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.registry.PlayerData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

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
	private static final String CLAIM_COORDS_TAG = "claimCoords";

	private ItemStack book = ItemStack.EMPTY;
	@Deprecated
	private int page;
	private ICoords claimCoords;

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
		ClaimLecternContainer container = new ClaimLecternContainer(id, this.bookAccess, this.dataAccess);
		Claim claim = ProtectionRegistries.block().getClaimByCoords(claimCoords);
		container.setClaim(claim);
		return container;
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
		this.book = ItemStack.EMPTY;
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

		if (nbt.contains(CLAIM_COORDS_TAG)) {
			setClaimCoords(WorldInfo.EMPTY_COORDS.load(nbt.getCompound(CLAIM_COORDS_TAG)));
		}
		if (nbt.contains(BOOK_TAG, 10)) {
			this.book = ItemStack.of(nbt.getCompound(BOOK_TAG));
		} else {
			this.book = ItemStack.EMPTY;
		}
		this.page = nbt.getInt(PAGE_TAG);
	}

	/**
	 * 
	 */
	@Override
	public CompoundNBT save(CompoundNBT nbt) {
		ProtectIt.LOGGER.debug("saving ClaimLecternTileEntity");
		super.save(nbt);

		if (getClaimCoords() != null) {
			CompoundNBT coordsNbt = new CompoundNBT();
			getClaimCoords().save(coordsNbt);
			nbt.put(CLAIM_COORDS_TAG, coordsNbt);
		}

		if (!this.getBook().isEmpty()) {
			nbt.put(BOOK_TAG, this.getBook().save(new CompoundNBT()));
			nbt.putInt(PAGE_TAG, this.page);
		}
		return nbt;
	}

	public ItemStack getBook() {
		return this.book;
	}

	/**
	 * 
	 * @param bookStack
	 */
	public void setBook(ItemStack bookStack) {
		// get the claim from the registry (use min coords as key instead of coords)
		Claim registryClaim = ProtectionRegistries.block().getClaimByCoords(getClaimCoords());

		if (registryClaim !=null) {
			if (bookStack != null && bookStack.getItem() == ProtectItItems.CLAIM_BOOK) {
				List<PlayerData> bookPlayerDataList = ClaimBook.loadPlayerData(bookStack);
				/*
				 * compare white lists and update
				 */
				// get list of everything in B (book list) that is net new to A (registry list)
				List<PlayerData> netNew = new ArrayList<>(bookPlayerDataList);
				netNew.removeAll(registryClaim.getWhitelist());
				ProtectIt.LOGGER.debug("net new list -> {}", netNew);
				// add net new to registryClaim
				registryClaim.getWhitelist()
				.addAll(netNew.stream().filter(data -> !data.getUuid().isEmpty()).collect(Collectors.toList()));
				ProtectIt.LOGGER.debug("registry claim white list after ADD net new -> {}", registryClaim.getWhitelist());

				// remove from registryClaim that are no longer contained in bookPlayerDataList.
				// note, not using difference of lists as only the name is tested, not the equals() of the object
				List<String> newNames = bookPlayerDataList.stream().map(data -> data.getName())
						.collect(Collectors.toList());
				registryClaim.getWhitelist().removeIf(data -> !newNames.contains(data.getName()));
				ProtectIt.LOGGER.debug("registry claim white list after REMOVE names -> {}", registryClaim.getWhitelist());

				// update this claim - mark as dirty
				ProtectItSavedData savedData = ProtectItSavedData.get(getLevel());
				if (savedData != null) {
					savedData.setDirty();
				}

				// update bookStack
				bookStack.removeTagKey("playerData");
				ClaimBook.savePlayerData(bookStack, bookPlayerDataList);
			}
			else {
				registryClaim.getWhitelist().clear();
			}
		}
		this.setBook(bookStack, (PlayerEntity) null);
	}

	public void setBook(ItemStack itemStack, @Nullable PlayerEntity player) {
		this.book = itemStack;
		this.page = 0;
		// this.pageCount = WrittenBookItem.getPageCount(this.book);
		this.setChanged();
	}

	@Deprecated
	private void setPage(int page) {
	}

	@Deprecated
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

		@Override
		public ItemStack removeItem(int slot, int size) {
			if (slot == 0) {
				ItemStack itemstack = ClaimLecternTileEntity.this.book.split(size);
				if (ClaimLecternTileEntity.this.book.isEmpty()) {
					ClaimLecternTileEntity.this.onBookItemRemove();
				}
				return itemstack;
			} else {
				return ItemStack.EMPTY;
			}
		}

		@Override
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

	public ICoords getClaimCoords() {
		return claimCoords;
	}

	public void setClaimCoords(ICoords claimCoords) {
		this.claimCoords = claimCoords;
	}
}
