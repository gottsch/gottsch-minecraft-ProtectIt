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
package com.someguyssoftware.protectit.block;

import java.util.List;

import javax.annotation.Nullable;

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.entity.ClaimLecternBlockEntity;
import com.someguyssoftware.protectit.claim.Property;
import com.someguyssoftware.protectit.item.ProtectItItems;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;


/**
 * 
 * @author Mark Gottschling on Nov 16, 2021
 *
 */
// REMOVE until a suitable GUI is added
@Deprecated
public class ClaimLectern extends LecternBlock {

	/**
	 * 
	 * @param modID
	 * @param name
	 * @param material
	 */
	public ClaimLectern(Block.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		ClaimLecternBlockEntity blockEntity = null;
		try {
			blockEntity = new ClaimLecternBlockEntity(pos, state);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		ProtectIt.LOGGER.info("createNewTileEntity | blockEntity -> {}}", blockEntity);
		return blockEntity;
	}

	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		ProtectIt.LOGGER.info("lectern TE -> {}", tileEntity.getClass().getSimpleName());
		if (tileEntity instanceof ClaimLecternBlockEntity) {
			// get the claim for this position
			List<Box> list = ProtectionRegistries.block().getProtections(new Coords(pos), new Coords(pos).add(1, 1, 1), false, false);
			ProtectIt.LOGGER.info("found claim list -> {}", list);
			if (!list.isEmpty()) {
				Property claim = ProtectionRegistries.block().getClaimByCoords(list.get(0).getMinCoords());
				ProtectIt.LOGGER.info("found claim -> {}", claim);
				((ClaimLecternBlockEntity)tileEntity).setClaimCoords(claim.getBox().getMinCoords());
			}
		}
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTrace) {
		ProtectIt.LOGGER.info("using lectern block");
		if (state.getValue(HAS_BOOK)) {
			ProtectIt.LOGGER.info("has  book");
			if (!world.isClientSide) {
				ProtectIt.LOGGER.info("server side");
				this.openScreen(world, pos, player);
			}

			return InteractionResult.sidedSuccess(world.isClientSide);
		} else {
			ProtectIt.LOGGER.info("testing if book is in hand.");
			ItemStack itemStack = player.getItemInHand(hand);
			// if Book in hand, replace with claim book
			
//			TEMP REMOVE
//			if (!itemStack.isEmpty() && itemStack.getItem() == Items.BOOK) {
//				itemStack.shrink(1);
//				player.setItemInHand(hand, new ItemStack(ProtectItItems.CLAIM_BOOK.get()));
//			}
//			else if (!itemStack.isEmpty() && itemStack.getItem() != ProtectItItems.CLAIM_BOOK.get()) {
//				return InteractionResult.CONSUME; // TODO this should consume the book
//			}
			
//			return !itemStack.isEmpty() && itemStack.getItem() != ProtectItItems.CLAIM_BOOK ? ActionResultType.CONSUME : ActionResultType.PASS;
			return InteractionResult.PASS;
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
	public static boolean tryPlaceBook(@Nullable Player player, Level world, BlockPos pos, BlockState state, ItemStack itemStack) {
		ProtectIt.LOGGER.info("trying to place book.");
		if (!state.getValue(HAS_BOOK)) {
			ProtectIt.LOGGER.info("doesn't have a book yet.");
			if (!world.isClientSide) {
				ProtectIt.LOGGER.info("on server side.");
				
				// test if the player is the owner
				BlockEntity tileEntity = world.getBlockEntity(pos);
				
				if (tileEntity instanceof ClaimLecternBlockEntity) {
					ProtectIt.LOGGER.info("it is a claim lectern TE");
					ClaimLecternBlockEntity lecternTileEntity = (ClaimLecternBlockEntity)tileEntity;
					ProtectIt.LOGGER.info("claim coords -> {}", lecternTileEntity.getClaimCoords());
					Property lecternClaim = ProtectionRegistries.block().getClaimByCoords(lecternTileEntity.getClaimCoords());
					ProtectIt.LOGGER.info("lectern claim -> {}", lecternClaim);
					if (lecternClaim == null || !lecternClaim.getOwner().getUuid().equalsIgnoreCase(player.getStringUUID())) {
						ProtectIt.LOGGER.info("claim is null or not owner");
						// TODO display a message to the user
						return false;
					}
					ProtectIt.LOGGER.info("claim owner -> {}", lecternClaim.getOwner().getUuid() );
				}
				else {
					return false;
				}
				placeBook(world, pos, state, itemStack);
			}
			return true;
		} else {
			ProtectIt.LOGGER.info("tryPlaceBook -> has book!");
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
	private static void placeBook(Level world, BlockPos pos, BlockState state, ItemStack itemStack) {
		ProtectIt.LOGGER.info("placing book");
		BlockEntity tileEntity = world.getBlockEntity(pos);
		ProtectIt.LOGGER.info("lectern TE -> {}", tileEntity.getClass().getSimpleName());
		if (tileEntity instanceof ClaimLecternBlockEntity) {
			ClaimLecternBlockEntity lecternTileEntity = (ClaimLecternBlockEntity)tileEntity;
			ProtectIt.LOGGER.info("setting the book.");
			lecternTileEntity.setBook(itemStack.split(1));
			// update the block state
			resetBookState(world, pos, state, true);
			world.playSound((Player)null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		else {
			ProtectIt.LOGGER.info("not the right TE.");
		}
	}

	/**
	 * Only update the HAS_BOOK.
	 * @param world
	 * @param pos
	 * @param state
	 * @param hasBook
	 */
	public static void resetBookState(Level world, BlockPos pos, BlockState state, boolean hasBook) {
		world.setBlock(pos, state.setValue(HAS_BOOK, Boolean.valueOf(hasBook)), 3);
	}

	@Override
	@Nullable
	public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
		if (!state.getValue(HAS_BOOK)) {
			ProtectIt.LOGGER.info("doesn't have book");
		}
		return !state.getValue(HAS_BOOK) ? null : super.getMenuProvider(state, world, pos);
	}

	private void openScreen(Level world, BlockPos pos, Player player) {
		ProtectIt.LOGGER.info("opening lectern screen...");
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof ClaimLecternBlockEntity) {
			ProtectIt.LOGGER.info("it is a BE");
			player.openMenu((ClaimLecternBlockEntity)blockEntity);
//			NetworkHooks.openGui((ServerPlayer) player, (ClaimLecternBlockEntity)blockEntity, pos);
			ProtectIt.LOGGER.info("after opening menu");
		}
	}
}
