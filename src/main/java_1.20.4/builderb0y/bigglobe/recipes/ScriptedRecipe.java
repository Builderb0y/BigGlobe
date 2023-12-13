package builderb0y.bigglobe.recipes;

import java.util.stream.IntStream;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.DynamicRegistryManager;
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
	public boolean matches(RecipeInputInventory inventory, World world) {
		return this.value.matches().matches(new CraftingGrid(
			IntStream.range(0, inventory.size()).mapToObj(inventory::getStack),
			inventory.getWidth(),
			inventory.getHeight(),
			false
		));
	}

	@Override
	public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager dynamicRegistryManager) {
		return this.value.output().output(new CraftingGrid(
			IntStream.range(0, inventory.size()).mapToObj(inventory::getStack),
			inventory.getWidth(),
			inventory.getHeight(),
			false
		));
	}

	@Override
	public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inventory) {
		if (this.value.remainder() != null) {
			CraftingGrid input = new CraftingGrid(
				IntStream.range(0, inventory.size()).mapToObj(inventory::getStack),
				inventory.getWidth(),
				inventory.getHeight(),
				false
			);
			CraftingGrid output = new CraftingGrid(
				IntStream.range(0, inventory.size()).mapToObj(index -> ItemStack.EMPTY),
				inventory.getWidth(),
				inventory.getHeight(),
				true
			);
			this.value.remainder().remainder(input, output);
			return new DefaultedList<>(output, ItemStack.EMPTY) {};
		}
		else {
			return super.getRemainder(inventory);
		}
	}

	@Override
	public boolean fits(int width, int height) {
		return width >= this.value.width() && height >= this.value.height();
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return BigGlobeRecipeSerializers.SCRIPTED;
	}
}