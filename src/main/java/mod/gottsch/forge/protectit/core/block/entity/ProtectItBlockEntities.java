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

import mod.gottsch.forge.protectit.core.block.ProtectItBlocks;
import mod.gottsch.forge.protectit.core.setup.Registration;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
public class ProtectItBlockEntities {

	public static final RegistryObject<BlockEntityType<FoundationStoneBlockEntity>> FOUNDATION_STONE;

	static {
		FOUNDATION_STONE = Registration.BLOCK_ENTITIES.register("foundation_stone_be", () -> BlockEntityType.Builder.of(FoundationStoneBlockEntity::new,
				ProtectItBlocks.FOUNDATION_STONE.get()
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
