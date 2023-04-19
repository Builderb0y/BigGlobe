package builderb0y.bigglobe.chunkgen;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructurePresence;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.decoders.RecordDecoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.perSection.RockLayerReplacer;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.features.rockLayers.LinkedRockLayerConfig;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixinInterfaces.ColumnValueDisplayer;
import builderb0y.bigglobe.mixinInterfaces.StructurePlacementCalculatorWithChunkGenerator;
import builderb0y.bigglobe.mixins.Heightmap_StorageAccess;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.WorldgenProfiler;

public abstract class BigGlobeChunkGenerator extends ChunkGenerator implements ColumnValueDisplayer {

	public static final boolean WORLD_SLICES = false;

	public static final GenerationStep.Feature[] FEATURE_STEPS = GenerationStep.Feature.values();
	public static final ObjectArrayFactory<RegistryEntry<?>> REGISTRY_ENTRY_ARRAY_FACTORY = new ObjectArrayFactory<>(RegistryEntry.class).generic();

	@EncodeInline
	public final SortedFeatures configuredFeatures;
	public transient ColumnValue<?>[] displayedColumnValues;

	public transient long seed;
	//no idea if this needs to be synchronized or not, but it can't hurt.
	public final transient Map<GenerationStep.Feature, RegistryEntry<Structure>[]> sortedStructures = Collections.synchronizedMap(new EnumMap<>(GenerationStep.Feature.class));
	public final transient WorldgenProfiler profiler = new WorldgenProfiler();

	public BigGlobeChunkGenerator(BiomeSource biomeSource, SortedFeatures configuredFeatures) {
		super(biomeSource);
		this.configuredFeatures = configuredFeatures;
	}

	@Wrapper
	public static class SortedFeatures {

		public final RegistryWrapper<ConfiguredFeature<?, ?>> registry;
		public final Map<Feature<?>, List<RegistryEntry<ConfiguredFeature<?, ?>>>> map;

