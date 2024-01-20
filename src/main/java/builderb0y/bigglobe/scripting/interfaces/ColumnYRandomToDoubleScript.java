package builderb0y.bigglobe.scripting.interfaces;

import java.util.Set;
import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnYRandomToDoubleScript extends Script {

	public abstract double evaluate(WorldColumn column, double y, RandomGenerator random);

	@Wrapper
	public static class Holder extends ScriptHolder<ColumnYRandomToDoubleScript> implements ColumnYRandomToDoubleScript {

		public final transient Set<ColumnValue<?>> usedValues;

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnYRandomToDoubleScript script, Set<ColumnValue<?>> usedValues) {
			super(usage, script);
			this.usedValues = usedValues;
		}

		public static Holder create(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			ColumnScriptEnvironmentBuilder columnYScriptEnvironment = (
				ColumnScriptEnvironmentBuilder.createFixedXYZ(
					ColumnValue.REGISTRY,
					load("column", type(WorldColumn.class)),
					load("y", TypeInfos.DOUBLE)
				)
				.addXZ("x", "z")
				.addY("y")
				.addSeed("worldSeed")
			);
			ColumnYRandomToDoubleScript actualScript = (
				new TemplateScriptParser<>(ColumnYRandomToDoubleScript.class, usage)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(columnYScriptEnvironment.build())
				.addEnvironment(RandomScriptEnvironment.create(
					load("random", type(RandomGenerator.class))
				))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.parse()
			);
			return new Holder(usage, actualScript, columnYScriptEnvironment.usedValues);
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