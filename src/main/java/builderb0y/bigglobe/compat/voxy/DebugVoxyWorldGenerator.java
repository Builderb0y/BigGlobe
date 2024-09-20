package builderb0y.bigglobe.compat.voxy;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.world.WorldEngine;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public class DebugVoxyWorldGenerator extends AbstractVoxyWorldGenerator {

	public final Object2ObjectArrayMap<BlockState, ColumnToIntScript.Holder> states;

	public DebugVoxyWorldGenerator(
		WorldEngine engine,
		ServerWorld world,
		BigGlobeScriptedChunkGenerator generator,
		Object2ObjectArrayMap<BlockState, ColumnToIntScript.Holder> states
	) {
		super(engine, world, generator);
		this.states = states;
	}

	@Override
	public void createChunk(int levelX, int levelZ, int level, StorageBackend storage) {
		int startX = levelX << (level + 5);
		int startZ = levelZ << (level + 5);

		ScriptedColumn[] columns = this.columns.get();
		BlockSegmentList[] lists = new BlockSegmentList[1024];
		int minY = this.generator.height.min_y();
		int maxY = this.generator.height.max_y();
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int offsetZ = 0; offsetZ < 32; offsetZ ++) {
				int offsetZ_ = offsetZ;
				for (int offsetX = 0; offsetX < 32; offsetX ++) {
					int offsetX_ = offsetX;
					async.submit(() -> {
						int x = startX | (offsetX_ << level);
						int z = startZ | (offsetZ_ << level);
						int baseIndex = (offsetZ_ << 5) | offsetX_;
						ScriptedColumn column = columns[baseIndex];
						column.setParamsUnchecked(column.params.at(x, z));
						BlockSegmentList list = new BlockSegmentList(minY, maxY);
						for (
							ObjectIterator<Object2ObjectMap.Entry<BlockState, ColumnToIntScript.Holder>> iterator = this.states.object2ObjectEntrySet().fastIterator();
							iterator.hasNext();
						) {
							Object2ObjectMap.Entry<BlockState, ColumnToIntScript.Holder> entry = iterator.next();
							list.setBlockState(entry.getValue().get(column), entry.getKey());
						}
						lists[baseIndex] = list;
					});
				}
			}
		}
		this.convertSection(levelX, levelZ, level, lists, storage);
	}
}