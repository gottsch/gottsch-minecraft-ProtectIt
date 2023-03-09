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
package mod.gottsch.forge.protectit.core.block;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.PropertyLeverBlockEntity;
import mod.gottsch.forge.protectit.core.block.entity.UnclaimedStakeBlockEntity;
import mod.gottsch.forge.protectit.core.network.PropertyLeverS2C;
import mod.gottsch.forge.protectit.core.network.ModNetworking;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Nov 7, 2021
 *
 */
public class PropertyLever extends LeverBlock implements EntityBlock {

	// redefine the VoxelShape as ClaimLever is larger than Minecraft Lever
	protected static final VoxelShape NORTH_AABB = Block.box(4.0D, 3.0D, 10.0D, 12.0D, 13.0D, 16.0D);
	protected static final VoxelShape SOUTH_AABB = Block.box(4.0D, 3.0D, 0.0D, 12.0D, 13.0D, 6.0D);
	protected static final VoxelShape WEST_AABB = Block.box(10.0D, 3.0D, 4.0D, 16.0D, 13.0D, 12.0D);
	protected static final VoxelShape EAST_AABB = Block.box(0.0D, 3.0D, 4.0D, 6.0D, 13.0D, 12.0D);
	protected static final VoxelShape UP_AABB_Z = Block.box(4.0D, 0.0D, 3.0D, 12.0D, 6.0D, 13.0D);
	protected static final VoxelShape UP_AABB_X = Block.box(3.0D, 0.0D, 4.0D, 13.0D, 6.0D, 12.0D);
	protected static final VoxelShape DOWN_AABB_Z = Block.box(4.0D, 10.0D, 3.0D, 12.0D, 16.0D, 13.0D);
	protected static final VoxelShape DOWN_AABB_X = Block.box(3.0D, 10.0D, 4.0D, 13.0D, 16.0D, 12.0D);

