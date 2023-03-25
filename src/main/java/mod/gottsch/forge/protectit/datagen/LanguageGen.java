package mod.gottsch.forge.protectit.datagen;

import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.item.ModItems;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * 
 * @author Mark Gottschling Feb 28, 2023
 *
 */
public class LanguageGen extends LanguageProvider {

    public LanguageGen(DataGenerator gen, String locale) {
        super(gen, ProtectIt.MODID, locale);
    }
    
    @Override
    protected void addTranslations() {
        // items
        add(ModItems.FIEFDOM_DEED.get(), "Subdivide License");
   
        /*
         *  Util.tooltips
         */
        // general
        add(LangUtil.tooltip("hold_shift"), "Hold [SHIFT] to expand");

    }
}
