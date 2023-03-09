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

import java.util.List;
import java.util.Optional;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 
 * @author Mark Gottschling Mar 1, 2023
 *
 */
public class UnclaimedStakeBlockEntity extends AbstractPropertyOutlinerBlockEntity {

	public UnclaimedStakeBlockEntity(BlockPos pos, BlockState state) {
		this(ModBlockEntities.UNCLAIMED_TYPE.get(), pos, state);
	}

	/**
	 * 
	 * @param pos
	 * @param state
	 */
	public UnclaimedStakeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
	
	@Override
	protected Optional<Property> selectProperty(List<Property> properties, Box box) {
		// get all the children
		properties.addAll(properties.stream().flatMap(p -> p.getChildren().stream()).toList());
		Property property = null;
		for (Property p : properties	) {
			if ((p.getOwner() == null || p.getOwner().equals(PlayerData.EMPTY)) && p.intersects(box)) {
				property = p;
			}
		}
		return Optional.ofNullable(property);
	}
}
