package builderb0y.bigglobe.chunkgen;

import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;

import builderb0y.bigglobe.columns.WorldColumn;

public class ColumnBiomeSource extends BiomeSource {

	public static final Codec<ColumnBiomeSource> CODEC = new Codec<>() {

		@Override
		public <T> DataResult<Pair<ColumnBiomeSource, T>> decode(DynamicOps<T> ops, T input) {
			return DataResult.error(() -> "Should not decode ColumnBiomeSource directly.");
		}

		@Override
		public <T> DataResult<T> encode(ColumnBiomeSource input, DynamicOps<T> ops, T prefix) {
			return DataResult.error(() -> "Should not encode ColumnBiomeSource directly.");
		}

		@Override
		public String toString() {
			return "ColumnBiomeSource.CODEC";
		}
	};

	public BigGlobeChunkGenerator generator;
	public ThreadLocal<WorldColumn> threadLocalColumn;
	public Stream<RegistryEntry<Biome>> biomeStream;

	public ColumnBiomeSource(Stream<RegistryEntry<Biome>> biomeStream) {
		this.biomeStream = biomeStream;
	}

	public void setGenerator(BigGlobeChunkGenerator generator) {
		this.generator = generator;
		this.threadLocalColumn = ThreadLocal.withInitial(() -> generator.column(0, 0));
	}

	@Override
	public Codec<? extends BiomeSource> getCodec() {
		return CODEC;
	}

	/**
	this will only be called once, which conveniently
	is the number of times a Stream can be used.
	*/
	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		Stream<RegistryEntry<Biome>> stream = this.biomeStream;
		this.biomeStream = null;
		return stream;
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseSampler noise) {
		WorldColumn column = this.threadLocalColumn.get();
		column.setPos(x << 2, z << 2);
		return column.getBiome(y << 2);
	}
}