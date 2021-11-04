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

import org.apache.commons.lang3.StringUtils;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ClaimTileEntity extends AbstractClaimTileEntity {
		
	/**
	 * 
	 */
	public ClaimTileEntity() {
		super(ProtectItTileEntities.CLAIM_TILE_ENTITY_TYPE);
		setOverlaps(new ArrayList<>());
	}

	/**
	 * 
	 */
	@Override
	public CompoundNBT save(CompoundNBT nbt) {
		super.save(nbt);
		ProtectIt.LOGGER.debug("saving overlap box -> {}", this);
		
		if (StringUtils.isNotBlank(getOwnerUuid())) {
			nbt.putString(OWNER_UUID, getOwnerUuid());
		}
		
		ListNBT list = new ListNBT();
		getOverlaps().forEach(box -> {
			CompoundNBT element = new CompoundNBT();
			box.save(element);
			list.add(element);
		});
		nbt.put(OVERLAPS, list);
		return nbt;
	}
	
	/**
	 * 
	 */
	@Override
	public void load(BlockState state, CompoundNBT nbt) {
		super.load(state, nbt);
		
		if (nbt.contains(OWNER_UUID)) {
			setOwnerUuid(nbt.getString(OWNER_UUID));
		}
		
		getOverlaps().clear();
		if (nbt.contains(OVERLAPS)) {
			ListNBT list = nbt.getList(OVERLAPS, 10);
			list.forEach(element -> {
				Box box = Box.load((CompoundNBT)element);
				if (box != null) {
					getOverlaps().add(box);
				}
			});
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
	
	/**
	 * collect data to send to client
	 */
	@Override
	public CompoundNBT getUpdateTag() {
		CompoundNBT nbt = super.getUpdateTag();
		nbt = save(nbt);
		return nbt;
	}
	
	/*
	 * handle on client
	 */
	@Override
	public void handleUpdateTag(BlockState state, CompoundNBT tag) {
		super.handleUpdateTag(state, tag);
		load(state, tag);
	}
}
