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

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.config.Config;
import com.someguyssoftware.protectit.network.ProtectItNetworking;
import com.someguyssoftware.protectit.network.RegistryMutatorMessageToClient;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.registry.PlayerData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.tileentity.ClaimLeverTileEntity;
import com.someguyssoftware.protectit.tileentity.ClaimBlockEntity;
import com.someguyssoftware.protectit.tileentity.RemoveClaimTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.Player;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Dec 2, 2021
 *
 */
public class RemoveClaimBlock extends ClaimBlock {

	public RemoveClaimBlock(String modID, String name, Block.Properties properties) {
		super(modID, name, properties);
	}

	public RemoveClaimBlock(String modID, String name, ICoords claimSize, Block.Properties properties) {
		super(modID, name, claimSize, properties);
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		RemoveClaimTileEntity tileEntity = null;
		try {
			tileEntity = new RemoveClaimTileEntity();
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		ProtectIt.LOGGER.info("create tileEntity -> {}}", tileEntity);
		return tileEntity;
	}

	/**
	 * 
	 */
	@Override
	public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		TileEntity tileEntity = worldIn.getBlockEntity(pos);
		if (tileEntity instanceof RemoveClaimTileEntity) {
			// get the claim for this position
			ProtectIt.LOGGER.debug("current protections -> {}", ProtectionRegistries.block().toStringList());
			ProtectIt.LOGGER.debug("search for claim @ -> {}", new Coords(pos).toShortString());
			List<Box> list = ProtectionRegistries.block().getProtections(new Coords(pos), new Coords(pos).add(1, 1, 1), false, false);
			if (!list.isEmpty()) {				
				Claim claim = ProtectionRegistries.block().getClaimByCoords(list.get(0).getMinCoords());
				ProtectIt.LOGGER.debug("found protection -> {}", claim);
				if (claim != null) {
					ProtectIt.LOGGER.debug("found claim -> {}", claim);
					((RemoveClaimTileEntity)tileEntity).setClaimCoords(claim.getBox().getMinCoords());
				}
			}
		}
	}

	/**
	 * 
	 */
	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, Player player,
			Hand handIn, BlockRayTraceResult hit) {

		// exit if on the client
		if (WorldInfo.isClientSide(world)) {
			return ActionResultType.SUCCESS;
		}
		ProtectIt.LOGGER.debug("in claim block use() on server... is dedicated -> {}", player.getServer().isDedicatedServer());

		// get the tile entity
		TileEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof RemoveClaimTileEntity) {
			// get this claim
			// prevent use if not the owner
			Claim claim = ProtectionRegistries.block().getClaimByCoords(((RemoveClaimTileEntity)tileEntity).getClaimCoords());
			if (claim == null || !player.getStringUUID().equalsIgnoreCase(claim.getOwner().getUuid())) {
				player.sendMessage(new TranslationTextComponent("message.protectit.block_region_not_protected_or_owner"), player.getUUID());
				return ActionResultType.SUCCESS;
			}

			// remove claim
			ProtectionRegistries.block().removeProtection(claim.getBox().getMinCoords());

			ProtectIt.LOGGER.info("should've removed -> {} {}", claim, player.getStringUUID());

			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			// mark data as dirty
			if (savedData != null) {
				savedData.setDirty();
			}

			if(((ServerWorld)world).getServer().isDedicatedServer()) {
				// send message to remove protection on all clients
				RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
						RegistryMutatorMessageToClient.BLOCK_TYPE, 
						RegistryMutatorMessageToClient.REMOVE_ACTION, 
						player.getStringUUID()).with($ -> {
							$.coords1 = claim.getBox().getMinCoords();
							$.coords2 = claim.getBox().getMaxCoords();
							$.playerName = player.getName().getString();
						}).build();
				ProtectIt.LOGGER.info("sending message to sync client side ");
				ProtectItNetworking.simpleChannel.send(PacketDistributor.ALL.noArg(), message);
			}

			// remove Remove Claim block
			world.removeBlock(pos, false);

			// TODO give player Claim Block - need the size?
			ItemStack claimStack = new ItemStack(ProtectItBlocks.SMALL_CLAIM);
			if (!player.inventory.add(claimStack)) {
				player.drop(claimStack, false);
			}

			// send message to player
			player.sendMessage(new TranslationTextComponent("message.protectit.claim_successfully_removed"), player.getUUID());

		}

		return ActionResultType.SUCCESS;
	}
}
