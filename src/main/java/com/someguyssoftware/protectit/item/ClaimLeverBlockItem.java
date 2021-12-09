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

import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.config.Config;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

/**
 * 
 * @author Mark Gottschling on Dec 4, 2021
 *
 */
public class ClaimLeverBlockItem extends BlockItem {

	/**
	 * 
	 * @param block
	 * @param properties
	 */
	public ClaimLeverBlockItem(Block block, Properties properties) {
		super(block, properties);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
		super.appendHoverText(stack, world, tooltip, flag);

		tooltip.add(new TranslationTextComponent("tooltip.protectit.claim_lever.howto").withStyle(TextFormatting.GREEN));
		
	}
}
