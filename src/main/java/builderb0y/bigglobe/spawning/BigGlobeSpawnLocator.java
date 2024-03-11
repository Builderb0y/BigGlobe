package builderb0y.bigglobe.spawning;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.HaltonIterator2D;
import builderb0y.bigglobe.mixins.MinecraftServer_InitializeSpawnPoint;
import builderb0y.bigglobe.mixins.PlayerManager_InitializeSpawnPoint;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.BlockPosVersions;
import builderb0y.bigglobe.versions.EntityVersions;

public class BigGlobeSpawnLocator {

	/** called by {@link MinecraftServer_InitializeSpawnPoint} */
	public static boolean initWorldSpawn(ServerWorld world) {
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			SpawnPoint spawnPoint = findSpawn(world, generator, world.getSeed());
			if (spawnPoint != null) {
				world.setSpawnPos(spawnPoint.toBlockPos(), spawnPoint.yaw);
				return true;
			}
		}
		return false;
	}

	/** called by {@link PlayerManager_InitializeSpawnPoint} */
	public static void initPlayerSpawn(ServerPlayerEntity player) {
		if (
			BigGlobeConfig.INSTANCE.get().playerSpawning.perPlayerSpawnPoints &&
			EntityVersions.getServerWorld(player).getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator
		) {
			SpawnPoint spawnPoint = findSpawn(
				EntityVersions.getServerWorld(player),
				generator,
				Permuter.permute(
					EntityVersions.getServerWorld(player).getSeed() ^ 0x4BB5FF80362770B0L,
					player.getGameProfile().getId()
				)
			);
			if (spawnPoint != null) {
				player.setSpawnPoint(EntityVersions.getWorld(player).getRegistryKey(), spawnPoint.toBlockPos(), spawnPoint.yaw, true, false);
				player.refreshPositionAndAngles(spawnPoint.toBlockPos(), spawnPoint.yaw, 0.0F);
			}
		}
	}

	public static @Nullable SpawnPoint findSpawn(
		HeightLimitView world,
		BigGlobeScriptedChunkGenerator generator,
		long seed
	) {
		if (generator.spawn_point == null) return null;
		ScriptedColumn column = generator.newColumn(world, 0, 0, Purpose.GENERIC);
		double radius = BigGlobeConfig.INSTANCE.get().playerSpawning.maxSpawnRadius;
		HaltonIterator2D halton = new HaltonIterator2D(
			-radius,
			-radius,
			radius,
			radius,
			Permuter.nextUniformInt(seed ^ 0x38AA7BFF7E2C684BL) & 0xFFFF
		);
		double startAngle = Permuter.nextPositiveDouble(seed ^ 0x55E7F77A3DF91E6AL) * BigGlobeMath.TAU;
		long startTime = System.currentTimeMillis();
		Permuter permuter = new Permuter(0L);
		for (int attempt = 0; attempt < 1024; attempt++) {
			permuter.setSeed(Permuter.permute(seed ^ 0x5E7658F173C1CF0AL, attempt));
			column.setParamsUnchecked(column.params.at(halton.floorX(), halton.floorY()));
			if (generator.spawn_point.get(column, permuter)) {
				long endTime = System.currentTimeMillis();
				BigGlobeMod.LOGGER.debug("Found good spawn point after " + attempt + " attempts and " + (endTime - startTime) + " ms.");
				return new SpawnPoint(
					halton.x,
					generator.getHeightOnGround(
						halton.floorX(),
						halton.floorY(),
						Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
						world,
						null
					),
					halton.y,
					(float)(startAngle)
				);
			}
			halton.next();
		}
		long endTime = System.currentTimeMillis();
		BigGlobeMod.LOGGER.warn("Could not find good spawn point after 1024 attempts and " + (endTime - startTime) + " ms.");
		return null;
	}

	public static class SpawnPoint {

		public final double x, y, z;
		public final float yaw;

		public SpawnPoint(double x, double y, double z, float yaw) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
		}

		public BlockPos toBlockPos() {
			return BlockPosVersions.floor(this.x, this.y, this.z);
		}
	}
}