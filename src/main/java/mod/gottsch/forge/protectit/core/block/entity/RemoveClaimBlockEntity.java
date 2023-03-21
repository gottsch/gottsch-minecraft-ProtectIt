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

import java.util.List;
import java.util.Optional;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.property.PropertyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 
 * @author Mark Gottschling on Dec 2, 2021
 *
 */
public class RemoveClaimBlockEntity extends AbstractPropertyOutlinerBlockEntity { //extends PropertyLeverBlockEntity {
	
	/**
	 * 
	 */
	public RemoveClaimBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.REMOVE_CLAIM_TYPE.get(), pos, state);
	}
	
	public RemoveClaimBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
	
	@Override
	protected Optional<Property> selectProperty(List<Property> properties, Box box) {
		Optional<Property> property = PropertyUtil.getLeastSignificant(properties);
		return property;
	}
}