package builderb0y.bigglobe.registration;

import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeBiomeKeys {

	public static final RegistryKey<Biome>
		BEACH               = of("beach"),
		COLD_FOREST         = of("cold_forest"),
		COLD_PLAINS         = of("cold_plains"),
		COLD_WASTELAND      = of("cold_wasteland"),
		DEEP_OCEAN          = of("deep_ocean"),
		HOT_FOREST          = of("hot_forest"),
		HOT_PLAINS          = of("hot_plains"),
		HOT_WASTELAND       = of("hot_wasteland"),
		OCEAN               = of("ocean"),
		SHALLOW_OCEAN       = of("shallow_ocean"),
		TEMPERATE_FOREST    = of("temperate_forest"),
		TEMPERATE_PLAINS    = of("temperate_plains"),
		TEMPERATE_WASTELAND = of("temperate_wasteland");

	public static RegistryKey<Biome> of(String name) {
		return RegistryKey.of(Registry.BIOME_KEY, BigGlobeMod.modID(name));
	}
}