package builderb0y.bigglobe.scripting;

import java.util.Set;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnPredicate extends Script {

	public abstract boolean test(WorldColumn column);

	public static class Parser extends ScriptParser<ColumnPredicate> {

		public final ColumnScriptEnvironmentBuilder builder;

		public Parser(String input) {
			super(ColumnPredicate.class, input);
			this.builder = (
				ColumnScriptEnvironmentBuilder
				.createFixedXZVariableY(ColumnValue.REGISTRY, load("column", 1, type(WorldColumn.class)), null)
				.trackUsedValues()
				.addXZ("x", "z")
			);
			this
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(this.builder.build());
		}
	}

	@Wrapper
	public static class Holder extends ScriptHolder<ColumnPredicate> implements ColumnPredicate {

		public final transient Set<ColumnValue<?>> usedValues;

		public Holder(ColumnPredicate predicate, Set<ColumnValue<?>> usedValues) {
			super(predicate);
			this.usedValues = usedValues;
		}

		public static Holder create(String script) throws ScriptParsingException {
			Parser parser = new Parser(script);
			return new Holder(parser.parse(), parser.builder.usedValues);
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