package builderb0y.bigglobe.compat.dhChunkGen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiChunk;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.SegmentList.Segment;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.util.AsyncRunner;

public class DhScriptedWorldGenerator implements IDhApiWorldGenerator {

	public final IDhApiLevelWrapper level;
	public final ServerWorld serverWorld;
	public final BigGlobeScriptedChunkGenerator chunkGenerator;
	public final AtomicInteger runningCount, maxRunningCount;

	public DhScriptedWorldGenerator(
		IDhApiLevelWrapper level,
		ServerWorld serverWorld,
		BigGlobeScriptedChunkGenerator chunkGenerator
	) {
		this.level = level;
		this.serverWorld = serverWorld;
		this.chunkGenerator = chunkGenerator;
		this.runningCount = new AtomicInteger();
		this.maxRunningCount = new AtomicInteger();
	}

	@Override
	public boolean isBusy() {
		int runningCount = this.runningCount.get();
		int maxRunningCount = runningCount == 0 ? this.maxRunningCount.incrementAndGet() : this.maxRunningCount.get();
		return runningCount >= maxRunningCount;
	}

	@Override
	public CompletableFuture<Void> generateApiChunks(int chunkPosMinX, int chunkPosMinZ, byte granularity, byte targetDataDetail, EDhApiDistantGeneratorMode generatorMode, ExecutorService worldGeneratorThreadPool, Consumer<DhApiChunk> resultConsumer) {
		this.runningCount.getAndIncrement();
		return CompletableFuture.runAsync(
			() -> {
				try {
					int chunkPosMaxX = chunkPosMinX + (1 << (granularity - 4));
					int chunkPosMaxZ = chunkPosMinZ + (1 << (granularity - 4));
					for (int chunkZ = chunkPosMinZ; chunkZ < chunkPosMaxZ; chunkZ++) {
						for (int chunkX = chunkPosMinX; chunkX < chunkPosMaxX; chunkX++) {
							try {
								resultConsumer.accept(this.generateChunkOfDataPoints(chunkX, chunkZ));
							}
							catch (Throwable throwable) {
								BigGlobeMod.LOGGER.error("An error occurred in a hyperspeed DH world generator: ", throwable);
								throw AutoCodecUtil.rethrow(throwable);
							}
						}
					}
				}
				finally {
					this.runningCount.getAndDecrement();
				}
			},
			worldGeneratorThreadPool
		);
	}

	public DhApiChunk generateChunkOfDataPoints(int chunkX, int chunkZ) {
		int chunkBottomY = this.chunkGenerator.height.min_y();
		int chunkTopY    = this.chunkGenerator.height.max_y();
		DhApiChunk results = new DhApiChunk(chunkX, chunkZ, chunkBottomY, chunkTopY);
		for (int index = 0; index < 256; index++) {
			//populate early to make sanity checking happen earlier.
			//we will mutate this array later.
			results.setDataPoints(index & 15, index >>> 4, new DataPointListBuilder(this.level, (byte)(0)));
		}
		ScriptedColumn.Factory factory = this.chunkGenerator.columnEntryRegistry.columnFactory;
		int startX = chunkX << 4;
		int startZ = chunkZ << 4;
		try (AsyncRunner async = new AsyncRunner()) {
			for (int columnIndex = 0; columnIndex < 256; columnIndex++) {
				final int columnIndex_ = columnIndex;
				async.submit(() -> {
					ScriptedColumn column = factory.create(
						this.chunkGenerator.seed,
						startX | (columnIndex_ & 15),
						startZ | (columnIndex_ >>> 4),
						this.chunkGenerator.height.min_y(),
						this.chunkGenerator.height.max_y()
					);
					DataPointListBuilder builder = (DataPointListBuilder)(results.getDataPoints(column.x & 15, column.z & 15));
					builder.query[0] = this.chunkGenerator.biomeRegistry.getOrCreateEntry(BiomeKeys.PLAINS);
					builder.biome = DhApi.Delayed.wrapperFactory.getBiomeWrapper(builder.query, this.level);
					BlockSegmentList list = new BlockSegmentList(chunkBottomY, chunkTopY);
					this.chunkGenerator.layer.emitSegments(column, list);
					for (int index = list.size(); --index >= 0;) {
						Segment<BlockState> segment = list.get(index);
						if (segment.value.isAir()) continue;
						builder.add(segment.value, segment.minY, segment.maxY + 1);
						builder.lightLevel = Math.max(builder.lightLevel - segment.value.getOpacity(EmptyBlockView.INSTANCE, BlockPos.ORIGIN) * (segment.maxY - segment.minY + 1), 0);
					}
				});
			}
		}
		return results;
	}

	@Override
	public EDhApiWorldGeneratorReturnType getReturnType() {
		return EDhApiWorldGeneratorReturnType.API_CHUNKS;
	}

	@Override
	public void preGeneratorTaskStart() {

	}

	@Override
	public void close() {

	}
}