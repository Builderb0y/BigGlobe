package builderb0y.bigglobe.scripting;

import java.util.Set;
import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

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
			ColumnScriptEnvironment columnYScriptEnvironment = ColumnScriptEnvironment.createFixedXYZ(
				ColumnValue.REGISTRY,
				load("column", 1, type(WorldColumn.class)),
				load("y", 2, TypeInfos.DOUBLE)
			)
			.addXZ("x", "z")
			.addY("y");
			ColumnYRandomToDoubleScript actualScript = (
				new ScriptParser<>(ColumnYRandomToDoubleScript.class, script)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(columnYScriptEnvironment.mutable)
				.addEnvironment(new RandomScriptEnvironment(
					load("random", 4, type(RandomGenerator.class))
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