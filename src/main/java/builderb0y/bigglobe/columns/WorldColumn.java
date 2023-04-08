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
	returns the Y level above the highest block that can generate in this column as part of normal terrain,
	or {@link Integer#MIN_VALUE} if no blocks can generate in this column.
	in other words, blocks can only exist in this column at positions where
	y >= {@link #getFinalBottomHeightI()} && y < {@link #getFinalTopHeightI()}.
	this value should differ from {@link #getFinalTopHeightD()} by no more than 1.0.
	*/
	public int getFinalTopHeightI() {
		double height = this.getFinalTopHeightD();
		return Double.isNaN(height) ? Integer.MIN_VALUE : BigGlobeMath.ceilI(height);
	}

	/**
	returns the Y level of the highest block that can generate in this column as part of normal
	terrain as a double, or {@link Double#NaN} if no blocks can generate in this column.
	the returned double is NOT required to be rounded to the nearest integer.
	*/
	public abstract double getFinalTopHeightD();

	/**
	returns the Y level of the lowest block that can generate in this column,
	or {@link Integer#MIN_VALUE} if no blocks can generate in this column.
	in other words, blocks can only exist in this column at positions where
	y >= {@link #getFinalBottomHeightI()} && y < {@link #getFinalTopHeightI()}.
	this value should differ from {@link #getFinalTopHeightD()} by no more than 1.0.
	*/
	public int getFinalBottomHeightI() {
		double height = this.getFinalBottomHeightD();
		return Double.isNaN(height) ? Integer.MIN_VALUE : BigGlobeMath.floorI(height);
	}

	/**
	returns the Y level of the lowest block that can generate in this column
	as a double, or {@link Double#NaN} if no blocks can generate in this column.
	the returned double is NOT required to be rounded to the nearest integer.
	*/
	public abstract double getFinalBottomHeightD();

	/**
	returns true if any blocks can generate in this column.
	this can be used as a shortcut instead of checking if the
	above 4 methods returned {@link Integer#MIN_VALUE} or {@link Double#NaN}.
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