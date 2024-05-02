package builderb0y.bigglobe.chunkgen;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.*;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.*;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.StructurePosition;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeContext.RootDecodePath;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.decoders.RecordDecoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState.ColorScript;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.RootLayer;
import builderb0y.bigglobe.chunkgen.scripted.SegmentList.Segment;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.columns.scripted.*;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnRandomToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.features.dispatch.FeatureDispatchers;
import builderb0y.bigglobe.features.RockReplacerFeature.ConfiguredRockReplacerFeature;
import builderb0y.bigglobe.mixins.Heightmap_StorageAccess;
import builderb0y.bigglobe.mixins.StructureStart_BoundingBoxSetter;
import builderb0y.bigglobe.mixins.StructureStart_ChildrenGetter;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ColumnValueOverrider;
import builderb0y.bigglobe.overriders.Overrider;
import builderb0y.bigglobe.overriders.Overrider.SortedOverriders;
import builderb0y.bigglobe.structures.ScriptStructures;
import builderb0y.bigglobe.overriders.StructureOverrider;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.AutoOverride;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.structures.DelegatingStructure;
import builderb0y.bigglobe.structures.RawGenerationStructure;
import builderb0y.bigglobe.structures.RawGenerationStructure.RawGenerationStructurePiece;
import builderb0y.bigglobe.util.*;
import builderb0y.bigglobe.util.WorldOrChunk.ChunkDelegator;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.bigglobe.versions.RegistryVersions;

#if MC_VERSION > MC_1_19_2
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
#endif

