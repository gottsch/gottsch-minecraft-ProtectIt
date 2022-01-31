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
package com.someguyssoftware.protectit.block;

import java.util.List;

import javax.annotation.Nullable;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.item.ProtectItItems;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.tileentity.ClaimLecternTileEntity;
import com.someguyssoftware.protectit.tileentity.ClaimLeverTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.Player;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

/**
 * 
 * @author Mark Gottschling on Nov 16, 2021
 *
 */
public class ClaimLectern extends LecternBlock {

	/**
	 * 
	 * @param modID
	 * @param name
	 * @param material
	 */
	public ClaimLectern(String modID, String name, Block.Properties properties) {
		super(properties);
		setBlockName(modID, name);
	}

	/**
	 * 
	 * @param modID
	 * @param name
	 */
	public void setBlockName(String modID, String name) {
		setRegistryName(modID, name);
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		ProtectIt.LOGGER.info("creating new ClaimLecternTileEntity...");
		ClaimLecternTileEntity tileEntity = null;
		//		TestTE tileEntity = null;
		try {
			tileEntity = new ClaimLecternTileEntity();
			//			tileEntity = new TestTE();
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred", e);
		}
		ProtectIt.LOGGER.info("createNewTileEntity | tileEntity -> {}}", tileEntity);
		return tileEntity;
	}

	/**
	 * 
	 * @param state
	 * @return
	 */
	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		TileEntity tileEntity = worldIn.getBlockEntity(pos);
		ProtectIt.LOGGER.debug("lectern TE -> {}", tileEntity.getClass().getSimpleName());
		if (tileEntity instanceof ClaimLecternTileEntity) {
			// get the claim for this position
			List<Box> list = ProtectionRegistries.block().getProtections(new Coords(pos), new Coords(pos).add(1, 1, 1), false, false);
			ProtectIt.LOGGER.debug("found claim list -> {}", list);
			if (!list.isEmpty()) {
				Claim claim = ProtectionRegistries.block().getClaimByCoords(list.get(0).getMinCoords());
				ProtectIt.LOGGER.debug("found claim -> {}", claim);
				((ClaimLecternTileEntity)tileEntity).setClaimCoords(claim.getBox().getMinCoords());
			}
		}
	}

	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, Player player, Hand hand, BlockRayTraceResult rayTrace) {
		ProtectIt.LOGGER.debug("using lectern block");
		if (state.getValue(HAS_BOOK)) {
			ProtectIt.LOGGER.debug("has  book");
			if (!world.isClientSide) {
				ProtectIt.LOGGER.debug("server side");
				this.openScreen(world, pos, player);
			}

			return ActionResultType.sidedSuccess(world.isClientSide);
		} else {
			ProtectIt.LOGGER.debug("testing if book is in hand.");
			ItemStack itemStack = player.getItemInHand(hand);
			// if Book in hand, replace with claim book
			if (!itemStack.isEmpty() && itemStack.getItem() == Items.BOOK) {
				itemStack.shrink(1);
				player.setItemInHand(hand, new ItemStack(ProtectItItems.CLAIM_BOOK));
			}
			else if (!itemStack.isEmpty() && itemStack.getItem() != ProtectItItems.CLAIM_BOOK) {
				return ActionResultType.CONSUME;
			}
//			return !itemStack.isEmpty() && itemStack.getItem() != ProtectItItems.CLAIM_BOOK ? ActionResultType.CONSUME : ActionResultType.PASS;
			return ActionResultType.PASS;
		}
	}

	/**
	 * 
	 * @param world
	 * @param pos
	 * @param state
	 * @param itemStack
	 * @return
	 */
	public static boolean tryPlaceBook(World world, BlockPos pos, BlockState state, Player player, ItemStack itemStack) {
		ProtectIt.LOGGER.debug("trying to place book.");
		if (!state.getValue(HAS_BOOK)) {
			ProtectIt.LOGGER.debug("doesn't have a book yet.");
			if (!world.isClientSide) {
				ProtectIt.LOGGER.debug("on server side.");
				
				// test if the player is the owner
				TileEntity tileEntity = world.getBlockEntity(pos);
				
				if (tileEntity instanceof ClaimLecternTileEntity) {
					ProtectIt.LOGGER.debug("it is a claim lectern TE");
					ClaimLecternTileEntity lecternTileEntity = (ClaimLecternTileEntity)tileEntity;
					ProtectIt.LOGGER.debug("claim coords -> {}", lecternTileEntity.getClaimCoords());
					Claim lecternClaim = ProtectionRegistries.block().getClaimByCoords(lecternTileEntity.getClaimCoords());
					ProtectIt.LOGGER.debug("lectern claim -> {}", lecternClaim);
					if (lecternClaim == null || !lecternClaim.getOwner().getUuid().equalsIgnoreCase(player.getStringUUID())) {
						ProtectIt.LOGGER.debug("claim is null or not owner");
						// TODO display a message to the user
						return false;
					}
					ProtectIt.LOGGER.debug("claim owner -> {}", lecternClaim.getOwner().getUuid() );
				}
				else {
					return false;
				}
				placeBook(world, pos, state, itemStack);
			}
			return true;
		} else {
			ProtectIt.LOGGER.debug("tryPlaceBook -> has book!");
			return false;
		}
	}

	/**
	 * 
	 * @param world
	 * @param pos
	 * @param state
	 * @param itemStack
	 */
	private static void placeBook(World world, BlockPos pos, BlockState state, ItemStack itemStack) {
		ProtectIt.LOGGER.debug("placing book");
		TileEntity tileEntity = world.getBlockEntity(pos);
		ProtectIt.LOGGER.debug("lectern TE -> {}", tileEntity.getClass().getSimpleName());
		if (tileEntity instanceof ClaimLecternTileEntity) {
			ClaimLecternTileEntity lecternTileEntity = (ClaimLecternTileEntity)tileEntity;
			ProtectIt.LOGGER.debug("settings the book.");
			lecternTileEntity.setBook(itemStack.split(1));
			// update the block state
			resetBookState(world, pos, state, true);
			world.playSound((Player)null, pos, SoundEvents.BOOK_PUT, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
		else {
			ProtectIt.LOGGER.debug("not the right TE.");
		}
	}

	/**
	 * Only update the HAS_BOOK.
	 * @param world
	 * @param pos
	 * @param state
	 * @param hasBook
	 */
	public static void resetBookState(World world, BlockPos pos, BlockState state, boolean hasBook) {
		world.setBlock(pos, state.setValue(HAS_BOOK, Boolean.valueOf(hasBook)), 3);
	}

	@Override
	@Nullable
	public INamedContainerProvider getMenuProvider(BlockState state, World world, BlockPos pos) {
		if (!state.getValue(HAS_BOOK)) {
			ProtectIt.LOGGER.debug("doesn't have book");
		}
		return !state.getValue(HAS_BOOK) ? null : super.getMenuProvider(state, world, pos);
	}

	private void openScreen(World world, BlockPos pos, Player player) {
		ProtectIt.LOGGER.debug("opening screen");
		TileEntity tileentity = world.getBlockEntity(pos);
		if (tileentity instanceof ClaimLecternTileEntity) {
			ProtectIt.LOGGER.debug("it is a CLTE");
			player.openMenu((ClaimLecternTileEntity)tileentity);
		}
	}
}
