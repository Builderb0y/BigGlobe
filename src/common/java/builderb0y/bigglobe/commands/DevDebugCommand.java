package builderb0y.bigglobe.commands;

import java.util.Arrays;
import java.util.Comparator;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.Vec3;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.features.OreFeature;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.UnregisteredObjectException;
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
						.then(
							Commands
								.argument("ore", IdentifierArgument.id())
								.executes((CommandContext<CommandSourceStack> context) -> {
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
								})
						)
						.executes((CommandContext<CommandSourceStack> context) -> {
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
						})
				)
		);
	}
}