package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.versions.TracyWrapper;

public class TracyCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		if (!TracyWrapper.ENABLED) return;
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":tracy")
			.requires((ServerCommandSource source) -> !source.isExecutedByPlayer() || !source.getServer().isDedicated())
			.then(
				CommandManager
				.literal("columngen")
				.executes((CommandContext<ServerCommandSource> context) -> {
					BigGlobeScriptedChunkGenerator.TRACE_OPERATION.set(true);
					return 1;
				})
			)
		);
	}
}