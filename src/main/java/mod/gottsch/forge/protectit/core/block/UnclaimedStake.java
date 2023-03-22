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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.ClaimBlockEntity;
import mod.gottsch.forge.protectit.core.block.entity.UnclaimedStakeBlockEntity;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.network.ModNetworking;
import mod.gottsch.forge.protectit.core.network.RegistryMutatorMessageToClient;
import mod.gottsch.forge.protectit.core.persistence.ProtectItSavedData;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.property.PropertyUtil;
import mod.gottsch.forge.protectit.core.registry.PlayerIdentity;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling Feb 28, 2023
 *
 */
public class UnclaimedStake extends ClaimBlock implements EntityBlock {

	/**
	 * 
	 * @param properties
	 */
	public UnclaimedStake(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		UnclaimedStakeBlockEntity blockEntity = null;
		try {
			blockEntity = new UnclaimedStakeBlockEntity(pos, state);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error(e);
		}
		ProtectIt.LOGGER.debug("blockEntity -> {}", blockEntity);
		return blockEntity;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!level.isClientSide()) {
			return (lvl, pos, blockState, t) -> {
				if (t instanceof UnclaimedStakeBlockEntity entity) { // test and cast
					entity.tickServer();
				}
			};
		}
		return null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (level.isClientSide()) {
			return;
		}
		
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (blockEntity instanceof UnclaimedStakeBlockEntity) {
			//			Box box = getBox(level, blockEntity.getBlockPos());
			// need to find ALL the protections
			List<Box> protections = ProtectionRegistries.property().getProtections(new Coords(pos), new Coords(pos).add(1, 1, 1), false, false);
			if (!protections.isEmpty()) {
				// get the property
				List<Property> properties = protections.stream().flatMap(p -> ProtectionRegistries.property().getPropertyByCoords(p.getMinCoords()).stream()).toList();
				Optional<Property> property = PropertyUtil.getLeastSignificant(properties);
				// ensure property is an available fief
				if (property.isPresent()) {
					if (property.get().isDomain() || !property.get().isFiefAvailable()) {
						property = Optional.empty();
					}
				}
				if (property.isPresent()) {
					((UnclaimedStakeBlockEntity)blockEntity).setPropertyCoords(property.get().getBox().getMinCoords());
					((UnclaimedStakeBlockEntity)blockEntity).setPropertyUuid(property.get().getUuid());
					((UnclaimedStakeBlockEntity)blockEntity).setPropertyBox(property.get().getBox());
				}
			}
			level.markAndNotifyBlock(pos, null, state, state, 0, 0);
		}
	}

	/**
	 * Nothing happens on use. Ownership transfer is accomplished with Lease item.
	 */
	@Override
	public  InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand handIn, BlockHitResult hit) {
		return InteractionResult.SUCCESS;
	}
}
