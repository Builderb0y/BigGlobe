package builderb0y.bigglobe.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import builderb0y.bigglobe.items.BallOfStringItem;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

#if MC_VERSION == MC_1_19_4
import net.minecraft.inventory.CraftingInventory;
#elif MC_VERSION == MC_1_20_1
import net.minecraft.inventory.RecipeInputInventory;
#endif

public class BallOfStringAddRecipe extends SpecialCraftingRecipe {

	public static final TagKey<Item> STRING = TagKey.of(RegistryKeyVersions.item(), new Identifier("c", "string"));

	public BallOfStringAddRecipe(Identifier id, CraftingRecipeCategory category) {
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
		boolean haveBall = false, haveString = false;
		for (int slot = 0, size = inventory.size(); slot < size; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isEmpty()) {
				continue;
			}
			else if (stack.isIn(STRING)) {
				haveString = true;
			}
			else if (stack.isOf(BigGlobeItems.BALL_OF_STRING)) {
				if (haveBall) return false;
				else haveBall = true;
			}
			else {
				return false;
			}
		}
		return haveBall & haveString;
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
		ItemStack ball = ItemStack.EMPTY;
		int string = 0;
		for (int slot = 0, size = inventory.size(); slot < size; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isEmpty()) {
				continue;
			}
			else if (stack.isIn(STRING)) {
				string++;
			}
			else if (stack.isOf(BigGlobeItems.BALL_OF_STRING)) {
				if (ball.isEmpty()) ball = stack;
				else return ItemStack.EMPTY;
			}
			else {
				return ItemStack.EMPTY;
			}
		}
		ball = ball.copy();
		if (string > 0) {
			BallOfStringItem.addString(ball, string);
		}
		return ball;
	}

	@Override
	public boolean fits(int width, int height) {
		return width * height > 1;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return BigGlobeRecipeSerializers.BALL_OF_STRING_ADD;
	}
}