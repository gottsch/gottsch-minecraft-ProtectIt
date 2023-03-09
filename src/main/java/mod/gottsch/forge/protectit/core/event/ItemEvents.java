/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.protectit.core.event;

import java.util.UUID;

import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.item.Deed;
import mod.gottsch.forge.protectit.core.item.PropertyLease;
import mod.gottsch.forge.protectit.core.registry.TransactionRegistry;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 
 * @author Mark Gottschling Mar 6, 2023
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.FORGE)
public class ItemEvents {

	@SubscribeEvent
	public static void onItemLeavesLevel(EntityLeaveLevelEvent event) {
		if (event.getLevel().isClientSide()) {
			return;
		}
		
		if (event.getEntity() instanceof ItemEntity) {
			ItemStack itemStack = ((ItemEntity)event.getEntity()).getItem();
			
			if (itemStack.getItem() instanceof Deed) {
				if (event.getEntity().isRemoved()) {
					if (itemStack.hasTag()) {
						UUID uuid = itemStack.getTag().getUUID(Deed.PROPERTY_ID_KEY);
						TransactionRegistry.buyDeed(uuid);
					}
				}
			} else if (itemStack.getItem() instanceof PropertyLease) {
				if (event.getEntity().isRemoved()) {
					if (itemStack.hasTag()) {
						UUID uuid = itemStack.getTag().getUUID(PropertyLease.PROPERTY_ID_KEY);
						TransactionRegistry.buyLease(uuid);
					}
				}				
			}
		}
		
	}
}
