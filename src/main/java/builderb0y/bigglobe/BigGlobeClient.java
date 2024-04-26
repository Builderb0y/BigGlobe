package builderb0y.bigglobe;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.commands.BigGlobeCommands;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.compat.satin.SatinCompat;
import builderb0y.bigglobe.entities.BigGlobeEntityRenderers;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.hyperspace.HyperspaceDimensionEffects;
import builderb0y.bigglobe.hyperspace.ClientWaypointManager;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.particles.BigGlobeParticles;
import builderb0y.bigglobe.scripting.ClientPrintSink;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;

@Environment(EnvType.CLIENT)
public class BigGlobeClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		BigGlobeMod.LOGGER.info("Initializing client...");
		BigGlobeFluids.initClient();
		BigGlobeBlocks.initClient();
		BigGlobeItems.initClient();
		BigGlobeEntityRenderers.init();
		BigGlobeNetwork.initClient();
		BigGlobeCommands.initClient();
		BigGlobeParticles.initClient();
		HyperspaceDimensionEffects.init();
		ClientWaypointManager.init();
		SatinCompat.init();
		BuiltinScriptEnvironment.PRINTER = new ClientPrintSink();
		DistantHorizonsCompat.init();
		BigGlobeMod.LOGGER.info("Done initializing client.");
	}
}