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
package com.someguyssoftware.protectit.block;

import java.util.List;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.tileentity.ClaimLeverTileEntity;
import com.someguyssoftware.protectit.tileentity.ClaimTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

/**
 * 
 * @author Mark Gottschling on Nov 7, 2021
 *
 */
public class ClaimLever extends LeverBlock {

	/**
	 * 
	 * @param modID
	 * @param name
	 * @param material
	 */
	public ClaimLever(String modID, String name, Block.Properties properties) {
		super(properties);
		setBlockName(modID, name);
	}

	/**
	 * 
	 * @param modID
	 * @param name
	 */
	public void setBlockName(String modID, String name) {
		setRegistryName(modID, name);
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		ClaimLeverTileEntity tileEntity = null;
		try {
			tileEntity = new ClaimLeverTileEntity();
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		ProtectIt.LOGGER.info("createNewTileEntity | tileEntity -> {}}", tileEntity);
		return tileEntity;
	}
	
	/**
	 * 
	 * @param state
	 * @return
	 */
	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}
	
	@Override
	public BlockRenderType getRenderShape(BlockState state) {
		return BlockRenderType.MODEL;
	}
	
	/**
	 * 
	 */
	public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		if (world.isClientSide) {
			BlockState blockstate1 = state.cycle(POWERED);
			if (blockstate1.getValue(POWERED)) {
				makeParticle(blockstate1, world, pos, 1.0F);
			}
			return ActionResultType.SUCCESS;
		} else {
			BlockState blockstate = this.pull(state, world, pos);
			float f = blockstate.getValue(POWERED) ? 0.6F : 0.5F;
			world.playSound((PlayerEntity)null, pos, SoundEvents.LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, f);
			return ActionResultType.CONSUME;
		}
	}

	@Override
	public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		TileEntity tileEntity = worldIn.getBlockEntity(pos);
		if (tileEntity instanceof ClaimLeverTileEntity) {
			// get the claim for this position
			List<Box> list = ProtectionRegistries.block().getProtections(new Coords(pos));
			if (!list.isEmpty()) {
				Claim claim = ProtectionRegistries.block().getClaimByCoords(list.get(0).getMinCoords());
				ProtectIt.LOGGER.debug("found claim -> {}", claim);
				((ClaimLeverTileEntity)tileEntity).setClaim(claim);
			}
		}
	}
	
	/**
	 * 
	 * @param state
	 * @param world
	 * @param pos
	 * @param scale
	 */
	private static void makeParticle(BlockState state, IWorld world, BlockPos pos, float scale) {
		Direction direction = state.getValue(FACING).getOpposite();
		Direction direction1 = getConnectedDirection(state).getOpposite();
		double d0 = (double)pos.getX() + 0.5D + 0.1D * (double)direction.getStepX() + 0.2D * (double)direction1.getStepX();
		double d1 = (double)pos.getY() + 0.5D + 0.1D * (double)direction.getStepY() + 0.2D * (double)direction1.getStepY();
		double d2 = (double)pos.getZ() + 0.5D + 0.1D * (double)direction.getStepZ() + 0.2D * (double)direction1.getStepZ();
		// TODO change to a different particle
		world.addParticle(new RedstoneParticleData(1.0F, 0.0F, 0.0F, scale), d0, d1, d2, 0.0D, 0.0D, 0.0D);
	}

	/**
	 * TODO update to set value in TE? or can the TE just look at the state of the block and render
	 */
	public BlockState pull(BlockState p_226939_1_, World p_226939_2_, BlockPos p_226939_3_) {
		p_226939_1_ = p_226939_1_.cycle(POWERED);
		p_226939_2_.setBlock(p_226939_3_, p_226939_1_, 3);
		return p_226939_1_;
	}

	/**
	 * does not provide power or signal for redstone - stand alone lever
	 */
	@Override
	public boolean isSignalSource(BlockState state) {
		return false;
	}

	@Override
	public int getSignal(BlockState state, IBlockReader reader, BlockPos pos, Direction direction) {
		return 0;
	}

	@Override
	public int getDirectSignal(BlockState state, IBlockReader reader, BlockPos pos, Direction direction) {
		return 0;
	}
}
