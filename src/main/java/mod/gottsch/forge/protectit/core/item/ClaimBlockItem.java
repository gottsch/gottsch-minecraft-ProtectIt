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
package mod.gottsch.forge.protectit.core.item;

import java.util.List;

import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.core.config.Config;
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
 * @author Mark Gottschling on Dec 1, 2021
 *
 */
public class ClaimBlockItem extends BlockItem {

	/**
	 * 
	 * @param block
	 * @param properties
	 */
	public ClaimBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	/**
	 * 
	 */
	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
		// gather the number of claims the player has
		List<Property> claims = ProtectionRegistries.block().getProtections(context.getPlayer().getStringUUID());
		
		if (claims.size() >= Config.GENERAL.propertiesPerPlayer.get()) {
			if (WorldInfo.isServerSide(context.getLevel())) {
				context.getPlayer().sendSystemMessage(Component.translatable("message.protectit.max_claims_met"));
			}
			return false;
		}
		return context.getLevel().setBlock(context.getClickedPos(), state, 26);
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, world, tooltip, flag);
		tooltip.add(Component.translatable(LangUtil.tooltip("claim.howto")).withStyle(ChatFormatting.GREEN));		
	}
}
