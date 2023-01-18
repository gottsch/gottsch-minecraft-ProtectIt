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
package com.someguyssoftware.protectit.item;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.block.ClaimLectern;
import com.someguyssoftware.protectit.block.ProtectItBlocks;
import com.someguyssoftware.protectit.claim.Claim;
import com.someguyssoftware.protectit.client.screen.OpenScreenUtil;
import com.someguyssoftware.protectit.registry.PlayerData;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * 
 * @author Mark Gottschling on Nov 16, 2021
 *
 */
public class ClaimBook extends Item {
	public static final String PAGES_TAG = "pages";
	public static final String PLAYER_DATA_TAG = "playerData";
	
	/**
	 * 
	 */
	public ClaimBook(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, world, tooltip, flag);
		tooltip.add(new TranslatableComponent("tooltip.protectit.claim_book.howto").withStyle(ChatFormatting.GREEN));		
	}
	
	/**
	 * 
	 */
	public InteractionResult useOn(UseOnContext context) {
		ProtectIt.LOGGER.info("using ClaimBook...");
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = world.getBlockState(pos);
		// TODO maybe have to change to instanceof interface in future
		if (state.is(ProtectItBlocks.CLAIM_LECTERN.get())) {
			ProtectIt.LOGGER.info("lectern is a ClaimLectern");
			return ClaimLectern.tryPlaceBook(context.getPlayer(), world, pos, state, context.getItemInHand())
					? InteractionResult.sidedSuccess(world.isClientSide)
					: InteractionResult.PASS;
		} else {
			ProtectIt.LOGGER.info("what is it then? -> {}", state.getBlock().getRegistryName().toString());
			return InteractionResult.PASS;
		}
	}

	/**
	 * 
	 */
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ProtectIt.LOGGER.info("right-using ClaimBook in hand..");
		ItemStack itemStack = player.getItemInHand(hand);
		if ( world.isClientSide()) {
			/*
			 *  open the edit claim book screen.
			 *  must use DistExectuor to safely open code that should only run on one physical side.
			 *  the actual call to the sided-code should be in it's own class
			 */
			DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> OpenScreenUtil.openEditClaimBookScreen(player, itemStack, hand));
		}
		return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
	}

	/**
	 * 
	 * @param nbt
	 * @return
	 */
	public static boolean makeSureTagIsValid(@Nullable CompoundTag nbt) {
		if (nbt == null) {
			return false;
		} else if (!nbt.contains(PAGES_TAG, 9)) {
			return false;
		} else {
			ListTag list = nbt.getList(PAGES_TAG, 8);

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
		if (itemStack.getItem() != ProtectItItems.CLAIM_BOOK.get()) {
			return result;
		}
		
		// get white list from book stack
		CompoundTag nbt = itemStack.getTag();
		if (nbt != null) {
			// load the PlayerData			
			ListTag playerDataListTag = nbt.getList(ClaimBook.PLAYER_DATA_TAG, 10);
			playerDataListTag.forEach(element -> {
				PlayerData playerData = new PlayerData();
				playerData.load((CompoundTag)element);
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
		CompoundTag nbt = itemStack.getTag();
		if (nbt != null) {
			CompoundTag claimNbt = nbt.getCompound("claim");
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
	public static ListTag savePlayerData(ItemStack itemStack, List<PlayerData> data) {
		ListTag playerDataList = new ListTag();
		data.forEach(playerData -> {
			CompoundTag nbt = new CompoundTag();
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
	public static CompoundTag saveClaim(ItemStack itemStack, Claim registryClaim) {
		CompoundTag claimNbt = new CompoundTag();
		registryClaim.save(claimNbt);
		itemStack.removeTagKey("claim");
		itemStack.addTagElement("claim", claimNbt);
		return claimNbt;
	}
}
