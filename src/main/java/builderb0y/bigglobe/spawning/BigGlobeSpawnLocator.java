package builderb0y.bigglobe.spawning;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

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
import builderb0y.bigglobe.structures.StructureManager.FinalStructures;
import builderb0y.bigglobe.structures.StructureManager.StructureGenerationParams;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

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
				player.setSpawnPoint(
					#if MC_VERSION >= MC_1_21_5 new net.minecraft.server.network.ServerPlayerEntity.Respawn( #endif
						EntityVersions.getWorld(player).getRegistryKey(),
						spawnPoint.toBlockPos(),
						spawnPoint.yaw,
						true
					#if MC_VERSION >= MC_1_21_5 ) #endif,
					false
				);
				player.refreshPositionAndAngles(spawnPoint.toBlockPos(), spawnPoint.yaw, 0.0F);
			}
		}
	}

	public static @Nullable SpawnPoint findSpawn(
		ServerWorld world,
		BigGlobeScriptedChunkGenerator generator,
		long seed
	) {
		if (generator.spawn_point == null) return null;
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
			if (generator.spawn_point.get(column, permuter)) {
				int ground = generator.getHeightOnGround(
					halton.floorX(),
					halton.floorY(),
					Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
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

	public static boolean checkStructures(ServerWorld world, BigGlobeScriptedChunkGenerator generator, int blockX, int blockY, int blockZ) {
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
			if (start.getBoundingBox().contains(blockX, blockY, blockZ)) {
				BigGlobeMod.LOGGER.debug("Prevented player from spawning in structure " + RegistryVersions.getRegistry(world.getRegistryManager(), RegistryKeys.STRUCTURE).getId(start.getStructure()));
				return false;
			}
		}
		return true;
	}

	public static record SpawnPoint(double x, double y, double z, float yaw) {

		public BlockPos toBlockPos() {
			return BlockPos.ofFloored(this.x, this.y, this.z);
		}
	}
}