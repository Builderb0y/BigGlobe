package builderb0y.bigglobe.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.mixinInterfaces.ColumnValueDisplayer;

@Environment(EnvType.CLIENT)
public class DisplayColumnsClientCommand {

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(
			ClientCommandManager
			.literal(BigGlobeMod.MODID + ":displayColumns")
			.requires(source -> getGenerator() != null)
			.executes(context -> {
				ColumnValueDisplayer generator = getGenerator();
				if (generator != null) {
					generator.bigglobe_setDisplayedColumnValues(ColumnValue.ARRAY_FACTORY.empty());
					return 1;
				}
				else {
					return 0;
				}
			})
			.then(
				ClientCommandManager.literal("*").executes(context -> {
					ColumnValueDisplayer generator = getGenerator();
					if (generator != null) {
						WorldColumn column = getColumn(0, 0);
						generator.bigglobe_setDisplayedColumnValues(
							ColumnValue
							.REGISTRY
							.stream()
							.filter(value -> value.accepts(column))
							.sorted(Comparator.comparing(ColumnValue::getName))
							.toArray(ColumnValue.ARRAY_FACTORY)
						);
						return 1;
					}
					else {
						return 0;
					}
				})
			)
			.then(
				ClientCommandManager
				.argument("filter", new FilterF3Argument())
				.executes(context -> {
					ColumnValueDisplayer generator = getGenerator();
					if (generator != null) {
						generator.bigglobe_setDisplayedColumnValues(context.getArgument("filter", ColumnValue[].class));
						return 1;
					}
					else {
						return 0;
					}
				})
			)
		);
	}

	public static @Nullable ColumnValueDisplayer getGenerator() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getServer() == null || client.world == null) return null;
		ServerWorld world = client.getServer().getWorld(client.world.getRegistryKey());
		if (world == null) return null;
		return world.getChunkManager().getChunkGenerator() instanceof ColumnValueDisplayer displayer ? displayer : null;
	}

	public static @Nullable WorldColumn getColumn(int x, int z) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getServer() == null || client.world == null) return null;
		ServerWorld world = client.getServer().getWorld(client.world.getRegistryKey());
		if (world == null) return null;
		return WorldColumn.forWorld(world, x, z);
	}

	@Environment(EnvType.CLIENT)
	public static class FilterF3Argument implements ArgumentType<ColumnValue<?>[]> {

		public static final Pattern WHITESPACE = Pattern.compile("\\s+");

		@Override
		public ColumnValue<?>[] parse(StringReader reader) throws CommandSyntaxException {
			String[] split = WHITESPACE.split(reader.getRemaining());
			reader.setCursor(reader.getTotalLength());
			if (split.length == 0) {
				throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
			}
			return filter(split);
		}

		public static ColumnValue<?>[] filter(String[] query) {
			ColumnValueDisplayer displayer = getGenerator();
			WorldColumn column = getColumn(0, 0);
			if (displayer == null || column == null) {
				return ColumnValue.ARRAY_FACTORY.empty();
			}

			@SuppressWarnings("unchecked")
			List<ColumnValue<?>>[] matches = new List[query.length];
			int count = 0;
			for (ColumnValue<?> value : ColumnValue.REGISTRY) {
				if (value.accepts(column)) {
					int matchIndex = getMatchIndex(query, value);
					if (matchIndex >= 0) {
						List<ColumnValue<?>> list = matches[matchIndex];
						if (list == null) {
							list = matches[matchIndex] = new ArrayList<>(8);
						}
						list.add(value);
						count++;
					}
				}
			}
			List<ColumnValue<?>> flattened = new ArrayList<>(count + query.length - 1);
			for (List<ColumnValue<?>> list : matches) {
				if (list != null) flattened.addAll(list);
				if (!flattened.isEmpty() && flattened.get(flattened.size() - 1) != null) flattened.add(null);
			}
			if (!flattened.isEmpty() && flattened.get(flattened.size() - 1) == null) {
				flattened.remove(flattened.size() - 1);
			}
			return flattened.toArray(new ColumnValue<?>[flattened.size()]);
		}

		/**
		returns true if name contains all the characters inside request in the same order as request, false otherwise.
		name may contain additional characters not contained within request without affecting the result.
		for example:
			matches("12345", "12345") returns true
			matches("234", "12345") returns true
			matches("135", "12345") returns true
			matches("6", "12345") returns false
			matches("54321", "12345") returns false
		*/
		public static boolean matches(String request, String name) {
			int start = 0;
			for (int i = 0, length = request.length(); i < length; i++) {
				int newStart = name.indexOf(request.charAt(i), start);
				if (newStart >= 0) start = newStart + 1;
				else return false;
			}
			return true;
		}

		/**
		tries to match the value's name against multiple requests.
		returns the index of the first request which matches the value's name,
		or -1 if none of the requests match the value's name.
		*/
		public static int getMatchIndex(String[] requests, ColumnValue<?> value) {
			String name = value.getName();
			for (int i = 0, length = requests.length; i < length; i++) {
				if (matches(requests[i], name)) {
					return i;
				}
			}
			return -1;
		}

		@Override
		public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
			ColumnValueDisplayer displayer = getGenerator();
			WorldColumn column = getColumn(0, 0);
			if (displayer == null || column == null) return Suggestions.empty();
			String current = builder.getRemaining();
			int lastSpace = current.length();
			while (lastSpace-- != 0) {
				if (Character.isWhitespace(current.charAt(lastSpace))) break;
			}
			String firstPart = current.substring(0, lastSpace + 1);
			String request = current.substring(lastSpace + 1);
			for (ColumnValue<?> value : ColumnValue.REGISTRY) {
				String name = value.getName();
				if (value.accepts(column) && matches(request, name)) {
					builder.suggest(firstPart + name);
				}
			}
			return builder.buildFuture();
		}
	}
}