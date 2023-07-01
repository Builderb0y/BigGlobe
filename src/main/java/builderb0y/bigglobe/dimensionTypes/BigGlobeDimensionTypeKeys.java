package builderb0y.bigglobe.dimensionTypes;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class BigGlobeDimensionTypeKeys {

	public static final RegistryKey<DimensionType>
		OVERWORLD = RegistryKey.of(RegistryKeyVersions.dimensionType(), BigGlobeMod.modID("overworld")),
		NETHER    = RegistryKey.of(RegistryKeyVersions.dimensionType(), BigGlobeMod.modID("nether")),
		END       = RegistryKey.of(RegistryKeyVersions.dimensionType(), BigGlobeMod.modID("end"));
}