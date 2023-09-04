package builderb0y.bigglobe.scripting;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.logging.AbstractTaskLogger;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptUsage;

public class ScriptHolder<S extends Script> implements Script {

	public final @UseName("script") ScriptUsage<GenericScriptTemplateUsage> usage;
	public final transient S script;
	public transient long nextWarning = Long.MIN_VALUE;

	public ScriptHolder(ScriptUsage<GenericScriptTemplateUsage> usage, S script) {
		this.usage = usage;
		this.script = script;
	}

	public void onError(Throwable throwable) {
		long time = System.currentTimeMillis();
		if (time >= this.nextWarning) {
			this.nextWarning = time + 5000L;
			BuiltinScriptEnvironment.PRINTER.println("Caught exception from " + this.getClass().getName() + ':');
			BuiltinScriptEnvironment.PRINTER.println("Script source was:\n" + ScriptLogger.addLineNumbers(this.getSource()));
			BuiltinScriptEnvironment.PRINTER.println("Exception was:\n" + AbstractTaskLogger.stackTraceToString(throwable));
			if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
				BuiltinScriptEnvironment.PRINTER.println("If this exception does not fit inside your chat window, check your logs instead.");
			}
		}
	}

	@Override
	public @MultiLine String getSource() {
		return this.script.getSource();
	}
}