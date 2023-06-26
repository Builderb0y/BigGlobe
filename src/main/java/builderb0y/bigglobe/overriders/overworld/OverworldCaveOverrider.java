package builderb0y.bigglobe.overriders.overworld;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public interface OverworldCaveOverrider extends OverworldVolumetricOverrider {

	public static class Context extends OverworldVolumetricOverrider.Context {

		public Context(ScriptStructures structures, OverworldColumn column) {
			super(structures, column, column.getFinalTopHeightI() - column.getCaveCell().settings.depth(), column.getCaveNoise());
		}

		@Override
		public double getExclusionMultiplier(int y) {
			return this.caveCell.settings.getEffectiveWidth(this.column(), y);
		}
	}

	@Wrapper
	public static class Holder extends OverworldVolumetricOverrider.Holder<OverworldCaveOverrider> implements OverworldCaveOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(new ScriptParser<>(OverworldCaveOverrider.class, script));
		}
	}
}