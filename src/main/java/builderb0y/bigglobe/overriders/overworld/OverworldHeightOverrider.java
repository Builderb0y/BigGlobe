package builderb0y.bigglobe.overriders.overworld;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

public interface OverworldHeightOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment Y_LEVELS_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("terrainY", FlatOverrider.createVariableFromField(OverworldColumn.class, "finalHeight"))
		.addVariable("snowY",    FlatOverrider.createVariableFromField(OverworldColumn.class, "snowHeight"))
	);

	@Wrapper
	public static class Holder extends OverworldFlatOverrider.Holder<OverworldHeightOverrider> implements OverworldHeightOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				usage,
				new TemplateScriptParser<>(OverworldHeightOverrider.class, usage)
				.addEnvironment(Y_LEVELS_ENVIRONMENT)
			);
		}
	}
}