package builderb0y.bigglobe.spawning;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.GoldenSpiralIterator;
import builderb0y.bigglobe.math.pointSequences.HaltonIterator2D;
import builderb0y.bigglobe.mixins.MinecraftServer_InitializeSpawnPoint;
import builderb0y.bigglobe.mixins.PlayerManager_InitializeSpawnPoint;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.BiomeLayout.OverworldBiomeLayout;
import builderb0y.bigglobe.versions.BlockPosVersions;
import builderb0y.bigglobe.versions.EntityVersions;

public class BigGlobeSpawnLocator {

	/** called by {@link MinecraftServer_InitializeSpawnPoint} */
	public static boolean initWorldSpawn(ServerWorld world) {
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeOverworldChunkGenerator overworldChunkGenerator) {
			SpawnPoint spawnPoint = findSpawn(overworldChunkGenerator.column(0, 0), world.getSeed());
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
			EntityVersions.getServerWorld(player).getChunkManager().getChunkGenerator() instanceof BigGlobeOverworldChunkGenerator overworldChunkGenerator
		) {
			SpawnPoint spawnPoint = findSpawn(
				overworldChunkGenerator.column(0, 0),
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

	public static @Nullable SpawnPoint findSpawn(OverworldColumn column, long seed) {
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
		for (int attempt = 0; attempt < 1024; attempt++) {
			column.setPos(halton.floorX(), halton.floorY());
			if (isGoodSpawnPoint(column, startAngle)) {
				long endTime = System.currentTimeMillis();
				BigGlobeMod.LOGGER.debug("Found good spawn point after " + attempt + " attempts and " + (endTime - startTime) + " ms.");
				return new SpawnPoint(column, halton.x, column.getFinalTopHeightI(), halton.y, (float)(startAngle));
			}
			halton.next();
		}
		long endTime = System.currentTimeMillis();
		BigGlobeMod.LOGGER.warn("Could not find good spawn point after 1024 attempts and " + (endTime - startTime) + " ms.");
		return null;
	}

	public static boolean isGoodSpawnPoint(OverworldColumn column, double startAngle) {
		if (!column.settings.biomes.root.search(column, column.getFinalTopHeightD(), column.seed, layout -> ((OverworldBiomeLayout)(layout)).player_spawn_friendly)) {
			return false;
		}
		int restoreX = column.x, restoreZ = column.z;
		try {
			for (
				GoldenSpiralIterator spiral = new GoldenSpiralIterator(column.x, column.z, 4.0D, startAngle);
				spiral.radius <= 64.0D;
				spiral.next()
			) {
				column.setPos(spiral.floorX(), spiral.floorY());
				if (column.getSurfaceFoliage() > 0.0D) {
					return true;
				}
			}
			return false;
		}
		finally {
			column.setPos(restoreX, restoreZ);
		}
	}

	public static class SpawnPoint {

		public final OverworldColumn column;
		public final double x, y, z;
		public final float yaw;

		public SpawnPoint(OverworldColumn column, double x, double y, double z, float yaw) {
			this.column = column;
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