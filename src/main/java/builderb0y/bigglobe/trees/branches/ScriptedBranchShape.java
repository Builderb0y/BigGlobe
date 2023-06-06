package builderb0y.bigglobe.trees.branches;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironment;
import builderb0y.bigglobe.scripting.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.StatelessRandomScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ScriptedBranchShape extends Script {

	public abstract double evaluate(double fraction, WorldColumn column, double y, RandomGenerator random);

	@Wrapper
	public static class Holder extends ScriptHolder<ScriptedBranchShape> implements ScriptedBranchShape {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(ScriptedBranchShape.class, script)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(
					new MutableScriptEnvironment()
					.addVariableLoad("fraction", 1, TypeInfos.DOUBLE)
				)
				.addEnvironment(
					ColumnScriptEnvironment.createFixedXYZ(
						ColumnValue.REGISTRY,
						load("column", 3, type(WorldColumn.class)),
						load("y", 4, TypeInfos.DOUBLE)
					)
					.addXZ("x", "z")
					.addY("y")
					.mutable
				)
				.addEnvironment(RandomScriptEnvironment.create(
					load("random", 6, type(RandomGenerator.class))
				))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.parse()
			);
		}

		@Override
		public double evaluate(double fraction, WorldColumn column, double y, RandomGenerator random) {
			try {
				return this.script.evaluate(fraction, column, y, random);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return 0.0D;
			}
		}
	}
}