package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToDoubleScript;
import builderb0y.bigglobe.commands.LocateMinMaxCommand.CompareMode;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.AdditiveRecurrenceIterator2D;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator2D;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.SourceScriptUsage;

public class LocateCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":locate")
			.requires((ServerCommandSource source) -> source.getWorld().getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator && source.hasPermissionLevel(4))
			.then(
				CommandManager.literal("nearest").then(
					CommandManager
					.argument("script", StringArgumentType.greedyString())
					.executes((CommandContext<ServerCommandSource> context) -> {
						ColumnToBooleanScript.Holder script = new ColumnToBooleanScript.Holder(
							new SourceScriptUsage(context.getArgument("script", String.class))
						);
						if (!compile(script, context.getSource())) return 0;
						LocateNearestCommand command = new LocateNearestCommand(context.getSource(), script);
						context.getSource().sendFeedback(() -> Text.translatable("commands.bigglobe.locate.searching"), false);
						command.start(context.getInput());
						return 1;
					})
				)
			)
			.then(
				CommandManager.literal("largest").then(
					CommandManager.argument("range", IntegerArgumentType.integer(0, 30_000_000)).then(
						CommandManager
						.argument("script", StringArgumentType.greedyString())
						.executes((CommandContext<ServerCommandSource> context) -> {
							ServerCommandSource source = context.getSource();
							ColumnToBooleanScript.Holder script = new ColumnToBooleanScript.Holder(
								new SourceScriptUsage(context.getArgument("script", String.class))
							);
							if (!compile(script, source)) return 0;
							LocateLargestCommand command = new LocateLargestCommand(
								source,
								iterator(context),
								script,
								context.getArgument("range", int.class)
							);
							source.sendFeedback(() -> Text.translatable("commands.bigglobe.locate.searching"), false);
							command.start(context.getInput());
							return 1;
						})
					)
				)
			)
			.then(
				CommandManager.literal("min").then(
					CommandManager.argument("range", IntegerArgumentType.integer(0, 30_000_000)).then(
						CommandManager
						.argument("script", StringArgumentType.greedyString())
						.executes((CommandContext<ServerCommandSource> context) -> {
							ServerCommandSource source = context.getSource();
							ColumnToDoubleScript.Holder script = new ColumnToDoubleScript.Holder(
								new SourceScriptUsage(context.getArgument("script", String.class))
							);
							if (!compile(script, source)) return 0;
							LocateMinMaxCommand command = new LocateMinMaxCommand(
								source,
								iterator(context),
								script,
								CompareMode.MIN,
								context.getArgument("range", int.class)
							);
							source.sendFeedback(() -> Text.translatable("commands.bigglobe.locate.searching"), false);
							command.start(context.getInput());
							return 1;
						})
					)
				)
			)
			.then(
				CommandManager.literal("max").then(
					CommandManager.argument("range", IntegerArgumentType.integer(0, 30_000_000)).then(
						CommandManager
						.argument("script", StringArgumentType.greedyString())
						.executes((CommandContext<ServerCommandSource> context) -> {
							ServerCommandSource source = context.getSource();
							ColumnToDoubleScript.Holder script = new ColumnToDoubleScript.Holder(
								new SourceScriptUsage(context.getArgument("script", String.class))
							);
							if (!compile(script, source)) return 0;
							LocateMinMaxCommand command = new LocateMinMaxCommand(
								source,
								iterator(context),
								script,
								CompareMode.MAX,
								context.getArgument("range", int.class)
							);
							source.sendFeedback(() -> Text.translatable("commands.bigglobe.locate.searching"), false);
							command.start(context.getInput());
							return 1;
						})
					)
				)
			)
		);
	}

	public static BoundedPointIterator2D iterator(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		Vec3d centerPos = source.getPosition();
		int
			centerX = BigGlobeMath.floorI(centerPos.x),
			centerZ = BigGlobeMath.floorI(centerPos.z),
			range   = context.getArgument("range", int.class);
		return new AdditiveRecurrenceIterator2D(
			centerX - range,
			centerZ - range,
			centerX + range,
			centerZ + range,
			source.getWorld().random.nextDouble(),
			source.getWorld().random.nextDouble()
		);
	}

	public static boolean compile(ScriptHolder<?> script, ServerCommandSource source) {
		try {
			script.compile(
				(
					(BigGlobeScriptedChunkGenerator)(
						source
						.getWorld()
						.getChunkManager()
						.getChunkGenerator()
					)
				)
				.columnEntryRegistry
			);
			return true;
		}
		catch (ScriptParsingException exception) {
			exception.getLocalizedMessage().lines().map(Text::literal).forEachOrdered(source::sendError);
			return false;
		}
	}
}