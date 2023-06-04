package builderb0y.bigglobe.dynamicRegistries;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;

import builderb0y.bigglobe.BigGlobeMod;

public class WoodPaletteKeys {

	@SuppressWarnings("unused")
	public static final RegistryKey<WoodPalette>
		OAK              = vanilla ("oak"             ),
		SPRUCE           = vanilla ("spruce"          ),
		BIRCH            = vanilla ("birch"           ),
		JUNGLE           = vanilla ("jungle"          ),
		ACACIA           = vanilla ("acacia"          ),
		DARK_OAK         = vanilla ("dark_oak"        ),
		MANGROVE         = vanilla ("mangrove"        ),
		AZALEA           = vanilla ("azalea"          ),
		FLOWERING_AZALEA = vanilla ("flowering_azalea"),
		CRIMSON          = vanilla ("crimson"         ),
		WARPED           = vanilla ("warped"          ),
		CHARRED          = bigglobe("charred"         );

	public static RegistryKey<WoodPalette> of(Identifier id) {
		return RegistryKey.of(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY, id);
	}

	public static RegistryKey<WoodPalette> vanilla(String name) {
		return of(BigGlobeMod.mcID(name));
	}

	public static RegistryKey<WoodPalette> bigglobe(String name) {
		return of(BigGlobeMod.modID(name));
	}
}