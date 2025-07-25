package builderb0y.bigglobe.recipes;

import java.util.stream.IntStream;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.bigglobe.recipes.ScriptedRecipeClasses.ScriptedRecipeData;
import builderb0y.bigglobe.scripting.wrappers.CraftingGrid;

public class ScriptedRecipe extends SpecialCraftingRecipe {

	@EncodeInline
	public final ScriptedRecipeData value;

	public ScriptedRecipe(ScriptedRecipeData value) {
		super(value.category());
		this.value = value;
	}

	@Override
	public boolean matches(CraftingRecipeInput inventory, World world) {
		return this.value.matches().matches(new CraftingGrid(
			inventory.getStacks().stream(),
			inventory.getWidth(),
			inventory.getHeight(),
			false
		));
	}

	@Override
	public ItemStack craft(CraftingRecipeInput inventory, WrapperLookup lookup) {
		return this.value.output().output(new CraftingGrid(
			inventory.getStacks().stream(),
			inventory.getWidth(),
			inventory.getHeight(),
			false
		));
	}

	@Override
	public DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput inventory) {
		if (this.value.remainder() != null) {
			CraftingGrid input = new CraftingGrid(
				inventory.getStacks().stream(),
				inventory.getWidth(),
				inventory.getHeight(),
				false
			);
			CraftingGrid output = new CraftingGrid(
				IntStream.range(0, inventory.size()).mapToObj((int index) -> ItemStack.EMPTY),
				inventory.getWidth(),
				inventory.getHeight(),
				true
			);
			this.value.remainder().remainder(input, output);
			return new DefaultedList<>(output, ItemStack.EMPTY) {};
		}
		else {
			return super.getRecipeRemainders(inventory);
		}
	}

	@Override
	public RecipeSerializer<ScriptedRecipe> getSerializer() {
		return BigGlobeRecipeSerializers.SCRIPTED;
	}
}