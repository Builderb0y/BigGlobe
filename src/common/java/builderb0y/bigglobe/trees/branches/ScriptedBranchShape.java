package builderb0y.bigglobe.trees.branches;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

public interface ScriptedBranchShape extends ColumnScript {

	public abstract double evaluate(ScriptedColumn column, double fraction, RandomGenerator random);

	@Wrapper
	public static class Catcher extends BaseCatcher<ScriptedBranchShape> implements ScriptedBranchShape {

		public Catcher(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public Class<ScriptedBranchShape> getScriptClass() {
			return ScriptedBranchShape.class;
		}

		@Override
		public void addExtraFunctionsToEnvironment(ImplParameters parameters, ExpressionParser parser) {
			super.addExtraFunctionsToEnvironment(parameters, parser);
			parser.environment.mutable().addVariableLoad("fraction", TypeInfos.DOUBLE);
		}

		/*
		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(ScriptedBranchShape.class, this.usage, registry.parserFlags())
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					environment.addVariableLoad("fraction", TypeInfos.DOUBLE);
				})
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.addImportedValue("random", load("random", type(RandomGenerator.class)))
				.parse(new ScriptClassLoader())
			);
		}
		*/

		@Override
		public double evaluate(ScriptedColumn column, double fraction, RandomGenerator random) {
			try {
				return this.script.evaluate(column, fraction, random);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return 0.0D;
			}
		}
	}
}