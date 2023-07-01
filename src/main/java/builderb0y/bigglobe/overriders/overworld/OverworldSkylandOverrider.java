package builderb0y.bigglobe.overriders.overworld;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public interface OverworldSkylandOverrider extends OverworldFlatOverrider {

	public static final ScriptEnvironment SKYLAND_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("skylandMinY", FlatOverrider.createVariableFromStaticGetterAndSetter(OverworldSkylandOverrider.class, OverworldColumn.class, "getSkylandMinY", "setSkylandMinY"))
		.addVariable("skylandMaxY", FlatOverrider.createVariableFromStaticGetterAndSetter(OverworldSkylandOverrider.class, OverworldColumn.class, "getSkylandMaxY", "setSkylandMaxY"))
	);

	public static double getSkylandMinY(OverworldColumn column) {
		return column.skylandMinY;
	}

	public static void setSkylandMinY(OverworldColumn column, double y) {
		column.skylandMinY = y;
	}

	public static double getSkylandMaxY(OverworldColumn column) {
		return column.skylandMaxY;
	}

	public static void setSkylandMaxY(OverworldColumn column, double y) {
		column.skylandMaxY = y;
	}

	@Wrapper
	public static class Holder extends OverworldFlatOverrider.Holder<OverworldSkylandOverrider> implements OverworldSkylandOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(OverworldSkylandOverrider.class, script)
				.addEnvironment(SKYLAND_ENVIRONMENT)
			);
		}
	}
}