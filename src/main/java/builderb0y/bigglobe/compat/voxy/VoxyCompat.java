package builderb0y.bigglobe.compat.voxy;

import net.fabricmc.loader.api.FabricLoader;

import builderb0y.bigglobe.BigGlobeMod;

public class VoxyCompat {

	public static void init() {
		if (FabricLoader.getInstance().isModLoaded("voxy")) try {
			VoxyWorldGenerator.init();
			BigGlobeMod.LOGGER.info("Voxy compat initialized.");
		}
		catch (LinkageError error) {
			BigGlobeMod.LOGGER.error("Failed to setup voxy integration: ", error);
		}
	}
}