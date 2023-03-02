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
import java.util.UUID;
import java.util.stream.Collectors;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.block.ClaimBlock;
import mod.gottsch.forge.protectit.core.block.UnclaimedStake;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.PlayerData;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 
 * @author Mark Gottschling Mar 1, 2023
 *
 */
public class UnclaimedStakeBlockEntity extends PropertyLeverBlockEntity {

	public UnclaimedStakeBlockEntity(BlockPos pos, BlockState state) {
		this(ProtectItBlockEntities.UNCLAIMED_TYPE.get(), pos, state);
	}
	
	/**
	 * 
	 * @param pos
	 * @param state
	 */
	public UnclaimedStakeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	
	public void tickServer() {
		// fetch overlaps from protection registry every 5 seconds
		if (getLevel().getGameTime() % FIVE_SECONDS == 0) {
			UnclaimedStake block = (UnclaimedStake)getLevel().getBlockState(getBlockPos()).getBlock();
			Box box = block.getBox(getBlockPos());
			List<Box> protections = ProtectionRegistries.block().getProtections(box.getMinCoords(), box.getMaxCoords(), false, false);
			if (!protections.isEmpty()) {
				// get the property
				List<Property> properties = protections.stream().map(b -> ProtectionRegistries.block().getClaimByCoords(b.getMinCoords())).collect(Collectors.toList());
				// get all the children
				properties.addAll(properties.stream().flatMap(p -> p.getChildren().stream()).toList());
				Property property = null;
				for (Property p : properties	) {
					if ((p.getOwner() == null || p.getOwner().equals(PlayerData.EMPTY)) && p.intersects(box)) {
						property = p;
					}
				}

				if (property != null) {
					setPropertyCoords(property.getCoords());
					setPropertyUuid(property.getUuid());
				}
			}
		}
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
}
