package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.versions.TracyWrapper;

public class TracyCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (!TracyWrapper.ENABLED) return;
		dispatcher.register(
			Commands
				.literal(BigGlobeMod.MODID + ":tracy")
				.requires((CommandSourceStack source) -> !source.isPlayer() || !source.getServer().isDedicatedServer())
				.then(
					Commands
						.literal("columngen")
						.executes((CommandContext<CommandSourceStack> context) -> {
							BigGlobeScriptedChunkGenerator.TRACE_OPERATION.set(true);
							return 1;
						})
				)
		);
	}
}