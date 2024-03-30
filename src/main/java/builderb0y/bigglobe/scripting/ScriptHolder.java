package builderb0y.bigglobe.scripting;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptUsage;

public abstract class ScriptHolder<S extends Script> extends ScriptErrorCatcher.Impl implements Script, DelayedCompileable {

	public final @UseName("script") ScriptUsage usage;
	public transient S script;

	public ScriptHolder(ScriptUsage usage) {
		this.usage = usage;
	}

	@Override
	public @MultiLine String getSource() {
		return this.script != null ? this.script.getSource() : null;
	}

	@Override
	public @Nullable String getDebugName() {
		return this.script != null ? this.script.getDebugName() : null;
	}
}