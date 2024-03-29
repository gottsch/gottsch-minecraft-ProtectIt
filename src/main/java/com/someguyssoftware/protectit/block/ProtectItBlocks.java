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
package com.someguyssoftware.protectit.block;

import com.someguyssoftware.protectit.claim.PropertySizes;
import com.someguyssoftware.protectit.setup.Registration;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ProtectItBlocks {
	public static final RegistryObject<Block> SMALL_CLAIM;
	public static final RegistryObject<Block> MEDIUM_CLAIM;
	public static final RegistryObject<Block> LARGE_CLAIM;
	public static final RegistryObject<Block> REMOVE_CLAIM;
	
	public static final RegistryObject<Block> PROPERTY_LEVER = Registration.BLOCKS.register("property_lever", 
			() -> new PropertyLever(Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.75F)));
	
	static {
		VoxelShape smallClaimShape = Block.box(7, 0, 7, 9, 10, 9);
		VoxelShape mediumClaimShape = Block.box(7, 0, 7, 9, 13, 9);
		VoxelShape largeClaimShape = Block.box(7, 0, 7, 9, 16, 9);
		
		SMALL_CLAIM = Registration.BLOCKS.register("small_claim", 
				() -> new ClaimBlock(PropertySizes.SMALL_CLAIM_SIZE, Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F))
				.setBounds(new VoxelShape[] {  smallClaimShape, smallClaimShape, smallClaimShape, smallClaimShape }));
		
		MEDIUM_CLAIM = Registration.BLOCKS.register("medium_claim", 
				() -> new ClaimBlock(PropertySizes.MEDIUM_CLAIM_SIZE, Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F))
				.setBounds(new VoxelShape[] {  mediumClaimShape, mediumClaimShape, mediumClaimShape, mediumClaimShape }));
		
		LARGE_CLAIM = Registration.BLOCKS.register("large_claim", 
				() -> new ClaimBlock(PropertySizes.LARGE_CLAIM_SIZE, Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F))
				.setBounds(new VoxelShape[] {  largeClaimShape, largeClaimShape, largeClaimShape, largeClaimShape }));
		
		REMOVE_CLAIM = Registration.BLOCKS.register("remove_claim", 
				() -> new RemoveClaimBlock(PropertySizes.MEDIUM_CLAIM_SIZE, Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5F))
				.setBounds(new VoxelShape[] {  mediumClaimShape, mediumClaimShape, mediumClaimShape, mediumClaimShape }));
	}
	
	/**
	 * 
	 */
	public static void register() {
		// cycle through all block and create items
		Registration.registerBlocks();
	}
}
