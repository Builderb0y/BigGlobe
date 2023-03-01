package builderb0y.bigglobe.scripting;

import java.util.Set;
import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnYRandomToDoubleScript extends Script {

	public abstract double evaluate(WorldColumn column, double y, RandomGenerator random);

	@Wrapper
	public static class Holder extends ScriptHolder<ColumnYRandomToDoubleScript> implements ColumnYRandomToDoubleScript {

		public final transient Set<ColumnValue<?>> usedValues;

		public Holder(ColumnYRandomToDoubleScript script, Set<ColumnValue<?>> usedValues) {
			super(script);
			this.usedValues = usedValues;
		}

		public static Holder create(String script) throws ScriptParsingException {
			ColumnYScriptEnvironment columnYScriptEnvironment = new ColumnYScriptEnvironment(
				new VarInfo("column", 1, type(WorldColumn.class)),
				new VarInfo("y", 2, TypeInfos.DOUBLE)
			);
			ColumnYRandomToDoubleScript actualScript = (
				new ScriptParser<>(ColumnYRandomToDoubleScript.class, script)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(columnYScriptEnvironment)
				.addEnvironment(new RandomScriptEnvironment(
					new LoadInsnTree(new VarInfo("random", 4, RandomScriptEnvironment.RANDOM_GENERATOR_TYPE))
				))
				.parse()
			);
			return new Holder(actualScript, columnYScriptEnvironment.usedValues);
		}

		@Override
		public double evaluate(WorldColumn column, double y, RandomGenerator random) {
			try {
				return this.script.evaluate(column, y, random);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return Double.NaN;
			}
		}
	}
}