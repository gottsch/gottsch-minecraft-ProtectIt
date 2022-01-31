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

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.someguyssoftware.gottschcore.item.ModItem;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ClaimLectern;
import com.someguyssoftware.protectit.block.ProtectItBlocks;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.gui.screen.EditClaimBookScreen;
import com.someguyssoftware.protectit.gui.screen.OpenScreenUtil;
import com.someguyssoftware.protectit.registry.PlayerData;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.Player;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * 
 * @author Mark Gottschling on Nov 16, 2021
 *
 */
public class ClaimBook extends ModItem {
	public static final String PAGES_TAG = "pages";
	public static final String PLAYER_DATA_TAG = "playerData";
	
	/**
	 * 
	 */
	public ClaimBook(String modID, String name, Item.Properties properties) {
		super(modID, name, properties);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
		super.appendHoverText(stack, world, tooltip, flag);
		tooltip.add(new TranslationTextComponent("tooltip.protectit.claim_book.howto").withStyle(TextFormatting.GREEN));		
	}
	
	/**
	 * 
	 */
	public ActionResultType useOn(ItemUseContext context) {
		ProtectIt.LOGGER.debug("using ClaimBook...");
		World world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = world.getBlockState(pos);
		// TODO maybe have to change to instanceof interface in future
		if (state.is(ProtectItBlocks.CLAIM_LECTERN)) {
			ProtectIt.LOGGER.debug("lectern is a ClaimLectern");
			return ClaimLectern.tryPlaceBook(world, pos, state, context.getPlayer(), context.getItemInHand())
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
	public ActionResult<ItemStack> use(World world, Player player, Hand hand) {

		ItemStack itemStack = player.getItemInHand(hand);
		if ( world.isClientSide()) {
			/*
			 *  open the edit claim book screen.
			 *  must use DistExectuor to safely open code that should only run on one physical side.
			 *  the actual call to the sided-code should be in it's own class
			 */
			DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> OpenScreenUtil.openEditClaimBookScreen(player, itemStack, hand));
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
	
	/**
	 * 
	 * @param itemStack
	 * @return
	 */
	public static List<PlayerData> loadPlayerData(ItemStack itemStack) {
		List<PlayerData> result = Lists.newArrayList();
		if (itemStack.getItem() != ProtectItItems.CLAIM_BOOK) {
			return result;
		}
		
		// get white list from book stack
		CompoundNBT nbt = itemStack.getTag();
		if (nbt != null) {
			// load the PlayerData			
			ListNBT playerDataListNbt = nbt.getList(ClaimBook.PLAYER_DATA_TAG, 10);
			playerDataListNbt.forEach(element -> {
				PlayerData playerData = new PlayerData();
				playerData.load((CompoundNBT)element);
				result.add(playerData);
			});
		}
		return result;
	}
	
	/**
	 * 
	 * @param itemStack
	 * @return
	 */
	public static Claim loadClaim(ItemStack itemStack) {
		Claim claim = null;
		CompoundNBT nbt = itemStack.getTag();
		if (nbt != null) {
			CompoundNBT claimNbt = nbt.getCompound("claim");
			claim = new Claim();
			claim.load(claimNbt);
		}
		return claim;
	}
	
	/**
	 * 
	 * @param itemStack
	 * @param data
	 * @return
	 */
	public static ListNBT savePlayerData(ItemStack itemStack, List<PlayerData> data) {
		ListNBT playerDataList = new ListNBT();
		data.forEach(playerData -> {
			CompoundNBT nbt = new CompoundNBT();
			playerData.save(nbt);
			playerDataList.add(nbt);
		});
		itemStack.removeTagKey(PLAYER_DATA_TAG);
		itemStack.addTagElement(PLAYER_DATA_TAG, playerDataList);
		return playerDataList;
	}

	/**
	 * 
	 * @param itemStack
	 * @param registryClaim
	 * @return
	 */
	public static CompoundNBT saveClaim(ItemStack itemStack, Claim registryClaim) {
		CompoundNBT claimNbt = new CompoundNBT();
		registryClaim.save(claimNbt);
		itemStack.removeTagKey("claim");
		itemStack.addTagElement("claim", claimNbt);
		return claimNbt;
	}
}
