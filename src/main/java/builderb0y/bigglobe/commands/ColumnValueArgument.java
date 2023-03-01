package builderb0y.bigglobe.commands;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;

public class ColumnValueArgument implements ArgumentType<ColumnValue<?>> {

	@Override
	public ColumnValue<?> parse(StringReader reader) throws CommandSyntaxException {
		//match logic from Identifier.fromCommandInput().
		int start = reader.getCursor();
		while (reader.canRead() && Identifier.isCharValid(reader.peek())) reader.skip();
		String name = reader.getString().substring(start, reader.getCursor());
		ColumnValue<?> value = ColumnValue.get(name);
		if (value != null) return value;
		else throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (context.getSource() instanceof ServerCommandSource source) {
			WorldColumn column = WorldColumn.forWorld(source.getWorld(), 0, 0);
			return CommandSource.suggestIdentifiers(
				ColumnValue
				.REGISTRY
				.getEntrySet()
				.stream()
				.filter(entry -> entry.getValue().accepts(column))
				.map(entry -> entry.getKey().getValue()),
				builder
			);
		}
		else if (context.getSource() instanceof CommandSource source) {
			return source.getCompletions(context);
		}
		else {
			return CommandSource.suggestIdentifiers(ColumnValue.REGISTRY.getIds(), builder);
		}
	}
}