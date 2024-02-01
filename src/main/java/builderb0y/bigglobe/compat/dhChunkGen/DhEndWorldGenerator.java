package builderb0y.bigglobe.compat.dhChunkGen;

import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;

import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.chunkgen.BigGlobeEndChunkGenerator;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.NumberArray;

public class DhEndWorldGenerator extends AbstractDhWorldGenerator {

	public final BigGlobeEndChunkGenerator generator;

	public DhEndWorldGenerator(IDhApiLevelWrapper level, ServerWorld serverWorld, BigGlobeEndChunkGenerator generator) {
		super(level, serverWorld);
		this.generator = generator;
	}

	@Override
	public BigGlobeChunkGenerator getGenerator() {
		return this.generator;
	}

	@Override
	public void prepareColumn(WorldColumn column) {
		EndColumn endColumn = (EndColumn)(column);
		endColumn.getNestNoise();
		endColumn.getMountainCenterY();
		endColumn.getMountainThickness();
		endColumn.getFoliage();
		endColumn.getLowerRingCloudNoise();
		endColumn.getUpperRingCloudNoise();
		endColumn.getLowerBridgeCloudNoise();
		endColumn.getUpperBridgeCloudNoise();
	}

	@Override
	public DataPointListPopulator getDataPointPopulator(int chunkX, int chunkZ) {
		IDhApiBlockStateWrapper endStone = this.blockState(new Object[1], BlockStates.END_STONE);
		return (ChunkOfColumns<? extends WorldColumn> columns, int columnIndex, DataPointListBuilder builder) -> {
			EndColumn
				column            = (EndColumn)(columns.getColumn(columnIndex));
			NumberArray
				nestNoise         = column.getNestNoise(),
				lowerRingNoise    = column.getLowerRingCloudNoise(),
				upperRingNoise    = column.getUpperRingCloudNoise(),
				lowerBridgeNoise  = column.getLowerBridgeCloudNoise(),
				upperBridgeNoise  = column.getUpperBridgeCloudNoise();
			int
				nestStartY        = column.settings.nest.min_y(),
				lowerRingStartY   = column.getLowerRingCloudSampleStartY(),
				upperRingStartY   = column.getUpperRingCloudSampleStartY(),
				lowerBridgeStartY = column.getLowerBridgeCloudSampleStartY(),
				upperBridgeStartY = column.getUpperBridgeCloudSampleStartY();
			this.addVolumetric(builder, endStone, upperRingNoise, upperRingStartY);
			this.addVolumetric(builder, endStone, upperBridgeNoise, upperBridgeStartY);
			this.addVolumetric(builder, endStone, nestNoise, nestStartY);
			/*
			if (column.hasTerrain()) builder.add(
				column.settings.biomes.getPrimarySurface(
					column,
					column.getFinalTopHeightD(),
					this.serverWorld.getSeed()
				)
				.top(),
				column.getFinalBottomHeightI(),
				column.getFinalTopHeightI()
			);
			*/
			this.addVolumetric(builder, endStone, lowerRingNoise, lowerRingStartY);
			this.addVolumetric(builder, endStone, lowerBridgeNoise, lowerBridgeStartY);
		};
	}

	/*
	@Override
	public DhApiChunkOfDataPoints generateChunkOfDataPoints(int chunkX, int chunkZ, byte detailLevel) {
		record Data(int x, int z, List<DhApiTerrainDataPoint> dataPoints) {}
		int chunkBottomY = this.generator.settings.min_y;
		DhApiChunkOfDataPoints result = new DhApiChunkOfDataPoints(chunkX, chunkZ, chunkBottomY, this.generator.settings.max_y);
		ChunkOfColumns<EndColumn> columns = this.generator.chunkOfColumnsRecycler.get().asType(EndColumn.class);
		columns.setPosUncheckedAndPopulate(chunkX << 4, chunkZ << 4, (EndColumn column) -> {
			column.getNestNoise();
			column.getMountainCenterY();
			column.getMountainThickness();
			column.getFoliage();
			column.getLowerRingCloudNoise();
			column.getUpperRingCloudNoise();
			column.getLowerBridgeCloudNoise();
			column.getUpperBridgeCloudNoise();
		});

		try (AsyncConsumer<Data> async = new AsyncConsumer<>((Data data) -> {
			result.setDataPoints(data.x, data.z, data.dataPoints);
		})) {
			IDhApiBlockStateWrapper endStone = this.blockState(new Object[1], BlockStates.END_STONE);
			for (int columnIndex = 0; columnIndex < 256; columnIndex++) {
				final int columnIndex_ = columnIndex;
				async.submit(() -> {
					DataPointListBuilder builder = new DataPointListBuilder(this.level, detailLevel);
					EndColumn column = columns.getColumn(columnIndex_);
					builder.biome = this.biome(builder.query, column.getSurfaceBiome());
					NumberArray nestNoise = column.getNestNoise();
					NumberArray lowerRingNoise = column.getLowerRingCloudNoise();
					NumberArray upperRingNoise = column.getUpperRingCloudNoise();
					NumberArray lowerBridgeNoise = column.getLowerBridgeCloudNoise();
					NumberArray upperBridgeNoise = column.getUpperBridgeCloudNoise();
					int nestStartY = column.settings.nest.min_y();
					int lowerRingStartY = column.getLowerRingCloudSampleStartY();
					int upperRingStartY = column.getUpperRingCloudSampleStartY();
					int lowerBridgeStartY = column.getLowerBridgeCloudSampleStartY();
					int upperBridgeStartY = column.getUpperBridgeCloudSampleStartY();
					this.addVolumetric(builder, endStone, upperRingNoise, upperRingStartY);
					this.addVolumetric(builder, endStone, upperBridgeNoise, upperBridgeStartY);
					this.addVolumetric(builder, endStone, nestNoise, nestStartY);
					if (column.hasTerrain()) builder.add(column.settings.biomes.getPrimarySurface(column, column.getFinalTopHeightD(), this.serverWorld.getSeed()).top(), column.getFinalBottomHeightI(), column.getFinalTopHeightI());
					this.addVolumetric(builder, endStone, lowerRingNoise, lowerRingStartY);
					this.addVolumetric(builder, endStone, lowerBridgeNoise, lowerBridgeStartY);
					return new Data(column.x & 15, column.z & 15, builder);
				});
			}
		}
		this.generator.chunkOfColumnsRecycler.reclaim(columns);
		return result;
	}
	*/

	public void addVolumetric(DataPointListBuilder builder, IDhApiBlockStateWrapper endStone, NumberArray noise, int startY) {
		if (noise != null) {
			int endY = startY + noise.length();
			IDhApiBlockStateWrapper current = null;
			int topOfSegment = endY;
			for (int y = endY; --y >= startY;) {
				IDhApiBlockStateWrapper next = noise.getF(y - startY) > 0.0F ? endStone : null;
				if (next != current) {
					if (current != null) {
						builder.add(current, y + 1, topOfSegment);
					}
					current = next;
					topOfSegment = y + 1;
				}
			}
		}
	}
}