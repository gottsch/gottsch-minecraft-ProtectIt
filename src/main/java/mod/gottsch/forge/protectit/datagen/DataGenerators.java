
package mod.gottsch.forge.protectit.datagen;

import mod.gottsch.forge.protectit.core.ProtectIt;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 
 * @author Mark Gottschling on Mar 19, 2024
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		if (event.includeServer()) {
//			generator.addProvider(event.includeServer(), new Recipes(output));
			//            generator.addProvider(new TutLootTables(generator));
			//            TutBlockTags blockTags = new TutBlockTags(generator, event.getExistingFileHelper());
			//            generator.addProvider(blockTags);
			//            generator.addProvider(new DDItemTags(generator, blockTags, event.getExistingFileHelper()));
		}
		if (event.includeClient()) {
			generator.addProvider(event.includeClient(), new BlockStates(output, event.getExistingFileHelper()));
			generator.addProvider(event.includeClient(), new ItemModelsProvider(output, event.getExistingFileHelper()));
			generator.addProvider(event.includeClient(), new LanguageGen(output, "en_us"));
		}
	}
}