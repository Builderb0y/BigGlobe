package builderb0y.bigglobe.overriders.overworld;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public interface OverworldHeightOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment Y_LEVELS_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("terrainY", FlatOverrider.createVariableFromStaticGetterAndSetter(OverworldHeightOverrider.class, OverworldColumn.class, "getMaxY",  "setMaxY" ))
		.addVariable("snowY",    FlatOverrider.createVariableFromStaticGetterAndSetter(OverworldHeightOverrider.class, OverworldColumn.class, "getSnowY", "setSnowY"))
	);

	public static double getMaxY(OverworldColumn column) {
		return column.finalHeight;
	}

	public static void setMaxY(OverworldColumn column, double y) {
		column.finalHeight = y;
	}

	public static double getSnowY(OverworldColumn column) {
		return column.snowHeight;
	}

	public static void setSnowY(OverworldColumn column, double y) {
		column.snowHeight = y;
	}

	@Wrapper
	public static class Holder extends OverworldFlatOverrider.Holder<OverworldHeightOverrider> implements OverworldHeightOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(OverworldHeightOverrider.class, script)
				.addEnvironment(Y_LEVELS_ENVIRONMENT)
			);
		}
	}
}