package builderb0y.bigglobe.commands;

import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.scripting.environments.MutableScriptEnvironment.KeywordHandler;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class LocateNoiseLazyScript implements ColumnYToDoubleScript {

	public @Nullable ScriptParser<ColumnYToDoubleScript> parser;
	public Set<ColumnValue<?>> usedValues;
	public @Nullable ColumnYToDoubleScript script;

	public LocateNoiseLazyScript(String script) throws ScriptParsingException {
		this.parser = new ScriptParser<>(ColumnYToDoubleScript.class, script);
		this.usedValues = Holder.setupParser(this.parser).usedValues;
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
	}

	public ColumnYToDoubleScript getScript() {
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
	public double evaluate(WorldColumn column, double y) {
		return this.getScript().evaluate(column, y);
	}

	@Override
	public String getSource() {
		return this.getScript().getSource();
	}

	public static class Argument implements ArgumentType<LocateNoiseLazyScript> {

		@Override
		public LocateNoiseLazyScript parse(StringReader reader) throws CommandSyntaxException {
			try {
				String script = reader.getRemaining();
				reader.setCursor(reader.getTotalLength());
				return new LocateNoiseLazyScript(script);
			}
			catch (ScriptParsingException exception) {
				throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(exception.getMessage());
			}
		}
	}
}