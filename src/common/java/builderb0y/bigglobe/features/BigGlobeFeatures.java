package builderb0y.bigglobe.features;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class BigGlobeFeatures {

	static {
		BigGlobeMod.LOGGER.debug("Registering features...");
	}

	public static final #if MC_VERSION >= MC_26_3_0 MapCodec<? extends FeatureAdapter<?>> #else FeatureAdapter<?> #endif
		SINGLE_BLOCK       = register("single_block",       SingleBlockFeature.class),
		NATURAL_TREE       = register("natural_tree",       NaturalTreeFeature.class),
		ARTIFICIAL_TREE    = register("artificial_tree",    ArtificialTreeFeature.class),
		GENERIC_ORE        = register("generic_ore",        GenericOreFeature.class),
		FLOWER             = register("flower",             FlowerFeature.class),
		FLOWER_CONTROLLER  = register("flower_controller",  FlowerControllerFeature.class),
		NETHER_FLOWER      = register("nether_flower",      NetherFlowerFeature.class),

		CHUNK_SPRINKLE     = register("chunk_sprinkle",     ChunkSprinkleFeature.class),
		BEDROCK            = register("bedrock",            BedrockFeature.class),
		ROCK_LAYER         = register("rock_layer",         RockLayerFeature.class),
		ORE                = register("ore",                OreFeature.class),
		SCRIPTED_ORE       = register("scripted_ore",       ScriptedOreFeature.class),

		SCRIPT             = register("script",             ScriptedFeature.class);

	static {
		BigGlobeMod.LOGGER.debug("Done registering features.");
	}

	#if MC_VERSION >= MC_26_3_0

		public static <F extends Feature> MapCodec<F> register(String name, Class<F> clazz) {
			return Registry.register(BuiltInRegistries.FEATURE_TYPE, BigGlobeMod.modID(name), BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(clazz));
		}

	#else

		public static <F extends Feature<?>> F register(String name, Class<F> clazz) {
			F feature;
			try {
				feature = clazz.getDeclaredConstructor((Class<?>[])(null)).newInstance((Object[])(null));
			}
			catch (Exception exception) {
				throw AutoCodecUtil.rethrow(new InstantiationError().initCause(exception));
			}
			return Registry.register(BuiltInRegistries.FEATURE, BigGlobeMod.modID(name), feature);
		}

	#endif

	public static void init() {}
}