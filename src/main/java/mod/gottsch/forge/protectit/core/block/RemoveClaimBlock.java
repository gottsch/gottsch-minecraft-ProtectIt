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
package mod.gottsch.forge.protectit.core.block;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.CustomClaimBlockEntity;
import mod.gottsch.forge.protectit.core.block.entity.RemoveClaimBlockEntity;
import mod.gottsch.forge.protectit.core.network.ModNetworking;
import mod.gottsch.forge.protectit.core.network.RegistryMutatorMessageToClient;
import mod.gottsch.forge.protectit.core.persistence.ProtectItSavedData;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.property.PropertySizes;
import mod.gottsch.forge.protectit.core.property.PropertyUtil;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;


/**
 * 
 * @author Mark Gottschling on Dec 2, 2021
 *
 */
public class RemoveClaimBlock extends ClaimBlock implements EntityBlock {

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

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!level.isClientSide()) {
			return (lvl, pos, blockState, t) -> {
				if (t instanceof RemoveClaimBlockEntity entity) { // test and cast
					entity.tickServer();
				}
			};
		}
		return null;
	}
	
	/**
	 * 
	 */
	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		BlockEntity blockEntity = worldIn.getBlockEntity(pos);
		if (blockEntity instanceof RemoveClaimBlockEntity) {
			// get the claim for this position
//			ProtectIt.LOGGER.debug("current protections -> {}", ProtectionRegistries.block().toStringList());
			ProtectIt.LOGGER.debug("search for property @ -> {}", new Coords(pos).toShortString());
			List<Box> list = ProtectionRegistries.property().getProtections(new Coords(pos), new Coords(pos), false, true);
			if (!list.isEmpty()) {				
//				List<Property> properties = ProtectionRegistries.block().getPropertyByCoords(list.get(0).getMinCoords());
				List<Property> properties = list.stream().flatMap(p -> ProtectionRegistries.property().getPropertyByCoords(p.getMinCoords()).stream()).toList();
				Optional<Property> property = PropertyUtil.getLeastSignificant(properties);

//				if (properties != null && !properties.isEmpty()) {
//					Optional<Property> property = PropertyUtil.getLeastSignificant(properties);
					if (property.isPresent()) {
						ProtectIt.LOGGER.debug("found property -> {}", property.get());
						((RemoveClaimBlockEntity)blockEntity).setPropertyCoords(property.get().getBox().getMinCoords());
						((RemoveClaimBlockEntity)blockEntity).setPropertyUuid(property.get().getUuid());
						((RemoveClaimBlockEntity)blockEntity).setPropertyBox(property.get().getBox());
					}
//				}
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
		ProtectIt.LOGGER.debug("in property block use() on server... is dedicated -> {}", player.getServer().isDedicatedServer());

		// get the tile entity
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof RemoveClaimBlockEntity) {
			// get this claim
			// prevent use if not the owner
			List<Property> properties = ProtectionRegistries.property().getPropertyByCoords(((RemoveClaimBlockEntity)blockEntity).getPropertyCoords());
			Optional<Property> property = PropertyUtil.getLeastSignificant(properties);
			if (property.isEmpty() || !player.getUUID().equals(property.get().getOwner().getUuid()) || !property.get().isDomain()) {
				player.sendSystemMessage(Component.translatable(LangUtil.message("block_region.not_protected_or_owner")));
				return InteractionResult.SUCCESS;
			}

			// remove property
			ProtectionRegistries.property().removeProperty(property.get()); //.get().getBox().getMinCoords());

			ProtectIt.LOGGER.debug("should've removed -> {} {}", property, player.getStringUUID());

			ProtectItSavedData savedData = ProtectItSavedData.get(world);
			// mark data as dirty
			if (savedData != null) {
				savedData.setDirty();
			}

			// update message and handler
			if(((ServerLevel)world).getServer().isDedicatedServer()) {
				// send message to remove protection on all clients
				RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
						RegistryMutatorMessageToClient.BLOCK_TYPE, 
						RegistryMutatorMessageToClient.REMOVE_ACTION, 
						player.getStringUUID()).with($ -> {
							$.coords1 = property.get().getBox().getMinCoords();
							$.coords2 = property.get().getBox().getMaxCoords();
							$.playerName = player.getName().getString();
						}).build();
				ProtectIt.LOGGER.debug("sending message to sync client side ");
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}

			// remove Remove Claim block
			world.removeBlock(pos, false);

			// calculate the size
			ICoords propertySize = property.get().getBox().getMaxCoords().delta(property.get().getBox().getMinCoords());
			ItemStack claimStack;
			if (propertySize.equals(PropertySizes.SMALL_CLAIM_SIZE)) {
				claimStack = new ItemStack(ModBlocks.SMALL_CLAIM.get());
			}
			else if (propertySize.equals(PropertySizes.MEDIUM_CLAIM_SIZE)) {
				claimStack = new ItemStack(ModBlocks.MEDIUM_CLAIM.get());
			}
			else if (propertySize.equals(PropertySizes.LARGE_CLAIM_SIZE)) {
				claimStack = new ItemStack(ModBlocks.LARGE_CLAIM.get());
			}
			else {
				claimStack = new ItemStack(ModBlocks.CUSTOM_CLAIM.get());
				CompoundTag tag = claimStack.getOrCreateTag();
				tag.put(CustomClaimBlockEntity.CLAIM_SIZE_KEY, propertySize.save(new CompoundTag()));
			}
			
			if (!player.getInventory().add(claimStack)) {
//				player.drop(claimStack, false);
				ItemEntity itemEntity = player.drop(claimStack, false);
				if (itemEntity != null) {
					itemEntity.setNoPickUpDelay();
					itemEntity.setOwner(player.getUUID());
				}
			}

			// send message to player
			player.sendSystemMessage(Component.translatable(LangUtil.message("claim_successfully_removed")));
		}

		return InteractionResult.SUCCESS;
	}
}
