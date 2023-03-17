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
package mod.gottsch.forge.protectit.core.block.entity;

import java.util.ArrayList;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 
 * @author Mark Gottschling Mar 7, 2023
 *
 */
public class CustomClaimBlockEntity extends ClaimBlockEntity {

	public static final String CLAIM_SIZE_KEY = "propertySize";
	
	private ICoords claimSize;
	
	public CustomClaimBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CUSTOM_CLAIM_TYPE.get(), pos, state);
		setOverlaps(new ArrayList<>());
	}
	
	public CustomClaimBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("claimSize", claimSize.save(new CompoundTag()));		
	}
	
	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.contains("claimSize")) {
			ICoords coords = Coords.EMPTY.load(tag.getCompound("claimSize"));
			this.claimSize = coords;
		}
	}
	
	public ICoords getClaimSize() {
		return claimSize;
	}
	
	public void setClaimSize(ICoords size) {
		this.claimSize = size;
	}
}
