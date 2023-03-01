package builderb0y.bigglobe.scripting;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.scripting.parsing.Script;

@AddPseudoField(name = "script", getter = "getSource")
public class ScriptHolder<S extends Script> implements Script {

	public final transient S script;

	public ScriptHolder(S script) {
		this.script = script;
	}

	public void onError(Throwable throwable) {
		printError(this.script, this.getClass(), throwable);
	}

	public static void printError(Script script, Class<?> wrapper, Throwable throwable) {
		ScriptLogger.LOGGER.error("Caught exception from " + wrapper.getName() + ':', throwable);
		ScriptLogger.LOGGER.error("Script source was:\n" + ScriptLogger.addLineNumbers(script.getSource()));
	}

	@Override
	public @MultiLine String getSource() {
		return this.script.getSource();
	}
}