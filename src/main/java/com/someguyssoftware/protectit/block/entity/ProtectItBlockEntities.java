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
package com.someguyssoftware.protectit.block.entity;

import com.someguyssoftware.protectit.block.ProtectItBlocks;
import com.someguyssoftware.protectit.setup.Registration;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ProtectItBlockEntities {
	public static final RegistryObject<BlockEntityType<ClaimBlockEntity>> CLAIM_TYPE;
	public static final RegistryObject<BlockEntityType<RemoveClaimBlockEntity>> REMOVE_CLAIM_TYPE;
	public static final RegistryObject<BlockEntityType<ClaimLeverBlockEntity>> CLAIM_LEVER_TYPE;
	public static final RegistryObject<BlockEntityType<ClaimLecternBlockEntity>> CLAIM_LECTERN_TYPE;

	static {
		CLAIM_TYPE = Registration.BLOCK_ENTITIES.register("claim_te", () -> BlockEntityType.Builder.of(ClaimBlockEntity::new, 
				ProtectItBlocks.SMALL_CLAIM.get(), 
				ProtectItBlocks.MEDIUM_CLAIM.get(), 
				ProtectItBlocks.LARGE_CLAIM.get()
			).build(null));

		REMOVE_CLAIM_TYPE = Registration.BLOCK_ENTITIES.register("remove_claim_te", () -> BlockEntityType.Builder.of(RemoveClaimBlockEntity::new, 
				ProtectItBlocks.REMOVE_CLAIM.get()
			).build(null));
		
		CLAIM_LEVER_TYPE = Registration.BLOCK_ENTITIES.register("claim_lever_te", () -> BlockEntityType.Builder.of(ClaimLeverBlockEntity::new, 
				ProtectItBlocks.CLAIM_LEVER.get()
			).build(null));
		
		CLAIM_LECTERN_TYPE = Registration.BLOCK_ENTITIES.register("claim_lectern_te", () -> BlockEntityType.Builder.of(ClaimLecternBlockEntity::new, 
				ProtectItBlocks.CLAIM_LEVER.get()
			).build(null));
	}
	
	/**
	 * 
	 */
	public static void register() {
		// cycle through all block and create items
		Registration.registerBlockEntities();
	}
	
//	@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD)
//	public static class RegistrationHandler {

//		@SubscribeEvent
//		public static void onBlockEntityTypeRegistration(final RegistryEvent.Register<BlockEntityType<?>> event) {
//			CLAIM_TYPE = BlockEntityType.Builder
//					.of(ClaimBlockEntity::new, ProtectItBlocks.SMALL_CLAIM, ProtectItBlocks.MEDIUM_CLAIM, ProtectItBlocks.LARGE_CLAIM)
//					.build(null);
//			CLAIM_TYPE.setRegistryName("claim_te");
//			event.getRegistry().register(CLAIM_TYPE);
			
//			REMOVE_CLAIM_TYPE = BlockEntityType.Builder
//					.of(RemoveClaimBlockEntity::new, ProtectItBlocks.REMOVE_CLAIM)
//					.build(null);
//			REMOVE_CLAIM_TYPE.setRegistryName("remove_claim_te");
//			event.getRegistry().register(REMOVE_CLAIM_TYPE);

			// lever
//			CLAIM_LEVER_TYPE = BlockEntityType.Builder
//					.of(ClaimLeverBlockEntity::new, ProtectItBlocks.CLAIM_LEVER)
//					.build(null);
//			CLAIM_LEVER_TYPE.setRegistryName("claim_lever_te");
//			event.getRegistry().register(CLAIM_LEVER_TYPE);

			// lectern
//			CLAIM_LECTERN_TYPE = BlockEntityType.Builder
//					.of(ClaimLecternBlockEntity::new, ProtectItBlocks.CLAIM_LECTERN)
//					.build(null);
//			CLAIM_LECTERN_TYPE.setRegistryName("claim_lectern_te");
//			event.getRegistry().register(CLAIM_LECTERN_TYPE);


			//			TEST_TYPE = BlockEntityType.Builder
			//					.of(TestTE::new, ProtectItBlocks.CLAIM_LECTERN)
			//					.build(null);
			//			TEST_TYPE.setRegistryName("test_te");
			//			event.getRegistry().register(TEST_TYPE);
//		}
//	}
}