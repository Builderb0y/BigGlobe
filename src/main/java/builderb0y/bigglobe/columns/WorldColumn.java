package builderb0y.bigglobe.columns;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.math.BigGlobeMath;

public abstract class WorldColumn extends Column {

	public final long seed;

	public WorldColumn(long seed, int x, int z) {
		super(x, z);
		this.seed = seed;
	}

	public static WorldColumn forWorld(StructureWorldAccess world, int x, int z) {
		if (((ServerChunkManager)(world.getChunkManager())).getChunkGenerator() instanceof BigGlobeChunkGenerator generator) {
			return generator.column(x, z);
		}
		else {
			return new VanillaWorldColumn(world, x, z);
		}
	}

	public static WorldColumn forGenerator(long seed, ChunkGenerator chunkGenerator, NoiseConfig noiseConfig, int x, int z) {
		if (chunkGenerator instanceof BigGlobeChunkGenerator generator) {
			return generator.column(x, z);
		}
		else {
			return new VanillaWorldColumn(seed, chunkGenerator, noiseConfig, x, z);
		}
	}

	/**
	returns the Y level of the block above the primary surface of this column,
	or {@link Integer#MIN_VALUE} if no such surface exists.
	this is not necessarily the limit for where blocks can be placed in the column.
	for example, overworld skylands and end clouds are not
	considered by this method. additionally, in overworld oceans,
	this value is the bottom of the ocean, not the top of it.
	this value should differ from {@link #getFinalTopHeightD()} by no more than 1.0.
	*/
	public int getFinalTopHeightI() {
		double height = this.getFinalTopHeightD();
		return Double.isNaN(height) ? Integer.MIN_VALUE : BigGlobeMath.ceilI(height);
	}

	/**
	similar to {@link #getFinalTopHeightI()},
	but not required to be rounded to the nearest integer.
	in general, {@link #getFinalTopHeightI()} is the
	{@link BigGlobeMath#ceilI(double) ceil} of this value.
	if there is no surface at this column's position, returns {@link Double#NaN}.
	*/
	public abstract double getFinalTopHeightD();

	/**
	returns the Y level of the bottom of the primary surface of this column,
	or {@link Integer#MIN_VALUE} if no such surface exists.
	this is not necessarily the limit for where blocks can be placed in the column.
	for example, end clouds are not considered by this method.
	this value should differ from {@link #getFinalBottomHeightD()} by no more than 1.0.
	*/
	public int getFinalBottomHeightI() {
		double height = this.getFinalBottomHeightD();
		return Double.isNaN(height) ? Integer.MIN_VALUE : BigGlobeMath.floorI(height);
	}

	/**
	similar to {@link #getFinalBottomHeightI()},
	but not required to be rounded to the nearest integer.
	in general, {@link #getFinalBottomHeightI()} is the
	{@link BigGlobeMath#floorI(double)} floor} of this value.
	if there is no surface at this column's position, returns {@link Double#NaN}.
	*/
	public abstract double getFinalBottomHeightD();

	/**
	returns true if this column has a primary surface at this column's location.
	*/
	public boolean hasTerrain() {
		return true;
	}

	public abstract RegistryEntry<Biome> getBiome(int y);

	public RegistryEntry<Biome> getSurfaceBiome() {
		return this.getBiome(this.getFinalTopHeightI());
	}

	public abstract boolean isTerrainAt(int y, boolean cache);

	@Override
	public abstract WorldColumn blankCopy();
}