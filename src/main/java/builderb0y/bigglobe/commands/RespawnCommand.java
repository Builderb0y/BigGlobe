package builderb0y.bigglobe.commands;

import java.util.Collection;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import org.jetbrains.annotations.Nullable;

import net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator.SpawnPoint;

public class RespawnCommand {

	public static final String PREFIX = "commands." + BigGlobeMod.MODID + ".respawn.";

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":respawn")
			.requires(source -> source.hasPermissionLevel(2))
			.executes(context -> {
				RespawnMode.AUTO.respawnPlayer(context.getSource().getPlayerOrThrow());
				return 1;
			})
			.then(
				CommandManager
				.argument("mode", new EnumArgument<>(RespawnMode.class))
				.executes(context -> {
					Text failReason = context.getArgument("mode", RespawnMode.class).respawnPlayer(context.getSource().getPlayerOrThrow());
					if (failReason == null) return 1;
					context.getSource().sendError(failReason);
					return 0;
				})
				.then(
					CommandManager
					.argument("players", EntityArgumentType.players())
					.executes(context -> {
						RespawnMode point = context.getArgument("mode", RespawnMode.class);
						Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
						int successCount = 0;
						for (ServerPlayerEntity player : players) {
							if (point.respawnPlayer(player) == null) successCount++;
						}
						if (successCount != players.size()) {
							context.getSource().sendError(Text.translatable(PREFIX + "multi.fail"));
						}
						return successCount;
					})
				)
			)
		);
	}

	public static enum RespawnMode implements StringIdentifiable {

		AUTO {

			@Override
			public @Nullable Text respawnPlayer(ServerPlayerEntity player) {
				if (tryRespawnBed(player) == null) return null;
				if (tryRespawnCommand(player) == null) return null;
				return doRespawnWorld(player, player.server.getOverworld());
			}
		},

		BED {

			@Override
			public @Nullable Text respawnPlayer(ServerPlayerEntity player) {
				return tryRespawnBed(player);
			}
		},

		COMMAND {

			@Override
			public @Nullable Text respawnPlayer(ServerPlayerEntity player) {
				return tryRespawnCommand(player);
			}
		},

		WORLD {

			@Override
			public @Nullable Text respawnPlayer(ServerPlayerEntity player) {
				return doRespawnWorld(player, player.getWorld());
			}
		},

		OVERWORLD {

			@Override
			public @Nullable Text respawnPlayer(ServerPlayerEntity player) {
				return doRespawnWorld(player, player.getServer().getOverworld());
			}
		},

		NEW {

			@Override
			public @Nullable Text respawnPlayer(ServerPlayerEntity player) {
				return tryRespawnNew(player);
			}
		}

		;

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);

		@Override
		public String asString() {
			return this.lowerCaseName;
		}

		public abstract @Nullable Text respawnPlayer(ServerPlayerEntity player);

		public static @Nullable Text doRespawnWorld(ServerPlayerEntity player, ServerWorld world) {
			WorldProperties properties = world.getLevelProperties();
			player.teleport(world, properties.getSpawnX() + 0.5D, properties.getSpawnY(), properties.getSpawnZ() + 0.5D, properties.getSpawnAngle(), 0.0F);
			return null;
		}

		public static @Nullable Text tryRespawnBed(ServerPlayerEntity player) {
			if (player.isSpawnForced()) return Text.translatable(PREFIX + "bed.spawn_not_set_by_bed");

			BlockPos position = player.getSpawnPointPosition();
			if (position == null) return Text.translatable(PREFIX + "position_not_set");

			RegistryKey<World> dimension = player.getSpawnPointDimension();
			if (dimension == null) return Text.translatable(PREFIX + "dimension_not_set");

			ServerWorld world = player.getServer().getWorld(dimension);
			if (world == null) return Text.translatable(PREFIX + "dimension_doesnt_exist");

			float yaw = player.getSpawnAngle();
			Vec3d actualPosition = (
				PlayerEntity
				.findRespawnPosition(
					world,
					position,
					yaw,
					false,
					true
				)
				.orElse(null)
			);
			if (actualPosition == null) {
				return Text.translatable(PREFIX + "bed.destroyed");
			}
			player.teleport(world, actualPosition.x, actualPosition.y, actualPosition.z, 0.0F, 0.0F);
			player.lookAt(EntityAnchor.EYES, new Vec3d(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D));
			return null;
		}

		public static @Nullable Text tryRespawnCommand(ServerPlayerEntity player) {
			if (!player.isSpawnForced()) return Text.translatable(PREFIX + "command.spawn_not_set_by_command");

			BlockPos position = player.getSpawnPointPosition();
			if (position == null) return Text.translatable(PREFIX + "command.position_not_set");

			RegistryKey<World> dimension = player.getSpawnPointDimension();
			if (dimension == null) return Text.translatable(PREFIX + "command.dimension_not_set");

			ServerWorld world = player.server.getWorld(dimension);
			if (world == null) return Text.translatable(PREFIX + "command.dimension_doesnt_exist", dimension.getValue().toString());

			float yaw = player.getSpawnAngle();
			player.teleport(world, position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, yaw, 0.0F);
			return null;
		}

		public static @Nullable Text tryRespawnNew(ServerPlayerEntity player) {
			if (player.getWorld().getChunkManager().getChunkGenerator() instanceof BigGlobeOverworldChunkGenerator overworldChunkGenerator) {
				SpawnPoint spawnPoint = BigGlobeSpawnLocator.findSpawn(overworldChunkGenerator.column(0, 0), player.world.random.nextLong());
				if (spawnPoint != null) {
					player.teleport(player.getWorld(), spawnPoint.x, spawnPoint.y, spawnPoint.z, spawnPoint.yaw, 0.0F);
					return null;
				}
				else {
					return Text.translatable(PREFIX + "new.no_new_spawn");
				}
			}
			else {
				return Text.translatable(PREFIX + "new.not_overworld");
			}
		}
	}
}