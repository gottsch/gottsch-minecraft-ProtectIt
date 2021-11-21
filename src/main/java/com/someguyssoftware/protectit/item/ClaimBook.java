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

import javax.annotation.Nullable;

import com.someguyssoftware.gottschcore.item.ModItem;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ClaimLectern;
import com.someguyssoftware.protectit.block.ProtectItBlocks;
import com.someguyssoftware.protectit.gui.screen.EditClaimBookScreen;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 
 * @author Mark Gottschling on Nov 16, 2021
 *
 */
public class ClaimBook extends ModItem {
	public static final String PAGES_TAG = "pages";
	
	/**
	 * 
	 */
	public ClaimBook(String modID, String name, Item.Properties properties) {
		super(modID, name, properties);
	}

	/**
	 * 
	 */
	public ActionResultType useOn(ItemUseContext context) {
		World world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = world.getBlockState(pos);
		// TODO maybe have to change to instanceof interface in future
		if (state.is(ProtectItBlocks.CLAIM_LECTERN)) {
			ProtectIt.LOGGER.debug("lectern is a ClaimLectern");
			return ClaimLectern.tryPlaceBook(world, pos, state, context.getItemInHand())
					? ActionResultType.sidedSuccess(world.isClientSide)
					: ActionResultType.PASS;
		} else {
			ProtectIt.LOGGER.debug("what is it then? -> {}", state.getBlock().getRegistryName().toString());
			return ActionResultType.PASS;
		}
	}

	/**
	 * 
	 */
	public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		if (player instanceof ClientPlayerEntity) {
			// open the edit claim book screen
			Minecraft.getInstance().setScreen(new EditClaimBookScreen(player, itemStack, hand));
		}
		return ActionResult.sidedSuccess(itemStack, world.isClientSide());
	}

	/**
	 * 
	 * @param nbt
	 * @return
	 */
	public static boolean makeSureTagIsValid(@Nullable CompoundNBT nbt) {
		if (nbt == null) {
			return false;
		} else if (!nbt.contains(PAGES_TAG, 9)) {
			return false;
		} else {
			ListNBT list = nbt.getList(PAGES_TAG, 8);

			for (int i = 0; i < list.size(); ++i) {
				String s = list.getString(i);
				if (s.length() > 32767) {
					return false;
				}
			}
			return true;
		}
	}
}
