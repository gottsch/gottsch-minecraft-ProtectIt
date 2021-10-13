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
package com.someguyssoftware.protectit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;
import com.someguyssoftware.protectit.registry.ProtectionRegistry;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.BlockToolInteractEvent;
import net.minecraftforge.event.world.BlockEvent.EntityMultiPlaceEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.event.world.PistonEvent.PistonMoveType;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 
 * @author Mark Gottschling on Sep 15, 2021
 *
 */
@Mod(value = ProtectIt.MODID)
public class ProtectIt {
	// logger
	public static Logger LOGGER = LogManager.getLogger(ProtectIt.NAME);

	// constants
	public static final String MODID = "protectit";
	public static final String NAME = "Protect It";
	protected static final String VERSION = "1.1.0";

	public static ProtectIt instance;

	/**
	 * 
	 */
	public ProtectIt() {
		ProtectIt.instance = this;

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
	}

	/**
	 * 
	 * @param player
	 */
	private void sendProtectedMessage(PlayerEntity player) {
		player.sendMessage((new TranslationTextComponent("message.protectit.block_protected").withStyle(new TextFormatting[]{TextFormatting.GRAY, TextFormatting.ITALIC})), player.getUUID());
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onWorldLoad(WorldEvent.Load event) {
		/*
		 * On load of dimension 0 (overworld), initialize the loot table's context and other static loot tables
		 */
		if (!event.getWorld().isClientSide()) {
			LOGGER.info("loading Protect It data...");
			ServerWorld world = (ServerWorld) event.getWorld();
			ProtectItSavedData.get(world);
		}
	}

	@SubscribeEvent
	public void onBlockBreak(final BlockEvent.BreakEvent event) {
		// prevent protected blocks from breaking
		if (ProtectionRegistries.getRegistry().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID())) {
			event.setCanceled(true);
			sendProtectedMessage(event.getPlayer());
		}
	}

	@SubscribeEvent
	public void onBlockPlace(final EntityPlaceEvent event) {
		// prevent protected blocks from breaking
		if (event.getEntity() instanceof PlayerEntity) {
			if (ProtectionRegistries.getRegistry().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getStringUUID())) {
				event.setCanceled(true);
				sendProtectedMessage((PlayerEntity) event.getEntity());
			}
		}
		else if (ProtectionRegistries.getRegistry().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onMutliBlockPlace(final EntityMultiPlaceEvent event) {
		// prevent protected blocks from breaking
		if (event.getEntity() instanceof PlayerEntity) {
			if (ProtectionRegistries.getRegistry().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getStringUUID())) {
				event.setCanceled(true);
				sendProtectedMessage((PlayerEntity) event.getEntity());
			}
		}
		else if (ProtectionRegistries.getRegistry().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}		
	}

	@SubscribeEvent
	public void onToolInteract(final BlockToolInteractEvent event) {
		// prevent protected blocks from breaking
		if (ProtectionRegistries.getRegistry().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID())) {
			event.setCanceled(true);
			sendProtectedMessage(event.getPlayer());
		}
	}

	@SubscribeEvent
	public void onPlayerInteract(final PlayerInteractEvent.RightClickBlock event) {
		ProtectIt.LOGGER.info("player interact event");
		// ensure to check entity, because mobs like Enderman can pickup/place blocks
		if (event.getEntity() instanceof PlayerEntity) {
			// get the item in the player's hand
			if (event.getHand() == Hand.MAIN_HAND && event.getItemStack().getItem() instanceof BlockItem) {
				ProtectIt.LOGGER.info("held item is a block!");
				if (ProtectionRegistries.getRegistry().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID())) {
					event.setCanceled(true);
					sendProtectedMessage((PlayerEntity) event.getEntity());
				}
			}
		}
		else if (ProtectionRegistries.getRegistry().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public void onLivingDestroyBlock(final LivingDestroyBlockEvent event) {
		// prevent protected blocks from breaking
		if (event.getEntity() instanceof PlayerEntity) {
			if (ProtectionRegistries.getRegistry().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getStringUUID())) {
				event.setCanceled(true);
				sendProtectedMessage((PlayerEntity) event.getEntity());
			}
			else if (ProtectionRegistries.getRegistry().isProtected(new Coords(event.getPos()))) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onPiston(final PistonEvent.Pre event) {
		if (event.getDirection() == Direction.UP || event.getDirection() == Direction.DOWN) {
			return;
		}
		// check if piston itself is inside protected area - if so, exit ie. allow movement
		if (ProtectionRegistries.getRegistry().isProtected(new Coords(event.getPos()))) {
			return;
		}

		if (event.getPistonMoveType() == PistonMoveType.EXTEND) {
			for (int count = 1; count <=12; count++) {
				int xOffset = 0;
				int zOffset = 0;
				int xPush = 0;				
				int zPush = 0;
				switch(event.getDirection()) {
				default:
				case NORTH:
					zOffset = -count;
					zPush = -1;
					break;
				case SOUTH:
					zOffset = count;
					zPush = +1;
					break;
				case WEST:
					xOffset = -count;
					xPush = -1;
					break;
				case EAST:
					xOffset = count;
					xPush = 1;
					break;
				}

				if (event.getWorld().getBlockState(event.getPos().offset(xOffset, 0, zOffset)).getMaterial().isSolid()) {
					// prevent protected blocks from breaking
					if (ProtectionRegistries.getRegistry().isProtected(new Coords(event.getPos().offset(xOffset, 0, zOffset))) || 
							ProtectionRegistries.getRegistry().isProtected(new Coords(event.getPos().offset(xOffset + xPush, 0, zOffset + zPush)))) {
						event.setCanceled(true);
						return;
					}
				}
				else {
					return;
				}
			}
		}
	}

	@SubscribeEvent
	public void onExplosion(final ExplosionEvent.Detonate event) {
		// remove any affected blocks that are protected
		event.getAffectedBlocks().removeIf(block -> {
			// prevent protected blocks from breaking
			return ProtectionRegistries.getRegistry().isProtected(new Coords(block.getX(), block.getY(), block.getZ()));
		});
	}

}
