package com.someguyssoftware.protectit.config;

import com.someguyssoftware.gottschcore.config.AbstractConfig;
import com.someguyssoftware.gottschcore.mod.IMod;
import com.someguyssoftware.protectit.ProtectIt;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Reloading;
import net.minecraftforge.fml.loading.FMLPaths;

@EventBusSubscriber(modid = ProtectIt.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config extends AbstractConfig {
	public static final String GENERAL_CATEGORY = "03-general";
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
	
	static {
		MOD = new Mod(COMMON_BUILDER);
		LOGGING = new Logging(COMMON_BUILDER);
				
		GENERAL = new General(SERVER_BUILDER);
		
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
		General(final ForgeConfigSpec.Builder builder) {
			builder.comment(CATEGORY_DIV, " General properties for Protect It  mod.", CATEGORY_DIV).push(GENERAL_CATEGORY);
		}
		
		public void init() {
			
		}
	}
	
	@SubscribeEvent
	public static void onLoad(final ModConfig.Loading configEvent) {
		Config.loadConfig(Config.COMMON_CONFIG,
				FMLPaths.CONFIGDIR.get().resolve(mod.getId() + "-common.toml"));
		Config.loadConfig(Config.SERVER_CONFIG,
				FMLPaths.CONFIGDIR.get().resolve(mod.getId() + "-server.toml"));
	}

	@SubscribeEvent
	public static void onReload(final Reloading configEvent) {
	}

	@Override
	public boolean isEnableVersionChecker() {
		return Config.MOD.enableVersionChecker.get();
	}

	@Override
	public void setEnableVersionChecker(boolean enableVersionChecker) {
		Config.MOD.enableVersionChecker.set(enableVersionChecker);
	}

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
	public String getLoggingLevel() {
		return Config.LOGGING.level.get();
	}
}
