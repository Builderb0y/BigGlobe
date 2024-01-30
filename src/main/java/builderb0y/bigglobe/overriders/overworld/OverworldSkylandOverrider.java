package builderb0y.bigglobe.overriders.overworld;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.FlatOverrider;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public interface OverworldSkylandOverrider extends OverworldFlatOverrider {

	public static final MutableScriptEnvironment SKYLAND_ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addVariable("skylandMinY", FlatOverrider.createVariableFromField(OverworldColumn.class, "skylandMinY"))
		.addVariable("skylandMaxY", FlatOverrider.createVariableFromField(OverworldColumn.class, "skylandMaxY"))
	);

	@Wrapper
	public static class Holder extends OverworldFlatOverrider.Holder<OverworldSkylandOverrider> implements OverworldSkylandOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public MutableScriptEnvironment setupEnvironment(MutableScriptEnvironment environment) {
			return super.setupEnvironment(environment).addAll(SKYLAND_ENVIRONMENT);
		}

		@Override
		public Class<OverworldSkylandOverrider> getScriptClass() {
			return OverworldSkylandOverrider.class;
		}
	}
}