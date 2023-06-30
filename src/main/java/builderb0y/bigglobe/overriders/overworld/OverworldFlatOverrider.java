package builderb0y.bigglobe.overriders.overworld;

import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public interface OverworldFlatOverrider extends FlatOverrider {

	public abstract void override(ScriptStructures structureStarts, OverworldColumn column);

	@Override
	@Deprecated
	public default void override(ScriptStructures structureStarts, WorldColumn column) {
		this.override(structureStarts, (OverworldColumn)(column));
	}

	public static class Holder<T_Overrider extends OverworldFlatOverrider> extends FlatOverrider.Holder<T_Overrider> implements OverworldFlatOverrider {

		public Holder(ScriptParser<T_Overrider> parser) throws ScriptParsingException {
			super(parser, OverworldColumn.class);
		}

		@Override
		public void override(ScriptStructures structureStarts, OverworldColumn column) {
			try {
				this.script.override(structureStarts, column);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}
}