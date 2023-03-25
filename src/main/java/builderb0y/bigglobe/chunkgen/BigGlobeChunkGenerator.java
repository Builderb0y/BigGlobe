package builderb0y.bigglobe.chunkgen;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.google.gson.JsonElement;
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
import net.minecraft.util.registry.*;
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
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.decoders.RecordDecoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.ColumnValue.CustomDisplayContext;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.features.BigGlobeFeatures;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.features.UseScriptTemplateFeature;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixins.ChunkGenerator_getStructurePlacementAccess;
import builderb0y.bigglobe.mixins.Heightmap_StorageAccess;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.registration.BigGlobeBuiltinRegistries;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.WorldgenProfiler;
import builderb0y.scripting.parsing.ScriptParsingException;

@AddPseudoField(name = "structureSetRegistry", getter = "getStructureSetRegistry")
public abstract class BigGlobeChunkGenerator extends ChunkGenerator {

	public static final boolean WORLD_SLICES = false;
	public static final boolean RELOAD_GENERATORS = Boolean.getBoolean(BigGlobeMod.MODID + ".reloadGenerators");
	static {
		if (RELOAD_GENERATORS) {
			BigGlobeMod.LOGGER.error("Warning! -D" + BigGlobeMod.MODID + ".reloadGenerators is set to true in your java arguments!");
			BigGlobeMod.LOGGER.error("This will load chunk generators from the mod jar file instead of from data packs.");
			BigGlobeMod.LOGGER.error("If you want to tweak " + BigGlobeMod.MODNAME + "'s chunk generators with a data pack,");
			BigGlobeMod.LOGGER.error("you will need to remove this option from your java arguments first.");
		}
	}
	public static final GenerationStep.Feature[] FEATURE_STEPS = GenerationStep.Feature.values();
	public static final ObjectArrayFactory<RegistryEntry<?>> REGISTRY_ENTRY_ARRAY_FACTORY = new ObjectArrayFactory<>(RegistryEntry.class).generic();

	@EncodeInline
	public final Registry<ConfiguredFeature<?, ?>> configuredFeatureRegistry;
	public transient ColumnValue<?>[] displayedColumnValues;

	public transient long seed;
	//no idea if this needs to be synchronized or not, but it can't hurt.
	public final transient Map<GenerationStep.Feature, RegistryEntry<Structure>[]> sortedStructures = Collections.synchronizedMap(new EnumMap<>(GenerationStep.Feature.class));
	public final transient WorldgenProfiler profiler = new WorldgenProfiler();

	public BigGlobeChunkGenerator(
		Registry<StructureSet> structureSetRegistry,
		Registry<ConfiguredFeature<?, ?>> configuredFeatureRegistry,
		Optional<RegistryEntryList<StructureSet>> structureOverrides,
		BiomeSource biomeSource
	) {
		super(structureSetRegistry, structureOverrides, biomeSource);
		this.configuredFeatureRegistry = configuredFeatureRegistry;
		for (UseScriptTemplateFeature.Config config : SortedFeatures.extractOneFeature(configuredFeatureRegistry, BigGlobeFeatures.USE_SCRIPT_TEMPLATE)) {
			try {
				config.getCompiledScript();
			}
			catch (ScriptParsingException exception) {
				throw new RuntimeException(exception.getLocalizedMessage(), exception);
			}
		}
	}

	public static class SortedFeatures {

		public final Map<Feature<?>, List<FeatureConfig>> map;

		public SortedFeatures(Registry<ConfiguredFeature<?, ?>> registry) {
			Map<Feature<?>, List<FeatureConfig>> map = new HashMap<>(Registry.FEATURE.size());
			for (ConfiguredFeature<?, ?> configuredFeature : registry) {
				map.computeIfAbsent(configuredFeature.feature(), $ -> new ArrayList<>(4)).add(configuredFeature.config());
			}
			this.map = map;
		}

		@SuppressWarnings("unchecked")
		public <C extends FeatureConfig> List<C> get(Feature<C> feature) {
			return (List<C>)(this.map.getOrDefault(feature, Collections.emptyList()));
		}

		@SuppressWarnings("unchecked")
		public static <C extends FeatureConfig> List<C> extractOneFeature(Registry<ConfiguredFeature<?, ?>> registry, Feature<C> feature) {
			List<C> list = new ArrayList<>(8);
			for (ConfiguredFeature<?, ?> configuredFeature : registry) {
				if (configuredFeature.feature() == feature) {
					list.add((C)(configuredFeature.config()));
				}
			}
			return list;
		}
	}

	@EncodeInline
	public Registry<StructureSet> getStructureSetRegistry() {
		return this.structureSetRegistry;
	}

