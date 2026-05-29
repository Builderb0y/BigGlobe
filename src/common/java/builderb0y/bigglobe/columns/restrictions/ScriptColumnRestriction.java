package builderb0y.bigglobe.columns.restrictions;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted2.ColumnScript.ColumnYToDoubleScript.Catcher;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.input.ScriptUsage;

public class ScriptColumnRestriction implements ColumnRestriction {

	public final RestrictionScriptCatcher script;

	public ScriptColumnRestriction(RestrictionScriptCatcher script) {
		this.script = script;
	}

	@Override
	public double getRestriction(ScriptedColumn column, int y) {
		return this.script.get(column, y);
	}

	@Wrapper
	public static class RestrictionScriptCatcher extends Catcher {

		public RestrictionScriptCatcher(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void addExtraFunctionsToEnvironment(ImplParameters parameters, MutableScriptEnvironment environment) {
			super.addExtraFunctionsToEnvironment(parameters, environment);
			environment.addFunctionInvokeStatics(RangeColumnRestriction.class, "bandLinear", "bandSmooth");
		}
	}
}