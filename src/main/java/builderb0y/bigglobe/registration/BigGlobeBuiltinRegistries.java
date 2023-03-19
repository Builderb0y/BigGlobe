package builderb0y.bigglobe.registration;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.WorldPreset;

import builderb0y.autocodec.decoders.AutoDecoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class BigGlobeBuiltinRegistries {

	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/BuiltinRegistries");

	public static final RegistryKey<WorldPreset>
		BIG_GLOBE_WORLD_PRESET_KEY = RegistryKey.of(Registry.WORLD_PRESET_KEY, BigGlobeMod.modID(BigGlobeMod.MODID));
	public static final RegistryKey<DimensionType>
		BIG_GLOBE_OVERWORLD_DIMENSION_TYPE_KEY = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, BigGlobeMod.modID("overworld")),
		BIG_GLOBE_NETHER_DIMENSION_TYPE_KEY = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, BigGlobeMod.modID("nether"));

	public static JsonElement getJson(String path) {
		try (
			Reader reader = new InputStreamReader(
				Objects.requireNonNull(
					BigGlobeMod.class.getResourceAsStream(path),
					path
				),
				StandardCharsets.UTF_8
			)
		) {
			return JsonParser.parseReader(reader);
		}
		catch (Exception exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public static <T> T parseJson(JsonElement json, AutoDecoder<T> decoder) {
		RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, BuiltinRegistries.DYNAMIC_REGISTRY_MANAGER);
		try {
			return BigGlobeAutoCodec.AUTO_CODEC.decode(decoder, json, ops);
		}
		catch (DecodeException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public static <T> T parseJson(JsonElement json, Decoder<T> decoder) {
		return parseJson(json, BigGlobeAutoCodec.AUTO_CODEC.wrapDFUDecoder(decoder, false));
	}

	public static <T> T parseJson(JsonElement json, Class<T> clazz) {
		return parseJson(json, BigGlobeAutoCodec.AUTO_CODEC.createDecoder(clazz));
	}

	public static JsonElement getDimension(String preset, String dimension) {
		BigGlobeMod.LOGGER.info("Reading " + dimension + " chunk generator from mod jar.");
		return (
			getJson("/data/bigglobe/worldgen/world_preset/" + preset + ".json")
			.getAsJsonObject()
			.getAsJsonObject("dimensions")
			.getAsJsonObject("minecraft:" + dimension)
			.getAsJsonObject("generator")
			.getAsJsonObject("value")
		);
	}

	public static void init() {
		LOGGER.debug("Registering builtin registry objects...");
		for (String biomeName : new String[] {
			"cold_forest", "cold_plains", "cold_wasteland",
			"hot_forest", "hot_plains", "hot_wasteland",
			"temperate_forest", "temperate_plains", "temperate_wasteland",
			"beach", "shallow_ocean", "ocean", "deep_ocean",
			"ashen_wastes", "crimson_forest", "warped_forest",
			"inferno", "nether_wastes", "valley_of_souls"
		}) {
			BuiltinRegistries.add(
				BuiltinRegistries.BIOME,
				BigGlobeMod.modID(biomeName),
				parseJson(getJson("/data/bigglobe/worldgen/biome/" + biomeName + ".json"), Biome.CODEC)
			);
		}
		BuiltinRegistries.add(
			BuiltinRegistries.DIMENSION_TYPE,
			BIG_GLOBE_OVERWORLD_DIMENSION_TYPE_KEY,
			parseJson(getJson("/data/bigglobe/dimension_type/overworld.json"), DimensionType.CODEC)
		);
		BuiltinRegistries.add(
			BuiltinRegistries.DIMENSION_TYPE,
			BIG_GLOBE_NETHER_DIMENSION_TYPE_KEY,
			parseJson(getJson("/data/bigglobe/dimension_type/nether.json"), DimensionType.CODEC)
		);
		BuiltinRegistries.add(
			BuiltinRegistries.WORLD_PRESET,
			BIG_GLOBE_WORLD_PRESET_KEY,
			parseJson(getJson("/data/bigglobe/worldgen/world_preset/bigglobe.json"), WorldPreset.CODEC)
		);
		LOGGER.debug("Done registering builtin registry objects.");
	}
}