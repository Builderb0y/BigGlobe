package builderb0y.bigglobe.compat.voxy;

import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.world.WorldEngine;

import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.RootLayer;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public class VoxyWorldGenerator extends AbstractVoxyWorldGenerator {

	public VoxyWorldGenerator(WorldEngine engine, ServerWorld world, BigGlobeScriptedChunkGenerator generator) {
		super(engine, world, generator);
	}

	@Override
	public void createChunk(int levelX, int levelZ, int level, StorageBackend storage) {
		int startX = levelX << (level + 5);
		int startZ = levelZ << (level + 5);
		int step   = 1 << level;

		ScriptedColumn[] columns = this.columns.get();
		BlockSegmentList[] lists = new BlockSegmentList[1024];
		int minY = this.generator.height.min_y();
		int maxY = this.generator.height.max_y();
		RootLayer layer = this.generator.layer;
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int offsetZ = 0; offsetZ < 32; offsetZ += 2) {
				int offsetZ_ = offsetZ;
				for (int offsetX = 0; offsetX < 32; offsetX += 2) {
					int offsetX_ = offsetX;
					async.submit(() -> {
						int x = startX | (offsetX_ << level);
						int z = startZ | (offsetZ_ << level);
						int baseIndex = (offsetZ_ << 5) | offsetX_;
						ScriptedColumn
							column00 = columns[baseIndex     ],
							column01 = columns[baseIndex |  1],
							column10 = columns[baseIndex | 32],
							column11 = columns[baseIndex | 33];
						column00.setParamsUnchecked(column00.params.at(x,        z       ));
						column01.setParamsUnchecked(column01.params.at(x | step, z       ));
						column10.setParamsUnchecked(column10.params.at(x,        z | step));
						column11.setParamsUnchecked(column11.params.at(x | step, z | step));
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
						lists[baseIndex | 32] = list10;
						lists[baseIndex | 33] = list11;
					});
				}
			}
		}
		this.convertSection(levelX, levelZ, level, lists, storage);
	}
}