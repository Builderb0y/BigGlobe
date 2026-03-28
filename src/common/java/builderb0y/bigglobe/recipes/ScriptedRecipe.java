package builderb0y.bigglobe.recipes;

import java.util.stream.IntStream;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.bigglobe.recipes.ScriptedRecipeClasses.ScriptedRecipeData;
import builderb0y.bigglobe.scripting.wrappers.CraftingGrid;

public class ScriptedRecipe extends CustomRecipe {

	@EncodeInline
	public final ScriptedRecipeData value;

	public ScriptedRecipe(ScriptedRecipeData value) {
		super(value.category());
		this.value = value;
	}

	@Override
	public boolean matches(CraftingInput inventory, Level world) {
		return this.value.matches().matches(new CraftingGrid(
			inventory.items().stream(),
			inventory.width(),
			inventory.height(),
			false
		));
	}

	@Override
	public ItemStack assemble(CraftingInput inventory, Provider lookup) {
		return this.value.output().output(new CraftingGrid(
			inventory.items().stream(),
			inventory.width(),
			inventory.height(),
			false
		));
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput inventory) {
		if (this.value.remainder() != null) {
			CraftingGrid input = new CraftingGrid(
				inventory.items().stream(),
				inventory.width(),
				inventory.height(),
				false
			);
			CraftingGrid output = new CraftingGrid(
				IntStream.range(0, inventory.size()).mapToObj((int index) -> ItemStack.EMPTY),
				inventory.width(),
				inventory.height(),
				true
			);
			this.value.remainder().remainder(input, output);
			return new NonNullList<>(output, ItemStack.EMPTY) {

			};
		}
		else {
			return super.getRemainingItems(inventory);
		}
	}

	@Override
	public RecipeSerializer<ScriptedRecipe> getSerializer() {
		return BigGlobeRecipeSerializers.SCRIPTED;
	}
}