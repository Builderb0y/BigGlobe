package builderb0y.bigglobe;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.bigglobe.scripting.ServerPrintSink;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;

@Environment(EnvType.SERVER)
public class BigGlobeServer implements DedicatedServerModInitializer {

	@Override
	public void onInitializeServer() {
		BuiltinScriptEnvironment.PRINTER = new ServerPrintSink();
	}
}