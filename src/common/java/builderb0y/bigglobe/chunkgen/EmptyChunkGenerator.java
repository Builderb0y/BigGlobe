package builderb0y.bigglobe.chunkgen;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

@AddPseudoField("biome_source")
public class EmptyChunkGenerator extends ChunkGenerator {

	public static final MapCodec<EmptyChunkGenerator> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(EmptyChunkGenerator.class);

	public static record Height(int min_y, int max_y) {

	}

	public final Height height;

	public EmptyChunkGenerator(
		Height height,
		BiomeSource biome_source
	) {
		super(biome_source);
		this.height = height;
	}

	public BiomeSource biome_source() {
		return this.biomeSource;
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, BigGlobeMod.modID("empty"), CODEC);
	}

	@Override
	public MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public void applyCarvers(
		WorldGenRegion chunkRegion,
		long seed,
		RandomState noiseConfig,
		BiomeManager biomeAccess,
		StructureManager structureAccessor,
		ChunkAccess chunk

	) {

	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState noiseConfig, ChunkAccess chunk) {

	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {

	}

	@Override
	public int getGenDepth() {
		return this.height.max_y - this.height.min_y;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(

		Blender blender,
		RandomState noiseConfig,
		StructureManager structureAccessor,
		ChunkAccess chunk
	) {
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getSeaLevel() {
		return this.height.min_y;
	}

	@Override
	public int getMinY() {
		return this.height.max_y;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
		return this.height.min_y;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig) {
		BlockState[] states = new BlockState[16];
		Arrays.fill(states, BlockStates.AIR);
		return new NoiseColumn(this.height.min_y, states);
	}

	@Override
	public void

	addDebugScreenInfo

		(List<String> text, RandomState noiseConfig, BlockPos pos) {

	}

	@Nullable
	@Override
	public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(ServerLevel world, HolderSet<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures) {
		return null;
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structureAccessor) {

	}

	@Override
	public WeightedList<SpawnerData> getMobsAt(Holder<Biome> biome, StructureManager accessor, MobCategory group, BlockPos pos) {
		return WeightedList.of();
	}

	@Override
	public void createStructures(
		RegistryAccess registryManager,
		ChunkGeneratorStructureState placementCalculator,
		StructureManager structureAccessor,
		ChunkAccess chunk,
		StructureTemplateManager structureTemplateManager
		, ResourceKey<Level> dimension
	) {

	}

	@Override
	public void createReferences(WorldGenLevel world, StructureManager structureAccessor, ChunkAccess chunk) {

	}
}