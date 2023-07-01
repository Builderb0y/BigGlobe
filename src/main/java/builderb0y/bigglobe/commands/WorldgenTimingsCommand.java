package builderb0y.bigglobe.commands;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.versions.ServerCommandSourceVersions;

public class WorldgenTimingsCommand {

	public static final String PREFIX = "commands." + BigGlobeMod.MODID + ".profiler.";

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":worldgenProfiler")
			.requires(source -> (
				source.getWorld().getChunkManager().getChunkGenerator() instanceof BigGlobeChunkGenerator &&
				(source.getServer().isSingleplayer() || source.hasPermissionLevel(2))
			))
			.then(
				CommandManager.literal("start").executes(context -> {
					BigGlobeChunkGenerator generator = (BigGlobeChunkGenerator)(context.getSource().getWorld().getChunkManager().getChunkGenerator());
					if (generator.profiler.timings == null) {
						generator.profiler.timings = new ConcurrentHashMap<>(32);
						ServerCommandSourceVersions.sendFeedback(context.getSource(), () -> Text.translatable(PREFIX + "started", context.getSource().getWorld().getRegistryKey().getValue().toString()), true);
						return 1;
					}
					else {
						context.getSource().sendError(Text.translatable(PREFIX + "alreadyRunning"));
						return 0;
					}
				})
			)
			.then(
				CommandManager.literal("stop").executes(context -> {
					BigGlobeChunkGenerator generator = (BigGlobeChunkGenerator)(context.getSource().getWorld().getChunkManager().getChunkGenerator());
					ConcurrentHashMap<Object, Long> timings = generator.profiler.timings;
					if (timings != null) {
						ServerCommandSourceVersions.sendFeedback(context.getSource(), () -> Text.translatable(PREFIX + "stopped", context.getSource().getWorld().getRegistryKey().getValue().toString()), true);
						dump(context.getSource(), timings);
						generator.profiler.timings = null;
						return 1;
					}
					else {
						context.getSource().sendError(Text.translatable(PREFIX + "notRunning"));
						return 0;
					}
				})
			)
			.then(
				CommandManager.literal("restart").executes(context -> {
					BigGlobeChunkGenerator generator = (BigGlobeChunkGenerator)(context.getSource().getWorld().getChunkManager().getChunkGenerator());
					ConcurrentHashMap<Object, Long> timings = generator.profiler.timings;
					if (timings != null) {
						ServerCommandSourceVersions.sendFeedback(context.getSource(), () -> Text.translatable(PREFIX + "restarted", context.getSource().getWorld().getRegistryKey().getValue().toString()), true);
						dump(context.getSource(), timings);
						timings.clear();
						return 1;
					}
					else {
						context.getSource().sendError(Text.translatable(PREFIX + "notRunning"));
						return 0;
					}
				})
			)
			.then(
				CommandManager.literal("dump").executes(context -> {
					BigGlobeChunkGenerator generator = (BigGlobeChunkGenerator)(context.getSource().getWorld().getChunkManager().getChunkGenerator());
					ConcurrentHashMap<Object, Long> timings = generator.profiler.timings;
					if (timings != null) {
						dump(context.getSource(), timings);
						return 1;
					}
					else {
						context.getSource().sendError(Text.translatable(PREFIX + "notRunning"));
						return 0;
					}
				})
			)
		);
	}

	public static void dump(ServerCommandSource source, ConcurrentHashMap<Object, Long> map) {
		ServerCommandSourceVersions.sendFeedback(source, () -> Text.translatable(PREFIX + "dump.header"), false);
		long sum = map.values().stream().mapToLong(Long::longValue).sum();
		map.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(entry -> {
			ServerCommandSourceVersions.sendFeedback(
				source,
				() -> (
					Text.literal(entry.getKey().toString())
					.formatted(Formatting.GREEN)
					.append(
						Text.literal(": ")
						.formatted(Formatting.WHITE)
					)
					.append(
						Text.literal(String.format("%,d ns", entry.getValue()))
						.formatted(Formatting.AQUA)
					)
					.append(
						Text.literal(" (")
						.formatted(Formatting.WHITE)
					)
					.append(
						Text.literal(String.format("%,.1f%%", entry.getValue().doubleValue() * 100.0D / sum))
						.formatted(Formatting.BLUE)
					)
					.append(
						Text.literal(")")
						.formatted(Formatting.WHITE)
					)
				),
				false
			);
		});
	}
}