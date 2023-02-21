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

import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.entity.RemoveClaimBlockEntity;
import com.someguyssoftware.protectit.network.ProtectItNetworking;
import com.someguyssoftware.protectit.network.RegistryMutatorMessageToClient;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.property.Property;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.util.LangUtil;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;


/**
 * 
 * @author Mark Gottschling on Dec 2, 2021
 *
 */
public class RemoveClaimBlock extends ClaimBlock {

	public RemoveClaimBlock(Block.Properties properties) {
		super(properties);
	}

	public RemoveClaimBlock(ICoords claimSize, Block.Properties properties) {
		super(claimSize, properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		RemoveClaimBlockEntity blockEntity = null;
		try {
			blockEntity = new RemoveClaimBlockEntity(pos, state);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		ProtectIt.LOGGER.debug("createNewTileEntity | blockEntity -> {}}", blockEntity);
		return blockEntity;
	}

	/**
	 * 
	 */
	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		if (tileEntity instanceof RemoveClaimBlockEntity) {
			// get the claim for this position
			ProtectIt.LOGGER.debug("current protections -> {}", ProtectionRegistries.block().toStringList());
			ProtectIt.LOGGER.debug("search for claim @ -> {}", new Coords(pos).toShortString());
			List<Box> list = ProtectionRegistries.block().getProtections(new Coords(pos), new Coords(pos).add(1, 1, 1), false, false);
			if (!list.isEmpty()) {				
				Property claim = ProtectionRegistries.block().getClaimByCoords(list.get(0).getMinCoords());
				ProtectIt.LOGGER.debug("found protection -> {}", claim);
				if (claim != null) {
					ProtectIt.LOGGER.debug("found claim -> {}", claim);
					((RemoveClaimBlockEntity)tileEntity).setClaimCoords(claim.getBox().getMinCoords());
				}
			}
		}
	}

	/**
	 * 
	 */
	
	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player,
			InteractionHand handIn, BlockHitResult hit) {

		// exit if on the client
		if (WorldInfo.isClientSide(world)) {
			return InteractionResult.SUCCESS;
		}
		ProtectIt.LOGGER.debug("in claim block use() on server... is dedicated -> {}", player.getServer().isDedicatedServer());

		// get the tile entity
		BlockEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof RemoveClaimBlockEntity) {
			// get this claim
			// prevent use if not the owner
			Property claim = ProtectionRegistries.block().getClaimByCoords(((RemoveClaimBlockEntity)tileEntity).getClaimCoords());
			if (claim == null || !player.getStringUUID().equalsIgnoreCase(claim.getOwner().getUuid())) {
				player.sendMessage(new TranslatableComponent(LangUtil.message("block_region.not_protected_or_owner")), player.getUUID());
				return InteractionResult.SUCCESS;
			}

			// remove claim
			ProtectionRegistries.block().removeProtection(claim.getBox().getMinCoords());

			ProtectIt.LOGGER.debug("should've removed -> {} {}", claim, player.getStringUUID());

			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			// mark data as dirty
			if (savedData != null) {
				savedData.setDirty();
			}

			if(((ServerLevel)world).getServer().isDedicatedServer()) {
				// send message to remove protection on all clients
				RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
						RegistryMutatorMessageToClient.BLOCK_TYPE, 
						RegistryMutatorMessageToClient.REMOVE_ACTION, 
						player.getStringUUID()).with($ -> {
							$.coords1 = claim.getBox().getMinCoords();
							$.coords2 = claim.getBox().getMaxCoords();
							$.playerName = player.getName().getString();
						}).build();
				ProtectIt.LOGGER.debug("sending message to sync client side ");
				ProtectItNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}

			// remove Remove Claim block
			world.removeBlock(pos, false);

			// TODO give player Claim Block - need the size?
			ItemStack claimStack = new ItemStack(ProtectItBlocks.SMALL_CLAIM.get());
			if (!player.getInventory().add(claimStack)) {
				player.drop(claimStack, false);
			}

			// send message to player
			player.sendMessage(new TranslatableComponent(LangUtil.message("claim_successfully_removed")), player.getUUID());

		}

		return InteractionResult.SUCCESS;
	}
}
