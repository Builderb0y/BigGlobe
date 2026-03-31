package builderb0y.bigglobe.compat;

import net.fabricmc.loader.api.FabricLoader;

public class InstalledMods {

	public static final boolean
		C2ME = FabricLoader.getInstance().isModLoaded("c2me"),
		DISTANT_HORIZONS = FabricLoader.getInstance().isModLoaded("distanthorizons"),
		VOXY = FabricLoader.getInstance().isModLoaded("voxy"),
		IMMERSIVE_PORTALS = false;
}