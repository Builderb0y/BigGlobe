package builderb0y.bigglobe.columns;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.densityfunction.DensityFunction.NoisePos;
import net.minecraft.world.gen.noise.NoiseConfig;

import builderb0y.bigglobe.math.BigGlobeMath;

public class VanillaWorldColumn extends WorldColumn {

	public static final int
		FINAL_HEIGHT          = 1 << 0,
		VERTICAL_BLOCK_SAMPLE = 1 << 1;

	public final ChunkGenerator chunkGenerator;
	public final NoiseConfig noise;
	/** used only by {@link ChunkGenerator#getHeight(int, int, Heightmap.Type, HeightLimitView, NoiseConfig)}. */
	public final HeightLimitView world;

	public int finalHeight;
	public VerticalBlockSample verticalBlockSample;

	public final MutableNoisePos noisePos = this.new MutableNoisePos();

	public VanillaWorldColumn(long seed, HeightLimitView world, ChunkGenerator chunkGenerator, NoiseConfig noise, int x, int z) {
		super(seed, x, z);
		this.world = world;
		this.chunkGenerator = chunkGenerator;
		this.noise = noise;
	}

	public VanillaWorldColumn(long seed, ChunkGenerator chunkGenerator, NoiseConfig noise, int x, int z) {
		super(seed, x, z);
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
			world.getSeed(),
			world,
			((ServerChunkManager)(world.getChunkManager())).getChunkGenerator(),
			((ServerChunkManager)(world.getChunkManager())).getNoiseConfig(),
			x,
			z
		);
	}

	public NoisePos noisePos(double y) {
		this.noisePos.y = BigGlobeMath.floorI(y);
		return this.noisePos;
	}

	public double getTemperature                          (double y) { return this.noise.getMultiNoiseSampler().temperature                    ().sample(this.noisePos(y)); }
	public double getHumidity                             (double y) { return this.noise.getMultiNoiseSampler().humidity                       ().sample(this.noisePos(y)); }
	public double getContinentalness                      (double y) { return this.noise.getMultiNoiseSampler().continentalness                ().sample(this.noisePos(y)); }
	public double getErosion                              (double y) { return this.noise.getMultiNoiseSampler().erosion                        ().sample(this.noisePos(y)); }
	public double getDepth                                (double y) { return this.noise.getMultiNoiseSampler().depth                          ().sample(this.noisePos(y)); }
	public double getWeirdness                            (double y) { return this.noise.getMultiNoiseSampler().weirdness                      ().sample(this.noisePos(y)); }

	public double getRouterBarrier                        (double y) { return this.noise.getNoiseRouter      ().barrierNoise                   ().sample(this.noisePos(y)); }
	public double getRouterFluidLevelFloodedness          (double y) { return this.noise.getNoiseRouter      ().fluidLevelFloodednessNoise     ().sample(this.noisePos(y)); }
	public double getRouterFluidLevelSpread               (double y) { return this.noise.getNoiseRouter      ().fluidLevelSpreadNoise          ().sample(this.noisePos(y)); }
	public double getRouterLava                           (double y) { return this.noise.getNoiseRouter      ().lavaNoise                      ().sample(this.noisePos(y)); }
	public double getRouterTemperature                    (double y) { return this.noise.getNoiseRouter      ().temperature                    ().sample(this.noisePos(y)); }
	public double getRouterVegetation                     (double y) { return this.noise.getNoiseRouter      ().vegetation                     ().sample(this.noisePos(y)); }
	public double getRouterContinents                     (double y) { return this.noise.getNoiseRouter      ().continents                     ().sample(this.noisePos(y)); }
	public double getRouterErosion                        (double y) { return this.noise.getNoiseRouter      ().erosion                        ().sample(this.noisePos(y)); }
	public double getRouterDepth                          (double y) { return this.noise.getNoiseRouter      ().depth                          ().sample(this.noisePos(y)); }
	public double getRouterRidges                         (double y) { return this.noise.getNoiseRouter      ().ridges                         ().sample(this.noisePos(y)); }
	public double getRouterInitialDensityWithoutJaggedness(double y) { return this.noise.getNoiseRouter      ().initialDensityWithoutJaggedness().sample(this.noisePos(y)); }
	public double getRouterFinalDensity                   (double y) { return this.noise.getNoiseRouter      ().finalDensity                   ().sample(this.noisePos(y)); }
	public double getRouterVeinToggle                     (double y) { return this.noise.getNoiseRouter      ().veinToggle                     ().sample(this.noisePos(y)); }
	public double getRouterVeinRidged                     (double y) { return this.noise.getNoiseRouter      ().veinRidged                     ().sample(this.noisePos(y)); }
	public double getRouterVeinGap                        (double y) { return this.noise.getNoiseRouter      ().veinGap                        ().sample(this.noisePos(y)); }

	public VerticalBlockSample getVerticalBlockSample() {
		return (
			this.setFlag(VERTICAL_BLOCK_SAMPLE)
			? this.verticalBlockSample = this.chunkGenerator.getColumnSample(this.x, this.z, this.world, this.noise)
			: this.verticalBlockSample
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
	public boolean isTerrainAt(int y, boolean cache) {
		return !this.getVerticalBlockSample().getState(y).isAir();
	}

	@Override
	public VanillaWorldColumn blankCopy() {
		return new VanillaWorldColumn(this.seed, this.world, this.chunkGenerator, this.noise, this.x, this.z);
	}

	public class MutableNoisePos implements NoisePos {

		public int y;

		@Override public int blockX() { return VanillaWorldColumn.this.x; }
		@Override public int blockY() { return this.y; }
		@Override public int blockZ() { return VanillaWorldColumn.this.z; }
	}
}