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
package mod.gottsch.forge.protectit.core.block.entity;

import mod.gottsch.forge.protectit.core.block.ModBlocks;
import mod.gottsch.forge.protectit.core.setup.Registration;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ModBlockEntities {
	public static final RegistryObject<BlockEntityType<ClaimBlockEntity>> CLAIM_TYPE;
	public static final RegistryObject<BlockEntityType<CustomClaimBlockEntity>> CUSTOM_CLAIM_TYPE;
	public static final RegistryObject<BlockEntityType<RemoveClaimBlockEntity>> REMOVE_CLAIM_TYPE;
	public static final RegistryObject<BlockEntityType<PropertyLeverBlockEntity>> PROPERTY_LEVER_TYPE;
	public static final RegistryObject<BlockEntityType<UnclaimedStakeBlockEntity>> UNCLAIMED_TYPE;
	public static final RegistryObject<BlockEntityType<FiefStakeEntity>> FIEF_TYPE;
	
	static {
		CLAIM_TYPE = Registration.BLOCK_ENTITIES.register("claim_te", () -> BlockEntityType.Builder.of(ClaimBlockEntity::new, 
				ModBlocks.SMALL_CLAIM.get(), 
				ModBlocks.MEDIUM_CLAIM.get(), 
				ModBlocks.LARGE_CLAIM.get()
			).build(null));

		CUSTOM_CLAIM_TYPE = Registration.BLOCK_ENTITIES.register("custom_claim", () -> BlockEntityType.Builder.of(CustomClaimBlockEntity::new, 
				ModBlocks.CUSTOM_CLAIM.get()
			).build(null));
		
		REMOVE_CLAIM_TYPE = Registration.BLOCK_ENTITIES.register("remove_claim_te", () -> BlockEntityType.Builder.of(RemoveClaimBlockEntity::new, 
				ModBlocks.REMOVE_CLAIM.get()
			).build(null));
		
		PROPERTY_LEVER_TYPE = Registration.BLOCK_ENTITIES.register("property_lever_te", () -> BlockEntityType.Builder.of(PropertyLeverBlockEntity::new, 
				ModBlocks.PROPERTY_LEVER.get()
			).build(null));
		
		UNCLAIMED_TYPE = Registration.BLOCK_ENTITIES.register("unclaimed", () -> BlockEntityType.Builder.of(UnclaimedStakeBlockEntity::new, 
				ModBlocks.UNCLAIMED_STAKE.get()
			).build(null));
		
		FIEF_TYPE = Registration.BLOCK_ENTITIES.register("fief", () -> BlockEntityType.Builder.of(FiefStakeEntity::new, 
				ModBlocks.FIEF_STAKE.get()
			).build(null));
	}
	
	/**
	 * 
	 */
	public static void register() {
		// cycle through all block and create items
		Registration.registerBlockEntities();
	}
}
