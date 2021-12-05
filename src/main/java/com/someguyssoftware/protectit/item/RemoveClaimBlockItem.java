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
package com.someguyssoftware.protectit.item;

import java.util.List;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.config.Config;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.tileentity.ClaimLeverTileEntity;
import com.someguyssoftware.protectit.tileentity.RemoveClaimTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

/**
 * 
 * @author Mark Gottschling on Dec 1, 2021
 *
 */
public class RemoveClaimBlockItem extends BlockItem {

	/**
	 * 
	 * @param block
	 * @param properties
	 */
	public RemoveClaimBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	/**
	 * 
	 */
	@Override
	protected boolean placeBlock(BlockItemUseContext context, BlockState state) {
		// prevent use if not the owner
		Coords coords = new Coords(context.getClickedPos());
		List<Box> list = ProtectionRegistries.block().getProtections(coords, coords.add(1, 1,1), false, false);
		if (!list.isEmpty()) {				
			Claim claim = ProtectionRegistries.block().getClaimByCoords(list.get(0).getMinCoords());
			if (claim != null && !context.getPlayer().getStringUUID().equalsIgnoreCase(claim.getOwner().getUuid())) {
				context.getPlayer().sendMessage(new TranslationTextComponent("message.protectit.block_region_not_owner"), context.getPlayer().getUUID());
				return false;
			}
		}
		return context.getLevel().setBlock(context.getClickedPos(), state, 26);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
		super.appendHoverText(stack, world, tooltip, flag);

		tooltip.add(new TranslationTextComponent("tooltip.protectit.remove_claim.howto").withStyle(TextFormatting.RED));

	}
}
