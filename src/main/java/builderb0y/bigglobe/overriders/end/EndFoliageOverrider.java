package builderb0y.bigglobe.overriders.end;

import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public interface EndFoliageOverrider extends EndFlatOverrider {

	public static class Holder extends EndFlatOverrider.Holder<EndFoliageOverrider> implements EndFoliageOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, BetterRegistry.Lookup betterRegistryLookup) {
			super(usage, betterRegistryLookup);
		}

		@Override
		public Class<EndFoliageOverrider> getScriptClass() {
			return EndFoliageOverrider.class;
		}

		@Override
		public MutableScriptEnvironment setupEnvironment(MutableScriptEnvironment environment) {
			return (
				super
				.setupEnvironment(environment)
				.addVariable("foliage", FlatOverrider.createVariableFromField(EndColumn.class, "foliage"))
			);
		}
	}
}