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
package mod.gottsch.forge.protectit.core.block;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.FiefStakeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An ops tool.
 * @author Mark Gottschling Mar 22, 2023
 *
 */
public class FiefStake extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
	
	/*
	 * An array of VoxelShape shapes for the bounding box
	 */
	private VoxelShape[] bounds = new VoxelShape[4];

	/*
	 * size of the claim
	 */
	private ICoords size = new Coords(Coords.EMPTY);

	public FiefStake(Block.Properties properties) {
		super(properties);

		// set the default shapes/shape
		VoxelShape shape = Block.box(1, 0, 1, 15, 14, 15);
		setBounds(
				new VoxelShape[] {
						shape, 	// N
						shape,  	// E
						shape,  	// S
						shape	// W
				});
	}

	/**
	 * 
	 * @param modID
	 * @param name
	 * @param claimSize
	 * @param properties
	 */
	public FiefStake(ICoords size, Block.Properties properties) {
		this(properties);
		setSize(size);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		FiefStakeEntity blockEntity = null;
		try {
			blockEntity = new FiefStakeEntity(pos, state);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		return blockEntity;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
	
	public VoxelShape[] getBounds() {
		return bounds;
	}

	public FiefStake setBounds(VoxelShape[] bounds) {
		this.bounds = bounds;
		return this;
	}

	public ICoords getSize() {
		return size;
	}

	public void setSize(ICoords size) {
		this.size = size;
	}
}
