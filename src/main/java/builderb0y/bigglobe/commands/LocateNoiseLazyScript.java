package builderb0y.bigglobe.commands;

import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.KeywordHandler;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class LocateNoiseLazyScript implements ColumnYToDoubleScript {

	public @Nullable ScriptParser<ColumnYToDoubleScript> parser;
	public Set<ColumnValue<?>> usedValues;
	public @Nullable ColumnYToDoubleScript script;

	public LocateNoiseLazyScript(String script) throws ScriptParsingException {
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
		this.parser = (
			new ScriptParser<>(ColumnYToDoubleScript.class, script, null)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(builder.build())
		);
		this.parser.toBytecode();
		this.usedValues = builder.usedValues;
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

	@Override
	public @Nullable String getDebugName() {
		return this.getScript().getDebugName();
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