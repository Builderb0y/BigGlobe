package builderb0y.bigglobe.overriders.overworld;

import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public interface OverworldFoliageOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment FOLIAGE_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("foliage", FlatOverrider.createVariableFromField(OverworldColumn.class, "foliage"))
	);

	public static class Holder extends OverworldFlatOverrider.Holder<OverworldFoliageOverrider> implements OverworldFoliageOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, BetterRegistry.Lookup betterRegistryLookup) {
			super(usage, betterRegistryLookup);
		}

		@Override
		public Class<OverworldFoliageOverrider> getScriptClass() {
			return OverworldFoliageOverrider.class;
		}

		@Override
		public MutableScriptEnvironment setupEnvironment(MutableScriptEnvironment environment) {
			return super.setupEnvironment(environment).addAll(FOLIAGE_ENVIRONMENT);
		}
	}
}