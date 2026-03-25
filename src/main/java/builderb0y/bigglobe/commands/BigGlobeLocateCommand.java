package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.command.argument.RegistryPredicateArgumentType.RegistryPredicate;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryList.ListBacked;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.LocateCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToDoubleScript;
import builderb0y.bigglobe.commands.LocateMinMaxCommand.CompareMode;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.AdditiveRecurrenceIterator2D;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator2D;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.structures.ActiveStructureManager;
import builderb0y.bigglobe.versions.CommandVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.SourceScriptUsage;

public class BigGlobeLocateCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":locate")
			.requires(CommandVersions.levelPredicate(4).and((ServerCommandSource source) -> BigGlobeCommands.generator(source) != null))
			.then(
				CommandManager.literal("nearest").then(
					CommandManager.argument("script", StringArgumentType.greedyString()).executes(
						(CommandContext<ServerCommandSource> context) -> {
							ColumnToBooleanScript.Holder script = new ColumnToBooleanScript.Holder(
								new SourceScriptUsage(context.getArgument("script", String.class))
							);
							if (!compile(script, context.getSource())) return 0;
							LocateNearestCommand command = new LocateNearestCommand(context.getSource(), script);
							context.getSource().sendFeedback(() -> Text.translatable("commands.bigglobe.locate.searching"), false);
							command.start(context.getInput());
							return 1;
						}
					)
				)
			)
			.then(
				CommandManager.literal("largest").then(
					CommandManager.argument("range", IntegerArgumentType.integer(0, 30_000_000)).then(
						CommandManager.argument("script", StringArgumentType.greedyString()).executes(
							(CommandContext<ServerCommandSource> context) -> {
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
							}
						)
					)
				)
			)
			.then(
				CommandManager.literal("min").then(
					CommandManager.argument("range", IntegerArgumentType.integer(0, 30_000_000)).then(
						CommandManager.argument("script", StringArgumentType.greedyString()).executes(
							(CommandContext<ServerCommandSource> context) -> {
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
							}
						)
					)
				)
			)
			.then(
				CommandManager.literal("max").then(
					CommandManager.argument("range", IntegerArgumentType.integer(0, 30_000_000)).then(
						CommandManager.argument("script", StringArgumentType.greedyString()).executes(
							(CommandContext<ServerCommandSource> context) -> {
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
								source.sendFeedback(() -> Text.translatable("commands.bigglobe.locate.searching"), true);
								command.start(context.getInput());
								return 1;
							}
						)
					)
				)
			)
			.then(
				CommandManager.literal("structures").then(
					CommandManager.argument("range", IntegerArgumentType.integer(0, 10_000)).then(
						CommandManager.argument("structure", RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE)).executes(
							(CommandContext<ServerCommandSource> context) -> {
								BigGlobeScriptedChunkGenerator generator = BigGlobeCommands.generator(context);
								if (!(generator.structureManager instanceof ActiveStructureManager)) {
									context.getSource().sendError(Text.translatable("commands.bigglobe.locate.structure.fail.structures_disabled"));
									return 0;
								}
								int range = context.getArgument("range", Integer.class);
								RegistryPredicate<Structure> predicate = RegistryPredicateArgumentType.getPredicate(context, "structure", RegistryKeys.STRUCTURE, LocateCommand.STRUCTURE_INVALID_EXCEPTION);
								Registry<Structure> registry = RegistryVersions.getRegistry(context.getSource().getRegistryManager(), RegistryKeys.STRUCTURE);
								RegistryEntryList<Structure> tag = LocateCommand.getStructureListForPredicate(predicate, registry).orElse(null);
								StructurePlacementCalculator calculator = context.getSource().getWorld().getChunkManager().getStructurePlacementCalculator();
								Vec3d position = context.getSource().getPosition();
								LocateStructuresCommand command = new LocateStructuresCommand(context.getSource(), tag, calculator, BigGlobeMath.floorI(position.x * 0.0625D), BigGlobeMath.floorI(position.z * 0.0625D), range);
								context.getSource().sendFeedback(() -> Text.translatable("commands.bigglobe.locate.searching"), true);
								command.start(context.getInput());
								return 1;
							}
						)
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