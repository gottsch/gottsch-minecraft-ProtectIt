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
package com.someguyssoftware.protectit.block.entity;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;


/**
 * 
 * @author Mark Gottschling on Nov 8, 2021
 *
 */
public class ClaimLeverBlockEntity extends BlockEntity {
	private static final String CLAIM_COORDS_TAG = "claimCoords";

	private ICoords claimCoords;
	
	public ClaimLeverBlockEntity(BlockPos pos, BlockState state) {
		this(ProtectItBlockEntities.CLAIM_LEVER_TYPE.get(), pos, state);
	}

	public ClaimLeverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
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
	}
	
	/*
	 * Get the render bounding box. Typical block is 1x1x1.
	 */
	@Override
	public AABB getRenderBoundingBox() {
		// always render regardless if TE is in FOV.
		return INFINITE_EXTENT_AABB;
	}
	
	// TODO shouldn't need these are they are already in AbstractModTileEntity
	/**
	 * collect data to send to client
	 */
	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = new CompoundTag(); //super.getUpdateTag();
		saveAdditional(nbt);
		return nbt;
	}
	
	/*
	 * handle on client
	 */
	@Override
	public void handleUpdateTag(CompoundTag tag) {
		//super.handleUpdateTag(state, tag);
		load(tag);
	}

	public ICoords getClaimCoords() {
		return claimCoords;
	}

	public void setClaimCoords(ICoords claimCoords) {
		this.claimCoords = claimCoords;
	}
}
