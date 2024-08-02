package builderb0y.bigglobe.compat.voxy;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import me.cortex.voxy.common.world.WorldEngine;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;

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
		super(engine, world, generator, DistanceGraph.worldOfChunks());
		this.states = states;
	}

	@Override
	public void createChunk(int chunkX, int chunkZ, RegistryEntry<Biome> biome) {
		int startX = chunkX << 4;
		int startZ = chunkZ << 4;
		ScriptedColumn[] columns = this.columns;
		BlockSegmentList[] lists = new BlockSegmentList[256];
		int minY = this.generator.height.min_y();
		int maxY = this.generator.height.max_y();
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int offsetZ = 0; offsetZ < 16; offsetZ++) {
				int offsetZ_ = offsetZ;
				for (int offsetX = 0; offsetX < 16; offsetX++) {
					int offsetX_ = offsetX;
					async.submit(() -> {
						int baseIndex = (offsetZ_ << 4) | offsetX_;
						ScriptedColumn column = columns[baseIndex];
						column.setParamsUnchecked(column.params.at(startX | offsetX_, startZ | offsetZ_));
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
		for (int y = minY; y < maxY; y += 16) {
			VoxelizedSection section = this.convertSection(chunkX, y >> 4, chunkZ, lists, biome);
			if (section != null) this.engine.insertUpdate(section);
		}
	}

	@Override
	public void save() {
		//no-op.
	}
}