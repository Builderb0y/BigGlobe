package builderb0y.bigglobe.recipes;

import builderb0y.bigglobe.BigGlobeMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class BigGlobeRecipeSerializers {

	public static final RecipeSerializer<ScriptedRecipe> SCRIPTED = register("scripted", ScriptedRecipeSerializer.INSTANCE);

	public static void init() {}

	public static <T extends RecipeSerializer<?>> T register(String name, T serializer) {
		return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, BigGlobeMod.modID(name), serializer);
	}
}