package builderb0y.bigglobe.chunkgen;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.util.math.MathHelper;
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
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;

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
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.RootLayer;
import builderb0y.bigglobe.chunkgen.scripted.SegmentList.Segment;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.columns.ChunkOfBiomeColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.features.FeatureDispatcher.DualFeatureDispatcher;
import builderb0y.bigglobe.mixins.Heightmap_StorageAccess;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.util.Async;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.WorldOrChunk.ChunkDelegator;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.util.CollectionTransformer;

@AddPseudoField("biome_source")
@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeScriptedChunkGenerator extends ChunkGenerator {

	public static final AutoCoder<BigGlobeScriptedChunkGenerator> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(BigGlobeScriptedChunkGenerator.class);
	public static final Codec<BigGlobeScriptedChunkGenerator> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(CODER);

	public final @VerifyNullable String reload_dimension;
	public final @EncodeInline ColumnEntryRegistry columnEntryRegistry;
	public static record Height(@VerifyDivisibleBy16 int min_y, @VerifyDivisibleBy16 @VerifySorted(greaterThan = "min_y") int max_y, int sea_level) {}
	public final Height height;
	public final RootLayer layer;
	public final DualFeatureDispatcher feature_dispatcher;
	/**
	seed is the first 64 bits of the SHA256 hash of actualWorldSeed.
	seed is used for {@link ScriptedColumn#seed}, because column
	entries are synced to the client for grass coloration purposes.
	it is important that the client not be able to reverse-engineer
	the actual world seed from the hashed seed sent to it.
	*/
	public transient long seed, actualWorldSeed;
	public DisplayEntry[] debugDisplay = new DisplayEntry[0];

	public BigGlobeScriptedChunkGenerator(
		#if MC_VERSION == MC_1_19_2
			BetterRegistry<StructureSet> structureSetRegistry,
		#endif
		@VerifyNullable String reload_dimension,
		Height height,
		RootLayer layer,
		DualFeatureDispatcher feature_dispatcher,
		BiomeSource biome_source
	) {
		super(
			#if (MC_VERSION == MC_1_19_2)
				((BetterHardCodedRegistry<StructureSet>)(structureSetRegistry)).registry,
				Optional.empty(),
			#endif
			biome_source
		);
		this.reload_dimension = reload_dimension;
		this.columnEntryRegistry = ColumnEntryRegistry.Loading.get().getRegistry();
		this.height = height;
		this.layer = layer;
		this.feature_dispatcher = feature_dispatcher;
	}

	public BiomeSource biome_source() {
		return this.biomeSource;
	}

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
		};
	}

	public ScriptedColumn newColumn(HeightLimitView world, int x, int z, boolean distantHorizons) {
		return this.columnEntryRegistry.columnFactory.create(
			this.seed,
			x,
			z,
			world.getBottomY(),
			world.getTopY(),
			distantHorizons
		);
	}

	public void setSeed(long seed) {
		this.actualWorldSeed = seed;
		//make it impossible to reverse-engineer the seed from information sent to the client.
		this.seed = Hashing.sha256().hashLong(seed).asLong();
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
	public Codec<? extends ChunkGenerator> getCodec() {
		return CODEC;
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
		return CompletableFuture.runAsync(
			() -> {
				int startX = chunk.getPos().getStartX();
				int startZ = chunk.getPos().getStartZ();
				int minY = chunk.getBottomY();
				int maxY = chunk.getTopY();
				ScriptedColumn[] columns = new ScriptedColumn[256];
				BlockSegmentList[] lists = new BlockSegmentList[256];
				try (AsyncRunner async = new AsyncRunner()) {
					for (int offsetZ = 0; offsetZ < 16; offsetZ += 2) {
						final int offsetZ_ = offsetZ;
						for (int offsetX = 0; offsetX < 16; offsetX += 2) {
							final int offsetX_ = offsetX;
							async.submit(() -> {
								int quadX = startX | offsetX_;
								int quadZ = startZ | offsetZ_;
								ScriptedColumn
									column00 = this.newColumn(chunk, quadX,     quadZ,     distantHorizons),
									column01 = this.newColumn(chunk, quadX | 1, quadZ,     distantHorizons),
									column10 = this.newColumn(chunk, quadX,     quadZ | 1, distantHorizons),
									column11 = this.newColumn(chunk, quadX | 1, quadZ | 1, distantHorizons);
								BlockSegmentList
									list00 = new BlockSegmentList(minY, maxY),
									list01 = new BlockSegmentList(minY, maxY),
									list10 = new BlockSegmentList(minY, maxY),
									list11 = new BlockSegmentList(minY, maxY);
								this.layer.emitSegments(column00, column01, column10, column11, list00);
								this.layer.emitSegments(column01, column00, column11, column10, list01);
								this.layer.emitSegments(column10, column11, column00, column01, list10);
								this.layer.emitSegments(column11, column10, column01, column00, list11);
								int baseIndex = (offsetZ_ << 4) | offsetX_;
								columns[baseIndex     ] = column00;
								columns[baseIndex ^  1] = column01;
								columns[baseIndex ^ 16] = column10;
								columns[baseIndex ^ 17] = column11;
								lists  [baseIndex     ] = list00;
								lists  [baseIndex ^  1] = list01;
								lists  [baseIndex ^ 16] = list10;
								lists  [baseIndex ^ 17] = list11;
							});
						}
					}
				}
				Async.loop(chunk.getBottomSectionCoord(), chunk.getTopSectionCoord(), 1, (int coord) -> {
					ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(coord));
					int baseY = coord << 4;
					SectionGenerationContext context = new SectionGenerationContext(chunk, section, baseY, this.seed, null);
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
					context.recalculateCounts();
				});
				for (Heightmap.Type type : chunk.getStatus().getHeightmapTypes()) {
					Heightmap heightmap = chunk.getHeightmap(type);
					@SuppressWarnings("CastToIncompatibleInterface")
					PaletteStorage heightmapStorage = ((Heightmap_StorageAccess)(heightmap)).bigglobe_getStorage();
					for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
						BlockSegmentList list = lists[horizontalIndex];
						if (!list.isEmpty()) {
							int height = list.get(list.size() - 1).maxY + 1; //convert to exclusive.
							height = MathHelper.clamp(height - chunk.getBottomY(), 0, chunk.getHeight());
							heightmapStorage.set(horizontalIndex, height);
						}
					}
				}
				WorldWrapper worldWrapper = new WorldWrapper(
					new ChunkDelegator(chunk, this.actualWorldSeed),
					this,
					new Permuter(0L),
					new Coordination(
						SymmetricOffset.IDENTITY,
						WorldUtil.chunkBox(chunk),
						WorldUtil.chunkBox(chunk)
					),
					distantHorizons
				);
				worldWrapper.columns = new Long2ObjectOpenHashMap<>(256);
				for (ScriptedColumn column : columns) {
					worldWrapper.columns.put(ColumnPos.pack(column.x, column.z), column);
				}
				this.feature_dispatcher.raw.generate(worldWrapper);
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
		this.feature_dispatcher.normal.generate(
			new WorldWrapper(
				new WorldDelegator(world),
				this,
				new Permuter(0L),
				new Coordination(
					SymmetricOffset.IDENTITY,
					WorldUtil.chunkBox(chunk),
					WorldUtil.surroundingChunkBox(chunk)
				),
				DistantHorizonsCompat.isOnDistantHorizonThread()
			)
		);
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
				ScriptedColumn column = this.newColumn(chunk, 0, 0, distantHorizons);
				for (int z = 0; z < 16; z += 4) {
					for (int x = 0; x < 16; x += 4) {
						column.setPos(chunk.getPos().getStartX() | x, chunk.getPos().getStartZ() | z);
						for (int y = bottomY; y < topY; y += 4) {
							ChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
							PalettedContainer<RegistryEntry<Biome>> container = (PalettedContainer<RegistryEntry<Biome>>)(section.getBiomeContainer());
							int newID = SectionUtil.id(container, source.script.get(column, y).entry());
							SectionUtil.storage(container).set((((y >>> 2) & 3) << 4) | z | (x >>> 2), newID);
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
		return this.height.sea_level;
	}

	@Override
	public int getMinimumY() {
		return this.height.min_y;
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		ScriptedColumn column = this.columnEntryRegistry.columnFactory.create(this.seed, x, z, world.getBottomY(), world.getTopY(), distantHorizons);
		BlockSegmentList list = new BlockSegmentList(world.getBottomY(), world.getTopY());
		this.layer.emitSegments(column, list);
		for (int index = list.size(); --index >= 0;) {
			Segment<BlockState> segment = list.get(index);
			if (heightmap.getBlockPredicate().test(segment.value)) {
				return segment.maxY + 1;
			}
		}
		return world.getBottomY();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		ScriptedColumn column = this.columnEntryRegistry.columnFactory.create(this.seed, x, z, world.getBottomY(), world.getTopY(), distantHorizons);
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
		ScriptedColumn column = this.columnEntryRegistry.columnFactory.create(this.seed, pos.getX(), pos.getZ(), this.height.min_y, this.height.max_y, false);
		for (DisplayEntry entry : this.debugDisplay) {
			try {
				text.add(entry.id + ": " + entry.handle.invokeExact(column, pos.getY()));
			}
			catch (Throwable exception) {
				text.add(entry.id + ": " + exception);
			}
		}
	}

	public void setDisplay(String regex) {
		if (regex == null) {
			this.debugDisplay = new DisplayEntry[0];
			return;
		}
		Pattern pattern = Pattern.compile(regex);
		List<DisplayEntry> displayEntries = new ArrayList<>();
		MethodHandles.Lookup lookup = this.columnEntryRegistry.columnLookup;
		for (Map.Entry<Identifier, ColumnEntryMemory> entry : (Iterable<? extends Map.Entry<Identifier, ColumnEntryMemory>>)(this.columnEntryRegistry.filteredMemories.stream().map(memory -> Map.entry(memory.getTyped(ColumnEntryMemory.ACCESSOR_ID), memory)).sorted(Map.Entry.comparingByKey())::iterator)) {
			if (pattern.matcher(entry.getKey().toString()).find()) {
				MethodCompileContext getter = entry.getValue().getTyped(ColumnEntryMemory.GETTER);
				if (getter.clazz == this.columnEntryRegistry.columnContext.mainClass) try {
					MethodHandle handle = lookup.findVirtual(
						this.columnEntryRegistry.columnClass,
						getter.info.name,
						MethodType.methodType(
							getter.info.returnType.toClass(this.columnEntryRegistry.loader),
							CollectionTransformer.convertArray(
								getter.info.paramTypes,
								Class<?>[]::new,
								(TypeInfo info) -> info.toClass(this.columnEntryRegistry.loader)
							)
						)
					);
					if (handle.type().parameterCount() < 2) {
						handle = MethodHandles.dropArguments(handle, 1, int.class);
					}
					//primitive -> Object, because doing primitive -> String would
					//require special-casing every primitive type, and I am lazy.
					handle = handle.asType(MethodType.methodType(Object.class, ScriptedColumn.class, int.class));
					displayEntries.add(new DisplayEntry(entry.getKey(), handle));
				}
				catch (Throwable throwable) {
					BigGlobeMod.LOGGER.error("An unknown error occurred while trying to set the display for the active chunk generator: ", throwable);
				}
			}
		}
		this.debugDisplay = displayEntries.toArray(new DisplayEntry[displayEntries.size()]);
	}

	public static record DisplayEntry(Identifier id, MethodHandle handle) {}
}