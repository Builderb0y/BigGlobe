package builderb0y.bigglobe;

import builderb0y.bigglobe.platform.fabric.api.DedicatedServerModInitializer;
import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;

import builderb0y.bigglobe.scripting.ServerPrintSink;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.DEDICATED_SERVER)
public class BigGlobeServer implements DedicatedServerModInitializer {

	@Override
	public void onInitializeServer() {
		BuiltinScriptEnvironment.PRINTER = new ServerPrintSink();
	}
}