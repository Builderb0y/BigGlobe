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
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.EndSpikeFeature;
import net.minecraft.world.gen.feature.EndSpikeFeature.Spike;
import net.minecraft.world.gen.feature.EndSpikeFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

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
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.features.BigGlobeConfiguredFeatureTagKeys;
import builderb0y.bigglobe.features.BigGlobeFeatures;
import builderb0y.bigglobe.features.OverrideFeature;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ScriptStructureOverrider;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.overriders.end.EndFoliageOverrider;
import builderb0y.bigglobe.overriders.end.EndHeightOverrider;
import builderb0y.bigglobe.overriders.end.EndVolumetricOverrider;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.settings.BiomeLayout;
import builderb0y.bigglobe.settings.BiomeLayout.PrimarySurface;
import builderb0y.bigglobe.settings.BiomeLayout.SecondarySurface;
import builderb0y.bigglobe.settings.EndSettings;
import builderb0y.bigglobe.settings.EndSettings.BridgeCloudSettings;
import builderb0y.bigglobe.settings.EndSettings.RingCloudSettings;
import builderb0y.bigglobe.versions.RegistryVersions;

@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeEndChunkGenerator extends BigGlobeChunkGenerator {

	public static final AutoCoder<BigGlobeEndChunkGenerator> END_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(BigGlobeEndChunkGenerator.class);
	public static final Codec<BigGlobeEndChunkGenerator> END_CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(END_CODER);

	@EncodeInline
	public final EndSettings settings;

	public final transient EndHeightOverrider.Holder[] heightOverriders;
	public final transient EndFoliageOverrider.Holder[] foliageOverriders;
	public final transient EndVolumetricOverrider.Holder[] lowerRingCloudOverriders, upperRingCloudOverriders, lowerBridgeCloudOverriders, upperBridgeCloudOverriders;
	public final transient ScriptStructureOverrider.Holder[] structureOverriders;

	public final transient SortedFeatureTag
		bridgeCloudLowerCeilingDecorators,
		bridgeCloudLowerFloorDecorators,
		bridgeCloudUpperCeilingDecorators,
		bridgeCloudUpperFloorDecorators,
		mountainCeilingDecorators,
		mountainFloorDecorators,
		nestCeilingDecorators,
		nestFloorDecorators,
		ringCloudLowerCeilingDecorators,
		ringCloudLowerFloorDecorators,
		ringCloudUpperCeilingDecorators,
		ringCloudUpperFloorDecorators;

	public BigGlobeEndChunkGenerator(
		#if MC_VERSION == MC_1_19_2
			BetterRegistry<StructureSet> structureSetRegistry,
		#endif
		EndSettings settings,
		SortedFeatures configuredFeatures,
		SortedStructures sortedStructures
	) {
		super(
			#if MC_VERSION == MC_1_19_2
				structureSetRegistry,
			#endif
			new ColumnBiomeSource(
				settings
				.biomes
				.registry
				.streamEntries()
				.map(RegistryEntry::value)
				.map(BiomeLayout::biome)
				.filter(Objects::nonNull)
			),
			configuredFeatures,
			sortedStructures
		);
		this.settings                          = settings;
		this.heightOverriders                  = OverrideFeature.collect(configuredFeatures, BigGlobeFeatures.            END_HEIGHT_OVERRIDER);
		this.foliageOverriders                 = OverrideFeature.collect(configuredFeatures, BigGlobeFeatures.           END_FOLIAGE_OVERRIDER);
		this.lowerRingCloudOverriders          = OverrideFeature.collect(configuredFeatures, BigGlobeFeatures.  END_LOWER_RING_CLOUD_OVERRIDER);
		this.upperRingCloudOverriders          = OverrideFeature.collect(configuredFeatures, BigGlobeFeatures.  END_UPPER_RING_CLOUD_OVERRIDER);
		this.lowerBridgeCloudOverriders        = OverrideFeature.collect(configuredFeatures, BigGlobeFeatures.END_LOWER_BRIDGE_CLOUD_OVERRIDER);
		this.upperBridgeCloudOverriders        = OverrideFeature.collect(configuredFeatures, BigGlobeFeatures.END_UPPER_BRIDGE_CLOUD_OVERRIDER);
		this.structureOverriders               = OverrideFeature.collect(configuredFeatures, BigGlobeFeatures.         END_STRUCTURE_OVERRIDER);

		this.bridgeCloudLowerCeilingDecorators = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_BRIDGE_CLOUD_LOWER_CEILING);
		this.bridgeCloudLowerFloorDecorators   = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_BRIDGE_CLOUD_LOWER_FLOOR);
		this.bridgeCloudUpperCeilingDecorators = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_BRIDGE_CLOUD_UPPER_CEILING);
		this.bridgeCloudUpperFloorDecorators   = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_BRIDGE_CLOUD_UPPER_FLOOR);
		this.mountainCeilingDecorators         = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_MOUNTAIN_CEILING);
		this.mountainFloorDecorators           = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_MOUNTAIN_FLOOR);
		this.nestCeilingDecorators             = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_NEST_CEILING);
		this.nestFloorDecorators               = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_NEST_FLOOR);
		this.ringCloudLowerCeilingDecorators   = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_RING_CLOUD_LOWER_CEILING);
		this.ringCloudLowerFloorDecorators     = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_RING_CLOUD_LOWER_FLOOR);
		this.ringCloudUpperCeilingDecorators   = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_RING_CLOUD_UPPER_CEILING);
		this.ringCloudUpperFloorDecorators     = this.getFeatures(BigGlobeConfiguredFeatureTagKeys.END_RING_CLOUD_UPPER_FLOOR);
	}

	public SortedFeatureTag getFeatures(TagKey<ConfiguredFeature<?, ?>> key) {
		return new SortedFeatureTag(this.configuredFeatures.registry.getOrCreateTag(key));
	}

	public static void init() {
		Registry.register(RegistryVersions.chunkGenerator(), BigGlobeMod.modID("end"), END_CODEC);
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
			this.populateHeight(column, structures);
			column.getFoliage();
			for (EndFoliageOverrider.Holder overrider : this.foliageOverriders) {
				overrider.override(structures, column);
			}
			column.getLowerRingCloudNoise();
			column.getUpperRingCloudNoise();
			column.getLowerBridgeCloudNoise();
			column.getUpperBridgeCloudNoise();
			for (EndVolumetricOverrider.Holder overrider : this.lowerRingCloudOverriders) {
				overrider.override(EndVolumetricOverrider.lowerRingCloudContext(structures, column));
			}
			for (EndVolumetricOverrider.Holder overrider : this.upperRingCloudOverriders) {
				overrider.override(EndVolumetricOverrider.upperRingCloudContext(structures, column));
			}
			for (EndVolumetricOverrider.Holder overrider : this.lowerBridgeCloudOverriders) {
				overrider.override(EndVolumetricOverrider.lowerBridgeCloudContext(structures, column));
			}
			for (EndVolumetricOverrider.Holder overrider : this.upperBridgeCloudOverriders) {
				overrider.override(EndVolumetricOverrider.upperBridgeCloudContext(structures, column));
			}
			column.updateLevels();
		});
	}

	public void populateHeight(EndColumn column, ScriptStructures structures) {
		column.getMountainCenterY();
		column.getMountainThickness();
		for (EndHeightOverrider.Holder overrider : this.heightOverriders) {
			overrider.override(structures, column);
		}
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
			if (column.getDistanceToOrigin() < column.settings.nest.max_radius()) {
				minSurface = Math.min(lowerStart, column.settings.nest.min_y());
				maxSurface = Math.max(upperEnd,   column.settings.nest.max_y());
			}
		}
		if (maxSurface > minSurface) { //will also verify that the chunk has terrain in it somewhere.
			this.generateSectionsParallelSimple(chunk, minSurface, maxSurface, columns, context -> {
				int startY = context.startY();
				int solidCount = 0;
				for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
					EndColumn column = null; //(EndColumn)(context.columns.getColumn(horizontalIndex));
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
					for (int yIndex = 0; yIndex < 16; yIndex++) {
						int y = startY | yIndex;
						if (
							(yIndex >= mountainMinY && yIndex < mountainMaxY)
							|| (lowerRingNoise   != null && y >= lowerRingStartY   && y < lowerRingStartY   + lowerRingNoise  .length() && lowerRingNoise  .getF(y - lowerRingStartY  ) > 0.0F)
							|| (upperRingNoise   != null && y >= upperRingStartY   && y < upperRingStartY   + upperRingNoise  .length() && upperRingNoise  .getF(y - upperRingStartY  ) > 0.0F)
							|| (lowerBridgeNoise != null && y >= lowerBridgeStartY && y < lowerBridgeStartY + lowerBridgeNoise.length() && lowerBridgeNoise.getF(y - lowerBridgeStartY) > 0.0F)
							|| (upperBridgeNoise != null && y >= upperBridgeStartY && y < upperBridgeStartY + upperBridgeNoise.length() && upperBridgeNoise.getF(y - upperBridgeStartY) > 0.0F)
							|| (nestNoise        != null && y >= nestStartY        && y < nestStartY        + nestNoise       .length() && nestNoise       .getF(y - nestStartY       ) > 0.0F)
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

			PrimarySurface primarySurface = null; //this.settings.biomes.getPrimarySurface(column, currentHeight, this.seed);
			SecondarySurface[] secondarySurfaces = null; //this.settings.biomes.getSecondarySurfaces(column, currentHeight, this.seed);

			int depth = 0;
			done: {
				int primaryDepth = BigGlobeMath.floorI(this.settings.mountains.primary_surface_depth().evaluate(column, currentHeight, derivativeMagnitudeSquared, permuter));
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
				int height = column.settings.min_y;
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
			this.generateRawStructures(chunk, structureAccessor, columns);
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
			ScriptStructures structures = this.preGenerateFeatureColumns(world, chunk.getPos(), structureAccessor, distantHorizons);
			ChunkOfColumns<EndColumn> columns = this.getChunkOfColumns(chunk, structures, distantHorizons).asType(EndColumn.class);

			if (!(distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipStructures)) {
				this.profiler.run("Structures", () -> {
					this.generateStructures(world, chunk, structureAccessor);
				});
			}

			this.profiler.run("Feature placement", () -> {
				BlockPos.Mutable    mutablePos          = new BlockPos.Mutable();
				Permuter            permuter            = new Permuter(0L);
				MojangPermuter      mojang              = permuter.mojang();
				RingCloudSettings   ringCloudSettings   = this.settings.ring_clouds;
				BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
				for (int columnIndex = 0; columnIndex < 256; columnIndex++) {
					EndColumn column = columns.getColumn(columnIndex);
					mutablePos.setX(column.x).setZ(column.z);
					permuter.setSeed(Permuter.permute(this.seed ^ 0x9C4110F7CC26D977L, column.x, column.z));
					this.runDecorators(world, mutablePos, mojang, this.nestFloorDecorators, column.nestFloorLevels);
					this.runDecorators(world, mutablePos, mojang, this.nestCeilingDecorators, column.nestCeilingLevels);
					if (column.hasTerrain()) {
						this.runDecorators(world, mutablePos, mojang, this.mountainFloorDecorators, column.getFinalTopHeightI());
						this.runDecorators(world, mutablePos, mojang, this.mountainCeilingDecorators, column.getFinalBottomHeightI() - 1);
					}
					if (ringCloudSettings != null) {
						this.runDecorators(world, mutablePos, mojang, this.ringCloudLowerFloorDecorators,   column.lowerRingCloudFloorLevels);
						this.runDecorators(world, mutablePos, mojang, this.ringCloudLowerCeilingDecorators, column.lowerRingCloudCeilingLevels);
						this.runDecorators(world, mutablePos, mojang, this.ringCloudUpperFloorDecorators,   column.upperRingCloudFloorLevels);
						this.runDecorators(world, mutablePos, mojang, this.ringCloudUpperCeilingDecorators, column.upperRingCloudCeilingLevels);
					}
					if (bridgeCloudSettings != null) {
						this.runDecorators(world, mutablePos, mojang, this.bridgeCloudLowerFloorDecorators,   column.lowerBridgeCloudFloorLevels);
						this.runDecorators(world, mutablePos, mojang, this.bridgeCloudLowerCeilingDecorators, column.lowerBridgeCloudCeilingLevels);
						this.runDecorators(world, mutablePos, mojang, this.bridgeCloudUpperFloorDecorators,   column.upperBridgeCloudFloorLevels);
						this.runDecorators(world, mutablePos, mojang, this.bridgeCloudUpperCeilingDecorators, column.upperBridgeCloudCeilingLevels);
					}
				}
				if (columns.getColumn(8, 8).getDistanceToOrigin() < 64.0D) {
					List<Spike> spikes = EndSpikeFeature.getSpikes(world).stream().filter(spike -> spike.getCenterX() >> 4 == chunk.getPos().x && spike.getCenterZ() >> 4 == chunk.getPos().z).collect(Collectors.toList());
					if (!spikes.isEmpty()) {
						BigGlobeFeatures.END_SPIKE_RESPAWN.generate(new FeatureContext<>(Optional.empty(), world, this, new MojangPermuter(0L) /* ignored */, BlockPos.ORIGIN /* also ignored */, new EndSpikeFeatureConfig(false, spikes, null)));
					}
				}
			});
		});
	}

	@Override
	public boolean canStructureSpawn(RegistryEntry<Structure> entry, StructureStart start, Permuter permuter, boolean distantHorizons) {
		StructureStartWrapper wrapper = StructureStartWrapper.of(entry, start);
		EndColumn column = this.column(0, 0);
		for (ScriptStructureOverrider.Holder overrider : this.structureOverriders) {
			if (!overrider.override(wrapper, column, permuter, distantHorizons)) {
				return false;
			}
		}
		return true;
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
		return this.settings.max_y - this.settings.min_y;
	}

	@Override
	public int getSeaLevel() {
		return 0;
	}

	@Override
	public int getMinimumY() {
		return this.settings.min_y;
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
		int minY = this.settings.min_y;
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