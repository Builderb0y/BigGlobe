package builderb0y.bigglobe.scripting.interfaces;

import java.util.Set;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnYToDoubleScript extends Script {

	public abstract double evaluate(WorldColumn column, double y);

	public static class Holder extends ScriptHolder<ColumnYToDoubleScript> implements ColumnYToDoubleScript {

		public transient Set<ColumnValue<?>> usedValues;

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, BetterRegistry.Lookup betterRegistryLookup) {
			super(usage, betterRegistryLookup);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			ColumnScriptEnvironmentBuilder builder = (
				ColumnScriptEnvironmentBuilder.createFixedXYZ(
					ColumnValue.REGISTRY,
					load("column", type(WorldColumn.class)),
					load("y", TypeInfos.DOUBLE)
				)
				.trackUsedValues()
				.addXZ("x", "z")
				.addY("y")
				.addSeed("worldSeed")
			);
			this.script = (
				new TemplateScriptParser<>(ColumnYToDoubleScript.class, this.usage)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.addEnvironment(builder.build())
				.parse()
			);
			this.usedValues = builder.usedValues;
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