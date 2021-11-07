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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
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
import net.minecraftforge.fml.network.PacketDistributor;

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
	protected static final String VERSION = "2.0.0";

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
	private void sendProtectedMessage(IWorld world, PlayerEntity player) {
		if (!world.isClientSide()) {
			player.sendMessage((new TranslationTextComponent("message.protectit.block_protected").withStyle(new TextFormatting[]{TextFormatting.GRAY, TextFormatting.ITALIC})), player.getUUID());
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onWorldLoad(WorldEvent.Load event) {
		if (!event.getWorld().isClientSide()) {
			World world = (World) event.getWorld();
			ProtectIt.LOGGER.debug("In world load event for dimension {}", WorldInfo.getDimension(world).toString());
			if (WorldInfo.isSurfaceWorld(world)) {
				LOGGER.debug("loading Protect It data...");
				ProtectItSavedData.get(world);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onWorldLoad(PlayerLoggedInEvent event) {
		ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
		if(player.getServer().isDedicatedServer()) {
			// TODO will need two different message types now - block & pvp
			RegistryLoadMessageToClient message = new RegistryLoadMessageToClient(event.getPlayer().getStringUUID(), ProtectionRegistries.block().list());
			ProtectItNetworking.simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), message);
		}
	}

	@SubscribeEvent
	public void onBlockBreak(final BlockEvent.BreakEvent event) {
		// prevent protected blocks from breaking
		if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID())) {
			event.setCanceled(true);
			sendProtectedMessage(event.getWorld(), event.getPlayer());
		}
	}

	@SubscribeEvent
	public void onBlockPlace(final EntityPlaceEvent event) {
		// prevent protected blocks from breaking
		if (event.getEntity() instanceof PlayerEntity) {
			if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getStringUUID())) {
				event.setCanceled(true);
				sendProtectedMessage(event.getWorld(), (PlayerEntity) event.getEntity());
			}
		}
		else if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onMutliBlockPlace(final EntityMultiPlaceEvent event) {
		// prevent protected blocks from breaking
		if (event.getEntity() instanceof PlayerEntity) {
			if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getStringUUID())) {
				event.setCanceled(true);
				sendProtectedMessage(event.getWorld(), (PlayerEntity) event.getEntity());
			}
		}
		else if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}		
	}

	@SubscribeEvent
	public void onToolInteract(final BlockToolInteractEvent event) {
		// prevent protected blocks from breaking
		if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID())) {
			event.setCanceled(true);
			sendProtectedMessage(event.getWorld(), event.getPlayer());
		}
	}

	@SubscribeEvent
	public void onPlayerInteract(final PlayerInteractEvent.RightClickBlock event) {
		// ensure to check entity, because mobs like Enderman can pickup/place blocks
		if (event.getEntity() instanceof PlayerEntity) {
			// get the item in the player's hand
			if (event.getHand() == Hand.MAIN_HAND && event.getItemStack().getItem() instanceof BlockItem) {
				if (ProtectionRegistries.block().isProtectedAgainst(new Coords(event.getPos()), event.getPlayer().getStringUUID())) {
					event.setCanceled(true);
					sendProtectedMessage(event.getWorld(), (PlayerEntity) event.getEntity());
				}
			}
		}
		else if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onLivingDestroyBlock(final LivingDestroyBlockEvent event) {
		// prevent protected blocks from breaking from mob action
		//		if (event.getEntity() instanceof PlayerEntity) {
		//			if (ProtectionRegistries.getRegistry().isProtectedAgainst(new Coords(event.getPos()), event.getEntity().getStringUUID())) {
		//				event.setCanceled(true);
		//				sendProtectedMessage((PlayerEntity) event.getEntity());
		//			}	
		//		}
		//		else
		if (ProtectionRegistries.block().isProtected(new Coords(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onPiston(final PistonEvent.Pre event) {
		if (event.getDirection() == Direction.UP || event.getDirection() == Direction.DOWN) {
			return;
		}
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
			return ProtectionRegistries.block().isProtected(new Coords(block.getX(), block.getY(), block.getZ()));
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
