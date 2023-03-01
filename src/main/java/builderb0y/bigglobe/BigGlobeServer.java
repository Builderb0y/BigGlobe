package builderb0y.bigglobe;

import net.fabricmc.api.DedicatedServerModInitializer;

import builderb0y.bigglobe.trees.TreeRegistry;

public class BigGlobeServer implements DedicatedServerModInitializer {

	@Override
	public void onInitializeServer() {
		BigGlobeMod.LOGGER.info("Initializing server...");
		TreeRegistry.init();
		BigGlobeMod.LOGGER.info("Done initializing server.");
	}
}