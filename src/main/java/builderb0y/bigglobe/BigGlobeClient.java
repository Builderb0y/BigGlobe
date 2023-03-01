package builderb0y.bigglobe;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.commands.BigGlobeCommands;
import builderb0y.bigglobe.entities.BigGlobeEntityRenderers;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.trees.TreeRegistry;

@Environment(EnvType.CLIENT)
public class BigGlobeClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		BigGlobeMod.LOGGER.info("Initializing client...");
		TreeRegistry.init();
		BigGlobeBlocks.initClient();
		BigGlobeItems.initClient();
		BigGlobeEntityRenderers.init();
		BigGlobeNetwork.initClient();
		BigGlobeCommands.initClient();
		BigGlobeMod.LOGGER.info("Done initializing client.");
	}
}