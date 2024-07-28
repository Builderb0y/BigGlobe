package builderb0y.bigglobe.compat.dhChunkGen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiChunk;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.RootLayer;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.compat.DistantHorizonsCompat.DHCode;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

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
	public CompletableFuture<Void> generateApiChunks(
		int chunkPosMinX,
		int chunkPosMinZ,
		byte granularity,
		byte targetDataDetail,
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
								BigGlobeMod.LOGGER.error("An error occurred in a hyperspeed DH world generator for chunk [" + chunkX + ", " + chunkZ + ']', throwable);
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
		DhApiChunk results = DHCode.newChunk(chunkX, chunkZ, chunkBottomY, chunkTopY);
		IDhApiBiomeWrapper biome = DhApi.Delayed.wrapperFactory.getBiomeWrapper(
			new Object[] {
				this
				.serverWorld
				.getRegistryManager()
				.get(RegistryKeyVersions.biome())
				.entryOf(BiomeKeys.PLAINS)
			},
			this.level
		);
		for (int index = 0; index < 256; index++) {
			//populate early to make sanity checking happen earlier.
			//we will mutate this list later.
			results.setDataPoints(index & 15, index >>> 4, new DataPointListBuilder(this.level, (byte)(0), biome));
		}
		int startX = chunkX << 4;
		int startZ = chunkZ << 4;
		ScriptedColumn[] columns = this.chunkGenerator.chunkReuseColumns.get();
		ScriptedColumn.Params params = new ScriptedColumn.Params(this.chunkGenerator, 0, 0, Purpose.RAW_DH);
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int offsetZ = 0; offsetZ < 16; offsetZ += 2) {
				final int offsetZ_ = offsetZ;
				for (int offsetX = 0; offsetX < 16; offsetX += 2) {
					final int offsetX_ = offsetX;
					async.submit(() -> {
						int minY = this.chunkGenerator.height.min_y();
						int maxY = this.chunkGenerator.height.max_y();
						RootLayer layer = this.chunkGenerator.layer;
						int baseIndex = (offsetZ_ << 4) | offsetX_;
						int quadX = startX | offsetX_;
						int quadZ = startZ | offsetZ_;
						ScriptedColumn
							column00 = columns[baseIndex     ],
							column01 = columns[baseIndex |  1],
							column10 = columns[baseIndex | 16],
							column11 = columns[baseIndex | 17];
						column00.setParamsUnchecked(params.at(quadX,     quadZ    ));
						column01.setParamsUnchecked(params.at(quadX | 1, quadZ    ));
						column10.setParamsUnchecked(params.at(quadX,     quadZ | 1));
						column11.setParamsUnchecked(params.at(quadX | 1, quadZ | 1));
						BlockSegmentList
							list00 = new BlockSegmentList(minY, maxY),
							list01 = new BlockSegmentList(minY, maxY),
							list10 = new BlockSegmentList(minY, maxY),
							list11 = new BlockSegmentList(minY, maxY);
						layer.emitSegments(column00, column01, column10, column11, list00);
						layer.emitSegments(column01, column00, column11, column10, list01);
						layer.emitSegments(column10, column11, column00, column01, list10);
						layer.emitSegments(column11, column10, column01, column00, list11);
						this.convertToDataPoints((DataPointListBuilder)(results.getDataPoints(offsetX_,     offsetZ_    )), list00);
						this.convertToDataPoints((DataPointListBuilder)(results.getDataPoints(offsetX_ | 1, offsetZ_    )), list01);
						this.convertToDataPoints((DataPointListBuilder)(results.getDataPoints(offsetX_,     offsetZ_ | 1)), list10);
						this.convertToDataPoints((DataPointListBuilder)(results.getDataPoints(offsetX_ | 1, offsetZ_ | 1)), list11);
					});
				}
			}
		}
		return results;
	}

	public void convertToDataPoints(DataPointListBuilder builder, BlockSegmentList segments) {
		segments.computeLightLevels();
		for (int index = segments.size(); --index >= 0;) {
			LitSegment segment = segments.getLit(index);
			//some versions of DH break if I don't provide air...
			//if (segment.value.isAir()) continue;
			builder.skyLightLevel = segment.lightLevel;
			builder.add(segment.value, segment.minY, segment.maxY + 1);
		}
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

	/*
	public static class DhDebugging {

		public static String debugDataPoints(FullDataSourceV2 source) {
			StringBuilder builder = new StringBuilder(16384).append("Source position: ").append(DhSectionPos.toString(source.getPos()));
			for (int z = 0; z < 64; z++) {
				for (int x = 0; x < 64; x++) {
					builder.append("\nColumn [").append(x).append(", ").append(z).append("]:");
					LongArrayList column = source.get(x, z);
					if (column == null) {
						builder.append(" null");
						continue;
					}
					for (int index = 0, size = column.size(); index < size; index++) {
						long dataPoint = column.getLong(index);
						builder
						.append("\n\tindex ")
						.append(index)
						.append(": ")
						.append(FullDataPointUtil.toString(dataPoint))
						.append("; ID maps to block ")
						.append(source.mapping.getBlockStateWrapper(FullDataPointUtil.getId(dataPoint)))
						.append(" and biome ")
						.append(source.mapping.getBiomeWrapper(FullDataPointUtil.getId(dataPoint)));
					}
				}
			}
			return builder.toString();
		}

		public static String debugDataPoints(LongArrayList column, FullDataPointIdMap mapping) {
			StringBuilder builder = new StringBuilder(1024);
			for (int index = 0, size = column.size(); index < size; index++) {
				long dataPoint = column.getLong(index);
				builder
				.append("\nindex ")
				.append(index)
				.append(": ")
				.append(FullDataPointUtil.toString(dataPoint))
				.append("; ID maps to block ")
				.append(mapping.getBlockStateWrapper(FullDataPointUtil.getId(dataPoint)))
				.append(" and biome ")
				.append(mapping.getBiomeWrapper(FullDataPointUtil.getId(dataPoint)));
			}
			return builder.toString();
		}

		public static String debugRenderPoints(ColumnRenderSource source) {
			StringBuilder builder = new StringBuilder(65536).append("Render source position: ").append(DhSectionPos.toString(source.pos));
			for (int z = 0; z < 64; z++) {
				for (int x = 0; x < 64; x++) {
					builder.append("\nColumn [").append(x).append(", ").append(z).append("]:");
					ColumnArrayView column = source.getVerticalDataPointView(x, z);
					for (int index = 0, size = column.size; index < size; index++) {
						long dataPoint = column.get(index);
						builder
						.append("\n\tindex ")
						.append(index)
						.append(": ")
						.append(RenderDataPointUtil.toString(dataPoint));
					}
				}
			}
			return builder.toString();
		}
	}
	//*/
}