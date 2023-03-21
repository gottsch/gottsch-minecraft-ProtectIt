package mod.gottsch.forge.protectit.datagen;

import java.util.function.Consumer;

import mod.gottsch.forge.protectit.core.item.ModItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;

/**
 * 
 * @author Mark Gottschling Feb 28, 2023
 *
 */
public class Recipes extends RecipeProvider {

		public Recipes(DataGenerator generator) {
			super(generator);
		}

		@Override
		protected void buildCraftingRecipes(Consumer<FinishedRecipe> recipe) {
			ShapelessRecipeBuilder.shapeless(ModItems.FIEFDOM_GRANT.get())
			.requires(Items.STONE_SHOVEL)
			.requires(Items.PAPER)
			.unlockedBy("has_stone", InventoryChangeTrigger.TriggerInstance.hasItems(
					Items.STONE))
			.save(recipe);
		}
}