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
package mod.gottsch.forge.protectit.core.item;

import java.util.List;

import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 
 * @author Mark Gottschling Mar 8, 2023
 *
 */
public class CustomClaimBlockItem extends ClaimBlockItem {

	public CustomClaimBlockItem(Block block, Properties properties) {
		super(block, properties.stacksTo(1));
	}
	
	// TODO
	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
		// gather the number of claims the player has
		List<Property> properties = ProtectionRegistries.block().getPropertiesByOwner(context.getPlayer().getUUID());
//				.getProtections(context.getPlayer().getStringUUID());
		
		if (properties.size() >= Config.GENERAL.propertiesPerPlayer.get()) {
			if (WorldInfo.isServerSide(context.getLevel())) {
				context.getPlayer().sendSystemMessage(Component.translatable("message.protectit.max_claims_met"));
			}
			return false;
		}
		return context.getLevel().setBlock(context.getClickedPos(), state, 26);
	}
}
