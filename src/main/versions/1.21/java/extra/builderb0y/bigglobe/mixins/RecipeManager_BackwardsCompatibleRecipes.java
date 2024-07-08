package builderb0y.bigglobe.mixins;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import builderb0y.bigglobe.BigGlobeMod;

@Mixin(RecipeManager.class)
public class RecipeManager_BackwardsCompatibleRecipes {

	@Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At("HEAD"))
	private void bigglobe_portRecipes(
		Map<Identifier, JsonElement> map,
		ResourceManager resourceManager,
		Profiler profiler,
		CallbackInfo callback
	) {
		for (Map.Entry<Identifier, JsonElement> entry : map.entrySet()) {
			if (
				entry.getKey().getNamespace().equals(BigGlobeMod.MODID) &&
				entry.getValue() instanceof JsonObject root &&
				root.get("type") instanceof JsonPrimitive type &&
				type.isString()
			) {
				switch (type.getAsString()) {
					case
						"crafting_shaped",
						"crafting_shapeless",
						"smithing_transform",
						"minecraft:crafting_shaped",
						"minecraft:crafting_shapeless",
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
							BigGlobeMod.LOGGER.warn("Unexpected format in crafting or smithing recipe " + entry.getKey());
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
							BigGlobeMod.LOGGER.warn("Unexpected format in stonecutting or smelting recipe " + entry.getKey());
						}
					}
					case "bigglobe:scripted" -> {}
					default -> {
						BigGlobeMod.LOGGER.warn("Unknown recipe type: " + type.getAsString());
					}
				}
			}
		}
	}
}