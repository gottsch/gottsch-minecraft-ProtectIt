
package mod.gottsch.forge.protectit.datagen;

import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.block.ProtectItBlocks;
import mod.gottsch.forge.protectit.core.item.ProtectItItems;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * 
 * @author Mark Gottschling on Mar 19, 2024
 *
 */
public class LanguageGen extends LanguageProvider {

    public LanguageGen(PackOutput gen, String locale) {
        super(gen, ProtectIt.MODID, locale);
    }
    
    @Override
    protected void addTranslations() {
        // deeds
        add(ProtectItItems.PERSONAL_DEED.get(), "Personal Deed");
        add(ProtectItItems.NATION_DEED.get(), "Nation Deed");
        add(ProtectItItems.CITIZEN_DEED.get(), "Citizen Deed");
        // blocks
        add(ProtectItBlocks.FOUNDATION_STONE.get(), "Foundation Stone");

//        add(ProtectItItems.FOUNDATION_STONE_ITEM.get(), "F")
//
//                "message.protectit.block_region.successfully_protected":"Region %s -> %s is now owned and protected.",
//                "message.protectit.block_region.protected":"A block(s) in that region are already owned and protected.",
//                "message.protectit.block_region.not_protected":"That region is not protected.",
//                "message.protectit.block_region.not_owner":"You are not the owner of that property.",
//                "message.protectit.block_region.not_protected_or_owner":"That region is not owned or you are not the owner.",
//                "message.protectit.invalid_coords_format": "Block pos B must be >= than block pos A.",
//
//                "message.protectit.option_unavailable": "That option is not available yet.",
//                "message.protectit.non_givable_item": "That is not a valid item to give.",
//
//                "message.protectit.claim_successfully_removed": "The claim has been removed.",
//                "message.protectit.unable_locate_player": "Unable to locate the player.",
//                "message.protectit.property.list": "%s's Protected Properties",
//                "message.protectit.property.list.empty": "[Empty]",
//                "message.protectit.property.rename.success": "The property was successfully renamed to ",
//                "message.protectit.property.name.unknown": "Player does not own a property named ",
//                "message.protectit.whitelist.property.list": "Whitelist for property ",
//                "message.protectit.whitelist.add.success": "The player was successfully added to ",
//                "message.protectit.whitelist.remove.success": "The player was successfully removed from ",
//                "message.protectit.whitelist.clear.success": "The whitelist was successfully cleared from ",
//

        /*
         * Util.chats
         */
        // exceptions / errors
        add(LangUtil.chat("unable_locate_player"), "Unable to locate the player -> %s");
        add(LangUtil.chat("unexpected_error"), "An unexpected error occurred.");

        // parcels
        add(LangUtil.chat("parcel.block_protected"),"Block is protected.");
        add(LangUtil.chat("parcel.outside_world_boundaries"), "The parcel extends beyond the world boundaries.");
        add(LangUtil.chat("parcel.max_reached"), "You have already reached your max. number of parcels.");

        add(LangUtil.chat("parcel.list"), "%s's Parcels");
        add(LangUtil.chat("parcel.list.empty"), "[Empty]");

        add(LangUtil.chat("parcel.add.success"), "The parcel has been added.");
        add(LangUtil.chat("parcel.add.failure"), "Unable to add the parcel.");

        add(LangUtil.chat("parcel.remove.success"), "The parcel has been removed.");
        add(LangUtil.chat("parcel.remove.failure"), "Unable to remove the parcel.");

        add(LangUtil.chat("parcel.rename.success"), "The parcel has been renamed.");
        add(LangUtil.chat("parcel.rename.failure"), "Unable to rename the parcel.");

        add(LangUtil.chat("parcel.transfer.success"), "The parcel has been transferred.");
        add(LangUtil.chat("parcel.transfer.failure"), "Unable to transfer the parcel.");

        add(LangUtil.chat("parcel.whitelist.add.success"), "A player was added to the whitelist.");
        add(LangUtil.chat("parcel.whitelist.add.failure"), "Unable to player to the whitelist.");
        add(LangUtil.chat("parcel.whitelist.list"), "Whitelist for property ");

        // deeds
        add(LangUtil.chat("deed.generate.failure"), "Unable to generate deed.");

        /*
         *  Util.tooltips
         */
        // general
        add(LangUtil.tooltip("hold_shift"), "Hold [SHIFT] to expand");

        // parcel
        add(LangUtil.tooltip("parcel.howto.remove"), "Place cornerstone block inside parcel boundaries.\\nUse cornerstone block to open GUI.\nClick Remove button.");


    }
}
