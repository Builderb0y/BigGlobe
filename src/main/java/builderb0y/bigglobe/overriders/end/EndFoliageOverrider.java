package builderb0y.bigglobe.overriders.end;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public interface EndFoliageOverrider extends EndFlatOverrider {

	public static final MutableScriptEnvironment FOLIAGE_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("foliage", FlatOverrider.createVariableFromStaticGetterAndSetter(EndVolumetricOverrider.class, EndColumn.class, "getFoliage", "setFoliage"))
	);

	public static double getFoliage(EndColumn column) {
		return column.foliage;
	}

	public static void setFoliage(EndColumn column, double foliage) {
		column.foliage = foliage;
	}

	@Wrapper
	public static class Holder extends EndFlatOverrider.Holder<EndFoliageOverrider> implements EndFoliageOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(EndFoliageOverrider.class, script)
				.addEnvironment(FOLIAGE_ENVIRONMENT)
			);
		}
	}
}