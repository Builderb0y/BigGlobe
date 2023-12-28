package builderb0y.bigglobe.compat.dhChunkGen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiChunk;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.util.AsyncRunner;

public abstract class AbstractDhWorldGenerator implements IDhApiWorldGenerator {

	public final IDhApiLevelWrapper level;
	public final ServerWorld serverWorld;
	public final AtomicInteger runningCount, maxRunningCount;

	public AbstractDhWorldGenerator(IDhApiLevelWrapper level, ServerWorld serverWorld) {
		this.level = level;
		this.serverWorld = serverWorld;
		this.runningCount = new AtomicInteger();
		this.maxRunningCount = new AtomicInteger();
	}

	@Override
	public boolean isBusy() {
		int runningCount = this.runningCount.get();
		int maxRunningCount = runningCount == 0 ? this.maxRunningCount.incrementAndGet() : this.maxRunningCount.get();
		return this.runningCount.get() >= maxRunningCount;
	}

	@Override
	public CompletableFuture<Void> generateApiChunks(
		int chunkPosMinX,
		int chunkPosMinZ,
		byte granularity,
		byte targetDetailLevel,
		EDhApiDistantGeneratorMode generatorMode,
		ExecutorService worldGeneratorThreadPool,
		Consumer<DhApiChunk> resultConsumer
	) {
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
		int chunkBottomY = this.getGenerator().getMinimumY();
		DhApiChunk results = new DhApiChunk(chunkX, chunkZ, chunkBottomY, chunkBottomY + this.getGenerator().getWorldHeight());
		for (int index = 0; index < 256; index++) {
			//populate early to make sanity checking happen earlier.
			//we will mutate this array later.
			results.setDataPoints(index & 15, index >>> 4, new DataPointListBuilder(this.level, (byte)(0)));
		}
		ChunkOfColumns<? extends WorldColumn> columns = this.getGenerator().chunkOfColumnsRecycler.get();
		columns.setPosUncheckedAndPopulate(chunkX << 4, chunkZ << 4, this::prepareColumn);
		DataPointListPopulator populator = this.getDataPointPopulator(chunkX, chunkZ);
		try (AsyncRunner async = new AsyncRunner()) {
			for (int columnIndex = 0; columnIndex < 256; columnIndex++) {
				final int columnIndex_ = columnIndex;
				async.submit(() -> {
					WorldColumn column = columns.getColumn(columnIndex_);
					DataPointListBuilder builder = (DataPointListBuilder)(results.getDataPoints(column.x & 15, column.z & 15));
					builder.biome = this.biome(builder.query, column.getSurfaceBiome());
					populator.populateDataPoints(columns, columnIndex_, builder);
				});
			}
		}
		this.getGenerator().chunkOfColumnsRecycler.reclaim(columns);
		return results;
	}

	public abstract BigGlobeChunkGenerator getGenerator();

	public abstract void prepareColumn(WorldColumn column);

	/**
	problem: different subclasses need to pre-compute different values once.
	it would be inefficient to compute them with every iteration of the loop,
	it would be impossible to have different arguments for this method for different subclasses,
	and it would be messy to have this method take an Object[] with a different method to populate that Object[].
	solution: allow subclasses to return a lambda expression which captures whatever they need.
	it's a hacky solution, but it works.
	*/
	public abstract DataPointListPopulator getDataPointPopulator(int chunkX, int chunkZ);

	public static interface DataPointListPopulator {

		public abstract void populateDataPoints(ChunkOfColumns<? extends WorldColumn> columns, int columnIndex, DataPointListBuilder builder);
	}

	@Override
	public EDhApiWorldGeneratorReturnType getReturnType() {
		return EDhApiWorldGeneratorReturnType.API_CHUNKS;
	}

	public IDhApiBlockStateWrapper blockState(Object[] array, BlockState state) {
		array[0] = state;
		return DhApi.Delayed.wrapperFactory.getBlockStateWrapper(array, this.level);
	}

	public IDhApiBiomeWrapper biome(Object[] array, RegistryEntry<Biome> biome) {
		array[0] = biome;
		return DhApi.Delayed.wrapperFactory.getBiomeWrapper(array, this.level);
	}

	@Override
	public void preGeneratorTaskStart() {

	}

	@Override
	public void close() {

	}
}