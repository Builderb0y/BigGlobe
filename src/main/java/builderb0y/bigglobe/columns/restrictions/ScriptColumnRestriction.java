package builderb0y.bigglobe.columns.restrictions;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptUsage;

public class ScriptColumnRestriction implements ColumnRestriction {

	public final RestrictionScriptHolder script;

	public ScriptColumnRestriction(RestrictionScriptHolder script) {
		this.script = script;
	}

	@Override
	public double getRestriction(ScriptedColumn column, int y) {
		return this.script.get(column, y);
	}

	@Wrapper
	public static class RestrictionScriptHolder extends ColumnYToDoubleScript.Holder {

		public RestrictionScriptHolder(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void addExtraFunctionsToEnvironment(ColumnEntryRegistry registry, MutableScriptEnvironment environment) {
			super.addExtraFunctionsToEnvironment(registry, environment);
			environment.addFunctionInvokeStatics(RangeColumnRestriction.class, "bandLinear", "bandSmooth");
		}
	}
}