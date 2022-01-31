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

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ClaimBlock;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;


/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ClaimBlockEntity extends AbstractClaimBlockEntity {
	private static final int TICKS_PER_SECOND = 20;
	private static final int FIVE_SECONDS = 5 * TICKS_PER_SECOND;

	/**
	 * 
	 */
	public ClaimBlockEntity(BlockPos pos, BlockState state) {
		this(ProtectItTileEntities.CLAIM_TILE_ENTITY_TYPE, pos, state);
	}

	/**
	 * 
	 * @param type
	 */
	public ClaimBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setOverlaps(new ArrayList<>());
	}

	/**
	 * 
	 * @param level
	 * @param pos
	 * @param state
	 * @param blockEntity
	 */
	public static void tick(Level level, BlockPos pos, BlockState state, ClaimBlockEntity blockEntity) {
		// fetch overlaps from protection registry every 5 seconds
		if (level.getGameTime() % FIVE_SECONDS == 0) {
			ClaimBlock block = (ClaimBlock)level.getBlockState(pos).getBlock();
			Box box = block.getBox(pos);
			List<Box> overlaps = ProtectionRegistries.block().getProtections(box.getMinCoords(), box.getMaxCoords(), false, false);
			blockEntity.getOverlaps().clear();
			if (!overlaps.isEmpty()) {
				blockEntity.getOverlaps().addAll(overlaps);
			}
		}
	}

	/**
	 * 
	 */
	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		//		ProtectIt.LOGGER.debug("saving overlap box -> {}", this);

		if (StringUtils.isNotBlank(getOwnerUuid())) {
			nbt.putString(OWNER_UUID, getOwnerUuid());
		}

		ListTag list = new ListTag();
		getOverlaps().forEach(box -> {
			CompoundTag element = new CompoundTag();
			box.save(element);
			list.add(element);
		});
		nbt.put(OVERLAPS, list);
	}

	/**
	 * 
	 */
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);

		if (nbt.contains(OWNER_UUID)) {
			setOwnerUuid(nbt.getString(OWNER_UUID));
		}

		getOverlaps().clear();
		if (nbt.contains(OVERLAPS)) {
			ListTag list = nbt.getList(OVERLAPS, 10);
			list.forEach(element -> {
				Box box = Box.load((CompoundTag)element);
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
	public AABB getRenderBoundingBox() {
		// always render regardless if TE is in FOV.
		return INFINITE_EXTENT_AABB;
	}

	/**
	 * TODO review this ....
	 * collect data to send to client
	 */
//	@Override
//	public CompoundTag getUpdateTag() {
//		CompoundTag nbt = new CompoundTag();
//		save(nbt);
//		return nbt;
//	}

}
