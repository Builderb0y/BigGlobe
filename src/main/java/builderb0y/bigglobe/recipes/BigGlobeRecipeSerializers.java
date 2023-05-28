package builderb0y.bigglobe.recipes;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeRecipeSerializers {

	public static final RecipeSerializer<BallOfStringRecipe> BALL_OF_STRING = Registry.register(Registries.RECIPE_SERIALIZER, BigGlobeMod.modID("crafting_special_ball_of_string"), new SpecialRecipeSerializer<>(BallOfStringRecipe::new));

	public static void init() {}
}