package builderb0y.bigglobe.commands;

import java.util.Collection;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.StringRepresentableArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator.SpawnPoint;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.CommandVersions;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.WorldPropertiesVersions;

public class RespawnCommand {

	public static final String PREFIX = "commands." + BigGlobeMod.MODID + ".respawn.";

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands
			.literal(BigGlobeMod.MODID + ":respawn")
			.requires(CommandVersions.levelPredicate(2))
			.executes((CommandContext<CommandSourceStack> context) -> {
				Component failReason = RespawnMode.AUTO.respawnPlayer(context.getSource().getPlayerOrException(), false);
				if (failReason == null) return 1;
				context.getSource().sendFailure(failReason);
				return 0;
			})
			.then(
				Commands
				.argument("mode", new RespawnModeArgumentType())
				.executes((CommandContext<CommandSourceStack> context) -> {
					Component failReason = context.getArgument("mode", RespawnMode.class).respawnPlayer(context.getSource().getPlayerOrException(), false);
					if (failReason == null) return 1;
					context.getSource().sendFailure(failReason);
					return 0;
				})
				.then(
					Commands
					.argument("force", BoolArgumentType.bool())
					.executes((CommandContext<CommandSourceStack> context) -> {
						Component failReason = context.getArgument("mode", RespawnMode.class).respawnPlayer(context.getSource().getPlayerOrException(), context.getArgument("force", Boolean.class));
						if (failReason == null) return 1;
						context.getSource().sendFailure(failReason);
						return 0;
					})
					.then(
						Commands
						.argument("players", EntityArgument.players())
						.executes((CommandContext<CommandSourceStack> context) -> {
							RespawnMode point = context.getArgument("mode", RespawnMode.class);
							Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
							int successCount = 0;
							for (ServerPlayer player : players) {
								if (point.respawnPlayer(player, false) == null) successCount++;
							}
							if (successCount != players.size()) {
								context.getSource().sendFailure(Component.translatable(PREFIX + "multi.fail"));
							}
							return successCount;
						})
					)
				)
			)
		);
	}

	public static enum RespawnMode implements StringRepresentable {

		AUTO {
			@Override
			public @Nullable Component respawnPlayer(ServerPlayer player, boolean force) {
				if (tryRespawnBed(player, force) == null) return null;
				if (tryRespawnCommand(player, force) == null) return null;
				return doRespawnWorld(player, EntityVersions.getServer(player).overworld(), force);
			}
		},

		BED {
			@Override
			public @Nullable Component respawnPlayer(ServerPlayer player, boolean force) {
				return tryRespawnBed(player, force);
			}
		},

		COMMAND {
			@Override
			public @Nullable Component respawnPlayer(ServerPlayer player, boolean force) {
				return tryRespawnCommand(player, force);
			}
		},

		WORLD {
			@Override
			public @Nullable Component respawnPlayer(ServerPlayer player, boolean force) {
				return doRespawnWorld(player, EntityVersions.getServerWorld(player), force);
			}
		},

		OVERWORLD {
			@Override
			public @Nullable Component respawnPlayer(ServerPlayer player, boolean force) {
				return doRespawnWorld(player, EntityVersions.getServer(player).overworld(), force);
			}
		},

		NEW {
			@Override
			public @Nullable Component respawnPlayer(ServerPlayer player, boolean force) {
				return tryRespawnNew(player);
			}
		},

		;

		//why is there a class named Codec in StringIdentifiable?
		public static final Codec<RespawnMode> CODEC = BigGlobeAutoCodec.SILENT_CODEC.createDFUCodec(RespawnMode.class);

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT).intern();

		@Override
		public String getSerializedName() {
			return this.lowerCaseName;
		}

		public abstract @Nullable Component respawnPlayer(ServerPlayer player, boolean force);

		public static @Nullable Component doRespawnWorld(ServerPlayer player, ServerLevel world, boolean force) {
			LevelData properties = world.getLevelData();
			if (
				force || (
					BlockStateVersions.canSpawnInside(
						world.getBlockState(
							WorldPropertiesVersions.getSpawnPos(properties)
						)
					)
					&&
					BlockStateVersions.canSpawnInside(
						world.getBlockState(
							WorldPropertiesVersions.getSpawnPos(properties).above()
						)
					)
				)
			) {
				EntityVersions.teleport(
					player,
					world,
					new Vec3(
						WorldPropertiesVersions.getSpawnX(properties) + 0.5D,
						WorldPropertiesVersions.getSpawnY(properties),
						WorldPropertiesVersions.getSpawnZ(properties) + 0.5D
					),
					Vec3.ZERO,
					WorldPropertiesVersions.getSpawnYaw(properties),
					0.0F
				);
				return null;
			}
			else {
				return Component.translatable(PREFIX + "area_obstructed");
			}
		}

		public static @Nullable Component tryRespawnBed(ServerPlayer player, boolean force) {
			if (EntityVersions.isRespawnForced(player)) return Component.translatable(PREFIX + "bed.spawn_not_set_by_bed");

			BlockPos position = EntityVersions.getRespawnPosition(player);
			if (position == null) return Component.translatable(PREFIX + "position_not_set");

			ResourceKey<Level> dimension = EntityVersions.getRespawnDimension(player);
			if (dimension == null) return Component.translatable(PREFIX + "dimension_not_set");

			ServerLevel world = EntityVersions.getServer(player).getLevel(dimension);
			if (world == null) return Component.translatable(PREFIX + "dimension_doesnt_exist");

			float yaw = EntityVersions.getRespawnAngle(player);
			Vec3 actualPosition = (
				ServerPlayer.findRespawnAndUseSpawnBlock(
					world,
					new ServerPlayer.RespawnConfig(
						new LevelData.RespawnData(
							new GlobalPos(
								dimension,
								position
							),
							yaw,
							0.0F
						),
						false
					),
					false
				)
				.map(ServerPlayer.RespawnPosAngle::position)
				.orElse(null)
			);
			if (actualPosition == null) {
				if (force) {
					actualPosition = Vec3.atBottomCenterOf(position);
				}
				else {
					return Component.translatable(PREFIX + "bed.destroyed");
				}
			}
			EntityVersions.teleport(player, world, actualPosition, Vec3.ZERO, 0.0F, 0.0F);
			player.lookAt(Anchor.EYES, new Vec3(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D));
			return null;
		}

		public static @Nullable Component tryRespawnCommand(ServerPlayer player, boolean force) {
			if (!EntityVersions.isRespawnForced(player)) return Component.translatable(PREFIX + "command.spawn_not_set_by_command");

			BlockPos position = EntityVersions.getRespawnPosition(player);
			if (position == null) return Component.translatable(PREFIX + "command.position_not_set");

			ResourceKey<Level> dimension = EntityVersions.getRespawnDimension(player);
			if (dimension == null) return Component.translatable(PREFIX + "command.dimension_not_set");

			ServerLevel world = EntityVersions.getServer(player).getLevel(dimension);
			if (world == null) return Component.translatable(PREFIX + "command.dimension_doesnt_exist", dimension.identifier().toString());

			if (
				force
				|| (
					BlockStateVersions.canSpawnInside(world.getBlockState(position)) &&
					BlockStateVersions.canSpawnInside(world.getBlockState(position.above()))
				)
			) {
				float yaw = EntityVersions.getRespawnAngle(player);
				EntityVersions.teleport(
					player,
					world,
					new Vec3(
						position.getX() + 0.5D,
						position.getY(),
						position.getZ() + 0.5D
					),
					Vec3.ZERO,
					yaw,
					0.0F
				);
				return null;
			}

			return Component.translatable(PREFIX + "area_obstructed");
		}

		public static @Nullable Component tryRespawnNew(ServerPlayer player) {
			if (EntityVersions.getServerWorld(player).getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
				SpawnPoint spawnPoint = BigGlobeSpawnLocator.findSpawn(EntityVersions.getServerWorld(player), generator, EntityVersions.getServerWorld(player).getRandom().nextLong());
				if (spawnPoint != null) {
					EntityVersions.teleport(
						player,
						EntityVersions.getServerWorld(player),
						new Vec3(spawnPoint.x(), spawnPoint.y(), spawnPoint.z()),
						Vec3.ZERO,
						spawnPoint.yaw(),
						0.0F
					);
					return null;
				}
				else {
					return Component.translatable(PREFIX + "new.no_good_location");
				}
			}
			else {
				return Component.translatable(PREFIX + "new.not_supported_dimension");
			}
		}
	}

	public static class RespawnModeArgumentType extends StringRepresentableArgument<RespawnMode> {

		public RespawnModeArgumentType() {
			super(RespawnMode.CODEC, RespawnMode::values);
		}
	}
}