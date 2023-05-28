package builderb0y.bigglobe.recipes;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import builderb0y.bigglobe.items.BigGlobeItems;

public class BallOfStringRemoveRecipe extends SpecialCraftingRecipe {

	public BallOfStringRemoveRecipe(Identifier id, CraftingRecipeCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(CraftingInventory inventory, World world) {
		boolean haveBall = false;
		for (int slot = 0, size = inventory.size(); slot < size; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isEmpty()) {
				continue;
			}
			else if (stack.isOf(BigGlobeItems.BALL_OF_STRING)) {
				haveBall = true;
			}
			else {
				return false;
			}
		}
		return haveBall;
	}

	@Override
	public ItemStack craft(CraftingInventory inventory, DynamicRegistryManager registryManager) {
		int balls = 0;
		for (int slot = 0, size = inventory.size(); slot < size; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isEmpty()) {
				continue;
			}
			else if (stack.isOf(BigGlobeItems.BALL_OF_STRING)) {
				if (stack.getDamage() < stack.getMaxDamage()) balls++;
			}
			else {
				return ItemStack.EMPTY;
			}
		}
		return new ItemStack(Items.STRING, balls);
	}

	@Override
	public DefaultedList<ItemStack> getRemainder(CraftingInventory inventory) {
		int size = inventory.size();
		DefaultedList<ItemStack> items = DefaultedList.ofSize(size, ItemStack.EMPTY);
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isOf(BigGlobeItems.BALL_OF_STRING)) {
				stack = stack.copy();
				if (stack.getDamage() < stack.getMaxDamage()) {
					stack.setDamage(stack.getDamage() + 1);
				}
				items.set(slot, stack);
			}
		}
		return items;
	}

	@Override
	public boolean fits(int width, int height) {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return BigGlobeRecipeSerializers.BALL_OF_STRING_REMOVE;
	}
}