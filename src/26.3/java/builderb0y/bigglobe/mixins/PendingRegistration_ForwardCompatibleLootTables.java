package builderb0y.bigglobe.mixins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import builderb0y.bigglobe.BigGlobeMod;

@Mixin(targets = "net.minecraft.resources.RegistryLoadTask$PendingRegistration")
public class PendingRegistration_ForwardCompatibleLootTables {

	@Inject(method = "loadFromResource", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"))
	private static <T> void bigglobe_modifyJson(CallbackInfoReturnable<Either<T, Exception>> callback, @Local(argsOnly = true) ResourceKey key, @Local JsonElement root) {
		if (key.registryKey() == Registries.LOOT_TABLE && key.identifier().getNamespace().equals(BigGlobeMod.MODID)) {
			bigglobe_processRecursive(root);
		}
	}

	@Unique
	private static void bigglobe_processRecursive(JsonElement root) {
		switch (root) {
			case JsonObject object -> {
				for (JsonElement value : object.asMap().values()) {
					bigglobe_processRecursive(value);
				}
				switch (object.remove("conditions")) {
					case JsonArray conditions -> {
						switch (conditions.size()) {
							case 0 -> {}
							case 1 -> {
								object.add("condition", conditions.get(0));
							}
							default -> {
								JsonObject condition = new JsonObject();
								condition.addProperty("type", "minecraft:all_of");
								condition.add("terms", conditions);
								object.add("condition", condition);
							}
						}
					}
					case JsonElement other -> {
						object.add("conditions", other);
					}
					case null -> {}
				}
				switch (object.remove("condition")) {
					case JsonPrimitive condition -> {
						switch (condition.getAsString()) {
							case "block_state_property", "minecraft:block_state_property" -> {
								object.addProperty("type", "minecraft:match_block");
								object.add("blocks", object.remove("block"));
								object.add("state", object.remove("properties"));
							}
							case String other -> {
								object.addProperty("type", other);
							}
						}
					}
					case JsonElement other -> {
						object.add("condition", other);
					}
					case null -> {}
				}
				switch (object.remove("functions")) {
					case JsonArray functions -> {
						switch (functions.size()) {
							case 0 -> {}
							case 1 -> {
								object.add("modifier", functions.get(0));
							}
							default -> {
								JsonObject function = new JsonObject();
								function.addProperty("type", "minecraft:sequence");
								function.add("functions", functions);
								object.add("modifier", function);
							}
						}
					}
					case JsonElement other -> {
						object.add("functions", other);
					}
					case null -> {}
				}
				switch (object.remove("function")) {
					case JsonPrimitive function -> {
						switch (function.getAsString()) {
							case "set_count", "minecraft:set_count" -> {
								if (object.get("count") instanceof JsonObject count) {
									count.addProperty("type", "minecraft:uniform");
								}
							}
							case "set_damage", "minecraft:set_damage" -> {
								if (object.get("damage") instanceof JsonObject count) {
									count.addProperty("type", "minecraft:uniform");
								}
							}
						}
						object.add("type", function);
					}
					case JsonElement other -> {
						object.add("function", other);
					}
					case null -> {}
				}
			}
			case JsonArray array -> {
				array.forEach(PendingRegistration_ForwardCompatibleLootTables::bigglobe_processRecursive);
			}
			default -> {}
		}
	}
}