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
package com.someguyssoftware.protectit.block;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.ClaimSizes;
import com.someguyssoftware.protectit.item.ClaimBlockItem;
import com.someguyssoftware.protectit.item.ClaimLecternBlockItem;
import com.someguyssoftware.protectit.item.ClaimLeverBlockItem;
import com.someguyssoftware.protectit.item.RemoveClaimBlockItem;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ProtectItBlocks {
	public static Block SMALL_CLAIM;
	public static Block MEDIUM_CLAIM;
	public static Block LARGE_CLAIM;
	public static Block REMOVE_CLAIM;
	
	public static Block CLAIM_LEVER;
	public static Block CLAIM_LECTERN;
	
	/**
	 *
	 */
	@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD)
	public static class RegistrationHandler {

		/**
		 * 
		 * @param event
		 */
		@SubscribeEvent
		public static void registerBlocks(RegistryEvent.Register<Block> event) {
			VoxelShape smallClaimShape = Block.box(7, 0, 7, 9, 10, 9);
			SMALL_CLAIM = new ClaimBlock(ProtectIt.MODID, "small_stake", ClaimSizes.SMALL_CLAIM_SIZE, Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F))
					.setBounds(new VoxelShape[] {  smallClaimShape, smallClaimShape, smallClaimShape, smallClaimShape });
			
			VoxelShape mediumClaimShape = Block.box(7, 0, 7, 9, 13, 9);
			MEDIUM_CLAIM = new ClaimBlock(ProtectIt.MODID, "medium_claim", ClaimSizes.MEDIUM_CLAIM_SIZE, Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F))
					.setBounds(new VoxelShape[] {  mediumClaimShape, mediumClaimShape, mediumClaimShape, mediumClaimShape });
			
			VoxelShape largeClaimShape = Block.box(7, 0, 7, 9, 16, 9);
			LARGE_CLAIM = new ClaimBlock(ProtectIt.MODID, "large_claim", ClaimSizes.LARGE_CLAIM_SIZE, Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F))
					.setBounds(new VoxelShape[] {  largeClaimShape, largeClaimShape, largeClaimShape, largeClaimShape });
			
			REMOVE_CLAIM = new RemoveClaimBlock(ProtectIt.MODID, "remove_claim", ClaimSizes.MEDIUM_CLAIM_SIZE, Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F))
					.setBounds(new VoxelShape[] {  mediumClaimShape, mediumClaimShape, mediumClaimShape, mediumClaimShape });
			
			CLAIM_LEVER = new ClaimLever(ProtectIt.MODID, "claim_lever", Block.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.75F));
			CLAIM_LECTERN = new ClaimLectern(ProtectIt.MODID, "claim_lectern", Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(2.5F));
			
			/*
	         * register blocks
	         */
			final IForgeRegistry<Block> registry = event.getRegistry();
			registry.registerAll(
					SMALL_CLAIM,
					MEDIUM_CLAIM,
					LARGE_CLAIM,
					REMOVE_CLAIM,
					CLAIM_LEVER,
					CLAIM_LECTERN
					);
		}
		
		/**
		 * Register this mod's {@link ItemBlock}s.
		 *
		 * @param event The event
		 */
		@SubscribeEvent
		public static void registerItemBlocks(final RegistryEvent.Register<Item> event) {
			final IForgeRegistry<Item> registry = event.getRegistry();
			
			List<Block> blocks = new ArrayList<>(3);
			List<Block> claimBlocks = new ArrayList<>(3);
			
			claimBlocks.add(SMALL_CLAIM);
			claimBlocks.add(MEDIUM_CLAIM);
			claimBlocks.add(LARGE_CLAIM);
//			blocks.add(CLAIM_LEVER);
//			blocks.add(CLAIM_LECTERN);
			
			// claim blocks must use the ClaimBlockItem class
			for (Block b : claimBlocks) {
				ClaimBlockItem blockItem = new ClaimBlockItem(b, new Item.Properties().tab(ItemGroup.TAB_MISC));
				final ResourceLocation registryName = Preconditions.checkNotNull(b.getRegistryName(),
						"Block %s has null registry name", b);
				registry.register(blockItem.setRegistryName(registryName));
			}
						
//			for (Block b : blocks) {
//				BlockItem blockItem = new BlockItem(b, new Item.Properties().tab(ItemGroup.TAB_MISC));
//				final ResourceLocation registryName = Preconditions.checkNotNull(b.getRegistryName(),
//						"Block %s has null registry name", b);
//				registry.register(blockItem.setRegistryName(registryName));
//			}
			
			RemoveClaimBlockItem blockItem = new RemoveClaimBlockItem(REMOVE_CLAIM, new Item.Properties().tab(ItemGroup.TAB_MISC));
			register(registry, blockItem, REMOVE_CLAIM);
			
			BlockItem leverBlockItem = new ClaimLeverBlockItem(CLAIM_LEVER, new Item.Properties().tab(ItemGroup.TAB_MISC));			
			register(registry, leverBlockItem, CLAIM_LEVER);
			
			BlockItem lecternBlockItem = new ClaimLecternBlockItem(CLAIM_LECTERN, new Item.Properties().tab(ItemGroup.TAB_MISC));			
			register(registry, lecternBlockItem, CLAIM_LECTERN);
		}

		/**
		 * 
		 * @param registry
		 * @param blockItem
		 * @param block
		 */
		private static void register(IForgeRegistry<Item> registry, BlockItem blockItem,	Block block) {
			final ResourceLocation registryName = Preconditions.checkNotNull(block.getRegistryName(),
					"Block %s has null registry name", block);
			registry.register(blockItem.setRegistryName(registryName));
		}
	}
}
