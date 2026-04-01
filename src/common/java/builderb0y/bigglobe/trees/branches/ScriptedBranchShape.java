package builderb0y.bigglobe.trees.branches;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ScriptedBranchShape extends Script {

	public abstract double evaluate(double fraction, RandomGenerator random);

	@Wrapper
	public static class Catcher extends ScriptCatcher<ScriptedBranchShape> implements ScriptedBranchShape {

		public Catcher(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(ScriptedBranchShape.class, this.usage, registry.parserFlags())
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.configureEnvironment((MutableScriptEnvironment environment) -> {
						environment.addVariableLoad("fraction", TypeInfos.DOUBLE);
					})
					.configureEnvironment(RandomScriptEnvironment.create(
						load("random", type(RandomGenerator.class))
					))
					.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
					.parse(new ScriptClassLoader())
			);
		}

		@Override
		public double evaluate(double fraction, RandomGenerator random) {
			try {
				return this.script.evaluate(fraction, random);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return 0.0D;
			}
		}
	}
}