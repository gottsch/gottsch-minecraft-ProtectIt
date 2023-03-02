
package mod.gottsch.forge.protectit.datagen;

import mod.gottsch.forge.protectit.ProtectIt;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 
 * @author Mark Gottschling Feb 28, 2023
 *
 */
public class ItemModelsProvider extends ItemModelProvider {

	public ItemModelsProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
		super(generator, ProtectIt.MODID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		// tabs
		singleTexture(
				"subdivide_license",
				mcLoc("item/generated"), "layer0", modLoc("item/subdivide_license"));

	}
}
