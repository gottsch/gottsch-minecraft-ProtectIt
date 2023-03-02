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
package mod.gottsch.forge.protectit.core.block.entity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 
 * @author Mark Gottschling on Oct 30, 2021
 *
 */
public abstract class AbstractClaimBlockEntity extends BlockEntity implements IClaimBlockEntity {
	protected static final String OWNER_UUID = "owner_uuid";
	protected static final String OVERLAPS = "overlaps";	
	
	private String ownerUuid;
	private List<Box> overlaps;
	
	/**
	 * 
	 * @param type
	 */
	public AbstractClaimBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	abstract public void tick();
	
	/**
	 * Sync client and server states
	 */
	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		saveAdditional(tag);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		if (tag != null) {
			load(tag);
		}
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		CompoundTag tag = pkt.getTag();
		handleUpdateTag(tag);
	}
	
	@Override
	public String getOwnerUuid() {
		return ownerUuid;
	}

	@Override
	public void setOwnerUuid(String ownerUuid) {
		this.ownerUuid = ownerUuid;
	}

	@Override
	public List<Box> getOverlaps() {
		if (overlaps == null) {
			overlaps = new ArrayList<>();
		}
		return overlaps;
	}

	@Override
	public void setOverlaps(List<Box> overlaps) {
		this.overlaps = overlaps;
	}
}
