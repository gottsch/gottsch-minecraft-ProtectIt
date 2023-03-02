/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.protectit.core.item;

import java.util.List;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;


/**
 * 
 * @author Mark Gottschling Mar 1, 2023
 *
 */
public class UnclaimedStakeBlockItem extends BlockItem {

	/**
	 * 
	 * @param block
	 * @param properties
	 */
	public UnclaimedStakeBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	/**
	 * 
	 */
	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
		// prevent use if not the owner
		Coords coords = new Coords(context.getClickedPos());
		List<Box> list = ProtectionRegistries.block().getProtections(coords, coords.add(1, 1,1), false, false);
		if (!list.isEmpty()) {				
			Property property = ProtectionRegistries.block().getClaimByCoords(list.get(0).getMinCoords());
			
			// TODO change to only landlord or owner
			if (property != null && !context.getPlayer().getStringUUID().equalsIgnoreCase(property.getOwner().getUuid())) {
				context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.message("block_region.not_owner")));
				return false;
			}
		}
		return context.getLevel().setBlock(context.getClickedPos(), state, 26);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, world, tooltip, flag);

		tooltip.add(Component.translatable(LangUtil.tooltip("unclaimed_stake.howto")).withStyle(ChatFormatting.RED));

	}
}
