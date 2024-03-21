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
package mod.gottsch.forge.protectit.core.item;

import java.util.function.Supplier;

import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.block.ProtectItBlocks;
import mod.gottsch.forge.protectit.core.setup.Registration;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * @author Mark Gottschling on Oct 15, 2021
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ProtectItItems {

	// items
	public static RegistryObject<Item> PERSONAL_DEED = Registration.ITEMS.register("personal_deed", () -> new PersonalDeed(new Item.Properties()));

	public static RegistryObject<Item> FOUNDATION_STONE_ITEM = Registration.ITEMS.register("foundation_stone", () -> new FoundationStoneItem(ProtectItBlocks.FOUNDATION_STONE.get(), new Item.Properties()));

	/**
	 * 
	 */
	public static void register() {
		// cycle through all block and create items
		Registration.registerItems();
	}

	public static <B extends Block> RegistryObject<Item> fromBlock(RegistryObject<B> block, Item.Properties itemProperties) {
		return Registration.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), itemProperties));
	}
}
