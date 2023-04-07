package builderb0y.bigglobe.chunkgen;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.SnowyBlock;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet.WeightedEntry;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.mixinInterfaces.PositionCache.OverworldPositionCache;
import builderb0y.bigglobe.mixinInterfaces.PositionCache.PositionCacheHolder;
import builderb0y.bigglobe.chunkgen.perSection.*;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.*;
import builderb0y.bigglobe.columns.ColumnZone.RestrictedColumnZone;
import builderb0y.bigglobe.columns.OverworldColumn.CaveCell;
import builderb0y.bigglobe.columns.OverworldColumn.CavernCell;
import builderb0y.bigglobe.columns.OverworldColumn.SkylandCell;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.features.BigGlobeFeatures;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.features.flowers.FlowerEntryFeature;
import builderb0y.bigglobe.features.flowers.LinkedFlowerConfig;
import builderb0y.bigglobe.features.ores.OverworldOreFeature;
import builderb0y.bigglobe.features.rockLayers.LinkedRockLayerConfig;
import builderb0y.bigglobe.features.rockLayers.OverworldRockLayerEntryFeature;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.mixins.SingularPalette_EntryAccess;
import builderb0y.bigglobe.mixins.StructureStart_BoundingBoxSetter;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ScriptStructureOverrider;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.overriders.overworld.OverworldCaveOverrider;
import builderb0y.bigglobe.overriders.overworld.OverworldCavernOverrider;
import builderb0y.bigglobe.overriders.overworld.OverworldFoliageOverrider;
import builderb0y.bigglobe.overriders.overworld.OverworldHeightOverrider;
import builderb0y.bigglobe.randomLists.RestrictedList;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.registry.BetterRegistryEntry;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.settings.OverworldCavernSettings;
import builderb0y.bigglobe.settings.OverworldSettings;
import builderb0y.bigglobe.settings.OverworldSettings.OverworldSurfaceSettings.OverworldSurfaceBlocks;
import builderb0y.bigglobe.settings.OverworldSkylandSettings.SkylandSurfaceSettings;
import builderb0y.bigglobe.settings.OverworldUndergroundSettings;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.structures.LakeStructure;
import builderb0y.bigglobe.structures.LakeStructure.Piece.Data;
import builderb0y.bigglobe.structures.RawGenerationStructure;
import builderb0y.bigglobe.structures.megaTree.MegaTreeStructure;
import builderb0y.bigglobe.util.SemiThreadLocal;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.util.WorldUtil;

