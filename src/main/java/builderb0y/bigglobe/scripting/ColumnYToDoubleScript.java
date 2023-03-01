package builderb0y.bigglobe.scripting;

import java.util.Set;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnYToDoubleScript extends Script {

	public abstract double evaluate(WorldColumn column, double y);

	public static class Parser extends ScriptParser<ColumnYToDoubleScript> {

		public final ColumnYScriptEnvironment columnYScriptEnvironment;

		public Parser(String input) {
			super(ColumnYToDoubleScript.class, input);
			this.columnYScriptEnvironment = new ColumnYScriptEnvironment(
				new VarInfo("column", 1, type(WorldColumn.class)),
				new VarInfo("y", 2, TypeInfos.DOUBLE)
			);
			this.addEnvironment(MathScriptEnvironment.INSTANCE).addEnvironment(this.columnYScriptEnvironment);
		}
	}

	@Wrapper
	public static class Holder extends ScriptHolder<ColumnYToDoubleScript> implements ColumnYToDoubleScript {

		public final transient Set<ColumnValue<?>> usedValues;

		public Holder(ColumnYToDoubleScript script, Set<ColumnValue<?>> usedValues) {
			super(script);
			this.usedValues = usedValues;
		}

		public static Holder create(String script) throws ScriptParsingException {
			Parser parser = new Parser(script);
			return new Holder(parser.parse(), parser.columnYScriptEnvironment.usedValues);
		}

		@Override
		public double evaluate(WorldColumn column, double y) {
			try {
				return this.script.evaluate(column, y);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return Double.NaN;
			}
		}
	}
}