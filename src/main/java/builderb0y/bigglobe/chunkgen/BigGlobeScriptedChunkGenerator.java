package builderb0y.bigglobe.chunkgen;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
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
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.RootLayer;
import builderb0y.bigglobe.chunkgen.scripted.SegmentList.Segment;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.mixins.Heightmap_StorageAccess;
import builderb0y.bigglobe.util.Async;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.parsing.ScriptParsingException;

@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeScriptedChunkGenerator extends ChunkGenerator {

	public static final AutoCoder<BigGlobeScriptedChunkGenerator> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(BigGlobeScriptedChunkGenerator.class);
	public static final Codec<BigGlobeScriptedChunkGenerator> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(CODER);

	public final @VerifyNullable String reload_dimension;
	public final @EncodeInline ColumnEntryRegistry columnEntryRegistry;
	public final RootLayer layer;
	public final BetterRegistry<Biome> biomeRegistry;
	public static record Height(int min_y, int max_y, int sea_level) {}
	public final Height height;
	public transient long seed;

	public BigGlobeScriptedChunkGenerator(
		#if MC_VERSION == MC_1_19_2
			BetterRegistry<StructureSet> structureSetRegistry,
		#endif
		@VerifyNullable String reload_dimension,
		BetterRegistry<Biome> biomeRegistry,
		ColumnEntryRegistry columnEntryRegistry,
		RootLayer layer,
		Height height
	)
	throws ScriptParsingException {
		super(
			#if (MC_VERSION == MC_1_19_2)
				((BetterHardCodedRegistry<StructureSet>)(structureSetRegistry)).registry,
				Optional.empty(),
			#endif
			new FixedBiomeSource(biomeRegistry.getOrCreateEntry(BiomeKeys.PLAINS))
		);
		this.reload_dimension = reload_dimension;
		this.biomeRegistry = biomeRegistry;
		this.columnEntryRegistry = columnEntryRegistry;
		this.layer = layer;
		this.height = height;
		layer.compile(columnEntryRegistry);
	}

	public static void init() {
		Registry.register(RegistryVersions.chunkGenerator(), BigGlobeMod.modID("scripted"), CODEC);
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
					T_Encoded encoded = JsonOps.INSTANCE.convertTo(context.ops, json);
					return context.input(encoded, RootDecodePath.INSTANCE).decodeWith(coder);
				}
				else {
					return context.decodeWith(coder);
				}
			}

			@Override
			public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, BigGlobeScriptedChunkGenerator> context) throws EncodeException {
				return context.encodeWith(coder);
			}
			public JsonElement getDimension(String dimension) {
				BigGlobeMod.LOGGER.info("Reading " + dimension + " chunk generator from mod jar.");
				return (
					this
					.getJson("/data/bigglobe/worldgen/world_preset/bigglobe.json")
					.getAsJsonObject()
					.getAsJsonObject("dimensions")
					.getAsJsonObject(dimension)
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
		return CompletableFuture.runAsync(
			() -> {
				int startX = chunk.getPos().getStartX();
				int startZ = chunk.getPos().getStartZ();
				int minY = chunk.getBottomY();
				int maxY = chunk.getTopY();
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
									column00 = this.columnEntryRegistry.columnFactory.create(this.seed, quadX,     quadZ,     minY, maxY),
									column01 = this.columnEntryRegistry.columnFactory.create(this.seed, quadX | 1, quadZ,     minY, maxY),
									column10 = this.columnEntryRegistry.columnFactory.create(this.seed, quadX,     quadZ | 1, minY, maxY),
									column11 = this.columnEntryRegistry.columnFactory.create(this.seed, quadX | 1, quadZ | 1, minY, maxY);
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
								lists[baseIndex     ] = list00;
								lists[baseIndex ^  1] = list01;
								lists[baseIndex ^ 16] = list10;
								lists[baseIndex ^ 17] = list11;
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
		ScriptedColumn column = this.columnEntryRegistry.columnFactory.create(this.seed, x, z, world.getBottomY(), world.getTopY());
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
		ScriptedColumn column = this.columnEntryRegistry.columnFactory.create(this.seed, x, z, world.getBottomY(), world.getTopY());
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

	}
}