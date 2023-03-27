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

import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.zone.ZonePermission;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 
 * @author Mark Gottschling Mar 21, 2023
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

	@SubscribeEvent
	public static void onPlayerHurt(LivingHurtEvent event) {

		
		if (event.getEntity() instanceof Player) {
			Player player = (ServerPlayer) event.getEntity();
			
			// mob on player hurt
			if (event.getSource().getEntity() instanceof Mob) {
				//			if (!Config.PROTECTION.enableMobPvpEvent.get()) {
				//				return;
				//			}

				// prevent mob from hurting player
				if (ProtectionRegistries.pvp().isProtectedAgainst(new Coords(player.blockPosition()), ZonePermission.MOB_PVP_PERMISSION.value)) {
					event.setCanceled(true);
//					ProtectIt.LOGGER.debug("denied mob attack -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(player.blockPosition()).toShortString());
				}
			}
			// player on player hurt
			else if (event.getSource().getEntity() instanceof Player) {
				// prevent player from hurting player
				if (ProtectionRegistries.pvp().isProtectedAgainst(new Coords(player.blockPosition()), ZonePermission.PLAYER_PVP_PERMISSION.value)) {
					event.setCanceled(true);
//					ProtectIt.LOGGER.debug("denied player attack -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(player.blockPosition()).toShortString());
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerHurt(LivingSpawnEvent.CheckSpawn event) {
		if (ProtectionRegistries.pvp().isProtectedAgainst(new Coords(event.getEntity().blockPosition()), ZonePermission.MOB_SPAWN_PERMISSION.value)) {
			event.setResult(Result.DENY);
			ProtectIt.LOGGER.debug("denied mob spawn -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(event.getEntity().blockPosition()).toShortString());
		}
	}
	
	private void sendProtectedMessage(LevelAccessor world, Player player) {
		if (world.isClientSide() && Config.GUI.enableProtectionMessage.get()) {
			player.sendSystemMessage((Component.translatable("message.protectit.mob_pvp_protected").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})));
		}
	}
}
