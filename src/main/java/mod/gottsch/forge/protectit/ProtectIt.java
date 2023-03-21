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
package mod.gottsch.forge.protectit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.core.block.ModBlocks;
import mod.gottsch.forge.protectit.core.block.entity.ModBlockEntities;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.item.ModItems;
import mod.gottsch.forge.protectit.core.network.ModNetworking;
import mod.gottsch.forge.protectit.core.network.RegistryLoadMessageToClient;
import mod.gottsch.forge.protectit.core.persistence.ProtectItSavedData;
import mod.gottsch.forge.protectit.core.property.Permission;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.setup.CommonSetup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.BlockEvent.BlockToolModificationEvent;
import net.minecraftforge.event.level.BlockEvent.EntityMultiPlaceEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.level.PistonEvent.PistonMoveType;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Sep 15, 2021
 *
 */
@Mod(value = ProtectIt.MODID)
public class ProtectIt {
	// logger
	public static Logger LOGGER = LogManager.getLogger(ProtectIt.MODID);

	// constants
	public static final String MODID = "protectit";
	public static final String NAME = "Protect It";

	public static ProtectIt instance;

	/**
	 * 
	 */
	public ProtectIt() {
		ProtectIt.instance = this;

		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);

		// Register the setup method for modloading
		IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

		// register the deferred registries
		ModBlocks.register();
		ModItems.register();
		ModBlockEntities.register();
		
		// regular register
		eventBus.addListener(CommonSetup::common);
	}

	/**
	 * 
	 * @param world 
	 * @param player
	 */
	private void sendProtectedMessage(LevelAccessor world, Player player) {
		if (world.isClientSide() && Config.GUI.enableProtectionMessage.get()) {
			player.sendSystemMessage((Component.translatable("message.protectit.block_protected").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})));
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onWorldLoad(LevelEvent.Load event) {
		if (!event.getLevel().isClientSide()) {
			Level world = (Level) event.getLevel();
			ProtectIt.LOGGER.debug("In world load event for dimension {}", WorldInfo.getDimension(world).toString());
			if (WorldInfo.isSurfaceWorld(world)) {
				LOGGER.debug("loading Protect It data...");
				ProtectionRegistries.block().clear();
				ProtectItSavedData.get(world);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		ServerPlayer player = (ServerPlayer) event.getEntity();
		if(player.getServer().isDedicatedServer()) {
			ProtectIt.LOGGER.debug("player logged in -> {}, sending registry data...", player.getDisplayName().getString());
			// TODO will need two different message types now - block & pvp
			//RegistryLoadMessageToClient message = new RegistryLoadMessageToClient(event.getPlayer().getStringUUID(), ProtectionRegistries.block().list());
			RegistryLoadMessageToClient message = new RegistryLoadMessageToClient(event.getEntity().getStringUUID(), ProtectionRegistries.block().getAll());
			ProtectIt.LOGGER.debug("player logged in, sending all property data -> {}", ProtectionRegistries.block().getAll());
			ModNetworking.channel.send(PacketDistributor.PLAYER.with(() -> player), message);
		}
	}

	// permission events
	@SubscribeEvent
	public void onBlockBreak(final BlockEvent.BreakEvent event) {
		LOGGER.debug("attempt to break block by player -> {} @ {}", event.getPlayer().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
		// prevent protected blocks from breaking
		if (!Config.PROTECTION.enableBlockBreakEvent.get()
				|| event.getPlayer().hasPermissions(Config.GENERAL.opsPermissionLevel.get())) {
			return;
		}
		
//		LOGGER.debug("block break is protectedagainst -> {}", ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID()));
		if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getUUID(), Permission.BLOCK_BREAK_PERMISSION.value)) {
			event.setCanceled(true);
			LOGGER.debug("denied breakage -> {} @ {}", event.getPlayer().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
			if (!event.getLevel().isClientSide()) {
				sendProtectedMessage(event.getLevel(), event.getPlayer());
			}
		}
	}

	@SubscribeEvent
	public void onBlockPlace(final EntityPlaceEvent event) {
		if (!Config.PROTECTION.enableEntityPlaceEvent.get()
				|| event.getEntity().hasPermissions(Config.GENERAL.opsPermissionLevel.get()) ) {
			return;
		}

		// prevent protected blocks from placing
		if (event.getEntity() instanceof Player) {
			if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getUUID(), Permission.BLOCK_PLACE_PERMISSION.value)) {
				event.setCanceled(true);
//				LOGGER.debug("denied block place -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
				if (!event.getLevel().isClientSide()) {
					sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
				}
			}
		}
		else if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onMutliBlockPlace(final EntityMultiPlaceEvent event) {
		if (!Config.PROTECTION.enableEntityMultiPlaceEvent.get()
				|| event.getEntity().hasPermissions(Config.GENERAL.opsPermissionLevel.get()) ) {
			return;
		}

		// prevent protected blocks from breaking
		if (event.getEntity() instanceof Player) {
			if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getUUID(), Permission.MULTIBLOCK_PLACE_PERMISSION.value)) {
				event.setCanceled(true);
				if (!event.getLevel().isClientSide()) {
//					LOGGER.debug("denied multi-block place -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
					sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
				}
			}
		}
		else if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}		
	}

	@SubscribeEvent
	public void onToolInteract(final BlockToolModificationEvent event) {
		if (!Config.PROTECTION.enableBlockToolInteractEvent.get()
				|| event.getPlayer().hasPermissions(Config.GENERAL.opsPermissionLevel.get())) {
			return;
		}

		if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getUUID(), Permission.TOOL_PERMISSION.value)) {
			event.setCanceled(true);
			if (!event.getLevel().isClientSide()) {
				sendProtectedMessage(event.getLevel(), event.getPlayer());
			}
		}
	}

	@SubscribeEvent
	public void onPlayerInteract(final PlayerInteractEvent.RightClickBlock event) {
		if (!Config.PROTECTION.enableRightClickBlockEvent.get()
				|| event.getEntity().hasPermissions(Config.GENERAL.opsPermissionLevel.get())) {
			return;
		}

		// ensure to check entity, because mobs like Enderman can pickup/place blocks
		if (event.getEntity() instanceof Player) {

			// get the item in the player's hand
			if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getUUID(), Permission.INTERACT_PERMISSION.value)) {

				Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
				if ((block instanceof DoorBlock || block instanceof TrapDoorBlock) && 
						!ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getUUID(), Permission.DOOR_INTERACT_PERMISSION.value)) {
					return;
				}
				if ((block instanceof ChestBlock) && 
						!ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getUUID(), Permission.INVENTORY_INTERACT_PERMISSION.value)) {
					return;
				}
				
				event.setCanceled(true);
