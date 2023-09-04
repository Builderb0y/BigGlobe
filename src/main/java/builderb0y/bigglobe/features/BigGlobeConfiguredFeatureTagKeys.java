package builderb0y.bigglobe.features;

import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class BigGlobeConfiguredFeatureTagKeys {

	public static final TagKey<ConfiguredFeature<?, ?>>
		OVERWORLD_SURFACE_DECORATORS   = overworld("surface_decorators"),
		OVERWORLD_SEA_LEVEL_DECORATORS = overworld("sea_level_decorators"),
		OVERWORLD_GLACIER_DECORATORS   = overworld("glacier_decorators"),
		OVERWORLD_BEDROCK_DECORATORS   = overworld("bedrock_decorators"),
		END_BRIDGE_CLOUD_LOWER_CEILING =       end("bridge_cloud_lower_ceiling"),
		END_BRIDGE_CLOUD_LOWER_FLOOR   =       end("bridge_cloud_lower_floor"),
		END_BRIDGE_CLOUD_UPPER_CEILING =       end("bridge_cloud_upper_ceiling"),
		END_BRIDGE_CLOUD_UPPER_FLOOR   =       end("bridge_cloud_upper_floor"),
		END_MOUNTAIN_CEILING           =       end("mountain_ceiling"),
		END_MOUNTAIN_FLOOR             =       end("mountain_floor"),
		END_NEST_CEILING               =       end("nest_ceiling"),
		END_NEST_FLOOR                 =       end("nest_floor"),
		END_RING_CLOUD_LOWER_CEILING   =       end("ring_cloud_lower_ceiling"),
		END_RING_CLOUD_LOWER_FLOOR     =       end("ring_cloud_lower_floor"),
		END_RING_CLOUD_UPPER_CEILING   =       end("ring_cloud_upper_ceiling"),
		END_RING_CLOUD_UPPER_FLOOR     =       end("ring_cloud_upper_floor");

	public static TagKey<ConfiguredFeature<?, ?>> overworld(String name) {
		return TagKey.of(RegistryKeyVersions.configuredFeature(), BigGlobeMod.modID("overworld/" + name));
	}

	public static TagKey<ConfiguredFeature<?, ?>> end(String name) {
		return TagKey.of(RegistryKeyVersions.configuredFeature(), BigGlobeMod.modID("end/" + name));
	}
}