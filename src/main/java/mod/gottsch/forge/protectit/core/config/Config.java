/*
 * This file is part of  Protect It.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.protectit.core.config;

import mod.gottsch.forge.protectit.core.ProtectIt;

import mod.gottsch.forge.gottschcore.config.AbstractConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 
 * @author Mark Gottschling on Nov 28, 2022
 *
 */
@EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config extends AbstractConfig {
	public static final String GENERAL_CATEGORY = "general";
	public static final String PROTECTION_CATEGORY = "protection";
	public static final String UNDERLINE_DIV = "------------------------------";

	protected static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
	protected static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
	protected static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
	public static ForgeConfigSpec COMMON_CONFIG;
	public static ForgeConfigSpec CLIENT_CONFIG;
	public static ForgeConfigSpec SERVER_CONFIG;

	public static final Logging LOGGING;
	public static final Gui GUI;
	public static final General GENERAL;
	public static final Protection PROTECTION;
	
	public static Config instance = new Config();
	
	static {
		LOGGING = new Logging(COMMON_BUILDER);
		GUI = new Gui(CLIENT_BUILDER);
		GENERAL = new General(SERVER_BUILDER);
		PROTECTION = new Protection(SERVER_BUILDER);
		
		COMMON_CONFIG = COMMON_BUILDER.build();
		CLIENT_CONFIG = CLIENT_BUILDER.build();
		SERVER_CONFIG = SERVER_BUILDER.build();
		
		Config.init();
	}
		
	public static void init() {
		Config.GENERAL.init();
	}

	/*
	 * 
	 */
	public static class Gui {
		public BooleanValue enableProtectionChatMessages;
		
		public Gui(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, " Client GUI properties for Protect It  mod.", CATEGORY_DIV).push("GUI");
			enableProtectionChatMessages = builder
					.comment(" Enables protection messages in chat. If enabled, when protection is triggered, a message will display in the chat.")
					.define("enableProtectionChatMessages:", false);
			builder.pop();
		}		
	}
	
	/**
	 * 
	 * @author Mark Gottschling on Nov 3, 2021
	 *
	 */
	public static class General {
		public IntValue giveCommandLevel;
		public IntValue parcelsPerPlayer;
		public IntValue opsPermissionLevel;
		public IntValue parcelBufferRadius;
		public IntValue nationParcelBufferRadius;
		
		General(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, " General properties for Protect It  mod.", CATEGORY_DIV).push(GENERAL_CATEGORY);
			
			giveCommandLevel = builder
					.comment("The access level required for the 'give' command.")
					.defineInRange("giveCommandLevel", 2, 0, 4);
			parcelsPerPlayer = builder
					.comment(" The number of properties each player can own per world.")
					.defineInRange("parcelsPerPlayer", 5, 1, 100);
			opsPermissionLevel = builder
					.comment(" The permission level required to be Ops within Protect It.","This is not the op-permission-level that is set in the server.propeties.",
							" This allows players who are not server-level ops, to have Protect It Ops permissions. ie protections don't protect against Ops.",
							" Ex. server-level ops = 4, but Protect It ops = 3 - a player with permission 3 would be considered an Ops within Protect It.")
					.defineInRange("opsPermissionLevel", 4, 0, 4);

			parcelBufferRadius = builder
					.comment(" A buffer between parcels. Another parcel cannot be built within this area.",
							" This is a radius beyond (or in addition to) the parcel border.",
							" Ex. parcel size = 10x10x10, with a buffer radius = 3. The total size = 13x13x13",
							" that another parcel cannot build within.",
							" Note that the buffer is between parcel borders, not other buffers, meaning if 2 parcels",
							" both have a buffer = 3, there is a buffer of 3 between the parcels, not 6.")
							.defineInRange("parcelBufferRadius", 3, 1, 10);

			nationParcelBufferRadius = builder
					.comment(" Like 'parcelBufferRadius', but for Nation parcels.")
							.defineInRange("nationParcelBufferRadius", 10, 1, 50);

			builder.pop();
		}
		
		public void init() {			
		}
	}
	
	public static class Protection {
		public BooleanValue enableBlockBreakEvent;
		public BooleanValue enableEntityPlaceEvent;
		public BooleanValue enableEntityMultiPlaceEvent;
		public BooleanValue enableBlockToolInteractEvent;
		public BooleanValue enableRightClickBlockEvent;
		public BooleanValue enableLivingDestroyBlockEvent;
		public BooleanValue enablePistionEvent;
		public BooleanValue enableExplosionDetonateEvent;
		
		Protection(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, 
					" Protection properties for Protect It mod.",
					" Note: these config settings are for enabling the protections of the property, not for enabling the actions.",
					" ex. Block break protection = true, enables the PROTECTION AGAINST breaking blocks in the property,",
					" it does NOT enable the player TO BREAK a block.",
					CATEGORY_DIV).push(PROTECTION_CATEGORY);
			
			enableBlockBreakEvent = builder
					.comment(" Enables block break protection. If enabled, blocks in property are protected from being broken by others.")
					.define("enableBlockBreakProtection", true);
			
			enableEntityPlaceEvent = builder
					.comment(" Enables block placement protection. If enabled, blocks are not allowed to be placed in the property by others.")
					.define("enableBlockPlacementProtection", true);
			
			enableEntityMultiPlaceEvent = builder
					.comment(" Enables multi-block placement protection. If enabled, multi-blocks are not allowed to be placed in the property by others.")
					.define("enableMultiBlockPlacementProtection", true);

			enableBlockToolInteractEvent = builder					
					.comment(" Enables block tool interaction protection. If enabled, blocks in the property will not change state when right-clicked with tool. Ex. axe will not strip a log.")
					.define("enableBlockToolInteractProtection", true);
			
			enableRightClickBlockEvent = builder					
					.comment(" Enables right click protection. If enabled, blocks in the property will not perform any action if right-clicked. Ex. chests will not open for others.")
					.define("enableRightClickProtection", true);

			enableLivingDestroyBlockEvent = builder
					.comment(" Enables block break protection from living entities. If enabled, blocks in the property  are protected from being broken for living entities (mobs).")
					.define("enableLivingDestroyBlockProtection", true);
			
			enablePistionEvent = builder
					.comment(" Enables piston movement protection. If enabled, pistons outside the property will not fire if their movement would move protected blocks.")
					.define("enablePistonMovementProtection", true);
			
			enableExplosionDetonateEvent = builder
					.comment(" Enables explosion protection. If enabled, explosions will not destory protected blocks.")
					.define("enableExplosionProtection", true);
			
			builder.pop();
		}
	}

	@Override
	public String getLogsFolder() {
		return Config.LOGGING.folder.get();
	}
	
	public void setLogsFolder(String folder) {
		Config.LOGGING.folder.set(folder);
	}
	
	@Override
	public String getLoggingLevel() {
		return Config.LOGGING.level.get();
	}
}
