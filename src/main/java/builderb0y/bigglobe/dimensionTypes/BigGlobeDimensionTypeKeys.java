package builderb0y.bigglobe.dimensionTypes;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.dimension.DimensionType;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeDimensionTypeKeys {

	public static final RegistryKey<DimensionType>
		OVERWORLD = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, BigGlobeMod.modID("overworld")),
		NETHER    = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, BigGlobeMod.modID("nether")),
		END       = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, BigGlobeMod.modID("end"));
}