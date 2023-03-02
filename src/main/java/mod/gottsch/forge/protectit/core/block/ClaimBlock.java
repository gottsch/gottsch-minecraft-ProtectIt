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

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.ClaimBlockEntity;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.network.ProtectItNetworking;
import mod.gottsch.forge.protectit.core.network.RegistryMutatorMessageToClient;
import mod.gottsch.forge.protectit.core.persistence.ProtectItSavedData;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.PlayerData;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ClaimBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
	public static final ICoords EMPTY = new Coords(0, -255, 0);
	
	/*
	 * An array of VoxelShape shapes for the bounding box
	 */
	private VoxelShape[] bounds = new VoxelShape[4];

	/*
	 * size of the claim
	 */
	private ICoords claimSize = new Coords(Coords.EMPTY);

	public ClaimBlock(Block.Properties properties) {
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
	public ClaimBlock(ICoords claimSize, Block.Properties properties) {
		this(properties);
		setClaimSize(claimSize);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		ClaimBlockEntity blockEntity = null;
		try {
			blockEntity = new ClaimBlockEntity(pos, state);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		ProtectIt.LOGGER.debug("createNewTileEntity | blockEntity -> {}}", blockEntity);
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

	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		BlockEntity blockEntity = worldIn.getBlockEntity(pos);
//		ProtectIt.LOGGER.debug("setPlacedBy claimBlock TE -> {}", tileEntity.getClass().getSimpleName());
		// gather the number of claims the player has
		List<Property> claims = ProtectionRegistries.block().getProtections(placer.getStringUUID());		
		if (claims.size() >= Config.GENERAL.propertiesPerPlayer.get()) {
			placer.sendSystemMessage(Component.translatable("message.protectit.max_claims_met"));
			return;
		}
		
		if (blockEntity instanceof ClaimBlockEntity) {
			((ClaimBlockEntity) blockEntity).setOwnerUuid(placer.getStringUUID());
//			ProtectIt.LOGGER.debug("setting ower to -> {}",( (ClaimTileEntity) tileEntity).getOwnerUuid());
			// save any overlaps to the TileEntity
			Box box = getBox(blockEntity.getBlockPos());
			List<Box> overlaps = ProtectionRegistries.block().getProtections(box.getMinCoords(), box.getMaxCoords(), false, false);
			ProtectIt.LOGGER.debug("num of overlaps @ {} <--> {} -> {}", box.getMinCoords().toShortString(), box.getMaxCoords().toShortString(), overlaps.size());
			if (!overlaps.isEmpty()) {
				((ClaimBlockEntity)blockEntity).getOverlaps().addAll(overlaps);
			}
		}
	}

	/**
	 * 
	 */
	@Override
	public  InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand handIn, BlockHitResult hit) {

		// exit if on the client
		if (WorldInfo.isClientSide(level)) {
			return InteractionResult.SUCCESS;
		}
		ProtectIt.LOGGER.debug("in claim block use() on server... is dedicated -> {}", player.getServer().isDedicatedServer());

		// gather the number of claims the player has
		List<Property> claims = ProtectionRegistries.block().getProtections(player.getStringUUID());		
		ProtectIt.LOGGER.debug("claims -> {}", claims);
		
		// prevent the use of claim if max claims is met
		if (claims.size() >= Config.GENERAL.propertiesPerPlayer.get()) {
			player.sendSystemMessage(Component.translatable("message.protectit.max_claims_met"));
			return InteractionResult.SUCCESS;
		}
		
		// get the tile entity
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof ClaimBlockEntity) {
			final Box box = getBox(pos);

			// add area to protections registry if this is a dedicated server
			if (!ProtectionRegistries.block().isProtected(box.getMinCoords(), box.getMaxCoords(), false)) {
				ProtectIt.LOGGER.debug("not protected");
				// check if player already owns protections
//				List<Claim> claims = ProtectionRegistries.block().getProtections(player.getStringUUID());
				// create a claim
				Property property = new Property(
						box.getMinCoords(), 
						box,
						new PlayerData(player.getStringUUID(), player.getName().getString()),
						String.valueOf(claims.size() + 1));
				property.setCreateTime(level.getGameTime());
				
				ProtectionRegistries.block().addProtection(property);

				ProtectIt.LOGGER.debug("should've added -> {} {}", box, player.getStringUUID());
				ProtectItSavedData savedData = ProtectItSavedData.get(level);
				// mark data as dirty
				if (savedData != null) {
					savedData.setDirty();
				}

				if(((ServerLevel)level).getServer().isDedicatedServer()) {
					// send message to add protection on all clients
					RegistryMutatorMessageToClient message = new RegistryMutatorMessageToClient.Builder(
							RegistryMutatorMessageToClient.BLOCK_TYPE, 
							RegistryMutatorMessageToClient.ADD_ACTION, 
							player.getStringUUID()).with($ -> {
								$.coords1 = box.getMinCoords();//.get();
								$.coords2 = box.getMaxCoords();//ref2.get();
								$.playerName = player.getName().getString();
							}).build();
					ProtectIt.LOGGER.debug("sending message to sync client side ");
					ProtectItNetworking.channel.send(PacketDistributor.ALL.noArg(), message);
				}
				// remove claim block
				level.removeBlock(pos, false);

				// give player Claim Lectern and Lever
//				ItemStack lecternStack = new ItemStack(ProtectItBlocks.CLAIM_LECTERN.get());
//				if (!player.getInventory().add(lecternStack)) {
//					player.drop(lecternStack, false);
//				}
				ItemStack leverStack = new ItemStack(ProtectItBlocks.PROPERTY_LEVER.get());
				if (!player.getInventory().add(leverStack)) {
					player.drop(leverStack, false);
				}
				
				// send message to player
				player.sendSystemMessage(Component.translatable("message.protectit.block_region.successfully_protected", box.getMinCoords(), box.getMaxCoords()));
			}
			else {
				// message player that the area is already protected
				player.sendSystemMessage(Component.translatable("message.protectit.block_region.protected"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Called on server when World#addBlockEvent is called. If server returns true, then also called on the client. On
	 * the Server, this may perform additional changes to the world, like pistons replacing the block with an extended
	 * base. On the client, the update may involve replacing tile entities or effects such as sounds or particles
	 * @deprecated call via {@link IBlockState#onBlockEventReceived(World,BlockPos,int,int)} whenever possible.
	 * Implementing/overriding is fine.
	 */
	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int id, int param) {
		super.triggerEvent(state, world, pos, id, param);
		BlockEntity tileEntity = world.getBlockEntity(pos);
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
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
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
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState blockState = this.defaultBlockState().setValue(FACING,
				context.getHorizontalDirection().getOpposite());
		return blockState;
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
