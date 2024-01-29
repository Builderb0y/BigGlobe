package builderb0y.bigglobe.overriders.overworld;

import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public interface OverworldGlacierHeightOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment GLACIER_HEIGHT_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("glacierHeight", FlatOverrider.createVariableFromField(OverworldColumn.class, "glacierHeight"))
	);

	public static class Holder extends OverworldFlatOverrider.Holder<OverworldGlacierHeightOverrider> implements OverworldGlacierHeightOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, BetterRegistry.Lookup betterRegistryLookup) {
			super(usage, betterRegistryLookup);
		}

		@Override
		public Class<OverworldGlacierHeightOverrider> getScriptClass() {
			return OverworldGlacierHeightOverrider.class;
		}
	}
}