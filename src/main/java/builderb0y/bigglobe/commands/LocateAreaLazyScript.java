package builderb0y.bigglobe.commands;

import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ColumnPredicate;
import builderb0y.scripting.environments.MutableScriptEnvironment.KeywordHandler;
import builderb0y.scripting.parsing.ScriptParsingException;

public class LocateAreaLazyScript implements ColumnPredicate {

	public @Nullable ColumnPredicate.Parser parser;
	public Set<ColumnValue<?>> usedValues;
	public @Nullable ColumnPredicate script;

	public LocateAreaLazyScript(String script) throws ScriptParsingException {
		this.parser = new ColumnPredicate.Parser(script);
		Map<String, KeywordHandler> keywords = this.parser.environment.mutable().keywords;
		keywords.remove("class");
		keywords.remove("while");
		keywords.remove("until");
		keywords.remove("do");
		keywords.remove("repeat");
		keywords.remove("for");
		keywords.remove("block");
		keywords.remove("break");
		keywords.remove("continue");
		this.parser.toBytecode();
		this.usedValues = this.parser.builder.usedValues;
	}

	public ColumnPredicate getScript() {
		if (this.script == null) try {
			this.script = this.parser.toScript();
			this.parser = null; //free for GC.
		}
		catch (ScriptParsingException exception) {
			throw new RuntimeException(exception);
		}
		return this.script;
	}

	@Override
	public boolean test(WorldColumn column) {
		return this.getScript().test(column);
	}

	@Override
	public String getSource() {
		return this.getScript().getSource();
	}

	public static class Argument implements ArgumentType<LocateAreaLazyScript> {

		@Override
		public LocateAreaLazyScript parse(StringReader reader) throws CommandSyntaxException {
			try {
				String script = reader.getRemaining();
				reader.setCursor(reader.getTotalLength());
				return new LocateAreaLazyScript(script);
			}
			catch (ScriptParsingException exception) {
				throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(exception.getMessage());
			}
		}
	}
}