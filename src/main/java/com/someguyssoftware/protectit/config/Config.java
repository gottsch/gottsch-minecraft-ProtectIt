package com.someguyssoftware.protectit.config;

import com.someguyssoftware.gottschcore.config.AbstractConfig;
import com.someguyssoftware.gottschcore.mod.IMod;
import com.someguyssoftware.protectit.ProtectIt;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;

@EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config extends AbstractConfig {
	public static final String GENERAL_CATEGORY = "03-general";
	public static final String PROTECTION_CATEGORY = "04-protection";
	public static final String UNDERLINE_DIV = "------------------------------";

	protected static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
	protected static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
	protected static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
	public static ForgeConfigSpec COMMON_CONFIG;
	public static ForgeConfigSpec SERVER_CONFIG;

	private static IMod mod;

	public static final Mod MOD;
	public static final Logging LOGGING;
	public static final General GENERAL;
	public static final Protection PROTECTION;
	
	static {
		MOD = new Mod(COMMON_BUILDER);
		LOGGING = new Logging(COMMON_BUILDER);
				
		GENERAL = new General(SERVER_BUILDER);
		PROTECTION = new Protection(SERVER_BUILDER);
		
		COMMON_CONFIG = COMMON_BUILDER.build();
		SERVER_CONFIG = SERVER_BUILDER.build();
		
		Config.init();
	}
	
	/**
	 * 
	 * @param mod
	 */
	public Config(IMod mod) {
		Config.mod = mod;
	}
	
	public static void init() {
		Config.GENERAL.init();
	}

	/**
	 * 
	 * @author Mark Gottschling on Nov 3, 2021
	 *
	 */
	public static class General {
		public IntValue giveCommandLevel;
		public IntValue claimsPerPlayer;
		
		General(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, " General properties for Protect It  mod.", CATEGORY_DIV).push(GENERAL_CATEGORY);
			
			giveCommandLevel = builder
					.comment("The access level required for the 'give' command.")
					.defineInRange("'give' command level:", 0, 0, 4);
			claimsPerPlayer = builder
					.comment(" The number of claims each player can place per world.")
					.defineInRange("Number of claims per player:", 5, 1, 100);
			
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
					" Note: these properties are for enabling the protections in the claim, not for enabling the actions.",
					" ex. Block break protection = true, enables the PROTECTION AGAINST breaking blocks in the claim,",
					" it does NOT enable the player TO BREAK a block.",
					CATEGORY_DIV).push(PROTECTION_CATEGORY);
			
			enableBlockBreakEvent = builder
					.comment(" Enables block break protection. If enabled, blocks in claim are protected from being broken by others.")
					.define("Block break proctection:", true);
			
			enableEntityPlaceEvent = builder
					.comment(" Enables block placement protection. If enabled, blocks are not allowed to be placed in the claim by others.")
					.define("Block placement proctection:", true);
			
			enableEntityMultiPlaceEvent = builder
					.comment(" Enables multi-block placement protection. If enabled, multi-blocks are not allowed to be placed in the claim by others.")
					.define("Multi-block placement proctection:", true);

			enableBlockToolInteractEvent = builder					
					.comment(" Enables block tool interaction protection. If enabled, blocks in claim will not change state when right-clicked with tool. Ex. axe will not strip a log.")
					.define("Block tool interact proctection:", true);
			
			enableRightClickBlockEvent = builder					
					.comment(" Enables right click protection. If enabled, blocks in claim will not perform any action if right-clicked. Ex. chests will not open for others.")
					.define("Right-click block proctection:", true);

			enableLivingDestroyBlockEvent = builder
					.comment(" Enables block break protection from living entities. If enabled, blocks in claim are protected from being broken for living entities (mobs).")
					.define("Living destory block proctection:", true);
			
			enablePistionEvent = builder
					.comment(" Enables piston movement protection. If enabled, pistons outside the claim will not fire if their movement would move protected blocks.")
					.define("Piston movement proctection:", true);
			
			enableExplosionDetonateEvent = builder
					.comment(" Enables explosion protection. If enabled, explosions will not destory protected blocks.")
					.define("Explosion proctection:", true);
			
			builder.pop();
		}
	}
	
	@SubscribeEvent
	public static void onLoad(final ModConfigEvent.Loading configEvent) {
		Config.loadConfig(Config.COMMON_CONFIG,
				FMLPaths.CONFIGDIR.get().resolve(mod.getId() + "-common.toml"));
		Config.loadConfig(Config.SERVER_CONFIG,
				FMLPaths.CONFIGDIR.get().resolve(mod.getId() + "-server.toml"));
	}

	@SubscribeEvent
	public static void onReload(final ModConfigEvent.Reloading configEvent) {
	}

//	@Override
//	public boolean isEnableVersionChecker() {
//		return Config.MOD.enableVersionChecker.get();
//	}
//
//	@Override
//	public void setEnableVersionChecker(boolean enableVersionChecker) {
//		Config.MOD.enableVersionChecker.set(enableVersionChecker);
//	}

	@Override
	public boolean isLatestVersionReminder() {
		return Config.MOD.latestVersionReminder.get();
	}

	@Override
	public void setLatestVersionReminder(boolean latestVersionReminder) {
		Config.MOD.latestVersionReminder.set(latestVersionReminder);
	}

	@Override
	public boolean isModEnabled() {
		return Config.MOD.enabled.get();
	}

	@Override
	public void setModEnabled(boolean modEnabled) {
		Config.MOD.enabled.set(modEnabled);
	}

	@Override
	public String getModsFolder() {
		return Config.MOD.folder.get();
	}

	@Override
	public void setModsFolder(String modsFolder) {
		Config.MOD.folder.set(modsFolder);
	}

	@Override
	public String getConfigFolder() {
		return Config.MOD.configFolder.get();
	}

	@Override
	public void setConfigFolder(String configFolder) {
		Config.MOD.configFolder.set(configFolder);
	}
	
	@Override
	public String getLogsFolder() {
		return Config.LOGGING.folder.get();
	}
	
	public void setLogsFolder(String folder) {
		Config.LOGGING.folder.set(folder);
	}
	
	@Override
	public String getLoggerLevel() {
		return Config.LOGGING.level.get();
	}

	@Override
	public void setLoggerLevel(String level) {
		Config.LOGGING.level.set(level);
	}
	
	@Override
	public String getLoggerSize() {
		return Config.LOGGING.size.get();
	}

	@Override
	public void setLoggerSize(String size) {
		Config.LOGGING.size.set(size);
	}
}
