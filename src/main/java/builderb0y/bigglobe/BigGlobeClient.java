package builderb0y.bigglobe;

import builderb0y.bigglobe.platform.fabric.api.ClientModInitializer;
import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.commands.BigGlobeCommands;
import builderb0y.bigglobe.entities.BigGlobeEntityRenderers;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.hyperspace.HyperspaceDimensionEffects;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.particles.BigGlobeParticles;
import builderb0y.bigglobe.rendering.lods.LodSystem;
import builderb0y.bigglobe.scripting.ClientPrintSink;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
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
		LodSystem.init();


		BuiltinScriptEnvironment.PRINTER = new ClientPrintSink();
		BigGlobeMod.LOGGER.info("Done initializing client.");
	}
}