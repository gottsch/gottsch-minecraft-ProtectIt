
package com.someguyssoftware.protectit.util;

import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * 
 * @author Mark Gottschling on Nov 13, 2022
 *
 */
public class LangUtil {
	public static final String NEWLINE = "";
	public static final String INDENT2 = "  ";
	public static final String INDENT4 = "    ";
	
	/**
	 * 
	 * @param tooltip
	 * @param consumer
	 */
	public static void appendAdvancedHoverText(String modid, List<Component> tooltip, Consumer<List<Component>> consumer) {
		if (!Screen.hasShiftDown()) {
			tooltip.add(new TextComponent(NEWLINE));
			// TODO how do make this call to tooltip generic for any mod because it would require the modid
			tooltip.add(new TranslatableComponent(tooltip(modid, "hold_shift")).withStyle(ChatFormatting.GRAY));
			tooltip.add(new TextComponent(LangUtil.NEWLINE));
		}
		else {
			consumer.accept(tooltip);
		}
	}

    public static String name(String modid, String prefix, String suffix) {
    	return StringUtils.stripEnd(prefix.trim(), ".")
    			+ "."
    			+ modid
    			+ "."
    			+ StringUtils.stripStart(suffix.trim(), ".");
    }
    
    public static String item(String modid, String suffix) {
    	return name(modid, "item", suffix);
    }
    
    public static String tooltip(String modid, String suffix) {
    	return name(modid, "tooltip", suffix);
    }
    
    public static String screen(String modid, String suffix) {
    	return name(modid, "screen", suffix);
    }

	public static String chat(String modid, String suffix) {
		return name(modid, "chat", suffix);
	}
	
	/**
	 * TODO this is ProtectIt's extended methods
	 */
	public static void appendAdvancedHoverText(List<Component> tooltip, Consumer<List<Component>> consumer) {
		LangUtil.appendAdvancedHoverText(ProtectIt.MODID, tooltip, consumer);
	}
	
    public static String name(String prefix, String suffix) {
    	return name(ProtectIt.MODID, prefix, suffix);
    }
    
    /**
     * 
     * @param suffix
     * @return
     */
    public static String item(String suffix) {
    	return name(ProtectIt.MODID, "item", suffix);
    }
    
    public static String tooltip(String suffix) {
    	return name(ProtectIt.MODID, "tooltip", suffix);
    }
    
    public static String screen(String suffix) {
    	return name(ProtectIt.MODID, "screen", suffix);
    }

	public static String chat(String suffix) {
		return name(ProtectIt.MODID, "chat", suffix);
	}
	
	public static String message(String suffix) {
		return name(ProtectIt.MODID, "message", suffix);
	}
}