	/**
	 * 
	 * @param modID
	 * @param name
	 * @param material
	 */
	public PropertyLever(Block.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		PropertyLeverBlockEntity blockEntity = null;
		try {
			blockEntity = new PropertyLeverBlockEntity(pos, state);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		ProtectIt.LOGGER.debug("newBlockEntity() blockEntity -> {}}", blockEntity);
		return blockEntity;
	}


	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	// duplicate super so that this class's version of VoxelShapes will be used
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
		switch((AttachFace)state.getValue(FACE)) {
		case FLOOR:
			switch(state.getValue(FACING).getAxis()) {
			case X:
				return UP_AABB_X;
			case Z:
			default:
				return UP_AABB_Z;
			}
		case WALL:
			switch((Direction)state.getValue(FACING)) {
			case EAST:
				return EAST_AABB;
			case WEST:
				return WEST_AABB;
			case SOUTH:
				return SOUTH_AABB;
			case NORTH:
			default:
				return NORTH_AABB;
			}
		case CEILING:
		default:
			switch(state.getValue(FACING).getAxis()) {
			case X:
				return DOWN_AABB_X;
			case Z:
			default:
				return DOWN_AABB_Z;
			}
		}
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!level.isClientSide()) {
			return (lvl, pos, blockState, t) -> {
				if (t instanceof PropertyLeverBlockEntity entity) { // test and cast
					entity.tickServer();
				}
			};
		}
		return null;
	}
	
	/**
	 * 
	 */
	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		BlockEntity blockEntity = worldIn.getBlockEntity(pos);
		if (blockEntity instanceof PropertyLeverBlockEntity) {
			// get the claim for this position
			List<Box> list = ProtectionRegistries.block().getProtections(new Coords(pos), new Coords(pos).add(1, 1, 1), false, false);
			ProtectIt.LOGGER.debug("found properties -> {}", list);
			if (!list.isEmpty()) {				
				Property property = ProtectionRegistries.block().getPropertyByCoords(list.get(0).getMinCoords());
				((PropertyLeverBlockEntity)blockEntity).setPropertyCoords(property.getBox().getMinCoords());
				((PropertyLeverBlockEntity)blockEntity).setPropertyUuid(property.getUuid());

			}
		}
		worldIn.markAndNotifyBlock(pos, null, state, state, 0, 0);
	}
	
	/**
	 * 
	 */
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		// prevent use if not the owner
		if (blockEntity instanceof PropertyLeverBlockEntity) {
			Property property = ProtectionRegistries.block().getPropertyByCoords(((PropertyLeverBlockEntity)blockEntity).getPropertyCoords());
			if (property != null && !player.getStringUUID().equalsIgnoreCase(property.getOwner().getUuid()) &&
					property.getWhitelist().stream().noneMatch(p -> p.getUuid().equals(player.getStringUUID()))) {
				return InteractionResult.FAIL;
			}
			
			if (world.isClientSide) {
				BlockState blockstate1 = state.cycle(POWERED);
				if (blockstate1.getValue(POWERED)) {
					if (((PropertyLeverBlockEntity)blockEntity).getPropertyCoords() == null) {
						if (property != null) {
							((PropertyLeverBlockEntity)blockEntity).setPropertyCoords(property.getBox().getMinCoords());
							((PropertyLeverBlockEntity)blockEntity).setPropertyUuid(property.getUuid());
						}
					}
					makeParticle(blockstate1, world, pos, 1.0F);
				}
				return InteractionResult.SUCCESS;
			} else {
				BlockState blockstate = this.pull(state, world, pos);
				float f = blockstate.getValue(POWERED) ? 0.6F : 0.5F;
				world.playSound((Player)null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, f);
				// send message to clients
				if (property != null) {
					sendToClient(world, new Coords(pos), property.getBox().getMinCoords(), property.getUuid());
				}
				return InteractionResult.CONSUME;
			}
		}
		
		return InteractionResult.PASS;
	}

	// TODO don't need this is sync BE properly
	/**
	 * 
	 * @param world
	 * @param coords
	 * @param propertyCoords
	 */
	protected void sendToClient(Level world, ICoords coords, ICoords propertyCoords, UUID propertyUuid) {
		if (!world.isClientSide()) {
			if(((ServerLevel)world).getServer().isDedicatedServer()) {
				// send message to add protection on all clients
				PropertyLeverS2C message = new PropertyLeverS2C(coords, propertyCoords, propertyUuid);
				ProtectIt.LOGGER.debug("sending property lever message to sync client side -> {}", message);
				ModNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
			}
		}
	}

	
	@OnlyIn(Dist.CLIENT)
	public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
		if (state.getValue(POWERED) && random.nextFloat() < 0.25F) {
			makeParticle(state, world, pos, 0.5F);
		}
	}

	/**
	 * 
	 * @param state
	 * @param world
	 * @param pos
	 * @param scale
	 */
	private static void makeParticle(BlockState state, Level world, BlockPos pos, float scale) {
		Direction direction = state.getValue(FACING).getOpposite();
		Direction direction1 = getConnectedDirection(state).getOpposite();
		double d0 = (double)pos.getX() + 0.5D + 0.1D * (double)direction.getStepX() + 0.2D * (double)direction1.getStepX();
		double d1 = (double)pos.getY() + 0.5D + 0.1D * (double)direction.getStepY() + 0.2D * (double)direction1.getStepY();
		double d2 = (double)pos.getZ() + 0.5D + 0.1D * (double)direction.getStepZ() + 0.2D * (double)direction1.getStepZ();
//		world.addParticle(new RedstoneParticleData(0F, 1F, 0F, scale), d0, d1, d2, 0.0D, 0.0D, 0.0D);
//		world.sendParticles(ParticleTypes.SMOKE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
		world.addParticle(ParticleTypes.EFFECT, d0, d1, d2, 0.0D, 0.0D, 0.0D);
	}

	/**
	 * 
	 */
	@Override
	public BlockState pull(BlockState state, Level world, BlockPos pos) {
		state = state.cycle(POWERED);
		world.setBlock(pos, state, 3);
		return state;
	}

	/**
	 * does not provide power or signal for redstone - stand alone lever
	 */
	@Override
	public boolean isSignalSource(BlockState state) {
		return false;
	}

	@Override
	public int getSignal(BlockState state, BlockGetter reader, BlockPos pos, Direction direction) {
		return 0;
	}

	@Override
	public int getDirectSignal(BlockState state, BlockGetter reader, BlockPos pos, Direction direction) {
		return 0;
	}
}
