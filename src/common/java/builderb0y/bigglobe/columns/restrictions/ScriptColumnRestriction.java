package builderb0y.bigglobe.columns.restrictions;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToDoubleScript.Catcher;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.parsing.ExpressionParser;
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
		public void addExtraFunctionsToEnvironment(ImplParameters parameters, ExpressionParser parser) {
			super.addExtraFunctionsToEnvironment(parameters, parser);
			parser.environment.mutable().addFunctionInvokeStatics(RangeColumnRestriction.class, "bandLinear", "bandSmooth");
		}
	}
}