package builderb0y.bigglobe.scripting;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptUsage;

public abstract class ScriptHolder<S extends Script> implements Script, DelayedCompileable {

	public final @UseName("script") @EncodeInline ScriptUsage<GenericScriptTemplateUsage> usage;
	public transient S script;
	public transient long nextWarning = Long.MIN_VALUE;

	public ScriptHolder(ScriptUsage<GenericScriptTemplateUsage> usage, BetterRegistry.Lookup betterRegistryLookup) {
		this.usage = usage;
		betterRegistryLookup.getColumnEntryRegistryHolder().bigglobe_delayCompile(this);
	}

	public void onError(Throwable throwable) {
		long time = System.currentTimeMillis();
		if (time >= this.nextWarning) {
			this.nextWarning = time + 5000L;
			StringBuilder mainMessage = new StringBuilder().append("Caught exception from ").append(this.getClass().getName());
			if (this.getDebugName() != null) mainMessage.append(" (").append(this.getDebugName()).append(')');
			mainMessage.append(": ").append(throwable).append("; Check your logs for more info.");
			BuiltinScriptEnvironment.PRINTER.println(mainMessage.toString());
			ScriptLogger.LOGGER.error("Script source was:\n" + ScriptLogger.addLineNumbers(this.getSource()));
			ScriptLogger.LOGGER.error("Exception was: ", throwable);
		}
	}

	@Override
	public @MultiLine String getSource() {
		return this.script.getSource();
	}

	@Override
	public @Nullable String getDebugName() {
		return this.script.getDebugName();
	}
}