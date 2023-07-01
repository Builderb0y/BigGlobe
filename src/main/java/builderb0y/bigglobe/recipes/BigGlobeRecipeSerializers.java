package builderb0y.bigglobe.recipes;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeRecipeSerializers {

	public static final RecipeSerializer<BallOfStringAddRecipe> BALL_OF_STRING_ADD = register("crafting_special_ball_of_string_add", new SpecialRecipeSerializer<>(BallOfStringAddRecipe::new));
	public static final RecipeSerializer<BallOfStringRemoveRecipe> BALL_OF_STRING_REMOVE = register("crafting_special_ball_of_string_remove", new SpecialRecipeSerializer<>(BallOfStringRemoveRecipe::new));

	public static void init() {}

	public static <T extends RecipeSerializer<?>> T register(String name, T serializer) {
		return Registry.register(RegistryVersions.recipeSerializer(), BigGlobeMod.modID(name), serializer);
	}
}