	public static <G extends BigGlobeChunkGenerator> AutoCoder<G> createCoder(FactoryContext<G> context, String preset, String dimensionName) {
		AutoCoder<G> coder = (AutoCoder<G>)(context.forceCreateDecoder(RecordDecoder.Factory.INSTANCE));
		if (!RELOAD_GENERATORS) return coder;
		return new AutoCoder<>() {

			@Override
			public <T_Encoded> @Nullable G decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
				JsonElement json = BigGlobeBuiltinRegistries.getDimension(preset, dimensionName);
				T_Encoded encoded = JsonOps.INSTANCE.convertTo(context.ops, json);
				return context.autoCodec.decode(coder, encoded, context.ops);
			}

			@Override
			public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, G> context) throws EncodeException {
				return context.encodeWith(coder);
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
	public void computeStructurePlacementsIfNeeded(NoiseConfig noiseConfig) {
		//this is called from ServerWorld.<init>(), so very early.
		this.setSeed(noiseConfig.getLegacyWorldSeed());
		super.computeStructurePlacementsIfNeeded(noiseConfig);
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

	@Override
	public void setStructureStarts(DynamicRegistryManager registryManager, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk, StructureTemplateManager structureTemplateManager, long seed) {
		if (
			BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipStructures &&
			DistantHorizonsCompat.isOnDistantHorizonThread()
		) {
			return;
		}
		this.profiler.run("setStructureStarts", () -> {
			this.actuallySetStructureStarts(registryManager, noiseConfig, structureAccessor, chunk, structureTemplateManager, seed);
		});
	}

	public void actuallySetStructureStarts(DynamicRegistryManager registryManager, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk, StructureTemplateManager structureTemplateManager, long seed) {
		super.setStructureStarts(registryManager, noiseConfig, structureAccessor, chunk, structureTemplateManager, seed);
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
							//String.hashCode() will be cached, which means faster permutation times.
							weightedEntry.structure().getKey().orElseThrow().getValue().hashCode()
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
					.get(Registry.STRUCTURE_KEY)
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
			long seed = Permuter.permute(world.getSeed() ^ 0x5E4FE744EECE1D5BL, structure.getKey().orElseThrow().getValue());
			this.generateStructure(world, chunk, structureAccessor, seed, structure.value());
		}
	}

	public void generateStructure(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, long seed, Structure structure) {
		List<StructureStart> starts = structureAccessor.getStructureStarts(ChunkSectionPos.from(chunk), structure);
		for (int startIndex = 0, size = starts.size(); startIndex < size; startIndex++) {
			StructureStart start = starts.get(startIndex);
			try {
				world.setCurrentlyGeneratingStructureName(() -> {
					RegistryKey<Structure> key = world.getRegistryManager().get(Registry.STRUCTURE_KEY).getKey(structure).orElse(null);
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
				.add("ID", () -> String.valueOf(world.getRegistryManager().get(Registry.STRUCTURE_KEY).getId(structure)))
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
				List<StructurePlacement> placements = ((ChunkGenerator_getStructurePlacementAccess)(BigGlobeChunkGenerator.this)).bigglobe_getStructurePlacement(structure, this.noiseConfig);
				for (StructurePlacement placement : placements) {
					if (placement instanceof ConcentricRingsStructurePlacement concentric) {
						List<ChunkPos> positions = BigGlobeChunkGenerator.this.getConcentricRingsStartChunks(concentric, this.noiseConfig);
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
			List<StructurePlacement> placements = ((ChunkGenerator_getStructurePlacementAccess)(BigGlobeChunkGenerator.this)).bigglobe_getStructurePlacement(structure, this.noiseConfig);
			for (StructurePlacement placement : placements) {
				if (placement.shouldGenerate(BigGlobeChunkGenerator.this, this.noiseConfig, this.world.getSeed(), chunkX, chunkZ)) {
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
		WorldColumn column = this.column(blockPos.getX(), blockPos.getZ());
		PlayerEntity player = getClientPlayer();
		CustomDisplayContext context = player == null ? null : new CustomDisplayContext(player, column, blockPos.getY());
		if (this.displayedColumnValues == null) {
			this.displayedColumnValues = (
				ColumnValue
				.REGISTRY
				.stream()
				.filter(value -> value.accepts(column))
				.sorted(Comparator.comparing(ColumnValue::getName))
				.toArray(ColumnValue.ARRAY_FACTORY)
			);
		}
		text.add("Tip: use /bigglobe:filterF3 to filter the following information:");
		for (ColumnValue<?> value : this.displayedColumnValues) {
			text.add(
				value == null
				? ""
				: context != null
				? value.getName() + ": " + value.getDisplayText(context)
				: value.getName() + ": " + CustomDisplayContext.format(value.getValue(column, blockPos.getY()))
			);
		}
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
}