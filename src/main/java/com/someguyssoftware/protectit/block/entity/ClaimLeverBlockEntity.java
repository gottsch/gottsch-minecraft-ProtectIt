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

import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.gottschcore.tileentity.AbstractModTileEntity;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.protectit.claim.Claim;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * 
 * @author Mark Gottschling on Nov 8, 2021
 *
 */
public class ClaimLeverBlockEntity extends AbstractModTileEntity {
	private static final String CLAIM_COORDS_TAG = "claimCoords";

	private ICoords claimCoords;
	
	public ClaimLeverBlockEntity() {
		this(ProtectItBlockEntities.CLAIM_LEVER_TYPE);
	}

	public ClaimLeverBlockEntity(TileEntityType<?> type) {
		super(type);
	}
	
	/**
	 * 
	 */
	@Override
	public CompoundNBT save(CompoundNBT nbt) {
		super.save(nbt);
		if (getClaimCoords() != null) {
			CompoundNBT coordsNbt = new CompoundNBT();
			getClaimCoords().save(coordsNbt);
			nbt.put(CLAIM_COORDS_TAG, coordsNbt);
		}
		return nbt;
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
	}
	
	/*
	 * Get the render bounding box. Typical block is 1x1x1.
	 */
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		// always render regardless if TE is in FOV.
		return INFINITE_EXTENT_AABB;
	}
	
	// TODO shouldn't need these are they are already in AbstractModTileEntity
	/**
	 * collect data to send to client
	 */
	@Override
	public CompoundNBT getUpdateTag() {
		CompoundNBT nbt = new CompoundNBT(); //super.getUpdateTag();
		nbt = save(nbt);
		return nbt;
	}
	
	/*
	 * handle on client
	 */
	@Override
	public void handleUpdateTag(BlockState state, CompoundNBT tag) {
		//super.handleUpdateTag(state, tag);
		load(state, tag);
	}

	public ICoords getClaimCoords() {
		return claimCoords;
	}

	public void setClaimCoords(ICoords claimCoords) {
		this.claimCoords = claimCoords;
	}
}
