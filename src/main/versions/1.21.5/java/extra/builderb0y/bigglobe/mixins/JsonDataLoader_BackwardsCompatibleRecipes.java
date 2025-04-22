package builderb0y.bigglobe.mixins;

import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.recipe.Recipe;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;

@Mixin(JsonDataLoader.class)
public class JsonDataLoader_BackwardsCompatibleRecipes {

	@ModifyExpressionValue(
		method = "load(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/resource/ResourceFinder;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V",
		at = @At(
			value = "INVOKE",
			target = "Lcom/google/gson/JsonParser;parseReader(Ljava/io/Reader;)Lcom/google/gson/JsonElement;",
			remap = false
		)
	)
	private static JsonElement bigglobe_portRecipes(
		JsonElement original,
		@Local(argsOnly = true) Codec<?> codec,
		@Local(index = 7) Identifier id
	) {
		if (
			codec == Recipe.CODEC &&
			id.getNamespace().equals(BigGlobeMod.MODID) &&
			!id.getPath().startsWith("suspicious_stew_") && //already the correct format.
			original instanceof JsonObject root &&
			root.get("type") instanceof JsonPrimitive type &&
			type.isString()
		) {
			switch (type.getAsString()) {
				case
					"crafting_shaped",
					"minecraft:crafting_shaped"
				-> {
					if (
						root.get("result") instanceof JsonObject result &&
						result.get("item") instanceof JsonPrimitive item &&
						item.isString()
					) {
						result.add("id", item);
						result.remove("item");
					}
					else {
						BigGlobeMod.LOGGER.warn("Unexpected format in shaped crafting recipe " + id);
					}
					if (root.get("key") instanceof JsonObject keys) {
						for (Map.Entry<String, JsonElement> entry : keys.entrySet()) {
							entry.setValue(bigglobe_transformIngredient(entry.getValue()));
						}
					}
				}
				case
					"crafting_shapeless",
					"minecraft:crafting_shapeless"
				-> {
					if (
						root.get("result") instanceof JsonObject result &&
						result.get("item") instanceof JsonPrimitive item &&
						item.isString()
					) {
						result.add("id", item);
						result.remove("item");
					}
					else {
						BigGlobeMod.LOGGER.warn("Unexpected format in shapeless crafting recipe " + id);
					}
					if (root.get("ingredients") instanceof JsonArray ingredients) {
						root.add("ingredients", bigglobe_transformIngredient(ingredients));
					}
				}
				case
					"smithing_transform",
					"minecraft:smithing_transform"
				-> {
					if (
						root.get("result") instanceof JsonObject result &&
						result.get("item") instanceof JsonPrimitive item &&
						item.isString()
					) {
						result.add("id", item);
						result.remove("item");
					}
					else {
						BigGlobeMod.LOGGER.warn("Unexpected format in smithing recipe " + id);
					}
					JsonElement ingredient;
					if ((ingredient = root.get("base")) != null) {
						root.add("base", bigglobe_transformIngredient(ingredient));
					}
					if ((ingredient = root.get("addition")) != null) {
						root.add("addition", bigglobe_transformIngredient(ingredient));
					}
					if ((ingredient = root.get("template")) != null) {
						root.add("template", bigglobe_transformIngredient(ingredient));
					}
				}
				case
					"stonecutting",
					"smelting",
					"blasting",
					"minecraft:stonecutting",
					"minecraft:smelting",
					"minecraft:blasting"
				-> {
					if (root.get("result") instanceof JsonPrimitive result && result.isString()) {
						JsonObject newResult = new JsonObject();
						newResult.add("id", result);
						if (root.get("count") instanceof JsonPrimitive count && count.isNumber()) {
							newResult.add("count", count);
							root.remove("count");
						}
						root.remove("result");
						root.add("result", newResult);
					}
					else {
						BigGlobeMod.LOGGER.warn("Unexpected format in stonecutting or smelting recipe " + id);
					}
					JsonElement ingredient;
					if ((ingredient = root.get("ingredient")) != null) {
						root.add("ingredient", bigglobe_transformIngredient(ingredient));
					}
				}
				case "bigglobe:scripted" -> {}
				default -> {
					BigGlobeMod.LOGGER.warn("Unknown recipe type: " + type.getAsString());
				}
			}
		}
		return original;
	}

	@Unique
	private static JsonElement bigglobe_transformIngredient(JsonElement ingredient) {
		if (ingredient instanceof JsonObject object) {
			if (object.get("item") instanceof JsonPrimitive item) {
				return item;
			}
			else if (object.get("tag") instanceof JsonPrimitive tag) {
				return new JsonPrimitive('#' + tag.getAsString());
			}
			else {
				return object;
			}
		}
		else if (ingredient instanceof JsonArray array) {
			for (int index = 0, size = array.size(); index < size; index++) {
				array.set(index, bigglobe_transformIngredient(array.get(index)));
			}
			return array;
		}
		else {
			return ingredient;
		}
	}
}