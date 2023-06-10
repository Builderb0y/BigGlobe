package builderb0y.bigglobe.chunkgen;

import java.util.Arrays;
import java.util.concurrent.Executor;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.settings.EndSettings;
import builderb0y.bigglobe.util.SemiThreadLocal;

@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeEndChunkGenerator extends BigGlobeChunkGenerator {

	public static final AutoCoder<BigGlobeEndChunkGenerator> END_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(BigGlobeEndChunkGenerator.class);
	public static final Codec<BigGlobeEndChunkGenerator> END_CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(END_CODER);

	@EncodeInline
	public final EndSettings settings;
	public transient SemiThreadLocal<ChunkOfColumns<EndColumn>> chunkColumnCache;

	public BigGlobeEndChunkGenerator(EndSettings settings, SortedFeatures configuredFeatures) {
		super(new ColumnBiomeSource(settings.biomes().stream()), configuredFeatures);
		this.settings = settings;
	}

	public static void init() {
		Registry.register(Registries.CHUNK_GENERATOR, BigGlobeMod.modID("end"), END_CODEC);
	}

	public static AutoCoder<BigGlobeEndChunkGenerator> createCoder(FactoryContext<BigGlobeEndChunkGenerator> context) {
		return BigGlobeChunkGenerator.createCoder(context, "bigglobe", "the_end");
	}

	@Override
	public void setSeed(long seed) {
		super.setSeed(seed);
		this.chunkColumnCache = SemiThreadLocal.weak(4, () -> (
			new ChunkOfColumns<>(EndColumn[]::new, this::column)
		));
	}

	@Override
	public EndColumn column(int x, int z) {
		return new EndColumn(this.settings, this.seed, x, z);
	}

	public void generateRawSections(Chunk chunk, ChunkOfColumns<EndColumn> columns, boolean distantHorizons) {
		int minSurface = Integer.MAX_VALUE;
		int maxSurface = Integer.MIN_VALUE;
		for (EndColumn column : columns.columns) {
			if (column.hasTerrain()) {
				minSurface = Math.min(minSurface, column.getFinalBottomHeightI());
				maxSurface = Math.max(maxSurface, column.getFinalTopHeightI());
			}
			int lowerStart = column.getLowerRingCloudSampleStartY();
			int upperEnd   = column.getUpperRingCloudSampleEndY();
			if (lowerStart != Integer.MIN_VALUE) minSurface = Math.min(minSurface, lowerStart);
			if (upperEnd   != Integer.MIN_VALUE) maxSurface = Math.max(maxSurface, upperEnd);
			lowerStart = column.getLowerBridgeCloudSampleStartY();
			upperEnd   = column.getUpperBridgeCloudSampleEndY();
			if (lowerStart != Integer.MIN_VALUE) minSurface = Math.min(minSurface, lowerStart);
			if (upperEnd   != Integer.MIN_VALUE) maxSurface = Math.max(maxSurface, upperEnd);
		}
		if (maxSurface > minSurface) { //will also verify that the chunk has terrain in it somewhere.
			this.generateSectionsParallelSimple(chunk, minSurface, maxSurface, columns, context -> {
				int startY = context.startY();
				int solidCount = 0;
				for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
					EndColumn column = columns.getColumn(horizontalIndex);
					int endStoneID = context.id(BlockStates.END_STONE);
					PaletteStorage storage = context.storage();
					int mountainMinY, mountainMaxY;
					if (column.hasTerrain()) {
						mountainMinY = Math.max(0, column.getFinalBottomHeightI() - startY);
						mountainMaxY = Math.min(16, column.getFinalTopHeightI() - startY);
					}
					else {
						mountainMinY = mountainMaxY = -1;
					}
					double[] lowerRingNoise = column.getLowerRingCloudNoise();
					double[] upperRingNoise = column.getUpperRingCloudNoise();
					double[] lowerBridgeNoise = column.getLowerBridgeCloudNoise();
					double[] upperBridgeNoise = column.getUpperBridgeCloudNoise();
					int lowerRingStartY = column.getLowerRingCloudSampleStartY();
					int upperRingStartY = column.getUpperRingCloudSampleStartY();
					int lowerBridgeStartY = column.getLowerBridgeCloudSampleStartY();
					int upperBridgeStartY = column.getUpperBridgeCloudSampleStartY();
					for (int yIndex = 0; yIndex < 16; yIndex++) {
						int y = yIndex | startY;
						if (
							(yIndex >= mountainMinY && yIndex < mountainMaxY)
							|| (lowerRingNoise != null && y >= lowerRingStartY && y < lowerRingStartY + lowerRingNoise.length && lowerRingNoise[y - lowerRingStartY] > 0.0D)
							|| (upperRingNoise != null && y >= upperRingStartY && y < upperRingStartY + upperRingNoise.length && upperRingNoise[y - upperRingStartY] > 0.0D)
							|| (lowerBridgeNoise != null && y >= lowerBridgeStartY && y < lowerBridgeStartY + lowerBridgeNoise.length && lowerBridgeNoise[y - lowerBridgeStartY] > 0.0D)
							|| (upperBridgeNoise != null && y >= upperBridgeStartY && y < upperBridgeStartY + upperBridgeNoise.length && upperBridgeNoise[y - upperBridgeStartY] > 0.0D)
						) {
							storage.set(horizontalIndex | (yIndex << 8), endStoneID);
							solidCount++;
						}
					}
				}
				context.setNonEmpty(solidCount);
			});
		}
	}

	@Override
	public void generateRawTerrain(Executor executor, Chunk chunk, StructureAccessor structureAccessor, boolean distantHorizons) {
		ChunkOfColumns<EndColumn> columns = this.chunkColumnCache.get();
		try {
			this.profiler.run("Initial terrain column values", () -> {
				columns.setPosAndPopulate(chunk.getPos().getStartX(), chunk.getPos().getStartZ(), column -> {
					column.getMountainCenterY();
					column.getMountainThickness();
					column.getLowerRingCloudNoise();
					column.getUpperRingCloudNoise();
					column.getLowerBridgeCloudNoise();
					column.getUpperBridgeCloudNoise();
				});
			});
			this.profiler.run("generateRawSections", () -> {
				this.generateRawSections(chunk, columns, distantHorizons);
			});
			this.profiler.run("heightmaps", () -> {
				this.setHeightmaps(chunk, (index, includeWater) -> {
					EndColumn column = columns.getColumn(index);
					return column.hasTerrain() ? column.getFinalTopHeightI() : column.settings.min_y();
				});
			});
		}
		finally {
			this.chunkColumnCache.reclaim(columns);
		}
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {

	}

	@Override
	public void prepareBiomeColumn(WorldColumn column) {
		column.hasTerrain();
	}

	@Override
	public Codec<? extends ChunkGenerator> getCodec() {
		return END_CODEC;
	}

	@Override
	public void populateEntities(ChunkRegion region) {

	}

	@Override
	public int getWorldHeight() {
		return this.settings.max_y() - this.settings.min_y();
	}

	@Override
	public int getSeaLevel() {
		return 0;
	}

	@Override
	public int getMinimumY() {
		return this.settings.min_y();
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		EndColumn column = this.column(x, z);
		return column.hasTerrain() ? column.getFinalTopHeightI() : this.getMinimumY();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		BlockState[] states = new BlockState[this.getWorldHeight()];
		Arrays.fill(states, BlockStates.AIR);
		int minY = this.settings.min_y();
		EndColumn column = this.column(x, z);
		if (column.hasTerrain()) {
			int start = column.getFinalBottomHeightI();
			int end = column.getFinalTopHeightI();
			for (int y = start; y < end; y++) {
				states[y - minY] = BlockStates.END_STONE;
			}
		}
		return new VerticalBlockSample(minY, states);
	}
}