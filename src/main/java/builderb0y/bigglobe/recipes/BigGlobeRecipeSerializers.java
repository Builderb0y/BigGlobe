package builderb0y.bigglobe.recipes;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeRecipeSerializers {

	public static final RecipeSerializer<BallOfStringAddRecipe> BALL_OF_STRING_ADD = Registry.register(Registry.RECIPE_SERIALIZER, BigGlobeMod.modID("crafting_special_ball_of_string_add"), new SpecialRecipeSerializer<>(BallOfStringAddRecipe::new));
	public static final RecipeSerializer<BallOfStringRemoveRecipe> BALL_OF_STRING_REMOVE = Registry.register(Registry.RECIPE_SERIALIZER, BigGlobeMod.modID("crafting_special_ball_of_string_remove"), new SpecialRecipeSerializer<>(BallOfStringRemoveRecipe::new));

	public static void init() {}
}