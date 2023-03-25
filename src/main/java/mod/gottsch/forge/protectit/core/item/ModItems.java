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

import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.block.ModBlocks;
import mod.gottsch.forge.protectit.core.setup.Registration;
import net.minecraft.world.item.CreativeModeTab;
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
public class ModItems {
	
	// item properties
	public static final Supplier<Item.Properties> ITEM_PROPS_SUPPLIER = () -> new Item.Properties().tab(CreativeModeTab.TAB_MISC);

	// items
	public static RegistryObject<Item> SMALL_CLAIM = fromClaimBlock(ModBlocks.SMALL_CLAIM, ITEM_PROPS_SUPPLIER);
	public static RegistryObject<Item> MEDIUM_CLAIM = fromClaimBlock(ModBlocks.MEDIUM_CLAIM, ITEM_PROPS_SUPPLIER);
	public static RegistryObject<Item> LARGE_CLAIM = fromClaimBlock(ModBlocks.LARGE_CLAIM, ITEM_PROPS_SUPPLIER);
	public static RegistryObject<Item> CUSTOM_CLAIM = fromCustomClaimBlock(ModBlocks.CUSTOM_CLAIM, ITEM_PROPS_SUPPLIER);
	public static RegistryObject<Item> REMOVE_CLAIM = Registration.ITEMS.register("remove_claim", () -> new RemoveClaimBlockItem(ModBlocks.REMOVE_CLAIM.get(), ITEM_PROPS_SUPPLIER.get()));
	public static RegistryObject<Item> PROPERTY_LEVER = Registration.ITEMS.register("property_lever", () -> new PropertyLeverBlockItem(ModBlocks.PROPERTY_LEVER.get(), ITEM_PROPS_SUPPLIER.get()));

	public static RegistryObject<Item> UNCLAIMED_STAKE = Registration.ITEMS.register("unclaimed_stake", () -> new UnclaimedStakeBlockItem(ModBlocks.UNCLAIMED_STAKE.get(), ITEM_PROPS_SUPPLIER.get()));
	public static RegistryObject<Item> PROPERTY_DEED = Registration.ITEMS.register("property_deed", () -> new Deed(ITEM_PROPS_SUPPLIER.get()));
	public static RegistryObject<Item> FIEFDOM_DEED = Registration.ITEMS.register("fiefdom_deed", () -> new FiefdomDeed(ITEM_PROPS_SUPPLIER.get()));
	public static RegistryObject<Item> FIEF_DEED = Registration.ITEMS.register("fief_deed", () -> new FiefDeed(ITEM_PROPS_SUPPLIER.get()));
	

	/**
	 * 
	 */
	public static void register() {
		// cycle through all block and create items
		Registration.registerItems();
	}
		
	// convenience method: take a RegistryObject<Block> and make a corresponding RegistryObject<Item> from it
	public static <B extends Block> RegistryObject<Item> fromClaimBlock(RegistryObject<B> block, Supplier<Item.Properties> itemProperties) {
		return Registration.ITEMS.register(block.getId().getPath(), () -> new ClaimBlockItem(block.get(), itemProperties.get()));
	}
	
	public static <B extends Block> RegistryObject<Item> fromCustomClaimBlock(RegistryObject<B> block, Supplier<Item.Properties> itemProperties) {
		return Registration.ITEMS.register(block.getId().getPath(), () -> new CustomClaimBlockItem(block.get(), itemProperties.get()));
	}
}
