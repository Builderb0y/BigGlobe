package builderb0y.bigglobe.scripting;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptUsage;

public abstract class ScriptHolder<S extends Script> extends ScriptErrorCatcher.Impl implements Script, DelayedCompileable {

	public final @UseName("script") ScriptUsage<GenericScriptTemplateUsage> usage;
	public transient S script;

	public ScriptHolder(ScriptUsage<GenericScriptTemplateUsage> usage) {
		this.usage = usage;
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