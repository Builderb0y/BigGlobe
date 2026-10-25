package builderb0y.bigglobe.features;

import builderb0y.bigglobe.BigGlobeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class BigGlobeConfiguredFeatureTagKeys {

	public static final TagKey<ConfiguredFeature<?, ?>>
		OVERWORLD_SURFACE_DECORATORS = overworld("surface_decorators"),
		OVERWORLD_SEA_LEVEL_DECORATORS = overworld("sea_level_decorators"),
		OVERWORLD_GLACIER_DECORATORS = overworld("glacier_decorators"),
		OVERWORLD_BEDROCK_DECORATORS = overworld("bedrock_decorators"),
		END_BRIDGE_CLOUD_LOWER_CEILING = end("bridge_cloud_lower_ceiling"),
		END_BRIDGE_CLOUD_LOWER_FLOOR = end("bridge_cloud_lower_floor"),
		END_BRIDGE_CLOUD_UPPER_CEILING = end("bridge_cloud_upper_ceiling"),
		END_BRIDGE_CLOUD_UPPER_FLOOR = end("bridge_cloud_upper_floor"),
		END_MOUNTAIN_CEILING = end("mountain_ceiling"),
		END_MOUNTAIN_FLOOR = end("mountain_floor"),
		END_NEST_CEILING = end("nest_ceiling"),
		END_NEST_FLOOR = end("nest_floor"),
		END_RING_CLOUD_LOWER_CEILING = end("ring_cloud_lower_ceiling"),
		END_RING_CLOUD_LOWER_FLOOR = end("ring_cloud_lower_floor"),
		END_RING_CLOUD_UPPER_CEILING = end("ring_cloud_upper_ceiling"),
		END_RING_CLOUD_UPPER_FLOOR = end("ring_cloud_upper_floor");

	public static TagKey<ConfiguredFeature<?, ?>> overworld(String name) {
		return TagKey.create(Registries.CONFIGURED_FEATURE, BigGlobeMod.modID("overworld/" + name));
	}

	public static TagKey<ConfiguredFeature<?, ?>> end(String name) {
		return TagKey.create(Registries.CONFIGURED_FEATURE, BigGlobeMod.modID("end/" + name));
	}
}