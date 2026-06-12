package builderb0y.bigglobe.commands;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ConfiguredColumnFactory;
import builderb0y.bigglobe.features.OreFeature;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.management.FlatStructureLocator;
import builderb0y.bigglobe.structures.management.FlatStructureLocator.StructureCaches;
import builderb0y.bigglobe.structures.management.FlatStructureLocator.StructurePos;
import builderb0y.bigglobe.structures.management.StructureLocator;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor.ManyStructuresManyBoxes;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor.ManyStructuresOneBox;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.Streamable;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.RegistryVersions;

public class DevDebugCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return;
		dispatcher.register(
			Commands
			.literal(BigGlobeMod.MODID + ":debug")
			.then(
				Commands
				.literal("ore_spawn_chance")
				.executes(DevDebugCommand::displayMultiOreChance)
				.then(
					Commands
					.argument("ore", IdentifierArgument.id())
					.executes(DevDebugCommand::displaySingleOreChance)
				)
			)
			.then(
				Commands
				.literal("structure_caches")
				.then(
					Commands
					.literal("block")
					.executes(DevDebugCommand::displayBlockStructures)
				)
				.then(
					Commands
					.literal("chunk")
					.executes(DevDebugCommand::displayChunkStructures)
				)
				.then(
					Commands
					.literal("structurePos")
					.executes(DevDebugCommand::displayStructurePosStructures)
				)
				.then(
					Commands
					.literal("clear")
					.executes(DevDebugCommand::clearStructures)
				)
			)
		);
	}

	public static int displayBlockStructures(CommandContext<CommandSourceStack> context) {
		return displayStructures(context, new BoundingBox(BlockPos.containing(context.getSource().getPosition())));
	}

	public static int displayChunkStructures(CommandContext<CommandSourceStack> context) {
		return displayStructures(context, WorldUtil.chunkBox(ChunkPos.containing(BlockPos.containing(context.getSource().getPosition())), context.getSource().getLevel()));
	}

	public static int displayStructurePosStructures(CommandContext<CommandSourceStack> context) {
		return displayStructures(context, StructurePos.fromBlock(BlockPos.containing(context.getSource().getPosition())).toArea(context.getSource().getLevel()));
	}

	public static int displayStructures(CommandContext<CommandSourceStack> context, BoundingBox box) {
		ServerLevel world = context.getSource().getLevel();
		if (
			world
			.getChunkSource()
			.getGenerator()
			instanceof BigGlobeScriptedChunkGenerator generator &&
			generator
			.structureLocator()
			instanceof FlatStructureLocator locator
		) {
			class SecretMultiSearch extends ManyStructuresManyBoxes {

				public SecretMultiSearch(Streamable<Holder<Structure>> structures) {
					super(structures);
				}

				@Override
				public BoundingBox getAreaFor(Holder<Structure> structure) {
					return box;
				}

				@Override
				public WhatToSearchFor filter(Streamable<Holder<Structure>> structures) {
					return new SecretMultiSearch(structures);
				}
			}
			StructureCaches caches = locator.getCaches(StructurePos.fromBlock(box.minX(), box.minZ()));
			ConfiguredColumnFactory columnFactory = generator.configuredColumnFactory(world, ColumnUsage.GENERIC.normalHints());
			BigGlobeMod.LOGGER.info("SINGLE BOX:");
			dumpStructures(
				locator,
				caches,
				new StructureLocator.Params(
					generator,
					columnFactory,
					world,
					new ManyStructuresOneBox(
						locator.allStructures(),
						box
					)
				)
			);
			BigGlobeMod.LOGGER.info("MULTI BOX:");
			dumpStructures(
				locator,
				caches,
				new StructureLocator.Params(
					generator,
					columnFactory,
					world,
					new SecretMultiSearch(
						locator.allStructures()
					)
				)
			);
			return 1;
		}
		else {
			context.getSource().sendFailure(Component.literal("Not a big globe world"));
			return 0;
		}
	}

	public static void dumpStructures(FlatStructureLocator locator, StructureCaches caches, StructureLocator.Params params) {
		BigGlobeMod.LOGGER.info("\tUNFILTERED:");
		caches.getUnfiltered(params).map(Objects::toString).map("\t\t"::concat).forEach(BigGlobeMod.LOGGER::info);
		BigGlobeMod.LOGGER.info("\tFILTERED:");
		caches.getFiltered(params).map(Objects::toString).map("\t\t"::concat).forEach(BigGlobeMod.LOGGER::info);
		BigGlobeMod.LOGGER.info("\tINTERSECTING CACHES:");
		caches.getIntersecting(params).map(Objects::toString).map("\t\t"::concat).forEach(BigGlobeMod.LOGGER::info);
		BigGlobeMod.LOGGER.info("\tINTERSECTING POSITION:");
		locator.getStructuresIntersecting(params).map(StructureStartWrapper::originalID).map(Identifier::toString).map("\t\t"::concat).forEach(BigGlobeMod.LOGGER::info);
	}

	public static int clearStructures(CommandContext<CommandSourceStack> context) {
		if (context.getSource().getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.structureLocator() instanceof FlatStructureLocator locator) {
			locator.caches.clear();
			locator.mostRecentCache = null;
			return 1;
		}
		return 0;
	}

	public static int displaySingleOreChance(CommandContext<CommandSourceStack> context) {
		if (context.getSource().getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			ConfiguredFeature<?, ?> ore = (
				RegistryVersions.getRegistry(
					context
					.getSource()
					.getServer()
					.registryAccess(),
					Registries.CONFIGURED_FEATURE
				)
				.getValue(context.getArgument("ore", Identifier.class))
			);
			if (ore != null && ore.config() instanceof OreFeature.Config config) {
				Vec3 pos = context.getSource().getPosition();
				ScriptedColumn column = generator.newColumn(
					context.getSource().getLevel(),
					BigGlobeMath.floorI(pos.x),
					BigGlobeMath.floorI(pos.z),
					ColumnUsage.GENERIC.normalHints()
				);
				context.getSource().sendSuccess(
					() -> Component.literal(
						Double.toString(
							config.chance.get(
								column,
								BigGlobeMath.floorI(pos.y)
							)
						)
					),
					false
				);
				return 1;
			}
			else {
				context.getSource().sendFailure(Component.literal("Not an ore: " + ore));
			}
		}
		else {
			context.getSource().sendFailure(Component.literal("Not a big globe world"));
		}
		return 0;
	}

	public static int displayMultiOreChance(CommandContext<CommandSourceStack> context) {
		if (context.getSource().getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			Vec3 pos = context.getSource().getPosition();
			context.getSource().sendSuccess(
				() -> Component.literal(
					BigGlobeMath.floorI(pos.x) + ", " +
					BigGlobeMath.floorI(pos.y) + ", " +
					BigGlobeMath.floorI(pos.z)
				),
				false
			);
			ScriptedColumn column = generator.newColumn(
				context.getSource().getLevel(),
				BigGlobeMath.floorI(pos.x),
				BigGlobeMath.floorI(pos.z),
				ColumnUsage.GENERIC.normalHints()
			);
			Arrays
				.stream(generator.feature_dispatcher.rock_replacers)
				.flatMap(DelayedEntryList::entryStream)
				.filter((Holder<ConfiguredFeature<?, ?>> entry) -> (
					entry.value().config() instanceof OreFeature.Config
				))
				.sorted(Comparator.comparing(UnregisteredObjectException::getID))
				.forEachOrdered((Holder<ConfiguredFeature<?, ?>> entry) -> {
					context.getSource().sendSuccess(
						() -> Component.literal(
							UnregisteredObjectException.getID(entry) + ": " + (
								((OreFeature.Config)(entry.value().config())).chance.get(
									column,
									BigGlobeMath.floorI(pos.y)
								)
							)
						),
						false
					);
				});
			return 1;
		}
		else {
			context.getSource().sendFailure(Component.literal("Not a big globe world"));
			return 0;
		}
	}
}