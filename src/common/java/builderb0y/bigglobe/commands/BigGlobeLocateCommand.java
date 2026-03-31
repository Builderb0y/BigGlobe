package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument.Result;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
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

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands
				.literal(BigGlobeMod.MODID + ":locate")
				.requires(CommandVersions.levelPredicate(4).and((CommandSourceStack source) -> BigGlobeCommands.generator(source) != null))
				.then(
					Commands.literal("nearest").then(
						Commands.argument("script", StringArgumentType.greedyString()).executes(
							(CommandContext<CommandSourceStack> context) -> {
								ColumnToBooleanScript.Holder script = new ColumnToBooleanScript.Holder(
									new SourceScriptUsage(context.getArgument("script", String.class))
								);
								if (!compile(script, context.getSource())) return 0;
								LocateNearestCommand command = new LocateNearestCommand(context.getSource(), script);
								context.getSource().sendSuccess(() -> Component.translatable("commands.bigglobe.locate.searching"), false);
								command.start(context.getInput());
								return 1;
							}
						)
					)
				)
				.then(
					Commands.literal("largest").then(
						Commands.argument("range", IntegerArgumentType.integer(0, 30_000_000)).then(
							Commands.argument("script", StringArgumentType.greedyString()).executes(
								(CommandContext<CommandSourceStack> context) -> {
									CommandSourceStack source = context.getSource();
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
									source.sendSuccess(() -> Component.translatable("commands.bigglobe.locate.searching"), false);
									command.start(context.getInput());
									return 1;
								}
							)
						)
					)
				)
				.then(
					Commands.literal("min").then(
						Commands.argument("range", IntegerArgumentType.integer(0, 30_000_000)).then(
							Commands.argument("script", StringArgumentType.greedyString()).executes(
								(CommandContext<CommandSourceStack> context) -> {
									CommandSourceStack source = context.getSource();
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
									source.sendSuccess(() -> Component.translatable("commands.bigglobe.locate.searching"), false);
									command.start(context.getInput());
									return 1;
								}
							)
						)
					)
				)
				.then(
					Commands.literal("max").then(
						Commands.argument("range", IntegerArgumentType.integer(0, 30_000_000)).then(
							Commands.argument("script", StringArgumentType.greedyString()).executes(
								(CommandContext<CommandSourceStack> context) -> {
									CommandSourceStack source = context.getSource();
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
									source.sendSuccess(() -> Component.translatable("commands.bigglobe.locate.searching"), true);
									command.start(context.getInput());
									return 1;
								}
							)
						)
					)
				)
				.then(
					Commands.literal("structures").then(
						Commands.argument("range", IntegerArgumentType.integer(0, 10_000)).then(
							Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE)).executes(
								(CommandContext<CommandSourceStack> context) -> {
									BigGlobeScriptedChunkGenerator generator = BigGlobeCommands.generator(context);
									if (!(generator.structureManager instanceof ActiveStructureManager)) {
										context.getSource().sendFailure(Component.translatable("commands.bigglobe.locate.structure.fail.structures_disabled"));
										return 0;
									}
									int range = context.getArgument("range", Integer.class);
									Result<Structure> predicate = ResourceOrTagKeyArgument.getResourceOrTagKey(context, "structure", Registries.STRUCTURE, LocateCommand.ERROR_STRUCTURE_INVALID);
									Registry<Structure> registry = RegistryVersions.getRegistry(context.getSource().registryAccess(), Registries.STRUCTURE);
									HolderSet<Structure> tag = LocateCommand.getHolders(predicate, registry).orElse(null);
									ChunkGeneratorStructureState calculator = context.getSource().getLevel().getChunkSource().getGeneratorState();
									Vec3 position = context.getSource().getPosition();
									LocateStructuresCommand command = new LocateStructuresCommand(context.getSource(), tag, calculator, BigGlobeMath.floorI(position.x * 0.0625D), BigGlobeMath.floorI(position.z * 0.0625D), range);
									context.getSource().sendSuccess(() -> Component.translatable("commands.bigglobe.locate.searching"), true);
									command.start(context.getInput());
									return 1;
								}
							)
						)
					)
				)
		);
	}

	public static BoundedPointIterator2D iterator(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		Vec3 centerPos = source.getPosition();
		int
			centerX = BigGlobeMath.floorI(centerPos.x),
			centerZ = BigGlobeMath.floorI(centerPos.z),
			range = context.getArgument("range", int.class);
		return new AdditiveRecurrenceIterator2D(
			centerX - range,
			centerZ - range,
			centerX + range,
			centerZ + range,
			source.getLevel().getRandom().nextDouble(),
			source.getLevel().getRandom().nextDouble()
		);
	}

	public static boolean compile(ScriptHolder<?> script, CommandSourceStack source) {
		try {
			script.compile(
				(
					(BigGlobeScriptedChunkGenerator)(
						source
							.getLevel()
							.getChunkSource()
							.getGenerator()
					)
				)
					.columnEntryRegistry
			);
			return true;
		}
		catch (ScriptParsingException exception) {
			exception.getLocalizedMessage().lines().map(Component::literal).forEachOrdered(source::sendFailure);
			return false;
		}
	}
}