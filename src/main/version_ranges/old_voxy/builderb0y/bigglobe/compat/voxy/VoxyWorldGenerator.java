package builderb0y.bigglobe.compat.voxy;

import me.cortex.voxy.common.world.WorldEngine;

import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.QuadHolder;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadColumn;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public class VoxyWorldGenerator extends AbstractVoxyWorldGenerator {

	public VoxyWorldGenerator(WorldEngine engine, ServerWorld world, BigGlobeScriptedChunkGenerator generator) {
		super(engine, world, generator);
	}

	@Override
	public void createChunk(int levelX, int levelZ, int level) {
		int startX = levelX << (level + 5);
		int startZ = levelZ << (level + 5);
		int step   = 1 << level;

		ScriptedColumn[] columns = this.columns;
		BlockSegmentList[] lists = new BlockSegmentList[1024];
		int minY = this.generator.height.min_y();
		int maxY = this.generator.height.max_y();
		Layer layer = this.generator.layer.value();
		ScriptedColumn.Params params = new ScriptedColumn.Params(this.generator, 0, 0, ColumnUsage.RAW_GENERATION.voxyHints(level));
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int offsetZ = 0; offsetZ < 32; offsetZ += 2) {
				int offsetZ_ = offsetZ;
				for (int offsetX = 0; offsetX < 32; offsetX += 2) {
					int offsetX_ = offsetX;
					async.submit(() -> {
						int x = startX | (offsetX_ << level);
						int z = startZ | (offsetZ_ << level);
						int baseIndex = (offsetZ_ << 5) | offsetX_;
						QuadColumn quadColumn = new QuadColumn();
						quadColumn.loadFromArray(columns, baseIndex, 32);
						quadColumn.at(params, x, z, step);
						//reminder: pre-computing column values will compute things that aren't needed, like deep dark noise.
						/*
						for (String name : this.generator.getOverriders().rawColumnValueDependencies) try {
							column00.preComputeColumnValue(name);
							column01.preComputeColumnValue(name);
							column10.preComputeColumnValue(name);
							column11.preComputeColumnValue(name);
						}
						catch (Throwable throwable) {
							BigGlobeMod.LOGGER.error("Exception pre-computing overrider column value: ", throwable);
						}
						for (RegistryEntry<ColumnValueOverrider.Entry> overrider : this.generator.getOverriders().rawColumnValues) {
							overrider.override(column00, ScriptStructures.EMPTY_SCRIPT_STRUCTURES);
							overrider.override(column01, ScriptStructures.EMPTY_SCRIPT_STRUCTURES);
							overrider.override(column10, ScriptStructures.EMPTY_SCRIPT_STRUCTURES);
							overrider.override(column11, ScriptStructures.EMPTY_SCRIPT_STRUCTURES);
						}
						*/
						QuadList quadList = new QuadList();
						quadList.createNew(minY, maxY);
						QuadHolder.generate(quadColumn, quadList, layer);
						quadList.computeLightLevels(this.topSkylight);
						quadList.storeInArray(lists, baseIndex, 32);
					});
				}
			}
		}
		this.convertSection(levelX, levelZ, level, lists);
	}
}