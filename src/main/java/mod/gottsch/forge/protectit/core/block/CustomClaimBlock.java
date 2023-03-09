/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.ClaimBlockEntity;
import mod.gottsch.forge.protectit.core.block.entity.CustomClaimBlockEntity;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 
 * @author Mark Gottschling Mar 7, 2023
 *
 */
public class CustomClaimBlock extends ClaimBlock {

	public CustomClaimBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		ClaimBlockEntity blockEntity = null;
		try {
			blockEntity = new CustomClaimBlockEntity(pos, state);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		ProtectIt.LOGGER.debug("createNewTileEntity | blockEntity -> {}}", blockEntity);
		return blockEntity;
	}
	
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		BlockEntity blockEntity = level.getBlockEntity(pos);

		// gather the number of claims the player has
		List<Property> claims = ProtectionRegistries.block().getProtections(placer.getStringUUID());		
		if (claims.size() >= Config.GENERAL.propertiesPerPlayer.get()) {
			placer.sendSystemMessage(Component.translatable("message.protectit.max_claims_met"));
			return;
		}
		
		if (blockEntity instanceof CustomClaimBlockEntity) {
			((CustomClaimBlockEntity) blockEntity).setOwnerUuid(placer.getStringUUID());
			// update the block entity with values stored in the itemStack
			ICoords claimSize = Coords.EMPTY.load(stack.getOrCreateTag().getCompound(CustomClaimBlockEntity.CLAIM_SIZE_KEY));
			((CustomClaimBlockEntity) blockEntity).setClaimSize(claimSize);
			
			// save any overlaps to the BlockEntity
			Box box = getBox(level, blockEntity.getBlockPos());
			List<Box> overlaps = ProtectionRegistries.block().getProtections(box.getMinCoords(), box.getMaxCoords(), false, false);
			ProtectIt.LOGGER.debug("num of overlaps @ {} <--> {} -> {}", box.getMinCoords().toShortString(), box.getMaxCoords().toShortString(), overlaps.size());
			if (!overlaps.isEmpty()) {
				((ClaimBlockEntity)blockEntity).getOverlaps().addAll(overlaps);
			}
		}
	}
	
	/**
	 * Might be able to undo this????
	 * @param pos
	 * @return
	 */
	@Override
	public Box getBox(Level level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		ICoords claimSize = ((CustomClaimBlockEntity)blockEntity).getClaimSizeKey();
		// TODO need to get the BE to get the block size - how ??
		BlockPos p1 = pos.offset(0, -(claimSize.getY()/2), 0);
		BlockPos p2 = p1.offset(claimSize.getX(), claimSize.getY(), claimSize.getZ());		
		return getBox(new Coords(p1), new Coords(p2), claimSize);
	}
	
	@Override
	public ICoords getClaimSize() {
		return Coords.EMPTY;
	}
	
	@Override
	public void setClaimSize(ICoords size) {
	}
}
