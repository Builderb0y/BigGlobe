package builderb0y.bigglobe.compat.distanthorizons;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiChunk;
import com.seibel.distanthorizons.api.objects.data.IDhApiFullDataSource;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.QuadHolder;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadColumn;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat.DHCode;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.versions.RegistryVersions;

public class DhScriptedWorldGenerator implements IDhApiWorldGenerator {

	public final IDhApiLevelWrapper level;
	public final ServerLevel serverWorld;
	public final BigGlobeScriptedChunkGenerator chunkGenerator;
	public final ThreadLocal<ScriptedColumn[]> columns;
	public final byte topSkylight;

	public DhScriptedWorldGenerator(
		IDhApiLevelWrapper level,
		ServerLevel serverWorld,
		BigGlobeScriptedChunkGenerator chunkGenerator
	) {
		this.level = level;
		this.serverWorld = serverWorld;
		this.chunkGenerator = chunkGenerator;
		this.columns = new ThreadLocal<>();
		this.topSkylight = serverWorld.dimensionType().hasSkyLight() ? ((byte)(15)) : ((byte)(0));
	}

	public ScriptedColumn[] getColumns(int length) {
		ScriptedColumn[] columns = this.columns.get();
		if (columns == null || columns.length < length) {
			columns = new ScriptedColumn[length];
			ScriptedColumn.Params params = new ScriptedColumn.Params(this.chunkGenerator, 0, 0, ColumnUsage.RAW_GENERATION.normalHints());
			ScriptedColumn.Factory factory = this.chunkGenerator.columnEntryRegistry.columnFactory;
			for (int index = 0; index < length; index++) {
				if (columns[index] == null) columns[index] = factory.create(params);
				else columns[index].setParamsUnchecked(params);
			}
			this.columns.set(columns);
		}
		return columns;
	}

