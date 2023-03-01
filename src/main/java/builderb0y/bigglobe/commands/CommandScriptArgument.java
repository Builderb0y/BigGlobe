package builderb0y.bigglobe.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import builderb0y.bigglobe.commands.CommandScript.LazyCommandScript;
import builderb0y.scripting.parsing.ScriptParsingException;

public class CommandScriptArgument implements ArgumentType<LazyCommandScript> {

	@Override
	public LazyCommandScript parse(StringReader reader) throws CommandSyntaxException {
		try {
			String script = reader.getRemaining();
			reader.setCursor(reader.getTotalLength());
			return new LazyCommandScript(script);
		}
		catch (ScriptParsingException exception) {
			throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(exception.getMessage());
		}
	}
}