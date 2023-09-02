package builderb0y.bigglobe.scripting;

import java.util.Set;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnYToDoubleScript extends Script {

	public abstract double evaluate(WorldColumn column, double y);

	@Wrapper
	public static class Holder extends ScriptHolder<ColumnYToDoubleScript> implements ColumnYToDoubleScript {

		public final ScriptUsage<GenericScriptTemplateUsage> usage;
		public final transient Set<ColumnValue<?>> usedValues;

		public Holder(ColumnYToDoubleScript script, ScriptUsage<GenericScriptTemplateUsage> usage, Set<ColumnValue<?>> usedValues) {
			super(script);
			this.usage = usage;
			this.usedValues = usedValues;
		}

		public static ColumnScriptEnvironmentBuilder setupParser(ScriptParser<ColumnYToDoubleScript> parser) {
			ColumnScriptEnvironmentBuilder builder = (
				ColumnScriptEnvironmentBuilder.createFixedXYZ(
					ColumnValue.REGISTRY,
					load("column", 1, type(WorldColumn.class)),
					load("y", 2, TypeInfos.DOUBLE)
				)
				.trackUsedValues()
				.addXZ("x", "z")
				.addY("y")
				.addSeed("worldSeed")
			);
			parser
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
			.addEnvironment(builder.build());
			return builder;
		}

		public static Holder create(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			ScriptParser<ColumnYToDoubleScript> parser = TemplateScriptParser.createFrom(ColumnYToDoubleScript.class, usage);
			ColumnScriptEnvironmentBuilder builder = setupParser(parser);
			ColumnYToDoubleScript actualScript = parser.parse();
			return new Holder(actualScript, usage, builder.usedValues);
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