	@Override
	public byte getLargestDataDetailLevel() {
		return 20;
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
		//System.out.println("Request for chunk [" + chunkPosMinX + ", " + chunkPosMinZ + "], LOD [" + lodPosX + ", " + lodPosZ + "] @ " + detailLevel);
		return CompletableFuture.runAsync(
			() -> {
				int step = 1 << detailLevel;
				int startX = chunkPosMinX << 4;
				int startZ = chunkPosMinZ << 4;
				int width = pooledFullDataSource.getWidthInDataColumns();
				int totalColumns = width * width;
				BigGlobeScriptedChunkGenerator generator = this.chunkGenerator;
				ScriptedColumn[] columns = this.getColumns(totalColumns);
				IDhApiBiomeWrapper biome = DhApi.Delayed.wrapperFactory.getBiomeWrapper(
					new Object[] {
						RegistryVersions.getEntry(
							this
								.serverWorld
								.registryAccess(),
							Biomes.PLAINS
						)
					},
					this.level
				);
				int yOffset = generator.height.min_y();
				DataPointListBuilder[] dataPointBuilders = new DataPointListBuilder[totalColumns];
				for (int index = 0; index < totalColumns; index++) {
					dataPointBuilders[index] = new DataPointListBuilder(this.level, (byte)(0), biome, yOffset);
				}
				Layer layer = generator.layer.value();
				ScriptedColumn.Params params = new ScriptedColumn.Params(this.chunkGenerator, 0, 0, ColumnUsage.RAW_GENERATION.dhHints(detailLevel));
				try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
					for (int offsetZ = 0; offsetZ < width; offsetZ += 2) {
						final int offsetZ_ = offsetZ;
						for (int offsetX = 0; offsetX < width; offsetX += 2) {
							final int offsetX_ = offsetX;
							async.submit(() -> {
								int baseIndex = offsetZ_ * width + offsetX_;
								int quadX = startX | (offsetX_ << detailLevel);
								int quadZ = startZ | (offsetZ_ << detailLevel);
								QuadColumn quadColumn = new QuadColumn();
								quadColumn.loadFromArray(columns, baseIndex, width);
								quadColumn.at(params, quadX, quadZ, step);
								/*
								//pre-computing column values results in
								//computation of noise which will never
								//be used, like cave/deep dark/core noise.
								for (String name : this.chunkGenerator.getOverriders().rawColumnValueDependencies) try {
									quadColumn.preComputeColumnValue(name);
								}
								catch (Throwable throwable) {
									BigGlobeMod.LOGGER.error("Exception pre-computing overrider column value: ", throwable);
								}
								for (ColumnValueOverrider.Holder overrider : this.chunkGenerator.getOverriders().rawColumnValues) {
									quadColumn.override(overrider, ScriptStructures.EMPTY_SCRIPT_STRUCTURES);
								}
								*/
								QuadList quadList = new QuadList();
								quadList.createNew(generator.height.min_y(), generator.height.max_y());
								QuadHolder.generate(quadColumn, quadList, layer);
								this.convertToDataPoints(quadList, dataPointBuilders, baseIndex, width);
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
				RegistryVersions.getEntry(
					this
						.serverWorld
						.registryAccess(),
					Biomes.PLAINS
				)
			},
			this.level
		);
		DataPointListBuilder[] dataPointBuilders = new DataPointListBuilder[256];
		for (int index = 0; index < 256; index++) {
			dataPointBuilders[index] = new DataPointListBuilder(this.level, (byte)(0), biome, 0);
		}
		int chunkBottomY = this.chunkGenerator.height.min_y();
		int chunkTopY = this.chunkGenerator.height.max_y();
		DhApiChunk results = DHCode.newChunk(chunkX, chunkZ, chunkBottomY, chunkTopY);
		ScriptedColumn[] columns;
		try {
			columns = this.chunkGenerator.columnEntryRegistry.chunkReuseColumns.take();
		}
		catch (InterruptedException exception) {
			BigGlobeMod.LOGGER.warn("Unexpected interrupt", exception);
			return results;
		}
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			ScriptedColumn.Params params = new ScriptedColumn.Params(this.chunkGenerator, 0, 0, ColumnUsage.RAW_GENERATION.dhHints(0));
			int startX = chunkX << 4;
			int startZ = chunkZ << 4;
			for (int offsetZ = 0; offsetZ < 16; offsetZ += 2) {
				final int offsetZ_ = offsetZ;
				for (int offsetX = 0; offsetX < 16; offsetX += 2) {
					final int offsetX_ = offsetX;
					async.submit(() -> {
						int minY = this.chunkGenerator.height.min_y();
						int maxY = this.chunkGenerator.height.max_y();
						Layer layer = this.chunkGenerator.layer.value();
						int baseIndex = (offsetZ_ << 4) | offsetX_;
						int quadX = startX | offsetX_;
						int quadZ = startZ | offsetZ_;
						QuadColumn quadColumn = new QuadColumn();
						quadColumn.loadFromArray(columns, baseIndex, 16);
						quadColumn.at(params, quadX, quadZ, 1);
						QuadList quadList = new QuadList();
						quadList.createNew(minY, maxY);
						QuadHolder.generate(quadColumn, quadList, layer);
						this.convertToDataPoints(quadList, dataPointBuilders, baseIndex, 16);
					});
				}
			}
		}
		finally {
			this.chunkGenerator.columnEntryRegistry.chunkReuseColumns.add(columns);
		}
		for (int index = 0; index < 256; index++) {
			results.setDataPoints(index & 15, index >>> 4, dataPointBuilders[index]);
		}
		return results;
	}

	public void convertToDataPoints(QuadList quadList, DataPointListBuilder[] dataPointBuilders, int baseIndex, int zOffset) {
		this.convertToDataPoints(dataPointBuilders[baseIndex], quadList.object00);
		this.convertToDataPoints(dataPointBuilders[baseIndex + 1], quadList.object01);
		this.convertToDataPoints(dataPointBuilders[baseIndex + zOffset], quadList.object10);
		this.convertToDataPoints(dataPointBuilders[baseIndex + zOffset + 1], quadList.object11);
	}

	public void convertToDataPoints(DataPointListBuilder builder, BlockSegmentList segments) {
		segments.computeLightLevels(this.topSkylight);
		for (int index = segments.size(); --index >= 0; ) {
			LitSegment segment = segments.get(index);
			//some versions of DH break if I don't provide air...
			//if (segment.value.isAir()) continue;
			builder.skyLightLevel = segment.skylightLevel;
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