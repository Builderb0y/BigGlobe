package builderb0y.bigglobe.commands;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.jetbrains.annotations.Nullable;

import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.NetherColumn;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.settings.OverworldCaveSettings;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;
import builderb0y.bigglobe.settings.OverworldCavernSettings;
import builderb0y.bigglobe.settings.OverworldCavernSettings.LocalCavernSettings;
import builderb0y.bigglobe.settings.OverworldSkylandSettings;
import builderb0y.bigglobe.settings.OverworldSkylandSettings.LocalSkylandSettings;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;

public class LocateVoronoiCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> node = (
			CommandManager.literal(BigGlobeMod.MODID + ":locateVoronoi")
			.requires(source -> source.hasPermissionLevel(2))
		);
		for (VoronoiType<?> type : VoronoiType.VALUES) {
			node = node.then(
				CommandManager.literal(type.name)
				.requires(source -> (
					type.getDiagram(
						WorldColumn.forWorld(
							source.getWorld(),
							BigGlobeMath.floorI(source.getPosition().x),
							BigGlobeMath.floorI(source.getPosition().z)
						)
					)
					!= null
				))
				.then(
					CommandManager.argument(
						"settingsID",
						RegistryKeyArgumentType.registryKey(
							type.settingsRegistryKey
						)
					)
					.executes(context -> (
						type.locate(
							context.getSource(),
							context.getArgument(
								"settingsID",
								RegistryKey.class
							)
						)
					))
				)
			);
		}
		dispatcher.register(node);
	}

	public static abstract class VoronoiType<T_Settings> {

		public static final VoronoiType<LocalCavernSettings> CAVERN = new VoronoiType<>("cavern", BigGlobeDynamicRegistries.LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY) {

			@Override
			public @Nullable VoronoiDiagram2D getDiagram(WorldColumn column) {
				OverworldCavernSettings caverns;
				return column instanceof OverworldColumn overworld && (caverns = overworld.settings.underground.deep_caverns()) != null ? caverns.placement : null;
			}

			@Override
			public @Nullable LocalCavernSettings getSettings(WorldColumn column) {
				OverworldColumn.CavernCell cell;
				return column instanceof OverworldColumn overworld && (cell = overworld.getCavernCell()) != null ? cell.settings : null;
			}
		};
		public static final VoronoiType<LocalOverworldCaveSettings> CAVE = new VoronoiType<>("cave", BigGlobeDynamicRegistries.LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY) {

			@Override
			public @Nullable VoronoiDiagram2D getDiagram(WorldColumn column) {
				OverworldCaveSettings caves;
				return column instanceof OverworldColumn overworld && (caves = overworld.settings.underground.caves()) != null ? caves.placement : null;
			}

			@Override
			public @Nullable LocalOverworldCaveSettings getSettings(WorldColumn column) {
				OverworldColumn.CaveCell cell;
				return column instanceof OverworldColumn overworld && (cell = overworld.getCaveCell()) != null ? cell.settings : null;
			}
		};
		public static final VoronoiType<LocalSkylandSettings> SKYLAND = new VoronoiType<>("skyland", BigGlobeDynamicRegistries.LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY) {

			@Override
			public @Nullable VoronoiDiagram2D getDiagram(WorldColumn column) {
				OverworldSkylandSettings skylands;
				return column instanceof OverworldColumn overworld && (skylands = overworld.settings.skylands) != null ? skylands.placement : null;
			}

			@Override
			public @Nullable LocalSkylandSettings getSettings(WorldColumn column) {
				OverworldColumn.SkylandCell cell;
				return column instanceof OverworldColumn overworld && (cell = overworld.getSkylandCell()) != null && overworld.hasSkyland() ? cell.settings : null;
			}
		};
		public static final VoronoiType<LocalNetherSettings> NETHER_BIOME = new VoronoiType<>("nether_biome", BigGlobeDynamicRegistries.LOCAL_NETHER_SETTINGS_REGISTRY_KEY) {

			@Override
			public @Nullable VoronoiDiagram2D getDiagram(WorldColumn column) {
				return column instanceof NetherColumn nether ? nether.settings.biome_placement : null;
			}

			@Override
			public @Nullable LocalNetherSettings getSettings(WorldColumn column) {
				return column instanceof NetherColumn nether ? nether.getLocalCell().settings : null;
			}
		};
		public static final List<VoronoiType<?>> VALUES = new ArrayList<>(4);
		static {
			VALUES.add(CAVERN);
			VALUES.add(CAVE);
			VALUES.add(SKYLAND);
			VALUES.add(NETHER_BIOME);
		}

		public final String name;
		public final RegistryKey<Registry<T_Settings>> settingsRegistryKey;

		public VoronoiType(String name, RegistryKey<Registry<T_Settings>> settingsRegistryKey) {
			this.name = name;
			this.settingsRegistryKey = settingsRegistryKey;
		}

		public abstract @Nullable VoronoiDiagram2D getDiagram(WorldColumn column);

		public abstract @Nullable T_Settings getSettings(WorldColumn column);

		public int locate(ServerCommandSource source, RegistryKey<T_Settings> settingsKey) {
			WorldColumn column = WorldColumn.forWorld(
				source.getWorld(),
				BigGlobeMath.floorI(source.getPosition().x),
				BigGlobeMath.floorI(source.getPosition().z)
			);
			VoronoiDiagram2D diagram = this.getDiagram(column);
			if (diagram == null) return 0;
			T_Settings settings = source.getServer().getRegistryManager().get(this.settingsRegistryKey).get(settingsKey);
			if (settings == null) return 0;
			int centerCellX = Math.floorDiv(column.x, diagram.distance);
			int centerCellZ = Math.floorDiv(column.z, diagram.distance);
			if (this.check(source, column, diagram, settings, settingsKey, centerCellX, centerCellZ)) return 1;
			for (int radius = 1; radius <= 16; radius++) {
				if (this.check(source, column, diagram, settings, settingsKey, centerCellX + radius, centerCellZ)) return 1;
				if (this.check(source, column, diagram, settings, settingsKey, centerCellX - radius, centerCellZ)) return 1;
				if (this.check(source, column, diagram, settings, settingsKey, centerCellX, centerCellZ + radius)) return 1;
				if (this.check(source, column, diagram, settings, settingsKey, centerCellX, centerCellZ - radius)) return 1;
				for (int side = 1; side < radius; side++) {
					if (this.check(source, column, diagram, settings, settingsKey, centerCellX + radius, centerCellZ + side)) return 1;
					if (this.check(source, column, diagram, settings, settingsKey, centerCellX + radius, centerCellZ - side)) return 1;
					if (this.check(source, column, diagram, settings, settingsKey, centerCellX - radius, centerCellZ + side)) return 1;
					if (this.check(source, column, diagram, settings, settingsKey, centerCellX - radius, centerCellZ - side)) return 1;
					if (this.check(source, column, diagram, settings, settingsKey, centerCellX + side, centerCellZ + radius)) return 1;
					if (this.check(source, column, diagram, settings, settingsKey, centerCellX - side, centerCellZ + radius)) return 1;
					if (this.check(source, column, diagram, settings, settingsKey, centerCellX + side, centerCellZ - radius)) return 1;
					if (this.check(source, column, diagram, settings, settingsKey, centerCellX - side, centerCellZ - radius)) return 1;
				}
				if (this.check(source, column, diagram, settings, settingsKey, centerCellX + radius, centerCellZ + radius)) return 1;
				if (this.check(source, column, diagram, settings, settingsKey, centerCellX + radius, centerCellZ - radius)) return 1;
				if (this.check(source, column, diagram, settings, settingsKey, centerCellX - radius, centerCellZ + radius)) return 1;
				if (this.check(source, column, diagram, settings, settingsKey, centerCellX - radius, centerCellZ - radius)) return 1;
			}
			source.sendError(Text.translatable("commands.bigglobe.locateVoronoi.fail", settingsKey.getValue().toString(), this.name));
			return 0;
		}

		public boolean check(ServerCommandSource source, WorldColumn column, VoronoiDiagram2D diagram, T_Settings settings, RegistryKey<T_Settings> settingsKey, int cellX, int cellZ) {
			VoronoiDiagram2D.SeedPoint seedPoint = diagram.getSeedPoint(cellX, cellZ);
			column.setPosUnchecked(seedPoint.centerX, seedPoint.centerZ);
			if (this.getSettings(column) == settings) {
				source.sendFeedback(
					Text.translatable(
						"commands.bigglobe.locateVoronoi.success",
						settingsKey.getValue().toString(),
						this.name,
						Texts.bracketed(
							Text.translatable("chat.coordinates", column.x, "~", column.z)
							.styled(style -> (
								style
								.withColor(Formatting.GREEN)
								.withHoverEvent(new HoverEvent(
									HoverEvent.Action.SHOW_TEXT,
									Text.translatable("commands.bigglobe.locate.clickToTeleport")
								))
								.withClickEvent(new ClickEvent(
									ClickEvent.Action.SUGGEST_COMMAND,
									"/tp @s " + column.x + " ~ " + column.z
								))
							))
						),
						(int)(
							Math.sqrt(
								BigGlobeMath.squareD(
									column.x - source.getPosition().x,
									column.z - source.getPosition().z
								)
							)
						)
					),
					false
				);
				return true;
			}
			else {
				return false;
			}
		}
	}
}