@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeOverworldChunkGenerator extends BigGlobeChunkGenerator {

	public static final int BEDROCK_HEIGHT = 16;

	public static final AutoCoder<BigGlobeOverworldChunkGenerator> OVERWORLD_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(BigGlobeOverworldChunkGenerator.class);
	public static final Codec<BigGlobeOverworldChunkGenerator> OVERWORLD_CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(OVERWORLD_CODER);

	public transient SemiThreadLocal<ChunkOfColumns<OverworldColumn>> chunkColumnCache;

	@EncodeInline
	public final OverworldSettings settings;

	public final transient ColumnValue<OverworldColumn>[] biomeValues;
	public final transient OverworldOreFeature.Config[] oreConfigs;
	public final transient LinkedFlowerConfig[] flowerGroups;
	public final transient LinkedRockLayerConfig<OverworldRockLayerEntryFeature.Entry>[] rockLayers;
	public final transient OverworldHeightOverrider.Holder[] heightOverriders;
	public final transient OverworldFoliageOverrider.Holder[] foliageOverriders;
	public final transient OverworldCaveOverrider.Holder[] caveOverriders;
	public final transient OverworldCavernOverrider.Holder[] cavernOverriders;
	public final transient ScriptStructureOverrider.Holder[] structureOverriders;

	public BigGlobeOverworldChunkGenerator(
		OverworldSettings settings,
		SortedFeatures configuredFeatures
	) {
		super(
			new ColumnBiomeSource(
				settings
				.surface()
				.biomes()
				.streamZones()
				.map(ColumnZone::value)
			),
			configuredFeatures
		);
		this.settings = settings;
		this.biomeValues = (
			settings
			.surface()
			.biomes()
			.streamZones()
			.filter(RestrictedColumnZone.class::isInstance)
			.flatMap((Object zone) -> ((RestrictedColumnZone<?>)(zone)).restriction.getValues())
			.filter(value -> !value.dependsOnY() && OverworldColumn.class.isAssignableFrom(value.columnClass))
			.distinct()
			.toArray(ColumnValue.ARRAY_FACTORY.generic())
		);
		this.rockLayers = LinkedRockLayerConfig.OVERWORLD_FACTORY.link(configuredFeatures);
		this.flowerGroups = LinkedFlowerConfig.FACTORY.link(configuredFeatures);
		this.         oreConfigs = configuredFeatures.streamConfigs(BigGlobeFeatures.OVERWORLD_ORE).toArray(OverworldOreFeature.Config[]::new);
		this.   heightOverriders = filterFeatures(configuredFeatures, BigGlobeFeatures.   OVERWORLD_HEIGHT_OVERRIDER, config -> config.script,  OverworldHeightOverrider.Holder[]::new);
		this.  foliageOverriders = filterFeatures(configuredFeatures, BigGlobeFeatures.  OVERWORLD_FOLIAGE_OVERRIDER, config -> config.script, OverworldFoliageOverrider.Holder[]::new);
		this.     caveOverriders = filterFeatures(configuredFeatures, BigGlobeFeatures.     OVERWORLD_CAVE_OVERRIDER, config -> config.script,    OverworldCaveOverrider.Holder[]::new);
		this.   cavernOverriders = filterFeatures(configuredFeatures, BigGlobeFeatures.   OVERWORLD_CAVERN_OVERRIDER, config -> config.script,  OverworldCavernOverrider.Holder[]::new);
		this.structureOverriders = filterFeatures(configuredFeatures, BigGlobeFeatures.OVERWORLD_STRUCTURE_OVERRIDER, config -> config.script,  ScriptStructureOverrider.Holder[]::new);
	}

	public static <C extends FeatureConfig, H extends ScriptHolder<?>> H[] filterFeatures(
		SortedFeatures sortedFeatures,
		Feature<C> feature,
		Function<C, H> getter,
		IntFunction<H[]> arrayFactory
	) {
		return sortedFeatures.streamConfigs(feature).map(getter).toArray(arrayFactory);
	}

	@Override
	public void setSeed(long seed) {
		super.setSeed(seed);
		this.chunkColumnCache = SemiThreadLocal.weak(4, () -> (
			new ChunkOfColumns<>(OverworldColumn[]::new, this::column)
		));
	}

	public static void init() {
		Registry.register(Registries.CHUNK_GENERATOR, BigGlobeMod.modID("overworld"), OVERWORLD_CODEC);
	}

	public static AutoCoder<BigGlobeOverworldChunkGenerator> createCoder(FactoryContext<BigGlobeOverworldChunkGenerator> context) {
		return BigGlobeChunkGenerator.createCoder(context, "bigglobe", "overworld");
	}

	@Override
	public OverworldColumn column(int x, int z) {
		return new OverworldColumn(this.settings, this.seed, x, z);
	}

	public void generateRawSectionsAndCaves(Chunk chunk, ChunkOfColumns<OverworldColumn> columns, ScriptStructures structures, boolean distantHorizons) {
		int seaLevel = this.getSeaLevel();
		int minSurface = Integer.MAX_VALUE;
		int maxSurface = Integer.MIN_VALUE;
		int minSkyland = Integer.MAX_VALUE;
		int maxSkyland = Integer.MIN_VALUE;
		int minCavernY = Integer.MAX_VALUE;
		int maxCavernY = Integer.MIN_VALUE;
		for (OverworldColumn column : columns.columns) {
			int height = column.getFinalTopHeightI();
			minSurface = Math.min(minSurface, height);
			maxSurface = Math.max(maxSurface, height);
			if (column.hasSkyland()) {
				minSkyland = Math.min(minSkyland, BigGlobeMath.floorI(column.getSkylandMinY()));
				maxSkyland = Math.max(maxSkyland, BigGlobeMath.floorI(column.getSkylandMaxY()));
			}
			double cavernThickness = column.getCavernThickness();
			if (cavernThickness > 0.0D) {
				double cavernCenter = column.getCavernCenter();
				if (!Double.isNaN(cavernCenter)) {
					minCavernY = Math.min(minCavernY, BigGlobeMath.floorI(cavernCenter - cavernThickness));
					maxCavernY = Math.max(maxCavernY, BigGlobeMath.floorI(cavernCenter + cavernThickness));
				}
			}
		}
		int minSurface_ = minSurface;
		int maxSurface_ = maxSurface;
		int minSkyland_ = minSkyland;
		int maxSkyland_ = maxSkyland;
		int minCavernY_ = minCavernY;
		int maxCavernY_ = maxCavernY;
		int maxHeight = Math.max(seaLevel, maxSurface);
		int minHeight = (
			distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipUnderground
			? minSurface - 1
			: this.getMinimumY()
		);
		this.generateSectionsParallelSimple(chunk, minHeight, maxHeight, columns, (SectionGenerationContext context) -> {
			int startY = context.startY();
			int centerY = startY | 8;
			OverworldColumn initialColumn = columns.getColumn(8, 8);
			BlockState initialState;
			if (centerY < initialColumn.getFinalTopHeightI()) initialState = BlockStates.STONE;
			else if (centerY < seaLevel) initialState = BlockStates.WATER;
			else initialState = BlockStates.AIR;

			if (initialState != BlockStates.AIR) {
				if (context.palette() instanceof SingularPalette_EntryAccess singular) {
					//how to set 4096 blocks in one operation.
					singular.bigglobe_setEntry(initialState);
				}
				else {
					//this shouldn't happen, but we should handle it sanely anyway.
					int stoneID = context.id(initialState);
					PaletteStorage storage = context.storage();
					long payload = stoneID;
					for (int bits = storage.getElementBits(); bits < 64; bits <<= 1) {
						payload |= payload << bits;
					}
					Arrays.fill(storage.getData(), payload);
				}
			}

			PaletteStorage storage = context.storage();
			int airID        = context.id(BlockStates.AIR);
			int waterID      = context.id(BlockStates.WATER);
			int stoneID      = context.id(BlockStates.STONE);
			if (storage != (storage = context.storage())) { //resize.
				airID        = context.id(BlockStates.AIR);
				waterID      = context.id(BlockStates.WATER);
				stoneID      = context.id(BlockStates.STONE);
				assert storage == context.storage();
			}

			PaletteStorage storage_ = storage;
			int airID_        = airID;
			int stoneID_      = stoneID;
			int waterID_      = waterID;
			this.profiler.run("Place raw terrain blocks", () -> {
				OverworldUndergroundSettings undergroundSettings = this.settings.underground();
				boolean skipCaves = distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.areCavesSkipped();
				int endY = startY | 15;
				if (initialState != BlockStates.STONE && startY <= maxSurface_) {
					for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
						OverworldColumn column = columns.getColumn(horizontalIndex);
						applyCorrection(
							horizontalIndex,
							startY,
							Math.min(column.getFinalTopHeightI() - 1, endY),
							storage_,
							stoneID_
						);
					}
				}
				if (initialState != BlockStates.WATER && endY >= minSurface_ && startY < seaLevel) {
					for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
						OverworldColumn column = columns.getColumn(horizontalIndex);
						applyCorrection(
							horizontalIndex,
							Math.max(column.getFinalTopHeightI(), startY),
							Math.min(seaLevel - 1, endY),
							storage_,
							waterID_
						);
					}
				}
				if (initialState != BlockStates.AIR && endY >= Math.max(minSurface_, seaLevel + 1)) {
					for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
						OverworldColumn column = columns.getColumn(horizontalIndex);
						applyCorrection(
							horizontalIndex,
							Math.max(Math.max(column.getFinalTopHeightI(), seaLevel), startY),
							endY,
							storage_,
							airID_
						);
					}
				}
				if (!skipCaves) {
					if (undergroundSettings.hasCaves() && endY >= minSurface_ - undergroundSettings.caves().maxDepth() && startY <= maxSurface_) {
						for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
							OverworldColumn column = columns.getColumn(horizontalIndex);
							int surfaceY = column.getFinalTopHeightI();
							int minCaves = Math.max(surfaceY - undergroundSettings.caves().maxDepth(), startY);
							int maxCaves = Math.min(surfaceY - 1, endY);
							assert maxCaves - minCaves < 16;
							for (int y = minCaves; y <= maxCaves; y++) {
								if (column.isCaveAt(y, true)) {
									storage_.set(horizontalIndex | ((y & 15) << 8), airID_);
								}
							}
						}
					}
					if (undergroundSettings.hasCaverns() && endY >= minCavernY_ && startY <= maxCavernY_) {
						for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
							OverworldColumn column = columns.getColumn(horizontalIndex);
							double cavernThickness = column.getCavernThickness();
							if (cavernThickness > 0.0D) {
								double cavernCenter = column.getCavernCenter();
								if (!Double.isNaN(cavernCenter)) {
									int minCavern = Math.max(BigGlobeMath.floorI(cavernCenter - cavernThickness), startY);
									int maxCavern = Math.min(BigGlobeMath.floorI(cavernCenter + cavernThickness), endY);
									assert maxCavern - minCavern < 16;
									for (int y = minCavern; y <= maxCavern; y++) {
										storage_.set(horizontalIndex | ((y & 15) << 8), airID_);
									}
								}
							}
						}
					}
				}
			});
			if (!distantHorizons) {
				this.profiler.run("Cave surface", () -> {
					CaveSurfaceReplacer.generate(context);
				});
				this.profiler.run("Cobblestone", () -> {
					CobblestoneReplacer.generate(context, this.settings.underground().cobble_per_section());
				});
			}
		});
		if (maxSkyland >= minSkyland) {
			this.generateSectionsParallelSimple(chunk, minSkyland, maxSkyland + 1, columns, (SectionGenerationContext context) -> {
				int floatstoneID = context.id(BlockStates.FLOATSTONE);
				PaletteStorage storage = context.storage();
				int startY = context.startY();
				int endY = startY | 15;
				for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
					OverworldColumn column = columns.getColumn(horizontalIndex);
					double minSkylands = column.getSkylandMinY();
					if (!Double.isNaN(minSkylands)) {
						double maxSkylands = column.getSkylandMaxY();
						if (maxSkylands > minSkylands) {
							int minSkylandsI = Math.max(BigGlobeMath.floorI(minSkylands), startY);
							int maxSkylandsI = Math.min(BigGlobeMath.floorI(maxSkylands), endY);
							assert maxSkylandsI - minSkylandsI < 16;
							for (int y = minSkylandsI; y <= maxSkylandsI; y++) {
								storage.set(horizontalIndex | ((y & 15) << 8), floatstoneID);
							}
						}
					}
				}
			});
		}
		if (!distantHorizons) {
			this.generateRockLayers(this.rockLayers, chunk, minHeight, maxSurface, columns, true);
			this.profiler.run("ores", () -> {
				this.generateSectionsParallelSimple(chunk, minHeight, maxHeight, columns, context -> {
					OreReplacer.generate(context, columns, this.oreConfigs);
				});
			});
			this.generateRockLayers(this.rockLayers, chunk, minHeight, maxSurface, columns, false);
			ChunkSection bottomSection = chunk.getSection(chunk.getSectionIndex(this.getMinimumY()));
			BedrockReplacer.generateBottom(new SectionGenerationContext(chunk, bottomSection, this.seed, columns));
		}
		this.profiler.run("Recalculate counts", () -> {
			this.generateSectionsParallelSimple(chunk, minHeight, maxHeight, columns, SectionGenerationContext::recalculateCounts);
			if (maxSkyland_ >= minSkyland_) {
				this.generateSectionsParallelSimple(chunk, minSkyland_, maxSkyland_ + 1, columns, SectionGenerationContext::recalculateCounts);
			}
		});
	}

	public static void applyCorrection(int horizontalIndex, int minY, int maxY, PaletteStorage storage, int id) {
		assert maxY - minY < 16;
		for (int y = minY; y <= maxY; y++) {
			storage.set(horizontalIndex | ((y & 15) << 8), id);
		}
	}

	public void updatePostRawGenerationHeightmaps(Chunk chunk, ChunkOfColumns<OverworldColumn> columns) {
		this.setHeightmaps(chunk, (int index, boolean includeWater) -> {
			OverworldColumn column = columns.getColumn(index);
			int height = column.getFinalTopHeightI();
			if (column.hasSkyland()) height = Math.max(height, BigGlobeMath.ceilI(column.getSkylandMaxY()));
			if (height < 0 && includeWater) {
				return 0;
			}
			else {
				while (column.isCaveAt(height - 1, false)) height--;
				return height;
			}
		});
	}

	public void generateCavernFluids(Chunk chunk, ChunkOfColumns<OverworldColumn> columns) {
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		for (int index = 0; index < 256; index++) {
			OverworldColumn column = columns.getColumn(index);
			CavernCell cavernCell = column.getCavernCell();
			BlockState fluid;
			if (cavernCell == null || (fluid = cavernCell.settings.fluid()) == null || fluid.isAir()) {
				continue;
			}

			double center = column.getCavernCenter();
			double thickness = column.getCavernThickness();
			double averageCenter = column.getCavernAverageCenter();
			if (!Double.isNaN(center) && thickness > 0.0D && !Double.isNaN(averageCenter)) {
				int minY = BigGlobeMath.floorI(center - thickness);
				int maxY = BigGlobeMath.floorI(Math.min(averageCenter, center + thickness));
				mutablePos.setX(column.x).setZ(column.z);
				for (int y = minY; y <= maxY; y++) {
					chunk.setBlockState(mutablePos.setY(y), fluid, false);
				}
			}
		}
	}

	public void generateSurface(Chunk chunk, ChunkOfColumns<OverworldColumn> columns, ScriptStructures structures) {
		ChunkPos chunkPos = chunk.getPos();
		int startX = chunkPos.getStartX();
		int startZ = chunkPos.getStartZ();
		ColumnZone<OverworldSurfaceBlocks> blockPalette = this.settings.surface().blocks();
		double surfaceDepthFalloff = 1.0D / this.settings.miscellaneous().surface_depth_falloff();
		double seaLevel = this.getSeaLevel();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		Permuter permuter = new Permuter(0L);
		for (int index = 0; index < 256; index++) {
			OverworldColumn column = columns.getColumn(index);
			permuter.setSeed(Permuter.permute(this.seed ^ 0x7EF4E9F5C88A2506L, column.x, column.z));
			double currentHeight = column.getFinalTopHeightD();
			//for even x values, compute derivative in x direction by adding 1 to x.
			//for odd x values, subtract 1 instead.
			//same for z, but adding or subtracting 1 from z
			//is the same as adding or subtracting 16 from x.
			//both of these operations can be achieved very efficiently with xor.
			//this will give alternating signs for adjacent blocks,
			//but that doesn't matter since it'll be squared anyway.
			double derivativeMagnitudeSquared = BigGlobeMath.squareD(
				columns.getColumn(index ^  1).getFinalTopHeightD() - currentHeight,
				columns.getColumn(index ^ 16).getFinalTopHeightD() - currentHeight
			);
			int x = startX + (index &  15);
			int z = startZ + (index >>> 4);
			int surfaceDepth = BigGlobeMath.floorI(
				5.0D
				+ Permuter.nextUniformDouble(permuter)
					* 4.0D / (derivativeMagnitudeSquared + 2.0D)
				- derivativeMagnitudeSquared * 3.0D
				- (currentHeight - seaLevel) * surfaceDepthFalloff
				+ column.getFoliage() * 2.0D
			);
			double rawSubsurfaceDepth = (
				5.0D
				+ Permuter.nextUniformDouble(permuter)
					* 2.0D
				//- (uncompressedHeight - continentHeight) * (1.0D / 64.0D)
			);
			pos.set(x, BigGlobeMath.ceilI(currentHeight), z);
			BlockState top, under, subsurface;
			int subsurfaceDepth;
			double lakeChance = column.getInLake();
			if (Permuter.toChancedBoolean(Permuter.permute(this.seed ^ 0x99D9DE3ED6D24D11L, x, z), lakeChance)) {
				Data data = structures.lake.data;
				top = data.top_state();
				under = data.under_state();
				subsurface = data.subsurface_state();
				subsurfaceDepth = BigGlobeMath.floorI(rawSubsurfaceDepth * lakeChance);
			}
			else {
				OverworldSurfaceBlocks palette = blockPalette.get(column, column.getFinalTopHeightD());
				top = palette.top();
				under = palette.under();
				subsurface = this.settings.miscellaneous().subsurface_state();
				subsurfaceDepth = BigGlobeMath.floorI(rawSubsurfaceDepth * column.getBeachChance());
			}

			int depth = 0;
			boolean isTop = true;
			for (; depth < surfaceDepth; depth++) {
				pos.setY(pos.getY() - 1);
				if (chunk.getBlockState(pos).isOpaque()) {
					chunk.setBlockState(pos, isTop ? top : under, false);
					isTop = false;
				}
				else {
					isTop = true;
				}
			}
			for (; depth < subsurfaceDepth; depth++) {
				pos.setY(pos.getY() - 1);
				if (chunk.getBlockState(pos).isOpaque()) {
					chunk.setBlockState(pos, subsurface, false);
				}
			}

			//skyland
			if (column.hasSkyland()) {
				SkylandSurfaceSettings surfaceSettings = column.getSkylandCell().settings.surface();
				top = surfaceSettings.top_state();
				under = surfaceSettings.under_state();
				double maxY = column.getSkylandMaxY();
				surfaceDepth = BigGlobeMath.floorI(surfaceSettings.depth().evaluate(column, maxY, permuter));
				pos.setY(BigGlobeMath.ceilI(maxY));
				for (depth = 0; depth < surfaceDepth; depth++) {
					pos.setY(pos.getY() - 1);
					if (chunk.getBlockState(pos).isOpaque()) {
						chunk.setBlockState(pos, depth == 0 ? top : under, false);
					}
					else {
						break;
					}
				}
			}
		}
	}

	public void generateSnow(Chunk chunk, ChunkOfColumns<OverworldColumn> columns, boolean distantHorizons) {
		int startX = chunk.getPos().getStartX();
		int startZ = chunk.getPos().getStartZ();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		boolean skipCaveFilling = distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.areCavesSkipped();
		for (int index = 0; index < 256; index++) {
			OverworldColumn column = columns.getColumn(index);
			int x = startX + (index &  15);
			int z = startZ + (index >>> 4);
			double snowMaxY = column.getSnowHeight();
			double snowDiff = snowMaxY - column.getFinalTopHeightD();
			if (snowDiff < 0.0D) {
				continue;
				//snowDiff -= BigGlobeMath.squareF(snowDiff);
				//snowMaxY = snowDiff + column.getFinalTopHeightF();
			}
			int topY = column.getFinalTopHeightI();
			int snowLayers = BigGlobeMath.floorI(
				(snowMaxY - topY)
				* SnowBlock.MAX_LAYERS
			);
			pos.set(x, topY, z);
			boolean success = false;
			done: {
				if (!skipCaveFilling) {
					//fill in surface caves.
					while (true) {
						pos.setY(--topY);
						if (!chunk.isOutOfHeightLimit(pos) && chunk.getBlockState(pos).isAir()) {
							snowLayers += SnowBlock.MAX_LAYERS;
						}
						else {
							pos.setY(++topY);
							break;
						}
					}
				}
				BlockState snowState = BlockStates.SNOW.with(SnowBlock.LAYERS, SnowBlock.MAX_LAYERS);
				for (; snowLayers >= SnowBlock.MAX_LAYERS; snowLayers -= SnowBlock.MAX_LAYERS) {
					if (chunk.getBlockState(pos).isAir()) {
						chunk.setBlockState(pos, snowState, false);
						success = true;
					}
					else {
						break done;
					}
					pos.setY(pos.getY() + 1);
				}
				if (snowLayers > 0 && chunk.getBlockState(pos).isAir()) {
					chunk.setBlockState(pos, snowState.with(SnowBlock.LAYERS, snowLayers), false);
					success = true;
				}
			}
			if (success) {
				pos.setY(topY - 1);
				BlockState oldState = chunk.getBlockState(pos);
				if (oldState.contains(SnowyBlock.SNOWY)) {
					chunk.setBlockState(pos, oldState.with(SnowyBlock.SNOWY, Boolean.TRUE), false);
				}
			}
		}
	}

	public void generateIce(Chunk chunk, ChunkOfColumns<OverworldColumn> columns, ScriptStructures structures) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		Permuter permuter = new Permuter(0L);
		for (int index = 0; index < 256; index++) {
			OverworldColumn column = columns.getColumn(index);
			double rawIceDepth = column.computeIceDepth();
			if (rawIceDepth > 0.0D) {
				permuter.setSeed(Permuter.permute(this.seed ^ 0x4F05BB8A0D652F6FL, column.x, column.z));
				int iceDepth = (
					rawIceDepth > 1.0D
					? Permuter.roundRandomlyI(
						permuter,
						permuter.nextDouble(rawIceDepth - 1.0D)
					) + 1
					: Permuter.roundRandomlyI(
						permuter,
						rawIceDepth
					)
				);
				if (iceDepth > 0) {
					this.fillIce(chunk, pos, column, column.settings.height().sea_level(), iceDepth);
					if (structures.lake != null && structures.lake.isInsideCircle(column.x, column.z)) {
						this.fillIce(chunk, pos, column, structures.lake.data.waterSurface(), iceDepth);
					}
				}
			}
		}
	}

	public void fillIce(Chunk chunk, BlockPos.Mutable pos, OverworldColumn column, int maxY, int iceDepth) {
		pos.set(column.x, maxY, column.z);
		int minY = column.getFinalTopHeightI();
		for (int depth = 0; depth < iceDepth; depth++) {
			pos.setY(pos.getY() - 1);
			if (pos.getY() < minY) break;
			if (chunk.getBlockState(pos) == BlockStates.WATER) {
				chunk.setBlockState(pos, BlockStates.ICE, false);
			}
			else {
				break;
			}
		}
	}

	public void runHeightOverrides(OverworldColumn column, ScriptStructures structures, boolean rawTerrain) {
		LakeStructure.Piece lakePiece = structures.lake;
		if (lakePiece != null) {
			double distance = Math.sqrt(
				BigGlobeMath.squareD(
					column.x - lakePiece.data.x(),
					column.z - lakePiece.data.z()
				)
			);
			double radius = lakePiece.data.horizontalRadius();
			double y = lakePiece.data.y();
			if (distance < radius + 16.0D && column.finalHeight < y) {
				double mixLevel = Interpolator.unmixSmooth(radius + 16.0D, radius, distance);
				double newHeight = Interpolator.mixLinear(column.finalHeight, y, mixLevel);
				column.snowHeight += newHeight - column.finalHeight;
				column.finalHeight = newHeight;
			}
			if (distance < radius) {
				double dip = lakePiece.getDip(column.x, column.z, distance);
				column.finalHeight += dip;
				column.snowHeight += dip;
			}
		}
		column.populateInLake(lakePiece);
		for (OverworldHeightOverrider.Holder overrider : this.heightOverriders) {
			overrider.override(structures, column, rawTerrain);
		}
	}

	public void runFoliageOverrides(OverworldColumn column, ScriptStructures structures, boolean rawTerrain) {
		column.foliage *= 1.0D - column.getInLake();
		for (OverworldFoliageOverrider.Holder overrider : this.foliageOverriders) {
			overrider.override(structures, column, rawTerrain);
		}
	}

	public void runCaveOverrides(OverworldColumn column, ScriptStructures structures, boolean rawTerrain) {
		if (this.settings.underground().hasCaves()) {
			OverworldCaveOverrider.Context context = (
				new OverworldCaveOverrider.Context(structures, column, rawTerrain)
			);
			//lower cutoff
			{
				double minY = Math.max(context.bottomD, context.column.settings.height().minYAboveBedrock());
				CaveCell cell = context.column.getCaveCell();
				int topY = Math.min(BigGlobeMath.floorI(minY + cell.settings.lower_width()), context.topI - 1);
				double rcpLowerWidth = 1.0D / cell.settings.lower_width();
				for (int y = topY; y >= context.bottomI; y--) {
					double above = (y - minY) * rcpLowerWidth;
					context.excludeUnchecked(y, BigGlobeMath.squareD(1.0D - above));
				}
			}
			//edge
			{
				int distance = context.column.settings.underground().caves().placement().distance;
				double progress = context.caveCell.voronoiCell.progressToEdgeD(context.column.x, context.column.z);
				for (int y = context.bottomI; y < context.topI; y++) {
					double
						width     = context.caveSettings.getWidth(context.topD, y),
						threshold = 1.0D - width / (distance * 0.5D),
						fraction  = Interpolator.unmixLinear(threshold, 1.0D, progress);
					if (fraction > 0.0D) {
						context.excludeUnchecked(y, BigGlobeMath.squareD(fraction));
					}
				}
			}
			//scripts
			for (OverworldCaveOverrider.Holder overrider : this.caveOverriders) {
				overrider.override(context);
			}
			column.populateCaveFloorsAndCeilings();
		}
	}

	public void runCavernOverrides(OverworldColumn column, ScriptStructures structures, boolean rawTerrain) {
		if (this.settings.underground().hasCaverns()) {
			for (OverworldCavernOverrider.Holder overrider : this.cavernOverriders) {
				overrider.override(structures, column, rawTerrain);
			}
		}
	}

	@Override
	public void generateRawTerrain(Executor executor, Chunk chunk, StructureAccessor structureAccessor, boolean distantHorizons) {
		ChunkOfColumns<OverworldColumn> columns = this.chunkColumnCache.get();
		ScriptStructures structures = ScriptStructures.getStructures(structureAccessor, chunk.getPos(), distantHorizons);
		try {
			this.profiler.run("initial terrain column values", () -> {
				columns.setPosAndPopulate(chunk.getPos().getStartX(), chunk.getPos().getStartZ(), (OverworldColumn column) -> {
					column.getFinalTopHeightD();
					column.getSnowHeight();
					this.runHeightOverrides(column, structures, true);

					column.getTemperature();
					column.getFoliage();
					this.runFoliageOverrides(column, structures, true);

					if (!(distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.areCavesSkipped())) {
						column.getCaveCell();
						column.getCaveNoise();
						column.getCaveSurfaceDepth();
						this.runCaveOverrides(column, structures, true);

						column.getCavernCell();
						column.getCavernCenter();
						column.getCavernThicknessSquared();
						this.runCavernOverrides(column, structures, true);
					}

					column.getSkylandMinY();
					column.getSkylandMaxY();
				});
			});
			if (chunk instanceof PositionCacheHolder holder) {
				holder.bigglobe_setPositionCache(new OverworldPositionCache(columns));
			}
			this.profiler.run("generateRawSectionsAndCaves", () -> {
				this.generateRawSectionsAndCaves(chunk, columns, structures, distantHorizons);
			});
			this.profiler.run("Init heightmaps", () -> {
				this.updatePostRawGenerationHeightmaps(chunk, columns);
			});
			if (!(distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipUnderground)) {
				this.profiler.run("Cavern fluids", () -> {
					this.generateCavernFluids(chunk, columns);
				});
			}

			this.profiler.run("Raw structure generation", () -> {
				RawGenerationStructure.generateAll(structures, this.seed, chunk, columns, distantHorizons);
			});

			this.profiler.run("Surface", () -> {
				this.generateSurface(chunk, columns, structures);
				this.generateSnow(chunk, columns, distantHorizons);
				this.generateIce(chunk, columns, structures);
			});
		}
		finally {
			this.chunkColumnCache.reclaim(columns);
		}
	}

	//////////////////////////////// decoration ////////////////////////////////

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

			ChunkOfColumns<OverworldColumn> columns = this.chunkColumnCache.get();
			OverworldPositionCache cache = chunk instanceof PositionCacheHolder holder && holder.bigglobe_getPositionCache() instanceof OverworldPositionCache overworld ? overworld : null;
			ScriptStructures scriptStructures = ScriptStructures.getStructures(structureAccessor, chunk.getPos(), distantHorizons);
			this.profiler.run("Initial feature column values", () -> {
				columns.setPosAndPopulate(chunk.getPos().getStartX(), chunk.getPos().getStartZ(), column -> {
					column.getFinalTopHeightD();
					this.runHeightOverrides(column, scriptStructures, false);
					if (cache == null && !(distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.areCavesSkipped())) {
						column.getCaveCell();
						column.getCaveNoise();
						column.getCaveSurfaceDepth();
						this.runCaveOverrides(column, scriptStructures, false);
					}
					column.getCavernCell();
					column.getCavernCenter();
					column.getCavernThicknessSquared();
					this.runCavernOverrides(column, scriptStructures, false);

					column.getTemperature();
					column.getFoliage();
					this.runFoliageOverrides(column, scriptStructures, false);

					column.getSkylandMinY();
					column.getSkylandMaxY();
				});
			});
			//lambdas -_-
			OverworldPositionCache cache_ = cache != null ? cache : new OverworldPositionCache(columns);
			FeatureColumns.FEATURE_COLUMNS.set(columns::getColumnChecked);
			try {
				BlockPos.Mutable pos = new BlockPos.Mutable();
				Permuter permuter = new Permuter(0L);
				this.profiler.run("flowers", () -> {
					this.generateFlowers(world, columns, pos, permuter);
				});
				this.profiler.run("non-flower (custom) features", () -> {
					MojangPermuter mojang = permuter.mojang();
					for (int columnIndex = 0; columnIndex < 256; columnIndex++) {
						OverworldColumn column = columns.getColumn(columnIndex);
						pos.setX(column.x).setZ(column.z);
						permuter.setSeed(Permuter.permute(this.seed ^ 0xF9C81CB5E3A312C9L, column.x, column.z));

						if (!(distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.areCavesSkipped())) {
							CavernCell cavernCell = column.getCavernCell();
							if (cavernCell != null) {
								double center = column.getCavernCenter();
								double thickness = column.getCavernThickness();
								if (!Double.isNaN(center) && thickness > 0.0D) {
									this.runDecorators(world, pos, mojang, cavernCell.settings.floor_decorator(), BigGlobeMath.floorI(center - thickness));
									this.runDecorators(world, pos, mojang, cavernCell.settings.ceiling_decorator(), BigGlobeMath.floorI(center + thickness));
								}
							}
							CaveCell caveCell = column.getCaveCell();
							if (caveCell != null) {
								this.runDecorators(world, pos, mojang, caveCell.settings.floor_decorator(), cache_.getCaveFloors(columnIndex));
								this.runDecorators(world, pos, mojang, caveCell.settings.ceiling_decorator(), cache_.getCaveCeilings(columnIndex));
							}
						}
						this.runDecorators(world, pos, mojang, this.settings.surface().decorator(), column.getFinalTopHeightI());

						SkylandCell skylandCell = column.getSkylandCell();
						if (skylandCell != null && column.hasSkyland()) {
							this.runDecorators(world, pos, mojang, skylandCell.settings.floor_decorator(), BigGlobeMath.floorI(column.getSkylandMaxY()) + 1);
							this.runDecorators(world, pos, mojang, skylandCell.settings.ceiling_decorator(), BigGlobeMath.floorI(column.getSkylandMinY()) - 1);
						}
					}
				});
			}
			finally {
				FeatureColumns.FEATURE_COLUMNS.set(null);
				this.chunkColumnCache.reclaim(columns);
			}
		});
	}

	public void generateFlowers(StructureWorldAccess world, ChunkOfColumns<OverworldColumn> columns, BlockPos.Mutable pos, Permuter permuter) {
		if (this.flowerGroups.length == 0) return;
		FlowerEntryFeature.Entry[] entries = (
			IntStream
			.range(0, 256)
			.parallel()
			.mapToObj(columns::getColumn)
			.map(this::getFlowerEntry)
			.toArray(FlowerEntryFeature.Entry[]::new)
		);
		long seed = Permuter.permute(this.seed ^ 0x9A99AA4557D5FE0FL, columns.getColumn(0).x >> 4, columns.getColumn(0).z >> 4);
		for (int index = 0; index < 256; index++) {
			if (entries[index] != null) {
				permuter.setSeed(Permuter.permute(seed, index));
				OverworldColumn column = columns.getColumn(index);
				this.placeFlower(world, permuter, pos.set(column.x, column.getFinalTopHeightI(), column.z), entries[index]);
			}
		}
	}

	public FlowerEntryFeature.Entry getFlowerEntry(OverworldColumn column) {
		long worldSeed = this.seed;
		Grid2D flowerNoise = this.settings.flower_noise();

		FlowerEntryFeature.Entry chosen = null;
		long overlapSeed = Permuter.permute(worldSeed ^ 0x3C8F9545BAE6971FL, column.x, column.z);
		int overlapChance = 0;

		RestrictedList<FlowerEntryFeature.Entry> validEntries = new RestrictedList<>(null, column, column.getFinalTopHeightI());
		for (LinkedFlowerConfig group : this.flowerGroups) {
			validEntries.elements = group.entries;
			long groupSeed = Permuter.permute(worldSeed ^ 0x4BBA2A41585882E8L, group.name);
			int scale = group.group.scale;
			int variation = group.group.variation;
			int inGridX = BigGlobeMath.modulus_BP(column.x, scale);
			int inGridZ = BigGlobeMath.modulus_BP(column.z, scale);
			int gridStartX = column.x - inGridX;
			int gridStartZ = column.z - inGridZ;
			double noise = flowerNoise.getValue(groupSeed, column.x, column.z);
			for (int offsetX = -scale; offsetX <= scale; offsetX += scale) {
				for (int offsetZ = -scale; offsetZ <= scale; offsetZ += scale) {
					int otherGridStartX = gridStartX + offsetX;
					int otherGridStartZ = gridStartZ + offsetZ;
					long otherGridSeed = Permuter.permute(groupSeed ^ 0xA2BBF085229FA361L, otherGridStartX, otherGridStartZ);
					if (!Permuter.nextChancedBoolean(otherGridSeed += Permuter.PHI64, group.group.spawn_chance)) continue;
					FlowerEntryFeature.Entry entry;
					RandomSource radiusSource;
					if (Permuter.nextChancedBoolean(otherGridSeed += Permuter.PHI64, group.group.randomize_chance)) {
						entry = validEntries.getRandomElement(Permuter.permute(otherGridSeed += Permuter.PHI64, column.x, column.z));
						if (entry == null) continue;
						radiusSource = group.group.randomize_radius;
					}
					else {
						entry = validEntries.getRandomElement(Permuter.stafford(otherGridSeed += Permuter.PHI64));
						if (entry == null) continue;
						radiusSource = entry.radius;
					}
					double radius = radiusSource.get(Permuter.stafford(otherGridSeed += Permuter.PHI64));
					double otherGridCenterX = Permuter.nextPositiveDouble(otherGridSeed += Permuter.PHI64) * variation + offsetX;
					double otherGridCenterZ = Permuter.nextPositiveDouble(otherGridSeed += Permuter.PHI64) * variation + offsetZ;
					double distanceSquaredToCenter = BigGlobeMath.squareD(inGridX - otherGridCenterX, inGridZ - otherGridCenterZ);
					distanceSquaredToCenter /= BigGlobeMath.squareD(radius);
					double groupNoise = noise - distanceSquaredToCenter * flowerNoise.maxValue();
					if (Permuter.nextChancedBoolean(overlapSeed += Permuter.PHI64, groupNoise)) {
						if (overlapChance++ == 0 || Permuter.nextBoundedInt(overlapSeed += Permuter.PHI64, overlapChance) == 0) {
							chosen = entry;
						}
					}
				}
			}
		}
		return chosen;
	}

	public void placeFlower(StructureWorldAccess world, Permuter permuter, BlockPos.Mutable pos, FlowerEntryFeature.Entry patch) {
		pos = WorldUtil.findNonReplaceableGroundMutable(world, pos);
		if (pos == null) return;
		int groundY = pos.getY();
		int flowerY = groundY + 1;
		if (patch.under != null) {
			BlockState oldState = world.getBlockState(pos);
			if (SingleBlockFeature.place(world, pos.setY(groundY), permuter, patch.under)) {
				if (!SingleBlockFeature.place(world, pos.setY(flowerY), permuter, patch.state)) {
					world.setBlockState(pos.setY(groundY), oldState, Block.NOTIFY_ALL);
				}
			}
		}
		else {
			SingleBlockFeature.place(world, pos.setY(flowerY), permuter, patch.state);
		}
	}

	public class OverworldStructureFinder extends StructureFinder {

		public OverworldColumn ancientCityColumn;

		public OverworldStructureFinder(ServerWorld world, RegistryEntryList<Structure> structures, boolean skipReferencedStructures) {
			super(world, structures, skipReferencedStructures);
		}

		@Override
		public boolean canPossiblyGenerate(int chunkX, int chunkZ, RegistryEntry<Structure> structure) {
			if (UnregisteredObjectException.getKey(structure) == StructureKeys.ANCIENT_CITY) {
				OverworldCavernSettings cavernSettings = BigGlobeOverworldChunkGenerator.this.settings.underground().deep_caverns();
				if (cavernSettings != null) {
					VoronoiDiagram2D placement = cavernSettings.placement();
					int distance = placement.distance;
					int cellX = Math.floorDiv(chunkX << 4, distance);
					int cellZ = Math.floorDiv(chunkZ << 4, distance);
					int centerX = placement.getCenterX(cellX, cellZ);
					int centerZ = placement.getCenterZ(cellX, cellZ);
					if (centerX >> 4 == chunkX && centerZ >> 4 == chunkZ) {
						OverworldColumn column = this.ancientCityColumn;
						if (column == null) column = BigGlobeOverworldChunkGenerator.this.column(centerX, centerZ);
						if (column.getCavernCell().settings.has_ancient_cities()) {
							return true;
						}
					}
				}

			}
			return super.canPossiblyGenerate(chunkX, chunkZ, structure);
		}
	}

	@Override
	public StructureFinder structureFinder(ServerWorld world, RegistryEntryList<Structure> structures, boolean skipReferencedStructures) {
		return this.new OverworldStructureFinder(world, structures, skipReferencedStructures);
	}

	@Override
	public void actuallySetStructureStarts(
		DynamicRegistryManager registryManager,
		StructurePlacementCalculator placementCalculator,
		StructureAccessor structureAccessor,
		Chunk chunk,
		StructureTemplateManager structureTemplateManager
	) {
		super.actuallySetStructureStarts(registryManager, placementCalculator, structureAccessor, chunk, structureTemplateManager);
		if (!DistantHorizonsCompat.isOnDistantHorizonThread()) {
			OverworldCavernSettings cavernSettings = this.settings.underground().deep_caverns();
			if (cavernSettings != null) {
				VoronoiDiagram2D placement = cavernSettings.placement();
				int distance = placement.distance;
				int chunkX = chunk.getPos().x;
				int chunkZ = chunk.getPos().z;
				int cellX = Math.floorDiv(chunkX << 4, distance);
				int cellZ = Math.floorDiv(chunkZ << 4, distance);
				int centerX = placement.getCenterX(cellX, cellZ);
				int centerZ = placement.getCenterZ(cellX, cellZ);
				if (centerX >> 4 == chunkX && centerZ >> 4 == chunkZ) {
					OverworldColumn column = this.column(centerX, centerZ);
					if (column.getCavernCell().settings.has_ancient_cities()) {
						RegistryEntry<Structure> structure = registryManager.get(RegistryKeys.STRUCTURE).getEntry(StructureKeys.ANCIENT_CITY).orElse(null);
						if (structure != null) {
							this.forceSetStructureStart(
								new WeightedEntry(structure, 1),
								structureAccessor,
								registryManager,
								placementCalculator.getNoiseConfig(),
								structureTemplateManager,
								placementCalculator.getStructureSeed(),
								chunk
							);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean canStructureSpawn(RegistryEntry<Structure> entry, StructureStart start, Permuter permuter) {
		OverworldColumn column = this.column(0, 0);
		if (
			//given the size of mega trees, it is *overwhelmingly* likely
			//that they will have at least one invalid biome in their area,
			//particularly if they spawned in a high-foliage area.
			//cause higher foliage = bigger tree = even more
			//likely to have an invalid biome somewhere.
			!(start.getStructure() instanceof MegaTreeStructure) &&
			start.getStructure().getFeatureGenerationStep() == GenerationStep.Feature.SURFACE_STRUCTURES
		) {
			//stay inside your own biome!
			RegistryEntryList<Biome> validBiomes = start.getStructure().getValidBiomes();
			BlockBox box = start.getBoundingBox();
			for (int x = box.getMinX(); x <= box.getMaxX(); x += 4) {
				if (wrongBiome(validBiomes, column, x, box.getMinZ())) return false;
				if (wrongBiome(validBiomes, column, x, box.getMaxZ())) return false;
			}
			for (int z = box.getMinZ(); z <= box.getMaxZ(); z += 4) {
				if (wrongBiome(validBiomes, column, box.getMinX(), z)) return false;
				if (wrongBiome(validBiomes, column, box.getMaxX(), z)) return false;
			}
		}
		StructureStartWrapper wrapper = StructureStartWrapper.of(entry, start);
		for (ScriptStructureOverrider.Holder overrider : this.structureOverriders) {
			if (!overrider.override(wrapper, column, permuter)) {
				return false;
			}
		}
		//expand structure bounding boxes so that overriders
		//which depend on them being expanded work properly.
		((StructureStart_BoundingBoxSetter)(Object)(start)).bigglobe_setBoundingBox(
			start.getBoundingBox().expand(
				entry.value().getTerrainAdaptation() == StructureTerrainAdaptation.NONE
				? 16
				: 4
			)
		);
		return true;
	}

	public static OverworldColumn pos(OverworldColumn column, int x, int z) {
		column.setPos(x, z);
		return column;
	}

	public static boolean wrongBiome(RegistryEntryList<Biome> validBiomes, OverworldColumn column, int x, int z) {
		return !validBiomes.contains(pos(column, x, z).getSurfaceBiome().vanilla());
	}

	public static int y(OverworldColumn column, int x, int z) {
		return pos(column, x, z).getFinalTopHeightI();
	}

	////////////////////////////////////////////////////////////////

	@Override
	public Codec<? extends ChunkGenerator> getCodec() {
		return OVERWORLD_CODEC;
	}

	@Override
	public void populateEntities(ChunkRegion region) {
		//mostly copy-pasted from NoiseChunkGenerator.
		ChunkPos chunkPos = region.getCenterPos();
		OverworldColumn column = this.column(chunkPos.getOffsetX(8), chunkPos.getOffsetZ(8));
		RegistryEntry<Biome> registryEntry = column.getSurfaceBiome().vanilla();
		ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
		chunkRandom.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
		SpawnHelper.populateEntities(region, registryEntry, chunkPos, chunkRandom);
	}

	@Override
	public int getWorldHeight() {
		return this.settings.height().y_range();
	}

	@Override
	public int getSeaLevel() {
		return this.settings.height().sea_level();
	}

	@Override
	public int getMinimumY() {
		return this.settings.height().min_y();
	}

	@Override
	public CompletableFuture<Chunk> populateBiomes(Executor executor, NoiseConfig noiseConfig, Blender blender, StructureAccessor structureAccessor, Chunk chunk) {
		return CompletableFuture.supplyAsync(
			() -> this.profiler.get("populateBiomes", () -> {
				ChunkOfBiomeColumns<OverworldColumn> columns = new ChunkOfBiomeColumns<>(OverworldColumn[]::new, this::column);
				ColumnZone<BetterRegistryEntry<Biome>> surfaceZones = this.settings.surface().biomes();
				ColumnValue<OverworldColumn>[] biomeValues = this.biomeValues;
				this.profiler.run("initial biome column values", () -> {
					columns.setPosAndPopulate(chunk.getPos().getStartX(), chunk.getPos().getStartZ(), column -> {
						//populate the values that don't depend on Y in parallel.
						for (ColumnValue<OverworldColumn> value : biomeValues) {
							value.getValue(column, 0.0D);
						}
					});
				});
				HeightLimitView heightView = chunk.getHeightLimitView();
				IntStream.range(heightView.getBottomSectionCoord(), heightView.getTopSectionCoord()).parallel().forEach(sectionY -> {
					ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(sectionY));
					int startY = section.getYOffset();
					PalettedContainer<RegistryEntry<Biome>> container = (PalettedContainer<RegistryEntry<Biome>>)(section.getBiomeContainer());
					for (int paletteIndex = 0; paletteIndex < 64; paletteIndex++) {
						OverworldColumn column = columns.getColumn(paletteIndex & 15);
						double y = startY | (paletteIndex >>> 4 << 2);
						int newID = SectionUtil.id(container, surfaceZones.get(column, y).vanilla());
						SectionUtil.storage(container).set(paletteIndex, newID);
					}
				});
				return chunk;
			}),
			executor
		);
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		WorldColumn column = this.column(x, z);
		int height = column.getFinalTopHeightI();
		int seaLevel = this.getSeaLevel();
		if (height < seaLevel && heightmap.getBlockPredicate().test(BlockStates.WATER)) {
			height = seaLevel;
		}
		return height;
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		WorldColumn column = this.column(x, z);
		int height = column.getFinalTopHeightI();
		int minY = this.getMinimumY();
		BlockState[] states = new BlockState[Math.max(height, this.getSeaLevel()) - minY];
		fill(states, 0, height - minY, BlockStates.STONE);
		fill(states, height - minY, states.length, BlockStates.WATER);
		return new VerticalBlockSample(minY, states);
	}

	/** allows toIndex < fromIndex, unlike Arrays.fill(). */
	public static void fill(BlockState[] states, int fromIndex, int toIndex, BlockState state) {
		for (int index = fromIndex; index < toIndex; index++) {
			states[index] = state;
		}
	}
}