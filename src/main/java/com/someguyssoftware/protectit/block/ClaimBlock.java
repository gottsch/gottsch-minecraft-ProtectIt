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

import com.someguyssoftware.gottschcore.block.ModBlock;
import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.network.ProtectItNetworking;
import com.someguyssoftware.protectit.network.RegistryMutatorMessageToClient;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.registry.PlayerData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.tileentity.ClaimTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ClaimBlock extends ModBlock {
	public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
	public static final ICoords EMPTY = new Coords(0, -255, 0);
	/*
	 * An array of VoxelShape shapes for the bounding box
	 */
	private VoxelShape[] bounds = new VoxelShape[4];

	/*
	 * size of the claim
	 */
	private ICoords claimSize = EMPTY;

	public ClaimBlock(String modID, String name, Block.Properties properties) {
		super(modID, name, properties);

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
	public ClaimBlock(String modID, String name, ICoords claimSize, Block.Properties properties) {
		this(modID, name, properties);
		setClaimSize(claimSize);
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		ClaimTileEntity tileEntity = null;
		try {
			tileEntity = new ClaimTileEntity();
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
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockRenderType getRenderShape(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		TileEntity tileEntity = worldIn.getBlockEntity(pos);
		if (tileEntity instanceof ClaimTileEntity) {
			((ClaimTileEntity) tileEntity).setOwnerUuid(placer.getStringUUID());
			// save any overlaps to the TileEntity
			Box box = getBox(tileEntity.getBlockPos());
			List<Box> overlaps = ProtectionRegistries.block().getProtections(box.getMinCoords(), box.getMaxCoords());
//			ProtectIt.LOGGER.info("num of overlaps @ {} <--> {} -> {}", box.getMinCoords().toShortString(), box.getMaxCoords().toShortString(), overlaps.size());
			if (!overlaps.isEmpty()) {
				((ClaimTileEntity)tileEntity).getOverlaps().addAll(overlaps);
			}
		}
	}

	/**
	 * Called on server when World#addBlockEvent is called. If server returns true, then also called on the client. On
	 * the Server, this may perform additional changes to the world, like pistons replacing the block with an extended
	 * base. On the client, the update may involve replacing tile entities or effects such as sounds or particles
	 * @deprecated call via {@link IBlockState#onBlockEventReceived(World,BlockPos,int,int)} whenever possible.
	 * Implementing/overriding is fine.
	 */
	@Override
	public boolean triggerEvent(BlockState state, World world, BlockPos pos, int id, int param) {
		super.triggerEvent(state, world, pos, id, param);
		TileEntity tileEntity = world.getBlockEntity(pos);
		return tileEntity == null ? false : tileEntity.triggerEvent(id, param);
	}

	/**
	 * Convenience method.
	 * @param state
	 * @return
	 */
	public static Direction getFacing(BlockState state) {
		return state.getValue(FACING);
	}

	/**
	 * 
	 */
	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		switch(state.getValue(FACING)) {
		default:
		case NORTH:
			return bounds[0];
		case EAST:
			return bounds[1];
		case SOUTH:
			return bounds[2];
		case WEST:
			return bounds[3];
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		BlockState blockState = this.defaultBlockState().setValue(FACING,
				context.getHorizontalDirection().getOpposite());
		return blockState;
	}

	/**
	 * 
	 */
	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
			Hand handIn, BlockRayTraceResult hit) {

		// exit if on the client
		if (WorldInfo.isClientSide(world)) {
			return ActionResultType.SUCCESS;
		}
		ProtectIt.LOGGER.debug("in claim block use() on server... is dedicated -> {}", player.getServer().isDedicatedServer());

		// get the tile entity
		TileEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof ClaimTileEntity) {
			final Box box = getBox(pos);

			// add area to protections registry if this is a dedicated server
			if (!ProtectionRegistries.block().isProtected(box.getMinCoords(), box.getMaxCoords())) {
				ProtectIt.LOGGER.info("not protected");
				// check if player already owns protections
				List<Claim> claims = ProtectionRegistries.block().getProtections(player.getStringUUID());
				// create a claim
				Claim claim = new Claim(
						box.getMinCoords(), 
						box,
						new PlayerData(player.getStringUUID(), player.getName().getString()),
						String.valueOf(claims.size() + 1));
				ProtectionRegistries.block().addProtection(claim);
//				ProtectionRegistries.block().addProtection(box.getMinCoords(), box.getMaxCoords(), new PlayerData(player.getStringUUID(), player.getName().getString()));
				
				ProtectIt.LOGGER.info("should've added -> {} {}", box, player.getStringUUID());
				ProtectItSavedData savedData = ProtectItSavedData.get(world);
				// mark data as dirty
				if (savedData != null) {
					savedData.setDirty();
				}

				if(((ServerWorld)world).getServer().isDedicatedServer()) {
					// send message to add protection on all clients
					RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
							RegistryMutatorMessageToClient.BLOCK_TYPE, 
							RegistryMutatorMessageToClient.ADD_ACTION, 
							player.getStringUUID()).with($ -> {
								$.coords1 = box.getMinCoords();//.get();
								$.coords2 = box.getMaxCoords();//ref2.get();
								$.playerName = player.getName().getString();
							}).build();
					ProtectIt.LOGGER.info("sending message to sync client side ");
					ProtectItNetworking.simpleChannel.send(PacketDistributor.ALL.noArg(), message);
				}
				// remove claim block
				world.removeBlock(pos, false);

				// send message to player
				player.sendMessage(new TranslationTextComponent("message.protectit.block_region_successfully_protected", box.getMinCoords(), box.getMaxCoords()), player.getUUID());
			}
			else {
				// message player that the area is already protected
				player.sendMessage(new TranslationTextComponent("message.protectit.block_region_protected"), player.getUUID());
			}
		}

		return ActionResultType.SUCCESS;
	}

	/**
	 * 
	 * @param pos
	 * @return
	 */
	public Box getBox(BlockPos pos) {
		BlockPos p1 = pos.offset(0, -(claimSize.getY()/2), 0);
		BlockPos p2 = p1.offset(claimSize.getX(), claimSize.getY(), claimSize.getZ());		
		return getBox(new Coords(p1), new Coords(p2));
	}

	/**
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public Box getBox(ICoords c1, ICoords c2) {
		if (c1.getY() < WorldInfo.BOTTOM_HEIGHT) {
			c1 = c1.withY(WorldInfo.BOTTOM_HEIGHT);
			c2 = c1.offset(claimSize);
		}
		else if (c2.getY() > WorldInfo.MAX_HEIGHT) {
			c1 = new Coords(c1.getX(), WorldInfo.MAX_HEIGHT - claimSize.getY(), c1.getZ());
			c2 = new Coords(c2.getX(), WorldInfo.MAX_HEIGHT, c2.getZ());
		}
		return new Box(c1, c2);
	}

	/**
	 * Returns the blockstate setValue the given rotation from the passed blockstate. If inapplicable, returns the passed
	 * blockstate.
	 * @deprecated call via {@link IBlockState#setValueRotation(Rotation)} whenever possible. Implementing/overriding is
	 * fine.
	 */
	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	/**
	 * Returns the blockstate setValue the given mirror of the passed blockstate. If inapplicable, returns the passed
	 * blockstate.
	 * @deprecated call via {@link IBlockState#setValueMirror(Mirror)} whenever possible. Implementing/overriding is fine.
	 */
	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	public VoxelShape[] getBounds() {
		return bounds;
	}

	public ClaimBlock setBounds(VoxelShape[] bounds) {
		this.bounds = bounds;
		return this;
	}

	public ICoords getClaimSize() {
		return claimSize;
	}

	public void setClaimSize(ICoords claimSize) {
		this.claimSize = claimSize;
	}
}
