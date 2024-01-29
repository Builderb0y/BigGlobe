package builderb0y.bigglobe.scripting.interfaces;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnPredicate extends Script {

	public abstract boolean test(WorldColumn column);

	public static class Holder extends ScriptHolder<ColumnPredicate> implements ColumnPredicate {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, BetterRegistry.Lookup betterRegistryLookup) {
			super(usage, betterRegistryLookup);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(ColumnPredicate.class, this.usage)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder
					.createFixedXZVariableY(ColumnValue.REGISTRY, load("column", type(WorldColumn.class)), null)
					.trackUsedValues()
					.addXZ("x", "z")
					.build()
				)
				.parse()
			);
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