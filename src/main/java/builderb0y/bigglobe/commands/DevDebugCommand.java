package builderb0y.bigglobe.commands;

import java.util.Arrays;
import java.util.Comparator;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.features.OreFeature;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.util.TagOrObject;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class DevDebugCommand {

	public static final boolean ENABLED = false;

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		if (!ENABLED) return;
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":debug")
			.then(
				CommandManager
				.literal("ore_spawn_chance")
				.then(
					CommandManager
					.argument("ore", IdentifierArgumentType.identifier())
					.executes((CommandContext<ServerCommandSource> context) -> {
						if (context.getSource().getWorld().getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
							ConfiguredFeature<?, ?> ore = (
								context
								.getSource()
								.getServer()
								.getRegistryManager()
								.get(RegistryKeyVersions.configuredFeature())
								.get(context.getArgument("ore", Identifier.class))
							);
							if (ore != null && ore.config() instanceof OreFeature.Config config) {
								Vec3d pos = context.getSource().getPosition();
								ScriptedColumn column = generator.newColumn(
									context.getSource().getWorld(),
									BigGlobeMath.floorI(pos.x),
									BigGlobeMath.floorI(pos.z),
									Purpose.GENERIC
								);
								context.getSource().sendFeedback(
									() -> Text.literal(
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
								context.getSource().sendError(Text.literal("Not an ore: " + ore));
							}
						}
						else {
							context.getSource().sendError(Text.literal("Not a big globe world"));
						}
						return 0;
					})
				)
				.executes((CommandContext<ServerCommandSource> context) -> {
					if (context.getSource().getWorld().getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
						Vec3d pos = context.getSource().getPosition();
						context.getSource().sendFeedback(
							() -> Text.literal(
								BigGlobeMath.floorI(pos.x) + ", " +
								BigGlobeMath.floorI(pos.y) + ", " +
								BigGlobeMath.floorI(pos.z)
							),
							false
						);
						ScriptedColumn column = generator.newColumn(
							context.getSource().getWorld(),
							BigGlobeMath.floorI(pos.x),
							BigGlobeMath.floorI(pos.z),
							Purpose.GENERIC
						);
						Arrays
						.stream(generator.feature_dispatcher.rock_replacers)
						.flatMap(TagOrObject::stream)
						.filter((RegistryEntry<ConfiguredFeature<?, ?>> entry) -> (
							entry.value().config() instanceof OreFeature.Config
						))
						.sorted(Comparator.comparing(UnregisteredObjectException::getID))
						.forEachOrdered((RegistryEntry<ConfiguredFeature<?, ?>> entry) -> {
							context.getSource().sendFeedback(
								() -> Text.literal(
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
						context.getSource().sendError(Text.literal("Not a big globe world"));
						return 0;
					}
				})
			)
		);
	}
}