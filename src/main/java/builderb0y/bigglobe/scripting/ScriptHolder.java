package builderb0y.bigglobe.scripting;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.scripting.parsing.Script;

@AddPseudoField(name = "script", getter = "getSource")
public class ScriptHolder<S extends Script> implements Script {

	public final transient S script;
	public transient long nextWarning = Long.MIN_VALUE;

	public ScriptHolder(S script) {
		this.script = script;
	}

	public void onError(Throwable throwable) {
		long time = System.currentTimeMillis();
		if (time >= this.nextWarning) {
			this.nextWarning = time + 5000L;
			ScriptLogger.LOGGER.error("Caught exception from " + this.getClass().getName() + ':', throwable);
			ScriptLogger.LOGGER.error("Script source was:\n" + ScriptLogger.addLineNumbers(this.getSource()));
		}
	}

	@Override
	public @MultiLine String getSource() {
		return this.script.getSource();
	}
}