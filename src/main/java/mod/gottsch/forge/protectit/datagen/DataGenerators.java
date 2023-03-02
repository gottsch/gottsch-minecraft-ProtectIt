package mod.gottsch.forge.protectit.datagen;

import mod.gottsch.forge.protectit.ProtectIt;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 
 * @author Mark Gottschling Feb 27, 2023
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		if (event.includeServer()) {
			generator.addProvider(false, new Recipes(generator));
			//            generator.addProvider(new TutLootTables(generator));
			//            TutBlockTags blockTags = new TutBlockTags(generator, event.getExistingFileHelper());
			//            generator.addProvider(blockTags);
			//            generator.addProvider(new DDItemTags(generator, blockTags, event.getExistingFileHelper()));
		}
		if (event.includeClient()) {
			generator.addProvider(false, new BlockStates(generator, event.getExistingFileHelper()));
			generator.addProvider(false, new ItemModelsProvider(generator, event.getExistingFileHelper()));
			generator.addProvider(false, new LanguageGen(generator, "en_us"));
		}
	}
}