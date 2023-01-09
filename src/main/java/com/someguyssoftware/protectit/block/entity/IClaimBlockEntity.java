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
package com.someguyssoftware.protectit.block.entity;

import java.util.List;

import mod.gottsch.forge.gottschcore.spatial.Box;

/**
 * 
 * @author Mark Gottschling on Oct 30, 2021
 *
 */
public interface IClaimBlockEntity {

	String getOwnerUuid();

	void setOwnerUuid(String ownerUuid);

	List<Box> getOverlaps();

	void setOverlaps(List<Box> overlaps);

}
