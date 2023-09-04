package builderb0y.bigglobe.overriders.end;

import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

public interface EndFlatOverrider extends FlatOverrider {

	public abstract void override(ScriptStructures structureStarts, EndColumn column);

	@Override
	@Deprecated
	public default void override(ScriptStructures structureStarts, WorldColumn column) {
		this.override(structureStarts, (EndColumn)(column));
	}

	public static class Holder<T_Overrider extends EndFlatOverrider> extends FlatOverrider.Holder<T_Overrider> implements EndFlatOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, TemplateScriptParser<T_Overrider> parser) throws ScriptParsingException {
			super(usage, parser);
		}

		@Override
		public void override(ScriptStructures structureStarts, EndColumn column) {
			try {
				this.script.override(structureStarts, column);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}