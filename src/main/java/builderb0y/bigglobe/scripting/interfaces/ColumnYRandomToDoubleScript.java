package builderb0y.bigglobe.scripting.interfaces;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnYRandomToDoubleScript extends Script {

	public abstract double evaluate(WorldColumn column, double y, RandomGenerator random);

	@Wrapper
	public static class Holder extends ScriptHolder<ColumnYRandomToDoubleScript> implements ColumnYRandomToDoubleScript {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(ColumnYRandomToDoubleScript.class, this.usage)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createFixedXYZ(
						ColumnValue.REGISTRY,
						load("column", type(WorldColumn.class)),
						load("y", TypeInfos.DOUBLE)
					)
					.addXZ("x", "z")
					.addY("y")
					.addSeed("worldSeed")
					.build()
				)
				.addEnvironment(RandomScriptEnvironment.create(
					load("random", type(RandomGenerator.class))
				))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.parse(new ScriptClassLoader())
			);
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