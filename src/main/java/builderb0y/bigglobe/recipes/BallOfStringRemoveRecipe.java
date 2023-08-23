package builderb0y.bigglobe.recipes;

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

#if MC_VERSION == MC_1_19_4
import net.minecraft.inventory.CraftingInventory;
#elif MC_VERSION == MC_1_20_1
import net.minecraft.inventory.RecipeInputInventory;
#endif

public class BallOfStringRemoveRecipe extends SpecialCraftingRecipe {

	public BallOfStringRemoveRecipe(Identifier id, CraftingRecipeCategory category) {
		super(id, category);
	}

	@Override
	#if MC_VERSION == MC_1_19_2
		public boolean matches() {
	#elif MC_VERSION == MC_1_19_4
		public boolean matches(CraftingInventory inventory, World world) {
	#elif MC_VERSION == MC_1_20_1
		public boolean matches(RecipeInputInventory inventory, World world) {
	#else
		#error "check if minecraft changed the recipe methods again or not."
	#endif
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
	#if MC_VERSION == MC_1_19_2
		public ItemStack craft() {
	#elif MC_VERSION == MC_1_19_4
		public ItemStack craft(CraftingInventory inventory, DynamicRegistryManager dynamicRegistryManager) {
	#elif MC_VERSION == MC_1_20_1
		public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager dynamicRegistryManager) {
	#else
		#error "check if minecraft changed the recipe methods again or not."
	#endif
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
	#if MC_VERSION == MC_1_19_2
		public DefaultedList<ItemStack> getRemainder() {
	#elif MC_VERSION == MC_1_19_4
		public DefaultedList<ItemStack> getRemainder(CraftingInventory inventory) {
	#elif MC_VERSION == MC_1_20_1
		public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inventory) {
	#else
		#error "check if minecraft changed the recipe methods again or not."
	#endif
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