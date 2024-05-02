package builderb0y.bigglobe.chunkgen;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureSet.WeightedEntry;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.versions.RegistryVersions;

#if MC_VERSION > MC_1_19_2
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
#endif

@AddPseudoField("biome_source")
#if MC_VERSION <= MC_1_19_2
@AddPseudoField("structure_set_registry")
#endif
public class EmptyChunkGenerator extends ChunkGenerator {

	#if MC_VERSION >= MC_1_20_5
		public static final MapCodec<EmptyChunkGenerator> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(EmptyChunkGenerator.class);
	#else
		public static final Codec<EmptyChunkGenerator> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(EmptyChunkGenerator.class).codec();
	#endif

	public static record Height(int min_y, int max_y) {}
	public final Height height;

	public EmptyChunkGenerator(
		#if MC_VERSION <= MC_1_19_2
		BetterRegistry<StructureSet> structure_set_registry,
		#endif
		Height height,
		BiomeSource biome_source
	) {
		super(
			#if MC_VERSION <= MC_1_19_2
			((BetterHardCodedRegistry<StructureSet>)(structure_set_registry)).registry,
			Optional.empty(),
			#endif
			biome_source
		);
		this.height = height;
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
		Registry.register(RegistryVersions.chunkGenerator(), BigGlobeMod.modID("empty"), CODEC);
	}

	@Override
	public #if MC_VERSION >= MC_1_20_5 MapCodec #else Codec #endif <? extends ChunkGenerator> getCodec() {
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
	public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getSeaLevel() {
		return this.height.min_y;
	}

	@Override
	public int getMinimumY() {
		return this.height.max_y;
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		return this.height.min_y;
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		BlockState[] states = new BlockState[16];
		Arrays.fill(states, BlockStates.AIR);
		return new VerticalBlockSample(this.height.min_y, states);
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {

	}

	@Nullable
	@Override
	public Pair<BlockPos, RegistryEntry<Structure>> locateStructure(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures) {
		return null;
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {

	}

	@Override
	public Pool<SpawnEntry> getEntitySpawnList(RegistryEntry<Biome> biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos) {
		return Pool.empty();
	}

	#if MC_VERSION > MC_1_19_2
		@Override
		public void setStructureStarts(DynamicRegistryManager registryManager, StructurePlacementCalculator placementCalculator, StructureAccessor structureAccessor, Chunk chunk, StructureTemplateManager structureTemplateManager) {

		}
	#else
		@Override
		public void setStructureStarts(DynamicRegistryManager dynamicRegistryManager, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk, StructureTemplateManager structureTemplateManager, long l) {

		}
	#endif

	@Override
	public boolean trySetStructureStart(WeightedEntry weightedEntry, StructureAccessor structureAccessor, DynamicRegistryManager dynamicRegistryManager, NoiseConfig noiseConfig, StructureTemplateManager structureManager, long seed, Chunk chunk, ChunkPos pos, ChunkSectionPos sectionPos) {
		return false;
	}

	@Override
	public void addStructureReferences(StructureWorldAccess world, StructureAccessor structureAccessor, Chunk chunk) {

	}
}