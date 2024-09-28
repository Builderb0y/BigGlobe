package builderb0y.bigglobe.columns.scripted.traits;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class TraitLoader {

	public static final AutoCoder<Map<RegistryEntry<WorldTrait>, WorldTraitProvider>> MAP_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(new ReifiedType<Map<RegistryEntry<WorldTrait>, WorldTraitProvider>>() {});

	public static <T_Encoded> Map<RegistryEntry<WorldTrait>, WorldTraitProvider> load(Identifier generatorID, DecodeContext<T_Encoded> context) {
		if (generatorID == null) return null;
		generatorID = Identifier.of(generatorID.getNamespace(), "worldgen/bigglobe_world_trait_impl/" + generatorID.getPath() + ".json");
		List<Resource> resources = BigGlobeMod.getResourceManager().getAllResources(generatorID);
		if (resources == null || resources.isEmpty()) {
			throw new TraitLoadingException(new FileNotFoundException(generatorID.toString()));
		}
		context.logger().logMessageLazy(() -> "Loading traits from " + resources.size() + " data pack(s)...");
		Map<RegistryEntry<WorldTrait>, WorldTraitProvider> result = new HashMap<>(32);
		TraitLoadingException failure = null;
		for (Resource resource : resources) {
			context.logger().logMessageLazy(() -> "Loading traits from " + resource.#if MC_VERSION >= MC_1_20_5 getPackId() #else getResourcePackName() #endif);
			try (BufferedReader reader = resource.getReader()) {
				T_Encoded data = JsonOps.INSTANCE.convertTo(context.ops, JsonParser.parseReader(reader));
				result.putAll(context.input(data).decodeWith(MAP_CODER));
			}
			catch (Exception exception) {
				if (failure == null) failure = new TraitLoadingException(generatorID.toString());
				failure.addSuppressed(exception);
			}
		}
		if (failure != null) throw failure;
		else return result;
	}

	public static class TraitLoadingException extends RuntimeException {

		public TraitLoadingException() {}

		public TraitLoadingException(String message) {
			super(message);
		}

		public TraitLoadingException(Throwable cause) {
			super(cause);
		}

		public TraitLoadingException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}