		public SortedFeatures(RegistryWrapper<ConfiguredFeature<?, ?>> registry) {
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

	@Override
	public StructurePlacementCalculator createStructurePlacementCalculator(RegistryWrapper<StructureSet> structureSetRegistry, NoiseConfig noiseConfig, long seed) {
		this.setSeed(seed);
		StructurePlacementCalculator calculator = super.createStructurePlacementCalculator(structureSetRegistry, noiseConfig, seed);
		((StructurePlacementCalculatorWithChunkGenerator)(calculator)).bigglobe_setChunkGenerator(this);
		return calculator;
	}

	public abstract WorldColumn column(int x, int z);

	public void generateSectionsParallelSimple(Chunk chunk, int minYInclusive, int maxYExclusive, ChunkOfColumns<? extends WorldColumn> columns, Consumer<SectionGenerationContext> generator) {
		long seed = this.seed;
		Arrays
		.stream(
			chunk.getSectionArray(),
			chunk.getSectionIndex(minYInclusive),
			chunk.getSectionIndex(maxYExclusive - 1 /* convert to inclusive */) + 1 /* and then back to exclusive for stream() */
		)
		.parallel()
		.forEach((ChunkSection section) -> {
			section.lock();
			try {
				generator.accept(new SectionGenerationContext(chunk, section, seed, columns));
			}
			finally {
				section.unlock();
			}
		});
	}

	public void generateSectionsParallel(Chunk chunk, int minYInclusive, int maxYExclusive, ChunkOfColumns<? extends WorldColumn> columns, Consumer<SectionGenerationContext> generator) {
		long seed = this.seed;
		ConcurrentLinkedQueue<LightPositionCollector> lights = chunk instanceof ProtoChunk ? new ConcurrentLinkedQueue<>() : null;
		Arrays
		.stream(
			chunk.getSectionArray(),
			chunk.getSectionIndex(minYInclusive),
			chunk.getSectionIndex(maxYExclusive - 1 /* convert to inclusive */) + 1 /* and then back to exclusive for stream() */
		)
		.parallel()
		.forEach((ChunkSection section) -> {
			section.lock();
			try {
				SectionGenerationContext context = new SectionGenerationContext(chunk, section, seed, columns);
				generator.accept(context);
				if (context.hasLights()) {
					lights.add(context.lights());
				}
			}
			finally {
				section.unlock();
			}
		});
		if (lights != null) {
			ProtoChunk protoChunk = (ProtoChunk)(chunk);
			for (LightPositionCollector collector; (collector = lights.poll()) != null; ) {
				for (BlockPos pos : collector) {
					protoChunk.addLightSource(pos);
				}
			}
		}
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
				BigGlobeMod.LOGGER.error("", throwable);
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
			ConfiguredFeature<?, ?>[] features = decorator.getSortedFeatures(world);
			if (features.length != 0) {
				this.profiler.run(decorator.key.id(), () -> {
					long columnSeed = permuter.getSeed();
					for (int yIndex = 0, size = yLevels.size(); yIndex < size; yIndex++) {
						int y = yLevels.getInt(yIndex);
						pos.setY(y);
						long blockSeed = Permuter.permute(columnSeed, y);
						for (int featureIndex = 0, featureCount = features.length; featureIndex < featureCount; featureIndex++) {
							permuter.setSeed(Permuter.permute(blockSeed, featureIndex));
							features[featureIndex].generate(world, this, permuter, pos);
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
			ConfiguredFeature<?, ?>[] features = decorator.getSortedFeatures(world);
			if (features.length != 0) {
				this.profiler.run(decorator.key.id(), () -> {
					long columnSeed = permuter.getSeed();
					pos.setY(yLevel);
					long blockSeed = Permuter.permute(columnSeed, yLevel);
					for (int featureIndex = 0, featureCount = features.length; featureIndex < featureCount; featureIndex++) {
						permuter.setSeed(Permuter.permute(blockSeed, featureIndex));
						features[featureIndex].generate(world, this, permuter, pos);
					}
					permuter.setSeed(columnSeed);
				});
			}
		}
	}

	public void generateRockLayers(LinkedRockLayerConfig<?>[] rockLayers, Chunk chunk, int minHeight, int maxHeight, ChunkOfColumns<? extends WorldColumn> columns, boolean early) {
		this.profiler.run("Rock layers", () -> {
			for (LinkedRockLayerConfig<?> rock : rockLayers) {
				if (rock.group.generate_before_ores == early) {
					this.profiler.run(rock.name, () -> {
						RockLayerReplacer.generateNew(this.seed, chunk, columns, minHeight, maxHeight, rock);
					});
				}
			}
		});
	}

	@Override
	public void setStructureStarts(
		DynamicRegistryManager registryManager,
		StructurePlacementCalculator placementCalculator,
		StructureAccessor structureAccessor,
		Chunk chunk,
		StructureTemplateManager structureTemplateManager
	) {
		if (
			BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipStructures &&
			DistantHorizonsCompat.isOnDistantHorizonThread()
		) {
			return;
		}
		this.profiler.run("setStructureStarts", () -> {
			this.actuallySetStructureStarts(
				registryManager,
				placementCalculator,
				structureAccessor,
				chunk,
				structureTemplateManager
			);
		});
	}

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
		StructureStart existingStart = structureAccessor.getStructureStart(sectionPos, structure, chunk);
		int references = existingStart != null ? existingStart.getReferences() : 0;
		Predicate<RegistryEntry<Biome>> predicate = force ? Predicates.alwaysTrue() : structure.getValidBiomes()::contains;
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
				)
			)
		) {
			structureAccessor.setStructureStart(sectionPos, structure, newStart, chunk);
			return true;
		}
		return false;
	}

	public boolean canStructureSpawn(RegistryEntry<Structure> entry, StructureStart start, Permuter permuter) {
		return true;
	}

	@SuppressWarnings("deprecation")
	public static void moveStructure(StructureStart start, int dx, int dy, int dz) {
		start.getBoundingBox().move(dx, dy, dz);
		for (StructurePiece child : start.getChildren()) {
			child.translate(dx, dy, dz);
		}
	}

	public void generateStructuresInStage(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, GenerationStep.Feature step) {
		if (WORLD_SLICES && (chunk.getPos().x & 3) != 0) return;

		for (
			RegistryEntry<Structure> structure
		:
			this.sortedStructures.computeIfAbsent(
				step,
				step_ -> (
					world
					.getRegistryManager()
					.get(RegistryKeys.STRUCTURE)
					.streamEntries()
					.filter(structure -> structure.value().getFeatureGenerationStep() == step_)
					.toArray(REGISTRY_ENTRY_ARRAY_FACTORY.generic())
				)
			)
		) {
			//important: make sure this seed does NOT use chunkPos as part of the hash!
			//the same StructureStart should use the same seed for every chunk that it intersects with.
			//this fixes shipwrecks spawning at different heights or with
			//different wood types when they spawn intersecting a chunk border.
			long seed = Permuter.permute(world.getSeed() ^ 0x5E4FE744EECE1D5BL, UnregisteredObjectException.getID(structure));
			this.generateStructure(world, chunk, structureAccessor, seed, structure.value());
		}
	}

	public void generateStructure(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, long seed, Structure structure) {
		List<StructureStart> starts = structureAccessor.getStructureStarts(ChunkSectionPos.from(chunk), structure);
		for (int startIndex = 0, size = starts.size(); startIndex < size; startIndex++) {
			StructureStart start = starts.get(startIndex);
			try {
				world.setCurrentlyGeneratingStructureName(() -> {
					RegistryKey<Structure> key = world.getRegistryManager().get(RegistryKeys.STRUCTURE).getKey(structure).orElse(null);
					return (key != null ? key : structure).toString();
				});
				long startSeed = Permuter.permute(seed, start.getPos());
				start.place(
					world,
					structureAccessor,
					this,
					new MojangPermuter(startSeed),
					WorldUtil.chunkBox(chunk),
					chunk.getPos()
				);
			}
			catch (Exception exception) {
				CrashReport report = CrashReport.create(exception, "Structure placement");

				report
				.addElement("Structure being placed")
				.add("ID", () -> String.valueOf(world.getRegistryManager().get(RegistryKeys.STRUCTURE).getId(structure)))
				.add("Description", () -> String.valueOf(structure))
				.add("Start", () -> String.valueOf(start))
				.add("Start position", () -> String.valueOf(start.getPos()));

				report
				.addElement("World and location")
				.add("World", () -> String.valueOf(world))
				.add("Chunk", () -> String.valueOf(chunk))
				.add("Chunk position", () -> String.valueOf(chunk.getPos()))
				.add("Seed", seed);

				throw new CrashException(report);
			}
			finally {
				world.setCurrentlyGeneratingStructureName(null);
			}
		}
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
				List<StructurePlacement> placements = this.world.getChunkManager().getStructurePlacementCalculator().getPlacements(structure);
				for (StructurePlacement placement : placements) {
					if (placement instanceof ConcentricRingsStructurePlacement concentric) {
						List<ChunkPos> positions = this.world.getChunkManager().getStructurePlacementCalculator().getPlacementPositions(concentric);
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

		public boolean update(int chunkX, int chunkZ) {
			if (this.structure == null) {
				for (RegistryEntry<Structure> structure : this.structures) {
					if (this.canPossiblyGenerate(chunkX, chunkZ, structure)) {
						//match logic from ChunkGenerator.locateStructure().
						StructurePresence presence = this.structureAccessor.getStructurePresence(new ChunkPos(chunkX, chunkZ), structure.value(), this.skipReferencedStructures);
						if (presence == StructurePresence.START_NOT_PRESENT) continue;
						if (!this.skipReferencedStructures && presence == StructurePresence.START_PRESENT) {
							this.chunkX = chunkX;
							this.chunkZ = chunkZ;
							this.structure = structure;
							break;
						}
						Chunk chunk = this.world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS);
						StructureStart start = this.structureAccessor.getStructureStart(ChunkSectionPos.from(chunk), structure.value(), chunk);
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
		if (finder.tryStrongholds(centerChunkX, centerChunkZ)) return finder.toPair();
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
		return null;
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos blockPos) {
		this.bigglobe_appendText(text, this.column(blockPos.getX(), blockPos.getZ()), blockPos.getY());
	}

	public static @Nullable PlayerEntity getClientPlayer() {
		return switch (FabricLoader.getInstance().getEnvironmentType()) {
			case CLIENT -> getClientPlayer0();
			case SERVER -> null;
		};
	}

	@Environment(EnvType.CLIENT)
	public static PlayerEntity getClientPlayer0() {
		return MinecraftClient.getInstance().player;
	}

	@Override
	public ColumnValue<?>[] bigglobe_getDisplayedColumnValues() {
		return this.displayedColumnValues;
	}

	@Override
	public void bigglobe_setDisplayedColumnValues(ColumnValue<?>[] displayedColumnValues) {
		this.displayedColumnValues = displayedColumnValues;
	}
}