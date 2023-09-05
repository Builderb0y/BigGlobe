package builderb0y.bigglobe.overriders.overworld;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

public interface OverworldGlacierHeightOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment GLACIER_HEIGHT_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("glacierHeight", FlatOverrider.createVariableFromField(OverworldColumn.class, "glacierHeight"))
	);

	@Wrapper
	public static class Holder extends OverworldFlatOverrider.Holder<OverworldGlacierHeightOverrider> implements OverworldGlacierHeightOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				usage,
				new TemplateScriptParser<>(OverworldGlacierHeightOverrider.class, usage)
				.addEnvironment(GLACIER_HEIGHT_ENVIRONMENT)
			);
		}
	}
}