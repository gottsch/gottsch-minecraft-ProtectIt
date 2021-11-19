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

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
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
			SMALL_CLAIM = new ClaimBlock(ProtectIt.MODID, "small_stake", ClaimSizes.SMALL_CLAIM_SIZE, Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F));
			MEDIUM_CLAIM = new ClaimBlock(ProtectIt.MODID, "medium_claim", ClaimSizes.MEDIUM_CLAIM_SIZE, Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F));
			LARGE_CLAIM = new ClaimBlock(ProtectIt.MODID, "large_claim", ClaimSizes.LARGE_CLAIM_SIZE, Block.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F));
			
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
			
			blocks.add(SMALL_CLAIM);
			blocks.add(MEDIUM_CLAIM);
			blocks.add(LARGE_CLAIM);
			blocks.add(CLAIM_LEVER);
			blocks.add(CLAIM_LECTERN);
			
			for (Block b : blocks) {
				BlockItem blockItem = new BlockItem(b, new Item.Properties().tab(ItemGroup.TAB_MISC));
				final ResourceLocation registryName = Preconditions.checkNotNull(b.getRegistryName(),
						"Block %s has null registry name", b);
				registry.register(blockItem.setRegistryName(registryName));
			}
		}
	}
}
