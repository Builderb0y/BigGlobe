package builderb0y.bigglobe.spawning;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.LevelData;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.HaltonIterator2D;
import builderb0y.bigglobe.mixins.MinecraftServer_InitializeSpawnPoint;
import builderb0y.bigglobe.mixins.PlayerManager_InitializeSpawnPoint;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.StructurePlacementCalculator.FinalStructures;
import builderb0y.bigglobe.structures.StructurePlacementCalculator.StructureGenerationParams;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.GameProfileVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeSpawnLocator {

	/**
	called by {@link MinecraftServer_InitializeSpawnPoint}
	*/
	public static boolean initWorldSpawn(ServerLevel world) {
		if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			SpawnPoint spawnPoint = findSpawn(world, generator, world.getSeed());
			if (spawnPoint != null) {

				world.setRespawnData(new LevelData.RespawnData(new GlobalPos(world.dimension(), spawnPoint.toBlockPos()), spawnPoint.yaw(), 0.0F));

				return true;
			}
		}
		return false;
	}

	public static long perPlayerSeed(ServerLevel serverWorld, UUID playerUUID) {
		return Permuter.permute(serverWorld.getSeed() ^ 0x4BB5FF80362770B0L, playerUUID);
	}

	/**
	called by {@link PlayerManager_InitializeSpawnPoint}
	*/
	public static void initPlayerSpawn(ServerPlayer player) {
		if (
			BigGlobeConfig.INSTANCE.get().playerSpawning.perPlayerSpawnPoints &&
			EntityVersions.getServerWorld(player).getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator
		) {
			SpawnPoint spawnPoint = findSpawn(
				EntityVersions.getServerWorld(player),
				generator,
				perPlayerSeed(EntityVersions.getServerWorld(player), GameProfileVersions.getUUID(player.getGameProfile()))
			);
			if (spawnPoint != null) {

				player.setRespawnPosition(
					new ServerPlayer.RespawnConfig(
						new LevelData.RespawnData(
							new GlobalPos(
								EntityVersions.getWorld(player).dimension(),
								spawnPoint.toBlockPos()
							),
							spawnPoint.yaw(),
							0.0F
						),
						true
					),
					false
				);

				player.snapTo(spawnPoint.toBlockPos(), spawnPoint.yaw, 0.0F);
			}
		}
	}

	public static @Nullable SpawnPoint findSpawn(
		ServerLevel world,
		BigGlobeScriptedChunkGenerator generator,
		long seed
	) {
		if (generator.game_mechanics.spawn_point() == null) return null;
		ScriptedColumn column = generator.newColumn(world, 0, 0, ColumnUsage.GENERIC.normalHints());
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
		int structureAttempt = 0;
		for (int attempt = 0; attempt < 1024; attempt++) {
			permuter.setSeed(Permuter.permute(seed ^ 0x5E7658F173C1CF0AL, attempt));
			column.setParamsUnchecked(column.params.at(halton.floorX(), halton.floorY()));
			if (generator.game_mechanics.spawn_point().get(column, permuter)) {
				int ground = generator.getFirstFreeHeight(
					halton.floorX(),
					halton.floorY(),
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					world,
					null
				);
				if (structureAttempt >= 10 || checkStructures(world, generator, halton.floorX(), ground, halton.floorY())) {
					long endTime = System.currentTimeMillis();
					BigGlobeMod.LOGGER.debug("Found good spawn point after " + attempt + " attempts and " + (endTime - startTime) + " ms.");
					return new SpawnPoint(
						halton.x,
						ground,
						halton.y,
						(float)(startAngle)
					);
				}
				else if (++structureAttempt == 10) {
					BigGlobeMod.LOGGER.warn("10 different potential spawn points were obstructed by structures. Abandoning no structure requirement.");
				}
			}
			halton.next();
		}
		long endTime = System.currentTimeMillis();
		BigGlobeMod.LOGGER.warn("Could not find good spawn point after 1024 attempts and " + (endTime - startTime) + " ms.");
		return null;
	}

	public static boolean checkStructures(ServerLevel world, BigGlobeScriptedChunkGenerator generator, int blockX, int blockY, int blockZ) {
		Hints hints = ColumnUsage.GENERIC.normalHints();
		FinalStructures structures = generator.structureManager.getIntersectingStructures(
			new StructureGenerationParams(
				generator,
				generator.newColumnLookup(world, hints),
				world,
				new ChunkPos(blockX >> 4, blockZ >> 4)
			)
		);
		for (StructureStart start : structures) {
			if (start.getBoundingBox().isInside(blockX, blockY, blockZ)) {
				BigGlobeMod.LOGGER.debug("Prevented player from spawning in structure " + RegistryVersions.getRegistry(world.registryAccess(), Registries.STRUCTURE).getKey(start.getStructure()));
				return false;
			}
		}
		return true;
	}

	public static record SpawnPoint(double x, double y, double z, float yaw) {

		public BlockPos toBlockPos() {
			return BlockPos.containing(this.x, this.y, this.z);
		}
	}
}