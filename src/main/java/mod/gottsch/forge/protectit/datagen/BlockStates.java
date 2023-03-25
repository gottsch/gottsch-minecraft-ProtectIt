package mod.gottsch.forge.protectit.datagen;

import mod.gottsch.forge.protectit.ProtectIt;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 
 * @author Mark Gottschling Feb 27, 2023
 *
 */
public class BlockStates extends BlockStateProvider {

	public BlockStates(DataGenerator gen, ExistingFileHelper helper) {
        super(gen, ProtectIt.MODID, helper);
    }
	
	@Override
	protected void registerStatesAndModels() {
//		simpleBlock(ProtectItItems.fiefdom_deed.get());
	}

}
