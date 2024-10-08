package builderb0y.bigglobe.compat.dhChunkGen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiChunk;
import com.seibel.distanthorizons.api.objects.data.IDhApiFullDataSource;

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
	public final ThreadLocal<ScriptedColumn[]> columns;

	public DhScriptedWorldGenerator(
		IDhApiLevelWrapper level,
		ServerWorld serverWorld,
		BigGlobeScriptedChunkGenerator chunkGenerator
	) {
		this.level = level;
		this.serverWorld = serverWorld;
		this.chunkGenerator = chunkGenerator;
		this.columns = new ThreadLocal<>();
	}

	public ScriptedColumn[] getColumns(int length) {
		ScriptedColumn[] columns = this.columns.get();
		if (columns == null || columns.length < length) {
			columns = new ScriptedColumn[length];
			ScriptedColumn.Params params = new ScriptedColumn.Params(this.chunkGenerator, 0, 0, Purpose.RAW_DH);
			ScriptedColumn.Factory factory = this.chunkGenerator.columnEntryRegistry.columnFactory;
			for (int index = 0; index < length; index++) {
				columns[index] = factory.create(params);
			}
			this.columns.set(columns);
		}
		return columns;
	}

	//note: this method is removed via ASM if API_DATA_SOURCES is unavailable.
	@Override
	public CompletableFuture<Void> generateLod(
		int chunkPosMinX,
		int chunkPosMinZ,
		int lodPosX,
		int lodPosZ,
		byte detailLevel,
		IDhApiFullDataSource pooledFullDataSource,
		EDhApiDistantGeneratorMode generatorMode,
		ExecutorService worldGeneratorThreadPool,
		Consumer<IDhApiFullDataSource> resultConsumer
	) {
		return CompletableFuture.runAsync(
			() -> {
				int step = 1 << detailLevel;
				int startX = chunkPosMinX << (detailLevel + 4);
				int startZ = chunkPosMinZ << (detailLevel + 4);
				int width = pooledFullDataSource.getWidthInDataColumns();
				int totalColumns = width * width;
				BigGlobeScriptedChunkGenerator generator = this.chunkGenerator;
				ScriptedColumn[] columns = this.getColumns(totalColumns);
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
				int yOffset = generator.height.min_y();
				DataPointListBuilder[] dataPointBuilders = new DataPointListBuilder[totalColumns];
				for (int index = 0; index < totalColumns; index++) {
					dataPointBuilders[index] = new DataPointListBuilder(this.level, (byte)(0), biome, yOffset);
				}
				try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
					for (int offsetZ = 0; offsetZ < width; offsetZ += 2) {
						final int offsetZ_ = offsetZ;
						for (int offsetX = 0; offsetX < width; offsetX += 2) {
							final int offsetX_ = offsetX;
							async.submit(() -> {
								int baseIndex = offsetZ_ * width + offsetX_;
								int quadX = startX | (offsetX_ << detailLevel);
								int quadZ = startZ | (offsetZ_ << detailLevel);
								ScriptedColumn
									column00 = columns[baseIndex            ],
									column01 = columns[baseIndex + 1        ],
									column10 = columns[baseIndex + width    ],
									column11 = columns[baseIndex + width + 1];
								column00.setParamsUnchecked(column00.params.at(quadX,        quadZ       ));
								column01.setParamsUnchecked(column01.params.at(quadX | step, quadZ       ));
								column10.setParamsUnchecked(column10.params.at(quadX,        quadZ | step));
								column11.setParamsUnchecked(column11.params.at(quadX | step, quadZ | step));
								BlockSegmentList
									list00 = new BlockSegmentList(generator.height.min_y(), generator.height.max_y()),
									list01 = new BlockSegmentList(generator.height.min_y(), generator.height.max_y()),
									list10 = new BlockSegmentList(generator.height.min_y(), generator.height.max_y()),
									list11 = new BlockSegmentList(generator.height.min_y(), generator.height.max_y());
								generator.layer.emitSegments(column00, column01, column10, column11, list00);
								generator.layer.emitSegments(column01, column00, column11, column10, list01);
								generator.layer.emitSegments(column10, column11, column00, column01, list10);
								generator.layer.emitSegments(column11, column10, column01, column00, list11);
								this.convertToDataPoints(dataPointBuilders[baseIndex            ], list00);
								this.convertToDataPoints(dataPointBuilders[baseIndex + 1        ], list01);
								this.convertToDataPoints(dataPointBuilders[baseIndex + width    ], list10);
								this.convertToDataPoints(dataPointBuilders[baseIndex + width + 1], list11);
							});
						}
					}
				}
				for (int offsetZ = 0; offsetZ < width; offsetZ++) {
					for (int offsetX = 0; offsetX < width; offsetX++) {
						pooledFullDataSource.setApiDataPointColumn(offsetX, offsetZ, dataPointBuilders[offsetZ * width + offsetX]);
					}
				}
				resultConsumer.accept(pooledFullDataSource);
			},
			worldGeneratorThreadPool
		);
	}

	public CompletableFuture<Void> generateApiChunks(
		int chunkPosMinX,
		int chunkPosMinZ,
		byte granularity,
		byte targetDataDetail,
		EDhApiDistantGeneratorMode generatorMode,
		ExecutorService worldGeneratorThreadPool,
		Consumer<DhApiChunk> resultConsumer
	) {
		return this.generateApiChunks(
			chunkPosMinX,
			chunkPosMinZ,
			1 << (granularity - 4),
			targetDataDetail,
			generatorMode,
			worldGeneratorThreadPool,
			resultConsumer
		);
	}

	@Override
	public CompletableFuture<Void> generateApiChunks(
		int chunkPosMinX,
		int chunkPosMinZ,
		int chunkWidth,
		byte targetDataDetail,
		EDhApiDistantGeneratorMode generatorMode,
		ExecutorService worldGeneratorThreadPool,
		Consumer<DhApiChunk> resultConsumer
	) {
		return CompletableFuture.runAsync(
			() -> {
				int chunkPosMaxX = chunkPosMinX + chunkWidth;
				int chunkPosMaxZ = chunkPosMinZ + chunkWidth;
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
			},
			worldGeneratorThreadPool
		);
	}

	public DhApiChunk generateChunkOfDataPoints(int chunkX, int chunkZ) {
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
		DataPointListBuilder[] dataPointBuilders = new DataPointListBuilder[256];
		for (int index = 0; index < 256; index++) {
			dataPointBuilders[index] = new DataPointListBuilder(this.level, (byte)(0), biome, 0);
		}
		ScriptedColumn[] columns = this.chunkGenerator.chunkReuseColumns.get();
		ScriptedColumn.Params params = new ScriptedColumn.Params(this.chunkGenerator, 0, 0, Purpose.RAW_DH);
		int startX = chunkX << 4;
		int startZ = chunkZ << 4;
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
						this.convertToDataPoints(dataPointBuilders[baseIndex     ], list00);
						this.convertToDataPoints(dataPointBuilders[baseIndex |  1], list01);
						this.convertToDataPoints(dataPointBuilders[baseIndex | 16], list10);
						this.convertToDataPoints(dataPointBuilders[baseIndex | 17], list11);
					});
				}
			}
		}
		int chunkBottomY = this.chunkGenerator.height.min_y();
		int chunkTopY    = this.chunkGenerator.height.max_y();
		DhApiChunk results = DHCode.newChunk(chunkX, chunkZ, chunkBottomY, chunkTopY);
		for (int index = 0; index < 256; index++) {
			results.setDataPoints(index & 15, index >>> 4, dataPointBuilders[index]);
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

	//note: this method is ASM'd to return API_CHUNKS if API_DATA_SOURCES is unavailable.
	@Override
	public EDhApiWorldGeneratorReturnType getReturnType() {
		return EDhApiWorldGeneratorReturnType.API_DATA_SOURCES;
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