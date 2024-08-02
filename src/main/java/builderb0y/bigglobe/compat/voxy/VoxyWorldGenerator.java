package builderb0y.bigglobe.compat.voxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import me.cortex.voxy.common.storage.inmemory.MemoryStorageBackend;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import me.cortex.voxy.common.world.WorldEngine;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.level.storage.LevelStorage.Session;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.RootLayer;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.mixins.MinecraftServer_SessionAccess;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public class VoxyWorldGenerator extends AbstractVoxyWorldGenerator {

	public VoxyWorldGenerator(WorldEngine engine, ServerWorld world, BigGlobeScriptedChunkGenerator generator) {
		super(engine, world, generator, load(engine, world));
	}

	public static DistanceGraph load(WorldEngine engine, ServerWorld serverWorld) {
		if (!(engine.storage instanceof MemoryStorageBackend)) {
			Session session = ((MinecraftServer_SessionAccess)(serverWorld.getServer())).bigglobe_getSession();
			Path dimensionFolder = session.getWorldDirectory(serverWorld.getRegistryKey());
			Path distanceGraphFile = dimensionFolder.resolve("voxy").resolve("bigglobe_progress.dat");
			if (Files.exists(distanceGraphFile)) {
				try (
					BitInputStream bits = new BitInputStream(
						new DataInputStream(
							Files.newInputStream(distanceGraphFile)
						)
					)
				) {
					return DistanceGraphIO.read(
						-WORLD_SIZE_IN_CHUNKS,
						-WORLD_SIZE_IN_CHUNKS,
						+WORLD_SIZE_IN_CHUNKS,
						+WORLD_SIZE_IN_CHUNKS,
						bits
					);
				}
				catch (IOException exception) {
					BigGlobeMod.LOGGER.error("Exception loading voxy progress file. Restarting progress.");
				}
			}
		}
		return DistanceGraph.worldOfChunks();
	}

	@Override
	public void save() {
		if (!(this.engine.storage instanceof MemoryStorageBackend)) {
			Session session = ((MinecraftServer_SessionAccess)(this.world.getServer())).bigglobe_getSession();
			Path dimensionFolder = session.getWorldDirectory(this.world.getRegistryKey());
			Path voxyFolder = dimensionFolder.resolve("voxy");
			Path distanceGraphFile = voxyFolder.resolve("bigglobe_progress.dat");
			Path writeFile = voxyFolder.resolve("bigglobe_progress.tmp");
			try {
				Files.createDirectories(voxyFolder);
				try (
					BitOutputStream bits = new BitOutputStream(
						new DataOutputStream(
							Files.newOutputStream(
								writeFile,
								StandardOpenOption.CREATE,
								StandardOpenOption.TRUNCATE_EXISTING
							)
						)
					)
				) {
					DistanceGraphIO.write(this.distanceGraph, bits);
					Files.deleteIfExists(distanceGraphFile);
					Files.move(writeFile, distanceGraphFile);
				}
			}
			catch (IOException exception) {
				BigGlobeMod.LOGGER.error("Exception saving voxy progress: ", exception);
			}
		}
	}

	@Override
	public void createChunk(int chunkX, int chunkZ, RegistryEntry<Biome> biome) {
		int startX = chunkX << 4;
		int startZ = chunkZ << 4;
		ScriptedColumn[] columns = this.columns;
		BlockSegmentList[] lists = new BlockSegmentList[256];
		int minY = this.generator.height.min_y();
		int maxY = this.generator.height.max_y();
		RootLayer layer = this.generator.layer;
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int offsetZ = 0; offsetZ < 16; offsetZ += 2) {
				int offsetZ_ = offsetZ;
				for (int offsetX = 0; offsetX < 16; offsetX += 2) {
					int offsetX_ = offsetX;
					async.submit(() -> {
						int quadX = startX | offsetX_;
						int quadZ = startZ | offsetZ_;
						int baseIndex = (offsetZ_ << 4) | offsetX_;
						ScriptedColumn
							column00 = columns[baseIndex     ],
							column01 = columns[baseIndex |  1],
							column10 = columns[baseIndex | 16],
							column11 = columns[baseIndex | 17];
						column00.setParamsUnchecked(column00.params.at(quadX,     quadZ    ));
						column01.setParamsUnchecked(column01.params.at(quadX | 1, quadZ    ));
						column10.setParamsUnchecked(column10.params.at(quadX,     quadZ | 1));
						column11.setParamsUnchecked(column11.params.at(quadX | 1, quadZ | 1));
						BlockSegmentList
							list00 = new BlockSegmentList(minY, maxY),
							list01 = new BlockSegmentList(minY, maxY),
							list10 = new BlockSegmentList(minY, maxY),
							list11 = new BlockSegmentList(minY, maxY);
						layer.emitSegments(column00, column01, column10, column11, list00);
						layer.emitSegments(column01, column00, column11, column10, list01);
						layer.emitSegments(column10, column11, column00, column01, list10);
						layer.emitSegments(column11, column10, column01, column00, list11);
						list00.computeLightLevels();
						list01.computeLightLevels();
						list10.computeLightLevels();
						list11.computeLightLevels();
						lists[baseIndex     ] = list00;
						lists[baseIndex |  1] = list01;
						lists[baseIndex | 16] = list10;
						lists[baseIndex | 17] = list11;
					});
				}
			}
		}
		for (int y = minY; y < maxY; y += 16) {
			VoxelizedSection section = this.convertSection(chunkX, y >> 4, chunkZ, lists, biome);
			if (section != null) this.engine.insertUpdate(section);
		}
	}
}