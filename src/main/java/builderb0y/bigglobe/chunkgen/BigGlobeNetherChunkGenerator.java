package builderb0y.bigglobe.chunkgen;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.block.BlockState;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Util;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.PositionCache.NetherPositionCache;
import builderb0y.bigglobe.chunkgen.PositionCache.PositionCacheHolder;
import builderb0y.bigglobe.chunkgen.perSection.BedrockReplacer;
import builderb0y.bigglobe.chunkgen.perSection.OreReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.NetherColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.features.BigGlobeFeatures;
import builderb0y.bigglobe.features.ores.NetherOreFeature;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixins.StructureStart_BoundingBoxSetter;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ScriptStructureOverrider;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.overriders.nether.NoiseOverrider;
import builderb0y.bigglobe.scripting.ColumnYRandomToDoubleScript.Holder;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.settings.NetherSettings;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.settings.NetherSettings.NetherSurfaceSettings;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.NetherPillarStructure;
import builderb0y.bigglobe.structures.RawGenerationStructure;
import builderb0y.bigglobe.util.SemiThreadLocal;

@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeNetherChunkGenerator extends BigGlobeChunkGenerator {

	public static final int
		LOWER_BEDROCK_AMOUNT = 16,
		UPPER_BEDROCK_AMOUNT = 16;

	public static final AutoCoder<BigGlobeNetherChunkGenerator> NETHER_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(BigGlobeNetherChunkGenerator.class);
	public static final Codec<BigGlobeNetherChunkGenerator> NETHER_CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(NETHER_CODER);

	@EncodeInline
	public final NetherSettings settings;
	public transient SemiThreadLocal<ChunkOfColumns<NetherColumn>> chunkColumnCache;

	public final transient ScriptStructureOverrider.Holder[] structureOverriders;
	public final transient NoiseOverrider.Holder[] caveOverriders, cavernOverriders;
	public final transient NetherOreFeature.Config[] ores;

	public BigGlobeNetherChunkGenerator(
		NetherSettings settings,
		Registry<StructureSet> structureSetRegistry,
		SortedFeatures configuredFeatures
	) {
		super(
			structureSetRegistry,
			configuredFeatures,
			Optional.empty(),
			new ColumnBiomeSource(
				settings.local_settings().elements.stream().map(LocalNetherSettings::biome)
			)
		);
		this.settings = settings;
		this.structureOverriders = configuredFeatures.streamConfigs(BigGlobeFeatures.NETHER_STRUCTURE_OVERRIDER).map(config -> config.script).toArray(ScriptStructureOverrider.Holder[]::new);
		this.caveOverriders = configuredFeatures.streamConfigs(BigGlobeFeatures.NETHER_CAVE_OVERRIDER).map(config -> config.script).toArray(NoiseOverrider.Holder[]::new);
		this.cavernOverriders = configuredFeatures.streamConfigs(BigGlobeFeatures.NETHER_CAVERN_OVERRIDER).map(config -> config.script).toArray(NoiseOverrider.Holder[]::new);
		this.ores = configuredFeatures.streamConfigs(BigGlobeFeatures.NETHER_ORE).toArray(NetherOreFeature.Config[]::new);
	}

	public static void init() {
		Registry.register(Registry.CHUNK_GENERATOR, BigGlobeMod.modID("nether"), NETHER_CODEC);
	}

	public static AutoCoder<BigGlobeNetherChunkGenerator> createCoder(FactoryContext<BigGlobeNetherChunkGenerator> context) {
		return BigGlobeChunkGenerator.createCoder(context, "bigglobe", "the_nether");
	}

	@Override
	public void setSeed(long seed) {
		super.setSeed(seed);
		this.chunkColumnCache = SemiThreadLocal.weak(4, () -> {
			return new ChunkOfColumns<>(NetherColumn[]::new, this::column);
		});
	}

	@Override
	public NetherColumn column(int x, int z) {
		return new NetherColumn(this.settings, this.seed, x, z);
	}

	public void generateRawSections(Chunk chunk, ChunkOfColumns<NetherColumn> columns, ScriptStructures structures, boolean distantHorizons) {
		this.generateSectionsParallel(chunk, this.settings.min_y(), this.settings.max_y(), columns, context -> {
			BlockState previousFiller = null;
			BlockState previousFluid  = null;
			int fillerID = -1;
			int fluidID  = -1;
			PaletteStorage storage = context.storage();

			int startY = context.startY();
			for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
				NetherColumn column = columns.getColumn(horizontalIndex);
				double[] caveNoise = column.getCaveNoise();
				int caveLowerY = column.settings.min_y();
				double[] cavernNoise = column.getCavernNoise();
				LocalNetherSettings localSettings = column.getLocalCell().settings;
				int cavernLowerY = localSettings.caverns().min_y();
				int cavernUpperY = localSettings.caverns().max_y();
				ColumnYToDoubleScript.Holder widthScript = localSettings.caves().width();
				int lavaLevel = column.getLocalCell().lavaLevel;
				if (localSettings.filler() != previousFiller || localSettings.fluid_state() != previousFluid) {
					previousFiller = localSettings.filler();
					previousFluid = localSettings.fluid_state();
					fillerID = context.id(previousFiller);
					fluidID = context.id(previousFluid);
					if (storage != (storage = context.storage())) { //resize
						fillerID = context.id(previousFiller);
						fluidID = context.id(previousFluid);
						assert storage == context.storage();
					}
				}

				for (int verticalIndex = 0; verticalIndex < 16; verticalIndex++) {
					int y = startY | verticalIndex;
					double caveWidth = widthScript.evaluate(column, y);
					int index = horizontalIndex | (verticalIndex << 8);
					if (
						caveNoise[y - caveLowerY] < caveWidth * caveWidth || (
							y >= cavernLowerY &&
							y <  cavernUpperY &&
							cavernNoise[y - cavernLowerY] < 0.0D
						)
					) {
						if (y < lavaLevel) {
							storage.set(index, fluidID);
							context.addLight(index);
						}
					}
					else {
						storage.set(index, fillerID);
					}
				}
			}
		});
		if (!distantHorizons) {
			//todo: early rock layers
			this.profiler.run("ores", () -> {
				this.generateSectionsParallelSimple(chunk, this.settings.min_y(), this.settings.max_y(), columns, context -> {
					OreReplacer.generate(context, columns, this.ores);
				});
			});
			//todo: late rock layers
		}
	}

	public void generateSurface(Chunk chunk, ChunkOfColumns<NetherColumn> columns) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		Permuter permuter = new Permuter(0L);
		long chunkSeed = Permuter.permute(this.seed ^ 0x8B9B939B4728BD6EL, chunk.getPos());
		for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
			long columnSeed = Permuter.permute(chunkSeed, horizontalIndex);
			pos.setX(horizontalIndex & 15).setZ(horizontalIndex >>> 4);
			NetherColumn column = columns.getColumn(horizontalIndex);
			LocalNetherSettings localSettings = column.getLocalCell().settings;
			this.generateSurface(
				chunk,
				column,
				pos,
				permuter,
				columnSeed,
				localSettings.caverns().floor_surface(),
				-1,
				column.cavernFloors
			);
			this.generateSurface(
				chunk,
				column,
				pos,
				permuter,
				columnSeed,
				localSettings.caverns().ceiling_surface(),
				1,
				column.cavernCeilings
			);
			this.generateSurface(
				chunk,
				column,
				pos,
				permuter,
				columnSeed,
				localSettings.caves().floor_surface(),
				-1,
				column.caveFloors
			);
			this.generateSurface(
				chunk,
				column,
				pos,
				permuter,
				columnSeed,
				localSettings.caves().ceiling_surface(),
				1,
				column.caveCeilings
			);
		}
	}

	public void generateSurface(
		Chunk chunk,
		NetherColumn column,
		BlockPos.Mutable pos,
		Permuter permuter,
		long columnSeed,
		NetherSurfaceSettings surface,
		int delta,
		IntList yLevels
	) {
		if (surface == null) return;
		Holder depthScript = surface.depth();
		for (int floorIndex = 0, floorCount = yLevels.size(); floorIndex < floorCount; floorIndex++) {
			int y = yLevels.getInt(floorIndex) + delta;
			permuter.setSeed(Permuter.permute(columnSeed, y));
			int depth = BigGlobeMath.floorI(depthScript.evaluate(column, y, permuter));
			boolean top = true;
			for (int i = 0; i < depth; i++) {
				pos.setY(y + i * delta);
				BlockState existingState = chunk.getBlockState(pos);
				if (existingState.isAir()) {
					top = true;
				}
				else {
					chunk.setBlockState(pos, top ? surface.top_state() : surface.under_state(), false);
					top = false;
				}
			}
		}
	}

	public void runCaveOverriders(ScriptStructures structures, NetherColumn column, boolean rawTerrain) {
		NoiseOverrider.Context context = NoiseOverrider.Context.caves(column, structures, rawTerrain);
		for (StructureStartWrapper start : structures.starts) {
			if (start.structure().entry.value().getType() == BigGlobeStructures.NETHER_PILLAR) {
				for (StructurePiece piece : start.pieces()) {
					((NetherPillarStructure.Piece)(piece)).runCaveExclusions(context);
				}
			}
		}
		for (NoiseOverrider.Holder overrider : this.caveOverriders) {
			overrider.override(context);
		}
	}

	public void runCavernOverriders(ScriptStructures structures, NetherColumn column, boolean rawTerrain) {
		NoiseOverrider.Context context = NoiseOverrider.Context.caverns(column, structures, rawTerrain);
		for (StructureStartWrapper start : structures.starts) {
			if (start.structure().entry.value().getType() == BigGlobeStructures.NETHER_PILLAR) {
				for (StructurePiece piece : start.pieces()) {
					((NetherPillarStructure.Piece)(piece)).runCaveExclusions(context);
				}
			}
		}
		for (NoiseOverrider.Holder overrider : this.cavernOverriders) {
			overrider.override(context);
		}
	}

	@Override
	public void generateRawTerrain(Executor executor, Chunk chunk, StructureAccessor structureAccessor, boolean distantHorizons) {
		ChunkOfColumns<NetherColumn> columns = this.chunkColumnCache.get();
		ScriptStructures structures = ScriptStructures.getStructures(structureAccessor, chunk.getPos(), distantHorizons);
		try {
			this.profiler.run("initial terrain column values", () -> {
				columns.setPosAndPopulate(chunk.getPos().getStartX(), chunk.getPos().getStartZ(), column -> {
					column.getLocalCell();
					column.getEdginess();
					column.getCaveNoise();
					this.runCaveOverriders(structures, column, true);
					column.getCavernNoise();
					this.runCavernOverriders(structures, column, true);
					column.populateCaveAndCavernFloors();
				});
			});
			if (chunk instanceof PositionCacheHolder holder) {
				holder.bigglobe_setPositionCache(new NetherPositionCache(columns));
			}
			this.profiler.run("set raw terrain blocks", () -> {
				this.generateRawSections(chunk, columns, structures, distantHorizons);
			});
			this.profiler.run("Bedrock", () -> {
				CompletableFuture<Void> lower = CompletableFuture.runAsync(() -> BedrockReplacer.generateBottom(new SectionGenerationContext(chunk, chunk.getSection(chunk.getSectionIndex(this.settings.min_y()     )), this.seed, columns)));
				CompletableFuture<Void> upper = CompletableFuture.runAsync(() -> BedrockReplacer.generateTop   (new SectionGenerationContext(chunk, chunk.getSection(chunk.getSectionIndex(this.settings.max_y() - 16)), this.seed, columns)));
				lower.join();
				upper.join();
			});
			this.profiler.run("Recount", () -> {
				this.generateSectionsParallelSimple(
					chunk,
					chunk.getBottomY(),
					chunk.getTopY(),
					columns,
					SectionGenerationContext::recalculateCounts
				);
			});
			this.profiler.run("Init heightmaps", () -> {
				int maxY = this.settings.max_y();
				this.setHeightmaps(chunk, (index, includeWater) -> maxY);
			});
			this.profiler.run("Surface", () -> {
				this.generateSurface(chunk, columns);
			});
			this.profiler.run("Raw structure generation", () -> {
				RawGenerationStructure.generateAll(structures, this.seed, chunk, columns, distantHorizons);
			});
		}
		finally {
			this.chunkColumnCache.reclaim(columns);
		}
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

			NetherPositionCache cache = chunk instanceof PositionCacheHolder holder && holder.bigglobe_getPositionCache() instanceof NetherPositionCache nether ? nether : null;
			ChunkOfColumns<NetherColumn> columns = this.chunkColumnCache.get();
			try {
				this.profiler.run("Initial feature column values", () -> {
					ScriptStructures structures = ScriptStructures.getStructures(structureAccessor, chunk.getPos(), distantHorizons);
					columns.setPosAndPopulate(chunk.getPos().getStartX(), chunk.getPos().getStartZ(), column -> {
						column.getLocalCell();
						column.getEdginess();
						if (cache == null) {
							column.getCaveNoise();
							this.runCaveOverriders(structures, column, false);
							column.getCavernNoise();
							this.runCavernOverriders(structures, column, false);
							column.populateCaveAndCavernFloors();
						}
					});
				});
				NetherPositionCache cache_ = cache != null ? cache : new NetherPositionCache(columns);
				this.profiler.run("Feature placement", () -> {
					BlockPos.Mutable pos = new BlockPos.Mutable();
					Permuter permuter = new Permuter(0L);
					MojangPermuter mojang = permuter.mojang();
					for (int columnIndex = 0; columnIndex < 256; columnIndex++) {
						NetherColumn column = columns.getColumn(columnIndex);
						pos.setX(column.x).setZ(column.z);
						permuter.setSeed(Permuter.permute(this.seed ^ 0xC4D38782789D95FDL, column.x, column.z));
						LocalNetherSettings localSettings = column.getLocalCell().settings;
						this.runDecorators(world, pos, mojang, localSettings.caverns().floor_decorator(), cache_.cavernFloors[columnIndex]);
						this.runDecorators(world, pos, mojang, localSettings.caverns().ceiling_decorator(), cache_.cavernCeilings[columnIndex]);
						this.runDecorators(world, pos, mojang, localSettings.caves().floor_decorator(), cache_.caveFloors[columnIndex]);
						this.runDecorators(world, pos, mojang, localSettings.caves().ceiling_decorator(), cache_.caveCeilings[columnIndex]);
						this.runDecorators(world, pos, mojang, localSettings.fluid_decorator(), column.getLocalCell().lavaLevel);
					}
				});
			}
			finally {
				this.chunkColumnCache.reclaim(columns);
			}
		});
	}

	@Override
	public boolean canStructureSpawn(RegistryEntry<Structure> entry, StructureStart start, Permuter permuter) {
		StructureStartWrapper wrapper = StructureStartWrapper.of(entry, start);
		NetherColumn column = this.column(0, 0);
		for (ScriptStructureOverrider.Holder overrider : this.structureOverriders) {
			if (!overrider.override(wrapper, column, permuter)) return false;
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

	@Override
	public Codec<? extends ChunkGenerator> getCodec() {
		return NETHER_CODEC;
	}

	@Override
	public void populateEntities(ChunkRegion region) {

	}

	@Override
	public int getWorldHeight() {
		return this.settings.height();
	}

	@Override
	public int getSeaLevel() {
		return this.settings.min_y();
	}

	@Override
	public int getMinimumY() {
		return this.settings.min_y();
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		return this.settings.max_y();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		NetherColumn column = this.column(x, z);
		int worldMinY = this.settings.min_y();
		int worldMaxY = this.settings.max_y();
		return new VerticalBlockSample(worldMinY, Util.make(new BlockState[worldMaxY - worldMinY], states -> {
			LocalNetherSettings localSettings = column.getLocalCell().settings;
			int lavaLevel = column.getLocalCell().lavaLevel;
			ColumnYToDoubleScript.Holder widthScript = localSettings.caves().width();
			double[] caveNoise = column.getCaveNoise();
			double[] cavernNoise = column.getCavernNoise();
			int cavernLowerY = localSettings.caverns().min_y();
			int cavernUpperY = localSettings.caverns().max_y();
			for (int index = 0, length = states.length; index < length; index++) {
				int y = index + worldMinY;
				double caveWidth = widthScript.evaluate(column, y);
				if (
					caveNoise[y - worldMinY] < caveWidth * caveWidth || (
						y >= cavernLowerY &&
						y < cavernUpperY &&
						cavernNoise[y - cavernLowerY] < 0.0D
					)
				) {
					states[index] = y < lavaLevel ? BlockStates.LAVA : BlockStates.AIR;
				}
				else {
					states[index] = localSettings.filler();
				}
			}
		}));
	}
}