package builderb0y.bigglobe.scripting.interfaces;

import java.util.Set;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnPredicate extends Script {

	public abstract boolean test(WorldColumn column);

	@Wrapper
	public static class Holder extends ScriptHolder<ColumnPredicate> implements ColumnPredicate {

		public final transient Set<ColumnValue<?>> usedValues;

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnPredicate predicate, Set<ColumnValue<?>> usedValues) {
			super(usage, predicate);
			this.usedValues = usedValues;
		}

		public static ColumnScriptEnvironmentBuilder setupParser(ScriptParser<ColumnPredicate> parser) {
			ColumnScriptEnvironmentBuilder builder = (
				ColumnScriptEnvironmentBuilder
				.createFixedXZVariableY(ColumnValue.REGISTRY, load("column", 1, type(WorldColumn.class)), null)
				.trackUsedValues()
				.addXZ("x", "z")
			);
			parser
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(builder.build());
			return builder;
		}

		public static Holder create(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			TemplateScriptParser<ColumnPredicate> parser = (
				new TemplateScriptParser<>(ColumnPredicate.class, usage)
			);
			ColumnScriptEnvironmentBuilder builder = setupParser(parser);
			return new Holder(usage, parser.parse(), builder.usedValues);
		}

		@Override
		public boolean test(WorldColumn column) {
			try {
				return this.script.test(column);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return false;
			}
		}
	}
}