package builderb0y.bigglobe.recipes;

import java.util.stream.IntStream;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.recipes.ScriptedRecipeClasses.ScriptedRecipeData;
import builderb0y.bigglobe.scripting.wrappers.CraftingGrid;

@AddPseudoField("id")
public class ScriptedRecipe extends SpecialCraftingRecipe {

	public final ScriptedRecipeData value;

	public ScriptedRecipe(Identifier id, ScriptedRecipeData value) {
		super(id);
		this.value = value;
	}

	public Identifier id() {
		return this.getId();
	}

	@Override
	public boolean matches(CraftingInventory inventory, World world) {
		return this.value.matches().matches(new CraftingGrid(
			IntStream.range(0, inventory.size()).mapToObj(inventory::getStack),
			inventory.getWidth(),
			inventory.getHeight(),
			false
		));
	}

	@Override
	public ItemStack craft(CraftingInventory inventory) {
		return this.value.output().output(new CraftingGrid(
			IntStream.range(0, inventory.size()).mapToObj(inventory::getStack),
			inventory.getWidth(),
			inventory.getHeight(),
			false
		));
	}

	@Override
	public DefaultedList<ItemStack> getRemainder(CraftingInventory inventory) {
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