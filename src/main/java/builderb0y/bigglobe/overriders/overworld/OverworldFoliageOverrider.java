package builderb0y.bigglobe.overriders.overworld;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

public interface OverworldFoliageOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment FOLIAGE_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("foliage", FlatOverrider.createVariableFromStaticGetterAndSetter(OverworldFoliageOverrider.class, OverworldColumn.class, "getFoliage", "setFoliage"))
	);

	public static double getFoliage(OverworldColumn column) {
		return column.foliage;
	}

	public static void setFoliage(OverworldColumn column, double foliage) {
		column.foliage = foliage;
	}

	@Wrapper
	public static class Holder extends OverworldFlatOverrider.Holder<OverworldFoliageOverrider> implements OverworldFoliageOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				usage,
				new TemplateScriptParser<>(OverworldFoliageOverrider.class, usage)
				.addEnvironment(FOLIAGE_ENVIRONMENT)
			);
		}
	}
}