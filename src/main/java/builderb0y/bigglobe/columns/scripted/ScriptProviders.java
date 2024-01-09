package builderb0y.bigglobe.columns.scripted;

import java.util.Collections;
import java.util.Set;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryRegistrable;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

public class ScriptProviders {

	public static <I> Set<RegistryEntry<ColumnEntryRegistrable>> setupParser(TemplateScriptParser<I> parser) {
		parser.addEnvironment(MathScriptEnvironment.INSTANCE);
		return Collections.emptySet(); //todo: create column script environment.
	}

	public static abstract class DependantScriptHolder<S extends Script> extends ScriptHolder<S> {

		public final Set<RegistryEntry<ColumnEntryRegistrable>> dependencies;

		public DependantScriptHolder(ScriptUsage<GenericScriptTemplateUsage> usage, Set<RegistryEntry<ColumnEntryRegistrable>> dependencies, S script) {
			super(usage, script);
			this.dependencies = dependencies;
		}

		public DependantScriptHolder(TemplateScriptParser<S> parser) throws ScriptParsingException {
			this(parser.usage, setupParser(parser), parser.parse());
		}
	}

	public static interface ColumnToIntScript extends Script {

		public abstract int compute(ScriptedColumn column);

		public static class Holder extends DependantScriptHolder<ColumnToIntScript> implements ColumnToIntScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
				super(new TemplateScriptParser<>(ColumnToIntScript.class, usage));
			}

			@Override
			public int compute(ScriptedColumn column) {
				try {
					return this.script.compute(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}

	public static interface ColumnYToIntScript extends Script {

		public abstract int compute(ScriptedColumn column, int y);

		public static class Holder extends DependantScriptHolder<ColumnYToIntScript> implements ColumnYToIntScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
				super(new TemplateScriptParser<>(ColumnYToIntScript.class, usage));
			}

			@Override
			public int compute(ScriptedColumn column, int y) {
				try {
					return this.script.compute(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}

	public static interface ColumnToBooleanScript extends Script {

		public abstract boolean compute(ScriptedColumn column);

		public static class Holder extends DependantScriptHolder<ColumnToBooleanScript> implements ColumnToBooleanScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
				super(new TemplateScriptParser<>(ColumnToBooleanScript.class, usage));
			}

			@Override
			public boolean compute(ScriptedColumn column) {
				try {
					return this.script.compute(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}
}