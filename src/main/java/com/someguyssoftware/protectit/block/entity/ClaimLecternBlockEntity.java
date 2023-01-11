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
package com.someguyssoftware.protectit.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ClaimLectern;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.inventory.ClaimLecternMenu;
import com.someguyssoftware.protectit.item.ClaimBook;
import com.someguyssoftware.protectit.item.ProtectItItems;
import com.someguyssoftware.protectit.network.ProtectItNetworking;
import com.someguyssoftware.protectit.network.RegistryMutatorMessageToClient;
import com.someguyssoftware.protectit.network.RegistryWhitelistMutatorMessageToClient;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.registry.PlayerData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Nov 16, 2021
 *
 */
public class ClaimLecternBlockEntity extends BlockEntity implements Clearable, MenuProvider {
	private static final String BOOK_TAG = "book";
	private static final String PAGE_TAG = "page";
	private static final String CLAIM_COORDS_TAG = "claimCoords";

	private ItemStack book = ItemStack.EMPTY;
	@Deprecated
	private int page;
	private ICoords claimCoords;

	private final ContainerData dataAccess = new ContainerData() {
		public int get(int index) {
			return index == 0 ? ClaimLecternBlockEntity.this.page : 0;
		}

		public void set(int index, int value) {
			if (index == 0) {
				ClaimLecternBlockEntity.this.setPage(value);
			}
		}

		public int getCount() {
			return 1;
		}
	};

	/**
	 * 
	 */
	public ClaimLecternBlockEntity(BlockPos pos, BlockState state) {
		super(ProtectItBlockEntities.CLAIM_LECTERN_TYPE.get(), pos, state);
	}

	@Override
	public Component getDisplayName() {
		return new TranslatableComponent("container.claim_lectern");
	}

	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		ClaimLecternMenu container = new ClaimLecternMenu(id, this.bookAccess, this.dataAccess);
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
	public void load(CompoundTag nbt) {
		super.load(nbt);

		if (nbt.contains(CLAIM_COORDS_TAG)) {
			setClaimCoords(Coords.EMPTY.load(nbt.getCompound(CLAIM_COORDS_TAG)));
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
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);

		if (getClaimCoords() != null) {
			CompoundTag coordsNbt = new CompoundTag();
			getClaimCoords().save(coordsNbt);
			nbt.put(CLAIM_COORDS_TAG, coordsNbt);
		}

		if (!this.getBook().isEmpty()) {
			nbt.put(BOOK_TAG, this.getBook().save(new CompoundTag()));
			nbt.putInt(PAGE_TAG, this.page);
		}
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
			if (bookStack != null && bookStack.getItem() == ProtectItItems.CLAIM_BOOK.get()) {
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

				// update registry on client side
				List<Claim> claims = Lists.newArrayList();
				claims.add(registryClaim);
				if(((ServerLevel)this.level).getServer().isDedicatedServer()) {
					// send message to add protection on all clients
					RegistryWhitelistMutatorMessageToClient message = new RegistryWhitelistMutatorMessageToClient.Builder(
							RegistryMutatorMessageToClient.BLOCK_TYPE, 
							RegistryWhitelistMutatorMessageToClient.WHITELIST_REPLACE_ACTION, 
							claims).build();
					ProtectIt.LOGGER.info("sending message to sync client side -> {}", message);
					ProtectItNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
				}
				
				// update this claim - mark as dirty
				ProtectItSavedData savedData = ProtectItSavedData.get(getLevel());
				if (savedData != null) {
					savedData.setDirty();
				}

				// update bookStack				
				ClaimBook.savePlayerData(bookStack, bookPlayerDataList);
				ClaimBook.saveClaim(bookStack, registryClaim);
			}
			else {
				registryClaim.getWhitelist().clear();
			}
		}
		this.setBook(bookStack, (Player) null);
	}

	public void setBook(ItemStack itemStack, @Nullable Player player) {
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
	private final Container bookAccess = new Container() {
		public int getContainerSize() {
			return 1;
		}

		public boolean isEmpty() {
			return ClaimLecternBlockEntity.this.book.isEmpty();
		}

		public ItemStack getItem(int p_70301_1_) {
			return p_70301_1_ == 0 ? ClaimLecternBlockEntity.this.book : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int slot, int size) {
			if (slot == 0) {
				ItemStack itemstack = ClaimLecternBlockEntity.this.book.split(size);
				if (ClaimLecternBlockEntity.this.book.isEmpty()) {
					ClaimLecternBlockEntity.this.onBookItemRemove();
				}
				return itemstack;
			} else {
				return ItemStack.EMPTY;
			}
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			if (slot == 0) {
				ItemStack itemstack = ClaimLecternBlockEntity.this.book;
				ClaimLecternBlockEntity.this.book = ItemStack.EMPTY;
				ClaimLecternBlockEntity.this.onBookItemRemove();
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
			ClaimLecternBlockEntity.this.setChanged();
		}

		public boolean stillValid(Player player) {
			if (ClaimLecternBlockEntity.this.level
					.getBlockEntity(ClaimLecternBlockEntity.this.worldPosition) != ClaimLecternBlockEntity.this) {
				return false;
			} else {
				return player.distanceToSqr((double) ClaimLecternBlockEntity.this.worldPosition.getX() + 0.5D,
						(double) ClaimLecternBlockEntity.this.worldPosition.getY() + 0.5D,
						(double) ClaimLecternBlockEntity.this.worldPosition.getZ() + 0.5D) > 64.0D ? false
								: ClaimLecternBlockEntity.this.hasBook();
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