@AddPseudoField("biome_source")
#if MC_VERSION <= MC_1_19_2
@AddPseudoField("structure_set_registry")
#endif
@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeScriptedChunkGenerator extends ChunkGenerator {

	#if MC_VERSION >= MC_1_20_5
		public static final MapCodec<BigGlobeScriptedChunkGenerator> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(BigGlobeScriptedChunkGenerator.class);
	#else
		public static final Codec<BigGlobeScriptedChunkGenerator> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(BigGlobeScriptedChunkGenerator.class).codec();
	#endif

	public final @VerifyNullable String reload_dimension;
	public final @EncodeInline ColumnEntryRegistry columnEntryRegistry;
	public static record Height(
		@VerifyDivisibleBy16 int min_y,
		@VerifyDivisibleBy16 @VerifySorted(greaterThan = "min_y") int max_y,
		@VerifyNullable Integer sea_level
	) {}
	public final Height height;
	public final RootLayer layer;
	public final FeatureDispatchers feature_dispatcher;
	public final RegistryEntryList<Overrider> overriders;
	public final ColumnRandomToBooleanScript.@VerifyNullable Holder spawn_point;
	public static record ColorOverrides(
		ColorScript.@VerifyNullable Holder grass,
		ColorScript.@VerifyNullable Holder foliage,
		ColorScript.@VerifyNullable Holder water
	) {}
	public final @VerifyNullable ColorOverrides colors;
	public static record NetherOverrides(
		boolean place_portal_at_high_y_level
	) {}
	public final @VerifyNullable NetherOverrides nether_overrides;
	public static record EndOverrides(
		Spawning spawning,
		InnerGateways inner_gateways,
		OuterGateways outer_gateways
	) {

		public static record Spawning(
			int @VerifySizeRange(min = 3, max = 3) [] location,
			boolean obsidian_platform
		) {}

		public static record InnerGateways(
			double radius,
			int height
		) {}

		public static record OuterGateways(
			double min_radius,
			double max_radius,
			double step,
			ColumnToBooleanScript.Holder condition
		) {}
	}
	public final @VerifyNullable EndOverrides end_overrides;

	public transient SortedOverriders actualOverriders;
	public final SortedStructures sortedStructures;
	public transient long columnSeed, worldSeed;
	public transient Pattern displayPattern;
	public transient DisplayEntry rootDebugDisplay;
	public final transient ThreadLocal<ScriptedColumn[]> chunkReuseColumns;

	public BigGlobeScriptedChunkGenerator(
		#if MC_VERSION == MC_1_19_2
			BetterRegistry<StructureSet> structure_set_registry,
		#endif
		@VerifyNullable String reload_dimension,
		Height height,
		RootLayer layer,
		FeatureDispatchers feature_dispatcher,
		BiomeSource biome_source,
		RegistryEntryList<Overrider> overriders,
		ColumnRandomToBooleanScript.@VerifyNullable Holder spawn_point,
		@VerifyNullable ColorOverrides colors,
		@VerifyNullable NetherOverrides nether_overrides,
		@VerifyNullable EndOverrides end_overrides,
		SortedStructures sortedStructures
	) {
		super(
			#if (MC_VERSION == MC_1_19_2)
				((BetterHardCodedRegistry<StructureSet>)(structure_set_registry)).registry,
				Optional.empty(),
			#endif
			biome_source
		);
		if (biome_source instanceof ScriptedColumnBiomeSource source) {
			source.generator = this;
		}
		this.columnEntryRegistry = ColumnEntryRegistry.Loading.get().getRegistry();
		this.reload_dimension = reload_dimension;
		this.height = height;
		this.layer = layer;
		this.feature_dispatcher = feature_dispatcher;
		this.overriders = overriders;
		this.spawn_point = spawn_point;
		this.colors = colors;
		this.nether_overrides = nether_overrides;
		this.end_overrides = end_overrides;
		this.sortedStructures = sortedStructures;

		ScriptedColumn.Factory factory = this.columnEntryRegistry.columnFactory;
		this.chunkReuseColumns = ThreadLocal.withInitial(() -> {
			ScriptedColumn[] columns = new ScriptedColumn[256];
			for (int index = 0; index < 256; index++) {
				columns[index] = factory.create(new Params(0L, 0, 0, 0, 0, Purpose.GENERIC));
			}
			return columns;
		});

		this.rootDebugDisplay = new DisplayEntry(this);
	}

	public BiomeSource biome_source() {
		return this.biomeSource;
	}

	#if MC_VERSION <= MC_1_19_2
		public BetterRegistry<StructureSet> structure_set_registry() {
			return new BetterHardCodedRegistry<>(this.structureSetRegistry);
		}
	#endif

	public static void init() {
		Registry.register(RegistryVersions.chunkGenerator(), BigGlobeMod.modID("scripted"), CODEC);
		Registry.register(RegistryVersions.biomeSource(), BigGlobeMod.modID("scripted"), ScriptedColumnBiomeSource.CODEC);
	}

	public static AutoCoder<BigGlobeScriptedChunkGenerator> createCoder(FactoryContext<BigGlobeScriptedChunkGenerator> context) {
		AutoCoder<BigGlobeScriptedChunkGenerator> coder = (AutoCoder<BigGlobeScriptedChunkGenerator>)(context.forceCreateDecoder(RecordDecoder.Factory.INSTANCE));
		if (!BigGlobeConfig.INSTANCE.get().reloadGenerators) return coder;
		return new NamedCoder<BigGlobeScriptedChunkGenerator>("jar-reloading AutoCoder for BigGlobeScriptedChunkGenerator") {

			@Override
			public <T_Encoded> @Nullable BigGlobeScriptedChunkGenerator decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
				String dimension = context.getMember("reload_dimension").tryAsString();
				if (dimension != null) {
					JsonElement json = this.getDimension(dimension);
					if (json != null) {
						T_Encoded encoded = JsonOps.INSTANCE.convertTo(context.ops, json);
						return context.input(encoded, RootDecodePath.INSTANCE).decodeWith(coder);
					}
				}
				return context.decodeWith(coder);
			}

			@Override
			public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, BigGlobeScriptedChunkGenerator> context) throws EncodeException {
				return context.encodeWith(coder);
			}

			public JsonElement getDimension(String dimension) {
				BigGlobeMod.LOGGER.info("Reading " + dimension + " chunk generator from mod jar.");
				JsonElement element = this.getJson("/data/bigglobe/worldgen/world_preset/bigglobe.json");
				for (String key : new String[] { "dimensions", dimension, "generator", "value" }) {
					if (element instanceof JsonObject object) element = object.get(key);
					else return null;
				}
				return element;
			}

			public JsonElement getJson(String path) {
				try (
					Reader reader = new InputStreamReader(
						Objects.requireNonNull(
							BigGlobeMod.class.getResourceAsStream(path),
							path
						),
						StandardCharsets.UTF_8
					)
				) {
					return JsonParser.parseReader(reader);
				}
				catch (Exception exception) {
					throw AutoCodecUtil.rethrow(exception);
				}
			}

			@Override
			public @Nullable Stream<String> getKeys() {
				return coder.getKeys();
			}
		};
	}

	@Wrapper
	public static class SortedStructures {

		public final BetterRegistry<Structure> registry;
		public final RegistryEntry<Structure>[] sortedStructures;

		@SuppressWarnings("unchecked")
		public SortedStructures(BetterRegistry<Structure> registry) {
			this.registry = registry;
			this.sortedStructures = (
				registry
					.streamEntries()
					.sorted(
						Comparator.comparing(
								(RegistryEntry<Structure> entry) -> entry.value().getFeatureGenerationStep()
							)
							.thenComparing(UnregisteredObjectException::getID)
					)
					.toArray(RegistryEntry[]::new)
			);
		}
	}

	public ScriptedColumn newColumn(HeightLimitView world, int x, int z, Purpose purpose) {
		return this.columnEntryRegistry.columnFactory.create(
			new ScriptedColumn.Params(
				this.columnSeed,
				x,
				z,
				world,
				purpose
			)
		);
	}

	public void setSeed(long columnSeed) {
		this.worldSeed = columnSeed;
		//make it impossible to reverse-engineer the seed from information sent to the client.
		this.columnSeed = Hashing.sha256().hashLong(columnSeed).asLong();
	}

	#if MC_VERSION > MC_1_19_2
		@Override
		public StructurePlacementCalculator createStructurePlacementCalculator(RegistryWrapper<StructureSet> structureSetRegistry, NoiseConfig noiseConfig, long seed) {
			this.setSeed(seed);
			return super.createStructurePlacementCalculator(structureSetRegistry, noiseConfig, seed);
		}
	#else
		@Override
		public void computeStructurePlacementsIfNeeded(NoiseConfig noiseConfig) {
			this.setSeed(noiseConfig.getLegacyWorldSeed());
			super.computeStructurePlacementsIfNeeded(noiseConfig);
		}
	#endif

	@Override
	public #if MC_VERSION >= MC_1_20_5 MapCodec #else Codec #endif<? extends ChunkGenerator> getCodec() {
		return CODEC;
	}

	public SortedOverriders getOverriders() {
		if (this.actualOverriders == null) {
			this.actualOverriders = new SortedOverriders(this.overriders, this.columnEntryRegistry);
		}
		return this.actualOverriders;
	}

	@Override
	public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, Carver carverStep) {

	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {

	}

	@Override
	public void populateEntities(ChunkRegion region) {

	}

	@Override
	public int getWorldHeight() {
		return this.height.max_y - this.height.min_y;
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(
		Executor executor,
		Blender blender,
		NoiseConfig noiseConfig,
		StructureAccessor structureAccessor,
		Chunk chunk
	) {
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		ScriptStructures structures = ScriptStructures.getStructures(structureAccessor, chunk.getPos(), distantHorizons);
		ScriptedColumn.Params params = new ScriptedColumn.Params(this.columnSeed, 0, 0, chunk.getBottomY(), chunk.getTopY(), Purpose.rawGeneration(distantHorizons));
		return CompletableFuture.runAsync(
			() -> {
				int startX = chunk.getPos().getStartX();
				int startZ = chunk.getPos().getStartZ();
				int chunkMinY = chunk.getBottomY();
				int chunkMaxY = chunk.getTopY();
				ScriptedColumn[] columns = this.chunkReuseColumns.get();
				BlockSegmentList[] lists = new BlockSegmentList[256];
				try (AsyncRunner async = BigGlobeThreadPool.runner(distantHorizons)) {
					for (int offsetZ = 0; offsetZ < 16; offsetZ += 2) {
						final int offsetZ_ = offsetZ;
						for (int offsetX = 0; offsetX < 16; offsetX += 2) {
							final int offsetX_ = offsetX;
							async.submit(() -> {
								int baseIndex = (offsetZ_ << 4) | offsetX_;
								int quadX = startX | offsetX_;
								int quadZ = startZ | offsetZ_;
								ScriptedColumn
									column00 = columns[baseIndex     ],
									column01 = columns[baseIndex ^  1],
									column10 = columns[baseIndex ^ 16],
									column11 = columns[baseIndex ^ 17];
								column00.setParamsUnchecked(params.at(quadX,     quadZ    ));
								column01.setParamsUnchecked(params.at(quadX | 1, quadZ    ));
								column10.setParamsUnchecked(params.at(quadX,     quadZ | 1));
								column11.setParamsUnchecked(params.at(quadX | 1, quadZ | 1));
								for (String name : this.getOverriders().rawColumnValueDependencies) try {
									column00.preComputeColumnValue(name);
									column01.preComputeColumnValue(name);
									column10.preComputeColumnValue(name);
									column11.preComputeColumnValue(name);
								}
								catch (Throwable throwable) {
									BigGlobeMod.LOGGER.error("Exception pre-computing overrider column value: ", throwable);
								}
								for (ColumnValueOverrider.Holder overrider : this.getOverriders().rawColumnValues) {
									overrider.override(column00, structures);
									overrider.override(column01, structures);
									overrider.override(column10, structures);
									overrider.override(column11, structures);
								}
								BlockSegmentList
									list00 = new BlockSegmentList(chunkMinY, chunkMaxY),
									list01 = new BlockSegmentList(chunkMinY, chunkMaxY),
									list10 = new BlockSegmentList(chunkMinY, chunkMaxY),
									list11 = new BlockSegmentList(chunkMinY, chunkMaxY);
								this.layer.emitSegments(column00, column01, column10, column11, list00);
								this.layer.emitSegments(column01, column00, column11, column10, list01);
								this.layer.emitSegments(column10, column11, column00, column01, list10);
								this.layer.emitSegments(column11, column10, column01, column00, list11);
								lists  [baseIndex     ] = list00;
								lists  [baseIndex ^  1] = list01;
								lists  [baseIndex ^ 16] = list10;
								lists  [baseIndex ^ 17] = list11;
							});
						}
					}
				}
				int minFilledSectionY = Integer.MAX_VALUE;
				int maxFilledSectionY = Integer.MIN_VALUE;
				for (BlockSegmentList list : lists) {
					int size = list.size();
					for (int index = 0; index < size; index++) {
						Segment<BlockState> segment = list.get(index);
						if (!segment.value.isAir()) {
							minFilledSectionY = Math.min(minFilledSectionY, segment.minY);
							break;
						}
					}
					for (int index = size; --index >= 0;) {
						Segment<BlockState> segment = list.get(index);
						if (!segment.value.isAir()) {
							maxFilledSectionY = Math.max(maxFilledSectionY, segment.maxY);
							break;
						}
					}
				}
				minFilledSectionY >>= 4;
				maxFilledSectionY = (maxFilledSectionY >> 4) + 1;
				Async.loop(BigGlobeThreadPool.executor(distantHorizons), minFilledSectionY, maxFilledSectionY, 1, (int coord) -> {
					ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(coord));
					int baseY = coord << 4;
					SectionGenerationContext context = SectionGenerationContext.forBlockCoord(chunk, section, baseY, this.columnSeed);
					BlockState centerState = lists[0x88].getOverlappingObject(baseY | 8);
					if (centerState != null) context.setAllStates(centerState);
					for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
						BlockSegmentList list = lists[horizontalIndex];
						int size = list.size();
						int yIndex = list.getSegmentIndex(baseY, false);
						while (yIndex < size) {
							Segment<BlockState> segment = list.get(yIndex);
							int segmentMinY = Math.max(segment.minY - baseY, 0);
							int segmentMaxY = Math.min(segment.maxY - baseY, 15);
							if (segmentMaxY >= segmentMinY) {
								int id = context.id(segment.value);
								PaletteStorage storage = context.storage();
								for (int blockY = segmentMinY; blockY <= segmentMaxY; blockY++) {
									storage.set((blockY << 8) | horizontalIndex, id);
								}
							}
							yIndex++;
						}
					}
				});
				for (Heightmap.Type type : chunk.getStatus().getHeightmapTypes()) {
					Heightmap heightmap = chunk.getHeightmap(type);
					@SuppressWarnings("CastToIncompatibleInterface")
					PaletteStorage heightmapStorage = ((Heightmap_StorageAccess)(heightmap)).bigglobe_getStorage();
					for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
						BlockSegmentList list = lists[horizontalIndex];
						if (!list.isEmpty()) {
							int height = getHeight(list, type);
							height = MathHelper.clamp(height - chunk.getBottomY(), 0, chunk.getHeight());
							heightmapStorage.set(horizontalIndex, height);
						}
					}
				}
				WorldWrapper worldWrapper = new WorldWrapper(
					new ChunkDelegator(chunk, this.worldSeed),
					this,
					new Permuter(Permuter.permute(this.worldSeed, chunk.getPos())),
					new Coordination(
						SymmetricOffset.IDENTITY,
						WorldUtil.chunkBox(chunk),
						WorldUtil.chunkBox(chunk)
					),
					Purpose.rawGeneration(distantHorizons)
				);
				worldWrapper.overriders = new AutoOverride(
					structures,
					this.getOverriders().rawColumnValues,
					this.getOverriders().rawColumnValueDependencies
				);
				for (ScriptedColumn column : columns) {
					worldWrapper.columns.put(ColumnPos.pack(column.x(), column.z()), column);
				}
				int minFilledSectionY_ = minFilledSectionY;
				int maxFilledSectionY_ = maxFilledSectionY;
				ScriptedColumnLookup.GLOBAL.run(worldWrapper, () -> {
					if (!distantHorizons) {
						for (ConfiguredRockReplacerFeature<?> replacer : this.feature_dispatcher.getFlattenedRockReplacers()) {
							replacer.replaceRocks(this, worldWrapper, chunk, minFilledSectionY_, maxFilledSectionY_);
						}
					}
					Async.loop(BigGlobeThreadPool.executor(distantHorizons), chunk.getBottomSectionCoord(), chunk.getTopSectionCoord(), 1, (int coord) -> {
						chunk.getSection(chunk.sectionCoordToIndex(coord)).calculateCounts();
					});
					this.generateRawStructures(chunk, structureAccessor, worldWrapper);
					this.feature_dispatcher.generateRaw(worldWrapper);
				});
			},
			executor
		)
		.handle((Void result, Throwable throwable) -> {
			if (throwable != null) {
				BigGlobeMod.LOGGER.error("Exception populating noise", throwable);
			}
			return chunk;
		});
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
		this.generateStructures(world, chunk, structureAccessor);
		WorldWrapper worldWrapper = new WorldWrapper(
			new WorldDelegator(world),
			this,
			new Permuter(Permuter.permute(this.worldSeed, chunk.getPos())),
			new Coordination(
				SymmetricOffset.IDENTITY,
				WorldUtil.chunkBox(chunk),
				WorldUtil.surroundingChunkBox(chunk)
			),
			Purpose.features()
		);
		ScriptStructures structures = ScriptStructures.getStructures(structureAccessor, chunk.getPos(), worldWrapper.distantHorizons());
		ScriptedColumn[] columns = this.chunkReuseColumns.get();
		worldWrapper.overriders = new AutoOverride(
			structures,
			this.getOverriders().featureColumnValues,
			this.getOverriders().featureColumnValueDependencies
		);
		try (AsyncConsumer<ScriptedColumn> async = new AsyncConsumer<>(BigGlobeThreadPool.autoExecutor(), (ScriptedColumn column) -> {
			worldWrapper.columns.put(ColumnPos.pack(column.x(), column.z()), column);
		})) {
			for (int index = 0; index < 256; index++) {
				final int index_ = index;
				async.submit(() -> {
					int x = chunk.getPos().getStartX() | (index_ & 15);
					int z = chunk.getPos().getStartZ() | (index_ >>> 4);
					columns[index_].setParamsUnchecked(worldWrapper.params.at(x, z));
					worldWrapper.overriders.override(columns[index_]);
					return columns[index_];
				});
			}
		}
		ScriptedColumnLookup.GLOBAL.accept(worldWrapper, this.feature_dispatcher::generateNormal);
	}

	public void generateRawStructures(Chunk chunk, StructureAccessor structureAccessor, ScriptedColumnLookup columns) {
		RawGenerationStructurePiece.Context context = null;
		BlockBox chunkBox = WorldUtil.chunkBox(chunk);
		for (RegistryEntry<Structure> structureEntry : this.sortedStructures.sortedStructures) {
			if (structureEntry.value() instanceof RawGenerationStructure) {
				List<StructureStart> starts = structureAccessor.getStructureStarts(ChunkSectionPos.from(chunk), structureEntry.value());
				for (int startIndex = 0, startCount = starts.size(); startIndex < startCount; startIndex++) {
					StructureStart start = starts.get(startIndex);
					if (start.hasChildren()) {
						long structureSeed = getStructureSeed(this.columnSeed, structureEntry, start);
						List<StructurePiece> children = start.getChildren();
						for (int pieceIndex = 0, pieceCount = children.size(); pieceIndex < pieceCount; pieceIndex++) {
							StructurePiece piece = children.get(pieceIndex);
							if (piece instanceof RawGenerationStructurePiece rawPiece && piece.getBoundingBox().intersects(chunkBox)) {
								long pieceSeed = Permuter.permute(structureSeed, pieceIndex);
								if (context == null) {
									context = new RawGenerationStructurePiece.Context(chunk, this, columns, DistantHorizonsCompat.isOnDistantHorizonThread());
								}
								context.pieceSeed = pieceSeed;
								rawPiece.generateRaw(context);
							}
						}
					}
				}
			}
		}
	}

	public void generateStructures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
		BlockBox chunkBox = WorldUtil.chunkBox(chunk);
		for (RegistryEntry<Structure> structureEntry : this.sortedStructures.sortedStructures) {
			List<StructureStart> starts = structureAccessor.getStructureStarts(ChunkSectionPos.from(chunk), structureEntry.value());
			for (int startIndex = 0, startCount = starts.size(); startIndex < startCount; startIndex++) {
				StructureStart start = starts.get(startIndex);
				if (start.hasChildren()) {
					long structureSeed = getStructureSeed(this.columnSeed, structureEntry, start);
					List<StructurePiece> children = start.getChildren();
					BlockBox firstPieceBB = children.get(0).getBoundingBox();
					BlockPos pivot = new BlockPos(
						(firstPieceBB.getMinX() + firstPieceBB.getMaxX() + 1) >> 1,
						firstPieceBB.getMinY(),
						(firstPieceBB.getMinZ() + firstPieceBB.getMaxZ() + 1) >> 1
					);
					for (int pieceIndex = 0, pieceCount = children.size(); pieceIndex < pieceCount; pieceIndex++) {
						StructurePiece piece = children.get(pieceIndex);
						if (piece.getBoundingBox().intersects(chunkBox)) {
							long pieceSeed = Permuter.permute(structureSeed, pieceIndex);
							try {
								piece.generate(
									world,
									structureAccessor,
									this,
									new MojangPermuter(pieceSeed),
									chunkBox,
									chunk.getPos(),
									pivot
								);
							}
							catch (NullPointerException exception) {
								//I don't know why DH seems to have issues with this when it's a vanilla bug,
								//but I'm tired of having my console spammed with errors.
								if (!DistantHorizonsCompat.isOnDistantHorizonThread()) {
									throw exception;
								}
								//else silently ignore.
							}
						}
					}
					start.getStructure().postPlace(
						world,
						structureAccessor,
						this,
						new MojangPermuter(structureSeed),
						chunkBox,
						chunk.getPos(),
						((StructureStart_ChildrenGetter)(Object)(start)).bigglobe_getChildren()
					);
				}
			}
		}
	}

	public static long getStructureSeed(long worldSeed, RegistryEntry<Structure> structureEntry, StructureStart start) {
		return Permuter.permute(worldSeed ^ 0x74ED298CF4DD2677L, UnregisteredObjectException.getID(structureEntry).hashCode(), start.getPos().x, start.getPos().z);
	}

	@Override
	public boolean trySetStructureStart(
		StructureSet.WeightedEntry weightedEntry,
		StructureAccessor structureAccessor,
		DynamicRegistryManager dynamicRegistryManager,
		NoiseConfig noiseConfig,
		StructureTemplateManager structureManager,
		long seed,
		Chunk chunk,
		ChunkPos pos,
		ChunkSectionPos sectionPos
	) {
		Structure structure = weightedEntry.structure().value();
		Predicate<RegistryEntry<Biome>> predicate = structure.getValidBiomes()::contains;
		while (structure instanceof DelegatingStructure delegating && delegating.canDelegateStart()) {
			structure = delegating.delegate.value();
		}
		StructureStart existingStart = structureAccessor.getStructureStart(sectionPos, structure, chunk);
		int references = existingStart != null ? existingStart.getReferences() : 0;
		StructurePosition newStartPosition = structure.
		#if MC_VERSION > MC_1_19_2
			getValidStructurePosition
		#else
			getStructurePosition
		#endif
		(
			new Structure.Context(
				dynamicRegistryManager,
				this,
				this.biomeSource,
				noiseConfig,
				structureManager,
				seed,
				chunk.getPos(),
				chunk,
				Predicates.alwaysTrue()
			)
		)
		.orElse(null);
		if (newStartPosition == null) return false;
		StructurePiecesCollector collector = newStartPosition.generate();
		StructureStart newStart = new StructureStart(structure, chunk.getPos(), references, collector.toList());
		if (!newStart.hasChildren()) return false;
		int oldY = newStart.getBoundingBox().getMinY();
		if (
			!this.canStructureSpawn(
				weightedEntry.structure(),
				newStart,
				new Permuter(
					Permuter.permute(
						this.columnSeed ^ 0xD59E69D9AB0D41BAL,
						//String.hashCode() will be cached, which means faster permutation times.
						UnregisteredObjectException.getID(weightedEntry.structure()).hashCode(),
						chunk.getPos().x,
						chunk.getPos().z
					)
				),
				DistantHorizonsCompat.isOnDistantHorizonThread()
			)
		) {
			return false;
		}
		int newY = newStart.getBoundingBox().getMinY();
		if (
			!predicate.test(
				this.biomeSource.getBiome(
					newStartPosition.position().getX() >> 2,
					(newStartPosition.position().getY() + (newY - oldY)) >> 2,
					newStartPosition.position().getZ() >> 2,
					noiseConfig.getMultiNoiseSampler()
				)
			)
		) {
			return false;
		}
		//expand structure bounding boxes so that overriders
		//which depend on them being expanded work properly.
		((StructureStart_BoundingBoxSetter)(Object)(newStart)).bigglobe_setBoundingBox(
			newStart.getBoundingBox().expand(
				weightedEntry.structure().value().getTerrainAdaptation() == StructureTerrainAdaptation.NONE
				? 16
				: 4
			)
		);
		structureAccessor.setStructureStart(sectionPos, structure, newStart, chunk);
		return true;
	}

	public boolean canStructureSpawn(RegistryEntry<Structure> entry, StructureStart start, Permuter permuter, boolean distantHorizons) {
		ScriptedColumnLookup lookup = new ScriptedColumnLookup.Impl(this.columnEntryRegistry.columnFactory, new ScriptedColumn.Params(this, 0, 0, Purpose.generic(distantHorizons)));
		StructureStartWrapper wrapper = StructureStartWrapper.of(entry, start);
		for (StructureOverrider overrider : this.getOverriders().structures) {
			if (!overrider.override(lookup, wrapper, permuter, distantHorizons)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public CompletableFuture<Chunk> populateBiomes(
		#if MC_VERSION == MC_1_19_2
			Registry<Biome> biomeRegistry,
		#endif
		Executor executor,
		NoiseConfig noiseConfig,
		Blender blender,
		StructureAccessor structureAccessor,
		Chunk chunk
	) {
		if (!(this.biomeSource instanceof ScriptedColumnBiomeSource source)) {
			return super.populateBiomes(
				#if MC_VERSION == MC_1_19_2
					biomeRegistry,
				#endif
				executor,
				noiseConfig,
				blender,
				structureAccessor,
				chunk
			);
		}
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		return CompletableFuture.runAsync(
			() -> {
				int bottomY = chunk.getBottomY();
				int topY = chunk.getTopY();
				ScriptedColumn column = this.newColumn(chunk, 0, 0, Purpose.generic(distantHorizons));
				for (int z = 0; z < 16; z += 4) {
					for (int x = 0; x < 16; x += 4) {
						column.setParamsUnchecked(column.params.at(chunk.getPos().getStartX() | x, chunk.getPos().getStartZ() | z));
						for (int y = bottomY; y < topY; y += 4) {
							ChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
							PalettedContainer<RegistryEntry<Biome>> container = (PalettedContainer<RegistryEntry<Biome>>)(section.getBiomeContainer());
							int newID = SectionUtil.id(container, source.script.get(column, y).entry());
							SectionUtil.storage(container).set(((y & 0b1100) << 2) | z | (x >>> 2), newID);
						}
					}
				}
			},
			executor
		)
		.handle((Void result, Throwable throwable) -> {
			if (throwable != null) {
				BigGlobeMod.LOGGER.error("Exception populating chunk biomes", throwable);
			}
			return chunk;
		});
	}

	@Override
	public int getSeaLevel() {
		Integer seaLevel = this.height.sea_level;
		return seaLevel != null ? seaLevel.intValue() : this.height.min_y;
	}

	@Override
	public int getMinimumY() {
		return this.height.min_y;
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		ScriptedColumn column = this.newColumn(world, x, z, Purpose.heightmap());
		BlockSegmentList list = new BlockSegmentList(world.getBottomY(), world.getTopY());
		this.layer.emitSegments(column, list);
		return getHeight(list, heightmap);
	}

	public static int getHeight(BlockSegmentList list, Heightmap.Type type) {
		for (int index = list.size(); --index >= 0;) {
			Segment<BlockState> segment = list.get(index);
			if (type.getBlockPredicate().test(segment.value)) {
				return segment.maxY + 1;
			}
		}
		return list.minY();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		ScriptedColumn column = this.newColumn(world, x, z, Purpose.heightmap());
		BlockSegmentList list = new BlockSegmentList(world.getBottomY(), world.getTopY());
		this.layer.emitSegments(column, list);
		BlockState[] states = list.flatten(BlockState[]::new);
		for (int index = 0, length = states.length; index < length; index++) {
			if (states[index] == null) states[index] = BlockStates.AIR;
		}
		return new VerticalBlockSample(world.getBottomY(), states);
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
		ScriptedColumn column = this.columnEntryRegistry.columnFactory.create(new ScriptedColumn.Params(this, pos.getX(), pos.getZ(), Purpose.GENERIC));
		this.rootDebugDisplay.forEach(column, pos.getY(), (String id, Object value) -> text.add(id + ": " + value));
	}

	public void setDisplay(String regex) {
		this.displayPattern = Pattern.compile(regex);
		this.rootDebugDisplay.recomputeChildren();
	}

	public static class DisplayEntry {

		public static final ObjectArrayFactory<DisplayEntry> ARRAY_FACTORY = new ObjectArrayFactory<>(DisplayEntry.class);

		public BigGlobeScriptedChunkGenerator generator;
		public String id;
		public Class<?> expectedValueType;
		public DisplayEntry[] children;

		public DisplayEntry(BigGlobeScriptedChunkGenerator generator) {
			this.generator = generator;
			this.id = "";
		}

		public DisplayEntry(BigGlobeScriptedChunkGenerator generator, Method method) {
			this.generator = generator;
			this.id = method.getDeclaredAnnotation(ColumnValueGetter.class).value();
		}

		public void forEach(ColumnValueHolder holder, int y, BiConsumer<String, Object> results) {
			try {
				Object value;
				if (this.id.isEmpty()) {
					value = holder;
				}
				else {
					value = holder.getColumnValue(this.id, y);
					results.accept(this.id, value);
				}
				if (value != null) {
					if (this.expectedValueType != value.getClass()) {
						this.expectedValueType = value.getClass();
						this.recomputeChildren();
					}
					if (value instanceof ColumnValueHolder nextHolder) {
						for (DisplayEntry child : this.children) {
							child.forEach(nextHolder, y, results);
						}
					}
				}
			}
			catch (Throwable throwable) {
				results.accept(this.id, throwable.toString());
			}
		}

		public void recomputeChildren() {
			if (
				this.generator.displayPattern != null &&
				ColumnValueHolder.class.isAssignableFrom(this.expectedValueType)
			) {
				this.children = (
					Arrays
					.stream(this.expectedValueType.getDeclaredMethods())
					.filter((Method method) -> method.isAnnotationPresent(ColumnValueGetter.class))
					.filter((Method method) -> this.generator.displayPattern.matcher(method.getDeclaredAnnotation(ColumnValueGetter.class).value()).find())
					.map((Method method) -> new DisplayEntry(this.generator, method))
					.toArray(ARRAY_FACTORY)
				);
			}
			else {
				this.children = ARRAY_FACTORY.empty();
			}
		}
	}
}