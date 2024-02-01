package builderb0y.bigglobe.trees.branches;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ScriptedBranchShape extends Script {

	public abstract double evaluate(double fraction, RandomGenerator random);

	@Wrapper
	public static class Holder extends ScriptHolder<ScriptedBranchShape> implements ScriptedBranchShape {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(ScriptedBranchShape.class, this.usage)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					environment.addVariableLoad("fraction", TypeInfos.DOUBLE);
				})
				.addEnvironment(RandomScriptEnvironment.create(
					load("random", type(RandomGenerator.class))
				))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.parse(new ScriptClassLoader())
			);
		}

		@Override
		public boolean requiresColumns() {
			return false;
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