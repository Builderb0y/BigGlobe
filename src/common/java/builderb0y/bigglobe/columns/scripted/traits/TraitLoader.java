package builderb0y.bigglobe.columns.scripted.traits;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.Resource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.data.UnknownData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class TraitLoader {

	public static final AutoCoder<Map<Holder<WorldTrait>, WorldTraitProvider>> MAP_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(new ReifiedType<Map<Holder<WorldTrait>, WorldTraitProvider>>() {});

	public static <T_Encoded> Map<Holder<WorldTrait>, WorldTraitProvider> load(Identifier generatorID, DecodeContext<T_Encoded> context) {
		if (generatorID == null) return null;
		generatorID = Identifier.fromNamespaceAndPath(generatorID.getNamespace(), "bigglobe/worldgen/world_trait_impl/" + generatorID.getPath() + ".json");
		List<Resource> resources = BigGlobeMod.getResourceManager().getResourceStack(generatorID);
		if (resources == null || resources.isEmpty()) {
			throw new TraitLoadingException(new FileNotFoundException(generatorID.toString()));
		}
		context.logger().logMessageLazy(() -> "Loading traits from " + resources.size() + " data pack(s)...");
		Map<Holder<WorldTrait>, WorldTraitProvider> result = new HashMap<>(32);
		TraitLoadingException failure = null;
		for (Resource resource : resources) {
			context.logger().logMessageLazy(() -> "Loading traits from " + resource.sourcePackId());
			try (BufferedReader reader = resource.openAsReader()) {
				result.putAll(context.withData(new UnknownData<>(JsonOps.INSTANCE, JsonParser.parseReader(reader))).decodeWith(MAP_CODER));
			}
			catch (Exception exception) {
				if (failure == null) failure = new TraitLoadingException(generatorID.toString());
				failure.addSuppressed(exception);
			}
		}
		if (failure != null) throw failure;
		else return result;
	}

	public static Map<Holder<WorldTrait>, WorldTraitProvider> loadFromCode(JsonObject traits) {
		try {
			return BigGlobeAutoCodec.AUTO_CODEC.decode(
				MAP_CODER,
				traits,
				RegistryOps.create(
					JsonOps.INSTANCE,
					BigGlobeMod.getCurrentServer().registryAccess()
				)
			);
		}
		catch (DecodeException exception) {
			throw new IllegalArgumentException(exception);
		}
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