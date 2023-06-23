package builderb0y.bigglobe.chunkgen;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.feature.EndSpikeFeature;
import net.minecraft.world.gen.feature.EndSpikeFeature.Spike;
import net.minecraft.world.gen.feature.EndSpikeFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.noise.NoiseConfig;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.AbstractChunkOfColumns;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.EndColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.features.BigGlobeFeatures;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.settings.BiomeLayout;
import builderb0y.bigglobe.settings.BiomeLayout.PrimarySurface;
import builderb0y.bigglobe.settings.BiomeLayout.SecondarySurface;
import builderb0y.bigglobe.settings.EndSettings;
import builderb0y.bigglobe.settings.EndSettings.BridgeCloudSettings;
import builderb0y.bigglobe.settings.EndSettings.EndMountainSettings;
import builderb0y.bigglobe.settings.EndSettings.RingCloudSettings;
import builderb0y.bigglobe.structures.RawGenerationStructure;

@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeEndChunkGenerator extends BigGlobeChunkGenerator {

	public static final AutoCoder<BigGlobeEndChunkGenerator> END_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(BigGlobeEndChunkGenerator.class);
	public static final Codec<BigGlobeEndChunkGenerator> END_CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(END_CODER);

	@EncodeInline
	public final EndSettings settings;

	public BigGlobeEndChunkGenerator(EndSettings settings, SortedFeatures configuredFeatures) {
		super(
			new ColumnBiomeSource(
				settings
				.biomes()
				.registry
				.streamEntries()
				.map(RegistryEntry::value)
				.map(BiomeLayout::biome)
				.filter(Objects::nonNull)
			),
			configuredFeatures
		);
		this.settings = settings;
	}

	public static void init() {
		Registry.register(Registries.CHUNK_GENERATOR, BigGlobeMod.modID("end"), END_CODEC);
	}

	public static AutoCoder<BigGlobeEndChunkGenerator> createCoder(FactoryContext<BigGlobeEndChunkGenerator> context) {
		return BigGlobeChunkGenerator.createCoder(context, "bigglobe", "the_end");
	}

	@Override
	public EndColumn column(int x, int z) {
		return new EndColumn(this.settings, this.seed, x, z);
	}

	@Override
	public void populateChunkOfColumns(AbstractChunkOfColumns<? extends WorldColumn> columns, ChunkPos chunkPos, ScriptStructures structures, boolean distantHorizons) {
		columns.asType(EndColumn.class).setPosAndPopulate(chunkPos.getStartX(), chunkPos.getStartZ(), column -> {
			column.getNestNoise();
			column.getMountainCenterY();
			column.getMountainThickness();
			column.getLowerRingCloudNoise();
			column.getUpperRingCloudNoise();
			column.getLowerBridgeCloudNoise();
			column.getUpperBridgeCloudNoise();
			column.getFoliage();
			column.updateLevels();
		});
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
			if (column.getDistanceToOrigin() < column.settings.nest().max_radius()) {
				minSurface = Math.min(lowerStart, column.settings.nest().min_y());
				maxSurface = Math.max(upperEnd,   column.settings.nest().max_y());
			}
		}
		if (maxSurface > minSurface) { //will also verify that the chunk has terrain in it somewhere.
			this.generateSectionsParallelSimple(chunk, minSurface, maxSurface, columns, context -> {
				int startY = context.startY();
				int solidCount = 0;
				for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
					EndColumn column = (EndColumn)(context.columns.getColumn(horizontalIndex));
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
					double[] nestNoise = column.getNestNoise();
					double[] lowerRingNoise = column.getLowerRingCloudNoise();
					double[] upperRingNoise = column.getUpperRingCloudNoise();
					double[] lowerBridgeNoise = column.getLowerBridgeCloudNoise();
					double[] upperBridgeNoise = column.getUpperBridgeCloudNoise();
					int nestStartY = column.settings.nest().min_y();
					int lowerRingStartY = column.getLowerRingCloudSampleStartY();
					int upperRingStartY = column.getUpperRingCloudSampleStartY();
					int lowerBridgeStartY = column.getLowerBridgeCloudSampleStartY();
					int upperBridgeStartY = column.getUpperBridgeCloudSampleStartY();
					for (int yIndex = 0; yIndex < 16; yIndex++) {
						int y = startY | yIndex;
						if (
							(yIndex >= mountainMinY && yIndex < mountainMaxY)
							|| (lowerRingNoise   != null && y >= lowerRingStartY   && y < lowerRingStartY   + lowerRingNoise  .length && lowerRingNoise  [y - lowerRingStartY  ] > 0.0D)
							|| (upperRingNoise   != null && y >= upperRingStartY   && y < upperRingStartY   + upperRingNoise  .length && upperRingNoise  [y - upperRingStartY  ] > 0.0D)
							|| (lowerBridgeNoise != null && y >= lowerBridgeStartY && y < lowerBridgeStartY + lowerBridgeNoise.length && lowerBridgeNoise[y - lowerBridgeStartY] > 0.0D)
							|| (upperBridgeNoise != null && y >= upperBridgeStartY && y < upperBridgeStartY + upperBridgeNoise.length && upperBridgeNoise[y - upperBridgeStartY] > 0.0D)
							|| (nestNoise        != null && y >= nestStartY        && y < nestStartY        + nestNoise       .length && nestNoise       [y - nestStartY       ] > 0.0D)
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

	public void generateSurface(Chunk chunk, ChunkOfColumns<EndColumn> columns) {
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		Permuter permuter = new Permuter(0L);
		for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
			EndColumn column = columns.getColumn(horizontalIndex);
			double currentHeight = column.getFinalTopHeightD();
			mutablePos.set(column.x, BigGlobeMath.ceilI(currentHeight), column.z);
			permuter.setSeed(Permuter.permute(this.seed ^ 0xDA5BA4067BDFEE53L, column.x, column.z));

			double derivativeMagnitudeSquared = BigGlobeMath.squareD(
				columns.getColumn(horizontalIndex ^  1).getFinalTopHeightD() - currentHeight,
				columns.getColumn(horizontalIndex ^ 16).getFinalTopHeightD() - currentHeight
			);

			PrimarySurface primarySurface = this.settings.biomes().getPrimarySurface(column, currentHeight, this.seed);
			SecondarySurface[] secondarySurfaces = this.settings.biomes().getSecondarySurfaces(column, currentHeight, this.seed);

			int depth = 0;
			done: {
				int primaryDepth = BigGlobeMath.floorI(this.settings.mountains().primary_surface_depth().evaluate(column, currentHeight, derivativeMagnitudeSquared, permuter));
				for (; depth < primaryDepth; depth++) {
					mutablePos.setY(mutablePos.getY() - 1);
					if (chunk.getBlockState(mutablePos).isOpaque()) {
						chunk.setBlockState(mutablePos, depth == 0 ? primarySurface.top() : primarySurface.under(), false);
					}
					else {
						break done;
					}
				}
				if (secondarySurfaces != null) {
					for (SecondarySurface surface : secondarySurfaces) {
						int secondaryDepth = BigGlobeMath.floorI(surface.depth().evaluate(column, currentHeight, derivativeMagnitudeSquared, permuter));
						for (; depth < secondaryDepth; depth++) {
							mutablePos.setY(mutablePos.getY() - 1);
							if (chunk.getBlockState(mutablePos).isOpaque()) {
								chunk.setBlockState(mutablePos, surface.under(), false);
							}
							else {
								break done;
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void generateRawTerrain(Executor executor, Chunk chunk, StructureAccessor structureAccessor, boolean distantHorizons) {
		ScriptStructures structures = ScriptStructures.getStructures(structureAccessor, chunk.getPos(), distantHorizons);
		ChunkOfColumns<EndColumn> columns = this.getChunkOfColumns(chunk, structures, distantHorizons).asType(EndColumn.class);
		this.profiler.run("generateRawSections", () -> {
			this.generateRawSections(chunk, columns, distantHorizons);
		});
		this.profiler.run("heightmaps", () -> {
			this.setHeightmaps(chunk, (index, includeWater) -> {
				EndColumn column = columns.getColumn(index);
				int height = column.settings.min_y();
				if (column.hasTerrain()) height = column.getFinalTopHeightI();
				height = Math.max(height, last(column.nestFloorLevels));
				height = Math.max(height, last(column.lowerRingCloudFloorLevels));
				height = Math.max(height, last(column.upperRingCloudFloorLevels));
				height = Math.max(height, last(column.lowerBridgeCloudFloorLevels));
				height = Math.max(height, last(column.upperBridgeCloudFloorLevels));
				return height;
			});
		});
		this.profiler.run("Raw structure generation", () -> {
			RawGenerationStructure.generateAll(structures, this.seed, chunk, columns, distantHorizons);
		});
		this.profiler.run("Surface", () -> {
			this.generateSurface(chunk, columns);
		});
	}

	public static int last(IntList list) {
		return list != null && !list.isEmpty() ? list.getInt(list.size() - 1) : Integer.MIN_VALUE;
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
		if (WORLD_SLICES && (chunk.getPos().x & 3) != 0) return;

		this.profiler.run("Features", () -> {
			boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
			if (!(distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipStructures)) {
				this.profiler.run("Structures", () -> {
					for (GenerationStep.Feature step : FEATURE_STEPS) {
						this.generateStructuresInStage(world, chunk, structureAccessor, step);
					}
				});
			}
			ScriptStructures structures = ScriptStructures.getStructures(structureAccessor, chunk.getPos(), distantHorizons);
			ChunkOfColumns<EndColumn> columns = this.getChunkOfColumns(chunk, structures, distantHorizons).asType(EndColumn.class);
			this.profiler.run("Feature placement", () -> {
				BlockPos.Mutable mutablePos = new BlockPos.Mutable();
				Permuter permuter = new Permuter(0L);
				MojangPermuter mojang = permuter.mojang();
				EndMountainSettings mountainSettings    = this.settings.mountains();
				RingCloudSettings   ringCloudSettings   = this.settings.ring_clouds();
				BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
				for (int columnIndex = 0; columnIndex < 256; columnIndex++) {
					EndColumn column = columns.getColumn(columnIndex);
					mutablePos.setX(column.x).setZ(column.z);
					permuter.setSeed(Permuter.permute(this.seed ^ 0x9C4110F7CC26D977L, column.x, column.z));
					if (column.hasTerrain()) {
						this.runDecorators(world, mutablePos, mojang, mountainSettings.floor_decorator(), column.getFinalTopHeightI());
						this.runDecorators(world, mutablePos, mojang, mountainSettings.ceiling_decorator(), column.getFinalBottomHeightI() - 1);
					}
					if (ringCloudSettings != null) {
						this.runDecorators(world, mutablePos, mojang, ringCloudSettings.lower_floor_decorator(),   column.lowerRingCloudFloorLevels);
						this.runDecorators(world, mutablePos, mojang, ringCloudSettings.lower_ceiling_decorator(), column.lowerRingCloudCeilingLevels);
						this.runDecorators(world, mutablePos, mojang, ringCloudSettings.lower_floor_decorator(),   column.upperRingCloudFloorLevels);
						this.runDecorators(world, mutablePos, mojang, ringCloudSettings.lower_ceiling_decorator(), column.upperRingCloudCeilingLevels);
					}
					if (bridgeCloudSettings != null) {
						this.runDecorators(world, mutablePos, mojang, bridgeCloudSettings.lower_floor_decorator(),   column.lowerBridgeCloudFloorLevels);
						this.runDecorators(world, mutablePos, mojang, bridgeCloudSettings.lower_ceiling_decorator(), column.lowerBridgeCloudCeilingLevels);
						this.runDecorators(world, mutablePos, mojang, bridgeCloudSettings.lower_floor_decorator(),   column.upperBridgeCloudFloorLevels);
						this.runDecorators(world, mutablePos, mojang, bridgeCloudSettings.lower_ceiling_decorator(), column.upperBridgeCloudCeilingLevels);
					}
				}
				if (columns.getColumn(8, 8).getDistanceToOrigin() < 64.0D) {
					List<Spike> spikes = EndSpikeFeature.getSpikes(world).stream().filter(spike -> spike.getCenterX() >> 4 == chunk.getPos().x && spike.getCenterZ() >> 4 == chunk.getPos().z).collect(Collectors.toList());
					if (!spikes.isEmpty()) {
						BigGlobeFeatures.END_SPIKE.generate(new FeatureContext<>(Optional.empty(), world, this, new MojangPermuter(0L) /* ignored */, BlockPos.ORIGIN /* also ignored */, new EndSpikeFeatureConfig(false, spikes, null)));
					}
				}
			});
		});
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