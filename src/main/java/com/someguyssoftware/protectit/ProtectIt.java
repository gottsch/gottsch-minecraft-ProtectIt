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

import com.someguyssoftware.gottschcore.annotation.Credits;
import com.someguyssoftware.gottschcore.annotation.ModInfo;
import com.someguyssoftware.gottschcore.config.IConfig;
import com.someguyssoftware.gottschcore.mod.IMod;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.protectit.config.Config;
import com.someguyssoftware.protectit.init.ProtectItSetup;
import com.someguyssoftware.protectit.network.ProtectItNetworking;
import com.someguyssoftware.protectit.network.RegistryLoadMessageToClient;
import com.someguyssoftware.protectit.persistence.ProtectItSavedData;
import com.someguyssoftware.protectit.registry.ProtectionRegistries;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

/**
 * 
 * @author Mark Gottschling on Sep 15, 2021
 *
 */
@Mod(value = ProtectIt.MODID)
@ModInfo(
		modid =  ProtectIt.MODID, 
		name =  ProtectIt.NAME, 
		version =  ProtectIt.VERSION, 
		minecraftVersion = "1.16.5", 
		forgeVersion = "36.2.0",
		updateJsonUrl = "")
@Credits(values = { "ProtectIt was first developed by Mark Gottschling on Sep 15, 2021."})
public class ProtectIt implements IMod {
	// logger
	public static Logger LOGGER = LogManager.getLogger(ProtectIt.NAME);

	// constants
	public static final String MODID = "protectit";
	public static final String NAME = "Protect It";
	protected static final String VERSION = "2.1.0";

	public static ProtectIt instance;
	private static Config config;

	/**
	 * 
	 */
	public ProtectIt() {
		ProtectIt.instance = this;
		ProtectIt.config = new Config(this);

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);

		Config.loadConfig(Config.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve("protectit-common.toml"));
		Config.loadConfig(Config.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve("protectit-server.toml"));

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);

		// Register the setup method for modloading
		IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

		// regular register
		eventBus.addListener(ProtectItSetup::common);
		eventBus.addListener(ProtectItNetworking::common);
	}

	/**
	 * 
	 * @param world 
	 * @param player
	 */
	private void sendProtectedMessage(LevelAccessor world, Player player) {
		if (!world.isClientSide()) {
			player.sendMessage((new TextComponent("message.protectit.block_protected").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})), player.getUUID());
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onWorldLoad(WorldEvent.Load event) {
		if (!event.getWorld().isClientSide()) {
			Level world = (Level)event.getWorld();
			ProtectIt.LOGGER.debug("In world load event for dimension {}", WorldInfo.getDimension(world).toString());
			if (WorldInfo.isSurfaceWorld(world)) {
				LOGGER.debug("loading Protect It data...");
				ProtectionRegistries.block().clear();
				ProtectItSavedData.get(world);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onWorldLoad(PlayerLoggedInEvent event) {
		ServerPlayer player = (ServerPlayer) event.getPlayer();
		if(player.getServer().isDedicatedServer()) {
			LOGGER.debug("player logged in -> {}, sending registry data...", player.getDisplayName().getString());
			// TODO will need two different message types now - block & pvp
			//RegistryLoadMessageToClient message = new RegistryLoadMessageToClient(event.getPlayer().getStringUUID(), ProtectionRegistries.block().list());
			RegistryLoadMessageToClient message = new RegistryLoadMessageToClient(event.getPlayer().getStringUUID(), ProtectionRegistries.block().getAll());
			ProtectIt.LOGGER.debug("player logged in, sending all claim data -> {}", ProtectionRegistries.block().getAll());
			ProtectItNetworking.simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), message);
		}
	}

	@SubscribeEvent
	public void onBlockBreak(final BlockEvent.BreakEvent event) {
		LOGGER.debug("attempt to break block by player -> {} @ {}", event.getPlayer().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
		// prevent protected blocks from breaking
		if (Config.PROTECTION.enableBlockBreakEvent.get()
				&& ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID())) {
			LOGGER.debug("denied breakage -> {} @ {}", event.getPlayer().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
			event.setCanceled(true);
			sendProtectedMessage(event.getWorld(), event.getPlayer());
		}
	}

	@SubscribeEvent
	public void onBlockPlace(final EntityPlaceEvent event) {
		if (!Config.PROTECTION.enableEntityPlaceEvent.get()) {
			return;
		}
		
		// prevent protected blocks from placing
		if (event.getEntity() instanceof Player) {
			if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getStringUUID())) {
				event.setCanceled(true);
				sendProtectedMessage(event.getWorld(), (Player) event.getEntity());
			}
		}
		else if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onMutliBlockPlace(final EntityMultiPlaceEvent event) {
		if (!Config.PROTECTION.enableEntityMultiPlaceEvent.get()) {
			return;
		}
		
		// prevent protected blocks from breaking
		if (event.getEntity() instanceof Player) {
			if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getStringUUID())) {
				event.setCanceled(true);
				sendProtectedMessage(event.getWorld(), (Player) event.getEntity());
			}
		}
		else if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}		
	}

	@SubscribeEvent
	public void onToolInteract(final BlockToolInteractEvent event) {
		// prevent protected blocks from breaking
		if (Config.PROTECTION.enableBlockToolInteractEvent.get()
				&& ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID())) {
			event.setCanceled(true);
			sendProtectedMessage(event.getWorld(), event.getPlayer());
		}
	}

	@SubscribeEvent
	public void onPlayerInteract(final PlayerInteractEvent.RightClickBlock event) {
		if (!Config.PROTECTION.enableRightClickBlockEvent.get()) {
			return;
		}
		
		// ensure to check entity, because mobs like Enderman can pickup/place blocks
		if (event.getEntity() instanceof Player) {
			// get the item in the player's hand
			if (event.getHand() == InteractionHand.MAIN_HAND && event.getItemStack().getItem() instanceof BlockItem) {
				if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID())) {
					event.setCanceled(true);
					sendProtectedMessage(event.getWorld(), (Player) event.getEntity());
				}
				// TODO check if Claim Lectern and if one already exists in claim?
			}
		}
		else if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onLivingDestroyBlock(final LivingDestroyBlockEvent event) {
		// prevent protected blocks from breaking from mob action
		//		if (event.getEntity() instanceof Player) {
		//			if (ProtectionRegistries.getRegistry().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getStringUUID())) {
		//				event.setCanceled(true);
		//				sendProtectedMessage((Player) event.getEntity());
		//			}	
		//		}
		//		else
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
		// can be checked against an adjacent claim.
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

				if (event.getWorld().getBlockState(event.getPos().offset(xOffset, 0, zOffset)).getMaterial().isSolid()) {
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

	@Override
	public IMod getInstance() {
		return ProtectIt.instance;
	}

	@Override
	public String getId() {
		return ProtectIt.MODID;
	}

	@Override
	public IConfig getConfig() {
		return ProtectIt.config;
	}

}
