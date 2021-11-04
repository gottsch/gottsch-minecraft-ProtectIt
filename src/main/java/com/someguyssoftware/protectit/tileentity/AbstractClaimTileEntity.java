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

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.tileentity.AbstractModTileEntity;

import net.minecraft.tileentity.TileEntityType;

/**
 * 
 * @author Mark Gottschling on Oct 30, 2021
 *
 */
public abstract class AbstractClaimTileEntity extends AbstractModTileEntity implements IClaimTileEntity {
	protected static final String OWNER_UUID = "owner_uuid";
	protected static final String OVERLAPS = "overlaps";	
	
	private String ownerUuid;
	private List<Box> overlaps;
	
	/**
	 * 
	 * @param type
	 */
	public AbstractClaimTileEntity(TileEntityType<?> type) {
		super(type);
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
