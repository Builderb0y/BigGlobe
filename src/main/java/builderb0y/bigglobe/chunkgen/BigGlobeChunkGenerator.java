package builderb0y.bigglobe.chunkgen;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.*;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructurePresence;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.decoders.RecordDecoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.perSection.RockLayerReplacer;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.columns.*;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.features.rockLayers.LinkedRockLayerConfig;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixinInterfaces.ColumnValueDisplayer;
import builderb0y.bigglobe.mixins.Heightmap_StorageAccess;
import builderb0y.bigglobe.mixins.StructureStart_BoundingBoxSetter;
import builderb0y.bigglobe.mixins.StructureStart_ChildrenGetter;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.structures.DelegatingStructure;
import builderb0y.bigglobe.structures.RawGenerationStructure;
import builderb0y.bigglobe.structures.RawGenerationStructure.RawGenerationStructurePiece;
import builderb0y.bigglobe.util.*;
import builderb0y.bigglobe.versions.RegistryEntryListVersions;

#if MC_VERSION == MC_1_19_2
	import net.minecraft.registry.Registry;
	import java.util.concurrent.ConcurrentLinkedQueue;
	import builderb0y.autocodec.annotations.AddPseudoField;
	import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
#elif MC_VERSION == MC_1_19_4
	import java.util.concurrent.ConcurrentLinkedQueue;
	import net.minecraft.registry.RegistryWrapper;
	import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
#elif MC_VERSION >= MC_1_20_1 && MC_VERSION <= MC_1_20_4
	import net.minecraft.registry.RegistryWrapper;
	import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
#endif

#if MC_VERSION == MC_1_19_2
@AddPseudoField("structureSetRegistry")
#endif
public abstract class BigGlobeChunkGenerator extends ChunkGenerator implements ColumnValueDisplayer {

	public static final boolean WORLD_SLICES = false;

	public final SortedFeatures configuredFeatures;
	public transient ColumnValue<?>[] displayedColumnValues;

	public transient long seed;
	public final SortedStructures sortedStructures;
	public final transient WorldgenProfiler profiler = new WorldgenProfiler();
	public final transient ChunkOfColumnsRecycler chunkOfColumnsRecycler = new ChunkOfColumnsRecycler(this);

