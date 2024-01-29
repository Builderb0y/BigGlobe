package builderb0y.bigglobe.overriders.overworld;

import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public interface OverworldHeightOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment Y_LEVELS_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("terrainY", FlatOverrider.createVariableFromField(OverworldColumn.class, "finalHeight"))
		.addVariable("snowY",    FlatOverrider.createVariableFromField(OverworldColumn.class, "snowHeight"))
	);

	public static class Holder extends OverworldFlatOverrider.Holder<OverworldHeightOverrider> implements OverworldHeightOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public Class<OverworldHeightOverrider> getScriptClass() {
			return OverworldHeightOverrider.class;
		}

		@Override
		public MutableScriptEnvironment setupEnvironment(MutableScriptEnvironment environment) {
			return super.setupEnvironment(environment).addAll(Y_LEVELS_ENVIRONMENT);
		}
	}
}