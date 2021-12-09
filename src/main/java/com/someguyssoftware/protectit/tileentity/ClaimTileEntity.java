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
import com.someguyssoftware.protectit.block.ClaimBlock;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ClaimTileEntity extends AbstractClaimTileEntity implements ITickableTileEntity {
	private static final int TICKS_PER_SECOND = 20;
	private static final int FIVE_SECONDS = 5 * TICKS_PER_SECOND;
	
	/**
	 * 
	 */
	public ClaimTileEntity() {
		this(ProtectItTileEntities.CLAIM_TILE_ENTITY_TYPE);
	}
	
	/**
	 * 
	 * @param type
	 */
	public ClaimTileEntity(TileEntityType<?> type) {
		super(type);
		setOverlaps(new ArrayList<>());
	}

	@Override
	public void tick() {
		// fetch overlaps from protection registry every 5 seconds
		if (getLevel().getGameTime() % FIVE_SECONDS == 0) {
			ClaimBlock block = (ClaimBlock)getLevel().getBlockState(getBlockPos()).getBlock();
			Box box = block.getBox(getBlockPos());
			List<Box> overlaps = ProtectionRegistries.block().getProtections(box.getMinCoords(), box.getMaxCoords(), false, false);
			getOverlaps().clear();
			if (!overlaps.isEmpty()) {
				getOverlaps().addAll(overlaps);
			}
		}
	}

	/**
	 * 
	 */
	@Override
	public CompoundNBT save(CompoundNBT nbt) {
		super.save(nbt);
		//		ProtectIt.LOGGER.debug("saving overlap box -> {}", this);

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
		CompoundNBT nbt = new CompoundNBT();
		save(nbt);
		return nbt;
	}
	
	/*
	 * handle on client
	 */
	@Override
	public void handleUpdateTag(BlockState state, CompoundNBT tag) {
		load(state, tag);
	}
}
