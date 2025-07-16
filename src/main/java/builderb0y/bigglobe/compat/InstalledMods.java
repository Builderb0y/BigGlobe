package builderb0y.bigglobe.compat;

import net.fabricmc.loader.api.FabricLoader;

public class InstalledMods {

	public static final boolean
		C2ME              = FabricLoader.getInstance().isModLoaded("c2me"),
		DISTANT_HORIZONS  = FabricLoader.getInstance().isModLoaded("distanthorizons"),
		VOXY              = FabricLoader.getInstance().isModLoaded("voxy");
	//if setup fails for either of these two mods,
	//we will pretend they are not installed for
	//the remainder of the time the game stays open.
	public static boolean
		DIMLIB            = FabricLoader.getInstance().isModLoaded("dimlib"),
		IMMERSIVE_PORTALS = FabricLoader.getInstance().isModLoaded("immersive_portals");
}