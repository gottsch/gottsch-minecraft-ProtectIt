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
package mod.gottsch.forge.protectit.core.block.entity;

import java.util.UUID;

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
public class PropertyLeverBlockEntity extends BlockEntity {
	private static final int TICKS_PER_SECOND = 20;
	protected static final int FIVE_SECONDS = 5 * TICKS_PER_SECOND;
	
	private static final String PROPERTY_COORDS_TAG = "propertyCoords";
	private static final String PROPERTY_UUID_TAG = "propertyUuid";

	private ICoords propertyCoords;
	private UUID propertyUuid;
	
	public PropertyLeverBlockEntity(BlockPos pos, BlockState state) {
		this(ProtectItBlockEntities.PROPERTY_LEVER_TYPE.get(), pos, state);
	}

	public PropertyLeverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * 
	 */
	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		if (getPropertyCoords() != null) {
			CompoundTag coordsNbt = new CompoundTag();
			getPropertyCoords().save(coordsNbt);
			nbt.put(PROPERTY_COORDS_TAG, coordsNbt);
		}
		if (getPropertyUuid() != null) {
			nbt.putUUID(PROPERTY_UUID_TAG, getPropertyUuid());
		}
	}
	
	/**
	 * 
	 */
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		if (nbt.contains(PROPERTY_COORDS_TAG)) {
			setPropertyCoords(Coords.EMPTY.load(nbt.getCompound(PROPERTY_COORDS_TAG)));
		}
		if (nbt.contains(PROPERTY_UUID_TAG)) {
			setPropertyUuid(nbt.getUUID(PROPERTY_UUID_TAG));
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

	public ICoords getPropertyCoords() {
		return propertyCoords;
	}

	public void setPropertyCoords(ICoords claimCoords) {
		this.propertyCoords = claimCoords;
	}

	public UUID getPropertyUuid() {
		return propertyUuid;
	}

	public void setPropertyUuid(UUID propertyUuid) {
		this.propertyUuid = propertyUuid;
	}
}
