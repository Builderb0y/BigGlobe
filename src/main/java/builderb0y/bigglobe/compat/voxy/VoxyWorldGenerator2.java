package builderb0y.bigglobe.compat.voxy;

import java.util.Arrays;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.WorldSection;
import me.cortex.voxy.common.world.other.Mapper;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.RootLayer;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.compat.voxy.GeneratingStorageBackend.GenerationProgressTracker;
import builderb0y.bigglobe.mixins.Voxy_WorldSection_DataGetter;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public class VoxyWorldGenerator2 {

	public BigGlobeScriptedChunkGenerator generator;
	public WorldEngine worldEngine;
	public ScriptedColumn[] columns;
	public GenerationProgressTracker queue;
	public volatile boolean running;
	public Thread thread;
	public RegistryEntry<Biome> plains;

	public VoxyWorldGenerator2(BigGlobeScriptedChunkGenerator generator, WorldEngine worldEngine) {
		this.generator = generator;
		this.worldEngine = worldEngine;
		this.columns = new ScriptedColumn[1024];
		ScriptedColumn.Params params = new ScriptedColumn.Params(generator, 0, 0, Purpose.RAW_VOXY);
		for (int index = 0; index < 1024; index++) {
			this.columns[index] = generator.columnEntryRegistry.columnFactory.create(params);
		}
		this.queue = new GenerationProgressTracker();
		for (StorageBackend backend : worldEngine.storage.collectAllBackends()) {
			if (backend instanceof GeneratingStorageBackend generating) {
				generating.queue = this.queue;
			}
		}
		this.thread = new Thread(this::runLoop, "Big Globe Voxy Worldgen Thread");
		this.plains = MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.PLAINS);
	}

	public void start() {
		this.running = true;
		this.thread.start();
	}

	public void stop() {
		this.running = false;
		try {
			this.thread.join();
		}
		catch (InterruptedException exception) {
			BigGlobeMod.LOGGER.error("Unexpected interruption stopping " + this.thread.getName() + ": ", exception);
		}
	}

	public void runLoop() {
		while (this.running) {
			long next = this.queue.poll();
			if (next != -1L) try {
				this.generateNextArea(WorldEngine.getX(next), WorldEngine.getZ(next), WorldEngine.getLevel(next));
			}
			catch (Exception exception) {
				BigGlobeMod.LOGGER.error("Exception generating area: ", exception);
			}
			else try {
				Thread.sleep(500L);
			}
			catch (InterruptedException ignored) {}
		}
	}

	public void generateNextArea(int levelX, int levelZ, int level) {
		int biomeID = this.worldEngine.getMapper().getIdForBiome(this.plains);
		int startX = levelX << (level + 5);
		int startZ = levelZ << (level + 5);
		int step   = 1 << level;

		ScriptedColumn[] columns = this.columns;
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
		for (int sectionBottomY = minY & -(1 << (level + 5)); sectionBottomY < maxY; sectionBottomY += 1 << (level + 5)) {
			WorldSection section = null;
			BlockState previousColumnState = null;
			int previousColumnStateID = -1;
			try {
				for (int relativeZ = 0; relativeZ < 32; relativeZ++) {
					for (int relativeX = 0; relativeX < 32; relativeX++) {
						int packedXZ = (relativeZ << 5) | relativeX;
						BlockSegmentList list = lists[packedXZ];
						int segmentIndex = list.getSegmentIndex(sectionBottomY, false);
						while (segmentIndex < list.size()) {
							LitSegment segment = list.getLit(segmentIndex++);
							if (segment.minY > (sectionBottomY | ((1 << (level + 5)) - 1))) break;
							if (!segment.value.isAir()) {
								if (section == null) {
									section = this.worldEngine.acquire(level, levelX, sectionBottomY >> (level + 5), levelZ);
									Arrays.fill(((Voxy_WorldSection_DataGetter)(Object)(section)).bigglobe_getData(), 0L);
								}
								int minRelativeY = Math.max((segment.minY - sectionBottomY) >> level, 0);
								int maxRelativeY = Math.min((segment.maxY - sectionBottomY) >> level, 31);
								if (segment.value != previousColumnState) {
									previousColumnStateID = this.worldEngine.getMapper().getIdForBlockState(previousColumnState = segment.value);
								}
								byte startLightLevel = segment.lightLevel;
								int diminishment = previousColumnState.getOpacity(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
								if (startLightLevel == 0 || diminishment == 0) {
									long id = Mapper.composeMappingId((byte)(15 - startLightLevel), previousColumnStateID, biomeID);
									for (int relativeY = minRelativeY; relativeY <= maxRelativeY; relativeY++) {
										section.set(relativeX, relativeY, relativeZ, id);
									}
								}
								else {
									for (int relativeY = minRelativeY; relativeY <= maxRelativeY; relativeY++) {
										int lightLevel = Math.max(startLightLevel - diminishment * (segment.maxY - (relativeY + sectionBottomY)), 0);
										section.set(relativeX, relativeY, relativeZ, Mapper.composeMappingId((byte)(15 - lightLevel), previousColumnStateID, biomeID));
									}
								}
							}
						}
					}
				}
				if (section != null) this.worldEngine.markDirty(section);
			}
			finally {
				if (section != null) section.release();
			}
		}
	}
}