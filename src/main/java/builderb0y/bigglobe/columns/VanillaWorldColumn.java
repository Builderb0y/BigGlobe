package builderb0y.bigglobe.columns;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

public class VanillaWorldColumn extends WorldColumn {

	public static final int
		FINAL_HEIGHT = 1 << 0;

	public final ChunkGenerator chunkGenerator;
	public final NoiseConfig noise;
	/** used only by {@link ChunkGenerator#getHeight(int, int, Heightmap.Type, HeightLimitView, NoiseConfig)}. */
	public final HeightLimitView world;
	public int finalHeight;

	public VanillaWorldColumn(HeightLimitView world, ChunkGenerator chunkGenerator, NoiseConfig noise, int x, int z) {
		super(noise.getLegacyWorldSeed(), x, z);
		this.world = world;
		this.chunkGenerator = chunkGenerator;
		this.noise = noise;
	}

	public VanillaWorldColumn(ChunkGenerator chunkGenerator, NoiseConfig noise, int x, int z) {
		super(noise.getLegacyWorldSeed(), x, z);
		this.chunkGenerator = chunkGenerator;
		this.noise = noise;
		this.world = new HeightLimitView() {

			@Override
			public int getHeight() {
				return chunkGenerator.getWorldHeight();
			}

			@Override
			public int getBottomY() {
				return chunkGenerator.getMinimumY();
			}
		};
	}

	public VanillaWorldColumn(StructureWorldAccess world, int x, int z) {
		this(
			world,
			((ServerChunkManager)(world.getChunkManager())).getChunkGenerator(),
			((ServerChunkManager)(world.getChunkManager())).getNoiseConfig(),
			x,
			z
		);
	}

	@Override
	public int getFinalTopHeightI() {
		return this.setFlag(FINAL_HEIGHT) ? this.finalHeight = this.computeTopHeight() : this.finalHeight;
	}

	public int computeTopHeight() {
		int surface = this.chunkGenerator.getHeight(this.x, this.z, Heightmap.Type.OCEAN_FLOOR_WG, this.world, this.noise);
		return surface > this.chunkGenerator.getMinimumY() ? surface : Integer.MIN_VALUE;
	}

	@Override
	public double getFinalTopHeightD() {
		int height = this.getFinalTopHeightI();
		return height != Integer.MIN_VALUE ? height : Double.NaN;
	}

	@Override
	public double getFinalBottomHeightD() {
		return this.getFinalBottomHeightI();
	}

	@Override
	public int getFinalBottomHeightI() {
		return this.hasTerrain() ? this.chunkGenerator.getMinimumY() : Integer.MIN_VALUE;
	}

	@Override
	public boolean hasTerrain() {
		return this.getFinalTopHeightI() != Integer.MIN_VALUE;
	}

	@Override
	public RegistryEntry<Biome> getBiome(int y) {
		return this.chunkGenerator.getBiomeSource().getBiome(this.x >> 2, y >> 2, this.z >> 2, this.noise.getMultiNoiseSampler());
	}

	@Override
	public VanillaWorldColumn blankCopy() {
		return new VanillaWorldColumn(this.world, this.chunkGenerator, this.noise, this.x, this.z);
	}
}