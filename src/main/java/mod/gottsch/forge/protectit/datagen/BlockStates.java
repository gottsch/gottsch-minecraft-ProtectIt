
package mod.gottsch.forge.protectit.datagen;

import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.block.ProtectItBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 
 * @author Mark Gottschling on Mar 19, 2024
 *
 */
public class BlockStates extends BlockStateProvider {

	public BlockStates(PackOutput gen, ExistingFileHelper helper) {
        super(gen, ProtectIt.MODID, helper);
    }
	
	@Override
	protected void registerStatesAndModels() {
		simpleBlock(ProtectItBlocks.FOUNDATION_STONE.get());
	}

}