	public BigGlobeChunkGenerator(
		#if MC_VERSION == MC_1_19_2
		BetterRegistry<StructureSet> structureSetRegistry,
		#endif
		BiomeSource biomeSource,
		SortedFeatures configuredFeatures,
		SortedStructures sortedStructures
	) {
		super(#if (MC_VERSION == MC_1_19_2) ((BetterHardCodedRegistry<StructureSet>)(structureSetRegistry)).registry, Optional.empty(), #endif biomeSource);
		this.configuredFeatures = configuredFeatures;
		this.sortedStructures = sortedStructures;
	}

	#if MC_VERSION == MC_1_19_2
		public BetterRegistry<StructureSet> structureSetRegistry() {
			return new BetterHardCodedRegistry<>(this.structureSetRegistry);
		}
	#endif

	@Wrapper
	public static class SortedFeatures {

		public final BetterRegistry<ConfiguredFeature<?, ?>> registry;
		public final transient Map<Feature<?>, List<RegistryEntry<ConfiguredFeature<?, ?>>>> map;

		public SortedFeatures(BetterRegistry<ConfiguredFeature<?, ?>> registry) {
			this.registry = registry;
			Map<Feature<?>, List<RegistryEntry<ConfiguredFeature<?, ?>>>> map = new HashMap<>(128);
			registry.streamEntries().forEach(entry -> {
				map.computeIfAbsent(entry.value().feature(), $ -> new ArrayList<>(4)).add(entry);
			});
			this.map = map;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <C extends FeatureConfig> Stream<RegistryEntry<ConfiguredFeature<C, Feature<C>>>> streamRegistryEntries(Feature<C> feature) {
			return (Stream)(this.map.getOrDefault(feature, Collections.emptyList()).stream());
		}

		public <C extends FeatureConfig> Stream<ConfiguredFeature<C, Feature<C>>> streamConfiguredFeatures(Feature<C> feature) {
			return this.streamRegistryEntries(feature).map(RegistryEntry::value);
		}

		public <C extends FeatureConfig> Stream<C> streamConfigs(Feature<C> feature) {
			return this.streamConfiguredFeatures(feature).map(ConfiguredFeature::config);
		}
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

	public static <T_Generator extends BigGlobeChunkGenerator> AutoCoder<T_Generator> createCoder(FactoryContext<T_Generator> context, String preset, String dimensionName) {
		AutoCoder<T_Generator> coder = (AutoCoder<T_Generator>)(context.forceCreateDecoder(RecordDecoder.Factory.INSTANCE));
		if (!BigGlobeConfig.INSTANCE.get().reloadGenerators) return coder;
		return new AutoCoder<T_Generator>() {

			@Override
			public <T_Encoded> @Nullable T_Generator decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
				JsonElement json = this.getDimension(preset, dimensionName);
				T_Encoded encoded = JsonOps.INSTANCE.convertTo(context.ops, json);
				return context.autoCodec.decode(coder, encoded, context.ops);
			}

			@Override
			public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, T_Generator> context) throws EncodeException {
				return context.encodeWith(coder);
			}

			public JsonElement getDimension(String preset, String dimension) {
				BigGlobeMod.LOGGER.info("Reading " + dimension + " chunk generator from mod jar.");
				return (
					this
					.getJson("/data/bigglobe/worldgen/world_preset/" + preset + ".json")
					.getAsJsonObject()
					.getAsJsonObject("dimensions")
					.getAsJsonObject("minecraft:" + dimension)
					.getAsJsonObject("generator")
					.getAsJsonObject("value")
				);
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
		};
	}

	public void setSeed(long seed) {
		this.seed = seed;
		if (this.biomeSource instanceof ColumnBiomeSource columnBiomeSource) {
			columnBiomeSource.setGenerator(this);
		}
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

	public abstract WorldColumn column(int x, int z);

	public abstract void populateChunkOfColumns(AbstractChunkOfColumns<? extends WorldColumn> columns, ChunkPos chunkPos, ScriptStructures structures, boolean distantHorizons);

	public ChunkOfColumns<? extends WorldColumn> getChunkOfColumns(Chunk chunk, ScriptStructures structures, boolean distantHorizons) {
		return this.chunkOfColumnsRecycler.get(chunk, structures, distantHorizons);
	}

	public ScriptStructures preGenerateFeatureColumns(StructureWorldAccess world, ChunkPos chunkPos, StructureAccessor structureAccessor, boolean distantHorizons) {
		ScriptStructures structures = null;
		for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
			for (int offsetX = -1; offsetX <= 1; offsetX++) {
				ChunkPos newPos = new ChunkPos(chunkPos.x + offsetX, chunkPos.z + offsetZ);
				ScriptStructures newStructures = ScriptStructures.getStructures(structureAccessor, chunkPos, distantHorizons);
				this.getChunkOfColumns(world.getChunk(newPos.x, newPos.z), newStructures, distantHorizons);
				if (offsetX == 0 && offsetZ == 0) {
					structures = newStructures;
				}
			}
		}
		return structures;
	}

	@Override
	public abstract Codec<? extends ChunkGenerator> getCodec();

	public void generateSectionsParallelSimple(Chunk chunk, int minYInclusive, int maxYExclusive, ChunkOfColumns<? extends WorldColumn> columns, Consumer<SectionGenerationContext> generator) {
		long seed = this.seed;
		Async.loop(
			Math.max(chunk.getSectionIndex(minYInclusive), 0),
			Math.min(chunk.getSectionIndex(maxYExclusive - 1 /* convert to inclusive */), chunk.getSectionArray().length - 1) + 1 /* convert back to exclusive */,
			1,
			(int index) -> {
				ChunkSection section = chunk.getSection(index);
				section.lock();
				try {
					//generator.accept(SectionGenerationContext.forIndex(chunk, section, index, seed, columns));
				}
				finally {
					section.unlock();
				}
			}
		);
	}

	public void generateSectionsParallel(Chunk chunk, int minYInclusive, int maxYExclusive, ChunkOfColumns<? extends WorldColumn> columns, Consumer<SectionGenerationContext> generator) {
		long seed = this.seed;
		#if MC_VERSION < MC_1_20_0
			ConcurrentLinkedQueue<LightPositionCollector> lights = chunk instanceof ProtoChunk ? new ConcurrentLinkedQueue<>() : null;
		#endif
		Async.loop(
			Math.max(chunk.getSectionIndex(minYInclusive), 0),
			Math.min(chunk.getSectionIndex(maxYExclusive - 1 /* convert to inclusive */), chunk.getSectionArray().length - 1) + 1 /* convert back to exclusive */,
			1,
			(int index) -> {
				ChunkSection section = chunk.getSection(index);
				section.lock();
				try {
					SectionGenerationContext context = null; //SectionGenerationContext.forIndex(chunk, section, index, seed, columns);
					generator.accept(context);
					#if MC_VERSION < MC_1_20_0
						if (context.hasLights()) {
							lights.add(context.lights());
						}
					#endif
				}
				finally {
					section.unlock();
				}
			}
		);
		#if MC_VERSION < MC_1_20_0
			if (lights != null) {
				ProtoChunk protoChunk = (ProtoChunk)(chunk);
				for (LightPositionCollector collector; (collector = lights.poll()) != null; ) {
					for (BlockPos pos : collector) {
						protoChunk.addLightSource(pos);
					}
				}
			}
		#endif
	}

	public void setHeightmaps(Chunk chunk, HeightmapSupplier heightGetter) {
		for (Heightmap.Type type : chunk.getStatus().getHeightmapTypes()) {
			Heightmap heightmap = chunk.getHeightmap(type);
			boolean isWaterType = type.getBlockPredicate().test(BlockStates.WATER);
			@SuppressWarnings("CastToIncompatibleInterface")
			PaletteStorage heightmapStorage = ((Heightmap_StorageAccess)(heightmap)).bigglobe_getStorage();
			for (int index = 0; index < 256; index++) {
				int height = heightGetter.getHeight(index, isWaterType);
				height = MathHelper.clamp(height - this.getMinimumY(), 0, this.getWorldHeight());
				heightmapStorage.set(index, height);
			}
		}
	}

	@FunctionalInterface
	public static interface HeightmapSupplier {

		public abstract int getHeight(int index, boolean includeWater);
	}

	public abstract void generateRawTerrain(
		Executor executor,
		Chunk chunk,
		StructureAccessor structureAccessor,
		boolean distantHorizons
	);

	@Override
	public CompletableFuture<Chunk> populateNoise(
		Executor executor,
		Blender blender,
		NoiseConfig noiseConfig,
		StructureAccessor structureAccessor,
		Chunk chunk
	) {
		if (WORLD_SLICES && (chunk.getPos().x & 3) != 0) return CompletableFuture.completedFuture(chunk);

		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		return CompletableFuture.runAsync(
			() -> this.profiler.run("populateNoise", () -> this.generateRawTerrain(executor, chunk, structureAccessor, distantHorizons)),
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
	public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess world, StructureAccessor structureAccessor, Chunk chunk, Carver carverStep) {
		//no-op.
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
		//no-op.
	}

	@Override
	public abstract void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor);

	public void runDecorators(
		StructureWorldAccess world,
		BlockPos.Mutable pos,
		MojangPermuter permuter,
		SortedFeatureTag decorator,
		IntList yLevels
	) {
		if (decorator != null && yLevels != null && !yLevels.isEmpty()) {
			RegistryEntry<ConfiguredFeature<?, ?>>[] features = decorator.getSortedFeatures();
			if (features.length != 0) {
				this.profiler.run(RegistryEntryListVersions.getKeyOptional(decorator.list).<Object>map(TagKey::id).orElse("<unknown>"), () -> {
					long columnSeed = permuter.getSeed();
					for (int yIndex = 0, size = yLevels.size(); yIndex < size; yIndex++) {
						int y = yLevels.getInt(yIndex);
						pos.setY(y);
						long blockSeed = Permuter.permute(columnSeed, y);
						for (int featureIndex = 0, featureCount = features.length; featureIndex < featureCount; featureIndex++) {
							permuter.setSeed(Permuter.permute(blockSeed, UnregisteredObjectException.getID(features[featureIndex]).hashCode()));
							features[featureIndex].value().generate(world, this, permuter, pos);
						}
					}
					permuter.setSeed(columnSeed);
				});
			}
		}
	}

	public void runDecorators(
		StructureWorldAccess world,
		BlockPos.Mutable pos,
		MojangPermuter permuter,
		SortedFeatureTag decorator,
		int yLevel
	) {
		if (decorator != null && yLevel != Integer.MIN_VALUE) {
			RegistryEntry<ConfiguredFeature<?, ?>>[] features = decorator.getSortedFeatures();
			if (features.length != 0) {
				this.profiler.run(RegistryEntryListVersions.getKeyOptional(decorator.list).<Object>map(TagKey::id).orElse("<unknown>"), () -> {
					long columnSeed = permuter.getSeed();
					pos.setY(yLevel);
					long blockSeed = Permuter.permute(columnSeed, yLevel);
					for (int featureIndex = 0, featureCount = features.length; featureIndex < featureCount; featureIndex++) {
						permuter.setSeed(Permuter.permute(blockSeed, UnregisteredObjectException.getID(features[featureIndex]).hashCode()));
						features[featureIndex].value().generate(world, this, permuter, pos);
					}
					permuter.setSeed(columnSeed);
				});
			}
		}
	}

	public void generateRockLayers(LinkedRockLayerConfig<?>[] rockLayers, Chunk chunk, int minHeight, int maxHeight, ChunkOfColumns<? extends WorldColumn> columns) {
		this.profiler.run("Rock layers", () -> {
			for (LinkedRockLayerConfig<?> rock : rockLayers) {
				this.profiler.run(rock.name, () -> {
					//RockLayerReplacer.generateNew(this.seed, chunk, columns, minHeight, maxHeight, rock);
				});
			}
		});
	}

	@Override
	public void setStructureStarts(
		#if MC_VERSION == MC_1_19_2
			DynamicRegistryManager registryManager,
			NoiseConfig noiseConfig,
			StructureAccessor structureAccessor,
			Chunk chunk,
			StructureTemplateManager structureTemplateManager,
			long seed
		#else
			DynamicRegistryManager registryManager,
			StructurePlacementCalculator placementCalculator,
			StructureAccessor structureAccessor,
			Chunk chunk,
			StructureTemplateManager structureTemplateManager
		#endif
	) {
		if (
			BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipStructures &&
			DistantHorizonsCompat.isOnDistantHorizonThread()
		) {
			return;
		}
		this.profiler.run("setStructureStarts", () -> {
			this.actuallySetStructureStarts(
				#if MC_VERSION == MC_1_19_2
					registryManager,
					noiseConfig,
					structureAccessor,
					chunk,
					structureTemplateManager,
					seed
				#else
					registryManager,
					placementCalculator,
					structureAccessor,
					chunk,
					structureTemplateManager
				#endif
			);
		});
	}

	#if MC_VERSION == MC_1_19_2
		public void actuallySetStructureStarts(
			DynamicRegistryManager registryManager,
			NoiseConfig noiseConfig,
			StructureAccessor structureAccessor,
			Chunk chunk,
			StructureTemplateManager structureTemplateManager,
			long seed
		) {
			super.setStructureStarts(
				registryManager,
				noiseConfig,
				structureAccessor,
				chunk,
				structureTemplateManager,
				seed
			);
		}
	#else
		public void actuallySetStructureStarts(
			DynamicRegistryManager registryManager,
			StructurePlacementCalculator placementCalculator,
			StructureAccessor structureAccessor,
			Chunk chunk,
			StructureTemplateManager structureTemplateManager
		) {
			super.setStructureStarts(
				registryManager,
				placementCalculator,
				structureAccessor,
				chunk,
				structureTemplateManager
			);
		}
	#endif

	@Override
	public void addStructureReferences(StructureWorldAccess world, StructureAccessor structureAccessor, Chunk chunk) {
		if (
			BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipStructures &&
			DistantHorizonsCompat.isOnDistantHorizonThread()
		) {
			return;
		}
		this.profiler.run("addStructureReferences", () -> {
			super.addStructureReferences(world, structureAccessor, chunk);
		});
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
		return this.setStructureStart(
			weightedEntry,
			structureAccessor,
			dynamicRegistryManager,
			noiseConfig,
			structureManager,
			seed,
			chunk,
			false
		);
	}

	public boolean forceSetStructureStart(
		StructureSet.WeightedEntry weightedEntry,
		StructureAccessor structureAccessor,
		DynamicRegistryManager dynamicRegistryManager,
		NoiseConfig noiseConfig,
		StructureTemplateManager structureManager,
		long seed,
		Chunk chunk
	) {
		return this.setStructureStart(
			weightedEntry,
			structureAccessor,
			dynamicRegistryManager,
			noiseConfig,
			structureManager,
			seed,
			chunk,
			true
		);
	}

	public boolean setStructureStart(
		StructureSet.WeightedEntry weightedEntry,
		StructureAccessor structureAccessor,
		DynamicRegistryManager dynamicRegistryManager,
		NoiseConfig noiseConfig,
		StructureTemplateManager structureManager,
		long seed,
		Chunk chunk,
		boolean force
	) {
		ChunkSectionPos sectionPos = ChunkSectionPos.from(chunk);
		Structure structure = weightedEntry.structure().value();
		Predicate<RegistryEntry<Biome>> predicate = force ? Predicates.alwaysTrue() : structure.getValidBiomes()::contains;
		while (structure instanceof DelegatingStructure delegating && delegating.canDelegateStart()) {
			structure = delegating.delegate.value();
		}
		StructureStart existingStart = structureAccessor.getStructureStart(sectionPos, structure, chunk);
		int references = existingStart != null ? existingStart.getReferences() : 0;
		StructureStart newStart = structure.createStructureStart(
			dynamicRegistryManager,
			this,
			this.biomeSource,
			noiseConfig,
			structureManager,
			seed,
			chunk.getPos(),
			references,
			chunk,
			predicate
		);
		if (
			newStart.hasChildren() &&
			this.canStructureSpawn(
				weightedEntry.structure(),
				newStart,
				new Permuter(
					Permuter.permute(
						Permuter.permute(
							this.seed ^ 0xD59E69D9AB0D41BAL,
							chunk.getPos().x,
							chunk.getPos().z,
							//String.hashCode() will be cached, which means faster permutation times.
							UnregisteredObjectException.getID(weightedEntry.structure()).hashCode()
						),
						chunk.getPos()
					)
				),
				DistantHorizonsCompat.isOnDistantHorizonThread()
			)
		) {
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
		return false;
	}

	public boolean canStructureSpawn(RegistryEntry<Structure> entry, StructureStart start, Permuter permuter, boolean distantHorizons) {
		return true;
	}

	@SuppressWarnings("deprecation")
	public static void moveStructure(StructureStart start, int dx, int dy, int dz) {
		start.getBoundingBox().move(dx, dy, dz);
		for (StructurePiece child : start.getChildren()) {
			child.translate(dx, dy, dz);
		}
	}

	public void generateRawStructures(Chunk chunk, StructureAccessor structureAccessor, ChunkOfColumns<? extends WorldColumn> columns) {
		if (WORLD_SLICES && (chunk.getPos().x & 3) != 0) return;

		RawGenerationStructurePiece.Context context = null;
		BlockBox chunkBox = WorldUtil.chunkBox(chunk);
		for (RegistryEntry<Structure> structureEntry : this.sortedStructures.sortedStructures) {
			if (structureEntry.value() instanceof RawGenerationStructure) {
				List<StructureStart> starts = structureAccessor.getStructureStarts(ChunkSectionPos.from(chunk), structureEntry.value());
				for (int startIndex = 0, startCount = starts.size(); startIndex < startCount; startIndex++) {
					StructureStart start = starts.get(startIndex);
					if (start.hasChildren()) {
						long structureSeed = getStructureSeed(this.seed, structureEntry, start);
						List<StructurePiece> children = start.getChildren();
						for (int pieceIndex = 0, pieceCount = children.size(); pieceIndex < pieceCount; pieceIndex++) {
							StructurePiece piece = children.get(pieceIndex);
							if (piece instanceof RawGenerationStructurePiece rawPiece && piece.getBoundingBox().intersects(chunkBox)) {
								long pieceSeed = Permuter.permute(structureSeed, pieceIndex);
								if (context == null) {
									context = new RawGenerationStructurePiece.Context(0L, chunk, null /* this */, columns, DistantHorizonsCompat.isOnDistantHorizonThread());
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
		if (WORLD_SLICES && (chunk.getPos().x & 3) != 0) return;

		BlockBox chunkBox = WorldUtil.chunkBox(chunk);
		for (RegistryEntry<Structure> structureEntry : this.sortedStructures.sortedStructures) {
			List<StructureStart> starts = structureAccessor.getStructureStarts(ChunkSectionPos.from(chunk), structureEntry.value());
			for (int startIndex = 0, startCount = starts.size(); startIndex < startCount; startIndex++) {
				StructureStart start = starts.get(startIndex);
				if (start.hasChildren()) {
					long structureSeed = getStructureSeed(this.seed, structureEntry, start);
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

	public class StructureFinder {

		public final ServerWorld world;
		public final StructureAccessor structureAccessor;
		public final NoiseConfig noiseConfig;
		public final RegistryEntryList<Structure> structures;
		public final boolean skipReferencedStructures;

		public int chunkX;
		public int chunkZ;
		public RegistryEntry<Structure> structure;

		public StructureFinder(ServerWorld world, RegistryEntryList<Structure> structures, boolean skipReferencedStructures) {
			this.world = world;
			this.structureAccessor = world.getStructureAccessor();
			this.noiseConfig = world.getChunkManager().getNoiseConfig();
			this.structures = structures;
			this.skipReferencedStructures = skipReferencedStructures;
		}

		public boolean tryStrongholds(int centerChunkX, int centerChunkZ) {
			int distance = Integer.MAX_VALUE;
			for (RegistryEntry<Structure> structure : this.structures) {
				List<StructurePlacement> placements = (
					#if MC_VERSION == MC_1_19_2
						BigGlobeChunkGenerator.this.getStructurePlacement(structure, this.noiseConfig)
					#else
						this.world.getChunkManager().getStructurePlacementCalculator().getPlacements(structure)
					#endif
				);
				for (StructurePlacement placement : placements) {
					if (placement instanceof ConcentricRingsStructurePlacement concentric) {
						List<ChunkPos> positions = (
							#if MC_VERSION == MC_1_19_2
								BigGlobeChunkGenerator.this.getConcentricRingsStartChunks(concentric, this.noiseConfig)
							#else
								this.world.getChunkManager().getStructurePlacementCalculator().getPlacementPositions(concentric)
							#endif
						);
						if (positions != null) for (ChunkPos pos : positions) {
							int newDistance = BigGlobeMath.squareI(pos.x - centerChunkX, pos.z - centerChunkZ);
							if (newDistance < distance) {
								distance = newDistance;
								this.chunkX = pos.x;
								this.chunkZ = pos.z;
								this.structure = structure;
							}
						}
					}
				}
			}
			return this.structure != null;
		}

		public static boolean checkNotReferenced(StructureAccessor accessor, StructureStart start) {
			//match logic from ChunkGenerator.checkNotReferenced().
			if (start.isNeverReferenced()) {
				accessor.incrementReferences(start);
				return true;
			}
			return false;
		}

		#if MC_VERSION == MC_1_19_2
			public boolean canPossiblyGenerate(int chunkX, int chunkZ, RegistryEntry<Structure> structure) {
				List<StructurePlacement> placements = BigGlobeChunkGenerator.this.getStructurePlacement(structure, this.noiseConfig);
				for (StructurePlacement placement : placements) {
					if (placement.shouldGenerate(BigGlobeChunkGenerator.this, this.noiseConfig, this.noiseConfig.getLegacyWorldSeed(), chunkX, chunkZ)) {
						return true;
					}
				}
				return false;
			}
		#else
			public boolean canPossiblyGenerate(int chunkX, int chunkZ, RegistryEntry<Structure> structure) {
				StructurePlacementCalculator calculator = this.world.getChunkManager().getStructurePlacementCalculator();
				List<StructurePlacement> placements = calculator.getPlacements(structure);
				for (StructurePlacement placement : placements) {
					if (placement.shouldGenerate(calculator, chunkX, chunkZ)) {
						return true;
					}
				}
				return false;
			}
		#endif

		public RegistryEntry<Structure> unwrap(RegistryEntry<Structure> entry) {
			while (entry.value() instanceof DelegatingStructure delegating && delegating.canDelegateStart()) {
				entry = delegating.delegate;
			}
			return entry;
		}

		public boolean update(int chunkX, int chunkZ) {
			if (this.structure == null) {
				for (RegistryEntry<Structure> structure : this.structures) {
					if (this.canPossiblyGenerate(chunkX, chunkZ, structure)) {
						//match logic from ChunkGenerator.locateStructure().
						StructurePresence presence = this.structureAccessor.getStructurePresence(new ChunkPos(chunkX, chunkZ), this.unwrap(structure).value(), this.skipReferencedStructures);
						if (presence == StructurePresence.START_NOT_PRESENT) continue;
						if (!this.skipReferencedStructures && presence == StructurePresence.START_PRESENT) {
							this.chunkX = chunkX;
							this.chunkZ = chunkZ;
							this.structure = structure;
							break;
						}
						Chunk chunk = this.world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS);
						StructureStart start = this.structureAccessor.getStructureStart(ChunkSectionPos.from(chunk), this.unwrap(structure).value(), chunk);
						if (start == null || !start.hasChildren() || this.skipReferencedStructures && !checkNotReferenced(this.structureAccessor, start)) continue;
						this.chunkX = chunkX;
						this.chunkZ = chunkZ;
						this.structure = structure;
						break;
					}
				}
			}
			return this.structure != null;
		}

		public Pair<BlockPos, RegistryEntry<Structure>> toPair() {
			return Pair.of(new BlockPos((this.chunkX << 4) | 8, 0, (this.chunkZ << 4) | 8), this.structure);
		}
	}

	public StructureFinder structureFinder(ServerWorld world, RegistryEntryList<Structure> structures, boolean skipReferencedStructures) {
		return this.new StructureFinder(world, structures, skipReferencedStructures);
	}

	@Nullable
	@Override
	public Pair<BlockPos, RegistryEntry<Structure>> locateStructure(
		ServerWorld world,
		RegistryEntryList<Structure> structures,
		BlockPos center,
		int maxRadius,
		boolean skipReferencedStructures
	) {
		if (structures.size() == 0) {
			return null;
		}
		int centerChunkX = center.getX() >> 4;
		int centerChunkZ = center.getZ() >> 4;
		StructureFinder finder = this.structureFinder(world, structures, skipReferencedStructures);
		if (finder.update(centerChunkX, centerChunkZ)) return finder.toPair();
		for (int radius = 1; radius <= maxRadius; radius++) {
			if (finder.update(centerChunkX + radius, centerChunkZ)) return finder.toPair();
			if (finder.update(centerChunkX - radius, centerChunkZ)) return finder.toPair();
			if (finder.update(centerChunkX, centerChunkZ + radius)) return finder.toPair();
			if (finder.update(centerChunkX, centerChunkZ - radius)) return finder.toPair();
			for (int outwards = 1; outwards < radius; outwards++) {
				if (finder.update(centerChunkX + radius, centerChunkZ + outwards)) return finder.toPair();
				if (finder.update(centerChunkX + radius, centerChunkZ - outwards)) return finder.toPair();
				if (finder.update(centerChunkX - radius, centerChunkZ + outwards)) return finder.toPair();
				if (finder.update(centerChunkX - radius, centerChunkZ - outwards)) return finder.toPair();
				if (finder.update(centerChunkX + outwards, centerChunkZ + radius)) return finder.toPair();
				if (finder.update(centerChunkX - outwards, centerChunkZ + radius)) return finder.toPair();
				if (finder.update(centerChunkX + outwards, centerChunkZ - radius)) return finder.toPair();
				if (finder.update(centerChunkX - outwards, centerChunkZ - radius)) return finder.toPair();
			}
			if (finder.update(centerChunkX + radius, centerChunkZ + radius)) return finder.toPair();
			if (finder.update(centerChunkX + radius, centerChunkZ - radius)) return finder.toPair();
			if (finder.update(centerChunkX - radius, centerChunkZ + radius)) return finder.toPair();
			if (finder.update(centerChunkX - radius, centerChunkZ - radius)) return finder.toPair();
		}
		if (finder.tryStrongholds(centerChunkX, centerChunkZ)) return finder.toPair();
		return null;
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos blockPos) {
		text.add("Reclaimed chunks of columns: " + this.chunkOfColumnsRecycler.available.valueCount + " / " + ChunkOfColumnsRecycler.RECYCLER_SIZE);
		this.bigglobe_appendText(text, this.column(blockPos.getX(), blockPos.getZ()), blockPos.getY());
	}

	//public final transient SemiThreadLocal<ChunkOfBiomeColumns<? extends WorldColumn>> biomeColumns = SemiThreadLocal.strong(4, () -> new ChunkOfBiomeColumns<>(this::column));

	/*
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
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		return CompletableFuture.supplyAsync(
			() -> this.profiler.get("populateBiomes", () -> {
				ChunkOfBiomeColumns<? extends WorldColumn> columns = this.biomeColumns.get();
				try {
					this.populateChunkOfColumns(columns, chunk.getPos(), ScriptStructures.EMPTY_SCRIPT_STRUCTURES, distantHorizons);
					Async.loop(chunk.getBottomSectionCoord(), chunk.getTopSectionCoord(), 1, (int sectionY) -> {
						ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(sectionY));
						int startY = sectionY << 4;
						PalettedContainer<RegistryEntry<Biome>> container = (PalettedContainer<RegistryEntry<Biome>>)(section.getBiomeContainer());
						for (int zIndex = 0; zIndex < 4; zIndex++) {
							for (int xIndex = 0; xIndex < 4; xIndex++) {
								WorldColumn column = columns.getColumn(xIndex << 2, zIndex << 2);
								for (int yIndex = 0; yIndex < 4; yIndex++) {
									int y = startY | (yIndex << 2);
									int newID = SectionUtil.id(container, column.getBiome(y));
									SectionUtil.storage(container).set((yIndex << 4) | (zIndex << 2) | xIndex, newID);
								}
							}
						}
					});
				}
				finally {
					this.biomeColumns.reclaim(columns);
				}
				return chunk;
			}),
			executor
		)
		.handle((result, throwable) -> {
			if (throwable != null) {
				BigGlobeMod.LOGGER.error("Exception populating chunk biomes", throwable);
			}
			return chunk;
		});
	}
	*/

	@Override
	public ColumnValue<?>[] bigglobe_getDisplayedColumnValues() {
		return this.displayedColumnValues;
	}

	@Override
	public void bigglobe_setDisplayedColumnValues(ColumnValue<?>[] displayedColumnValues) {
		this.displayedColumnValues = displayedColumnValues;
	}
}