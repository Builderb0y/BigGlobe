package builderb0y.bigglobe.overriders.end;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

public interface EndFoliageOverrider extends EndFlatOverrider {

	public static final MutableScriptEnvironment FOLIAGE_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("foliage", FlatOverrider.createVariableFromField(EndColumn.class, "foliage"))
	);

	@Wrapper
	public static class Holder extends EndFlatOverrider.Holder<EndFoliageOverrider> implements EndFoliageOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(
				usage,
				new TemplateScriptParser<>(EndFoliageOverrider.class, usage)
				.addEnvironment(FOLIAGE_ENVIRONMENT)
			);
		}
	}
}