//				LOGGER.debug("denied right click -> {} @ {} w/ hand -> {}", event.getPlayer().getDisplayName().getString(), new Coords(event.getPos()).toShortString(), event.getHand().toString());
				// reduces to only 1 message per action
				if (event.getHand() == InteractionHand.MAIN_HAND) { 
					if (!event.getLevel().isClientSide()) {
						sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
					}
				}
			}
		}
		else if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onLivingDestroyBlock(final LivingDestroyBlockEvent event) {
		// prevent protected blocks from breaking by mob action
		if (Config.PROTECTION.enableLivingDestroyBlockEvent.get()
				&& ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onPiston(final PistonEvent.Pre event) {
		if (!Config.PROTECTION.enablePistionEvent.get()) {
			return;
		}

		if (event.getDirection() == Direction.UP || event.getDirection() == Direction.DOWN) {
			return;
		}
		// TODO this needs to change to be, just continue, so that the movement of blocks 
		// can be checked against an adjacent property.
		
		// check if piston itself is inside protected area - if so, exit ie. allow movement
		if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
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

				if (event.getLevel().getBlockState(event.getPos().offset(xOffset, 0, zOffset)).getMaterial().isSolid()) {
					// prevent protected blocks from breaking
					if (ProtectionRegistries.block().isProtected(new Coords(event.getPos().offset(xOffset, 0, zOffset))) || 
							ProtectionRegistries.block().isProtected(new Coords(event.getPos().offset(xOffset + xPush, 0, zOffset + zPush)))) {
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
			return Config.PROTECTION.enableExplosionDetonateEvent.get()
					&& ProtectionRegistries.block().isProtected(new Coords(block.getX(), block.getY(), block.getZ()));
		});
	}
}
